/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package superscalarprocessor;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import javafx.util.Pair;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author abhisheky
 */

class Indices{
    Integer resIndex;
    Integer robIndex;
    Integer pcValue;
    
    public Indices(Integer res, Integer rob, Integer pc){
        resIndex= res;
        robIndex= rob;
        pcValue= pc;
    }
}

class Latencies{
    Integer fpAddFULatency;
    Integer fpMulFULatency;
    Integer fpLdSdFULatency;
    
    public Latencies(Integer addLatency, Integer mulLatency,
            Integer ldSdLatency){
        fpAddFULatency=addLatency;
        fpMulFULatency=mulLatency;
        fpLdSdFULatency=ldSdLatency;
    }
}

public class Tomasulo extends javax.swing.JFrame {

    /**
     * Creates new form Tomasulo
     */
    public Integer flosPCIndex, flosOpIndex, flosOpd1Index,
                    flosOpd2Index,flosOpd3Index;
    
    public Integer flrRegIndex, flrBusyIndex, flrTagIndex,flrDataIndex;
    
    public Integer robInstructionIndex, robBIndex, robIssueIndex,robExecIndex,
                    robCompIndex, robSIndex,robVIndex, robAddrIndex;
    
    public Integer resIdIndex,resOpIndex,resBIndex,resQjIndex,
           resVjIndex,resQkIndex,resVkIndex,resAddrIndex;
    
    public Integer storeBufferAddressIndex, storeBufferDataIndex;
    
    private Timer timer;
    private ArrayList<String> instructionsCache;
    private HashMap<Integer, Integer> dataCache;
    private Integer maxPC;
    private Integer programCounter;
    private Integer instructionCounter;
    private Integer instructionsCount;
    private CommonDataBus CDB;
    private Latencies fuLatency;
    private HashMap<Object, Object> tagValueMap;
    
    private JTable flosTable, flrTable, robTable,
                    fpAddResTable, fpMulResTable,
                    fpLdStResTable, storeBufferTable;
    
    private DefaultTableModel flosTableModel,
            flrTableModel, robTableModel, fpAddResTableModel,
            fpMulResTableModel, fpLdStResTableModel, storeBufferTableModel;
    
    private HashMap<String, String> mappedOperator;
    private Integer clockCycles;
    private Integer stalls;
    private ArrayList<Indices> adderIssueQueue;
    private ArrayList<Indices> mulIssueQueue;
    private ArrayList<Indices> ldStIssueQueue;
    private ArrayList<Integer> robEntryOrder;
    private ArrayList<Integer> speculativeInstruction;
    private Boolean speculativeExecution;
    private Integer addExecutionCyclesCompleted;
    private Integer mulExecutionCyclesCompleted;
    private Integer ldSdExecutionCyclesCompleted;
    private Boolean storeExecution;
    
    public Tomasulo() {
        initComponents();
        initializeParameters();
        createTables();
        initializeFLOS();
        initializeFLR();
        initializeROB();
        initializeReservationStations();
        initializeStoreBuffer();
        resizeColumns();
        createOperatorsMapping();
        readInstructionFromFile("t3.b");
        initializeColumnIndices();
                
        timer= new Timer(1000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if(instructionCounter < instructionsCount){
                        clockCycles++;
                        
                        getDataFromCDB();
                        instructionRetireStage();
                        instructionExecute();
                        instructionIssue();
                        Integer emptySlots= findFlosSpace();
                        channelizeFlosInstructions(emptySlots);
                        bringInstructionsToFlosTable(emptySlots);
                    }else{
                        //System.out.println(instructionCounter+"  "+instructionsCount);
                        clockCycles++;
                        
                        getDataFromCDB();
                        instructionRetireStage();
                        instructionExecute();
                        instructionIssue();
                        Integer emptySlots= findFlosSpace();
                        channelizeFlosInstructions(emptySlots);
                        System.out.println(clockCycles);
                    }
                }
        });
    }
    
    /*Execution stage*/
    public void setResultInReservationStation(DefaultTableModel tableModel,Object tag, Object data,
        ArrayList<Indices> indexQueue){
        
        Iterator<Indices> iterator= indexQueue.iterator();
        while(iterator.hasNext()){
                Integer index= iterator.next().resIndex;
                if(tableModel.getValueAt(index, resQjIndex).equals(tag)){
                    tableModel.setValueAt(data, index,resVjIndex);
                    tableModel.setValueAt("0", index, resQjIndex);
                }
                
                if(tableModel.getValueAt(index, resQkIndex).equals(tag)){
                    tableModel.setValueAt("0", index, resQkIndex);
                    tableModel.setValueAt(data,index, resVkIndex);
                }
        }
    }
    
    public void instructionRetireStage(){
        // write the results of the valid entries to the register
        Iterator<Integer> iterator= robEntryOrder.iterator();
        
        while(iterator.hasNext()){
            Integer index= iterator.next();
            Object tag= robTable.getValueAt(index, robAddrIndex);
            Integer valid=Integer.parseInt(robTable.getValueAt(index, robVIndex).toString());
            if(valid==1){
                // search for the tag in the register file and update the registers
                for(int i=0;i< flrTable.getRowCount(); i++){
                    if(tag.equals(flrTable.getValueAt(i, flrTagIndex))){
                        flrTableModel.setValueAt(tagValueMap.get(tag), i, flrDataIndex);
                        flrTableModel.setValueAt("0", i, flrTagIndex);
                        flrTableModel.setValueAt(0, i, flrBusyIndex);
                    }
                }
                resetROBRow(index);
                iterator.remove();
            }else break;
        }
    }
    
    public void getResultFromParticularUnit(Result res){
        Object data= res.getData();
        Object tag= res.getTag();
        Integer robIndex= res.getRobIndex();
        //put result at required places in ROB, since is directed to only once entry in 
        //ROB, so no need to search through the whole ROB for the same
        //robTableModel.setValueAt(tag, robIndex, robAddrIndex);
        tagValueMap.put(tag, data);
        robTableModel.setValueAt(1, robIndex, robVIndex);
        //provide the results to reservation Stations
        setResultInReservationStation(fpAddResTableModel, tag, data, adderIssueQueue);
        setResultInReservationStation(fpMulResTableModel, tag, data, mulIssueQueue);
        setResultInReservationStation(fpLdStResTableModel,tag, data, ldStIssueQueue);
    }
    
    public void getDataFromCDB(){
        if(CDB.addFUResult.getValid()){
            getResultFromParticularUnit(CDB.addFUResult);
            CDB.addFUResult.resetCDB();
        }
        if(CDB.mulFUResult.getValid()){
            getResultFromParticularUnit(CDB.mulFUResult);
            CDB.mulFUResult.resetCDB();
        }
        if(CDB.ldSdFUResult.getValid()){
            getResultFromParticularUnit(CDB.ldSdFUResult);
            CDB.ldSdFUResult.resetCDB();
        }
    }
    
    public void resetRobEntriesWithSpeculativelyExecutedInstructions(){
        Iterator<Integer> iterator= robEntryOrder.iterator();
        while(iterator.hasNext()){
            Integer index= iterator.next();
            if(robTable.getValueAt(index, robSIndex).equals(1)){
                resetROBRow(index);
                iterator.remove();
            }
        }
    }
    
    public void executeConditionalBranch(Integer insIndex){
        Object Qj= fpAddResTable.getValueAt(insIndex, resQjIndex);
        if(Qj.equals("0")){
            Integer value= Integer.parseInt(fpAddResTable.getValueAt(insIndex,resVjIndex).toString());
            Integer offset= Integer.parseInt(fpAddResTable.getValueAt(insIndex, resAddrIndex).toString());
            if(value==0){
                resetRobEntriesWithSpeculativelyExecutedInstructions();
                for(int i=0;i< flosTable.getRowCount(); i++)
                    resetFlosRow(i);
                programCounter= offset;
                instructionCounter= (programCounter>>1);
            }else{
                //make all speculative bits in rob to 0
                Iterator<Integer> iterator= robEntryOrder.iterator();
                while(iterator.hasNext()){
                    Integer index= iterator.next();
                    if(robTable.getValueAt(index, robSIndex).equals(1))
                        robTable.setValueAt(0, index, robSIndex);
                }
            }
            
            adderIssueQueue.remove(0);
            resetResRow(insIndex, fpAddResTableModel);
            speculativeExecution=false;
        }
    }
    
    public void adderFUExecution(){
        if(adderIssueQueue.size()>0){
            Integer insIndex= adderIssueQueue.get(0).resIndex;
            Object operator= fpAddResTable.getValueAt(insIndex, resOpIndex);
            if(operator.equals("BEQZ"))
                executeConditionalBranch(insIndex);
            else{
                Object Qj,Qk;
                Qj= fpAddResTable.getValueAt(insIndex, resQjIndex);
                Qk= fpAddResTable.getValueAt(insIndex, resQkIndex);
                if(Qj.equals("0") && Qk.equals("0")){
                    //instruction being sent for execution
                    addExecutionCyclesCompleted=0;
                    Integer operand1= Integer.parseInt(fpAddResTable.getValueAt(insIndex, resVjIndex).toString());
                    Integer operand2= Integer.parseInt(fpAddResTable.getValueAt(insIndex, resVkIndex).toString());
                    String op="+";
                    if(operator.equals("SUB"))
                        op="-";
                    String instruction="result: ("+operand1+") "+op+" ("+operand2+")";
                    fpAdderFU.setText(instruction);
                    if(operator.equals("ADD"))
                        CDB.addFUResult.setData(operand1+operand2);
                    else 
                        CDB.addFUResult.setData(operand1- operand2);
                    CDB.addFUResult.setValid(false);
                    CDB.addFUResult.setRobIndex(adderIssueQueue.get(0).robIndex);
                    CDB.addFUResult.setTag(fpAddResTable.getValueAt(insIndex, resIdIndex));
                    resetResRow(insIndex, fpAddResTableModel);
                    robTableModel.setValueAt(clockCycles, adderIssueQueue.get(0).robIndex, robExecIndex);
                    adderIssueQueue.remove(0);
                }
            }
        }
    }
    
    public void mulFUExecution(){
        if(mulIssueQueue.size()>0){
            Integer insIndex= mulIssueQueue.get(0).resIndex;
            Object Qj,Qk;
            Qj= fpMulResTable.getValueAt(insIndex, resQjIndex);
            Qk= fpMulResTable.getValueAt(insIndex, resQkIndex);
            if(Qj.equals("0") && Qk.equals("0")){
                //instruction being sent for execution
                mulExecutionCyclesCompleted=0;
                Integer operand1= Integer.parseInt(fpMulResTable.getValueAt(insIndex, resVjIndex).toString());
                Integer operand2= Integer.parseInt(fpMulResTable.getValueAt(insIndex, resVkIndex).toString());

                String instruction="Result: ("+operand1+") * ("+operand2+")";
                fpMulFU.setText(instruction);
                CDB.mulFUResult.setData(operand1*operand2);
                CDB.mulFUResult.setValid(false);
                CDB.mulFUResult.setRobIndex(mulIssueQueue.get(0).robIndex);
                CDB.mulFUResult.setTag(fpMulResTable.getValueAt(insIndex, resIdIndex));
                resetResRow(insIndex, fpMulResTableModel);
                robTableModel.setValueAt(clockCycles, mulIssueQueue.get(0).robIndex, robExecIndex);
                mulIssueQueue.remove(0);
            }
        }
    }
    
    public Integer searchInStoreBuffer(Integer address){
        for(int i=0;i< storeBufferTable.getRowCount(); i++){
            Integer storeBufferAddress= Integer.parseInt(storeBufferTable.getValueAt(i, 
                    storeBufferAddressIndex).toString());
            if(storeBufferAddress.equals(address))
                return i;
        }
        return -1;
    }
    
    public void executeLoad(Integer insIndex){
        Object Qk= fpLdStResTable.getValueAt(insIndex,resQkIndex);
        if(Qk.equals("0")){
            ldSdExecutionCyclesCompleted=0;
            Integer baseAddress= Integer.parseInt(fpLdStResTable.getValueAt(insIndex,resVkIndex).toString());
            Integer offset= Integer.parseInt(fpLdStResTable.getValueAt(insIndex, resAddrIndex).toString());
            Integer effectiveAddress= baseAddress+ offset;
            // load forwarding or bypassing
            Integer dataIndex= searchInStoreBuffer(effectiveAddress);
            Object data;
            if(dataIndex < 0)
                data=dataCache.get(effectiveAddress);
            else 
                data= storeBufferTable.getValueAt(dataIndex, storeBufferDataIndex);
            
            String instruction= "Load: <- "+data;
            loadInstructionField.setText(instruction);
            
            CDB.ldSdFUResult.setData(data);
            CDB.ldSdFUResult.setRobIndex(ldStIssueQueue.get(0).robIndex);
            CDB.ldSdFUResult.setValid(false);
            CDB.ldSdFUResult.setTag(fpLdStResTable.getValueAt(insIndex, resIdIndex));
            resetResRow(insIndex, fpLdStResTableModel);
            robTableModel.setValueAt(clockCycles, ldStIssueQueue.get(0).robIndex, robExecIndex);
            ldStIssueQueue.remove(0);
        }
    }
    
    public void executeStore(Integer insIndex){
        Object Qk= fpLdStResTable.getValueAt(insIndex,resQkIndex);
        Object Qj= fpLdStResTable.getValueAt(insIndex,resQjIndex);
        if(Qk.equals("0") && Qj.equals("0")){
            ldSdExecutionCyclesCompleted=fuLatency.fpLdSdFULatency-1;
            Integer baseAddress= Integer.parseInt(fpLdStResTable.getValueAt(insIndex,resVjIndex).toString());
            Integer offset= Integer.parseInt(fpLdStResTable.getValueAt(insIndex, resAddrIndex).toString());
            Integer effectiveAddress= baseAddress+ offset;
            
            // load forwarding or bypassing
            Object data= fpLdStResTable.getValueAt(insIndex, resVkIndex);
            String instruction= "Store: ["+effectiveAddress+"] <- "+data;
            storeInstructionField.setText(instruction);
            
//            robTableModel.setValueAt(effectiveAddress, ldStIssueQueue.get(0).robIndex, robAddrIndex);
//            tagValueMap.put(effectiveAddress, data);
            Random rand= new Random();
            Integer index= rand.nextInt(storeBufferTable.getRowCount());
            storeBufferTableModel.setValueAt(effectiveAddress,index ,storeBufferAddressIndex);
            storeBufferTableModel.setValueAt(data, index, storeBufferDataIndex);
            resetResRow(insIndex, fpLdStResTableModel);
            robTableModel.setValueAt(clockCycles, ldStIssueQueue.get(0).robIndex, robExecIndex);
//            robTableModel.setValueAt(clockCycles, ldStIssueQueue.get(0).robIndex, robCompIndex);
            CDB.ldSdFUResult.setData(data);
            CDB.ldSdFUResult.setRobIndex(ldStIssueQueue.get(0).robIndex);
            CDB.ldSdFUResult.setTag(effectiveAddress);
            CDB.ldSdFUResult.setValid(false);
            ldStIssueQueue.remove(0);
            storeExecution=true;
        }
    }
    
    public void ldSdFUExecution(){
        if(ldStIssueQueue.size()>0){
            Integer insIndex= ldStIssueQueue.get(0).resIndex;
            Object operator= fpLdStResTable.getValueAt(insIndex, resOpIndex);
            if(operator.equals("LD")){
                executeLoad(insIndex);
            }else if(operator.equals("SD")){
                executeStore(insIndex);
            }
        }
    }
    
    public void instructionExecute(){
        if(addExecutionCyclesCompleted >= fuLatency.fpAddFULatency)
            adderFUExecution();
        else{ 
            addExecutionCyclesCompleted++;
            if(addExecutionCyclesCompleted.equals(fuLatency.fpAddFULatency) && 
                    CDB.addFUResult.getRobIndex() >=0 ){
                robTableModel.setValueAt(clockCycles,CDB.addFUResult.getRobIndex(),robCompIndex);
                CDB.addFUResult.setValid(true);
                fpAdderFU.setText("");
            }
        }
        
        if(mulExecutionCyclesCompleted>= fuLatency.fpMulFULatency)
            mulFUExecution();
        else{ 
            mulExecutionCyclesCompleted++;
            if(mulExecutionCyclesCompleted.equals(fuLatency.fpMulFULatency) && CDB.mulFUResult.getRobIndex() >= 0){
                robTableModel.setValueAt(clockCycles,CDB.mulFUResult.getRobIndex(),robCompIndex);
                CDB.mulFUResult.setValid(true);
                fpMulFU.setText("");
            }
        }
        
        if(ldSdExecutionCyclesCompleted >= fuLatency.fpLdSdFULatency)
            ldSdFUExecution();
        else{
            ldSdExecutionCyclesCompleted++;
            if(ldSdExecutionCyclesCompleted.equals(fuLatency.fpLdSdFULatency) && CDB.ldSdFUResult.getRobIndex() >= 0){
                robTableModel.setValueAt(clockCycles, CDB.ldSdFUResult.getRobIndex(), robCompIndex);
                CDB.ldSdFUResult.setValid(true);
                if(storeExecution)
                    storeInstructionField.setText("");
                else
                    loadInstructionField.setText("");
                storeExecution=false;
            }
        }
    }

    public void resetFlrTable(){
        for(int i=0;i< flrTable.getRowCount(); i++){
            flrTableModel.setValueAt("0", i, flrBusyIndex);
            flrTableModel.setValueAt("0", i, flrTagIndex);
            flrTableModel.setValueAt("0", i, flrDataIndex);
        }
    }
    public void resetROBRow(int rowNum){
        robTableModel.setValueAt("", rowNum, robInstructionIndex);
        robTableModel.setValueAt("0", rowNum, robBIndex);
        robTableModel.setValueAt("", rowNum, robIssueIndex);
        robTableModel.setValueAt("", rowNum, robExecIndex);
        robTableModel.setValueAt("", rowNum, robCompIndex);
        robTableModel.setValueAt("", rowNum, robAddrIndex);
        robTableModel.setValueAt("0", rowNum, robSIndex);
        robTableModel.setValueAt("0", rowNum, robVIndex);
    }
    
    public void resetResRow(int rowNum, DefaultTableModel tableModel){
        tableModel.setValueAt("", rowNum, resOpIndex);
        tableModel.setValueAt("0",rowNum, resBIndex);
        tableModel.setValueAt("0",rowNum, resQjIndex);
        tableModel.setValueAt("",rowNum, resVjIndex);
        tableModel.setValueAt("0",rowNum,resQkIndex);
        tableModel.setValueAt("", rowNum, resVkIndex);
        tableModel.setValueAt("", rowNum, resAddrIndex);
    }
    
    public void resetFlosRow(int rowNum){
        for(int i=0; i< flosTable.getColumnCount(); i++)
            flosTable.setValueAt("",rowNum, i);
    }
    
    public int getFirstAvailableEntry(JTable table){
        int firstAvailable=0;
        int nRows=table.getRowCount();
        while(firstAvailable < nRows){
            if(table.getValueAt(firstAvailable, table.getColumn("B").getModelIndex()).equals("0"))
                return firstAvailable;
            firstAvailable++;
        }
        return -1;
    }
    
    public Integer getRegIndex(Object reg){
        for(int i=0;i< flrTable.getRowCount(); i++){
            if(reg.equals(flrTable.getValueAt(i, flrRegIndex)))
                return i;
        }
        return -1;
    }
    
    public Integer getRegBusyBit(Integer index){
        return Integer.parseInt(flrTable.getValueAt(index,flrBusyIndex).toString());
    }
    
    public Integer issueAddSubMul(Integer firstAvailableIndex, Integer flosIndex, 
            DefaultTableModel tableModel, JTable table){
        
        Integer firstAvailableInRob= getFirstAvailableEntry(robTable);
        Object operator= flosTable.getValueAt(flosIndex,flosOpIndex);
        Object dest= flosTable.getValueAt(flosIndex,flosOpd1Index);
        Object src1= flosTable.getValueAt(flosIndex,flosOpd2Index);
        Object src2= flosTable.getValueAt(flosIndex,flosOpd3Index);
        
        Integer index1= getRegIndex(src1);
        Integer index2= getRegIndex(src2);
        Integer index3= getRegIndex(dest);
        Integer srcBusy1=0, srcBusy2=0;
        
        if(index1>=0)
            srcBusy1= getRegBusyBit(index1);
        if(index2>=0)
            srcBusy2= getRegBusyBit(index2);
        
        if(index1 >= 0){
            if(srcBusy1.equals(1))
                tableModel.setValueAt(flrTable.getValueAt(index1, flrTagIndex), firstAvailableIndex, resQjIndex);
            else
                tableModel.setValueAt(flrTable.getValueAt(index1, flrDataIndex), firstAvailableIndex, resVjIndex);
        }else{
            tableModel.setValueAt(src1, firstAvailableIndex, resVjIndex);
        }
        
        if(index2 >= 0){
            if(srcBusy2.equals(1))
                tableModel.setValueAt(flrTable.getValueAt(index2, flrTagIndex),firstAvailableIndex, resQkIndex);
            else
                tableModel.setValueAt(flrTable.getValueAt(index2, flrDataIndex), firstAvailableIndex, resVkIndex);
        }else{
            tableModel.setValueAt(src2, firstAvailableIndex, resVkIndex);
        }
        
        tableModel.setValueAt(operator, firstAvailableIndex, resOpIndex);
        tableModel.setValueAt(1, firstAvailableIndex, resBIndex);
        
        if(firstAvailableInRob>=0){
            robTableModel.setValueAt(operator+" "+dest+", "+src1+", "+src2, firstAvailableInRob, robInstructionIndex);
            robTableModel.setValueAt(1, firstAvailableInRob, robBIndex);
            robTableModel.setValueAt(clockCycles, firstAvailableInRob, robIssueIndex);
            robEntryOrder.add(firstAvailableInRob);
            if(speculativeExecution)
                robTableModel.setValueAt(1, firstAvailableInRob, robSIndex);
            robTableModel.setValueAt(table.getValueAt(firstAvailableIndex, resIdIndex),
                    firstAvailableInRob, robAddrIndex);
        }
        
        flrTableModel.setValueAt(table.getValueAt(firstAvailableIndex, resIdIndex),index3, flrTagIndex);
        flrTableModel.setValueAt(1, index3,flrBusyIndex);
        return firstAvailableInRob;
    }
    
    public void issueConditionalBranch(Integer flosIndex, Integer firstAvailable){
        Object operator= "BEQZ";
        Object src= flosTable.getValueAt(flosIndex, flosOpd1Index);
//        System.out.println("programCounter: "+programCounter);
        Integer offset= (Integer)flosTable.getValueAt(flosIndex, flosOpd2Index)+
                Integer.parseInt(flosTable.getValueAt(flosIndex, flosPCIndex).toString());
//        System.out.println("Offset: "+offset);
        Integer regIndex= getRegIndex(src);
        Integer regBusy= getRegBusyBit(regIndex);
        if(regBusy.equals(1))
            fpAddResTableModel.setValueAt(flrTable.getValueAt(regIndex, flrTagIndex),
                    firstAvailable, resQjIndex);
        else 
            fpAddResTableModel.setValueAt(flrTable.getValueAt(regIndex, flrDataIndex), 
                    firstAvailable, resVjIndex);
        
        fpAddResTableModel.setValueAt(1, firstAvailable, resBIndex);
        fpAddResTableModel.setValueAt(offset, firstAvailable, resAddrIndex);
        fpAddResTableModel.setValueAt(operator,firstAvailable, resOpIndex);
        speculativeExecution=true;
    }
    
    public void issueUnconditionalBranch(Integer flosIndex){
        Integer offset=0;
        offset= (Integer)flosTable.getValueAt(flosIndex, flosOpd1Index);
        
        programCounter=(Integer)flosTable.getValueAt(flosIndex, flosPCIndex)+offset;
        System.out.println("New PC: "+programCounter);
        instructionCounter= (programCounter>>1);
        for(int i=flosIndex; i>=0; i--)
            resetFlosRow(i);
    }
    
    public Integer issueLoadStore(Integer flosIndex, Integer firstAvailable){
       
        Integer firstAvailableInRob= getFirstAvailableEntry(robTable);
        Object operator= flosTable.getValueAt(flosIndex, flosOpIndex);
        Object dest= flosTable.getValueAt(flosIndex, flosOpd1Index);
        Object src= flosTable.getValueAt(flosIndex, flosOpd2Index);
        Integer offset= (Integer)flosTable.getValueAt(flosIndex, flosOpd3Index);
        
        Integer opd1Index= getRegIndex(dest);
        Integer opd2Index= getRegIndex(src);
        Integer busyBit2= getRegBusyBit(opd2Index);
        Integer busyBit1= getRegBusyBit(opd1Index);
        
        fpLdStResTableModel.setValueAt(operator, firstAvailable,resOpIndex);
        fpLdStResTableModel.setValueAt(1, firstAvailable, resBIndex);
        fpLdStResTableModel.setValueAt(offset,firstAvailable,resAddrIndex);
        
        if(busyBit2.equals(1)){
            fpLdStResTableModel.setValueAt(flrTable.getValueAt(opd2Index, 
                    flrTagIndex), firstAvailable, resQkIndex);
        }else{
            fpLdStResTableModel.setValueAt(flrTable.getValueAt(opd2Index, flrDataIndex), firstAvailable, resVkIndex);
        }
        
        if(operator.equals("SD")){
            if(busyBit1.equals(1))
                fpLdStResTableModel.setValueAt(flrTable.getValueAt(opd1Index, flrTagIndex),firstAvailable,resQjIndex);
            else
                fpLdStResTableModel.setValueAt(flrTable.getValueAt(opd1Index, flrDataIndex),firstAvailable,resVjIndex);
        }
        
        if(firstAvailableInRob >= 0){
            if(operator.equals("LD"))
                robTableModel.setValueAt((Object)(operator+" "+dest+", "+offset+"["+src+"]"), firstAvailableInRob, 
            robInstructionIndex);
            else if(operator.equals("SD"))
                robTableModel.setValueAt((Object)(operator+" "+offset+"["+dest+"], "+src), firstAvailableInRob, 
            robInstructionIndex);
            
            robTableModel.setValueAt(1,firstAvailableInRob,robBIndex);
            robTableModel.setValueAt(clockCycles, firstAvailableInRob, robIssueIndex);
            if(speculativeExecution)
                robTableModel.setValueAt(1, firstAvailableInRob,robSIndex);
            if(operator.equals("LD")){
                robTableModel.setValueAt(fpLdStResTable.getValueAt(firstAvailable,resIdIndex), 
                    firstAvailableInRob, robAddrIndex);
                flrTableModel.setValueAt(fpLdStResTable.getValueAt(firstAvailable, resIdIndex), opd1Index, flrTagIndex);
                flrTableModel.setValueAt(1, opd1Index, flrBusyIndex);
            }
            
            robEntryOrder.add(firstAvailableInRob);
        }
        
        return firstAvailableInRob;
    }
    
    public void instructionIssue(){
        int firstAvailable=-1;
        Integer robIndex;
        Boolean issueFlag;
        for(int i=flosTable.getRowCount()-1; i>=0; i--){
            Object operator= flosTable.getValueAt(i, flosOpIndex);
            issueFlag=false;
            if(operator.equals("ADD") || operator.equals("SUB")){
                firstAvailable= getFirstAvailableEntry(fpAddResTable);
                if(firstAvailable >=0){
                   robIndex= issueAddSubMul(firstAvailable, i, fpAddResTableModel, fpAddResTable);
                   adderIssueQueue.add(new Indices(firstAvailable, robIndex, 
                           (Integer)flosTable.getValueAt(i, flosPCIndex)));
                   issueFlag=true;
                }
            }else if(operator.equals("MUL")){
                firstAvailable= getFirstAvailableEntry(fpMulResTable);
                if(firstAvailable>=0){
                    robIndex= issueAddSubMul(firstAvailable, i, fpMulResTableModel, fpMulResTable);
                    mulIssueQueue.add(new Indices(firstAvailable, robIndex, 
                            Integer.parseInt(flosTable.getValueAt(i, flosPCIndex).toString())));
                    issueFlag=true;
                }
            }else if(operator.equals("BEQZ")){
                firstAvailable= getFirstAvailableEntry(fpAddResTable);
                if(firstAvailable>=0){
                    issueConditionalBranch(i, firstAvailable);
                    adderIssueQueue.add(new Indices(firstAvailable, -1, 
                            (Integer)flosTable.getValueAt(i, flosPCIndex)));
                    issueFlag=true;
                }
               
            }else if(operator.equals("LD") || operator.equals("SD")){
                firstAvailable= getFirstAvailableEntry(fpLdStResTable);
                if(firstAvailable>=0){
                    robIndex= issueLoadStore(i, firstAvailable);
                    ldStIssueQueue.add(new Indices(firstAvailable, robIndex,
                            (Integer)flosTable.getValueAt(i, flosPCIndex)));
                    issueFlag=true;
                }
                
            }else if(operator.equals("JMP")){
                issueUnconditionalBranch(i);
            }else if(operator.equals("HLT")){
                instructionCounter= instructionsCount;
                for(int j=i; j>=0; j--)
                    resetFlosRow(j);
            }
            if(issueFlag)
                resetFlosRow(i);
            else break;
        } 
    }
    
    public InstructionClass instructionDecode(String instruction){
        
        InstructionClass decodedInstruction= new InstructionClass();
        Object operator=mappedOperator.get(instruction.substring(0,3));
        Integer immediateBit= Integer.parseInt(instruction.substring(3,4));
        //System.out.println("immediate Bit:"+immediateBit);
        Object operand1=flrTable.getValueAt(Integer.parseInt(instruction.substring(4,8), 2), 0);
        Object operand2=flrTable.getValueAt(Integer.parseInt(instruction.substring(8,12), 2), 0);
        Object operand3=instruction.substring(12,16);
        
        if(operator.equals("ADD") || operator.equals("SUB")
                || operator.equals("MUL")){
            Integer immediateValue= Integer.parseInt((String)operand3,2);
            if(immediateBit == 1){
                if(((String)operand3).charAt(0)=='1')
                    immediateValue-=16;
                operand3= immediateValue;
            }else
                operand3= flrTable.getValueAt(Integer.parseInt((String)operand3, 2), 0);          
        
        }else if(operator.equals("LD") || operator.equals("SD")){
            Integer offset= Integer.parseInt((String)operand3,2);
            if(((String)operand3).charAt(0)=='1')
                offset -= 16;
            operand3= (offset<<1);
        
        }else if(operator.equals("JMP")){
            Integer jumpOffset= Integer.parseInt(instruction.substring(4,12),2);
            if(instruction.charAt(4)=='1')
                jumpOffset-=256;
            operand1= (jumpOffset<<1);
            operand2=""; operand3="";
        
        }else if(operator.equals("BEQZ")){
            Integer jumpOffset= Integer.parseInt(instruction.substring(8),2);
            if(instruction.charAt(8)=='1')
                jumpOffset-=256;
            operand2=(jumpOffset<<1);
            operand3="";
        }else if(operator.equals("HLT")){
            operand1=""; operand2="";
            operand3="";
        }else{
             operator=""; operand1="";
             operand2=""; operand3="";
        }
        
        decodedInstruction.setOperator(operator);
        decodedInstruction.setOperand1(operand1);
        decodedInstruction.setOperand2(operand2);
        decodedInstruction.setOperand3(operand3);
        return decodedInstruction;
    }
    
    public void bringInstructionsToFlosTable(int nInstructions){
        for(int i=nInstructions-1; i>=0 && instructionCounter < instructionsCount; i--){
            InstructionClass decodedInstruction= instructionDecode(instructionsCache.
                    get(instructionCounter));
            flosTableModel.setValueAt(programCounter, i, flosPCIndex);
            flosTableModel.setValueAt(decodedInstruction.getOperator(), i, flosOpIndex);
            flosTableModel.setValueAt(decodedInstruction.getOperand1(), i, flosOpd1Index);
            flosTableModel.setValueAt(decodedInstruction.getOperand2(), i, flosOpd2Index);
            flosTableModel.setValueAt(decodedInstruction.getOperand3(), i, flosOpd3Index);
            instructionCounter++;
            programCounter=(instructionCounter<<1);
        }
    }
    
    public void channelizeFlosInstructions(int byNRows){
        int nRows= flosTable.getRowCount()-1;
        int startRow= nRows- byNRows;
        int nColumns= flosTable.getColumnCount();
        for(int i= startRow; i>=0 && i< nRows; i--,nRows--){
            for(int j=0;j< nColumns; j++)
                flosTableModel.setValueAt(flosTable.getValueAt(i, j), nRows, j);
            resetFlosRow(i);
        }
    }
    
    public int findFlosSpace(){
        int spaces=0;
        for(int i=flosTable.getRowCount()-1; i>=0; i--){
            if(flosTable.getValueAt(i, flosPCIndex).equals(""))
                spaces++;
            else 
                break;
        }
        return spaces;
    }
    
    /*Algorithmic part ends here*/
    public void createTables(){
        flosTable= new JTable(); flosTableModel= new DefaultTableModel();
        flrTable= new JTable(); flrTableModel= new DefaultTableModel();
        robTable= new JTable(); robTableModel= new DefaultTableModel();
        fpAddResTable= new JTable(); fpAddResTableModel= new DefaultTableModel();
        fpAddResTable.setName("fpAddResTable");
        fpMulResTable= new JTable(); fpMulResTableModel= new DefaultTableModel();
        fpMulResTable.setName("fpMulResTable");
        fpLdStResTable= new JTable(); fpLdStResTableModel= new DefaultTableModel();
        fpLdStResTable.setName("fpLdStResTable");
        storeBufferTable= new JTable(); storeBufferTableModel= new DefaultTableModel();
        storeBufferTable.setName("storeBufferTable");
    }
    
    public void readInstructionFromFile(String fileName){
        File inFile= new File(fileName);
        instructionsCache = new ArrayList<>();
        try{
            BufferedReader reader= new BufferedReader(new FileReader(inFile));
            String instruction="";
            while((instruction= reader.readLine())!=null)
                instructionsCache.add(instruction);
        }catch(IOException ex){
            ex.printStackTrace();
        }
        instructionsCount= instructionsCache.size();
        maxPC= (instructionsCount <<1);
    }
    
    public void initializeParameters(){
        nEntriesRS.setText("3");
        ROBSize.setText("10");
        storeBuffer.setText("5");
        FPAddLatency.setText("1");
        FPMulLatency.setText("1");
        FPLoadLatency.setText("1");
    }
   
    public void setTableProperties(JTable table){
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setEnabled(false);
    }
    
    public void resizeColumns(){
        flosTable.getColumn("PC").setPreferredWidth(50);
        setTableProperties(flosTable);
        
        // resizing FlrTable
        flrTable.getColumn("Busy").setPreferredWidth(50);
        setTableProperties(flrTable);
        
        // RobTable
        Integer tableWidth= robTable.getPreferredSize().width;
        robTable.getColumn("Instruction").setPreferredWidth(220);
        robTable.getColumn("B").setPreferredWidth(30);
        robTable.getColumn("S").setPreferredWidth(30);
        robTable.getColumn("V").setPreferredWidth(30);
        
        setTableProperties(robTable);
        
        //ReservationStation
        fpAddResTable.getColumn("B").setPreferredWidth(30);
        fpAddResTable.getColumn("Id").setPreferredWidth(50);
        fpAddResTable.getColumn("Op").setPreferredWidth(100);
        
        fpMulResTable.getColumn("B").setPreferredWidth(30);
        fpMulResTable.getColumn("Id").setPreferredWidth(50);
        fpMulResTable.getColumn("Op").setPreferredWidth(100);
        
        fpLdStResTable.getColumn("B").setPreferredWidth(30);
        fpLdStResTable.getColumn("Id").setPreferredWidth(50);
        fpLdStResTable.getColumn("Op").setPreferredWidth(100);
        
        setTableProperties(fpAddResTable);
        setTableProperties(fpMulResTable);
        setTableProperties(fpLdStResTable);
        // StorBufferTable
        storeBufferTable.getTableHeader().setReorderingAllowed(false);
        storeBufferTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        
    }
    public Object[] getFlosColumnHeader(){
        return new Object[]{"PC","Op","Opd1","Opd2","Opd3"};
    }
    
    public Object[][] getFlosInitializationData(Integer nRows){
        Object [][] data= new Object[nRows][];
        for(int i=0;i< nRows; i++)
            data[i]= new Object[]{"","","","",""};
        return data;
    }
    
    public Object[] getFlrColumnHeader(){
        return new Object []{"Reg","Busy","Tag","Data"};
    }
    
    public Object[][] getFlrInitializationData(int nRows){
        Object [][] data= new Object[16][4];
        String reg="R";
        for(int i=0;i< nRows; i++)
            data[i]=new Object[]{"R"+i,"0","0","0"};
        return data;
    }
    
    public Object[] getRobColumnHeader(){
        return new Object[]{"Instruction","B","Issue","Exec","Comp",
        "Addr","S","V"};
    }
    
    public Object [][] getRobInitializationData(int nRows){
        Object [][] data= new Object [nRows][];
        for(int i=0;i< nRows; i++)
            data[i]= new Object []{"","0","","","","","0","0"};
        return data;
    }
    
    public Object[] getResColumnHeader(){
        return new Object[]{"Id","Op","B","Qj","Vj",
            "Qk","Vk","Addr"};
    }
    
    public Object[][] getResInitializationData(JTable table,int nRows){
        String idInitial="";
        if(table.getName().equals(fpAddResTable.getName()))
            idInitial="A";
        else if(table.getName().equals(fpMulResTable.getName()))
            idInitial="M";
        else if(table.getName().equals(fpLdStResTable.getName()))
            idInitial="L";
        
        Object [][]data= new Object [nRows][];
        for(int i=0;i< nRows; i++){
            data[i]= new Object[]{idInitial+i,"","0","0","","0","",""};
        }
        return data;
    }
    
    public Object[] getStoreBufferColumnHeader(){
        return new String[]{"Address","Data"};
    }
    
    public Object[][] getStoreBufferInitializationData(int nRows){
        Object [][]data= new Object [nRows][];
        for(int i=0;i< nRows; i++)
            data[i]= new Object[]{""+i,"0"};
        return data;
    }
    
    public void initializeFLOS(){
        Integer entries= Integer.parseInt(nEntriesRS.getText());
        Object [] columns = getFlosColumnHeader();
        Object [][] data= getFlosInitializationData(3*entries);
        
        flosTableModel.setDataVector(data, columns);
        flosTable.setModel(flosTableModel);
        
        // disables any sort of editing on the flrTable
        flosPanel.setLayout(new GridLayout());
        JScrollPane flosScrollPane= new JScrollPane();
        flosScrollPane.setViewportView(flosTable);
        flosScrollPane.setPreferredSize(flosPanel.getSize());
        flosPanel.add(flosScrollPane, BorderLayout.CENTER);
    }
   
    public void initializeFLR(){
        int nRows= 16;
        flrPanel.setLayout(new BorderLayout());
        Object []Columns= getFlrColumnHeader();
        Object [][]data= getFlrInitializationData(nRows);
        flrTableModel.setDataVector(data, Columns);
        
        flrTable.setModel(flrTableModel);
        JScrollPane flrScrollPane= new JScrollPane();
        flrScrollPane.setViewportView(flrTable);
        flrScrollPane.setPreferredSize(flrPanel.getSize());
        flrPanel.add(flrScrollPane, BorderLayout.CENTER);
    }
    
    public void initializeROB(){
        int nRows= Integer.parseInt(ROBSize.getText());
        Object [] columns= getRobColumnHeader();
        Object [][] data= getRobInitializationData(nRows);
        robTableModel.setDataVector(data, columns);
        
        ROBPanel.setLayout(new BorderLayout());
        robTable.setModel(robTableModel);
        JScrollPane robScrollPane= new JScrollPane();
        robScrollPane.setViewportView(robTable);
        robScrollPane.setPreferredSize(ROBPanel.getSize());
        ROBPanel.add(robScrollPane, BorderLayout.CENTER);
    }
        
    public void initializeReservationStation(JPanel panel, JTable table, 
            DefaultTableModel tableModel){
        panel.setLayout(new BorderLayout());
        Object [] columns= getResColumnHeader();
        Object [][]data= getResInitializationData(table,Integer.parseInt(nEntriesRS.getText()));
        tableModel.setDataVector(data, columns);
        panel.setLayout(new BorderLayout());
        table.setModel(tableModel);
        JScrollPane resScrollPane= new JScrollPane();
        resScrollPane.setViewportView(table);
        resScrollPane.setPreferredSize(panel.getSize());
        panel.add(resScrollPane, BorderLayout.CENTER);
    }
    
    public void initializeReservationStations(){
        initializeReservationStation(fpAdderReservationStationPanel, 
                fpAddResTable, fpAddResTableModel);
        initializeReservationStation(fpMulReservationStationPanel,
                fpMulResTable, fpMulResTableModel);
        initializeReservationStation(fpLdStrReservationStationPanel,
                fpLdStResTable, fpLdStResTableModel);
    }
    
    public void initializeStoreBuffer(){
        int nRows=Integer.parseInt(storeBuffer.getText());
        Object [] columns= getStoreBufferColumnHeader();
        Object [][]data= getStoreBufferInitializationData(nRows);
        
        storeBufferTableModel.setDataVector(data, columns);
        storeBufferPanel.setLayout(new BorderLayout());
        storeBufferTable= new JTable(storeBufferTableModel);
        // disables any sort of editing on the flrTable
        JScrollPane resScrollPane= new JScrollPane();
        resScrollPane.setViewportView(storeBufferTable);
        resScrollPane.setPreferredSize(storeBufferPanel.getSize());
        storeBufferPanel.add(resScrollPane, BorderLayout.CENTER);
        
    }
    
    public void updateTables(){
        //updating flosTable with new Rows and columns
        Integer uResRows= Integer.parseInt(nEntriesRS.getText());
        Integer uRobRows= Integer.parseInt(ROBSize.getText());
        Integer uStrBuffRows= Integer.parseInt(storeBuffer.getText());
        flosTableModel.setDataVector(getFlosInitializationData(4),
                getFlosColumnHeader());
        robTableModel.setDataVector(getRobInitializationData(uRobRows),
                getRobColumnHeader());
        fpAddResTableModel.setDataVector(getResInitializationData(fpAddResTable, uResRows),
                getResColumnHeader());
        fpMulResTableModel.setDataVector(getResInitializationData(fpMulResTable, uResRows), 
                getResColumnHeader());
        fpLdStResTableModel.setDataVector(getResInitializationData(fpLdStResTable, uResRows),
                getResColumnHeader());
        storeBufferTableModel.setDataVector(getStoreBufferInitializationData(uStrBuffRows), 
                getStoreBufferColumnHeader());
        resizeColumns();
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.JLabel nEntriesRSLabel = new javax.swing.JLabel();
        nEntriesRS = new javax.swing.JTextField();
        ROBSizeLabel = new javax.swing.JLabel();
        ROBSize = new javax.swing.JTextField();
        nstoreBufferLabel = new javax.swing.JLabel();
        storeBuffer = new javax.swing.JTextField();
        FPAddLatencyLabel = new javax.swing.JLabel();
        FPAddLatency = new javax.swing.JTextField();
        FPMulLatencyLabel = new javax.swing.JLabel();
        FPMulLatency = new javax.swing.JTextField();
        FPLoadLatencyLabel = new javax.swing.JLabel();
        FPLoadLatency = new javax.swing.JTextField();
        startButton = new javax.swing.JButton();
        stopButton = new javax.swing.JButton();
        resumeButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        resultsArea = new javax.swing.JTextArea();
        resultsLabel = new javax.swing.JLabel();
        dispatchQueueLabel = new javax.swing.JLabel();
        fpAdderReservationStationPanel = new javax.swing.JPanel();
        fpMulReservationStationPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        flosPanel = new javax.swing.JPanel();
        flrPanel = new javax.swing.JPanel();
        fpAdderFULabel = new javax.swing.JLabel();
        fpMulFULabel = new javax.swing.JLabel();
        fpAdderFU = new javax.swing.JTextField();
        fpMulFU = new javax.swing.JTextField();
        ROBPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        fpLdStrReservationStationPanel = new javax.swing.JPanel();
        storeLabel = new javax.swing.JLabel();
        loadLabel = new javax.swing.JLabel();
        storeBufferPanel = new javax.swing.JPanel();
        dataCachePanel = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        storeInstructionField = new javax.swing.JTextField();
        loadInstructionField = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        nEntriesRSLabel.setText("No RS Entries");

        ROBSizeLabel.setText("No ROB Entries");

        nstoreBufferLabel.setText("STR Buffer Size");

        FPAddLatencyLabel.setText("FP ADD Latency");

        FPMulLatencyLabel.setText("FP MUL Latency");

        FPLoadLatencyLabel.setText("FP LD Latency");

        startButton.setText("START");
        startButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startButtonActionPerformed(evt);
            }
        });

        stopButton.setText("STOP");
        stopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopButtonActionPerformed(evt);
            }
        });

        resumeButton.setText("RESUME");
        resumeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resumeButtonActionPerformed(evt);
            }
        });

        resultsArea.setEditable(false);
        resultsArea.setColumns(20);
        resultsArea.setRows(5);
        jScrollPane1.setViewportView(resultsArea);

        resultsLabel.setText("RESULTS");

        dispatchQueueLabel.setText("FLOS");

        fpAdderReservationStationPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        fpAdderReservationStationPanel.setName(""); // NOI18N

        javax.swing.GroupLayout fpAdderReservationStationPanelLayout = new javax.swing.GroupLayout(fpAdderReservationStationPanel);
        fpAdderReservationStationPanel.setLayout(fpAdderReservationStationPanelLayout);
        fpAdderReservationStationPanelLayout.setHorizontalGroup(
            fpAdderReservationStationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 302, Short.MAX_VALUE)
        );
        fpAdderReservationStationPanelLayout.setVerticalGroup(
            fpAdderReservationStationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 125, Short.MAX_VALUE)
        );

        fpMulReservationStationPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        fpMulReservationStationPanel.setName(""); // NOI18N

        javax.swing.GroupLayout fpMulReservationStationPanelLayout = new javax.swing.GroupLayout(fpMulReservationStationPanel);
        fpMulReservationStationPanel.setLayout(fpMulReservationStationPanelLayout);
        fpMulReservationStationPanelLayout.setHorizontalGroup(
            fpMulReservationStationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 305, Short.MAX_VALUE)
        );
        fpMulReservationStationPanelLayout.setVerticalGroup(
            fpMulReservationStationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 125, Short.MAX_VALUE)
        );

        jLabel1.setText("Architecture Registers");

        flosPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        javax.swing.GroupLayout flosPanelLayout = new javax.swing.GroupLayout(flosPanel);
        flosPanel.setLayout(flosPanelLayout);
        flosPanelLayout.setHorizontalGroup(
            flosPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 263, Short.MAX_VALUE)
        );
        flosPanelLayout.setVerticalGroup(
            flosPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 196, Short.MAX_VALUE)
        );

        flrPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        javax.swing.GroupLayout flrPanelLayout = new javax.swing.GroupLayout(flrPanel);
        flrPanel.setLayout(flrPanelLayout);
        flrPanelLayout.setHorizontalGroup(
            flrPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 228, Short.MAX_VALUE)
        );
        flrPanelLayout.setVerticalGroup(
            flrPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 196, Short.MAX_VALUE)
        );

        fpAdderFULabel.setText("FP Adder Unit");

        fpMulFULabel.setText("FP Multiplier Unit");

        fpAdderFU.setEditable(false);

        fpMulFU.setEditable(false);

        ROBPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        javax.swing.GroupLayout ROBPanelLayout = new javax.swing.GroupLayout(ROBPanel);
        ROBPanel.setLayout(ROBPanelLayout);
        ROBPanelLayout.setHorizontalGroup(
            ROBPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 421, Short.MAX_VALUE)
        );
        ROBPanelLayout.setVerticalGroup(
            ROBPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 198, Short.MAX_VALUE)
        );

        jLabel2.setText("Reorder Buffer");

        fpLdStrReservationStationPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        fpLdStrReservationStationPanel.setName(""); // NOI18N

        javax.swing.GroupLayout fpLdStrReservationStationPanelLayout = new javax.swing.GroupLayout(fpLdStrReservationStationPanel);
        fpLdStrReservationStationPanel.setLayout(fpLdStrReservationStationPanelLayout);
        fpLdStrReservationStationPanelLayout.setHorizontalGroup(
            fpLdStrReservationStationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 301, Short.MAX_VALUE)
        );
        fpLdStrReservationStationPanelLayout.setVerticalGroup(
            fpLdStrReservationStationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 125, Short.MAX_VALUE)
        );

        storeLabel.setText(" Store Unit");
        storeLabel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        loadLabel.setText("  Load Unit");
        loadLabel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        storeBufferPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        javax.swing.GroupLayout storeBufferPanelLayout = new javax.swing.GroupLayout(storeBufferPanel);
        storeBufferPanel.setLayout(storeBufferPanelLayout);
        storeBufferPanelLayout.setHorizontalGroup(
            storeBufferPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 176, Short.MAX_VALUE)
        );
        storeBufferPanelLayout.setVerticalGroup(
            storeBufferPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 99, Short.MAX_VALUE)
        );

        dataCachePanel.setBackground(java.awt.Color.lightGray);

        jLabel5.setText(" Data Cache");

        javax.swing.GroupLayout dataCachePanelLayout = new javax.swing.GroupLayout(dataCachePanel);
        dataCachePanel.setLayout(dataCachePanelLayout);
        dataCachePanelLayout.setHorizontalGroup(
            dataCachePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, dataCachePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, 96, Short.MAX_VALUE)
                .addContainerGap())
        );
        dataCachePanelLayout.setVerticalGroup(
            dataCachePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dataCachePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel5)
                .addContainerGap(51, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(resumeButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(stopButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(startButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(FPLoadLatencyLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(FPAddLatencyLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(nEntriesRSLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(ROBSizeLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(nstoreBufferLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(FPMulLatencyLabel, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addGap(4, 4, 4)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(nEntriesRS)
                                .addComponent(ROBSize)
                                .addComponent(FPMulLatency)
                                .addComponent(FPLoadLatency)
                                .addComponent(storeBuffer, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(FPAddLatency, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(8, 8, 8))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 187, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(84, 84, 84)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(fpAdderReservationStationPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(33, 33, 33)
                                .addComponent(fpMulReservationStationPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(54, 54, 54)
                                .addComponent(fpAdderFU, javax.swing.GroupLayout.PREFERRED_SIZE, 176, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(fpMulFU, javax.swing.GroupLayout.PREFERRED_SIZE, 176, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(89, 89, 89))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(73, 73, 73)
                                .addComponent(fpAdderFULabel, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(fpMulFULabel, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(136, 136, 136)))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(fpLdStrReservationStationPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(47, 47, 47))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(storeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(161, 161, 161)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(loadLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(loadInstructionField, javax.swing.GroupLayout.PREFERRED_SIZE, 114, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGap(16, 16, 16))))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(storeBufferPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(92, 92, 92)
                                        .addComponent(dataCachePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(storeInstructionField, javax.swing.GroupLayout.PREFERRED_SIZE, 122, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(0, 0, Short.MAX_VALUE))))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(102, 102, 102)
                        .addComponent(dispatchQueueLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(205, 205, 205)
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 177, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(185, 185, 185)
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(flosPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(63, 63, 63)
                        .addComponent(flrPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(ROBPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(82, 82, 82))
            .addGroup(layout.createSequentialGroup()
                .addGap(57, 57, 57)
                .addComponent(resultsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addGap(29, 29, 29)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(nEntriesRSLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(nEntriesRS, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(ROBSize, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(ROBSizeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(storeBuffer, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(nstoreBufferLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(FPAddLatencyLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(FPAddLatency)
                                .addGap(1, 1, 1)))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(FPMulLatency, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(FPMulLatencyLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(FPLoadLatencyLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(FPLoadLatency, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(32, 32, 32)
                        .addComponent(startButton, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(stopButton, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(resumeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(14, 14, 14)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(dispatchQueueLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGap(2, 2, 2)
                                .addComponent(flosPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(flrPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(ROBPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(fpMulReservationStationPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(fpLdStrReservationStationPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(fpAdderReservationStationPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(18, 18, 18)
                .addComponent(resultsLabel)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 2, Short.MAX_VALUE)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 202, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(34, 34, 34)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(fpAdderFULabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(fpMulFULabel, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(0, 0, 0)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(fpAdderFU, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(fpMulFU, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(storeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(loadLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(5, 5, 5)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(storeInstructionField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(loadInstructionField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(storeBufferPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(11, 11, 11)
                                        .addComponent(dataCachePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addGap(46, 46, 46))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void startButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startButtonActionPerformed
        System.out.println("instruction cache size: "+instructionsCache.size());
        dataCache= new HashMap<>();
        programCounter= 0;
        instructionCounter=0;
        adderIssueQueue= new ArrayList<>();
        mulIssueQueue= new ArrayList<>();
        ldStIssueQueue= new ArrayList<>();
        robEntryOrder= new ArrayList<>();
        fuLatency= new Latencies(Integer.parseInt(FPAddLatency.getText()),
                Integer.parseInt(FPMulLatency.getText()), Integer.parseInt(FPLoadLatency.getText()));
        tagValueMap = new HashMap<>();
        addExecutionCyclesCompleted=Integer.parseInt(FPAddLatency.getText());
        mulExecutionCyclesCompleted=Integer.parseInt(FPMulLatency.getText());;
        ldSdExecutionCyclesCompleted=Integer.parseInt(FPLoadLatency.getText());
        storeExecution=false;
        fpAdderFU.setText("");
        fpMulFU.setText("");
        loadInstructionField.setText("");
        storeInstructionField.setText("");
        
        CDB= new CommonDataBus();
        speculativeExecution=false;
        clockCycles=0;
        stalls=0;
        updateTables();
        resetFlrTable();
        for(int i=0;i<1024;i++)
            dataCache.put(i, 0);
        timer.start();
    }//GEN-LAST:event_startButtonActionPerformed

    private void stopButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopButtonActionPerformed
        timer.stop();
    }//GEN-LAST:event_stopButtonActionPerformed

    private void resumeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resumeButtonActionPerformed
        timer.restart();
    }//GEN-LAST:event_resumeButtonActionPerformed

    public void initializeColumnIndices(){
        flosPCIndex= flosTable.getColumn("PC").getModelIndex();
        flosOpIndex= flosTable.getColumn("Op").getModelIndex();
        flosOpd1Index= flosTable.getColumn("Opd1").getModelIndex();
        flosOpd2Index= flosTable.getColumn("Opd2").getModelIndex();
        flosOpd3Index= flosTable.getColumn("Opd3").getModelIndex();
        
        flrRegIndex= flrTable.getColumn("Reg").getModelIndex();
        flrBusyIndex= flrTable.getColumn("Busy").getModelIndex();
        flrTagIndex= flrTable.getColumn("Tag").getModelIndex();
        flrDataIndex= flrTable.getColumn("Data").getModelIndex();
        
        robInstructionIndex= robTable.getColumn("Instruction").getModelIndex();
        robBIndex= robTable.getColumn("B").getModelIndex();
        robIssueIndex= robTable.getColumn("Issue").getModelIndex();
        robExecIndex= robTable.getColumn("Exec").getModelIndex();
        robCompIndex= robTable.getColumn("Comp").getModelIndex();
        robAddrIndex= robTable.getColumn("Addr").getModelIndex();
        robSIndex= robTable.getColumn("S").getModelIndex();
        robVIndex= robTable.getColumn("V").getModelIndex();
        
        resIdIndex= fpAddResTable.getColumn("Id").getModelIndex();
        resBIndex= fpAddResTable.getColumn("B").getModelIndex();
        resOpIndex= fpAddResTable.getColumn("Op").getModelIndex();
        resQjIndex= fpAddResTable.getColumn("Qj").getModelIndex();
        resVjIndex= fpAddResTable.getColumn("Vj").getModelIndex();
        resQkIndex= fpAddResTable.getColumn("Qk").getModelIndex();
        resVkIndex= fpAddResTable.getColumn("Vk").getModelIndex();
        resAddrIndex= fpAddResTable.getColumn("Addr").getModelIndex();
        
        storeBufferAddressIndex= storeBufferTable.getColumn("Address").getModelIndex();
        storeBufferDataIndex= storeBufferTable.getColumn("Data").getModelIndex();
        
    }
    
    public void createOperatorsMapping(){
        mappedOperator= new HashMap<>();
        mappedOperator.put("000","ADD");
        mappedOperator.put("001","SUB");
        mappedOperator.put("010","MUL");
        mappedOperator.put("011","LD");
        mappedOperator.put("100","SD");
        mappedOperator.put("101","JMP");
        mappedOperator.put("110","BEQZ");
        mappedOperator.put("111","HLT");
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Tomasulo.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Tomasulo.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Tomasulo.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Tomasulo.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Tomasulo().setVisible(true);
            }
        });
    }

    @Override
    public void paint(Graphics g){
        Graphics2D g2d= (Graphics2D)g;
        super.paint(g);
        //super.repaint(1000);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setPaint(Color.BLUE);
        g2d.setStroke(new BasicStroke(7));
        
        float startX, startY;
        float endX, endY;
        float temp1, temp2, temp3;
        // drawing Common Data Bus below Functional Units
        startX= fpAdderReservationStationPanel.getX()-30;
        temp2= endX= dataCachePanel.getX()+ dataCachePanel.getWidth()+40;
        temp1=startY = endY = storeBufferPanel.getY()+ storeBufferPanel.getHeight()+70;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
       
        //drawing CDB on top of rservation Stations
        temp3= startY= endY= fpAdderReservationStationPanel.getY()-7;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
        
        // drawing CDB to the left of the FUs
        endX= startX;
        endY = temp1;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
        
        // drawing CDB to the right of the reservation Stations 
        startX= endX= temp2;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
        
        // drawing databus to fetch instruction from the FLOS to reservation stations
        g2d.setStroke(new BasicStroke(5));
        g2d.setColor(Color.getHSBColor(231, 191, 151));
        startX= fpAdderReservationStationPanel.getX()+ 50;
        temp1= endX = fpLdStrReservationStationPanel.getX()+ 80;
        temp2= startY= endY = temp3 - 17;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
        
        // drawing busses to connect reservation stations to previoulsy drawn 
        // instruction databusdatabus: 1. fpAddReservationStationPanel
        g2d.setStroke(new BasicStroke(3));
        endX= startX= startX- 1;
        endY= fpAdderReservationStationPanel.getY()+25;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
        
        // 2. fpMulReservationStationPanel 
        startX= endX= fpMulReservationStationPanel.getX()+50;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
        
        // 3. fpLdStrReservationStationPanel
        startX= endX= temp1+1;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY)); 
        
        // Connecting FLOS to instruction Bus
        startX= endX= flosPanel.getX()+ flosPanel.getWidth()/2;
        endY = temp2;
        startY= flosPanel.getY()+ flosPanel.getHeight()+27;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
        
         // connecting FLR to instrution BUS
        endX= startX = flrPanel.getX()+ flrPanel.getWidth()/2;
        startY= flrPanel.getY()+ flrPanel.getHeight()+27;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
 
         // connecting different units with the CDB
        g2d.setColor(Color.getHSBColor(192, 254, 145));
        
        // fpAddReservationStationPanel to CDB
        startX= endX= fpAdderReservationStationPanel.getX()+ 80;
        startY= fpAdderReservationStationPanel.getY()+25;
        endY = temp3+5;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
        
        // fpMulReservationStationPanel to CDB
        startX= endX= fpMulReservationStationPanel.getX()+ 80;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
        
        // fpLdStrReservationStationPanel to CDB
        startX= endX= fpLdStrReservationStationPanel.getX()+ 50;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
              
        // connecting ROB with CDB
        startX= endX = ROBPanel.getX()+ ROBPanel.getWidth()/2;
        startY= ROBPanel.getY()+ ROBPanel.getHeight()+27;
        endY = endY -10;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
        
        // fpAddReservationStationPanel to fpAddFu
        startY= fpAdderReservationStationPanel.getY()+ 
                fpAdderReservationStationPanel.getHeight()+27;
        endY = fpAdderFU.getY()+27;
        startX= endX= fpAdderReservationStationPanel.getX()+ 
                fpAdderReservationStationPanel.getWidth()/2+20;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
        
        //fpMulreservationStationPanel to fpMulFu
        startX= endX= fpMulReservationStationPanel.getX()+ 
                fpMulReservationStationPanel.getWidth()/2+20;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
        
        // fpAddFU result to CDB
        startX= endX = fpAdderFU.getX()+ fpAdderFU.getWidth()/2;
        startY= fpAdderFU.getY()+ fpAdderFU.getHeight()+25;
        endY = storeBufferPanel.getY()+ storeBufferPanel.getHeight()+65;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
        
        // fpMulFU result to CDB
        startX= endX= fpMulFU.getX()+ fpMulFU.getWidth()/2;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
        
        // Wiring for load and store operations
        g2d.setStroke(new BasicStroke(5));
        g2d.setColor(Color.black);
        startX= storeBufferPanel.getX()+ storeBufferPanel.getWidth()/2;
        endX= dataCachePanel.getX()+ dataCachePanel.getWidth()/2;
        endY = startY= storeBufferPanel.getY()+ storeBufferPanel.getHeight()+50;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
        
        // drawing databus to the top of store and load lables
        startY= endY = storeLabel.getY();
        temp1= startX= startX-15;
        temp2= endX =  endX-15;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
        
        g2d.setStroke(new BasicStroke(3));
        
        // connecting the fpLdStrReservationStationPanel with the LD/Str databus
        startX= endX= fpLdStrReservationStationPanel.getX()+ 
                fpLdStrReservationStationPanel.getWidth()/2;
        startY = fpLdStrReservationStationPanel.getY()+ 
                fpLdStrReservationStationPanel.getHeight()+27;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
        
        //wire from bus to storeLabel
        startX= endX= temp1-1;
        startY= storeLabel.getY()+25;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
         
        // wire from bus to the loadLabel
        startX= endX= temp2+1;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
        
        //connect load label with the store buffer
        startY = loadLabel.getY()+ loadLabel.getHeight()+27;
        endY = dataCachePanel.getY()+25;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
       
        // connect store label with the store buffer
        startX= endX= temp1-1;
        endY= storeBufferPanel.getY()+25;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
        
        // connect store Buffer with the databus
        endX= startX= storeBufferPanel.getX()+ storeBufferPanel.getWidth()/2 -1;
        startY= storeBufferPanel.getY()+ storeBufferPanel.getHeight()+27;
        endY = startY +20;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
        
        // connect datacache with the databus
        startX= endX= dataCachePanel.getX()+ dataCachePanel.getWidth()/2+1;
        startY= dataCachePanel.getY()+ dataCachePanel.getHeight()+27;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
        
        // connecting load Label with store Buffer
        startX= loadLabel.getX()+2;
        endX= startX - 120;
        startY = endY= loadLabel.getY()+ loadLabel.getHeight()+13;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
        
        startX= endX;
        endY= storeBufferPanel.getY()+25;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
        
        // connecting load Unit to CDB
        startX = loadLabel.getX()+ loadLabel.getWidth()+3;
        endX= startX+80;
        endY= startY;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
        
        // ROB to FLR
        g2d.setColor(Color.getHSBColor(112, 212, 192));
        startX= ROBPanel.getX()+2;
        endX= flrPanel.getX()+ flrPanel.getWidth()+3;
        startY = endY = ROBPanel.getY()+ ROBPanel.getHeight();
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
    }
    
    public JTable getFlosTable(){return flosTable;}
    public JTable getFlrTable(){return flrTable;}
    public JTable getRobTable(){return robTable;}
    public JTable getFpAddRessTable(){return fpAddResTable;}
    public JTable getFpMulResTable(){return fpMulResTable;}
    public JTable getLdSdResTable(){return fpLdStResTable;}
    public JTable getStoreBufferTable(){return storeBufferTable;}
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField FPAddLatency;
    private javax.swing.JLabel FPAddLatencyLabel;
    private javax.swing.JTextField FPLoadLatency;
    private javax.swing.JLabel FPLoadLatencyLabel;
    private javax.swing.JTextField FPMulLatency;
    private javax.swing.JLabel FPMulLatencyLabel;
    private javax.swing.JPanel ROBPanel;
    private javax.swing.JTextField ROBSize;
    private javax.swing.JLabel ROBSizeLabel;
    private javax.swing.JPanel dataCachePanel;
    private javax.swing.JLabel dispatchQueueLabel;
    private javax.swing.JPanel flosPanel;
    private javax.swing.JPanel flrPanel;
    private javax.swing.JTextField fpAdderFU;
    private javax.swing.JLabel fpAdderFULabel;
    private javax.swing.JPanel fpAdderReservationStationPanel;
    private javax.swing.JPanel fpLdStrReservationStationPanel;
    private javax.swing.JTextField fpMulFU;
    private javax.swing.JLabel fpMulFULabel;
    private javax.swing.JPanel fpMulReservationStationPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField loadInstructionField;
    private javax.swing.JLabel loadLabel;
    private javax.swing.JTextField nEntriesRS;
    private javax.swing.JLabel nstoreBufferLabel;
    private javax.swing.JTextArea resultsArea;
    private javax.swing.JLabel resultsLabel;
    private javax.swing.JButton resumeButton;
    private javax.swing.JButton startButton;
    private javax.swing.JButton stopButton;
    private javax.swing.JTextField storeBuffer;
    private javax.swing.JPanel storeBufferPanel;
    private javax.swing.JTextField storeInstructionField;
    private javax.swing.JLabel storeLabel;
    // End of variables declaration//GEN-END:variables
}

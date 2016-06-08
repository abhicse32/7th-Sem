/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package in.ac.iitm.cse;

import com.sun.jmx.snmp.BerDecoder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import javafx.util.Pair;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;

/**
 * @author abhisheky
 */
class InstructionsClass implements Cloneable{
    private String operator;
    private String operand1;
    private String operand2;
    private String operand3;
    private Boolean immediateBit;

    public InstructionsClass() {
        operator=""; operand1="";
        operand2=""; operand3="";
        immediateBit=false;
    }
    
    public InstructionsClass clone(){
        try{
            return (InstructionsClass)super.clone();
        }catch(CloneNotSupportedException ex){
            ex.printStackTrace();
            throw new RuntimeException();
        }
    }
    
    public void setOperator(String operator){this.operator= operator;}
    public String getOperator(){return operator;}
    public void setOperand1(String operand){this.operand1= operand;}
    public String getOperand1(){return operand1;}
    public void setOperand2(String operand){this.operand2= operand;}
    public String getOperand2(){return operand2;}
    public void setOperand3(String operand){this.operand3= operand;}
    public String getOperand3(){return operand3;}
    public void setImmediateBit(Boolean immediate){this.immediateBit= immediate;}
    public Boolean getImmediateBit(){return immediateBit;}
    
    public void reset(){
        this.operator=""; this.operand1="";
        this.operand2=""; this.operand3="";
        this.immediateBit=false;
    }
    
    public void copy(InstructionsClass source){
        this.operator= source.operator;
        this.operand1= source.operand1;
        this.operand2= source.operand2;
        this.operand3= source.operand3;
        this.immediateBit= source.immediateBit;
    }
}

public class PipelineWindow extends javax.swing.JFrame {

    /**
     * Creates new form PipelineWindow
     */
    private Timer timer;
    private ArrayList<String> instructionCache;
    private HashMap<Integer, Integer> dataCache;
    private Integer ProgramCounter;
    private String IFInstruction;
    private String IDInstruction;
    private HashMap<String,String> opCodes;
    private HashMap<String,String> rfRegisters;
    private Integer immediateOperand;
    private Boolean immediateBit;
    private Integer jumpLabel;
    private HashMap<String, JTextField> regFileDataFields;
    private int startWindow;
    private int endWindow;
    private char offsetSign;
    private InstructionsClass decodeStage;
    private InstructionsClass registerReadStage;
    private InstructionsClass executionStage;
    private InstructionsClass memoryOpStage;
    private InstructionsClass writeBackStage;
    private Integer regA,regB;
    private Integer ALUOutput;
    private Integer clockCycles;
    private Integer stallPeriod;
    private Integer stalls;
    private Boolean jmpFlag;
    private Boolean beqzFlag;
    private Integer instructionsCount;
    private Integer insExecuted;
    private ArrayList<String> stallReasons;
    
    public void resetInstructions(){
        decodeStage.reset();
        registerReadStage.reset();
        executionStage.reset();
        memoryOpStage.reset();
        writeBackStage.reset();
    }
    
    public void buildRegistersAndOpCodes(){
        String []localOpCodes=new String[]{"ADD","SUB","MUL","LD","SD",
            "JMP","BEQZ","HLT"};
        opCodes= new HashMap<>();
        rfRegisters= new HashMap<>();
        for(int i=0;i< localOpCodes.length;i++)
            opCodes.put(Integer.toBinaryString(0x10|i).substring(2), localOpCodes[i]);
        
        for(int i=0;i < 16;i++)
            rfRegisters.put(Integer.toBinaryString(0x10|i).substring(1),"R"+i);
    }
    
    public void buildInstructionCache(){
       File instructionFile= new File("test.txt");
       try{
           instructionCache= new ArrayList<>();
            BufferedReader fileReader= new BufferedReader(new FileReader(instructionFile));
            String instructions;
            while((instructions= fileReader.readLine())!=null)
                instructionCache.add(instructions);
            instructionsCount= instructionCache.size();
       }catch(IOException ex){
           ex.printStackTrace();
       }
    }
    public void initializeRegFile(){
        regFileDataFields= new HashMap<>();
        regFileDataFields.put("R0",R0Data);
        regFileDataFields.put("R1",R1Data);
        regFileDataFields.put("R2",R2Data);
        regFileDataFields.put("R3",R3Data);
        regFileDataFields.put("R4",R4Data);
        regFileDataFields.put("R5",R5Data);
        regFileDataFields.put("R6",R6Data);
        regFileDataFields.put("R7",R7Data);
        regFileDataFields.put("R8",R8Data);
        regFileDataFields.put("R9",R9Data);
        regFileDataFields.put("R10",R10Data);
        regFileDataFields.put("R11",R11Data);
        regFileDataFields.put("R12",R12Data);
        regFileDataFields.put("R13",R13Data);
        regFileDataFields.put("R14",R14Data);
        regFileDataFields.put("R15",R15Data);
        
        decodeStage=new InstructionsClass();
        registerReadStage= new InstructionsClass();
        executionStage= new InstructionsClass();
        memoryOpStage= new InstructionsClass();
        writeBackStage= new InstructionsClass();
    }
    
    public PipelineWindow() {
        //super.setResizable(false);
        initComponents();
        buildInstructionCache();
        InstructionCache.setLineWrap(true);
        InstructionCache.setWrapStyleWord(true);
        buildRegistersAndOpCodes();
        initializeRegFile();
        timer= new Timer(1000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {

                        if(stallPeriod <= 0  && startWindow <= instructionsCount){
                            if((ProgramCounter>>1) > instructionsCount)
                                printError("invalid PC number");
                            clockCycles++;
                            String instruction="";
                            for(int j=(6+startWindow-endWindow); j>=0 ;j--)
                                instruction+='\n';
                            
                           // System.out.println(endWindow+"  "+startWindow);
                            for(int i=endWindow; i > startWindow;i--)
                                instruction+=instructionCache.get(i)+"\n";
                            
                            InstructionCache.setText(instruction);
                            if(startWindow >= instructionsCount)
                                IFInstruction="";
                            else IFInstruction=instructionCache.get(startWindow);
                            insExecuted++;
                            startWindow++; endWindow++;
                            ProgramCounter+=2;
                            showPipelineState();
                            if(endWindow >= instructionsCount)
                                endWindow--;
                         }else{
                             clockCycles++;
                             stallPeriod--;
                             showPipelineState();
                        }
                }
            });
    }
    
     public void showPipelineState(){
        if(beqzFlag)
            handleControlHazard();
        else if(jmpFlag)
            jumpStall();
        if(stallPeriod<=0){
            PCField.setText(ProgramCounter.toString());
            InstructionFetch.setText(IFInstruction);
            performInstructionDecode();
        }
        //System.out.println(IDInstruction);
        performRegisterRead();
        performExecution();
        performMemoryOp();
        performWriteBackData();
        if(stallPeriod==0){
            checkForDataHazard();
            if(stallPeriod == 0){
                IDInstruction="";
                if(IFInstruction.length()>=16)
                    IDInstruction= IFInstruction;
            }else
                stallReasons.add("RAW");
        }
        moveInstructions();
        checkForTermination();
        
    }
    
    public void moveInstructions(){
        writeBackStage.copy(memoryOpStage);
        memoryOpStage.copy(executionStage);
        executionStage.copy(registerReadStage);
        if(stallPeriod==0)
            registerReadStage.copy(decodeStage);
        
        if(stallPeriod==4)
            registerReadStage.reset();
        else if(stallPeriod==3)
            executionStage.reset();
        else if(stallPeriod==2)
            memoryOpStage.reset();
        else if(stallPeriod==1)
            writeBackStage.reset();
    }
    
    public void printError(String ErrorMessage){
        Results.setText("\n Error: "+ErrorMessage);
        timer.stop();
    }
    
    public void checkForDataHazard(){
        stallPeriod=0;
        if(!decodeStage.getOperator().equals("")){
            String operand2= decodeStage.getOperand2();
            String operand3= decodeStage.getOperand3();
            if(operand2.equals(registerReadStage.getOperand1()) || 
                    operand3.equals(registerReadStage.getOperand1()))
                stallPeriod=4;
            else if(operand2.equals(executionStage.getOperand1()) ||
                    operand3.equals(executionStage.getOperand1()))
                stallPeriod=3;
            else if(operand2.equals(memoryOpStage.getOperand1()) ||
                    operand3.equals(memoryOpStage.getOperand1()))
                stallPeriod=2;
            else if(operand2.equals(writeBackStage.getOperand1()) ||
                    operand3.equals(writeBackStage.getOperand1()))
                stallPeriod=1;
        }
    }
    
    public void handleControlHazard(){
        stalls++;
        InstructionDecode.setText(""); decodeStage.reset();
        RegisterRead.setText(""); registerReadStage.reset();
        InstructionFetch.setText("");
        beqzFlag=false;
   }
    
   public void jumpStall(){
       stalls++;
       decodeStage.reset();
       InstructionDecode.setText("");
       InstructionFetch.setText("");
       jmpFlag=false;
   }
    
   public void checkForTermination(){
       if(IFInstruction.equals("") && decodeStage.getOperator().equals("") &&
               registerReadStage.getOperator().equals("") && executionStage.
               getOperator().equals("") && memoryOpStage.getOperator().equals("")
               && writeBackStage.getOperator().equals("")){
           
           Results.append("Final Results:\n");
           showResults();
           timer.stop();
           }
   }
   
   public void showResults(){
       String CPI="CPI: "+clockCycles+"/"+insExecuted+" = "+
               ((float)clockCycles)/insExecuted;
       String nStalls="Stalls: "+stalls;
       String reasons="Reasons:";
       Iterator<String> iterator= stallReasons.iterator();
       while(iterator.hasNext())
           reasons=reasons+"\n"+iterator.next();
       Results.append(CPI+"\n"+nStalls+"\n"+reasons);
   }
   
   public void executeJump(Integer PCOffset){
       int decPC=4;
       if(beqzFlag)
           decPC=6;
       ProgramCounter=ProgramCounter-decPC+(PCOffset);
       startWindow= ProgramCounter>>1;
       endWindow= startWindow+6;
       if(endWindow >= instructionsCount)
           endWindow= instructionsCount-1;
   }
   
   public void performInstructionDecode(){
        InstructionDecode.setText("");
        if(!IDInstruction.equals("")){
            String operator= opCodes.get(IDInstruction.substring(0, 3));
            immediateBit= false;
            if(IDInstruction.charAt(3)=='1')
                immediateBit=true;
            
            String LSBString=IDInstruction.substring(12, 16);
            String operand1= rfRegisters.get(IDInstruction.substring(4, 8));
            String operand2= rfRegisters.get(IDInstruction.substring(8, 12));
            String operand3= rfRegisters.get(LSBString);
            decodeStage.setOperator(operator);
            decodeStage.setOperand1(operand1);
            decodeStage.setOperand2(operand2);
            decodeStage.setOperand3(operand3);
            decodeStage.setImmediateBit(immediateBit);
            immediateOperand= Integer.parseInt(LSBString, 2);
            if(LSBString.charAt(0)=='1')
                    immediateOperand-= 16;
            
            if(operator.equals("ADD") || operator.equals("SUB")||
                    operator.equals("MUL")){
                    String thirdOperand= decodeStage.getOperand3();
                    if(immediateBit){
                        decodeStage.setOperand3(String.valueOf(immediateOperand));
                        thirdOperand = "#"+ immediateOperand;
                    }
                    
                    InstructionDecode.setText(operator+"  "+operand1+",  "+
                        operand2+",  "+thirdOperand);
                    
            }else if(operator.equals("LD")){
                
                decodeStage.setOperand3(String.valueOf(immediateOperand));
                InstructionDecode.setText(operator+"  "+operand1+",  ["+operand2+"]");
            
            }else if(operator.equals("SD")){
                decodeStage.setOperand3(String.valueOf(immediateOperand));
                InstructionDecode.setText(operator+"  ["+operand1+"],  "+operand2);
                     
            }else if(operator.equals("BEQZ")){
                LSBString= IDInstruction.substring(8, 16);
                jumpLabel= Integer.parseInt(LSBString,2);
                if(LSBString.charAt(0)=='1')
                    jumpLabel-=256;
                decodeStage.setOperand2(String.valueOf(jumpLabel<<1));
                InstructionDecode.setText(operator+"  ("+operand1+"),  "+
                        "[PC + ("+decodeStage.getOperand2()+")]");
            }

            else if(operator.equals("JMP")){
                LSBString= IDInstruction.substring(4,12);
                jumpLabel= Integer.parseInt(LSBString,2);
                if(LSBString.charAt(0)=='1')
                    jumpLabel-=256;
                
                InstructionDecode.setText(operator+"   [PC + ("+
                      decodeStage.getOperand1()+")]");
                jmpFlag= true;
                executeJump(jumpLabel<<1);
            }else if(operator.equals("HLT")){
                InstructionDecode.setText(operator);
                showResults();
                timer.stop();
            }
        }else 
            decodeStage.reset();
    }

    public void performRegisterRead(){
       RegisterRead.setText("");
       String Operator= registerReadStage.getOperator();
//        System.out.println(Operator+" "+registerReadStage.getOperand1()+"  "+ registerReadStage.getOperand2()+
//                " "+registerReadStage.getOperand3());
       if(!Operator.equals("")){
           if(Operator.equals("ADD") || Operator.equals("SUB")
                   || Operator.equals("MUL")){
                regA=Integer.parseInt(regFileDataFields.get(registerReadStage.getOperand2()).getText());
                String readDataB;
                if(!registerReadStage.getImmediateBit()){
                    regB= Integer.parseInt(regFileDataFields.get(registerReadStage.getOperand3()).getText());
                    readDataB="B <- regF[ "+registerReadStage.getOperand3()+
                            " ] : "+regB;
                }
                else{
                    regB= Integer.parseInt(registerReadStage.getOperand3());
                    readDataB="B :  "+regB;
                }
                
                String readDataA= "A <- regF[ "+registerReadStage.getOperand2()+
                        " ] : "+ regA;
                registerReadStage.setOperand2(String.valueOf(regA));
                registerReadStage.setOperand3(String.valueOf(regB));
                RegisterRead.setText(Operator+":\n "+ readDataA+"\n "+readDataB);
          
           }else if(Operator.equals("BEQZ")){
               regA=Integer.parseInt(regFileDataFields.get(registerReadStage.getOperand1()).getText());
               RegisterRead.setText("BEQZ:\n A <- regFile[ "+registerReadStage.getOperand1()+
                       " ] : "+regA);
               if(regA==0){
                   Integer offset= Integer.parseInt(registerReadStage.getOperand2()); 
                   beqzFlag= true;
                   executeJump(offset);
               }
           }else if(Operator.equals("LD")){
               regA=Integer.parseInt(regFileDataFields.get(registerReadStage.getOperand2()).getText());
               RegisterRead.setText("LD:\n A <- [ "+registerReadStage.getOperand2()+
                       " ] : "+regA);
               registerReadStage.setOperand2(String.valueOf(regA));
           }
            else if(Operator.equals("SD")){
               regA= Integer.parseInt(regFileDataFields.get(registerReadStage.getOperand1()).getText());
               regB= Integer.parseInt(regFileDataFields.get(registerReadStage.getOperand2()).getText());
               RegisterRead.setText("SD:\n A <- [ "+registerReadStage.getOperand1()+
                       " ] : "+regA+"\n "+"B  : "+regB);
               registerReadStage.setOperand1(String.valueOf(regA));
               registerReadStage.setOperand2(String.valueOf(regB));
            }
       }else registerReadStage.reset();
    }
    
    public void performExecution(){
        Execution.setText("");
        String operator= executionStage.getOperator();
        if(!operator.equals("")){
            if(operator.equals("ADD")){
                regA= Integer.parseInt(executionStage.getOperand2());
                regB= Integer.parseInt(executionStage.getOperand3());
                ALUOutput= regA+ regB;
                Execution.setText("ADD:\n  ALUOut <-  "+regA+" + "+regB+" : "+ALUOutput);
           }
            else if(operator.equals("SUB")){
                regA= Integer.parseInt(executionStage.getOperand2());
                regB= Integer.parseInt(executionStage.getOperand3());
                ALUOutput= regA - regB;
                Execution.setText("SUB:\n  ALUOut <-  "+regA+" - ("+regB+") : "+ALUOutput);
            }
            
            else if(operator.equals("MUL")){
                regA= Integer.parseInt(executionStage.getOperand2());
                regB= Integer.parseInt(executionStage.getOperand3());
                ALUOutput= regA * regB;
                Execution.setText("MUL:\n  ALUOut <-  "+regA+" * "+regB+" : "+ALUOutput);
            }
            
            else if(operator.equals("LD")){
                // actual offset
                Integer offset= Integer.parseInt(executionStage.getOperand3());
                regA= Integer.parseInt(executionStage.getOperand2());
                ALUOutput= regA; //+ offset;
                Execution.setText("LD:\n  ALUOut <- [ "+regA+" ]"/* + ( "+
                        offset+" )*/+" : "+ALUOutput);
            }else if(operator.equals("SD")){
                Integer offset= Integer.parseInt(executionStage.getOperand3());
                regA= Integer.parseInt(executionStage.getOperand1());
                ALUOutput= regA; //+ offset;
                Execution.setText("SD:\n  ALUOutput <- [ "+regA+" ]"/*+ ( "+
                        offset+" )*/+" : "+ALUOutput);
            }
            executionStage.setOperand3(String.valueOf(ALUOutput));
        }else
            executionStage.reset();
    }
    
    public void performMemoryOp(){
        MemoryAccess.setText("");
        String decodedOperator= memoryOpStage.getOperator();
        if(!decodedOperator.equals("")){
            if(decodedOperator.equals("LD")){
                MemoryAccess.setText("LMD <- Mem[ 0x"+
                        memoryOpStage.getOperand3()+" ]");
                Integer address= Integer.parseInt(memoryOpStage.getOperand3());
                if(address< 0){
                    //printError("reading from negative address");
                    address*=-1;
                }
                memoryOpStage.setOperand3(String.valueOf(dataCache.get(address)));
                
            }else if(decodedOperator.equals("SD")){
                Integer address= Integer.parseInt(memoryOpStage.getOperand3());
                
                MemoryAccess.setText("Mem[ 0x"+address+" ] <-  "+
                        memoryOpStage.getOperand2());
                if(address< 0){
                    //printError("Writing to negative address");
                    address*=-1;
                }
                dataCache.put(address, Integer.parseInt(memoryOpStage.getOperand2()));
            }
        }else memoryOpStage.reset();
    }
    
    public void performWriteBackData(){
        String Operator= writeBackStage.getOperator();
        WriteBack.setText("");
        if(!Operator.equals("")){
            if(Operator.equals("ADD") || Operator.equals("SUB") || 
                       Operator.equals("MUL") || Operator.equals("LD")){
                WriteBack.setText("rf[ "+writeBackStage.getOperand1()+" ] <-  "+
                        writeBackStage.getOperand3());
                Integer address= Integer.parseInt(regFileDataFields.get(writeBackStage.
                        getOperand1()).getText());
                if(address < 0)
                    address*=-1;
                dataCache.put(address,Integer.parseInt(writeBackStage.getOperand3()));
                regFileDataFields.get(writeBackStage.getOperand1()).
                        setText(writeBackStage.getOperand3());
            }
        }else writeBackStage.reset();
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        InstructionCache = new javax.swing.JTextArea();
        InstructionFetch = new javax.swing.JTextField();
        InstructionDecode = new javax.swing.JTextField();
        MemoryAccess = new javax.swing.JTextField();
        WriteBack = new javax.swing.JTextField();
        PCLabel = new javax.swing.JLabel();
        PCField = new javax.swing.JTextField();
        IDLabel = new javax.swing.JLabel();
        IFLabel = new javax.swing.JLabel();
        RDLabel = new javax.swing.JLabel();
        ALULabel = new javax.swing.JLabel();
        MEM = new javax.swing.JLabel();
        WBLabel = new javax.swing.JLabel();
        instructionCacheLabel = new javax.swing.JLabel();
        Start = new javax.swing.JButton();
        StopButton = new javax.swing.JButton();
        ResumeButton = new javax.swing.JButton();
        RegFile = new javax.swing.JPanel();
        R0 = new javax.swing.JLabel();
        R0Data = new javax.swing.JTextField();
        R1 = new javax.swing.JLabel();
        R1Data = new javax.swing.JTextField();
        R2 = new javax.swing.JLabel();
        R2Data = new javax.swing.JTextField();
        R3 = new javax.swing.JLabel();
        R3Data = new javax.swing.JTextField();
        R4 = new javax.swing.JLabel();
        R4Data = new javax.swing.JTextField();
        R5 = new javax.swing.JLabel();
        R5Data = new javax.swing.JTextField();
        R6 = new javax.swing.JLabel();
        R6Data = new javax.swing.JTextField();
        R7 = new javax.swing.JLabel();
        R7Data = new javax.swing.JTextField();
        R8Data = new javax.swing.JTextField();
        R9Data = new javax.swing.JTextField();
        R11 = new javax.swing.JLabel();
        R9 = new javax.swing.JLabel();
        R10 = new javax.swing.JLabel();
        R11Data = new javax.swing.JTextField();
        R10Data = new javax.swing.JTextField();
        R8 = new javax.swing.JLabel();
        R12 = new javax.swing.JLabel();
        R12Data = new javax.swing.JTextField();
        R13 = new javax.swing.JLabel();
        R13Data = new javax.swing.JTextField();
        R14 = new javax.swing.JLabel();
        R14Data = new javax.swing.JTextField();
        R15 = new javax.swing.JLabel();
        R15Data = new javax.swing.JTextField();
        RegisterFileLabel = new javax.swing.JLabel();
        DataCache = new javax.swing.JPanel();
        DataCacheLabel = new java.awt.Label();
        jScrollPane2 = new javax.swing.JScrollPane();
        RegisterRead = new javax.swing.JTextArea();
        jScrollPane3 = new javax.swing.JScrollPane();
        Execution = new javax.swing.JTextArea();
        jScrollPane4 = new javax.swing.JScrollPane();
        Results = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setPreferredSize(new java.awt.Dimension(750, 700));
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        InstructionCache.setBackground(javax.swing.UIManager.getDefaults().getColor("InternalFrame.borderColor"));
        InstructionCache.setColumns(20);
        InstructionCache.setLineWrap(true);
        InstructionCache.setRows(6);
        InstructionCache.setWrapStyleWord(true);
        jScrollPane1.setViewportView(InstructionCache);

        getContentPane().add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 40, 169, 133));

        InstructionFetch.setEditable(false);
        getContentPane().add(InstructionFetch, new org.netbeans.lib.awtextra.AbsoluteConstraints(471, 130, 220, 32));

        InstructionDecode.setEditable(false);
        getContentPane().add(InstructionDecode, new org.netbeans.lib.awtextra.AbsoluteConstraints(471, 190, 220, 30));

        MemoryAccess.setEditable(false);
        MemoryAccess.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MemoryAccessActionPerformed(evt);
            }
        });
        getContentPane().add(MemoryAccess, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 410, 220, 32));

        WriteBack.setEditable(false);
        getContentPane().add(WriteBack, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 470, 220, 32));

        PCLabel.setText("PC");
        getContentPane().add(PCLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 130, 22, 25));

        PCField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PCFieldActionPerformed(evt);
            }
        });
        getContentPane().add(PCField, new org.netbeans.lib.awtextra.AbsoluteConstraints(21, 130, 40, 25));

        IDLabel.setText("ID");
        getContentPane().add(IDLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(700, 190, 20, 32));

        IFLabel.setText("IF");
        getContentPane().add(IFLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(700, 130, 20, 32));

        RDLabel.setText("RD");
        getContentPane().add(RDLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(700, 260, 20, 32));

        ALULabel.setText("ALU");
        getContentPane().add(ALULabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(700, 340, 30, 32));

        MEM.setText("MEM");
        getContentPane().add(MEM, new org.netbeans.lib.awtextra.AbsoluteConstraints(700, 410, -1, 32));

        WBLabel.setText("WB");
        getContentPane().add(WBLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(700, 470, -1, 32));

        instructionCacheLabel.setText("Instruction Cache");
        getContentPane().add(instructionCacheLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 10, 142, 29));

        Start.setText("Start");
        Start.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StartActionPerformed(evt);
            }
        });
        getContentPane().add(Start, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 190, -1, 37));

        StopButton.setActionCommand("Stop");
        StopButton.setLabel("Stop");
        StopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StopButtonActionPerformed(evt);
            }
        });
        getContentPane().add(StopButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 190, 70, 37));

        ResumeButton.setText("Resume");
        ResumeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ResumeButtonActionPerformed(evt);
            }
        });
        getContentPane().add(ResumeButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(180, 190, 92, 37));

        RegFile.setBackground(new java.awt.Color(166, 203, 220));
        RegFile.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        R0.setText("R0");

        R0Data.setEditable(false);
        R0Data.setText("0");

        R1.setText("R1");

        R1Data.setEditable(false);

        R2.setText("R2");

        R2Data.setEditable(false);

        R3.setText("R3");

        R3Data.setEditable(false);

        R4.setText("R4");

        R4Data.setEditable(false);

        R5.setText("R5");

        R5Data.setEditable(false);

        R6.setText("R6");

        R6Data.setEditable(false);

        R7.setText("R7");

        R7Data.setEditable(false);

        R8Data.setEditable(false);

        R9Data.setEditable(false);

        R11.setText("R11");

        R9.setText("R9");

        R10.setText("R10");

        R11Data.setEditable(false);

        R10Data.setEditable(false);

        R8.setText("R8");

        R12.setText("R12");

        R12Data.setEditable(false);

        R13.setText("R13");

        R13Data.setEditable(false);

        R14.setText("R14");

        R14Data.setEditable(false);

        R15.setText("R15");

        R15Data.setEditable(false);

        RegisterFileLabel.setText("RegisterFile");

        javax.swing.GroupLayout RegFileLayout = new javax.swing.GroupLayout(RegFile);
        RegFile.setLayout(RegFileLayout);
        RegFileLayout.setHorizontalGroup(
            RegFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(RegFileLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(RegFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(RegFileLayout.createSequentialGroup()
                        .addComponent(R0, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(2, 2, 2)
                        .addComponent(R0Data, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(10, 10, 10)
                        .addComponent(R1, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(R1Data, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(10, 10, 10)
                        .addComponent(R2, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(R2Data, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(8, 8, 8)
                        .addComponent(R3, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(R3Data, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(RegFileLayout.createSequentialGroup()
                        .addComponent(R4, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(2, 2, 2)
                        .addComponent(R4Data, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(10, 10, 10)
                        .addComponent(R5, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(R5Data, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(10, 10, 10)
                        .addComponent(R6, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(R6Data, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(8, 8, 8)
                        .addComponent(R7, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(R7Data, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(RegFileLayout.createSequentialGroup()
                        .addComponent(R8, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(2, 2, 2)
                        .addComponent(R8Data, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(10, 10, 10)
                        .addComponent(R9, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(R9Data, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(10, 10, 10)
                        .addComponent(R10, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(R10Data, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(8, 8, 8)
                        .addComponent(R11, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(R11Data, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(RegFileLayout.createSequentialGroup()
                        .addComponent(R12, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(2, 2, 2)
                        .addComponent(R12Data, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(10, 10, 10)
                        .addComponent(R13, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(R13Data, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(10, 10, 10)
                        .addComponent(R14, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(R14Data, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(8, 8, 8)
                        .addComponent(R15, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(R15Data, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, RegFileLayout.createSequentialGroup()
                .addGap(95, 95, 95)
                .addComponent(RegisterFileLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 107, Short.MAX_VALUE)
                .addGap(99, 99, 99))
        );
        RegFileLayout.setVerticalGroup(
            RegFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(RegFileLayout.createSequentialGroup()
                .addComponent(RegisterFileLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 20, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(RegFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(R0, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(R0Data, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(R1, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(R1Data, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(R2, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(R2Data, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(R3, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(R3Data, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(RegFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(R4, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(R4Data, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(R5, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(R5Data, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(R6, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(R6Data, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(R7, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(R7Data, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(RegFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(R8, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(R8Data, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(R9, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(R9Data, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(R10, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(R10Data, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(R11, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(R11Data, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(RegFileLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(R12, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(R12Data, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(R13, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(R13Data, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(R14, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(R14Data, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(R15, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(R15Data, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(2, 2, 2))
        );

        getContentPane().add(RegFile, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 250, 303, 180));

        DataCache.setBackground(new java.awt.Color(199, 199, 220));
        DataCache.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        DataCacheLabel.setText("DataCache");

        javax.swing.GroupLayout DataCacheLayout = new javax.swing.GroupLayout(DataCache);
        DataCache.setLayout(DataCacheLayout);
        DataCacheLayout.setHorizontalGroup(
            DataCacheLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(DataCacheLayout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addComponent(DataCacheLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(34, Short.MAX_VALUE))
        );
        DataCacheLayout.setVerticalGroup(
            DataCacheLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(DataCacheLayout.createSequentialGroup()
                .addGap(38, 38, 38)
                .addComponent(DataCacheLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(51, Short.MAX_VALUE))
        );

        getContentPane().add(DataCache, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 530, 140, 110));

        RegisterRead.setEditable(false);
        RegisterRead.setColumns(20);
        RegisterRead.setLineWrap(true);
        RegisterRead.setRows(5);
        RegisterRead.setWrapStyleWord(true);
        jScrollPane2.setViewportView(RegisterRead);

        getContentPane().add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 240, 220, 60));

        Execution.setEditable(false);
        Execution.setColumns(20);
        Execution.setLineWrap(true);
        Execution.setRows(5);
        Execution.setWrapStyleWord(true);
        jScrollPane3.setViewportView(Execution);

        getContentPane().add(jScrollPane3, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 320, 220, 70));

        Results.setEditable(false);
        Results.setColumns(20);
        Results.setLineWrap(true);
        Results.setRows(5);
        Results.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jScrollPane4.setViewportView(Results);

        getContentPane().add(jScrollPane4, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 460, 300, 200));

        jLabel1.setText("Results");
        getContentPane().add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 440, 60, 20));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void PCFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PCFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_PCFieldActionPerformed

    public void paint(Graphics g){
        super.paint(g);
        drawDatBuses(g);
    }
      
    public void createTestFile(String fileName) {
        File outFile= new File(fileName);
        String instruction;
        Random randGenerator= new Random();
        try {
            if(!outFile.exists())
                outFile.createNewFile();
            BufferedWriter writer= new BufferedWriter(new FileWriter(outFile));
            for(int i=0;i<4000;i++){
                instruction="";
                for(int j=0; j< 16; j++)
                    instruction+=randGenerator.nextInt(2);
                writer.write(instruction+"\n");
            }
            writer.close();
        } catch (IOException error) {
            error.printStackTrace();
        }
    }
    
    private void StartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StartActionPerformed
        // TODO add your handling code here:    
            IFInstruction=""; IDInstruction= "";
            InstructionDecode.setText("");InstructionFetch.setText("");
            RegisterRead.setText("");Execution.setText("");
            MemoryAccess.setText("");WriteBack.setText("");
            Results.setText("");
            jmpFlag=false; beqzFlag= false;
            
            resetInstructions();
            for(Map.Entry<String, JTextField>entry: regFileDataFields.entrySet())
                entry.getValue().setText("0");
                
            ProgramCounter=0;
            clockCycles=0; stallPeriod=0;
            insExecuted=0;
            stalls=0;
            stallReasons= new ArrayList<>();
            startWindow=0;endWindow=6;
            if(endWindow >= instructionsCount)
                endWindow= instructionsCount-1;
            
            dataCache = new HashMap<>();
            for(int i=0;i<1024;i++)
                dataCache.put(i,0);
           
            timer.start();
    }//GEN-LAST:event_StartActionPerformed

    private void ResumeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ResumeButtonActionPerformed
        // TODO add your handling code here:
        timer.restart();
    }//GEN-LAST:event_ResumeButtonActionPerformed

    private void StopButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StopButtonActionPerformed
        // TODO add your handling code here:
        timer.stop();
    }//GEN-LAST:event_StopButtonActionPerformed

    private void MemoryAccessActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MemoryAccessActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_MemoryAccessActionPerformed

    public void drawDatBuses(Graphics g){
        Graphics2D g2D= (Graphics2D)g;
        Stroke stroke= new BasicStroke(7,BasicStroke.CAP_SQUARE,BasicStroke.CAP_ROUND);
        g2D.setStroke(stroke);
        g2D.setColor(Color.GRAY);
        g2D.drawString("WP", 259.0f, 96.0f);
        g2D.draw(new Line2D.Float(253.4f, 101.50f, 390.6f, 101.50f));
        g2D.drawString("RP", 259.0f, 166.0f);
        g2D.draw(new Line2D.Float(253.4f, 171.50f, 474.50f, 171.50f));
        g2D.draw(new Line2D.Float(64.45f, 171.50f, 82.5f, 171.50f));
        g2D.draw(new Line2D.Float(395.5f, 80.0f, 395.f, 650.0f));
        g2D.drawString("RP1", 328.0f, 316.0f);
        g2D.draw(new Line2D.Float(318.1f, 320.0f, 390.6f, 320.0f));
        g2D.drawString("RP2", 328.0f, 346.0f);
        g2D.draw(new Line2D.Float(318.1f, 350.0f, 390.6f, 350.0f));
        g2D.drawString("WP", 328.0f, 436.0f);
        g2D.draw(new Line2D.Float(318.1f, 440.0f, 390.6f, 440.0f));
        g2D.drawString("RP", 450.0f, 566.0f);
        g2D.draw(new Line2D.Float(398.1f, 570.0f, 469.6f, 570.0f));
        g2D.drawString("WP", 450.0f, 630.0f);
        g2D.draw(new Line2D.Float(398.1f, 636.0f, 469.6f, 636.0f));
        
        // databus for pipeline Registers
        g2D.draw(new Line2D.Float(398.1f, 289.0f, 471.6f, 289.0f)); // databus for RD/EX reg
        g2D.draw(new Line2D.Float(398.1f, 452.0f, 471.6f, 452.0f)); // databus for MEM/WB reg
        g2D.draw(new Line2D.Float(398.1f, 510.0f, 471.6f, 510.0f));
        
    }
   
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (intrx oduced in Java SE 6) is not available, stay with the default look and feel.
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
            java.util.logging.Logger.getLogger(PipelineWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(PipelineWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(PipelineWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(PipelineWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                PipelineWindow pipelineWindow= new PipelineWindow();
                //pipelineWindow.createTestFile("test.txt");
                pipelineWindow.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel ALULabel;
    private javax.swing.JPanel DataCache;
    private java.awt.Label DataCacheLabel;
    private javax.swing.JTextArea Execution;
    private javax.swing.JLabel IDLabel;
    private javax.swing.JLabel IFLabel;
    private javax.swing.JTextArea InstructionCache;
    private javax.swing.JTextField InstructionDecode;
    private javax.swing.JTextField InstructionFetch;
    private javax.swing.JLabel MEM;
    private javax.swing.JTextField MemoryAccess;
    private javax.swing.JTextField PCField;
    private javax.swing.JLabel PCLabel;
    private javax.swing.JLabel R0;
    private javax.swing.JTextField R0Data;
    private javax.swing.JLabel R1;
    private javax.swing.JLabel R10;
    private javax.swing.JTextField R10Data;
    private javax.swing.JLabel R11;
    private javax.swing.JTextField R11Data;
    private javax.swing.JLabel R12;
    private javax.swing.JTextField R12Data;
    private javax.swing.JLabel R13;
    private javax.swing.JTextField R13Data;
    private javax.swing.JLabel R14;
    private javax.swing.JTextField R14Data;
    private javax.swing.JLabel R15;
    private javax.swing.JTextField R15Data;
    private javax.swing.JTextField R1Data;
    private javax.swing.JLabel R2;
    private javax.swing.JTextField R2Data;
    private javax.swing.JLabel R3;
    private javax.swing.JTextField R3Data;
    private javax.swing.JLabel R4;
    private javax.swing.JTextField R4Data;
    private javax.swing.JLabel R5;
    private javax.swing.JTextField R5Data;
    private javax.swing.JLabel R6;
    private javax.swing.JTextField R6Data;
    private javax.swing.JLabel R7;
    private javax.swing.JTextField R7Data;
    private javax.swing.JLabel R8;
    private javax.swing.JTextField R8Data;
    private javax.swing.JLabel R9;
    private javax.swing.JTextField R9Data;
    private javax.swing.JLabel RDLabel;
    private javax.swing.JPanel RegFile;
    private javax.swing.JLabel RegisterFileLabel;
    private javax.swing.JTextArea RegisterRead;
    private javax.swing.JTextArea Results;
    private javax.swing.JButton ResumeButton;
    private javax.swing.JButton Start;
    private javax.swing.JButton StopButton;
    private javax.swing.JLabel WBLabel;
    private javax.swing.JTextField WriteBack;
    private javax.swing.JLabel instructionCacheLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    // End of variables declaration//GEN-END:variables
}

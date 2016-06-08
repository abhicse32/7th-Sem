/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cse.iitm;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
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
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

/**
 *
 * @author abhisheky
 */
class Request{
    private String name;
    private Integer address;
    private String readWrite;
    
    public Request(){
        requestReset();
    }
    
    public Request(String name, Integer address, String readWrite){
        this.name= name;
        this.address= address;
        this.readWrite= readWrite;
    }
    public void setName(String name){this.name= name;}
    public String getName(){return name;}
    public void setAddress(Integer address){this.address= address;}
    public Integer getAddress(){return address;}
    public void setReadWrite(String readWrite){this.readWrite= readWrite;}
    public String getReadWrite(){return readWrite;}
    public void requestReset(){
        address=-1; name="";
        readWrite="";
    }
}

public class ProcessorCacheWindow extends javax.swing.JFrame {

    /**
     * Creates new form ProcessorCacheWindow
     */
    private JTable p1CacheTable, p2CacheTable;
    private DefaultTableModel p1CacheTableModel, p2CacheTableModel;
    private Timer timer;
    private ArrayList<Request> requests;
    private Integer requestCounter;
    private Integer requestCount;
    private Request busRequest;
    private Request requestVar;
    private Boolean protocolBit;
    private Integer tag, block;
    private Integer memBlockIndex,mIndex, sIndex,iIndex,eIndex;
    private Integer iFrequency, mFrequency, sFrequency, eFrequency;
    private Integer nBusPlacedRequests;
    private JTable table, tempTable;
    private DefaultTableModel tempTableModel, tableModel;
    
    /*
        Since main memory is considered to be of 32KB, hence
        15 bits  will be required to address all the bytes
    */
    
    public ProcessorCacheWindow() {
        initComponents();
        super.setResizable(false);
        initializeTables();
        initializeP1CacheTable();
        initializeP2CacheTable();
        resizeCacheColumnsWhenMSI();
        readRequestsFromFile();
        initializeIndices();
        protocolBit=false;
        
        timer= new Timer(10, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(requestCounter < requestCount){
                    generateRequest();
                }else{
                    timer.stop();
                    showResults();
                }
            }
        });
    }

    public void showResults(){
        resultsArea.setText("Invalid State:"+iFrequency+"\n");
        resultsArea.append("Shared State:"+sFrequency+"\n");
        resultsArea.append("Modified State:"+mFrequency+"\n");
        resultsArea.append("Exclusive State:"+eFrequency+"\n");
        resultsArea.append("Bus-placed requests:"+nBusPlacedRequests);
    }
    
    public void generateRequest(){
        requestVar= requests.get(requestCounter);
        busRequest=null;
        resolveAddress(requestVar);
        requestCounter++;
    }
    
    public void getTagAndBlock(Integer address){
        address= (address>>5);
        block= address;
        int temp= address>>5;
        temp= temp<<5;
        tag= (address-temp);
    }
    
    public void resolveAddress(Request request){
        String processor= request.getName();
        getTagAndBlock(request.getAddress());
        if(processor.equals("P1")){
            table= p1CacheTable;
            tempTable= p2CacheTable;
            tableModel= p1CacheTableModel;
            tempTableModel= p2CacheTableModel;
        }else{
            table= p2CacheTable;
            tempTable= p1CacheTable;
            tableModel= p2CacheTableModel;
            tempTableModel= p1CacheTableModel;
        }
        if(!protocolBit)
            handleProcessorRequest(request);
        else handleProcessorRequestWhenMESI(request);
    }
    
    public void handleProcessorRequest(Request request){
        
        if(table.getValueAt(tag, iIndex).equals(1)){
            tableModel.setValueAt(0, tag, iIndex);
            busRequest= request;
            if(request.getReadWrite().equals("W")){
                tableModel.setValueAt(1, tag, mIndex);
                mFrequency++;
            }else if(request.getReadWrite().equals("R")){
                 if(protocolBit && !tempTable.getValueAt(tag, memBlockIndex).equals(block)){
                    tableModel.setValueAt(1, tag, eIndex);
                    eFrequency++;
                }else{
                    tableModel.setValueAt(1, tag, sIndex);
                    sFrequency++;
                }
            }
            tableModel.setValueAt(block, tag, memBlockIndex);
            
        }else if(table.getValueAt(tag, sIndex).equals(1)){
                
                if(request.getReadWrite().equals("W")){
                    mFrequency++;
                    tableModel.setValueAt(1, tag, mIndex);
                    tableModel.setValueAt(0, tag, sIndex);
                    busRequest= request;
                    if(table.getValueAt(tag, memBlockIndex).equals(block))
                         busRequest.setReadWrite("I");
                       
                }else if(request.getReadWrite().equals("R") && !table.getValueAt(tag, memBlockIndex).equals(block))
                    busRequest= request;
                tableModel.setValueAt(block, tag, memBlockIndex);
                
        }else if(table.getValueAt(tag, mIndex).equals(1)){

            if(!table.getValueAt(tag, memBlockIndex).equals(block)){
                busRequest= request;
                if(request.getReadWrite().equals("R")){
                    sFrequency++;
                    tableModel.setValueAt(1, tag, sIndex);
                    tableModel.setValueAt(0, tag, mIndex);
                }
            }
            tableModel.setValueAt(block, tag, memBlockIndex);
        }
        if(busRequest!=null){
            handleBusRequestsWhenMSI(tempTable, tempTableModel);
        }
    }
    
    public void handleBusRequestsWhenMSI(JTable ftable, DefaultTableModel ftableModel){
        if(ftable.getValueAt(tag, memBlockIndex).equals(block)){
            if(ftable.getValueAt(tag, sIndex).equals(1) && (busRequest.getReadWrite().equals("W") 
                    || busRequest.getReadWrite().equals("I"))){
                    iFrequency++;
                    ftableModel.setValueAt(0, tag, sIndex);
                    ftableModel.setValueAt(1, tag, iIndex);
                    ftableModel.setValueAt(-1, tag, memBlockIndex);            
            }else if(ftable.getValueAt(tag, mIndex).equals(1)){
                if(busRequest.getReadWrite().equals("W")){
                    iFrequency++;
                    ftableModel.setValueAt(0,tag, mIndex);
                    ftableModel.setValueAt(1,tag, iIndex);
                    ftableModel.setValueAt(-1, tag, memBlockIndex);
                }else if(busRequest.getReadWrite().equals("R")){
                    sFrequency++;
                    ftableModel.setValueAt(0, tag, mIndex);
                    ftableModel.setValueAt(1, tag, sIndex);
                }
            }
        }
        if(!protocolBit)
            nBusPlacedRequests++;
    }
    
    public void handleProcessorRequestWhenMESI(Request request){
     
            if(table.getValueAt(tag, eIndex).equals(1)){
                if(request.getReadWrite().equals("W")){
                    mFrequency++;
                    tableModel.setValueAt(0, tag, eIndex);
                    tableModel.setValueAt(1, tag, mIndex);
                    if(!table.getValueAt(tag, memBlockIndex).equals(block)){
                        busRequest= request;
                    }
                }else if(request.getReadWrite().equals("R") && !table.getValueAt(tag, memBlockIndex).equals(block)){
                        sFrequency++;
                        tableModel.setValueAt(1, tag, sIndex);
                        tableModel.setValueAt(0, tag, eIndex);
                        busRequest= request;
                }
                tableModel.setValueAt(block, tag, memBlockIndex);
            }else
                 handleProcessorRequest(request);
            
            if(busRequest!=null)
                handleBusRequestsWhenMESI(tempTable, tempTableModel);
    }
    
    public void handleBusRequestsWhenMESI(JTable ftable, DefaultTableModel ftableModel){
        if(protocolBit)
            nBusPlacedRequests++;
        if(ftable.getValueAt(tag, eIndex).equals(1)){
            eFrequency++;
            if(busRequest.getReadWrite().equals("R")){
                sFrequency++;
                ftableModel.setValueAt(1, tag, sIndex);
                ftableModel.setValueAt(0, tag, eIndex);
            }else if(busRequest.getReadWrite().equals("W")){
                iFrequency++;
                ftableModel.setValueAt(0, tag, eIndex);
                ftableModel.setValueAt(1, tag, iIndex);
                ftableModel.setValueAt(-1, tag, memBlockIndex);
            }
        }else handleBusRequestsWhenMSI(ftable, ftableModel);
    }
    
    public void readRequestsFromFile(){
        requests= new ArrayList<>();
        File readFile= new File("private_testcases/Prtest_2.txt");
        try{
            BufferedReader reader= new BufferedReader(new FileReader(readFile));
            String str;
            String[] tokens;
            while((str=reader.readLine())!=null){
                tokens= str.split(" ");
                requests.add(new Request(tokens[0],Integer.parseInt(tokens[1]), tokens[2]));
            }
            reader.close();
            requestCount= requests.size();
        }catch(IOException ex){
            ex.printStackTrace();
        }
    }
    public void initializeTables(){
        p1CacheTable= new JTable();
        p2CacheTable= new JTable();
        p1CacheTableModel= new DefaultTableModel();
        p2CacheTableModel= new DefaultTableModel();
    }
    
    public void initializeIndices(){
        tag= p1CacheTable.getColumn("CBlock").getModelIndex();
        memBlockIndex= p1CacheTable.getColumn("MemBlock").getModelIndex();
        mIndex= p1CacheTable.getColumn("M").getModelIndex();
        sIndex= p1CacheTable.getColumn("S").getModelIndex();
        iIndex= p1CacheTable.getColumn("I").getModelIndex();
    }
    
    public Object[]getMSICacheColumnHeader(){
        return new Object[]{"CBlock","MemBlock","M","S","I"};
    }
    
    public Object[]getMESICacheColumnHeader(){
        return new Object[]{"CBlock","MemBlock","M","E","S","I"};
    }
    
    public Object[][] getMSICacheInitializationData(int entries){
        Object[][]data= new Object[entries][];
        for(int i=0;i< entries; i++){
            data[i]= new Object[]{i,-1,0,0,1};
        }
        return data;
    }
    
    public Object[][] getMESICacheInitializationData(int entries){
        Object[][]data= new Object[entries][];
        for(int i=0;i< entries; i++){
            data[i]= new Object[]{i,-1,0,0,0,1};
        }
        return data;
    }
    
    public void resizeCacheColumnsWhenMESI(){
        p1CacheTable.getColumn("E").setPreferredWidth(20);
        p2CacheTable.getColumn("E").setPreferredWidth(20);
        resizeCacheColumnsWhenMSI();
        
    }
    
    public void resizeCacheColumnsWhenMSI(){
        p1CacheTable.getColumn("M").setPreferredWidth(20);
        p1CacheTable.getColumn("S").setPreferredWidth(20);
        p1CacheTable.getColumn("I").setPreferredWidth(20);
        p1CacheTable.getColumn("CBlock").setPreferredWidth(40);

        p2CacheTable.getColumn("M").setPreferredWidth(20);
        p2CacheTable.getColumn("S").setPreferredWidth(20);
        p2CacheTable.getColumn("I").setPreferredWidth(20);
        p2CacheTable.getColumn("CBlock").setPreferredWidth(40);
        p1CacheTable.getTableHeader().setReorderingAllowed(false);
        p1CacheTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        p1CacheTable.setEnabled(false);
        
        p2CacheTable.getTableHeader().setReorderingAllowed(false);
        p2CacheTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        p2CacheTable.setEnabled(false);
    }
    
    public void initializeP1CacheTable(){
        Integer entries= 32;
        Object [] columns = getMSICacheColumnHeader();
        Object [][] data= getMSICacheInitializationData(entries);
        
        p1CacheTableModel.setDataVector(data, columns);
        p1CacheTable.setModel(p1CacheTableModel);
        p1CachePanel.setLayout(new GridLayout());
        JScrollPane p1CacheScrollPane= new JScrollPane();
        p1CacheScrollPane.setViewportView(p1CacheTable);
        p1CacheScrollPane.setPreferredSize(p1CachePanel.getSize());
        p1CachePanel.add(p1CacheScrollPane, BorderLayout.CENTER);
    }
    
    public void initializeP2CacheTable(){
        Integer entries= 32;
        Object [] columns= getMSICacheColumnHeader();
        Object [][] data= getMSICacheInitializationData(entries);
        p2CacheTableModel.setDataVector(data, columns);
        p2CacheTable.setModel(p2CacheTableModel);
        p2CachePanel.setLayout(new GridLayout());
        JScrollPane p2CacheScrollPane= new JScrollPane();
        p2CacheScrollPane.setViewportView(p2CacheTable);
        p2CacheScrollPane.setPreferredSize(p2CachePanel.getSize());
        p2CachePanel.add(p2CacheScrollPane,BorderLayout.CENTER);
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        processor1Label = new javax.swing.JLabel();
        processor2Label = new javax.swing.JLabel();
        p1CachePanel = new javax.swing.JPanel();
        p2CachePanel = new javax.swing.JPanel();
        sharedCachePanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        resultsArea = new javax.swing.JTextArea();
        MSIButton = new javax.swing.JButton();
        STARTButton = new javax.swing.JButton();
        MESIButton = new javax.swing.JButton();
        STOPButton = new javax.swing.JButton();
        resumeButton = new javax.swing.JButton();
        nextButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        processor1Label.setText("      Processor1");
        processor1Label.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        processor1Label.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        processor2Label.setText("      Processor2");
        processor2Label.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        processor2Label.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        p1CachePanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        javax.swing.GroupLayout p1CachePanelLayout = new javax.swing.GroupLayout(p1CachePanel);
        p1CachePanel.setLayout(p1CachePanelLayout);
        p1CachePanelLayout.setHorizontalGroup(
            p1CachePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 266, Short.MAX_VALUE)
        );
        p1CachePanelLayout.setVerticalGroup(
            p1CachePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 227, Short.MAX_VALUE)
        );

        p2CachePanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        javax.swing.GroupLayout p2CachePanelLayout = new javax.swing.GroupLayout(p2CachePanel);
        p2CachePanel.setLayout(p2CachePanelLayout);
        p2CachePanelLayout.setHorizontalGroup(
            p2CachePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 264, Short.MAX_VALUE)
        );
        p2CachePanelLayout.setVerticalGroup(
            p2CachePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        sharedCachePanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        javax.swing.GroupLayout sharedCachePanelLayout = new javax.swing.GroupLayout(sharedCachePanel);
        sharedCachePanel.setLayout(sharedCachePanelLayout);
        sharedCachePanelLayout.setHorizontalGroup(
            sharedCachePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 209, Short.MAX_VALUE)
        );
        sharedCachePanelLayout.setVerticalGroup(
            sharedCachePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        resultsArea.setEditable(false);
        resultsArea.setColumns(20);
        resultsArea.setRows(5);
        jScrollPane1.setViewportView(resultsArea);

        MSIButton.setText("MSI");
        MSIButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MSIButtonActionPerformed(evt);
            }
        });

        STARTButton.setText("START");
        STARTButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                STARTButtonActionPerformed(evt);
            }
        });

        MESIButton.setText("MESI");
        MESIButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MESIButtonActionPerformed(evt);
            }
        });

        STOPButton.setText("STOP");
        STOPButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                STOPButtonActionPerformed(evt);
            }
        });

        resumeButton.setText("RESUME");
        resumeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resumeButtonActionPerformed(evt);
            }
        });

        nextButton.setText("NEXT");
        nextButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(159, 159, 159)
                .addComponent(processor1Label, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(processor2Label, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(120, 120, 120))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(72, 72, 72)
                .addComponent(p1CachePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(p2CachePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(47, 47, 47))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(33, 33, 33)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 169, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(67, 67, 67)
                .addComponent(sharedCachePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 12, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(nextButton, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(MSIButton, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(resumeButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(STOPButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(STARTButton, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addComponent(MESIButton, javax.swing.GroupLayout.DEFAULT_SIZE, 74, Short.MAX_VALUE))))
                .addGap(69, 69, 69))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(36, 36, 36)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(processor1Label, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(processor2Label, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(41, 41, 41)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(p1CachePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(p2CachePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 77, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(sharedCachePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 134, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(8, 8, 8)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(MSIButton, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(nextButton, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(9, 9, 9)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(MESIButton, javax.swing.GroupLayout.DEFAULT_SIZE, 37, Short.MAX_VALUE)
                            .addComponent(resumeButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(STARTButton, javax.swing.GroupLayout.DEFAULT_SIZE, 36, Short.MAX_VALUE)
                            .addComponent(STOPButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addGap(24, 24, 24))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void MSIButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MSIButtonActionPerformed
        // TODO add your handling code here:
        Object[] columns= getMSICacheColumnHeader();
        Object[][] data= getMSICacheInitializationData(32);
        p1CacheTableModel.setDataVector(data, columns);
        p2CacheTableModel.setDataVector(data, columns);
        resizeCacheColumnsWhenMSI();
        initializeIndices();
        protocolBit= false;
    }//GEN-LAST:event_MSIButtonActionPerformed

    private void STARTButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_STARTButtonActionPerformed
        // TODO add your handling code here:
        requestCounter=0;
        nBusPlacedRequests=0;
        iFrequency=mFrequency=sFrequency= eFrequency=0;
        timer.start();
    }//GEN-LAST:event_STARTButtonActionPerformed

    private void MESIButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MESIButtonActionPerformed
        // TODO add your handling code here:
        Object[] columns= getMESICacheColumnHeader();
        Object[][] data= getMESICacheInitializationData(32);
        p2CacheTableModel.setDataVector(data, columns);
        p1CacheTableModel.setDataVector(data, columns);
        resizeCacheColumnsWhenMESI();
        initializeIndices();
        eIndex= p1CacheTable.getColumn("E").getModelIndex();
        protocolBit=true;
    }//GEN-LAST:event_MESIButtonActionPerformed

    private void STOPButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_STOPButtonActionPerformed
        // TODO add your handling code here:
        timer.stop();
    }//GEN-LAST:event_STOPButtonActionPerformed

    private void nextButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextButtonActionPerformed
        // TODO add your handling code here:
        if(requestCounter < requestCount)
              generateRequest();
        else showResults();
    }//GEN-LAST:event_nextButtonActionPerformed

    private void resumeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resumeButtonActionPerformed
        // TODO add your handling code here:
        timer.restart();
    }//GEN-LAST:event_resumeButtonActionPerformed

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
        float temp1;
        startX=endX= processor1Label.getX()+ processor1Label.getWidth()/2;
        startY= processor1Label.getY()+ processor1Label.getHeight()+29;
        endY= p1CachePanel.getY()+23;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
        
        startX=endX= processor2Label.getX()+ processor2Label.getWidth()/2;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
        
        g2d.setStroke(new BasicStroke(15));
        startX= p1CachePanel.getX()+ p1CachePanel.getWidth()/2;
        temp1=endX= p2CachePanel.getX()+ p2CachePanel.getWidth()/2;
        startY = endY = p1CachePanel.getY()+ p1CachePanel.getHeight()+70;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
        
        g2d.setStroke(new BasicStroke(7));
        g2d.setColor(Color.blue);
        endX= startX= startX-4;
        endY= startY - 42;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
        startX= endX= temp1+4;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
        startX= endX= sharedCachePanel.getX()+sharedCachePanel.getWidth()/2;
        endY = sharedCachePanel.getY()+23;
        g2d.draw(new Line2D.Double(startX, startY, endX, endY));
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
            java.util.logging.Logger.getLogger(ProcessorCacheWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ProcessorCacheWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ProcessorCacheWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ProcessorCacheWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ProcessorCacheWindow().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton MESIButton;
    private javax.swing.JButton MSIButton;
    private javax.swing.JButton STARTButton;
    private javax.swing.JButton STOPButton;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton nextButton;
    private javax.swing.JPanel p1CachePanel;
    private javax.swing.JPanel p2CachePanel;
    private javax.swing.JLabel processor1Label;
    private javax.swing.JLabel processor2Label;
    private javax.swing.JTextArea resultsArea;
    private javax.swing.JButton resumeButton;
    private javax.swing.JPanel sharedCachePanel;
    // End of variables declaration//GEN-END:variables
}

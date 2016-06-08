/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package in.ac.iitm.cse;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Label;
import java.awt.geom.AffineTransform;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOError;
import java.io.IOException;
import java.util.Random;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
/**
 *
 * @author abhisheky
 */
public class PipelinedProcessor extends JFrame{

    /**
     * @param args the command line arguments
     */
    
    public void readInstruction(String instructionMemory){
        File instructions= new File(instructionMemory);
        try {
            BufferedReader instructionCache= new BufferedReader(new FileReader(instructions));
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    public PipelinedProcessor(){
        super("vertical String");
        setSize(new Dimension(400, 400));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }
    
    public void paint(Graphics g){
        super.paint(g);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.setColor(Color.BLUE);
        
        Graphics2D g2D= (Graphics2D)g;
        AffineTransform defaultAt= g2D.getTransform();
        AffineTransform at= new AffineTransform();
        at.rotate(-Math.PI/2);
        g2D.setTransform(at);
        g2D.drawString("1000101010101101", -250,50);
        g2D.drawString("1001111010101001", -250,67);
        g2D.setTransform(defaultAt);         
    }
//    public static void main(String[] args) {
//        // TODO code application logic here
//        SwingUtilities.invokeLater(new Runnable() {
//
//            @Override
//            public void run() {
//                new PipelinedProcessor().setVisible(true);
//            }
//        });
//        PipelinedProcessor testCreator= new PipelinedProcessor();
//        //testCreator.createTestFile("test.txt");
//       
//    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cse.iitm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOError;
import java.io.IOException;
import java.util.Random;

/**
 *
 * @author abhisheky
 */
public class CohenceProtocol {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        File outFile =new File("requestFile.txt");
        String[] array=new String[]{"R","W"};
        int processor, address, wOrR;
        try{
            BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
            Random rand= new Random();
            for(int i=0;i< 10000 ; i++){
                processor= 1+rand.nextInt(2);
                address= rand.nextInt(32768);
                wOrR= rand.nextInt(2);
                writer.write("P"+processor+" "+address+" "+array[wOrR]+"\n");
                //System.out.println(i+ " P"+processor+" "+address+" "+array[wOrR]+"\n");
            }
        }catch(IOException ex){
            ex.printStackTrace();
        }
    }
    
}

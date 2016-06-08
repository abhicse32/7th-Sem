package in.ac.iitm.cse;


import java.util.HashMap;
import java.util.Map;
import javax.sound.midi.SysexMessage;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author abhisheky
 */
class temp implements Cloneable{
    String str1;
    String str2;
    public temp clone(){
        try{
           return (temp)super.clone();
        }catch(CloneNotSupportedException ex){
            ex.printStackTrace();
            throw new RuntimeException();
        }
    }
}
public class testClass {
    public static void main(String[] args){
        temp t= new temp();
        t.str1="this";
         t.str2="nothign";
        temp t2= t.clone();
        t.str1="change";
        System.out.println(t.str1+" "+t.str2);
        System.out.println(t2.str1+" "+t2.str2);
    }
}

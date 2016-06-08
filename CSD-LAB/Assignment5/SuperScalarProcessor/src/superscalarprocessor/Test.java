package superscalarprocessor;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.*;

public class Test{
public static void main (String args[]){
    ArrayList<Integer> list= new ArrayList<>();
    for(int i=0;i<= 12;i++)
        list.add(i);
    Iterator<Integer> iterator= list.iterator();
    while(iterator.hasNext()){
        if(iterator.next()%3==0)
            iterator.remove();
    }
    
    iterator= list.iterator();
    while(iterator.hasNext())
        System.out.println(iterator.next());
}
}
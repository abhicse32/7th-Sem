/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package superscalarprocessor;

/**
 *
 * @author abhisheky
 */
public class InstructionClass {
    private Object operator;
    private Object operand1;
    private Object operand2;
    private Object operand3;
    
    public InstructionClass(){
        reset();
    }
    public void setOperator(Object operator){this.operator= operator;}
    public Object getOperator(){return operator;}
    public void setOperand1(Object operand){operand1= operand;}
    public Object getOperand1(){return operand1;}
    public void setOperand2(Object operand){operand2= operand;}
    public Object getOperand2(){return operand2;}
    public void setOperand3(Object operand){operand3= operand;}
    public Object getOperand3(){return operand3;}
    
    public void reset(){
        operator=""; operand1="";
        operand2=""; operand3="";
    }
}

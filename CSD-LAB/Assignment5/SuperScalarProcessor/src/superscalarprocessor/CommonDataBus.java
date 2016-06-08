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
class Result{
    private Object data;
    private Object tag;
    private Integer robIndex;
    private Boolean valid;
    
    public Result(){
        resetCDB();
    }
    public void setTag(Object tag){
        this.tag= tag;
    }
    public Object getTag(){
        return tag;
    }
    public void setData(Object data){
        this.data = data;
    }
    public Object getData(){
        return data;
    }
    public void setRobIndex(Integer robIndex){
        this.robIndex= robIndex;
    }
    public Integer getRobIndex(){
        return robIndex;
    }
    public Boolean getValid(){
        return valid;
    }
    
    public void setValid(Boolean valid){
        this.valid= valid;
    }
    public void resetCDB(){
        data=""; tag=""; robIndex=-10;
        valid=false;
    }
}

public class CommonDataBus {
    public Result addFUResult;
    public Result mulFUResult;
    public Result ldSdFUResult;

    public CommonDataBus(){
        addFUResult= new Result();
        mulFUResult= new Result();
        ldSdFUResult= new Result();
    }
}

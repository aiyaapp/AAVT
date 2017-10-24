package com.wuwang.aavt.mediacmd;

/*
 * Created by Wuwang on 2017/10/23
 */
public abstract class AActuator implements IActuator {

    protected IActuator mSuccessor;

    public void setSuccessor(IActuator actuator){
        this.mSuccessor=actuator;
    }

}

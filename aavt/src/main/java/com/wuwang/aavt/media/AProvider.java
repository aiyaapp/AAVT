package com.wuwang.aavt.media;

/**
 * Created by wuwang on 2017/10/22.
 */

public abstract class AProvider<D> implements IProvider<D> {

    protected IProcessor mProcessor;

    public void setProcessor(IProcessor processor){
        this.mProcessor=processor;
    }

}

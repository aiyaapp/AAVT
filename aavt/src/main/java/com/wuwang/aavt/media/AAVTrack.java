package com.wuwang.aavt.media;

/**
 * Created by wuwang on 2017/10/20.
 */

public abstract class AAVTrack<T,K> {

    protected IAVProvider<T> mProvider;
    protected IAVProcessor<K> mProcessor;

    public void setProvider(IAVProvider<T> provider){
        this.mProvider=provider;
    }

    public void setProcessor(IAVProcessor<K> processor){
        this.mProcessor=processor;
    }

    public abstract void start();
    public abstract void stop();

}

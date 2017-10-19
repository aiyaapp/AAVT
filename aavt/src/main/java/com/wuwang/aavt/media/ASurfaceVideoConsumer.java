package com.wuwang.aavt.media;

import com.wuwang.aavt.core.IObserver;

/**
 * Created by wuwang on 2017/10/20.
 */

public abstract class ASurfaceVideoConsumer implements IVideoConsumer {

    private IObserver<AVMsg> mObserver;
    protected int mOutputWidth,mOutputHeight;
    protected Object mOutputSurface;

    @Override
    public void addObserver(IObserver<AVMsg> observer) {
        this.mObserver=observer;
    }

    @Override
    public void notify(AVMsg avMsg) {
        mObserver.onCall(avMsg);
    }

    public void setOutputSurface(Object surface){
        this.mOutputSurface=surface;
    }

    @Override
    public void setOutSize(int width, int height) {
        this.mOutputWidth=width;
        this.mOutputHeight=height;
    }

}

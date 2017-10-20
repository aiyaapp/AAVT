package com.wuwang.aavt.media;

import java.util.concurrent.Semaphore;

/**
 * Created by wuwang on 2017/10/20.
 */

public class SurfaceProvideBean extends AAVBean{

    public Semaphore mVideoSem;
    private Runnable mRun;

    public SurfaceProvideBean(Runnable run){
        this.mRun=run;
    }

    private int mSourceWidth;
    private int mSourceHeight;

    public void setSourceSize(int width,int height){
        this.mSourceWidth=width;
        this.mSourceHeight=height;
        if(mRun!=null){
            mRun.run();
        }
    }

    public int getSourceWidth(){
        return mSourceWidth;
    }

    public int getSourceHeight(){
        return mSourceHeight;
    }

}

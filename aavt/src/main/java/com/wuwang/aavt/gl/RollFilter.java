package com.wuwang.aavt.gl;

import android.content.res.Resources;
import android.opengl.GLES20;

/**
 * Created by aiya on 2017/9/25.
 */

public class RollFilter extends BaseFilter {

    private int mXRollTime=600;
    private int mYRollTime=600;
    private long mStartTime=0;

    public RollFilter(Resources resource) {
        super(resource);
    }

    public RollFilter(String vert, String frag) {
        super(vert, frag);
    }

    public void setRollTime(int xTime,int yTime){
        this.mXRollTime=xTime;
        this.mYRollTime=yTime;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        mStartTime=0;
    }

    @Override
    protected void onDraw() {
        if(mStartTime==0){
            mStartTime=System.currentTimeMillis();
        }
        long countTime=System.currentTimeMillis()-mStartTime;
        int sampleTime= (int) (countTime%(mXRollTime+mYRollTime));
        if(sampleTime<mXRollTime){
            //todo x方向滚动
            int shift= (sampleTime*mWidth/mXRollTime)/2;
            for (int i=0;i<3;i++){
                GLES20.glViewport(mWidth*i/2-shift,0,mWidth/2,mHeight/2);
                super.onDraw();
                GLES20.glViewport(mWidth*i/2+shift-mWidth/2,mHeight/2,mWidth/2,mHeight/2);
                super.onDraw();
            }
        }else{
            //todo y方向滚动
            int shift= (mHeight-(sampleTime-mXRollTime)*mHeight/mYRollTime)/2;
            for (int i=0;i<3;i++){
                GLES20.glViewport(0,mHeight*i/2-shift,mWidth/2,mHeight/2);
                super.onDraw();
                GLES20.glViewport(mWidth/2,mHeight*i/2+shift-mHeight/2,mWidth/2,mHeight/2);
                super.onDraw();
            }
        }
    }

}

package com.wuwang.aavt.gl;

import android.content.res.Resources;
import android.opengl.GLES20;

public class RollFilter extends BaseFilter {

    private int mXRollTime=10;
    private int mYRollTime=10;
    private int mFrameCount=0;

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
        mFrameCount=0;
    }

    @Override
    protected void onDraw() {
        mFrameCount++;
        if(mFrameCount>=(mXRollTime+mYRollTime)){
            mFrameCount=0;
        }
        if(mFrameCount<mXRollTime){
            //todo x方向滚动
            int shift= (mFrameCount*mWidth/mXRollTime)/2;
            for (int i=0;i<3;i++){
                GLES20.glViewport(mWidth*i/2-shift,0,mWidth/2,mHeight/2);
                super.onDraw();
                GLES20.glViewport(mWidth*i/2+shift-mWidth/2,mHeight/2,mWidth/2,mHeight/2);
                super.onDraw();
            }
        }else{
            //todo y方向滚动
            int shift= (mHeight-(mFrameCount-mXRollTime)*mHeight/mYRollTime)/2;
            for (int i=0;i<3;i++){
                GLES20.glViewport(0,mHeight*i/2-shift,mWidth/2,mHeight/2);
                super.onDraw();
                GLES20.glViewport(mWidth/2,mHeight*i/2+shift-mHeight/2,mWidth/2,mHeight/2);
                super.onDraw();
            }
        }
    }

}

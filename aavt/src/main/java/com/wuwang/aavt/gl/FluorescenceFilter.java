package com.wuwang.aavt.gl;

import android.content.res.Resources;
import android.opengl.GLES20;

public class FluorescenceFilter extends Filter {

    private int mGLTexture2;
    private int mGLBorderColor;
    private int mGLStep;
    private BlackMagicFilter mBlackFilter;
    private int mTempTexture;
    private float[] mBorderColor=new float[]{0f,1f,1f,1};
    private float mStep=1.0f;

    private boolean isAdd=true;

    public FluorescenceFilter(Resources resource) {
        super(resource, "shader/base.vert", "shader/effect/fluorescence.frag");
        shaderNeedTextureSize(true);
        mBlackFilter=new BlackMagicFilter(resource);
    }

    @Override
    protected void onCreate() {
        mBlackFilter.create();
        super.onCreate();
        mGLTexture2= GLES20.glGetUniformLocation(mGLProgram,"uTexture2");
        mGLBorderColor= GLES20.glGetUniformLocation(mGLProgram,"uBorderColor");
        mGLStep=GLES20.glGetUniformLocation(mGLProgram,"uStep");
    }

    @Override
    protected void onSizeChanged(int width, int height) {
        super.onSizeChanged(width, height);
        mBlackFilter.sizeChanged(width, height);
    }

    @Override
    public void draw(int texture) {
        mTempTexture=mBlackFilter.drawToTexture(texture);
        super.draw(texture);
    }

    @Override
    protected void onBindTexture(int textureId) {
        super.onBindTexture(textureId);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,mTempTexture);
        GLES20.glUniform1i(mGLTexture2,1);
    }

    @Override
    protected void onSetExpandData() {
        //todo 根据时间修改

        if(isAdd){
            mStep+=0.08f;
        }else{
            mStep-=0.08f;
        }

        if(mStep>=1.0f){
            isAdd=false;
            mStep=1.0f;
        }else if(mStep<=0.0f){
            isAdd=true;
            mStep=0.0f;
        }

        super.onSetExpandData();
        GLES20.glUniform4fv(mGLBorderColor,1,mBorderColor,0);
        GLES20.glUniform1f(mGLStep,mStep);
    }
}

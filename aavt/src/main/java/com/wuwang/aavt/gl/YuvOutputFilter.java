package com.wuwang.aavt.gl;

import android.content.res.Resources;
import android.opengl.GLES20;
import android.util.Log;

import com.wuwang.aavt.core.Filter;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by aiya on 2017/9/21.
 */

public class YuvOutputFilter extends Filter {

    private ByteBuffer mTempBuffer;

    private int mGLWidth;
    private int mGLHeight;
    private int[] lastViewPort=new int[4];

    public YuvOutputFilter(Resources resource) {
        super(resource, "shader/base.vert", "shader/convert/rgb2yuv420p.frag");
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        mGLWidth=GLES20.glGetUniformLocation(mGLProgram,"uWidth");
        mGLHeight=GLES20.glGetUniformLocation(mGLProgram,"uHeight");
    }

    @Override
    protected void onDraw() {
        GLES20.glGetIntegerv(GLES20.GL_VIEWPORT,lastViewPort,0);
        GLES20.glViewport(0,0,mWidth,mHeight);
        super.onDraw();
        if(mTempBuffer==null){
            mTempBuffer=ByteBuffer.allocate(mWidth*mHeight*3/2);
        }
        GLES20.glReadPixels(0,0,mWidth,mHeight*3/8,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,mTempBuffer);
        GLES20.glViewport(lastViewPort[0],lastViewPort[1],lastViewPort[2],lastViewPort[3]);
        Log.e("wuwang","read data:  "+ Arrays.toString(mTempBuffer.array()));
    }

    @Override
    protected void onSetExpandData() {
        super.onSetExpandData();
        GLES20.glUniform1f(mGLWidth,mWidth);
        GLES20.glUniform1f(mGLHeight,mHeight);
    }

    public void getOutput(byte[] data){
        if(mTempBuffer!=null){
            mTempBuffer.get(data);
            mTempBuffer.clear();
        }
    }

}

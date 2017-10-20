package com.wuwang.aavt.media;

import android.opengl.EGLSurface;
import android.opengl.GLES20;

import com.wuwang.aavt.gl.BaseFilter;
import com.wuwang.aavt.gl.Filter;
import com.wuwang.aavt.utils.MatrixUtils;

/**
 * Created by wuwang on 2017/10/21.
 */

public class ShowVideoProcessor extends AAVProcessor<SurfaceProcessBean> {

    private Object mOutputSurface;
    private EGLSurface mEglSurface;
    private Filter mFilter;
    private int mOutputWidth;
    private int mOutputHeight;

    @Override
    public void startProcess() {

    }

    @Override
    public void onProcess(SurfaceProcessBean data) {
        if(mOutputSurface!=null&&data.texture!=-1){
            if(mEglSurface==null){
                mEglSurface=data.egl.createWindowSurface(mOutputSurface);
                mFilter=new BaseFilter();
                mFilter.create();
                mFilter.sizeChanged(data.width, data.height);
                MatrixUtils.getMatrix(mFilter.getVertexMatrix(),MatrixUtils.TYPE_CENTERCROP,data.width,data.height,
                        mOutputWidth,mOutputHeight);
//                MatrixUtils.flip(mFilter.getVertexMatrix(),false,true);
            }
            data.egl.makeCurrent(mEglSurface);
            GLES20.glViewport(0,0,mOutputWidth,mOutputHeight);
            mFilter.draw(data.texture);
            data.egl.swapBuffers(mEglSurface);
        }
    }

    @Override
    public void stopProcess() {
        mEglSurface=null;
    }

    public void setOutputSurface(Object output){
        this.mOutputSurface=output;
    }

    public void setOutputSize(int width,int height){
        this.mOutputWidth=width;
        this.mOutputHeight=height;
    }

}

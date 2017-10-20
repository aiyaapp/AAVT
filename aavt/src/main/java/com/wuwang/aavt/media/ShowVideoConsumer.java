package com.wuwang.aavt.media;

import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.view.Surface;

import com.wuwang.aavt.egl.EGLHelper;
import com.wuwang.aavt.gl.BaseFilter;
import com.wuwang.aavt.gl.Filter;
import com.wuwang.aavt.utils.MatrixUtils;

/**
 * Created by wuwang on 2017/10/20.
 */

public class ShowVideoConsumer extends ASurfaceVideoConsumer{

    private EGLSurface mEglSurface;
    private Filter mFilter;
    private AVMsg msg=new AVMsg(AVMsg.Type.TYPE_VIDEO_SURFACE);

    @Override
    public void onSurfaceFrame(EGLHelper egl, int width, int height, int texture) {
        if(mOutputSurface!=null&&texture!=-1){
            if(mEglSurface==null){
                mEglSurface=egl.createWindowSurface(mOutputSurface);
                mFilter=new BaseFilter();
                mFilter.create();
                mFilter.sizeChanged(width, height);
                MatrixUtils.getMatrix(mFilter.getVertexMatrix(),MatrixUtils.TYPE_CENTERCROP,width,height,
                        mOutputWidth,mOutputHeight);
                MatrixUtils.flip(mFilter.getVertexMatrix(),false,true);
            }
            egl.makeCurrent(mEglSurface);
            GLES20.glViewport(0,0,mOutputWidth,mOutputHeight);
            mFilter.draw(texture);
            egl.swapBuffers(mEglSurface);
        }
        notify(msg);
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

}

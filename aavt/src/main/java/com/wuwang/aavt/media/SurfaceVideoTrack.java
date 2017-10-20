package com.wuwang.aavt.media;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;

import com.wuwang.aavt.egl.EGLConfigAttrs;
import com.wuwang.aavt.egl.EGLContextAttrs;
import com.wuwang.aavt.egl.EGLHelper;
import com.wuwang.aavt.gl.FrameBuffer;

import java.util.concurrent.Semaphore;

/**
 * Created by wuwang on 2017/10/20.
 */

public class SurfaceVideoTrack extends AAVTrack<SurfaceProvideBean,SurfaceProcessBean> {

    private boolean mGLThreadFlag=false;
    private Thread mGLThread;
    private int mInputSurfaceTextureId;
    private SurfaceTexture mInputSurfaceTexture;
    private Semaphore mSem;
    private WrapRenderer mRenderer;
    private int mSourceWidth;
    private int mSourceHeight;
    private SurfaceProvideBean mBean;
    private SurfaceProcessBean mProcessBean;
    private Semaphore mVideoSem;

    public SurfaceVideoTrack(){
        mSem=new Semaphore(0,true);
        mVideoSem=new Semaphore(0);
        mBean=new SurfaceProvideBean(mSourceSizeChangeRun);
        mBean.mVideoSem=mVideoSem;
        mProcessBean=new SurfaceProcessBean();
    }

    private Runnable mSourceSizeChangeRun=new Runnable() {
        @Override
        public void run() {
            mSourceWidth=mBean.getSourceWidth();
            mSourceHeight=mBean.getSourceHeight();
        }
    };

    @Override
    public void start() {

        mGLThreadFlag=true;
        mGLThread=new Thread(new Runnable() {
            @Override
            public void run() {
                glRun();
            }
        });
        mGLThread.start();
        try {
            mSem.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mProvider.startProvide();
        mProcessor.startProcess();
        mSem.release();
    }

    @Override
    public void stop() {
        mGLThreadFlag=false;
        mVideoSem.release();
    }

    private void glRun(){
        EGLHelper egl=new EGLHelper();
        boolean ret=egl.createGLESWithSurface(new EGLConfigAttrs(),new EGLContextAttrs(),new SurfaceTexture(1));
        if(!ret){
            //todo 错误处理
            mBean.msg=AAVBean.MSG_ERROR;
            mBean.msgStr="创建EGL失败";
            mProvider.onCall(mBean);
            mSem.release();
            return;
        }
        mProcessBean.egl=egl;

        mInputSurfaceTextureId=EGLHelper.createTextureID();
        mInputSurfaceTexture=new SurfaceTexture(mInputSurfaceTextureId);
        mBean.msg=AAVBean.MSG_OK;
        mBean.data=mInputSurfaceTexture;
        mProvider.onCall(mBean);
        mSem.release();
        try {
            mSem.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(mRenderer==null){
            mRenderer=new WrapRenderer(null);
        }
        FrameBuffer sourceFrame=new FrameBuffer();
        mRenderer.create();
        mRenderer.sizeChanged(mSourceWidth,mSourceHeight);
        mRenderer.setFlag(WrapRenderer.TYPE_CAMERA);
        while (mGLThreadFlag){
            try {
                mVideoSem.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(mGLThreadFlag){
                mInputSurfaceTexture.updateTexImage();
                mInputSurfaceTexture.getTransformMatrix(mRenderer.getTextureMatrix());
                sourceFrame.bindFrameBuffer(mSourceWidth,mSourceHeight);
                GLES20.glViewport(0,0,mSourceWidth,mSourceHeight);
                mRenderer.draw(mInputSurfaceTextureId);
                sourceFrame.unBindFrameBuffer();
                mProcessBean.texture=sourceFrame.getCacheTextureId();
                mProcessBean.width=mSourceWidth;
                mProcessBean.height=mSourceHeight;
                mProcessor.onProcess(mProcessBean);
            }
        }
        mProvider.stopProvide();
        mProcessor.stopProcess();
    }

}

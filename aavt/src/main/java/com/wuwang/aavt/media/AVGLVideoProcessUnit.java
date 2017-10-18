package com.wuwang.aavt.media;

import android.graphics.SurfaceTexture;
import android.media.MediaFormat;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.util.Log;

import com.wuwang.aavt.Aavt;
import com.wuwang.aavt.egl.EGLContextAttrs;
import com.wuwang.aavt.egl.EGLHelper;
import com.wuwang.aavt.egl.EGLSurfaceAttrs;
import com.wuwang.aavt.gl.BaseFilter;
import com.wuwang.aavt.gl.Filter;
import com.wuwang.aavt.gl.FrameBuffer;

import java.util.Arrays;

/*
 * Created by Wuwang on 2017/10/18
 */
public class AVGLProcessUnit extends AVExecUnit {

    private Thread mGLThread;
    private boolean mGLThreadFlag=false;
    private WrapRenderer mRenderer;
    private int mLastSourfaceWidth,mLastSourfaceHeight;
    private final Object GL_LOCK=new Object();

    @Override
    public void start() {
        mGLThreadFlag=true;
        mGLThread=new Thread(new Runnable() {
            @Override
            public void run() {

            }
        });
        mGLThread.start();
    }

    @Override
    public void putCmd(AVCmd cmd) {

    }

    private Runnable glRunnable=new Runnable() {
        @Override
        public void run() {
            EGLHelper egl=new EGLHelper();
            EGLConfig eglConfig=egl.getConfig(new EGLSurfaceAttrs());
            EGLSurface eglSurface=egl.createWindowSurface(eglConfig,new SurfaceTexture(1));
            EGLContext eglContext=egl.createContext(eglConfig, EGL14.EGL_NO_CONTEXT,new EGLContextAttrs());
            boolean ret=egl.makeCurrent(eglSurface,eglSurface,eglContext);
            if(!ret){
                Log.e(Aavt.debugTag,"CameraRecorder GLThread exit : createGLES failed");
                return;
            }
            if(mRenderer==null){
                mRenderer=new WrapRenderer(null);
            }
            mRenderer.setFlag(WrapRenderer.TYPE_CAMERA);
            mRenderer.create();
            int[] t=new int[1];
            GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING,t,0);
            mRenderer.sizeChanged(mSourceWidth,mSourceHeight);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,t[0]);

            Filter mShowFilter=new BaseFilter();
            mShowFilter.create();
            mShowFilter.sizeChanged(mSourceWidth,mSourceHeight);

            FrameBuffer tempFrameBuffer=new FrameBuffer();
            while (mGLThreadFlag){
                try {
                    mShowSem.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(mGLThreadFlag){
                    long time=(System.currentTimeMillis()-BASE_TIME)*1000;
                    mInputTexture.updateTexImage();
                    mInputTexture.getTransformMatrix(mRenderer.getTextureMatrix());
                    if (isPreviewStarted) {
                        if(mEGLPreviewSurface==null){
                            mEGLPreviewSurface=egl.createWindowSurface(eglConfig,mOutputSurface);
                        }
                        egl.makeCurrent(mEGLPreviewSurface,mEGLPreviewSurface,eglContext);
                        tempFrameBuffer.bindFrameBuffer(mSourceWidth,mSourceHeight);
                        GLES20.glViewport(0,0,mSourceWidth,mSourceHeight);
                        mRenderer.draw(mInputTextureId);
                        tempFrameBuffer.unBindFrameBuffer();
                        GLES20.glViewport(0,0,mPreviewWidth,mPreviewHeight);
                        log(Arrays.toString(mPreMatrix));
                        mShowFilter.setVertexMatrix(mPreMatrix);
                        mShowFilter.draw(tempFrameBuffer.getCacheTextureId());
                        egl.swapBuffers(mEGLPreviewSurface);
                    }
                    synchronized (VIDEO_LOCK){
                        if(isRecordVideoStarted){
                            if(mEGLEncodeSurface==null){
                                mEGLEncodeSurface=egl.createWindowSurface(eglConfig,mEncodeSurface);
                            }
                            egl.makeCurrent(mEGLEncodeSurface,mEGLEncodeSurface,eglContext);
                            if(!isPreviewStarted){
                                tempFrameBuffer.bindFrameBuffer(mSourceWidth,mSourceHeight);
                                GLES20.glViewport(0,0,mSourceWidth,mSourceHeight);
                                mRenderer.draw(mInputTextureId);
                                tempFrameBuffer.unBindFrameBuffer();
                            }
                            GLES20.glViewport(0,0,mConfig.getVideoFormat().getInteger(MediaFormat.KEY_WIDTH),
                                    mConfig.getVideoFormat().getInteger(MediaFormat.KEY_HEIGHT));
                            mShowFilter.setVertexMatrix(mRecMatrix);
                            mShowFilter.draw(tempFrameBuffer.getCacheTextureId());
                            egl.setPresentationTime(mEGLEncodeSurface,time*1000);
                            videoEncodeStep(false);
                            egl.swapBuffers(mEGLEncodeSurface);
                        }
                    }
                }
            }
            egl.destroyGLES(eglSurface,eglContext);
        }
    };

    @Override
    public void stop() {
        mGLThreadFlag=false;
        if(mGLThread!=null){
            try {
                mGLThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}

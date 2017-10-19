package com.wuwang.aavt.media;

import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;

import com.wuwang.aavt.core.Renderer;
import com.wuwang.aavt.egl.EGLConfigAttrs;
import com.wuwang.aavt.egl.EGLContextAttrs;
import com.wuwang.aavt.egl.EGLHelper;
import com.wuwang.aavt.gl.FrameBuffer;

import java.util.concurrent.Semaphore;

/**
 * Created by wuwang on 2017/10/19.
 */

public class Mp4Util {

    private IVideoProvider mVideoProvider;
    private IAudioProvider mAudioProvider;
    private IVideoConsumer mVideoConsumer;
    private IAudioConsumer mAudioConsumer;

    private boolean mGLThreadFlag=false;
    private Thread mGLThread;
    private SurfaceTexture mInputSurfaceTexture;
    private int mInputSurfaceTextureId;

    private int mSourceWidth=0,mSourceHeight=0;
    private WrapRenderer mRenderer;

    private Semaphore mVideoSem;

    public void start(){
        mVideoSem=new Semaphore(0);
        final Semaphore semaphore=new Semaphore(0,true);
        mGLThreadFlag=true;
        mGLThread=new Thread(new Runnable() {
            @Override
            public void run() {
                glThreadRun(semaphore);
            }
        });
        mGLThread.start();
        if(mVideoProvider!=null){
            if(mVideoConsumer!=null){
                mVideoConsumer.addObserver(mVideoProvider);
            }
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Point size=mVideoProvider.start(mInputSurfaceTexture,mVideoSem);
            if(size==null){
                mGLThreadFlag=false;
                semaphore.release();
                return;
            }
            mSourceWidth=size.x;
            mSourceHeight=size.y;
            semaphore.release();
        }
        if(mAudioProvider!=null){
            mAudioProvider.start();
            if(mAudioConsumer!=null){
                mAudioConsumer.addObserver(mAudioProvider);
            }
        }
    }

    public void setRenderer(Renderer renderer){
        mRenderer=new WrapRenderer(renderer);
    }

    public void setVideoProvider(IVideoProvider video){
        this.mVideoProvider=video;
    }

    public void setAudioProvider(IAudioProvider audio){
        this.mAudioProvider=audio;
    }

    public void setVideoConsumer(IVideoConsumer video){
        this.mVideoConsumer=video;
    }

    public void setAudioConsumer(IAudioConsumer audio){
        this.mAudioConsumer=audio;
    }

    public void stop(){
        if(mAudioProvider!=null){
            mAudioProvider.stop();
        }
        if(mVideoProvider!=null){
            mVideoProvider.stop();
        }
        mGLThreadFlag=false;
        mVideoSem.release();
    }

    private void glThreadRun(Semaphore sem){
        EGLHelper egl=new EGLHelper();
        boolean ret=egl.createGLESWithSurface(new EGLConfigAttrs(),new EGLContextAttrs(),new SurfaceTexture(1));
        if(!ret){
            //todo 错误处理
            sem.release();
            return;
        }

        mInputSurfaceTextureId=EGLHelper.createTextureID();
        mInputSurfaceTexture=new SurfaceTexture(mInputSurfaceTextureId);
        sem.release();
        try {
            sem.acquire();
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
                if(mVideoConsumer!=null){
                    mVideoConsumer.onSurfaceFrame(egl,mSourceWidth,mSourceHeight,sourceFrame.getCacheTextureId());
                }
            }
        }
        if(mVideoConsumer!=null){
            mVideoConsumer.onSurfaceFrame(egl,mSourceWidth,mSourceHeight,-1);
        }


    }




}

/*
 * Created by Wuwang on 2017/10/17
 */
package com.wuwang.aavt.media;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.opengl.EGL14;
import android.opengl.EGLSurface;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.wuwang.aavt.core.Renderer;
import com.wuwang.aavt.gl.BaseFilter;
import com.wuwang.aavt.gl.EGLHelper;
import com.wuwang.aavt.gl.Filter;
import com.wuwang.aavt.gl.FrameBuffer;
import com.wuwang.aavt.utils.MatrixUtils;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class Mp4Maker {

    private boolean isShowLog=true;
    private Object mPreviewSurface;
    private Thread mGLThread;
    private int mTemplateWidth,mTemplateHeight;
    private int mPreviewWidth,mPreviewHeight;
    private boolean mGLThreadFlag=false;
    private WrapRenderer mRenderer;
    private int mInputTextureId;
    private SurfaceTexture mInputTexture;

    private boolean isPreviewOpen=false;
    private boolean isRecordOpen=false;
    private EGLSurface mEglPreviewSurface;
    private EGLSurface mEglRecordSurface;

    private final Object GL_TEXTURE_LOCK=new Object();

    public Mp4Maker(){

    }

    public void create(){
        startGLThread();

    }

    public void release(){
        stopGLThread();
    }

    public void startPreview(){
        isPreviewOpen=true;
    }

    public void stopPreview(){
        isPreviewOpen=false;
    }

    public void startRecord(){

    }

    public void stopRecord(){

    }

    public void setPreviewSurface(Surface surface){
        this.mPreviewSurface=surface;
        isPreviewOpen=mPreviewSurface!=null;
    }

    public void setSourceSize(int width,int height){
        this.mTemplateWidth=width;
        this.mTemplateHeight=height;
    }

    public void setPreviewSize(int width,int height){
        this.mPreviewWidth=width;
        this.mPreviewHeight=height;
    }


    public void setRenderer(Renderer renderer){
        mRenderer=new WrapRenderer(renderer);
    }

    public SurfaceTexture getInputTexture(){
        return mInputTexture;
    }

    public void requestRender(){
        synchronized (GL_TEXTURE_LOCK){
            GL_TEXTURE_LOCK.notifyAll();
        }
    }

    private void startGLThread(){
        mGLThreadFlag=true;
        mGLThread=new Thread(mGLRunnable);
        mGLThread.start();
    }

    private void stopGLThread(){
        mGLThreadFlag=false;
    }

    private Runnable mGLRunnable=new Runnable() {

        private FrameBuffer tempFrameBuffer;
        private Filter mPreviewFilter;
        private int glPreviewWidth,glPreviewHeight;

        private void drawToFrameBuffer(int input){
            tempFrameBuffer.bindFrameBuffer(mTemplateWidth,mTemplateHeight);
            mRenderer.draw(input);
            tempFrameBuffer.unBindFrameBuffer();
        }

        private void prepare(){
            if(mRenderer==null){
                mRenderer=new WrapRenderer(null);
            }
            mRenderer.create();
            mRenderer.sizeChanged(mTemplateWidth,mTemplateHeight);
            tempFrameBuffer=new FrameBuffer();
            mPreviewFilter=new BaseFilter();
            mPreviewFilter.create();
            mPreviewFilter.sizeChanged(mTemplateWidth,mTemplateHeight);
        }

        private boolean drawPreview(EGLHelper egl){
            if(isPreviewOpen&&mPreviewSurface!=null){
                if(mEglPreviewSurface==null){
                    mEglPreviewSurface=egl.createEGLWindowSurface(mPreviewSurface);
                    egl.makeCurrent(mEglPreviewSurface);
                }
                drawToFrameBuffer(mInputTextureId);
                if(glPreviewHeight!=mPreviewHeight||glPreviewWidth!=mPreviewWidth){
                    MatrixUtils.getMatrix(mPreviewFilter.getVertexMatrix(),MatrixUtils.TYPE_CENTERCROP,
                            mTemplateWidth,mTemplateHeight,mPreviewWidth,mPreviewHeight);
                    this.glPreviewWidth=mPreviewWidth;
                    this.glPreviewHeight=mPreviewHeight;
                }
                mPreviewFilter.draw(tempFrameBuffer.getCacheTextureId());
                egl.swapBuffers(mEglPreviewSurface);
                return true;
            }
            return false;
        }

        private boolean drawRecord(EGLHelper egl,boolean hasFrame){
            return false;
        }

        @Override
        public void run() {
            EGLHelper egl=new EGLHelper();
            Surface surface=new VirtualSurface().getSurface(mTemplateWidth,mTemplateHeight);
            egl.setSurface(surface);
            boolean ret=egl.createGLES(mTemplateWidth,mTemplateHeight);
            if(!ret){
                //todo 错误通知
                log("创建GL环境失败");
                return;
            }

            mInputTextureId=egl.createTextureID();
            mInputTexture=new SurfaceTexture(mInputTextureId);
            prepare();
            log("gl prepare ok");
            while (mGLThreadFlag){
                log("while");
                synchronized (GL_TEXTURE_LOCK){
                    try {
                        log("GL_TEXTURE_LOCK try wait ");
                        GL_TEXTURE_LOCK.wait();
                        log("GL_TEXTURE_LOCK wait ok");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if(mGLThreadFlag){
                    mInputTexture.updateTexImage();
                    mInputTexture.getTransformMatrix(mRenderer.getTextureMatrix());
                    boolean isPreviewShow=drawPreview(egl);
                }
            }
            egl.destroyGLES();
        }
    };

    private void log(String info){
        if(isShowLog){
            Log.e("MediaEntry",info);
        }
    }

}

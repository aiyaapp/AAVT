package com.wuwang.aavt.media;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;

import com.wuwang.aavt.egl.EGLConfigAttrs;
import com.wuwang.aavt.egl.EGLContextAttrs;
import com.wuwang.aavt.egl.EGLHelper;
import com.wuwang.aavt.gl.FrameBuffer;

import java.util.concurrent.Semaphore;

/**
 * Created by wuwang on 2017/10/22.
 */

public class SurfaceTextureProcess {

    private boolean mGLThreadFlag=false;
    private Thread mGLThread;
    private int mInputSurfaceTextureId;
    private SurfaceTexture mInputSurfaceTexture;
    private Semaphore mSem;
    private WrapRenderer mRenderer;
    private int mSourceWidth;
    private int mSourceHeight;
    private AVMsg msg=new AVMsg();
    private Semaphore mVideoSem;
    private IAVCall<AVMsg> mCall;
    private GLBean mGLBean;
    private static final long BASE_TIME=System.currentTimeMillis();
    private boolean isCamera=false;

    public SurfaceTextureProcess(IAVCall<AVMsg> call){
        mSem=new Semaphore(0,true);
        mVideoSem=new Semaphore(0,true);
        mGLBean=new GLBean();
        this.mCall=call;
    }

    public void start() {
        mSem.drainPermits();
        mVideoSem.drainPermits();
        mGLThreadFlag=true;
        mGLThread=new Thread(new Runnable() {
            @Override
            public void run() {
                glRun();
            }
        });
        mGLThread.start();
        //等待GL环境创建成功
        try {
            mSem.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setAsCamera(boolean flag){
        this.isCamera=false;
    }

    public void setSourceSize(int width,int height){
        this.mSourceWidth=width;
        this.mSourceHeight=height;
        mSem.release();
    }

    public void processFrame(){
        mVideoSem.drainPermits();
        mVideoSem.release();
    }

    public void stop() {
        mVideoSem.drainPermits();
        mGLThreadFlag=false;
        mVideoSem.release();
        try {
            mSem.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void glRun(){
        EGLHelper egl=new EGLHelper();
        boolean ret=egl.createGLESWithSurface(new EGLConfigAttrs(),new EGLContextAttrs(),new SurfaceTexture(1));
        if(!ret){
            //todo 错误处理
            onCall(AVMsg.TYPE_ERROR,1,"创建EGL失败");
            mSem.release();
            return;
        }
        mGLBean.egl=egl;
        mInputSurfaceTextureId=EGLHelper.createTextureID();
        mInputSurfaceTexture=new SurfaceTexture(mInputSurfaceTextureId);
        onCall(AVMsg.MSG_SURFACE_CREATED,"获取Surface成功",mInputSurfaceTexture);
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
        mRenderer.setFlag(isCamera?WrapRenderer.TYPE_CAMERA:WrapRenderer.TYPE_MOVE);
        while (mGLThreadFlag){
            try {
                mVideoSem.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(mGLThreadFlag){
                mInputSurfaceTexture.updateTexImage();
                mInputSurfaceTexture.getTransformMatrix(mRenderer.getTextureMatrix());
                mGLBean.timeStamp=mInputSurfaceTexture.getTimestamp();
                sourceFrame.bindFrameBuffer(mSourceWidth,mSourceHeight);
                GLES20.glViewport(0,0,mSourceWidth,mSourceHeight);
                mRenderer.draw(mInputSurfaceTextureId);
                sourceFrame.unBindFrameBuffer();
                mGLBean.texture=sourceFrame.getCacheTextureId();
                mGLBean.width=mSourceWidth;
                mGLBean.height=mSourceHeight;
                onCall(AVMsg.MSG_TEXTURE_OK,"渲染处理成功",mGLBean);
            }
        }
        mGLBean.texture=-1;
        onCall(AVMsg.MSG_TEXTURE_OK,"最后一次渲染",mGLBean);
        mSem.release();
    }

    public static class GLBean{
        public int texture;
        public int width;
        public int height;
        public long timeStamp;
        public EGLHelper egl;
    }

    private void onCall(int type,int ret,String msg){
        if(mCall!=null){
            mCall.onCall(new AVMsg(type,ret,msg));
        }
    }

    private void onCall(int ret,String msg,Object data){
        if(mCall!=null){
            AVMsg avMsg=new AVMsg(AVMsg.TYPE_DATA,ret,msg);
            avMsg.msgData=data;
            mCall.onCall(avMsg);
        }
    }

}

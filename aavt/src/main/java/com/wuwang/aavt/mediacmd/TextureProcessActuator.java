package com.wuwang.aavt.mediacmd;

import android.annotation.TargetApi;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.os.Build;
import android.util.Log;

import com.wuwang.aavt.core.IObserver;
import com.wuwang.aavt.core.Observable;
import com.wuwang.aavt.core.Renderer;
import com.wuwang.aavt.egl.EGLConfigAttrs;
import com.wuwang.aavt.egl.EGLContextAttrs;
import com.wuwang.aavt.egl.EGLHelper;
import com.wuwang.aavt.gl.FrameBuffer;
import com.wuwang.aavt.media.WrapRenderer;

/*
 * Created by Wuwang on 2017/10/23
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class TextureProcessActuator extends AActuator implements IObserver<Cmd> {

    private boolean mGLThreadFlag=false;
    private Thread mGLThread;
    private int mInputSurfaceTextureId;
    private SurfaceTexture mInputSurfaceTexture;
    private int mSourceWidth;
    private int mSourceHeight;
    private WrapRenderer mRenderer;
    private Observable<Cmd> observable;
    private boolean isCamera=false;
    private static long startTime=System.currentTimeMillis();

    public TextureProcessActuator(){
        observable=new Observable<>();
    }

    @Override
    public void execute(Cmd cmd) {
        if(cmd.cmd==Cmd.CMD_VIDEO_OPEN_SOURCE){
            if(mSuccessor!=null){
                createEnv(cmd);
            }else{
                cmd.errorCallback(Cmd.ERROR_CMD_NO_EXEC,"please set video source actuator to SurfaceTextureActuator");
            }
        }else if(cmd.cmd==Cmd.CMD_VIDEO_CLOSE_SOURCE){
            mGLThreadFlag=false;
            if(mSuccessor!=null){
                mSuccessor.execute(cmd);
            }
            if(mGLThread!=null&&mGLThread.isAlive()){
                try {
                    mGLThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }else{
            if(mSuccessor!=null){
                mSuccessor.execute(cmd);
            }
        }
    }

    public void setAsCamera(boolean flag){
        this.isCamera=flag;
    }

    public void setRenderer(Renderer renderer){
        mRenderer=new WrapRenderer(renderer);
    }

    private void createEnv(final Cmd cmd){
        mGLThreadFlag=true;
        mGLThread=new Thread(new Runnable() {
            @Override
            public void run() {
                glRun(cmd);
            }
        });
        mGLThread.start();
    }

    private void glRun(Cmd cmd){
        EGLHelper egl=new EGLHelper();
        boolean ret=egl.createGLESWithSurface(new EGLConfigAttrs(),new EGLContextAttrs(),new SurfaceTexture(1));
        if(!ret){
            //todo 错误处理
            cmd.errorCallback(Cmd.ERROR_CMD_EXEC_FAILED,"创建EGL环境失败");
            return;
        }
        mInputSurfaceTextureId=EGLHelper.createTextureID();
        mInputSurfaceTexture=new SurfaceTexture(mInputSurfaceTextureId);

        Cmd tempCmd=cmd.cloneMe();
        tempCmd.obj=mInputSurfaceTexture;
        tempCmd.callback=new IObserver<Cmd>() {
            @Override
            public void onCall(Cmd cmd) {
                if(cmd.retType==Cmd.RET_TYPE_DATA){
                    if(cmd.retObj instanceof Point){
                        Point size= (Point) cmd.retObj;
                        mSourceWidth=size.x;
                        mSourceHeight=size.y;
                    }
                }
            }
        };
        mSuccessor.execute(tempCmd);
        //要求数据源提供者必须同步返回数据大小
        if(mSourceWidth<=0||mSourceHeight<=0){
            cmd.errorCallback(Cmd.ERROR_CMD_EXEC_FAILED,"video source return inaccurate size to SurfaceTextureActuator");
            return;
        }
        tempCmd.obj=null;
        tempCmd.callback=null;

        if(mRenderer==null){
            mRenderer=new WrapRenderer(null);
        }
        FrameBuffer sourceFrame=new FrameBuffer();
        mRenderer.create();
        mRenderer.sizeChanged(mSourceWidth,mSourceHeight);
        mRenderer.setFlag(isCamera?WrapRenderer.TYPE_CAMERA:WrapRenderer.TYPE_MOVE);

        //用于其他的回调
        RenderBean rb=new RenderBean();
        rb.egl=egl;
        rb.sourceWidth=mSourceWidth;
        rb.sourceHeight=mSourceHeight;
        rb.endFlag=false;
        Cmd renderCmd=new Cmd(Cmd.CMD_VIDEO_RENDER,"render video");
        renderCmd.retObj=rb;
        renderCmd.retType=Cmd.RET_TYPE_DATA;

        while (mGLThreadFlag){
            //要求数据源必须同步填充SurfaceTexture，填充完成前等待
            tempCmd.cmd=Cmd.CMD_VIDEO_FRAME_DATA;
            mSuccessor.execute(tempCmd);
            if(mGLThreadFlag){
                mInputSurfaceTexture.updateTexImage();
                mInputSurfaceTexture.getTransformMatrix(mRenderer.getTextureMatrix());
                sourceFrame.bindFrameBuffer(mSourceWidth,mSourceHeight);
                GLES20.glViewport(0,0,mSourceWidth,mSourceHeight);
                mRenderer.draw(mInputSurfaceTextureId);
                sourceFrame.unBindFrameBuffer();
                rb.textureId=sourceFrame.getCacheTextureId();
                if(isCamera){
                    rb.timeStamp=-1;
                    observable.notify(renderCmd);
                }else{
                    MediaCodec.BufferInfo buf= (MediaCodec.BufferInfo) tempCmd.retObj;
                    rb.timeStamp= buf.presentationTimeUs;
                    observable.notify(renderCmd);
                    Log.e("wuwang","BufferInfo  :  "+buf.flags);
                    if(buf.flags==MediaCodec.BUFFER_FLAG_END_OF_STREAM){
                        break;
                    }
                }
            }
        }
        rb.endFlag=true;
        observable.notify(renderCmd);
        // TODO: 2017/10/23  销毁egl
        EGL14.eglMakeCurrent(egl.getDisplay(), EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
        EGL14.eglDestroyContext(egl.getDisplay(),egl.getDefaultContext());
        EGL14.eglTerminate(egl.getDisplay());
        Log.e("wuwang","gl thread exit");
    }

    @Override
    public void onCall(Cmd cmd) {

    }

    public void addObserver(IObserver<Cmd> observer) {
        if(observer==this){
            return;
        }
        observable.addObserver(observer);
    }

}

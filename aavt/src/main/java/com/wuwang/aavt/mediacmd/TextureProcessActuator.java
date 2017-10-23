package com.wuwang.aavt.mediacmd;

import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;

import com.wuwang.aavt.core.IObservable;
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
public class TextureProcessActuator extends AActuator implements IObserver<Cmd> {

    private boolean mGLThreadFlag=false;
    private Thread mGLThread;
    private int mInputSurfaceTextureId;
    private SurfaceTexture mInputSurfaceTexture;
    private int mSourceWidth;
    private int mSourceHeight;
    private WrapRenderer mRenderer;
    private Observable<Cmd> observable;

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
        }
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
        mRenderer.setFlag(WrapRenderer.TYPE_CAMERA);

        //用于其他的回调
        RenderBean rb=new RenderBean();
        rb.egl=egl;
        rb.sourceWidth=mSourceWidth;
        rb.sourceHeight=mSourceHeight;
        rb.textureId=mInputSurfaceTextureId;
        rb.endFlag=false;
        Cmd renderCmd=new Cmd(Cmd.CMD_VIDEO_RENDER,"render video");
        renderCmd.retObj=rb;
        renderCmd.retType=Cmd.RET_TYPE_DATA;

        while (mGLThreadFlag){
            //要求数据源必须同步填充SurfaceTexture，填充完成前等待
            tempCmd.cmd=Cmd.CMD_VIDEO_FRAME_DATA;
            mSuccessor.execute(cmd);
            if(mGLThreadFlag){
                mInputSurfaceTexture.updateTexImage();
                mInputSurfaceTexture.getTransformMatrix(mRenderer.getTextureMatrix());
                sourceFrame.bindFrameBuffer(mSourceWidth,mSourceHeight);
                GLES20.glViewport(0,0,mSourceWidth,mSourceHeight);
                mRenderer.draw(mInputSurfaceTextureId);
                sourceFrame.unBindFrameBuffer();
                // TODO: 2017/10/23 预览、录制等操作执行
                observable.notify(renderCmd);
            }
        }
        rb.endFlag=true;
        observable.notify(renderCmd);
        // TODO: 2017/10/23  销毁egl

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

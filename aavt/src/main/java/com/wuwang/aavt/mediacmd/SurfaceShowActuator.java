package com.wuwang.aavt.mediacmd;

import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.TextureView;

import com.wuwang.aavt.core.IObserver;

/*
 * Created by Wuwang on 2017/10/23
 */
public class SurfaceShowActuator extends AActuator implements IObserver<Cmd> {

    private Object mSurface;

    @Override
    public void execute(Cmd cmd) {
        if(cmd.cmd==Cmd.CMD_VIDEO_OPEN_SHOW){
            if(cmd.obj==null||!((cmd.obj instanceof Surface)||(cmd.obj instanceof SurfaceTexture)||(cmd.obj instanceof TextureView))){
                cmd.errorCallback(Cmd.ERROR_CMD_EXEC_FAILED,"please set the surface which you want" +
                        " to display the video source to the cmd.obj");
                return;
            }else if(mSuccessor==null){
                cmd.errorCallback(Cmd.ERROR_CMD_EXEC_FAILED,"SurfaceShowActuator can not show the video without other actuator.");
                return;
            }

            this.mSurface=cmd.obj;

        }
    }

    @Override
    public void onCall(Cmd cmd) {
        if(cmd.cmd==Cmd.CMD_VIDEO_RENDER&&cmd.retType==Cmd.RET_TYPE_DATA){
            RenderBean rb= (RenderBean) cmd.retObj;
            if(rb.endFlag){
                //egl 即将销毁
            }else{
                //渲染动作
            }
        }
    }

}

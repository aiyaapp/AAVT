package com.wuwang.aavt.mediacmd;

import android.graphics.SurfaceTexture;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.wuwang.aavt.core.IObserver;
import com.wuwang.aavt.gl.BaseFilter;
import com.wuwang.aavt.gl.Filter;
import com.wuwang.aavt.utils.MatrixUtils;

/*
 * Created by Wuwang on 2017/10/23
 */
public class SurfaceShowActuator extends AActuator implements IObserver<Cmd> {

    private ShowBean mSurface;
    private EGLSurface mShowSurface;
    private boolean isShow=false;
    private Filter mFilter;

    @Override
    public void execute(Cmd cmd) {
        if(cmd.cmd==Cmd.CMD_VIDEO_OPEN_SHOW){
            if(cmd.obj==null||!(cmd.obj instanceof ShowBean)){
                cmd.errorCallback(Cmd.ERROR_CMD_EXEC_FAILED,"please set the ShowBean which you want" +
                        " to display the video source to the cmd.obj");
                return;
            }else if(mSuccessor==null){
                cmd.errorCallback(Cmd.ERROR_CMD_EXEC_FAILED,"SurfaceShowActuator can not show the video without other actuator.");
                return;
            }
            this.mSurface= (ShowBean) cmd.obj;
            if(mSurface.surface==null||!((mSurface.surface instanceof Surface)||(mSurface.surface instanceof SurfaceTexture)||
                    (mSurface.surface instanceof TextureView))){
                cmd.errorCallback(Cmd.ERROR_CMD_EXEC_FAILED,"surface must not be null, and should be" +
                        " one of Surface,SurfaceTexture,TextureView");
            }
            isShow=true;
        }if (cmd.cmd==Cmd.CMD_VIDEO_CLOSE_SHOW){
            isShow=false;
        }else{
            if(mSuccessor!=null){
                mSuccessor.execute(cmd);
            }
        }
    }

    @Override
    public void onCall(Cmd cmd) {
        if(cmd.cmd==Cmd.CMD_VIDEO_RENDER&&cmd.retType==Cmd.RET_TYPE_DATA){
            RenderBean rb= (RenderBean) cmd.retObj;
            draw(rb);
        }
    }

    private void draw(RenderBean bean){
        if(bean.endFlag&&mShowSurface!=null){
            bean.egl.destroySurface(mShowSurface);
            mShowSurface=null;
        }else if(isShow&&mSurface!=null){
            if(mShowSurface==null){
                mShowSurface=bean.egl.createWindowSurface(mSurface.surface);
                mFilter=new BaseFilter();
                mFilter.create();
                mFilter.sizeChanged(bean.sourceWidth, bean.sourceHeight);
                MatrixUtils.getMatrix(mFilter.getVertexMatrix(),MatrixUtils.TYPE_CENTERCROP,bean.sourceWidth,bean.sourceHeight,
                        mSurface.mOutputWidth,mSurface.mOutputHeight);
            }
            bean.egl.makeCurrent(mShowSurface);
            GLES20.glViewport(0,0,mSurface.mOutputWidth,mSurface.mOutputHeight);
            mFilter.draw(bean.textureId);
            bean.egl.swapBuffers(mShowSurface);
        }
    }

}

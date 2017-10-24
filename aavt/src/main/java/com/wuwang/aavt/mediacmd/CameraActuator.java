package com.wuwang.aavt.mediacmd;

import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.Semaphore;

/*
 * Created by Wuwang on 2017/10/24
 */
public class CameraActuator extends AActuator{

    private int cameraId=1;
    private Camera mCamera;
    private Semaphore frameSem;
    private SurfaceTexture mPreviewTexture;

    @Override
    public void execute(final Cmd cmd){
        if(cmd.cmd==Cmd.CMD_VIDEO_OPEN_SOURCE){
            closeCamera();
            try {
                frameSem=new Semaphore(0);
                mCamera=Camera.open(cameraId);
                mPreviewTexture=(SurfaceTexture) cmd.obj;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mPreviewTexture.setOnFrameAvailableListener(frameAvailableListener);
                    }
                });
                mCamera.setPreviewTexture(mPreviewTexture);
                Camera.Size size=mCamera.getParameters().getPreviewSize();
                cmd.callback(Cmd.RET_VALUE_SUCCESS,"open camera ok",new Point(size.height,size.width));
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else if(cmd.cmd==Cmd.CMD_VIDEO_CLOSE_SOURCE){
            closeCamera();
        }else if(cmd.cmd==Cmd.CMD_VIDEO_FRAME_DATA){
            try {
                frameSem.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void closeCamera(){
        if(mCamera!=null){
            mCamera.stopPreview();
            mCamera.release();
            mCamera=null;
            frameSem.release();
        }
    }

    private SurfaceTexture.OnFrameAvailableListener frameAvailableListener=new SurfaceTexture.OnFrameAvailableListener() {
        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            frameSem.drainPermits();
            frameSem.release();
        }
    };
}

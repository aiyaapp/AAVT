package com.wuwang.aavt.media;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;

import java.io.IOException;

/**
 * Created by wuwang on 2017/10/22.
 */

public class CameraProvider extends AProvider<Object> implements IAVCall<AVMsg>,SurfaceTexture.OnFrameAvailableListener {

    private Camera mCamera;
    private int cameraId=1;
    private SurfaceTextureProcess mSurfaceProcess;
    private SurfaceTexture mSurface;
    private boolean isStarted=false;
    private Object mOutputSurface;

    public CameraProvider(){
        mSurfaceProcess=new SurfaceTextureProcess(this);
    }

    @Override
    public void start() {
        if(!isStarted){
            isStarted=true;
            mSurfaceProcess.start();
            try {
                mCamera=Camera.open(cameraId);
                Camera.Size size=mCamera.getParameters().getPreviewSize();
                mSurfaceProcess.setSourceSize(size.height,size.width);
                mSurface.setOnFrameAvailableListener(this);
                mCamera.setPreviewTexture(mSurface);
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void provide(Object surface) {
        this.mOutputSurface=surface;
    }

    @Override
    public void stop() {
        if(isStarted){
            mSurfaceProcess.stop();
            if(mCamera!=null){
                mCamera.stopPreview();
                mCamera.release();
                mCamera=null;
            }
            isStarted=false;
        }
    }

    @Override
    public void onCall(AVMsg data) {
        if(data.msgType==AVMsg.TYPE_DATA){
            switch (data.msgRet){
                case AVMsg.MSG_TEXTURE_OK:
                    onProcessFrame((SurfaceTextureProcess.GLBean)data.msgData);
                    break;
                case AVMsg.MSG_SURFACE_CREATED:
                    mSurface= (SurfaceTexture) data.msgData;
                    break;
            }
        }
    }

    private void onProcessFrame(SurfaceTextureProcess.GLBean bean){
        mProcessor.process(bean,mOutputSurface);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mSurfaceProcess.processFrame();
    }

}

package com.wuwang.aavt.media;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;

import java.io.IOException;

/**
 * Created by wuwang on 2017/10/21.
 */

public class CameraProvider implements IAVProvider<SurfaceProvideBean> {

    private SurfaceProvideBean data;
    private SurfaceTexture mSurface;
    private Camera mCamera;
    private int mCameraId=1;

    @Override
    public void onCall(SurfaceProvideBean data) {
        if(data.msg==SurfaceProvideBean.MSG_OK){
            this.mSurface= (SurfaceTexture) data.data;
            this.data=data;
        }
    }

    @Override
    public void startProvide() {
        try {
            mCamera= Camera.open(mCameraId);
            Camera.Size size=mCamera.getParameters().getPreviewSize();
            if(data!=null){
                data.setSourceSize(size.height,size.width);
            }
            mSurface.setOnFrameAvailableListener(onFrameAvailableListener);
            mCamera.setPreviewTexture(mSurface);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private SurfaceTexture.OnFrameAvailableListener onFrameAvailableListener=new SurfaceTexture.OnFrameAvailableListener() {
        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            if(data!=null){
                data.mVideoSem.release();
            }
        }
    };

    @Override
    public void stopProvide() {
        if(mCamera!=null){
            mCamera.stopPreview();
            mCamera.release();
            mCamera=null;
        }
    }

}

package com.wuwang.aavt.media;

import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Size;

import java.io.IOException;
import java.util.concurrent.Semaphore;

/**
 * Created by wuwang on 2017/10/19.
 */

public class CameraVideoProvider implements IVideoProvider,SurfaceTexture.OnFrameAvailableListener {

    private Camera mCamera;
    private int cameraId=1;
    private Semaphore mSourceSem;

    @Override
    public Point start(SurfaceTexture texture, Semaphore sem) {
        try {
            mCamera=Camera.open(cameraId);
            mCamera.setPreviewTexture(texture);
            Camera.Size size=mCamera.getParameters().getPreviewSize();
            mCamera.startPreview();
            this.mSourceSem=sem;
            texture.setOnFrameAvailableListener(this);
            return new Point(size.height,size.width);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void stop() {
        if(mCamera!=null){
            mCamera.stopPreview();
            mCamera.release();
            mCamera=null;
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mSourceSem.drainPermits();
        mSourceSem.release();
    }

    @Override
    public void onCall(AVMsg avMsg) {

    }
}

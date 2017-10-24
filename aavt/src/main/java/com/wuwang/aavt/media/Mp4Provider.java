package com.wuwang.aavt.media;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaExtractor;
import android.os.Build;

import java.io.IOException;

/*
 * Created by Wuwang on 2017/10/23
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class Mp4Provider extends AProvider<Object> implements IAVCall<HardCodecData>,SurfaceTexture.OnFrameAvailableListener{

    private Camera mCamera;
    private int cameraId=1;
    private SurfaceTextureProcess mSurfaceProcess;
    private SurfaceTexture mSurface;
    private boolean isStarted=false;
    private Object mOutputSurface;
    private MediaExtractor mExtractor;
    private String path;

    public Mp4Provider(){
        mSurfaceProcess=new SurfaceTextureProcess(new IAVCall<AVMsg>() {
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
        });
    }

    public void setDataSource(String path){
        this.path=path;
    }

    @Override
    public void start() {
        if(!isStarted){
            isStarted=true;
            mSurfaceProcess.start();

            try {
                mExtractor=new MediaExtractor();
                mExtractor.setDataSource(path);
                mExtractor.getTrackCount();
            } catch (IOException e) {
                e.printStackTrace();
            }
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

    private void onProcessFrame(SurfaceTextureProcess.GLBean bean){
        mProcessor.process(bean,mOutputSurface);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mSurfaceProcess.processFrame();
    }

    @Override
    public void onCall(HardCodecData data) {

    }
}

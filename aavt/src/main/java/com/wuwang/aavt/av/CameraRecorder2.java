package com.wuwang.aavt.av;

import android.os.Environment;

import com.wuwang.aavt.media.AudioEncoder;
import com.wuwang.aavt.media.CameraProvider;
import com.wuwang.aavt.media.IProcessor;
import com.wuwang.aavt.media.Mp4HardwareStore;
import com.wuwang.aavt.media.RecordAudioProvider;
import com.wuwang.aavt.media.SurfaceEncoder;
import com.wuwang.aavt.media.SurfaceShower;
import com.wuwang.aavt.media.SurfaceTextureProcess;

/**
 * Created by wuwang on 2017/10/22.
 */

public class CameraRecorder2 {

    private CameraProvider mProvider;
    private SurfaceShower mShower;
    private SurfaceEncoder mSurfaceEncoder;
    private Mp4HardwareStore mMp4Store;
    private AudioEncoder mAudioEncoder;
    private RecordAudioProvider mAudioProvider;
    private String mPath= Environment.getExternalStorageDirectory().getAbsolutePath()+"/test.mp4";

    private int mShowWidth=720;
    private int mShowHeight=1280;

    public CameraRecorder2(){
        mProvider=new CameraProvider();
        mShower=new SurfaceShower();
        mMp4Store=new Mp4HardwareStore();
        mSurfaceEncoder=new SurfaceEncoder(mMp4Store);
        mMp4Store.addAVCall(mSurfaceEncoder);
        mProvider.setProcessor(new IProcessor<SurfaceTextureProcess.GLBean,Object>() {
            @Override
            public int process(SurfaceTextureProcess.GLBean o, Object o2) {
                mShower.process(o, o2);
                mSurfaceEncoder.process(o,o2);
                return 0;
            }
        });
        mAudioEncoder=new AudioEncoder();
        mAudioProvider=new RecordAudioProvider();
        mAudioEncoder.setIAVCall(mAudioProvider);
        mAudioEncoder.setMediaStore(mMp4Store);
        mMp4Store.addAVCall(mAudioEncoder);
    }

    public void setShowSurface(Object surface){
        mProvider.provide(surface);
    }

    public void setShowSize(int width,int height){
        this.mShowWidth=width;
        this.mShowHeight=height;
    }

    public void setOutputPath(String path){
        mPath=path;
    }

    public void setOutputSize(int width,int height){
        mSurfaceEncoder.setOutputSize(width, height);
    }

    public void openCamera(){
        mProvider.start();
    }

    public void closeCamera(){
        mProvider.stop();
    }

    public void startPreview(){
        mShower.openShow(mShowWidth,mShowHeight);
    }

    public void stopPreview(){
        mShower.closeShow();
    }

    public void startRecord(){
        mMp4Store.start(mPath);
    }

    public void stopRecord(){
        mMp4Store.stop();
    }

}

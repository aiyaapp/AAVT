package com.wuwang.aavt.examples;

import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import com.wuwang.aavt.av.CameraRecorder;
import com.wuwang.aavt.gl.BeautyFilter;
import com.wuwang.aavt.gl.Filter;
import com.wuwang.aavt.core.Renderer;
import com.wuwang.aavt.media.ASurfaceVideoConsumer;
import com.wuwang.aavt.media.AudioRecordStuffer;
import com.wuwang.aavt.media.CameraVideoProvider;
import com.wuwang.aavt.media.IVideoProvider;
import com.wuwang.aavt.media.Mp4Maker;
import com.wuwang.aavt.media.Mp4Util;
import com.wuwang.aavt.media.Mp4VideoProvider;
import com.wuwang.aavt.media.ShowVideoConsumer;
import com.wuwang.aavt.utils.MatrixUtils;

import java.io.IOException;

public class CameraRecorderActivity extends AppCompatActivity{

    private Mp4Maker mMp4Maker;
    private SurfaceView mSurfaceView;
    private Camera mCamera;
    private TextView mTvPreview,mTvRecord;
    private boolean isPreviewOpen=false;
    private boolean isRecordOpen=false;
    private Filter mFilter;
    private int mCameraWidth,mCameraHeight;

    private IVideoProvider mVideoProvider;
    private ASurfaceVideoConsumer mVideoConsumer;
    private Mp4Util mMP4Util;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_record);
        mSurfaceView= (SurfaceView) findViewById(R.id.mSurface);
        mFilter=new BeautyFilter(getResources()).setBeautyLevel(5);
        mMP4Util=new Mp4Util();
        mVideoProvider=new CameraVideoProvider();//new Mp4VideoProvider();
        mVideoConsumer=new ShowVideoConsumer();
        mMP4Util.setVideoProvider(mVideoProvider);
        mMP4Util.setVideoConsumer(mVideoConsumer);

        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mVideoConsumer.setOutputSurface(holder.getSurface());
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                mVideoConsumer.setOutSize(width, height);
                mMP4Util.start();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mMP4Util.stop();
            }
        });
    }

    public void onClick(View view){
//        switch (view.getId()){
//            case R.id.mTvShow:
//                isPreviewOpen=!isPreviewOpen;
//                mTvPreview.setText(isPreviewOpen?"关预览":"开预览");
//                if(isPreviewOpen){
//                    mMp4Maker.startPreview();
//                }else{
//                    mMp4Maker.stopPreview();
//                }
//                break;
//            case R.id.mTvRec:
//                isRecordOpen=!isRecordOpen;
//                mTvRecord.setText(isRecordOpen?"关录制":"开录制");
//                if(isRecordOpen){
//                    try {
//                        mMp4Maker.startRecord();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }else{
//                    try {
//                        mMp4Maker.stopRecord();
//                        Intent v=new Intent(Intent.ACTION_VIEW);
//                        v.setDataAndType(Uri.parse(Environment.getExternalStorageDirectory().getAbsolutePath()+"/temp_cam.mp4"),"video/mp4");
//                        startActivity(v);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//                break;
//        }
    }

//    @Override
//    public void create() {
//        try {
//            mCamera.setPreviewTexture(mMp4Maker.createInputSurfaceTexture());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        Camera.Size mSize=mCamera.getParameters().getPreviewSize();
//        mCameraWidth=mSize.height;
//        mCameraHeight=mSize.width;
//        mMp4Maker.setSourceSize(mCameraWidth,mCameraHeight);
//        mCamera.startPreview();
//
//        mFilter.create();
//    }
//
//    @Override
//    public void sizeChanged(int width, int height) {
//        mFilter.sizeChanged(width, height);
//    }
//
//    @Override
//    public void draw(int texture) {
//        mFilter.draw(texture);
//    }
//
//    @Override
//    public void destroy() {
//        mFilter.destroy();
//    }
}

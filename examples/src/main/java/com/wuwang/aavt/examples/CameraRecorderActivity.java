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
import com.wuwang.aavt.media.AudioRecordStuffer;
import com.wuwang.aavt.media.Mp4Maker;
import com.wuwang.aavt.utils.MatrixUtils;

import java.io.IOException;

public class CameraRecorderActivity extends AppCompatActivity implements Renderer {

    private Mp4Maker mMp4Maker;
    private SurfaceView mSurfaceView;
    private Camera mCamera;
    private TextView mTvPreview,mTvRecord;
    private boolean isPreviewOpen=false;
    private boolean isRecordOpen=false;
    private Filter mFilter;
    private int mCameraWidth,mCameraHeight;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_record);
        mSurfaceView= (SurfaceView) findViewById(R.id.mSurface);
        mFilter=new BeautyFilter(getResources()).setBeautyLevel(5);
        mMp4Maker=new Mp4Maker();
        mMp4Maker.setAudioStuffer(new AudioRecordStuffer());

        mMp4Maker.setOutputPath(Environment.getExternalStorageDirectory().getAbsolutePath()+"/temp_cam.mp4");
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mCamera=Camera.open(1);
                mMp4Maker.setOutputSurface(holder.getSurface());
                mMp4Maker.setOutputSize(480, 640);
                mMp4Maker.setRenderer(CameraRecorderActivity.this);
                mMp4Maker.create();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                mMp4Maker.setSourceSize(width, height);
                mMp4Maker.setPreviewSize(width,height);
                mMp4Maker.startPreview();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if(isPreviewOpen){
                    isPreviewOpen=false;
                    try {
                        mMp4Maker.stopRecord();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mTvPreview.setText("开预览");
                    mTvRecord.setText("开录制");
                }
                mMp4Maker.stopPreview();
                mMp4Maker.setOutputSurface(null);
                try {
                    mMp4Maker.destroy();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(mCamera!=null){
                    mCamera.stopPreview();
                    mCamera.release();
                    mCamera=null;
                }
            }
        });
        mTvPreview= (TextView) findViewById(R.id.mTvShow);
        mTvRecord= (TextView) findViewById(R.id.mTvRec);
    }

    public void onClick(View view){
        switch (view.getId()){
            case R.id.mTvShow:
                isPreviewOpen=!isPreviewOpen;
                mTvPreview.setText(isPreviewOpen?"关预览":"开预览");
                if(isPreviewOpen){
                    mMp4Maker.startPreview();
                }else{
                    mMp4Maker.stopPreview();
                }
                break;
            case R.id.mTvRec:
                isRecordOpen=!isRecordOpen;
                mTvRecord.setText(isRecordOpen?"关录制":"开录制");
                if(isRecordOpen){
                    try {
                        mMp4Maker.startRecord();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else{
                    try {
                        mMp4Maker.stopRecord();
                        Intent v=new Intent(Intent.ACTION_VIEW);
                        v.setDataAndType(Uri.parse(Environment.getExternalStorageDirectory().getAbsolutePath()+"/temp_cam.mp4"),"video/mp4");
                        startActivity(v);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    @Override
    public void create() {
        try {
            mCamera.setPreviewTexture(mMp4Maker.createInputSurfaceTexture());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Camera.Size mSize=mCamera.getParameters().getPreviewSize();
        mCameraWidth=mSize.height;
        mCameraHeight=mSize.width;
        mMp4Maker.setSourceSize(mCameraWidth,mCameraHeight);
        mCamera.startPreview();

        mFilter.create();
    }

    @Override
    public void sizeChanged(int width, int height) {
        mFilter.sizeChanged(width, height);
    }

    @Override
    public void draw(int texture) {
        mFilter.draw(texture);
    }

    @Override
    public void destroy() {
        mFilter.destroy();
    }
}

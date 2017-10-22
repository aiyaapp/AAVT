package com.wuwang.aavt.examples;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import com.wuwang.aavt.av.CameraRecorder2;
import com.wuwang.aavt.gl.BeautyFilter;
import com.wuwang.aavt.gl.Filter;

public class CameraRecorderActivity extends AppCompatActivity{

    private SurfaceView mSurfaceView;
    private TextView mTvPreview,mTvRecord;
    private boolean isPreviewOpen=false;
    private boolean isRecordOpen=false;
    private Filter mFilter;
    private int mCameraWidth,mCameraHeight;

    private CameraRecorder2 mCamera2;

    private String tempPath= Environment.getExternalStorageDirectory().getAbsolutePath()+"/test.mp4";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_record);
        mSurfaceView= (SurfaceView) findViewById(R.id.mSurface);
        mTvRecord= (TextView) findViewById(R.id.mTvRec);
        mTvPreview= (TextView) findViewById(R.id.mTvShow);

        long startTime=System.currentTimeMillis();

        mCamera2=new CameraRecorder2();
        mCamera2.setOutputPath(tempPath);
        mCamera2.setOutputSize(368,640);

        mFilter=new BeautyFilter(getResources()).setBeautyLevel(5);


        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                mCamera2.setShowSurface(holder.getSurface());
                mCamera2.setShowSize(width, height);
                mCamera2.openCamera();
                mCamera2.startPreview();
                isPreviewOpen=true;
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mCamera2.closeCamera();
            }
        });
    }

    public void onClick(View view){
        switch (view.getId()){
            case R.id.mTvShow:
                isPreviewOpen=!isPreviewOpen;
                mTvPreview.setText(isPreviewOpen?"关预览":"开预览");
                if(isPreviewOpen){
                    mCamera2.startPreview();
                }else{
                    mCamera2.stopPreview();
                }
                break;
            case R.id.mTvRec:
                isRecordOpen=!isRecordOpen;
                mTvRecord.setText(isRecordOpen?"关录制":"开录制");
                if(isRecordOpen){
                    mCamera2.startRecord();
                }else{
                    mCamera2.stopRecord();
                    Intent v=new Intent(Intent.ACTION_VIEW);
                    v.setDataAndType(Uri.parse(tempPath),"video/mp4");
                    startActivity(v);
                }
                break;
        }
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

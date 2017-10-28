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
import com.wuwang.aavt.gl.GrayFilter;

public class CameraRecorderActivity extends AppCompatActivity{

    private SurfaceView mSurfaceView;
    private TextView mTvPreview,mTvRecord;
    private boolean isPreviewOpen=false;
    private boolean isRecordOpen=false;
    private Filter mFilter;
    private int mCameraWidth,mCameraHeight;

    private CameraRecorder2 mCamera;

    private String tempPath= Environment.getExternalStorageDirectory().getAbsolutePath()+"/test.mp4";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_record);
        mSurfaceView= (SurfaceView) findViewById(R.id.mSurfaceView);
        mTvRecord= (TextView) findViewById(R.id.mTvRec);
        mTvPreview= (TextView) findViewById(R.id.mTvShow);

        mCamera =new CameraRecorder2();
        mCamera.setRenderer(new BeautyFilter(getResources()).setBeautyLevel(4));
        mCamera.setOutputPath(tempPath);

        mFilter=new BeautyFilter(getResources()).setBeautyLevel(5);


        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                mCamera.open();
                mCamera.setSurface(holder.getSurface());
                mCamera.startPreview();
                isPreviewOpen=true;
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mCamera.stopPreview();
                mCamera.close();
            }
        });
    }

    public void onClick(View view){
        switch (view.getId()){
            case R.id.mTvShow:
                isPreviewOpen=!isPreviewOpen;
                mTvPreview.setText(isPreviewOpen?"关预览":"开预览");
                if(isPreviewOpen){
                    mCamera.startPreview();
                }else{
                    mCamera.stopPreview();
                }
                break;
            case R.id.mTvRec:
                isRecordOpen=!isRecordOpen;
                mTvRecord.setText(isRecordOpen?"关录制":"开录制");
                if(isRecordOpen){
                    mCamera.startRecord();
                }else{
                    mCamera.stopRecord();
                    Intent v=new Intent(Intent.ACTION_VIEW);
                    v.setDataAndType(Uri.parse(tempPath),"video/mp4");
                    startActivity(v);
                }
                break;
        }
    }

}

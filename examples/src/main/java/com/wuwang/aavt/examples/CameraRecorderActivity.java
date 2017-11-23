package com.wuwang.aavt.examples;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.wuwang.aavt.av.CameraRecorder2;
import com.wuwang.aavt.gl.BaseFilter;
import com.wuwang.aavt.gl.BeautyFilter;
import com.wuwang.aavt.gl.BlackMagicFilter;
import com.wuwang.aavt.gl.GrayFilter;
import com.wuwang.aavt.gl.GroupFilter;
import com.wuwang.aavt.gl.LazyFilter;
import com.wuwang.aavt.gl.StickFigureFilter;
import com.wuwang.aavt.gl.WaterMarkFilter;
import com.wuwang.aavt.utils.MatrixUtils;

public class CameraRecorderActivity extends AppCompatActivity{

    private SurfaceView mSurfaceView;
    private TextView mTvPreview,mTvRecord;
    private boolean isPreviewOpen=false;
    private boolean isRecordOpen=false;
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
        mCamera.setOutputPath(tempPath);

        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
//                GroupFilter filter=new GroupFilter(getResources());
//                mCamera.setRenderer(filter);
//                filter.addFilter(new StickFigureFilter(getResources()));
//                filter.addFilter(new BeautyFilter(getResources()).setBeautyLevel(4));
//                filter.addFilter(new WaterMarkFilter().setMarkPosition(30,10,100,76).setMark(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher)));
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                mCamera.open();
                mCamera.setSurface(holder.getSurface());
                mCamera.setPreviewSize(width, height);
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
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent v=new Intent(Intent.ACTION_VIEW);
                            v.setDataAndType(Uri.parse(tempPath),"video/mp4");
                            if(v.resolveActivity(getPackageManager()) != null){
                                startActivity(v);
                            }else{
                                Toast.makeText(CameraRecorderActivity.this,
                                        "无法找到默认媒体软件打开:"+tempPath, Toast.LENGTH_SHORT).show();
                            }
                        }
                    },1000);
                }
                break;
                default:
                    break;
        }
    }

}

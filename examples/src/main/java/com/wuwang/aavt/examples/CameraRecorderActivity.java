package com.wuwang.aavt.examples;

import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import com.wuwang.aavt.av.CameraRecorder;
import com.wuwang.aavt.core.Filter;
import com.wuwang.aavt.core.Renderer;
import com.wuwang.aavt.gl.BaseFilter;
import com.wuwang.aavt.gl.BeautyFilter;
import com.wuwang.aavt.gl.GrayFilter;
import com.wuwang.aavt.gl.WaterColorFilter;
import com.wuwang.aavt.gl.YuvOutputFilter;
import com.wuwang.aavt.utils.MatrixUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by aiya on 2017/9/12.
 */

public class CameraRecorderActivity extends AppCompatActivity implements Renderer {

    private CameraRecorder mCameraRecord;
    private SurfaceView mSurfaceView;
    private Camera mCamera;
    private TextView mTvStart;
    private boolean isStart=false;
    private Filter mFilter;
    private int mCameraWidth,mCameraHeight;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_record);
        mSurfaceView= (SurfaceView) findViewById(R.id.mSurface);
        mFilter= new WaterColorFilter(getResources()); //new BeautyFilter(getResources()).setBeautyLevel(5);
        mCameraRecord=new CameraRecorder();
        mCameraRecord.setOutputPath(Environment.getExternalStorageDirectory().getAbsolutePath()+"/temp_cam.mp4");
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mCamera=Camera.open(0);
                mCameraRecord.setOutputSurface(holder.getSurface());
                mCameraRecord.setOutputSize(480, 640);
                mCameraRecord.setRenderer(CameraRecorderActivity.this);
                mCameraRecord.startPreview();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                mCameraRecord.setPreviewSize(width,height);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if(isStart){
                    isStart=false;
                    try {
                        mCameraRecord.stopRecord();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mTvStart.setText("开始");
                }
                try {
                    mCameraRecord.stopPreview();
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
        mTvStart= (TextView) findViewById(R.id.mTvStart);
        mYuvFilter=new YuvOutputFilter(YuvOutputFilter.EXPORT_TYPE_NV12);
    }

    private boolean mGetYUVFlag=false;

    private YuvOutputFilter mYuvFilter;

    public void onClick(View view){
        switch (view.getId()){
            case R.id.mTvStart:
                isStart=!isStart;
                mTvStart.setText(isStart?"停止":"开始");
                if(isStart){
                    try {
                        mCameraRecord.startRecord();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else{
                    try {
                        mCameraRecord.stopRecord();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Intent v=new Intent(Intent.ACTION_VIEW);
                    v.setDataAndType(Uri.parse(Environment.getExternalStorageDirectory().getAbsolutePath()+"/temp_cam.mp4"),"video/mp4");
                    startActivity(v);
                }
                break;
            case R.id.mTvGetYUV:
                mGetYUVFlag=true;
                break;
        }
    }

    @Override
    public void create() {
        try {
            mCamera.setPreviewTexture(mCameraRecord.createInputSurfaceTexture());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Camera.Size mSize=mCamera.getParameters().getPreviewSize();
        mCameraWidth=mSize.height;
        mCameraHeight=mSize.width;
        mCamera.startPreview();

        mFilter.create();
        mYuvFilter.create();
    }

    private int width,height;

    @Override
    public void sizeChanged(int width, int height) {
        this.width=368;
        this.height=640;
        Log.e("wuwang","size:"+width+"/"+height);
        mFilter.sizeChanged(width, height);
        MatrixUtils.getMatrix(mFilter.getVertexMatrix(),MatrixUtils.TYPE_CENTERCROP,mCameraWidth,mCameraHeight,width,height);
        MatrixUtils.flip(mFilter.getVertexMatrix(),false,true);
        mYuvFilter.sizeChanged(this.width,this.height);
        MatrixUtils.getMatrix(mYuvFilter.getVertexMatrix(),MatrixUtils.TYPE_CENTERCROP,mCameraWidth,mCameraHeight,this.width,this.height);
        MatrixUtils.flip(mYuvFilter.getVertexMatrix(),false,true);
    }

    @Override
    public void draw(int texture) {
        if(mGetYUVFlag){
            mYuvFilter.drawToTexture(texture);
            byte[] data=new byte[width*height*3/2];
            mYuvFilter.getOutput(data);
            Log.e("wuwang","data->"+ Arrays.toString(data));
            File file=new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/test.yuv");
            try {
                FileOutputStream fos=new FileOutputStream(file);
                fos.write(data);
                fos.flush();
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mGetYUVFlag=false;
        }
        mFilter.draw(texture);
//        byte[] data=new byte[width*height*3/2];
//        mYuvFilter.getOutput(data);
//        File file=new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/test.yuv");
//        try {
//            FileOutputStream fos=new FileOutputStream(file);
//            fos.write(data);
//            fos.flush();
//            fos.close();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//
//        }
    }

    @Override
    public void destroy() {
        mFilter.destroy();
    }
}

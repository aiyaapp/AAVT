/*
 * Created by Wuwang on 2017/9/11
 * Copyright © 2017年 深圳哎吖科技. All rights reserved.
 */
package com.wuwang.aavt.examples;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.wuwang.aavt.av.Mp4Processor2;

public class ExampleMp4ProcessActivity extends AppCompatActivity {

    private SurfaceView mSurfaceView;
    private Mp4Processor2 mMp4Processor;
    private String tempPath= Environment.getExternalStorageDirectory().getAbsolutePath()+"/test.mp4";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mp4);
        mMp4Processor=new Mp4Processor2();
        mMp4Processor.setInputPath(Environment.getExternalStorageDirectory().getAbsolutePath()+"/a.mp4");
        mMp4Processor.setOutputPath(tempPath);
        mSurfaceView= (SurfaceView) findViewById(R.id.mSurfaceView);
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                mMp4Processor.setSurface(holder);
                mMp4Processor.setPreviewSize(width, height);
                mMp4Processor.startPreview();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mMp4Processor.stopPreview();
            }
        });
    }

    public void onClick(View view){
        switch (view.getId()){
            case R.id.mOpen:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                //intent.setType(“image/*”);//选择图片
                //intent.setType(“audio/*”); //选择音频
                intent.setType("video/mp4"); //选择视频 （mp4 3gp 是android支持的视频格式）
                //intent.setType(“video/*;image/*”);//同时选择视频和图片
                //intent.setType("*/*");//无类型限制
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, 1);
                break;
            case R.id.mProcess:
                mMp4Processor.startRecord();
                mMp4Processor.open();
                break;
            case R.id.mStop:
                mMp4Processor.stopRecord();
                mMp4Processor.close();
                break;
            case R.id.mPlay:
                Intent v=new Intent(Intent.ACTION_VIEW);
                v.setDataAndType(Uri.parse(tempPath),"video/mp4");
                startActivity(v);
                break;
            default:
                break;
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            String path = getRealFilePath(data.getData());
            if (path != null) {
                mMp4Processor.setInputPath(path);
            }
        }
    }

    public String getRealFilePath(final Uri uri) {
        if (null == uri) return null;
        final String scheme = uri.getScheme();
        String data = null;
        if (scheme == null) {
            Log.e("wuwang", "scheme is null");
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            data = uri.getPath();
            Log.e("wuwang", "SCHEME_FILE");
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            data = GetPathFromUri4kitkat.getPath(getApplicationContext(), uri);
        }
        return data;
    }

}

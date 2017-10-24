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

import com.wuwang.aavt.mediacmd.Mp4Processor;

public class ExampleMp4ProcessActivity extends AppCompatActivity {

    private Mp4Processor mProcessor;
    private SurfaceView mSurfaceView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mp4);
        mProcessor=new Mp4Processor(Environment.getExternalStorageDirectory().getAbsolutePath()+"/a.mp4");
        mProcessor.setOutputPath(Environment.getExternalStorageDirectory().getAbsolutePath()+"/temp.mp4");
        mSurfaceView= (SurfaceView) findViewById(R.id.mSurfaceView);
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                mProcessor.setPreviewSurface(holder.getSurface());
                mProcessor.setPreviewSize(width, height);
                mProcessor.startPreview();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mProcessor.stopPreview();
            }
        });
//        mProcessor.setOnCompleteListener(new Mp4Processor.OnProgressListener() {
//            @Override
//            public void onProgress(long max, long current) {
//                Log.e("wuwang","max/current:"+max+"/"+current);
//            }
//
//            @Override
//            public void onComplete(String path) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Toast.makeText(getApplicationContext(),"处理完毕",Toast.LENGTH_SHORT).show();
//                    }
//                });
//            }
//        });
//        mProcessor.setRenderer(new Renderer() {
//
//            Filter filter;
//
//            @Override
//            public void create() {
//                filter=new BlackMagicFilter(getResources());
//                filter.create();
//            }
//
//            @Override
//            public void sizeChanged(int width, int height) {
//                filter.sizeChanged(width, height);
//            }
//
//            @Override
//            public void draw(int texture) {
//                filter.draw(texture);
//            }
//
//            @Override
//            public void destroy() {
//                filter.destroy();
//            }
//        });
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
                mProcessor.close();
                mProcessor.startRecord();
                mProcessor.open();
                break;
            case R.id.mStop:
                mProcessor.stopRecord();
                mProcessor.close();
                break;
            case R.id.mPlay:
                Intent v=new Intent(Intent.ACTION_VIEW);
                v.setDataAndType(Uri.parse(Environment.getExternalStorageDirectory().getAbsolutePath()+"/temp.mp4"),"video/mp4");
                startActivity(v);
                break;
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            String path = getRealFilePath(data.getData());
            if (path != null) {
                mProcessor.setInputPath(path);
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

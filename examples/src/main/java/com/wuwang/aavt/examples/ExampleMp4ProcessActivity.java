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
import android.view.View;
import android.widget.Toast;

import com.wuwang.aavt.av.Mp4Processor;
import com.wuwang.aavt.gl.BaseFilter;
import com.wuwang.aavt.gl.Filter;
import com.wuwang.aavt.core.Renderer;
import com.wuwang.aavt.gl.GrayFilter;
import com.wuwang.aavt.gl.RollFilter;
import com.wuwang.aavt.gl.SobelFilter;

import java.io.IOException;

public class ExampleMp4ProcessActivity extends AppCompatActivity {

    private Mp4Processor mProcessor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mp4);
        mProcessor=new Mp4Processor();
        mProcessor.setOutputPath(Environment.getExternalStorageDirectory().getAbsolutePath()+"/temp.mp4");
        mProcessor.setOnCompleteListener(new Mp4Processor.CompleteListener() {
            @Override
            public void onComplete(String path) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),"处理完毕",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        mProcessor.setRenderer(new Renderer() {

            Filter filter;

            @Override
            public void create() {
                filter=new SobelFilter(getResources());
                filter.create();
            }

            @Override
            public void sizeChanged(int width, int height) {
                filter.sizeChanged(width, height);
            }

            @Override
            public void draw(int texture) {
                filter.draw(texture);
            }

            @Override
            public void destroy() {
                filter.destroy();
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
                try {
                    mProcessor.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.mStop:
                try {
                    mProcessor.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
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
//                Configuration config = new Configuration();
//                config.sourcePath = path;
//                mProviders.setConfiguration(config);
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

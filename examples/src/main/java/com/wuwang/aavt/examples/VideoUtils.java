/*
 * Created by Wuwang on 2017/10/11
 * Copyright © 2017年 深圳哎吖科技. All rights reserved.
 */
package com.wuwang.aavt.examples;

import android.util.Log;

import com.wuwang.aavt.av.Mp4Processor;
import com.wuwang.aavt.core.Renderer;
import com.wuwang.aavt.gl.BaseFilter;
import com.wuwang.aavt.gl.LazyFilter;

import java.io.IOException;


public class VideoUtils {

    public static void transcodeVideoFile(String srcVideoFile, String dstVideoFile, int dstWidth, int dstHeight, int durationUS, OnProgress onProgress) throws IOException {
        final Mp4Processor processor=new Mp4Processor();
        processor.setOutputPath(dstVideoFile);
        processor.setInputPath(srcVideoFile);
        processor.setOutputSize(dstWidth,dstHeight);
        processor.setOnCompleteListener(new Mp4Processor.OnProgressListener() {
            @Override
            public void onProgress(long max, long current) {

            }

            @Override
            public void onComplete(String path) {
                Log.e("wuwang","end:::::"+path);
            }
        });
        processor.setRenderer(new Renderer() {

            BaseFilter mFilter;

            @Override
            public void create() {
                mFilter=new LazyFilter();
                mFilter.create();
            }

            @Override
            public void sizeChanged(int width, int height) {
                mFilter.sizeChanged(width, height);
            }

            @Override
            public void draw(int texture) {
                mFilter.draw(texture);
                Log.e("wuwang","getPresentationTime:"+processor.getPresentationTime());
            }

            @Override
            public void destroy() {
                mFilter.destroy();
            }
        });
        processor.start();
    }

    interface OnProgress{
        void process(long time);
    }

}

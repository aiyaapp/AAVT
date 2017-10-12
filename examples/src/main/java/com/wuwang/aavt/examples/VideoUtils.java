package com.wuwang.aavt.examples;

import android.media.MediaPlayer;
import android.os.Environment;
import android.util.Log;

import com.wuwang.aavt.av.Mp4Processor;
import com.wuwang.aavt.core.Renderer;
import com.wuwang.aavt.gl.BaseFilter;
import com.wuwang.aavt.gl.Filter;

import java.io.IOException;

/*
 * Created by Wuwang on 2017/10/11
 * Copyright © 2017年 深圳哎吖科技. All rights reserved.
 */
public class VideoUtils {

    public static int transcodeVideoFile(String srcVideoFile, String dstVideoFile, int dstWidth, int dstHeight, final int durationUS, final Mp4Processor.OnProgressListener onProgress) throws IOException {
        if(srcVideoFile==null||dstVideoFile==null||durationUS<2000000){
            return -1;
        }
        final Mp4Processor processor=new Mp4Processor();
        processor.setOutputPath(dstVideoFile);
        processor.setInputPath(srcVideoFile);
        processor.setOutputSize(dstWidth,dstHeight);
        processor.setOnCompleteListener(new Mp4Processor.OnProgressListener() {
            @Override
            public void onProgress(long max, long current) {
                if(onProgress!=null){
                    onProgress.onProgress(max, current);
                    if(durationUS<=current){
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    processor.stop();
                                    processor.waitProcessFinish();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    }
                }
            }

            @Override
            public void onComplete(String path) {
                if(onProgress!=null){
                    onProgress.onComplete(path);
                }
            }
        });
        processor.start();
        return 0;
    }

}

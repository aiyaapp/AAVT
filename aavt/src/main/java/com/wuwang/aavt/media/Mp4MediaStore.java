package com.wuwang.aavt.media;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.os.Environment;

import java.io.IOException;
import java.nio.ByteBuffer;

/*
 * Created by Wuwang on 2017/10/21
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class Mp4MediaStore implements MediaStore<MediaFormat,HardCodecData> {

    private MediaMuxer mMuxer;
    private String mPath= Environment.getExternalStorageDirectory().toString()+"/test.mp4";
    private final Object MUX_LOCK=new Object();

    @Override
    public int start(MediaFormat format){
        synchronized (MUX_LOCK){
            try {
                if(mMuxer==null){
                    mMuxer=new MediaMuxer(mPath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                }
                return mMuxer.addTrack(format);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    @Override
    public int store(HardCodecData data){
        mMuxer.writeSampleData(data.trackIndex,data.data,data.info);
        return 0;
    }

    public void setSotrePath(String path){
        this.mPath=path;
    }

    @Override
    public int stop(){
        if(mMuxer!=null){
            mMuxer.stop();
        }
        return 0;
    }



}

package com.wuwang.aavt.mediacmd;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;

/*
 * Created by Wuwang on 2017/10/24
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class Mp4Muxer implements HardMediaStore {

    private MediaMuxer mMuxer;
    private Semaphore mTrackSem;
    private String mPath;
    private boolean hasAudio;
    private boolean isMuxStart=false;

    public Mp4Muxer(boolean hasAudio){
        this.hasAudio=hasAudio;
    }

    public void setOutputPath(String path){
        this.mPath=path;
    }

    @Override
    public int addFormat(MediaFormat format) {
        if(mMuxer==null){
            try {
                //如果无音频，设置1，有就设置0
                mTrackSem=new Semaphore(hasAudio?0:1);
                mMuxer=new MediaMuxer(mPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        int ret=-1;
        String mime=format.getString(MediaFormat.KEY_MIME);
        if(mime.startsWith("audio")){
            ret = mMuxer.addTrack(format);
            mTrackSem.release();
            Log.e("wuwang","add audio track:"+ret);
        }else if(mime.startsWith("video")){
            try {
                mTrackSem.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ret = mMuxer.addTrack(format);
            mMuxer.start();
            isMuxStart=true;
            Log.e("wuwang","add video track:"+ret);
        }
        return ret;
    }

    @Override
    public void addData(int track, ByteBuffer buffer, MediaCodec.BufferInfo info) {
        if(isMuxStart){
            if(info.size>0&&info.presentationTimeUs>=0){
                mMuxer.writeSampleData(track, buffer, info);
            }
            Log.e("wuwang","data-->"+track+"/"+info.flags);
            if (info.flags==MediaCodec.BUFFER_FLAG_END_OF_STREAM){
                stop();
            }
        }
    }

    private void stop(){
        isMuxStart=false;
        mMuxer.stop();
        mMuxer.release();
        mMuxer=null;
    }

}

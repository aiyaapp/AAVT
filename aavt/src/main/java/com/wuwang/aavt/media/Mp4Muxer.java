/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wuwang.aavt.media;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;

/**
 * Mp4Muxer Mp4混流类
 *
 * @author wuwang
 * @version v1.0 2017:10:26 18:30
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class Mp4Muxer implements HardMediaStore {

    private MediaMuxer mMuxer;
    private Semaphore mTrackSem;
    private String mPath;
    private boolean hasAudio;
    private boolean isMuxStart=false;
    private int mAudioCount=0;

    public Mp4Muxer(boolean hasAudio){
        this.hasAudio=hasAudio;
    }

    @Override
    public void setOutputPath(String path){
        this.mPath=path;
    }

    @Override
    public int addFormat(MediaFormat format) {
        Log.i("wuwang","addFormat:"+format.toString());
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
            mAudioCount++;
            mTrackSem.release();
            Log.e("wuwang","add audio track:"+ret);
        }else if(mime.startsWith("video")){
            try {
                mTrackSem.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ret = mMuxer.addTrack(format);
            mAudioCount++;
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
            if (info.flags==MediaCodec.BUFFER_FLAG_END_OF_STREAM){
                mAudioCount--;
                Log.i("wuwang","BUFFER_FLAG_END_OF_STREAM for track:"+track);
                if(mAudioCount==0){
                    stop();
                }
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


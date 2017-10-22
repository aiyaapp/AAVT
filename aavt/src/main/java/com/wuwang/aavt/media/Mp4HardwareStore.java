package com.wuwang.aavt.media;

import android.annotation.TargetApi;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

/**
 * Created by wuwang on 2017/10/22.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class Mp4HardwareStore implements MediaStore<HardCodecData>{

    private MediaMuxer mMuxer;
    private static long startTime=System.currentTimeMillis();

    private ArrayList<IAVCall<AVMsg>> calls=new ArrayList<>(2);
    private boolean isAudioAdd=false;
    private boolean isVideoAdd=false;
    private Semaphore mTrackSem;

    public void addAVCall(IAVCall<AVMsg> call){
        this.calls.add(call);
    }

    @Override
    public void start(String path) {
        try {
            mTrackSem=new Semaphore(0);
            mMuxer=new MediaMuxer(path,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            onCall(AVMsg.MSG_CMD_START,"开始录制",startTime);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void store(HardCodecData data) {
        if(mMuxer!=null){
            if(data.type== HardCodecData.ADDTRACK){
                MediaFormat format= (MediaFormat) data.other;
                String mime=format.getString(MediaFormat.KEY_MIME);
                if(mime.startsWith("audio")&&!isAudioAdd){
                    data.trackIndex=mMuxer.addTrack((MediaFormat) data.other);
                    isAudioAdd=true;
                    mTrackSem.release();
                }else if(mime.startsWith("video")&&!isVideoAdd){
                    if(!isAudioAdd){
                        try {
                            mTrackSem.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    data.trackIndex=mMuxer.addTrack((MediaFormat) data.other);
                    isVideoAdd=true;
                    mMuxer.start();
                    onCall(AVMsg.MSG_STORE_START,"添加Track成功",startTime);
                }
            }else if(data.type== HardCodecData.DATA&&data.info.size>0&&isAudioAdd&&isVideoAdd&&data.info.presentationTimeUs>0){
                Log.e("wuwang","writeSampleData:"+data.trackIndex+"/"+data.info.presentationTimeUs);
                mMuxer.writeSampleData(data.trackIndex,data.data,data.info);
            }
        }
    }

    @Override
    public void stop() {
        onCall(AVMsg.MSG_CMD_END,"结束录制",0);
        if(mMuxer!=null){
            try {
                mMuxer.stop();
                mMuxer.release();
            }catch (IllegalStateException e){
                e.printStackTrace();
            }finally {
                mMuxer=null;
                isAudioAdd=false;
                isVideoAdd=false;
                mTrackSem=null;
            }
        }
    }

    private void onCall(int ret,String msg,Object data){
        AVMsg avMsg=new AVMsg(AVMsg.TYPE_INFO,ret,msg);
        avMsg.msgData=data;
        for (IAVCall<AVMsg> call:calls){
            call.onCall(avMsg);
        }
    }

}

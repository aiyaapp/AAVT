package com.wuwang.aavt.media;

import android.annotation.TargetApi;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.os.Build;

/**
 * Created by wuwang on 2017/10/23.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class RecordAudioProvider implements IAVCall<HardCodecData> {

    private AudioRecord mRecord;

    private int mRecordBufferSize=0;
    private int mRecordSampleRate=48000;   //音频采样率
    private int mRecordChannelConfig= AudioFormat.CHANNEL_IN_STEREO;   //音频录制通道,默认为立体声
    private int mRecordAudioFormat=AudioFormat.ENCODING_PCM_16BIT; //音频录制格式，默认为PCM16Bit

    private boolean isStarted=false;
    private final Object REC_LOCK=new Object();

    @Override
    public void onCall(HardCodecData data) {
        synchronized (REC_LOCK){
            if(!isStarted){
                mRecordBufferSize = AudioRecord.getMinBufferSize(mRecordSampleRate,
                        mRecordChannelConfig, mRecordAudioFormat)*2;
                mRecord=new AudioRecord(MediaRecorder.AudioSource.MIC,mRecordSampleRate,mRecordChannelConfig,
                        mRecordAudioFormat,mRecordBufferSize);
                mRecord.startRecording();
                isStarted=true;
            }
            data.info.size=mRecord.read(data.data,mRecordBufferSize);
            if(isStarted&&data.info.flags==MediaCodec.BUFFER_FLAG_END_OF_STREAM){
                mRecord.stop();
                mRecord.release();
                mRecord=null;
                isStarted=false;
            }
        }
    }

}

package com.wuwang.aavt.media;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

/*
 * Created by Wuwang on 2017/10/18
 */
public class AudioRecordStuffer implements Stuffer {

    private AudioRecord mAudioRecord;

    private int mRecordBufferSize=0;
    private int mRecordSampleRate=48000;   //音频采样率
    private int mRecordChannelConfig= AudioFormat.CHANNEL_IN_STEREO;   //音频录制通道,默认为立体声
    private int mRecordAudioFormat=AudioFormat.ENCODING_PCM_16BIT; //音频录制格式，默认为PCM16Bit

    public AudioRecordStuffer(){
        mRecordBufferSize = AudioRecord.getMinBufferSize(mRecordSampleRate,
                mRecordChannelConfig, mRecordAudioFormat)*2;
        mAudioRecord=new AudioRecord(MediaRecorder.AudioSource.MIC,mRecordSampleRate,mRecordChannelConfig,
                mRecordAudioFormat,mRecordBufferSize);
    }

    @Override
    public int start() {
        mAudioRecord.startRecording();
        return 0;
    }

    @Override
    public int stuff(AVFrame frame) {
        return mAudioRecord.read(frame.mData,mRecordBufferSize);
    }

    @Override
    public int stop() {
        mAudioRecord.stop();
        return 0;
    }

}

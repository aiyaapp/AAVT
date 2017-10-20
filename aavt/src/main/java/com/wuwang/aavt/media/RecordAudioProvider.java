package com.wuwang.aavt.media;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.nio.ByteBuffer;

/*
 * Created by Wuwang on 2017/10/20
 */
public class RecordAudioProvider implements IAudioProvider<ByteBuffer> {


    private AudioRecord mAudioRecord;

    private int mRecordBufferSize=0;
    private int mRecordSampleRate=48000;   //音频采样率
    private int mRecordChannelConfig= AudioFormat.CHANNEL_IN_STEREO;   //音频录制通道,默认为立体声
    private int mRecordAudioFormat=AudioFormat.ENCODING_PCM_16BIT; //音频录制格式，默认为PCM16Bit

    public RecordAudioProvider(){
        mRecordBufferSize = AudioRecord.getMinBufferSize(mRecordSampleRate,
                mRecordChannelConfig, mRecordAudioFormat)*2;
        mAudioRecord=new AudioRecord(MediaRecorder.AudioSource.MIC,mRecordSampleRate,mRecordChannelConfig,
                mRecordAudioFormat,mRecordBufferSize);
    }

    @Override
    public void onCall(AVMsg<ByteBuffer> avMsg) {
        avMsg.receiveDataLength=mAudioRecord.read(avMsg.receiveData,mRecordBufferSize);
    }

    @Override
    public void start() {
        mAudioRecord.startRecording();
    }

    @Override
    public void stop() {
        mAudioRecord.stop();
    }

}

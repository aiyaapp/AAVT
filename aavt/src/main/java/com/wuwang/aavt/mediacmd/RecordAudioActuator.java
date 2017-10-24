package com.wuwang.aavt.mediacmd;

import android.annotation.TargetApi;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

import com.wuwang.aavt.Aavt;
import com.wuwang.aavt.media.CodecUtil;
import com.wuwang.aavt.media.MediaConfig;

import java.io.IOException;
import java.nio.ByteBuffer;

/*
 * Created by Wuwang on 2017/10/24
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class RecordAudioActuator extends AActuator {

    private AudioRecord mRecord;

    private int mRecordBufferSize=0;
    private int mRecordSampleRate=48000;   //音频采样率
    private int mRecordChannelConfig= AudioFormat.CHANNEL_IN_STEREO;   //音频录制通道,默认为立体声
    private int mRecordAudioFormat=AudioFormat.ENCODING_PCM_16BIT; //音频录制格式，默认为PCM16Bit
    private boolean isStarted=false;
    private final Object REC_LOCK=new Object();
    private HardMediaStore mStore;
    private MediaCodec mAudioEncoder;
    private MediaConfig.Audio mConfig=new MediaConfig().mAudio;
    private final int TIME_OUT=1000;
    private MediaCodec.BufferInfo mAudioEncodeBufferInfo=new MediaCodec.BufferInfo();
    private int mAudioTrack=-1;
    private long startTime=0;

    public RecordAudioActuator(HardMediaStore store){
        this.mStore=store;
    }

    @Override
    public void execute(Cmd cmd) {
        if(cmd.cmd==Cmd.CMD_AUDIO_OPEN_ENCODE){
            if(mStore==null){
                cmd.errorCallback(Cmd.ERROR_CMD_EXEC_FAILED,"please set HardMediaStore to RecordAudioActuator");
            }
            if(!isStarted){
                mRecordBufferSize = AudioRecord.getMinBufferSize(mRecordSampleRate,
                        mRecordChannelConfig, mRecordAudioFormat)*2;
                mRecord=new AudioRecord(MediaRecorder.AudioSource.MIC,mRecordSampleRate,mRecordChannelConfig,
                        mRecordAudioFormat,mRecordBufferSize);
                mRecord.startRecording();
                try {
                    MediaFormat format=convertConfigToFormat(mConfig);
                    mAudioEncoder=MediaCodec.createEncoderByType(format.getString(MediaFormat.KEY_MIME));
                    mAudioEncoder.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
                    mAudioEncoder.start();
                    startTime=System.currentTimeMillis();
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                isStarted=true;
                start();
            }
        }else if(cmd.cmd==Cmd.CMD_AUDIO_CLOSE_ENCODE){
            audioEncodeStep(true);
        }else{
            if(mSuccessor!=null){
                mSuccessor.execute(cmd);
            }
        }
    }

    public void setConfig(MediaConfig config){
        this.mConfig=config.mAudio;
    }
    protected MediaFormat convertConfigToFormat(MediaConfig.Audio config){
        MediaFormat format=MediaFormat.createAudioFormat(config.mime,config.sampleRate,config.channelCount);
        format.setInteger(MediaFormat.KEY_BIT_RATE,config.bitrate);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        return format;
    }

    private void start(){
        Thread thread=new Thread(new Runnable() {
            @Override
            public void run() {
                while (isStarted&&!audioEncodeStep(false));
            }
        });
        thread.start();
    }

    private synchronized boolean audioEncodeStep(boolean isEnd){
        if(isStarted){
            int inputIndex=mAudioEncoder.dequeueInputBuffer(TIME_OUT);
            if(inputIndex>=0){
                ByteBuffer buffer= CodecUtil.getInputBuffer(mAudioEncoder,inputIndex);
                buffer.clear();
                long time=(System.currentTimeMillis()-startTime)*1000;
                int length=mRecord.read(buffer,mRecordBufferSize);
                if(length>=0){
                    mAudioEncoder.queueInputBuffer(inputIndex,0,length,time,
                            isEnd?MediaCodec.BUFFER_FLAG_END_OF_STREAM:0);
                }
            }
            while (true){
                int outputIndex=mAudioEncoder.dequeueOutputBuffer(mAudioEncodeBufferInfo,TIME_OUT);
                if(outputIndex>=0){
                    //todo 第一帧音频时间戳为0的问题
                    if(mStore!=null){
                        mStore.addData(mAudioTrack,CodecUtil.getOutputBuffer(mAudioEncoder,outputIndex),mAudioEncodeBufferInfo);
                    }
                    mAudioEncoder.releaseOutputBuffer(outputIndex,false);
                    if(mAudioEncodeBufferInfo.flags==MediaCodec.BUFFER_FLAG_END_OF_STREAM){
                        Log.d(Aavt.debugTag,"CameraRecorder get audio encode end of stream");
                        stop();
                        return true;
                    }
                }else if(outputIndex==MediaCodec.INFO_TRY_AGAIN_LATER){
                    break;
                }else if(outputIndex==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                    Log.e(Aavt.debugTag,"get audio output format changed ->"+mAudioEncoder.getOutputFormat().toString());
                    mAudioTrack=mStore.addFormat(mAudioEncoder.getOutputFormat());
                }
            }
        }
        return false;
    }

    private void stop(){
        if(isStarted){
            mRecord.stop();
            mRecord.release();
            mRecord=null;
        }
        if(mAudioEncoder!=null){
            mAudioEncoder.stop();
            mAudioEncoder.release();
            mAudioEncoder=null;
        }
        isStarted=false;
    }

}

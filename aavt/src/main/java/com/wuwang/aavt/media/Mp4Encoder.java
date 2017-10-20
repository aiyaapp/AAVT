package com.wuwang.aavt.media;

import android.annotation.TargetApi;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.util.Log;

import com.wuwang.aavt.Aavt;
import com.wuwang.aavt.av.CameraRecorder;

import java.nio.ByteBuffer;

/*
 * Created by Wuwang on 2017/10/20
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class Mp4Encoder {

    private final int TIME_OUT=1000;
    private MediaCodec mVideoEncoder;
    private MediaCodec mAudioEncoder;
    private AudioRecord mAudioRecord;
    private MediaMuxer mMuxer;
    private boolean isMuxStarted=false;
    private CameraRecorder.Configuration mConfig;
    private String mOutputPath;

    private int mRecordBufferSize=0;
    private int mRecordSampleRate=48000;   //音频采样率
    private int mRecordChannelConfig= AudioFormat.CHANNEL_IN_STEREO;   //音频录制通道,默认为立体声
    private int mRecordAudioFormat=AudioFormat.ENCODING_PCM_16BIT; //音频录制格式，默认为PCM16Bit
    private boolean isRecordStarted=false;
    private boolean isRecordVideoStarted=false;
    private boolean isRecordAudioStarted=false;
    private boolean isTryStopAudio=false;


    private MediaCodec.BufferInfo mAudioEncodeBufferInfo;
    private MediaCodec.BufferInfo mVideoEncodeBufferInfo;
    private int mAudioTrack=-1;
    private int mVideoTrack=-1;

    public void setOutputPath(String path){
        this.mOutputPath=path;
    }

    public void setOutputSize(int width,int height){
        this.mConfig=new CameraRecorder.Configuration(width,height);
        this.mOutputWidth=width;
        this.mOutputHeight=height;
    }

    private boolean videoEncodeStep(boolean isEnd){
        if(isEnd){
            mVideoEncoder.signalEndOfInputStream();
        }
        while (true){
            int outputIndex=mVideoEncoder.dequeueOutputBuffer(mVideoEncodeBufferInfo,TIME_OUT);
            if(outputIndex>=0){
                if(isMuxStarted&&mVideoEncodeBufferInfo.size>0&&mVideoEncodeBufferInfo.presentationTimeUs>0){
                    mMuxer.writeSampleData(mVideoTrack,getOutputBuffer(mVideoEncoder,outputIndex),mVideoEncodeBufferInfo);
                }
                mVideoEncoder.releaseOutputBuffer(outputIndex,false);
                if(mVideoEncodeBufferInfo.flags== MediaCodec.BUFFER_FLAG_END_OF_STREAM){
                    Log.d(Aavt.debugTag,"CameraRecorder get video encode end of stream");
                    return true;
                }
            }else if(outputIndex==MediaCodec.INFO_TRY_AGAIN_LATER){
                break;
            }else if(outputIndex==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                Log.e(Aavt.debugTag,"get video output format changed ->"+mVideoEncoder.getOutputFormat().toString());
                mVideoTrack=mMuxer.addTrack(mVideoEncoder.getOutputFormat());
                mMuxer.start();
                isMuxStarted=true;
            }
        }
        return false;
    }

    private boolean audioEncodeStep(boolean isEnd){
        if(isRecordAudioStarted){
            int inputIndex=mAudioEncoder.dequeueInputBuffer(TIME_OUT);
            if(inputIndex>=0){
                ByteBuffer buffer=getInputBuffer(mAudioEncoder,inputIndex);
                buffer.clear();
                long time=(System.currentTimeMillis()-BASE_TIME)*1000;
                int length=mAudioRecord.read(buffer,mRecordBufferSize);
                if(length>=0){
                    mAudioEncoder.queueInputBuffer(inputIndex,0,length,time,
                            isEnd?MediaCodec.BUFFER_FLAG_END_OF_STREAM:0);
                }
            }
            while (true){
                int outputIndex=mAudioEncoder.dequeueOutputBuffer(mAudioEncodeBufferInfo,TIME_OUT);
                if(outputIndex>=0){
                    //todo 第一帧音频时间戳为0的问题
                    if(isMuxStarted&&mAudioEncodeBufferInfo.size>0&&mAudioEncodeBufferInfo.presentationTimeUs>0){
                        mMuxer.writeSampleData(mAudioTrack,getOutputBuffer(mAudioEncoder,outputIndex),mAudioEncodeBufferInfo);
                    }
                    mAudioEncoder.releaseOutputBuffer(outputIndex,false);
                    if(mAudioEncodeBufferInfo.flags==MediaCodec.BUFFER_FLAG_END_OF_STREAM){
                        Log.d(Aavt.debugTag,"CameraRecorder get audio encode end of stream");
                        isTryStopAudio=false;
                        isRecordAudioStarted=false;
                        return true;
                    }
                }else if(outputIndex==MediaCodec.INFO_TRY_AGAIN_LATER){
                    break;
                }else if(outputIndex==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                    Log.e(Aavt.debugTag,"get audio output format changed ->"+mAudioEncoder.getOutputFormat().toString());
                    synchronized (VIDEO_LOCK){
                        mAudioTrack=mMuxer.addTrack(mAudioEncoder.getOutputFormat());
                        isRecordVideoStarted=true;
                    }
                }
            }
        }
        return false;
    }

    public static class Configuration{

        private MediaFormat mAudioFormat;
        private MediaFormat mVideoFormat;

        public Configuration(MediaFormat audio,MediaFormat video){
            this.mAudioFormat=audio;
            this.mVideoFormat=video;
        }

        public Configuration(int width,int height){
            mAudioFormat=MediaFormat.createAudioFormat("audio/mp4a-latm",48000,2);
            mAudioFormat.setInteger(MediaFormat.KEY_BIT_RATE,128000);
            mAudioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);

            mVideoFormat=MediaFormat.createVideoFormat("video/avc",width,height);
            mVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE,24);
            mVideoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,1);
            mVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE,width*height*5);
            mVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        }

        public MediaFormat getAudioFormat(){
            return mAudioFormat;
        }

        public MediaFormat getVideoFormat(){
            return mVideoFormat;
        }

    }

    private ByteBuffer getInputBuffer(MediaCodec codec, int index){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return codec.getInputBuffer(index);
        }else{
            return codec.getInputBuffers()[index];
        }
    }

    private ByteBuffer getOutputBuffer(MediaCodec codec, int index){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return codec.getOutputBuffer(index);
        }else{
            return codec.getOutputBuffers()[index];
        }
    }

}

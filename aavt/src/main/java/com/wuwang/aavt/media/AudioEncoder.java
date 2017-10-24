package com.wuwang.aavt.media;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by wuwang on 2017/10/23.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class AudioEncoder implements IAVCall<AVMsg>{

    private boolean isRecordAudioStarted=false;
    private MediaCodec mAudioEncoder;
    private MediaCodec.BufferInfo mTempInfo;
    private MediaStore<HardCodecData> mMediaStore;
    private HardCodecData mHardCodecData=new HardCodecData();
    private MediaConfig.Audio mConfig;
    private IAVCall<HardCodecData> mCall;
    private boolean isCmdStarted=false;
    private Thread mThread;
    private final Object LOCK=new Object();
    private long timeStamp=-2;
    private boolean isStoreStarted=false;

    public void setIAVCall(IAVCall<HardCodecData> provider){
        this.mCall=provider;
    }

    public void setMediaStore(MediaStore<HardCodecData> store){
        this.mMediaStore=store;
    }

    private boolean audioEncodeStep(boolean isEnd){
        if(isRecordAudioStarted){
            int inputIndex=mAudioEncoder.dequeueInputBuffer(1000);
            if(inputIndex>=0){
                ByteBuffer buffer= CodecUtil.getInputBuffer(mAudioEncoder,inputIndex);
                buffer.clear();
                mHardCodecData.data=buffer;
                mHardCodecData.info=mTempInfo;
                mHardCodecData.info.presentationTimeUs=0;
                if(isEnd){
                    mHardCodecData.info.flags=MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                }
                mCall.onCall(mHardCodecData);
                mHardCodecData.info.presentationTimeUs=(System.currentTimeMillis()-timeStamp)*1000;
                mAudioEncoder.queueInputBuffer(inputIndex,0,mTempInfo.size,
                        mTempInfo.presentationTimeUs,
                        (mTempInfo.flags== MediaCodec.BUFFER_FLAG_END_OF_STREAM)? MediaCodec.BUFFER_FLAG_END_OF_STREAM:0);
            }
            while (true){
                int outputIndex=mAudioEncoder.dequeueOutputBuffer(mTempInfo,1000);
                if(outputIndex>=0){
                    //todo 第一帧音频时间戳为0的问题
                    if(mMediaStore!=null&&isStoreStarted){
                        mHardCodecData.type= HardCodecData.DATA;
                        mHardCodecData.data=CodecUtil.getOutputBuffer(mAudioEncoder,outputIndex);
                        mHardCodecData.info=mTempInfo;
                        mMediaStore.store(mHardCodecData);
                    }
                    mAudioEncoder.releaseOutputBuffer(outputIndex,false);
                }else if(outputIndex==MediaCodec.INFO_TRY_AGAIN_LATER){
                    break;
                }else if(outputIndex==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                    if(mMediaStore!=null){
                        mHardCodecData.type= HardCodecData.ADDTRACK;
                        mHardCodecData.other=mAudioEncoder.getOutputFormat();
                        mMediaStore.store(mHardCodecData);
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void onCall(AVMsg data) {
        if(data.msgType==AVMsg.TYPE_INFO){
            switch (data.msgRet){
                case AVMsg.MSG_CMD_START:
                    this.timeStamp= (long) data.msgData;
                    startCodec();
                    isCmdStarted=true;
                    break;
                case AVMsg.MSG_CMD_END:
                    isCmdStarted=false;
                    try {
                        mThread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    isStoreStarted=false;
                    break;
                case AVMsg.MSG_STORE_START:
                    isStoreStarted=true;
                    break;
            }
        }
    }

    private void startCodec(){
        try {
            mTempInfo=new MediaCodec.BufferInfo();
            if(mConfig==null){
                mConfig=new MediaConfig().mAudio;
                mConfig.profile= MediaCodecInfo.CodecProfileLevel.AACObjectLC;
            }
            MediaFormat format=MediaFormat.createAudioFormat(mConfig.mime,mConfig.sampleRate,mConfig.channelCount);
            format.setInteger(MediaFormat.KEY_AAC_PROFILE,mConfig.profile);
            format.setInteger(MediaFormat.KEY_BIT_RATE,mConfig.bitrate);
            mAudioEncoder=MediaCodec.createEncoderByType(mConfig.mime);
            mAudioEncoder.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
            mAudioEncoder.start();
            isRecordAudioStarted=true;
            mThread=new Thread(new Runnable() {
                @Override
                public void run() {
                    while (isCmdStarted){
                        audioEncodeStep(false);
                    }
                    audioEncodeStep(true);
                    stopCodec();
                }
            });
            mThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopCodec(){
        if(isRecordAudioStarted){
            isRecordAudioStarted=false;
            mAudioEncoder.stop();
            mAudioEncoder.release();
            mAudioEncoder=null;
        }
    }

}

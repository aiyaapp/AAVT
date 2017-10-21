package com.wuwang.aavt.media;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import com.wuwang.aavt.Aavt;

import java.io.IOException;
import java.nio.ByteBuffer;

/*
 * Created by Wuwang on 2017/10/21
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class VideoEncodeFromSurface {

    private MediaCodec mVideoCodec;
    private MediaStore<MediaFormat,HardCodecData> mMediaStore;
    private MediaConfig.Video mConfig=new MediaConfig().mVideo;
    private Surface mEncodeSurface;
    private MediaCodec.BufferInfo mVideoEncodeBufferInfo=new MediaCodec.BufferInfo();
    private HardCodecData mHardCodecData=new HardCodecData();
    private int mVideoTrack=-1;

    public void start(){
        try {
            mConfig.colorFormat=MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;
            MediaFormat format=MediaFormat.createVideoFormat(mConfig.mime,mConfig.width,
                    mConfig.height);
            format.setInteger(MediaFormat.KEY_FRAME_RATE,mConfig.frameRate);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,mConfig.iframe);
            format.setInteger(MediaFormat.KEY_BIT_RATE,mConfig.bitrate);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,mConfig.colorFormat);
            mVideoCodec=MediaCodec.createEncoderByType(mConfig.mime);
            mVideoCodec.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
            mEncodeSurface=mVideoCodec.createInputSurface();
            mVideoCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setOutputSize(int width,int height){
        mConfig.width=width;
        mConfig.height=height;
    }

    public void setMediaConfig(MediaConfig config){
        this.mConfig=config.mVideo;
    }


    private boolean videoEncodeStep(boolean isEnd){
        if(isEnd){
            mVideoCodec.signalEndOfInputStream();
        }
        while (true){
            int outputIndex=mVideoCodec.dequeueOutputBuffer(mVideoEncodeBufferInfo,1000);
            if(outputIndex>=0){
                if(mMediaStore!=null&&mVideoTrack>0){
                    mHardCodecData.data=getOutputBuffer(mVideoCodec,outputIndex);
                    mHardCodecData.info=mVideoEncodeBufferInfo;
                    mHardCodecData.trackIndex=mVideoTrack;
                    mMediaStore.store(mHardCodecData);
                }
                mVideoCodec.releaseOutputBuffer(outputIndex,false);
                if(mVideoEncodeBufferInfo.flags==MediaCodec.BUFFER_FLAG_END_OF_STREAM){
                    Log.d(Aavt.debugTag,"CameraRecorder get video encode end of stream");
                    return true;
                }
            }else if(outputIndex==MediaCodec.INFO_TRY_AGAIN_LATER){
                break;
            }else if(outputIndex==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                Log.e(Aavt.debugTag,"get video output format changed ->"+mVideoCodec.getOutputFormat().toString());
//                mVideoTrack=mMuxer.addTrack(mVideoCodec.getOutputFormat());
//                mMuxer.start();
//                isMuxStarted=true;
                if(mMediaStore!=null){
                    mVideoTrack=mMediaStore.start(mVideoCodec.getOutputFormat());
                }
            }
        }
        return false;
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

package com.wuwang.aavt.media;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.opengl.EGLSurface;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/*
 * Created by Wuwang on 2017/10/26
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class Mp4SurfaceStore extends SurfaceShower{

    private MediaConfig mConfig=new MediaConfig();
    private MediaCodec mVideoEncoder;
    private boolean isEncodeStarted=false;
    private static final int TIME_OUT=1000;

    private HardMediaStore mStore;
    private int mVideoTrack=-1;

    private OnDrawEndListener mListener;
    private long startTime=-1;

    public Mp4SurfaceStore(){
        super.setOnDrawEndListener(new OnDrawEndListener() {
            @Override
            public void onDrawEnd(EGLSurface surface, RenderBean bean) {
                if(bean.timeStamp!=-1){
                    bean.egl.setPresentationTime(surface,bean.timeStamp*1000);
                }else{
                    if(startTime==-1){
                        startTime=bean.textureTime;
                    }
                    bean.egl.setPresentationTime(surface,bean.textureTime-startTime);
                }
                videoEncodeStep(false);
                if(mListener!=null){
                    mListener.onDrawEnd(surface,bean);
                }
            }
        });
    }

    @Override
    public void onCall(RenderBean rb) {
        if (rb.endFlag){
            videoEncodeStep(true);
        }
        super.onCall(rb);
    }

    public void setConfig(MediaConfig config){
        this.mConfig=config;
    }

    public void setStore(HardMediaStore store){
        this.mStore=store;
    }

    @Override
    public void setOutputSize(int width, int height) {
        super.setOutputSize(width, height);
        mConfig.mVideo.width=width;
        mConfig.mVideo.height=height;
    }

    protected MediaFormat convertVideoConfigToFormat(MediaConfig.Video config){
        MediaFormat format=MediaFormat.createVideoFormat(config.mime,config.width,config.height);
        format.setInteger(MediaFormat.KEY_BIT_RATE,config.bitrate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE,config.frameRate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,config.iframe);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        return format;
    }

    protected MediaFormat convertAudioConfigToFormat(MediaConfig.Audio config){
        MediaFormat format=MediaFormat.createAudioFormat(config.mime,config.sampleRate,config.channelCount);
        format.setInteger(MediaFormat.KEY_BIT_RATE,config.bitrate);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        return format;
    }

    private void openVideoEncoder(){
        if(mVideoEncoder==null){
            try {
                MediaFormat format=convertVideoConfigToFormat(mConfig.mVideo);
                mVideoEncoder= MediaCodec.createEncoderByType(mConfig.mVideo.mime);
                mVideoEncoder.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
                super.setSurface(mVideoEncoder.createInputSurface());
                super.setOutputSize(mConfig.mVideo.width,mConfig.mVideo.height);
                mVideoEncoder.start();
                isEncodeStarted=true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void closeVideoEncoder(){
        Log.e("wuwang","closeEncoder");
        if(mVideoEncoder!=null){
            mVideoEncoder.stop();
            mVideoEncoder.release();
            mVideoEncoder=null;
        }
    }

    private synchronized boolean videoEncodeStep(boolean isEnd){
        Log.i("wuwang","videoEncodeStep:"+isEncodeStarted+"/"+isEnd);
        if(isEncodeStarted){
            if(isEnd){
                mVideoEncoder.signalEndOfInputStream();
            }
            MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
            while (true){
                int mOutputIndex=mVideoEncoder.dequeueOutputBuffer(info,TIME_OUT);
                Log.e("wuwang","videoEncodeStep:mOutputIndex="+mOutputIndex);
                if(mOutputIndex>=0){
                    ByteBuffer buffer= CodecUtil.getOutputBuffer(mVideoEncoder,mOutputIndex);
                    if(mStore!=null){
                        mStore.addData(mVideoTrack,buffer,info);
                    }
                    mVideoEncoder.releaseOutputBuffer(mOutputIndex,false);
                    if(info.flags==MediaCodec.BUFFER_FLAG_END_OF_STREAM){
                        closeVideoEncoder();
                        isEncodeStarted=false;
                        Log.e("wuwang","videoEncodeStep: MediaCodec.BUFFER_FLAG_END_OF_STREAM ");
                        break;
                    }
                }else if(mOutputIndex== MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                    MediaFormat format=mVideoEncoder.getOutputFormat();
                    if(mStore!=null){
                        mVideoTrack=mStore.addFormat(format);
                    }
                }else if(mOutputIndex== MediaCodec.INFO_TRY_AGAIN_LATER&&!isEnd){
                    break;
                }
            }
        }
        return false;
    }


    @Override
    public void open() {
        openVideoEncoder();
        super.open();
    }

    @Override
    public void close() {
        super.close();
        videoEncodeStep(true);
        startTime=-1;
    }

    @Override
    public void setOnDrawEndListener(OnDrawEndListener listener) {
        this.mListener=listener;
    }

    @Override
    public void setSurface(Object surface) {}


}

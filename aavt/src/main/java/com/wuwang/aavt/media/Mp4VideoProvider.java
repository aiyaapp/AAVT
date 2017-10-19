package com.wuwang.aavt.media;

import android.annotation.TargetApi;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import com.wuwang.aavt.Aavt;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;

/**
 * Created by wuwang on 2017/10/20.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class Mp4VideoProvider implements IVideoProvider,SurfaceTexture.OnFrameAvailableListener {

    private Semaphore mSourceSem;
    private MediaCodec mVideoCodec;
    private boolean mDecodeThreadFlag=false;
    private Thread mDecodeThread;
    private int TIME_OUT=10000;
    private int mVideoTrack;
    private boolean isVideoExtractorEnd;
    private boolean isUserWantToStop=false;

    //test
    private MediaExtractor mExtractor;
    private MediaCodec.BufferInfo mVideoDecoderBufferInfo;

    private Semaphore mDecodeSem;

    private String mime="video/avc";

    @Override
    public void onCall(AVMsg avMsg) {
        mDecodeSem.release();
    }

    @Override
    public Point start(SurfaceTexture texture, Semaphore sem) {
        this.mSourceSem=sem;
        mDecodeSem=new Semaphore(1);
        mDecodeThreadFlag=true;
        mVideoDecoderBufferInfo=new MediaCodec.BufferInfo();
        mDecodeThread=new Thread(new Runnable() {
            @Override
            public void run() {
                decodeRunnable();
            }
        });
        mExtractor=new MediaExtractor();
        try {
            mExtractor.setDataSource("/mnt/sdcard/temp.mp4");
            for (int i=0;i<mExtractor.getTrackCount();i++){
                MediaFormat format=mExtractor.getTrackFormat(i);
                mime=format.getString(MediaFormat.KEY_MIME);
                if(mime.startsWith("video")){
                    try {
                        this.mVideoTrack=i;
                        mVideoCodec=MediaCodec.createDecoderByType(mime);
                        mVideoCodec.configure(format,new Surface(texture),null,0);
                        mVideoCodec.start();
                        texture.setOnFrameAvailableListener(this);
                        mDecodeThread.start();
                        return new Point(format.getInteger(MediaFormat.KEY_WIDTH),
                                format.getInteger(MediaFormat.KEY_HEIGHT));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void decodeRunnable(){
        while (mDecodeThreadFlag&&!videoDecodeStep());
    }

    //视频解码到SurfaceTexture上，以供后续处理。返回值为是否是最后一帧视频
    private boolean videoDecodeStep(){
        int mInputIndex=mVideoCodec.dequeueInputBuffer(TIME_OUT);
        if(mInputIndex>=0){
            ByteBuffer buffer=getInputBuffer(mVideoCodec,mInputIndex);
            buffer.clear();
            mExtractor.selectTrack(mVideoTrack);
            int ret = mExtractor.readSampleData(buffer, 0);
            if (ret != -1) {
                long mVideoStopTimeStamp=mExtractor.getSampleTime();
                Log.d(Aavt.debugTag,"mVideoStopTimeStamp:"+mVideoStopTimeStamp);
                mVideoCodec.queueInputBuffer(mInputIndex, 0, ret, mVideoStopTimeStamp, mExtractor.getSampleFlags());
            }
            isVideoExtractorEnd = !mExtractor.advance();
        }
        while (true){
            int mOutputIndex=mVideoCodec.dequeueOutputBuffer(mVideoDecoderBufferInfo,TIME_OUT);
            if(mOutputIndex>=0){
//                try {
//                    Log.d(Aavt.debugTag," mDecodeSem.acquire ");
//                    if(!isUserWantToStop){
//                        mDecodeSem.acquire();
//                    }
//                    Log.d(Aavt.debugTag," mDecodeSem.acquire end ");
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                mVideoCodec.releaseOutputBuffer(mOutputIndex,true);
            }else if(mOutputIndex== MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                //MediaFormat format=mVideoDecoder.getOutputFormat();
            }else if(mOutputIndex== MediaCodec.INFO_TRY_AGAIN_LATER){
                break;
            }
        }
        return isVideoExtractorEnd||isUserWantToStop;
    }

    @Override
    public void stop() {
        mDecodeThreadFlag=false;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mSourceSem.drainPermits();
        mSourceSem.release();
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

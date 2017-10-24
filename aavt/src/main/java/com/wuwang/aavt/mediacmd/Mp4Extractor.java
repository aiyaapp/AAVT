package com.wuwang.aavt.mediacmd;

import android.annotation.TargetApi;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import com.wuwang.aavt.Aavt;
import com.wuwang.aavt.media.CodecUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;

/*
 * Created by Wuwang on 2017/10/24
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class Mp4Extractor extends AActuator {

    private MediaExtractor mExtractor;
    private String mPath;
    private MediaCodec mVideoDecoder;
    private final int TIME_OUT = 1000;
    private final Object Extractor_LOCK=new Object();
    private int mVideoDecoderTrack;
    private long mVideoStopTimeStamp;
    private boolean isVideoExtractorEnd;
    private MediaCodec.BufferInfo mVideoDecoderBufferInfo=new MediaCodec.BufferInfo();
    private boolean isUserWantToStop=true;
    private SurfaceTexture mPreviewTexture;
    private Semaphore frameSem;
    private Point mVideoSize=new Point();
    private Thread mVideoDecodeThread;
    private boolean isSourceOpened=false;
    private Semaphore mDecodeSem;
    private HardMediaStore mStore;
    private boolean isVideoStart=false;
    private long timeStamp=0;
    private MediaCodec.BufferInfo bufferInfo=new MediaCodec.BufferInfo();

    public Mp4Extractor(){

    }

    public Mp4Extractor(HardMediaStore store){
        this.mStore=store;
    }

    @Override
    public void execute(Cmd cmd) {
        if(cmd.cmd==Cmd.CMD_VIDEO_OPEN_SOURCE){
            try {
                Log.e("wuwang","mp4 try open");
                openSource();
                frameSem=new Semaphore(0);
                mDecodeSem=new Semaphore(0);
                mPreviewTexture=(SurfaceTexture) cmd.obj;
                cmd.callback(Cmd.RET_VALUE_SUCCESS,"open video source ok",mVideoSize);
                startVideoDecode();
                isUserWantToStop=false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else if(cmd.cmd==Cmd.CMD_VIDEO_CLOSE_SOURCE){
            stopVideoDecode();
        }else if(cmd.cmd==Cmd.CMD_VIDEO_FRAME_DATA){
            mDecodeSem.release();
            try {
                frameSem.acquire();
                cmd.retObj=bufferInfo;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }else if(cmd.cmd==Cmd.CMD_AUDIO_OPEN_ENCODE){

        }else if(cmd.cmd==Cmd.CMD_AUDIO_CLOSE_ENCODE){

        }
    }

    private void openSource() throws IOException {
        synchronized (Extractor_LOCK){
            if(mPath!=null&&!isSourceOpened){
                MediaMetadataRetriever mMetRet=new MediaMetadataRetriever();
                mMetRet.setDataSource(mPath);
                mExtractor=new MediaExtractor();
                mExtractor.setDataSource(mPath);
                int tracks=mExtractor.getTrackCount();
                for (int i=0;i<tracks;i++){
                    MediaFormat format=mExtractor.getTrackFormat(i);
                    String mime=format.getString(MediaFormat.KEY_MIME);
                    if(mime.startsWith("video")){
                        mVideoDecoderTrack=i;
                        int videoRotation=0;
                        String rotation=mMetRet.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
                        if(rotation!=null){
                            videoRotation=Integer.valueOf(rotation);
                        }
                        if(videoRotation%180!=0){
                            mVideoSize.y=format.getInteger(MediaFormat.KEY_WIDTH);
                            mVideoSize.x=format.getInteger(MediaFormat.KEY_HEIGHT);
                        }else{
                            mVideoSize.x=format.getInteger(MediaFormat.KEY_WIDTH);
                            mVideoSize.y=format.getInteger(MediaFormat.KEY_HEIGHT);
                        }
                    }else if(mime.startsWith("audio")){

                    }
                }
                isSourceOpened=true;
            }
        }
    }

    private void startVideoDecode(){
        if(!isVideoStart){
            mVideoDecodeThread=new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        isVideoStart=true;
                        MediaFormat format=mExtractor.getTrackFormat(mVideoDecoderTrack);
                        mVideoDecoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
                        mVideoDecoder.configure(format,new Surface(mPreviewTexture),null,0);
                        mVideoDecoder.start();
                        while (!videoDecodeStep());

                        if(mVideoDecoderBufferInfo.flags!=MediaCodec.BUFFER_FLAG_END_OF_STREAM){
                            Log.e("wuwang","end of decode");
                            try {
                                mDecodeSem.acquire();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            mVideoDecoderBufferInfo.flags=MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                            frameSem.release();
                        }

                        mVideoDecoder.stop();
                        mVideoDecoder.release();
                        isVideoStart=false;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        mVideoDecodeThread.start();
    }

    private void stopVideoDecode(){
        if(frameSem!=null){
            frameSem.release();
        }
        if(!isUserWantToStop){
            isUserWantToStop=true;
            mExtractor.release();
            mExtractor=null;
            isSourceOpened=false;
        }
    }

    private boolean videoDecodeStep(){
        int mInputIndex=mVideoDecoder.dequeueInputBuffer(TIME_OUT);
        if(mInputIndex>=0){
            ByteBuffer buffer= CodecUtil.getInputBuffer(mVideoDecoder,mInputIndex);
            buffer.clear();
            synchronized (Extractor_LOCK) {
                mExtractor.selectTrack(mVideoDecoderTrack);
                int ret = mExtractor.readSampleData(buffer, 0);
                if (ret != -1) {
                    mVideoStopTimeStamp=mExtractor.getSampleTime();
                    Log.d(Aavt.debugTag,"mVideoStopTimeStamp:"+mVideoStopTimeStamp+"/"+mExtractor.getSampleFlags());
                    mVideoDecoder.queueInputBuffer(mInputIndex, 0, ret, mVideoStopTimeStamp, mExtractor.getSampleFlags());
                }
                isVideoExtractorEnd = !mExtractor.advance();
            }
        }
        while (true){
            int mOutputIndex=mVideoDecoder.dequeueOutputBuffer(mVideoDecoderBufferInfo,TIME_OUT);
            if(mOutputIndex>=0){
                try {
                    Log.d(Aavt.debugTag," mDecodeSem.acquire ");
                    if(!isUserWantToStop){
                        mDecodeSem.acquire();
                    }
                    Log.d(Aavt.debugTag," mDecodeSem.acquire end ");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mVideoDecoder.releaseOutputBuffer(mOutputIndex,true);
                bufferInfo.set(mVideoDecoderBufferInfo.offset,mVideoDecoderBufferInfo.size,
                        mVideoDecoderBufferInfo.presentationTimeUs,mVideoDecoderBufferInfo.flags);
                frameSem.release();
            }else if(mOutputIndex== MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                //MediaFormat format=mVideoDecoder.getOutputFormat();
            }else if(mOutputIndex== MediaCodec.INFO_TRY_AGAIN_LATER){
                break;
            }
        }
        return isVideoExtractorEnd||isUserWantToStop;
    }



    public void setDataSource(String data){
        this.mPath=data;
    }

}

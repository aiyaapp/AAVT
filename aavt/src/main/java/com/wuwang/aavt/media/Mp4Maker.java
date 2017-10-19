package com.wuwang.aavt.media;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import com.wuwang.aavt.Aavt;
import com.wuwang.aavt.core.Renderer;
import com.wuwang.aavt.egl.EGLContextAttrs;
import com.wuwang.aavt.egl.EGLHelper;
import com.wuwang.aavt.egl.EGLSurfaceAttrs;
import com.wuwang.aavt.gl.BaseFilter;
import com.wuwang.aavt.gl.Filter;
import com.wuwang.aavt.gl.FrameBuffer;
import com.wuwang.aavt.utils.MatrixUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class Mp4Maker {

    private final int TIME_OUT=1000;
    private boolean isDebug=true;

    private MediaCodec mVideoEncoder;
    private MediaCodec mAudioEncoder;
//    private AudioRecord mAudioRecord;
    private Stuffer mAudioStuffer;
    private MediaMuxer mMuxer;


    private SurfaceTexture mInputTexture;
    private Surface mOutputSurface;
    private Surface mEncodeSurface;

    private Configuration mConfig;
    private String mOutputPath;

    private MediaCodec.BufferInfo mAudioEncodeBufferInfo;
    private MediaCodec.BufferInfo mVideoEncodeBufferInfo;
    private int mAudioTrack=-1;
    private int mVideoTrack=-1;

    private boolean mGLThreadFlag=false;
    private Thread mGLThread;
    private WrapRenderer mRenderer;
    private Semaphore mShowSem;
    private boolean isMuxStarted=false;
    private int mInputTextureId;
    private EGLSurface mEGLEncodeSurface=null;
    private EGLSurface mEGLPreviewSurface=null;

    private int mPreviewWidth=0;                //预览的宽度
    private int mPreviewHeight=0;               //预览的高度
    private int mOutputWidth=0;                 //输出的宽度
    private int mOutputHeight=0;                //输出的高度
    private int mSourceWidth=0;
    private int mSourceHeight=0;

    private boolean isRecordStarted=false;
    private boolean isRecordVideoStarted=false;
    private boolean isRecordAudioStarted=false;
    private boolean isTryStopAudio=false;

    private boolean isPreviewStarted=false;

    private float[] mRecMatrix=MatrixUtils.getOriginalMatrix();
    private float[] mPreMatrix=MatrixUtils.getOriginalMatrix();

    private Thread mAudioThread;
    private final Object VIDEO_LOCK=new Object();
    private final Object REC_LOCK=new Object();

    private final static long BASE_TIME=System.currentTimeMillis();

    public Mp4Maker(){
        mShowSem=new Semaphore(0);
        mAudioEncodeBufferInfo=new MediaCodec.BufferInfo();
        mVideoEncodeBufferInfo=new MediaCodec.BufferInfo();
    }

    public void setOutputPath(String path){
        this.mOutputPath=path;
    }

    public void setOutputSize(int width,int height){
        this.mConfig=new Configuration(width,height);
        this.mOutputWidth=width;
        this.mOutputHeight=height;
        MatrixUtils.getMatrix(mRecMatrix,MatrixUtils.TYPE_CENTERCROP,mSourceWidth,mSourceHeight,mOutputWidth,mOutputHeight);
    }

    public void setPreviewSize(int width,int height){
        this.mPreviewWidth=width;
        this.mPreviewHeight=height;
        MatrixUtils.getMatrix(mPreMatrix,MatrixUtils.TYPE_CENTERCROP,mSourceWidth,mSourceHeight,mPreviewWidth,mPreviewHeight);
    }

    public void setSourceSize(int width,int height){
        this.mSourceWidth=width;
        this.mSourceHeight=height;
        MatrixUtils.getMatrix(mPreMatrix,MatrixUtils.TYPE_CENTERCROP,mSourceWidth,mSourceHeight,mPreviewWidth,mPreviewHeight);
        MatrixUtils.getMatrix(mRecMatrix,MatrixUtils.TYPE_CENTERCROP,mSourceWidth,mSourceHeight,mOutputWidth,mOutputHeight);
    }

    public void setAudioStuffer(Stuffer stuffer){
        this.mAudioStuffer=stuffer;
    }

    public SurfaceTexture createInputSurfaceTexture(){
        mInputTextureId= EGLHelper.createTextureID();
        mInputTexture=new SurfaceTexture(mInputTextureId);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mInputTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        mShowSem.release();
                    }
                });
            }
        });
        return mInputTexture;
    }

    public void setConfiguration(Configuration config){
        this.mConfig=config;
    }

    public void setOutputSurface(Surface surface){
        this.mOutputSurface=surface;
        if(this.mOutputSurface==null){
            mEGLPreviewSurface=null;
            stopPreview();
        }
    }

    public void setRenderer(Renderer renderer){
        mRenderer=new WrapRenderer(renderer);
    }

    public void create(){
        mShowSem.drainPermits();
        mGLThreadFlag=true;
        mGLThread=new Thread(mGLRunnable);
        mGLThread.start();
    }

    public void destroy()  throws InterruptedException {
        synchronized (REC_LOCK){
            mGLThreadFlag=false;
            mShowSem.release();
            if(mGLThread!=null&&mGLThread.isAlive()){
                mGLThread.join();
                mGLThread=null;
            }
            Log.d(Aavt.debugTag,"CameraRecorder stopPreview");
        }
    }

    public void startPreview(){
        isPreviewStarted=true;
    }

    public void stopPreview(){
        isPreviewStarted=false;
    }

    public void startRecord() throws IOException {
        synchronized (REC_LOCK){
            isRecordStarted=true;
            MediaFormat audioFormat=mConfig.getAudioFormat();
            mAudioEncoder=MediaCodec.createEncoderByType(audioFormat.getString(MediaFormat.KEY_MIME));
            mAudioEncoder.configure(audioFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
            MediaFormat videoFormat=mConfig.getVideoFormat();
            mVideoEncoder=MediaCodec.createEncoderByType(videoFormat.getString(MediaFormat.KEY_MIME));
            //此处不能用mOutputSurface，会configure失败
            mVideoEncoder.configure(videoFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
            mEncodeSurface=mVideoEncoder.createInputSurface();

            mAudioEncoder.start();
            mVideoEncoder.start();
            mMuxer=new MediaMuxer(mOutputPath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
//        buffer=new byte[bufferSize];
            if(mAudioStuffer!=null){
                mAudioThread=new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mAudioStuffer.start();
                        while (!audioEncodeStep(isTryStopAudio));
                        mAudioStuffer.stop();
                    }
                });
                mAudioThread.start();
            }else{
                isRecordVideoStarted=true;
            }
            isRecordAudioStarted=true;
        }
    }

    public void stopRecord() throws InterruptedException {
        synchronized (REC_LOCK){
            if(isRecordStarted){
                isTryStopAudio=true;
                if(isRecordAudioStarted){
                    if(mAudioThread!=null){
                        mAudioThread.join();
                    }
                    isRecordAudioStarted=false;
                }
                synchronized (VIDEO_LOCK){
                    if(isRecordVideoStarted){
                        mEGLEncodeSurface=null;
                        videoEncodeStep(true);
                    }
                    isRecordVideoStarted=false;
                }
                mAudioEncoder.stop();
                mAudioEncoder.release();
                mVideoEncoder.stop();
                mVideoEncoder.release();
                try {
                    if(isMuxStarted){
                        isMuxStarted=false;
                        mMuxer.stop();
                        mMuxer.release();
                    }
                }catch (IllegalStateException e){
                    e.printStackTrace();
                    File file=new File(mOutputPath);
                    if(file.exists()&&file.delete()){
                        Log.d(Aavt.debugTag,"delete error file :"+mOutputPath);
                    }
                }


                mAudioEncoder=null;
                mVideoEncoder=null;
                mMuxer=null;

                mAudioTrack=-1;
                mVideoTrack=-1;

                isRecordStarted=false;
            }
        }
    }

    private Runnable mGLRunnable=new Runnable() {
        @Override
        public void run() {
            EGLHelper egl=new EGLHelper();
            EGLConfig eglConfig=egl.getConfig(new EGLSurfaceAttrs());
            EGLSurface eglSurface=egl.createWindowSurface(eglConfig,new SurfaceTexture(1));
            EGLContext eglContext=egl.createContext(eglConfig, EGL14.EGL_NO_CONTEXT,new EGLContextAttrs());
            boolean ret=egl.makeCurrent(eglSurface,eglSurface,eglContext);
            if(!ret){
                Log.e(Aavt.debugTag,"CameraRecorder GLThread exit : createGLES failed");
                return;
            }
            if(mRenderer==null){
                mRenderer=new WrapRenderer(null);
            }
            mRenderer.setFlag(WrapRenderer.TYPE_CAMERA);
            mRenderer.create();
            int[] t=new int[1];
            GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING,t,0);
            mRenderer.sizeChanged(mSourceWidth,mSourceHeight);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,t[0]);

            Filter mShowFilter=new BaseFilter();
            mShowFilter.create();
            mShowFilter.sizeChanged(mSourceWidth,mSourceHeight);

            FrameBuffer tempFrameBuffer=new FrameBuffer();
            while (mGLThreadFlag){
                try {
                    mShowSem.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(mGLThreadFlag){
                    long time=(System.currentTimeMillis()-BASE_TIME)*1000;
                    mInputTexture.updateTexImage();
                    mInputTexture.getTransformMatrix(mRenderer.getTextureMatrix());
                    if (isPreviewStarted) {
                        if(mEGLPreviewSurface==null){
                            mEGLPreviewSurface=egl.createWindowSurface(eglConfig,mOutputSurface);
                        }
                        egl.makeCurrent(mEGLPreviewSurface,mEGLPreviewSurface,eglContext);
                        tempFrameBuffer.bindFrameBuffer(mSourceWidth,mSourceHeight);
                        GLES20.glViewport(0,0,mSourceWidth,mSourceHeight);
                        mRenderer.draw(mInputTextureId);
                        tempFrameBuffer.unBindFrameBuffer();
                        GLES20.glViewport(0,0,mPreviewWidth,mPreviewHeight);
                        log(Arrays.toString(mPreMatrix));
                        mShowFilter.setVertexMatrix(mPreMatrix);
                        mShowFilter.draw(tempFrameBuffer.getCacheTextureId());
                        egl.swapBuffers(mEGLPreviewSurface);
                    }
                    synchronized (VIDEO_LOCK){
                        if(isRecordVideoStarted){
                            if(mEGLEncodeSurface==null){
                                mEGLEncodeSurface=egl.createWindowSurface(eglConfig,mEncodeSurface);
                            }
                            egl.makeCurrent(mEGLEncodeSurface,mEGLEncodeSurface,eglContext);
                            if(!isPreviewStarted){
                                tempFrameBuffer.bindFrameBuffer(mSourceWidth,mSourceHeight);
                                GLES20.glViewport(0,0,mSourceWidth,mSourceHeight);
                                mRenderer.draw(mInputTextureId);
                                tempFrameBuffer.unBindFrameBuffer();
                            }
                            GLES20.glViewport(0,0,mConfig.getVideoFormat().getInteger(MediaFormat.KEY_WIDTH),
                                    mConfig.getVideoFormat().getInteger(MediaFormat.KEY_HEIGHT));
                            mShowFilter.setVertexMatrix(mRecMatrix);
                            mShowFilter.draw(tempFrameBuffer.getCacheTextureId());
                            egl.setPresentationTime(mEGLEncodeSurface,time*1000);
                            videoEncodeStep(false);
                            egl.swapBuffers(mEGLEncodeSurface);
                        }
                    }
                }
            }
            egl.destroyGLES(eglSurface,eglContext);
        }
    };

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
                if(mVideoEncodeBufferInfo.flags==MediaCodec.BUFFER_FLAG_END_OF_STREAM){
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
                AVFrame audioFrame=new AVFrame();
                audioFrame.mData=buffer;
                int length=mAudioStuffer.stuff(audioFrame);
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

    private void log(String info){
        if(isDebug){
            Log.d("Mp4Maker",info);
        }
    }

}

package com.wuwang.aavt.media;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import com.wuwang.aavt.Aavt;
import com.wuwang.aavt.egl.EGLHelper;
import com.wuwang.aavt.gl.BaseFilter;
import com.wuwang.aavt.gl.Filter;
import com.wuwang.aavt.utils.MatrixUtils;

import java.io.IOException;

/*
 * Created by Wuwang on 2017/10/20
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MediaCodecSurfaceVideoConsumer extends ASurfaceVideoConsumer{

    private MediaCodec mVideoCodec;
    private String mime="video/avc";
    private EGLSurface mEglSurface;
    private Filter mFilter;
    private MediaCodec.BufferInfo mVideoEncodeBufferInfo;

    public MediaCodecSurfaceVideoConsumer(){
        mVideoEncodeBufferInfo=new MediaCodec.BufferInfo();
    }

    @Override
    public void onSurfaceFrame(EGLHelper egl, int width, int height, int texture) {
        if(mOutputSurface!=null&&texture!=-1){
            if(mEglSurface==null){
                mEglSurface=egl.createWindowSurface(mOutputSurface);
                mFilter=new BaseFilter();
                mFilter.create();
                mFilter.sizeChanged(width, height);
                MatrixUtils.getMatrix(mFilter.getVertexMatrix(),MatrixUtils.TYPE_CENTERCROP,width,height,
                        mOutputWidth,mOutputHeight);
                MatrixUtils.flip(mFilter.getVertexMatrix(),false,true);
            }
            egl.makeCurrent(mEglSurface);
            GLES20.glViewport(0,0,mOutputWidth,mOutputHeight);
            mFilter.draw(texture);
            egl.swapBuffers(mEglSurface);
        }
    }

    @Override
    public void setOutputSurface(Object surface) {

    }

    @Override
    public void start() {
        if(mVideoCodec==null){
            try {
                MediaFormat format=MediaFormat.createVideoFormat(mime,mOutputWidth,mOutputHeight);
                format.setInteger(MediaFormat.KEY_FRAME_RATE,24);
                format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,1);
                format.setInteger(MediaFormat.KEY_BIT_RATE,mOutputWidth*mOutputHeight*5);
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                mVideoCodec=MediaCodec.createEncoderByType(mime);
                mVideoCodec.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
                mOutputSurface=mVideoCodec.createInputSurface();
                mVideoCodec.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void stop() {
        if(mVideoCodec!=null){
            mVideoCodec.stop();
            mVideoCodec=null;
        }
    }

    private boolean videoEncodeStep(boolean isEnd){
        if(isEnd){
            mVideoCodec.signalEndOfInputStream();
        }
        while (true){
            int outputIndex=mVideoCodec.dequeueOutputBuffer(mVideoEncodeBufferInfo,TIME_OUT);
            if(outputIndex>=0){
                if(isMuxStarted&&mVideoEncodeBufferInfo.size>0&&mVideoEncodeBufferInfo.presentationTimeUs>0){
                    mMuxer.writeSampleData(mVideoTrack,getOutputBuffer(mVideoCodec,outputIndex),mVideoEncodeBufferInfo);
                }
                mVideoCodec.releaseOutputBuffer(outputIndex,false);
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

}

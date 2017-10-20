package com.wuwang.aavt.media;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Build;
import android.util.Log;

import com.wuwang.aavt.Aavt;
import com.wuwang.aavt.gl.BaseFilter;
import com.wuwang.aavt.gl.Filter;
import com.wuwang.aavt.utils.MatrixUtils;

import static android.net.sip.SipErrorCode.TIME_OUT;

/**
 * Created by wuwang on 2017/10/21.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public abstract class SurfaceEncodeProcessor implements IAVProcessor<SurfaceProcessBean> {

    private Object mOutputSurface;
    private EGLSurface mEglSurface;
    private Filter mFilter;
    private int mOutputWidth;
    private int mOutputHeight;

    private MediaCodec mVideoEncoder;
    private MediaCodec.BufferInfo mVideoEncodeBufferInfo=new MediaCodec.BufferInfo();

    @Override
    public void startProcess() {

    }

    @Override
    public void onProcess(SurfaceProcessBean data) {
        if(mOutputSurface!=null&&data.texture!=-1){
            if(mEglSurface==null){
                mEglSurface=data.egl.createWindowSurface(mOutputSurface);
                mFilter=new BaseFilter();
                mFilter.create();
                mFilter.sizeChanged(data.width, data.height);
                MatrixUtils.getMatrix(mFilter.getVertexMatrix(),MatrixUtils.TYPE_CENTERCROP,data.width,data.height,
                        mOutputWidth,mOutputHeight);
//                MatrixUtils.flip(mFilter.getVertexMatrix(),false,true);
            }
            data.egl.makeCurrent(mEglSurface);
            GLES20.glViewport(0,0,mOutputWidth,mOutputHeight);
            mFilter.draw(data.texture);
            data.egl.swapBuffers(mEglSurface);
        }
    }

    @Override
    public void stopProcess() {

    }

//    private boolean videoEncodeStep(boolean isEnd){
//        if(isEnd){
//            mVideoEncoder.signalEndOfInputStream();
//        }
//        while (true){
//            int outputIndex=mVideoEncoder.dequeueOutputBuffer(mVideoEncodeBufferInfo,TIME_OUT);
//            if(outputIndex>=0){
//                if(isMuxStarted()&&mVideoEncodeBufferInfo.size>0&&mVideoEncodeBufferInfo.presentationTimeUs>0){
//                    mMuxer.writeSampleData(mVideoTrack,getOutputBuffer(mVideoEncoder,outputIndex),mVideoEncodeBufferInfo);
//                }
//                mVideoEncoder.releaseOutputBuffer(outputIndex,false);
//                if(mVideoEncodeBufferInfo.flags== MediaCodec.BUFFER_FLAG_END_OF_STREAM){
//                    Log.d(Aavt.debugTag,"CameraRecorder get video encode end of stream");
//                    return true;
//                }
//            }else if(outputIndex==MediaCodec.INFO_TRY_AGAIN_LATER){
//                break;
//            }else if(outputIndex==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
//                Log.e(Aavt.debugTag,"get video output format changed ->"+mVideoEncoder.getOutputFormat().toString());
//                mVideoTrack=mMuxer.addTrack(mVideoEncoder.getOutputFormat());
//                mMuxer.start();
//                isMuxStarted=true;
//            }
//        }
//        return false;
//    }

    protected abstract boolean isMuxStarted();

}

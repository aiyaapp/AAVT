package com.wuwang.aavt.mediacmd;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.wuwang.aavt.Aavt;
import com.wuwang.aavt.core.IObserver;
import com.wuwang.aavt.gl.BaseFilter;
import com.wuwang.aavt.gl.Filter;
import com.wuwang.aavt.media.CodecUtil;
import com.wuwang.aavt.media.MediaConfig;
import com.wuwang.aavt.utils.MatrixUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

/*
 * Created by Wuwang on 2017/10/24
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class SurfaceEncodeActuator extends AActuator implements IObserver<Cmd> {

    private EGLSurface mEncodeSurface;
    private boolean isEncodeStarted=false;
    private Filter mFilter;
    private Surface mSurface;
    private MediaConfig.Video mConfig=new MediaConfig().mVideo;
    private HardMediaStore mMuxer;
    private MediaCodec.BufferInfo mVideoEncoderBufferInfo=new MediaCodec.BufferInfo();
    private MediaCodec mVideoEncoder;
    private final int TIME_OUT=1000;
    private int trackIndex;
    private long startTime=0;

    public SurfaceEncodeActuator(HardMediaStore store){
        this.mMuxer=store;
    }

    @Override
    public void execute(Cmd cmd) {
        if(cmd.cmd==Cmd.CMD_VIDEO_OPEN_ENCODE){
            if(mMuxer==null){
                cmd.errorCallback(Cmd.ERROR_CMD_EXEC_FAILED,"Please set HardMediaStore to SurfaceEncodeActuator");
                return;
            }
            try {
                openEncoder();
            } catch (IOException e) {
                e.printStackTrace();
                closeEncoder();
            }
            startTime=System.currentTimeMillis();
        }if (cmd.cmd==Cmd.CMD_VIDEO_CLOSE_ENCODE){
            videoEncodeStep(true);
        }else{
            if(mSuccessor!=null){
                mSuccessor.execute(cmd);
            }
        }
    }

    public void setConfig(MediaConfig config){
        this.mConfig=config.mVideo;
    }

    @Override
    public void onCall(Cmd cmd) {
        if(cmd.cmd==Cmd.CMD_VIDEO_RENDER&&cmd.retType==Cmd.RET_TYPE_DATA){
            RenderBean rb= (RenderBean) cmd.retObj;
            draw(rb);
        }
    }

    private void draw(RenderBean bean){
        if(bean.endFlag&&mEncodeSurface!=null){
            bean.egl.destroySurface(mEncodeSurface);
            mEncodeSurface=null;
        }else if(isEncodeStarted){
            if(mEncodeSurface==null){
                mEncodeSurface=bean.egl.createWindowSurface(mSurface);
                mFilter=new BaseFilter();
                mFilter.create();
                mFilter.sizeChanged(bean.sourceWidth, bean.sourceHeight);
                MatrixUtils.getMatrix(mFilter.getVertexMatrix(),MatrixUtils.TYPE_CENTERCROP,bean.sourceWidth,bean.sourceHeight,
                        mConfig.width,mConfig.height);
            }
            bean.egl.makeCurrent(mEncodeSurface);
            GLES20.glViewport(0,0,mConfig.width,mConfig.height);
            mFilter.draw(bean.textureId);
            if(bean.timeStamp>=0){
                bean.egl.setPresentationTime(mEncodeSurface,bean.timeStamp*1000);
            }else{
                bean.egl.setPresentationTime(mEncodeSurface,(System.currentTimeMillis()-startTime)*1000000);
            }
            videoEncodeStep(false);
            bean.egl.swapBuffers(mEncodeSurface);
        }
    }

    protected MediaFormat convertConfigToFormat(MediaConfig.Video config){
        MediaFormat format=MediaFormat.createVideoFormat(config.mime,config.width,config.height);
        format.setInteger(MediaFormat.KEY_BIT_RATE,config.bitrate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE,config.frameRate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,config.iframe);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        return format;
    }

    private void openEncoder() throws IOException {
        if(mVideoEncoder==null){
            MediaFormat format=convertConfigToFormat(mConfig);
            mVideoEncoder=MediaCodec.createEncoderByType(mConfig.mime);
            mVideoEncoder.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
            mSurface=mVideoEncoder.createInputSurface();
            mVideoEncoder.start();
            isEncodeStarted=true;
        }
    }

    private void closeEncoder(){
        Log.e("wuwang","closeEncoder");
        if(mVideoEncoder!=null){
            mVideoEncoder.stop();
            mVideoEncoder.release();
            mVideoEncoder=null;
        }
    }

    private synchronized boolean videoEncodeStep(boolean isEnd){
        Log.e("wuwang","videoEncodeStep:"+isEncodeStarted);
        if(isEncodeStarted){
            if(isEnd){
                mVideoEncoder.signalEndOfInputStream();
            }
            while (true){
                int mOutputIndex=mVideoEncoder.dequeueOutputBuffer(mVideoEncoderBufferInfo,TIME_OUT);
                Log.e("wuwang","videoEncodeStep:mOutputIndex="+mOutputIndex);
                if(mOutputIndex>=0){
                    ByteBuffer buffer= CodecUtil.getOutputBuffer(mVideoEncoder,mOutputIndex);
                    if(mMuxer!=null){
                        mMuxer.addData(trackIndex,buffer,mVideoEncoderBufferInfo);
                    }
                    mVideoEncoder.releaseOutputBuffer(mOutputIndex,false);
                    if(mVideoEncoderBufferInfo.flags==MediaCodec.BUFFER_FLAG_END_OF_STREAM){
                        closeEncoder();
                        isEncodeStarted=false;
                        break;
                    }
                }else if(mOutputIndex== MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                    MediaFormat format=mVideoEncoder.getOutputFormat();
                    if(mMuxer!=null){
                        trackIndex=mMuxer.addFormat(format);
                    }
                }else if(mOutputIndex== MediaCodec.INFO_TRY_AGAIN_LATER){
                    break;
                }
            }
        }
        return false;
    }


}

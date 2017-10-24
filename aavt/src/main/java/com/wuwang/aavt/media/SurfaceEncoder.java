package com.wuwang.aavt.media;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Build;
import android.util.Log;

import com.wuwang.aavt.gl.BaseFilter;
import com.wuwang.aavt.gl.Filter;
import com.wuwang.aavt.utils.MatrixUtils;

import java.io.IOException;

/**
 * Created by wuwang on 2017/10/22.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class SurfaceEncoder implements IProcessor<SurfaceTextureProcess.GLBean,Object>,IAVCall<AVMsg> {

    private boolean isShow=false;
    private EGLSurface mEncodeSurface;
    private Object mSurface;
    private int mOutputWidth=720;
    private int mOutputHeight=1280;
    private Filter mFilter;

    private MediaConfig.Video mConfig=new MediaConfig().mVideo;
    private MediaCodec mVideoCodec;
    private HardCodecData mHardCodecData;
    private MediaStore<HardCodecData> mMediaStore;
    private long startTime=-2;
    private boolean isCodecStarted=false;
    private final Object ENCODE_LOCK=new Object();
    private boolean isStoreStarted=false;

    public SurfaceEncoder(MediaStore<HardCodecData> store, int width, int height){
        setOutputSize(width, height);
        mHardCodecData=new HardCodecData();
        mHardCodecData.info=new MediaCodec.BufferInfo();
        this.mMediaStore=store;
    }

    public SurfaceEncoder(MediaStore<HardCodecData> store){
        this(store,720,1280);
    }

    public void setOutputSize(int width,int height){
        this.mOutputWidth=width;
        this.mOutputHeight=height;
    }

    @Override
    public int process(SurfaceTextureProcess.GLBean bean, Object o) {
        if(bean.texture==-1||(!isShow&&mEncodeSurface!=null)){
            if(mEncodeSurface!=null){
                bean.egl.destroySurface(mEncodeSurface);
                mEncodeSurface=null;
            }
            return 0;
        } else if(isShow&&o!=null&&isCodecStarted){
            if(mEncodeSurface==null){
                mEncodeSurface=bean.egl.createWindowSurface(mSurface);
                mFilter=new BaseFilter();
                mFilter.create();
                mFilter.sizeChanged(bean.width, bean.height);
                MatrixUtils.getMatrix(mFilter.getVertexMatrix(),MatrixUtils.TYPE_CENTERCROP,bean.width,bean.height,
                        mOutputWidth,mOutputHeight);
            }
            bean.egl.makeCurrent(mEncodeSurface);
            GLES20.glViewport(0,0,mOutputWidth,mOutputHeight);
            mFilter.draw(bean.texture);
            bean.egl.setPresentationTime(mEncodeSurface,/*bean.timeStamp*/(System.currentTimeMillis()-startTime)*1000000);
            videoEncodeStep(false);
            bean.egl.swapBuffers(mEncodeSurface);
        }
        return 0;
    }

    @Override
    public void onCall(AVMsg data) {
        if(data.msgType==AVMsg.TYPE_INFO){
            switch (data.msgRet){
                case AVMsg.MSG_CMD_START:
                    startTime= (long) data.msgData;
                    isShow=true;
                    startCodec();
                    break;
                case AVMsg.MSG_CMD_END:
                    isShow=false;
                    videoEncodeStep(true);
                    stopCodec();
                    isStoreStarted=false;
                    break;
                case AVMsg.MSG_STORE_START:
                    isStoreStarted=true;
                    startTime= (long) data.msgData;
                    break;
            }
        }
    }

    private void startCodec(){
        synchronized (ENCODE_LOCK){
            try {
                mConfig.colorFormat= MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;
                MediaFormat format=MediaFormat.createVideoFormat(mConfig.mime,mConfig.width,
                        mConfig.height);
                format.setInteger(MediaFormat.KEY_FRAME_RATE,mConfig.frameRate);
                format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,mConfig.iframe);
                format.setInteger(MediaFormat.KEY_BIT_RATE,mConfig.bitrate);
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT,mConfig.colorFormat);
                mVideoCodec=MediaCodec.createEncoderByType(mConfig.mime);
                mVideoCodec.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
                mSurface=mVideoCodec.createInputSurface();
                mVideoCodec.start();
                isCodecStarted=true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopCodec(){
        synchronized (ENCODE_LOCK){
            if(isCodecStarted){
                mVideoCodec.stop();
                mVideoCodec.release();
                mVideoCodec.release();
                isCodecStarted=false;
            }
            mHardCodecData.data=null;
            mHardCodecData.trackIndex=-1;
            mHardCodecData.type=0;
        }
    }

    private boolean videoEncodeStep(boolean isEnd){
        synchronized (ENCODE_LOCK){
            if(isCodecStarted){
                if(isEnd){
                    mVideoCodec.signalEndOfInputStream();
                }
                while (true){
                    int outputIndex=mVideoCodec.dequeueOutputBuffer(mHardCodecData.info,1000);
                    if(outputIndex>=0){
                        if(mMediaStore!=null&&mHardCodecData.trackIndex>=0&&isStoreStarted){
                            mHardCodecData.type= HardCodecData.DATA;
                            mHardCodecData.data= CodecUtil.getOutputBuffer(mVideoCodec,outputIndex);
                            mMediaStore.store(mHardCodecData);
                        }
                        mVideoCodec.releaseOutputBuffer(outputIndex,false);
                        if(mHardCodecData.info.flags== MediaCodec.BUFFER_FLAG_END_OF_STREAM){
                            return true;
                        }
                    }else if(outputIndex==MediaCodec.INFO_TRY_AGAIN_LATER){
                        break;
                    }else if(outputIndex==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                        if(mMediaStore!=null){
                            mHardCodecData.type= HardCodecData.ADDTRACK;
                            mHardCodecData.other=mVideoCodec.getOutputFormat();
                            mMediaStore.store(mHardCodecData);
                        }
                    }
                }
            }
        }
        return false;
    }



}

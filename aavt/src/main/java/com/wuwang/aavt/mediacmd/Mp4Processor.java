package com.wuwang.aavt.mediacmd;

import android.util.Log;

import com.wuwang.aavt.core.IObserver;
import com.wuwang.aavt.media.MediaConfig;

/*
 * Created by Wuwang on 2017/10/24
 */
public class Mp4Processor {


    public AActuator mProcessor;
    private Object mSurface;
    private ShowBean mShowBean=new ShowBean();
    private Mp4Muxer mMuxer;
    private MediaConfig mConfig=new MediaConfig();
    private Mp4Extractor mData;

    public Mp4Processor(String data){
        mShowBean.mOutputWidth=720;
        mShowBean.mOutputHeight=1280;

        //用于存储视频
        mMuxer=new Mp4Muxer(true);

        //用于展示视频
        SurfaceShowActuator surfaceShow=new SurfaceShowActuator();
        //用于编码视频
        SurfaceEncodeActuator surfaceEncode=new SurfaceEncodeActuator(mMuxer);
        surfaceEncode.setConfig(mConfig);

        //用于处理视频
        TextureProcessActuator process=new TextureProcessActuator();
        process.addObserver(surfaceShow);
        process.addObserver(surfaceEncode);

        //用于提供数据源
        if(data.equals("Camera")){
            CameraActuator cam=new CameraActuator();
            process.setSuccessor(cam);
            process.setAsCamera(true);
        }else {
            mData=new Mp4Extractor(mMuxer);
            mData.setDataSource(data);
            process.setSuccessor(mData);
            process.setAsCamera(false);
        }
        surfaceShow.setSuccessor(process);
        surfaceEncode.setSuccessor(surfaceShow);

        this.mProcessor=surfaceEncode;
    }

    public void setInputPath(String path){
        mData.setDataSource(path);
    }

    public void open(){
        mProcessor.execute(wrap(new Cmd(Cmd.CMD_VIDEO_OPEN_SOURCE,"打开数据源")));
    }

    public void close(){
        mProcessor.execute(wrap(new Cmd(Cmd.CMD_VIDEO_CLOSE_SOURCE,"关闭数据源")));
    }

    public void setPreviewSurface(Object surface){
        mShowBean.surface=surface;
    }

    public void setOutputSize(int width,int height){
        mConfig.mVideo.width=width;
        mConfig.mVideo.height=height;
    }

    public void setOutputPath(String path){
        mMuxer.setOutputPath(path);
    }

    public void setPreviewSize(int width,int height){
        this.mShowBean.mOutputWidth=width;
        this.mShowBean.mOutputHeight=height;
    }

    public void startPreview(){
        if(mShowBean.surface==null){
            log("please set output surface before startPreview");
            return;
        }
        mProcessor.execute(wrap(new Cmd(Cmd.CMD_VIDEO_OPEN_SHOW,"打开预览",mShowBean)));
    }

    public void stopPreview(){
        mProcessor.execute(wrap(new Cmd(Cmd.CMD_VIDEO_CLOSE_SHOW,"关闭预览")));
    }

    public void startRecord(){
        mProcessor.execute(wrap(new Cmd(Cmd.CMD_AUDIO_OPEN_ENCODE,"打开音频录制")));
        mProcessor.execute(wrap(new Cmd(Cmd.CMD_VIDEO_OPEN_ENCODE,"打开视频录制")));
    }

    public void stopRecord(){
        mProcessor.execute(wrap(new Cmd(Cmd.CMD_AUDIO_CLOSE_ENCODE,"关闭音频录制")));
        mProcessor.execute(wrap(new Cmd(Cmd.CMD_VIDEO_CLOSE_ENCODE,"关闭视频录制")));
    }

    private Cmd wrap(Cmd cmd){
        cmd.callback=new IObserver<Cmd>() {
            @Override
            public void onCall(Cmd cmd) {
                log(cmd.retInfo);
            }
        };
        return cmd;
    }

    private void log(String info){
        Log.e("wuwang","cmd info:"+info);
    }

}

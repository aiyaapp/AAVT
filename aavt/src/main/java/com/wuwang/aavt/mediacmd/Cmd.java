package com.wuwang.aavt.mediacmd;

import com.wuwang.aavt.core.IObserver;

/*
 * Created by Wuwang on 2017/10/23
 */
public class Cmd {

    //视频相关CMD
    public static final int CMD_VIDEO_OPEN_SOURCE=0x80001001;
    public static final int CMD_VIDEO_CLOSE_SOURCE=0x80001002;
    public static final int CMD_VIDEO_OPEN_SHOW=0x80001003;
    public static final int CMD_VIDEO_CLOSE_SHOW=0x80001004;
    public static final int CMD_VIDEO_OPEN_ENCODE=0x80001005;
    public static final int CMD_VIDEO_CLOSE_ENCODE=0x80001006;
    public static final int CMD_VIDEO_FRAME_DATA=0x80001007;

    public static final int CMD_VIDEO_RENDER=0x80001100;

    //音频相关CMD
    public static final int CMD_AUDIO_DATA=0x10002001;

    public static final int RET_TYPE_DATA=0x00001001;
    public static final int RET_TYPE_ERROR=0x00001002;
    public static final int RET_TYPE_INFO=0x00001003;

    //执行命令过程中，执行失败
    public static final int ERROR_CMD_EXEC_FAILED=0xF0000001;
    //没有可以执行此命令的执行器
    public static final int ERROR_CMD_NO_EXEC=0xF0000002;


    public IObserver<Cmd> callback;
    public int cmd;
    public String info;
    public Object obj;

    public int retType;
    public int retValue;
    public Object retObj;
    public String retInfo;

    public Cmd(int cmd,String info){
        this.cmd=cmd;
        this.info=info;
    }

    private Cmd(int cmd,String info,Object obj){
        this.cmd=cmd;
        this.info= info;
        this.obj=obj;
    }

    public void callback(){
        if(callback!=null){
            callback.onCall(this);
        }
    }

    public void errorCallback(int value,String info){
        callback(RET_TYPE_ERROR,value,info);
    }

    public void callback(int type,int value,String info){
        callback(type, value,info,retObj);
    }

    public void callback(int value,String info,Object data){
        callback(RET_TYPE_DATA,value,info,data);
    }

    public void callback(int type,int value,String info,Object data){
        if(callback!=null){
            this.retType=type;
            this.retValue=value;
            this.retInfo=info;
            this.retObj=data;
            callback.onCall(this);
        }
    }

    public Cmd cloneMe(){
        return new Cmd(cmd,info,obj);
    }

}

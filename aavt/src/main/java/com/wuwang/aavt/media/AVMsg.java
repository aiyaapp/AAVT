package com.wuwang.aavt.media;

/**
 * Created by wuwang on 2017/10/22.
 */

public class AVMsg {

    public static final int TYPE_ERROR=1;
    public static final int TYPE_INFO=2;
    public static final int TYPE_DATA=3;


    public static final int MSG_SURFACE_CREATED=0x00100100;
    public static final int MSG_TEXTURE_OK=0x00100101;
    public static final int MSG_CMD_START=0x00100200;
    public static final int MSG_CMD_END=0x00100201;
    public static final int MSG_STORE_START=0x00100202;

    public int msgType;
    public int msgRet;
    public String msgStr;
    public Object msgData;

    public AVMsg(int type,int ret,String msg){
        set(type, ret, msg);
    }

    public AVMsg(){}

    public void set(int type,int ret,String msg){
        this.msgType=type;
        this.msgRet=ret;
        this.msgStr=msg;
    }

}

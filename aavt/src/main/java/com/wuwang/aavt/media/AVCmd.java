package com.wuwang.aavt.media;


/**
 * 消息体，消息将在整个消息环路中流动，直到它被处理完毕，或者回到了发送者时停止。
 */
class AVCmd {

    public static final int CMD_PROCESS=0x00000001;
    public static final int CMD_SHOW=0x00000002;
    public static final int CMD_CREATE_SURFACE=0x00000004;

    public static final int CMD_CALLBACK=0x10000000;

    private long creator=0;

    public int cmd;
    public CmdRet cmdRet;
    public Object data;

    AVCmd(long creator){
        this.creator=creator;
    }

    boolean isCreator(long creator){
        return this.creator==creator;
    }

    public enum CmdRet{
        UNKNOW,SUCCESS,FAILED;
    }

}

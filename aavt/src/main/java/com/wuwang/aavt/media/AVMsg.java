package com.wuwang.aavt.media;

/**
 * Created by wuwang on 2017/10/20.
 */

public class AVMsg<T> {

    public static final int TYPE_VIDEO_SURFACE=1;
    public static final int TYPE_AUDIO=2;
    public static final int TYPE_VIDEO=3;

    public enum Type{
        TYPE_VIDEO_SURFACE,TYPE_AUDIO,TYPE_VIDEO,TYPE_UNKNOWN
    }

    private Type type=Type.TYPE_UNKNOWN;

    public AVMsg(Type type){
        this.type=type;
    }

    public Type getType(){
        return type;
    }

    public T receiveData;
    public int receiveDataLength;

}

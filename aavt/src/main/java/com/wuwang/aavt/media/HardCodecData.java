package com.wuwang.aavt.media;

import android.media.MediaCodec;

import java.nio.ByteBuffer;

/*
 * Created by Wuwang on 2017/10/21
 */
public class HardCodecData {

    public static final int ADDTRACK=1;
    public static final int DATA=2;

    public int type;

    public int trackIndex=-1;
    public MediaCodec.BufferInfo info;
    public ByteBuffer data;

    public Object other;


}

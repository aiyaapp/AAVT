package com.wuwang.aavt.mediacmd;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

/*
 * Created by Wuwang on 2017/10/24
 */
public interface HardMediaStore{

    int addFormat(MediaFormat format);

    void addData(int track, ByteBuffer buffer, MediaCodec.BufferInfo info);

}

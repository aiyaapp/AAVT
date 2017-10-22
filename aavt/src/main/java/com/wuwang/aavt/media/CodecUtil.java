package com.wuwang.aavt.media;

import android.media.MediaCodec;
import android.os.Build;

import java.nio.ByteBuffer;

/**
 * Created by wuwang on 2017/10/21.
 */

public class CodecUtil {


    public static ByteBuffer getInputBuffer(MediaCodec codec, int index){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return codec.getInputBuffer(index);
        }else{
            return codec.getInputBuffers()[index];
        }
    }

    public static ByteBuffer getOutputBuffer(MediaCodec codec, int index){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return codec.getOutputBuffer(index);
        }else{
            return codec.getOutputBuffers()[index];
        }
    }

}

package com.wuwang.aavt.media;

import android.opengl.EGLConfig;
import android.opengl.EGLContext;

import com.wuwang.aavt.core.IObservable;
import com.wuwang.aavt.egl.EGLHelper;

/**
 * Created by wuwang on 2017/10/19.
 */

public interface IVideoConsumer extends IObservable<AVMsg> {

    void onSurfaceFrame(EGLHelper egl,int width,int height,int texture);

    void setOutSize(int width,int height);

    void start();

    void stop();

}

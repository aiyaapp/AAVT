package com.wuwang.aavt.media;

import android.graphics.Point;
import android.graphics.SurfaceTexture;

import com.wuwang.aavt.core.IObserver;

import java.util.concurrent.Semaphore;

/**
 * Created by wuwang on 2017/10/19.
 */

public interface IVideoProvider extends IObserver<AVMsg> {

    Point start(SurfaceTexture texture, Semaphore sem);

    void stop();

}

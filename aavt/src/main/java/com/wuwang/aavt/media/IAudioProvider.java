package com.wuwang.aavt.media;

import com.wuwang.aavt.core.IObserver;

/**
 * Created by wuwang on 2017/10/19.
 */

public interface IAudioProvider<T> extends IObserver<AVMsg<T>> {

    void start();

    void stop();

}

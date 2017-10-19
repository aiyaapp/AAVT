package com.wuwang.aavt.media;

import com.wuwang.aavt.core.IObserver;

/**
 * Created by wuwang on 2017/10/19.
 */

public interface IAudioProvider extends IObserver<AVMsg> {

    void start();

    void stop();

}

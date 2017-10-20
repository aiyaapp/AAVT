package com.wuwang.aavt.media;

import com.wuwang.aavt.core.IObservable;

/**
 * Created by wuwang on 2017/10/20.
 */

public interface IAudioConsumer extends IObservable<AVMsg>{

    void start();

    void stop();

}

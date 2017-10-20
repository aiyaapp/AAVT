package com.wuwang.aavt.media;

/**
 * Created by wuwang on 2017/10/20.
 */

public interface IAVProcessor<T> {

    void startProcess();

    void onProcess(T data);

    void stopProcess();

}

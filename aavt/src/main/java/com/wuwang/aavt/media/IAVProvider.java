package com.wuwang.aavt.media;

/**
 * Created by wuwang on 2017/10/20.
 */

public interface IAVProvider<T> extends IAVCall<T>{

    void startProvide();

    void stopProvide();

}

package com.wuwang.aavt.media;

/**
 * Created by wuwang on 2017/10/22.
 */

public interface IProvider<D> {

    void start();

    void provide(D d);

    void stop();

}

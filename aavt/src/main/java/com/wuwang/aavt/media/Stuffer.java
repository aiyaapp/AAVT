package com.wuwang.aavt.media;

/*
 * Created by Wuwang on 2017/10/18
 */
public interface Stuffer {

    int start();

    int stuff(AVFrame frame);

    int stop();

}

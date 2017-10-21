package com.wuwang.aavt.media;

/*
 * Created by Wuwang on 2017/10/21
 */
public interface MediaStore<Start,Store> {

    int start(Start start);

    int store(Store store);

    int stop();

}

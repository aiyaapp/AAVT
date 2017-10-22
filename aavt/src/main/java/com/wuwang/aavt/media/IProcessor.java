package com.wuwang.aavt.media;

/**
 * Created by wuwang on 2017/10/22.
 */

public interface IProcessor<B,A> {

    int process(B b, A a);

}

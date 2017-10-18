package com.wuwang.aavt.egl;

import javax.microedition.khronos.egl.EGL10;

/*
 * Created by Wuwang on 2017/10/18
 */
public class EGLContextAttrs {

    private int version=2;

    public EGLContextAttrs version(int v){
        this.version=v;
        return this;
    }

    int[] build(){
        return new int[]{0x3098,version, EGL10.EGL_NONE};
    }

}

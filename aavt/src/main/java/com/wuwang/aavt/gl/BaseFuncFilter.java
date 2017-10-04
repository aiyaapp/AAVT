package com.wuwang.aavt.gl;

import android.content.res.Resources;

/**
 * Created by 15581 on 2017/9/30.
 */

class BaseFuncFilter extends Filter {

    static final String FILTER_SOBEL="shader/func/sobel.frag";
    static final String FILTER_GAUSS="shader/func/gauss.frag";

    BaseFuncFilter(Resources resource,String fragment) {
        super(resource, "shader/base.vert", fragment);
        shaderNeedTextureSize(true);
    }
}

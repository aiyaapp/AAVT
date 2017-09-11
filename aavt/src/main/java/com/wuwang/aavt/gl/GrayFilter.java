/*
 * Created by Wuwang on 2017/9/11
 * Copyright © 2017年 深圳哎吖科技. All rights reserved.
 */
package com.wuwang.aavt.gl;

import android.content.res.Resources;

import com.wuwang.aavt.core.Filter;

public class GrayFilter extends Filter {

    public GrayFilter(Resources resource) {
        super(resource, "shader/base.vert", "shader/color/gray.frag");
    }

}

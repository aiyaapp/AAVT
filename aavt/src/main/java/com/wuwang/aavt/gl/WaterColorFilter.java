package com.wuwang.aavt.gl;

import android.content.res.Resources;
import android.opengl.GLES20;

import com.wuwang.aavt.core.Filter;

/**
 * Created by aiya on 2017/9/23.
 */

public class WaterColorFilter extends Filter {

    private int mGLWidth;
    private int mGLHeight;

    public WaterColorFilter(Resources res){
        super(res,"shader/base.vert","shader/effect/water_color.frag");
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        mGLWidth= GLES20.glGetUniformLocation(mGLProgram,"uWidth");
        mGLHeight= GLES20.glGetUniformLocation(mGLProgram,"uHeight");
    }

    @Override
    protected void onSetExpandData() {
        super.onSetExpandData();
        GLES20.glUniform1f(mGLWidth,mWidth);
        GLES20.glUniform1f(mGLHeight,mHeight);
    }
}

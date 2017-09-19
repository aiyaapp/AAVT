package com.wuwang.aavt.gl;

import android.content.res.Resources;
import android.opengl.GLES20;

import com.wuwang.aavt.core.Filter;

/**
 * Created by aiya on 2017/9/18.
 */

public class BeautyFilter extends Filter {

    private int mGLaaCoef;
    private int mGLmixCoef;
    private int mGLiternum;


    private float aaCoef;
    private float mixCoef;
    private int iternum;

    public BeautyFilter(Resources resource) {
        super(resource,"shader/beauty/beauty.vert", "shader/beauty/beauty.frag");
        setBeautyLevel(0);
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        mGLaaCoef=GLES20.glGetUniformLocation(mGLProgram,"aaCoef");
        mGLmixCoef=GLES20.glGetUniformLocation(mGLProgram,"mixCoef");
        mGLiternum=GLES20.glGetUniformLocation(mGLProgram,"iternum");
    }

    public Filter setBeautyLevel(int level){
        switch (level){
            case 1:
                a(1,0.19f,0.54f);
                break;
            case 2:
                a(2,0.29f,0.54f);
                break;
            case 3:
                a(3,0.17f,0.39f);
                break;
            case 4:
                a(3,0.25f,0.54f);
                break;
            case 5:
                a(4,0.13f,0.54f);
                break;
            case 6:
                a(4,0.19f,0.69f);
                break;
            default:
                a(0,0f,0f);
                break;
        }
        return this;
    }

    private void a(int a,float b,float c){
        this.iternum=a;
        this.aaCoef=b;
        this.mixCoef=c;
    }

    @Override
    protected void onSetExpandData() {
        super.onSetExpandData();
        GLES20.glUniform1f(mGLaaCoef,aaCoef);
        GLES20.glUniform1f(mGLmixCoef,mixCoef);
        GLES20.glUniform1i(mGLiternum,iternum);
    }
}

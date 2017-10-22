package com.wuwang.aavt.media;

import android.opengl.EGLSurface;
import android.opengl.GLES20;

import com.wuwang.aavt.gl.BaseFilter;
import com.wuwang.aavt.gl.Filter;
import com.wuwang.aavt.utils.MatrixUtils;

/**
 * Created by wuwang on 2017/10/22.
 */

public class SurfaceShower implements IProcessor<SurfaceTextureProcess.GLBean,Object> {

    private boolean isShow=false;
    private EGLSurface mShowSurface;
    private int mOutputWidth=720;
    private int mOutputHeight=1280;
    private Filter mFilter;

    public SurfaceShower(){

    }

    public void openShow(int width,int height){
        isShow=true;
        this.mOutputWidth=width;
        this.mOutputHeight=height;
    }

    public void openShow(){
        isShow=true;
    }

    public void closeShow(){
        isShow=false;
    }

    @Override
    public int process(SurfaceTextureProcess.GLBean bean, Object o) {
        if(bean.texture==-1){
            if(mShowSurface!=null){
                bean.egl.destroySurface(mShowSurface);
                mShowSurface=null;
            }
            return 0;
        }
        if(isShow&&o!=null){
            if(mShowSurface==null){
                mShowSurface=bean.egl.createWindowSurface(o);
                mFilter=new BaseFilter();
                mFilter.create();
                mFilter.sizeChanged(bean.width, bean.height);
                MatrixUtils.getMatrix(mFilter.getVertexMatrix(),MatrixUtils.TYPE_CENTERCROP,bean.width,bean.height,
                        mOutputWidth,mOutputHeight);
            }
            bean.egl.makeCurrent(mShowSurface);
            GLES20.glViewport(0,0,mOutputWidth,mOutputHeight);
            mFilter.draw(bean.texture);
            bean.egl.swapBuffers(mShowSurface);
        }
        return 0;
    }

}

package com.wuwang.aavt.media;

import android.opengl.EGLSurface;
import android.opengl.GLES20;

import com.wuwang.aavt.core.IObserver;
import com.wuwang.aavt.gl.BaseFilter;
import com.wuwang.aavt.gl.Filter;
import com.wuwang.aavt.utils.MatrixUtils;

/*
 * Created by Wuwang on 2017/10/23
 */
public class SurfaceShower implements IObserver<RenderBean> {

    private EGLSurface mShowSurface;
    private boolean isShow=false;
    private Filter mFilter;
    private Object mSurface;
    private int mWidth;
    private int mHeight;
    private int mMatrixType=MatrixUtils.TYPE_CENTERCROP;
    private OnDrawEndListener mListener;

    public void setOutputSize(int width,int height){
        this.mWidth=width;
        this.mHeight=height;
    }

    public void setSurface(Object surface){
        this.mSurface=surface;
    }

    public void setMatrixType(int type){
        this.mMatrixType=type;
    }

    public void open(){
        isShow=true;
    }

    public void close(){
        isShow=false;
    }

    @Override
    public void onCall(RenderBean rb) {
        if(rb.endFlag&&mShowSurface!=null){
            rb.egl.destroySurface(mShowSurface);
            mShowSurface=null;
        }else if(isShow&&mSurface!=null){
            if(mShowSurface==null){
                mShowSurface=rb.egl.createWindowSurface(mSurface);
                mFilter=new BaseFilter();
                mFilter.create();
                mFilter.sizeChanged(rb.sourceWidth, rb.sourceHeight);
                MatrixUtils.getMatrix(mFilter.getVertexMatrix(),mMatrixType,rb.sourceWidth,rb.sourceHeight,
                        mWidth,mHeight);
            }
            rb.egl.makeCurrent(mShowSurface);
            GLES20.glViewport(0,0,mWidth,mHeight);
            mFilter.draw(rb.textureId);
            if(mListener!=null){
                mListener.onDrawEnd(mShowSurface,rb);
            }
            rb.egl.swapBuffers(mShowSurface);
        }
    }

    public void setOnDrawEndListener(OnDrawEndListener listener){
        this.mListener=listener;
    }

    public interface OnDrawEndListener{
        void onDrawEnd(EGLSurface surface,RenderBean bean);
    }

}

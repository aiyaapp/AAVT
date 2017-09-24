package com.wuwang.aavt.gl;

import android.opengl.GLES20;
import android.util.Log;

import com.wuwang.aavt.Aavt;

/**
 * Created by aiya on 2017/9/13.
 */

public class FrameBuffer {

    private int[] mFrameTemp;

    public int bindFrameBuffer(int width,int height){
        if(mFrameTemp==null){
            mFrameTemp=new int[3];
            GLES20.glGenFramebuffers(1,mFrameTemp,0);
            GLES20.glGenTextures(1,mFrameTemp,1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,mFrameTemp[1]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0,GLES20.GL_RGBA, width, height,
                    0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
            //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_NEAREST);
            //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
            //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);
            //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);

            GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING,mFrameTemp,2);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,mFrameTemp[0]);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20.GL_TEXTURE_2D, mFrameTemp[1], 0);
        }else{
            GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING,mFrameTemp,2);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,mFrameTemp[0]);
        }
        return GLES20.glGetError();
    }

    public void createFrameBuffer(int width,int height,int tex_type,int tex_format,
                                  int min_params,int max_params,int wrap_s,int wrap_t){
        mFrameTemp=new int[3];
        GLES20.glGenFramebuffers(1,mFrameTemp,0);
        GLES20.glGenTextures(1,mFrameTemp,1);
        GLES20.glBindTexture(tex_type,mFrameTemp[1]);
        GLES20.glTexImage2D(tex_type, 0,tex_format, width, height,
                0, tex_format, GLES20.GL_UNSIGNED_BYTE, null);
        //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
        GLES20.glTexParameteri(tex_type, GLES20.GL_TEXTURE_MIN_FILTER,min_params);
        //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
        GLES20.glTexParameteri(tex_type, GLES20.GL_TEXTURE_MAG_FILTER,max_params);
        //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameteri(tex_type, GLES20.GL_TEXTURE_WRAP_S,wrap_s);
        //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameteri(tex_type, GLES20.GL_TEXTURE_WRAP_T,wrap_t);

        GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING,mFrameTemp,2);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,mFrameTemp[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                tex_type, mFrameTemp[1], 0);
    }

    public int bindFrameBuffer(){
        if(mFrameTemp==null)return -1;
        GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING,mFrameTemp,2);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,mFrameTemp[0]);
        return GLES20.glGetError();
    }

    public void unBindFrameBuffer(){
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,mFrameTemp[2]);
    }

    public int getCacheTextureId(){
        return mFrameTemp!=null?mFrameTemp[1]:-1;
    }

    public void destroyFrameBuffer(){
        if(mFrameTemp!=null){
            GLES20.glDeleteFramebuffers(1,mFrameTemp,0);
            GLES20.glDeleteTextures(1,mFrameTemp,1);
            mFrameTemp=null;
        }
    }

}

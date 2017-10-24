package com.wuwang.aavt.egl;

import android.annotation.TargetApi;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import javax.microedition.khronos.opengles.GL10;

/*
 * Created by Wuwang on 2017/10/18
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class EGLHelper {

    private boolean isDebug=true;
    private EGLDisplay mEGLDisplay;
    private EGLConfig mEGLConfig;
    private EGLContext mEGLContext;
    private EGLSurface mEGLSurface;

    public EGLHelper(int display){
        changeDisplay(display);
    }

    public EGLHelper(){
        this(EGL14.EGL_DEFAULT_DISPLAY);
    }

    public void changeDisplay(int key){
        mEGLDisplay=EGL14.eglGetDisplay(key);
        //获取版本号，[0]为版本号，[1]为子版本号
        int[] versions=new int[2];
        EGL14.eglInitialize(mEGLDisplay,versions,0,versions,1);
        log(EGL14.eglQueryString(mEGLDisplay, EGL14.EGL_VENDOR));
        log(EGL14.eglQueryString(mEGLDisplay, EGL14.EGL_VERSION));
        log(EGL14.eglQueryString(mEGLDisplay, EGL14.EGL_EXTENSIONS));
    }

    public EGLConfig getConfig(EGLConfigAttrs attrs){
        EGLConfig[] configs = new EGLConfig[1];
        int[] configNum = new int[1];
        EGL14.eglChooseConfig(mEGLDisplay,attrs.build(),0,configs,0,1,configNum,0);
        //选择的过程可能出现多个，也可能一个都没有，这里只用一个
        if(configNum[0]>0){
            if(attrs.isDefault()){
                mEGLConfig=configs[0];
            }
            return configs[0];
        }
        return null;
    }

    public EGLConfig getDefaultConfig(){
        return mEGLConfig;
    }

    public EGLSurface getDefaultSurface(){
        return mEGLSurface;
    }

    public EGLContext getDefaultContext(){
        return mEGLContext;
    }

    public EGLContext createContext(EGLConfig config,EGLContext share,EGLContextAttrs attrs){
        EGLContext context= EGL14.eglCreateContext(mEGLDisplay,config,share,attrs.build(),0);
        if(attrs.isDefault()){
            mEGLContext=context;
        }
        return context;
    }

    public EGLSurface createWindowSurface(EGLConfig config,Object surface){
        return EGL14.eglCreateWindowSurface(mEGLDisplay,config,surface,new int[]{EGL14.EGL_NONE},0);
    }

    public EGLSurface createWindowSurface(Object surface){
        mEGLSurface=EGL14.eglCreateWindowSurface(mEGLDisplay,mEGLConfig,surface,new int[]{EGL14.EGL_NONE},0);
        return mEGLSurface;
    }

    public EGLSurface createPBufferSurface(EGLConfig config,int width,int height){
        return EGL14.eglCreatePbufferSurface(mEGLDisplay,config,new int[]{EGL14.EGL_WIDTH,width,EGL14.EGL_HEIGHT,height,EGL14.EGL_NONE},0);
    }

    public boolean createGLESWithSurface(EGLConfigAttrs attrs,EGLContextAttrs ctxAttrs,Object surface){
        EGLConfig config=getConfig(attrs.surfaceType(EGL14.EGL_WINDOW_BIT).makeDefault(true));
        if(config==null){
            log("getConfig failed : "+EGL14.eglGetError());
            return false;
        }
        mEGLContext=createContext(config,EGL14.EGL_NO_CONTEXT,ctxAttrs.makeDefault(true));
        if(mEGLContext==EGL14.EGL_NO_CONTEXT){
            log("createContext failed : "+EGL14.eglGetError());
            return false;
        }
        mEGLSurface=createWindowSurface(surface);
        if(mEGLSurface==EGL14.EGL_NO_SURFACE){
            log("createWindowSurface failed : "+EGL14.eglGetError());
            return false;
        }
        if(!EGL14.eglMakeCurrent(mEGLDisplay,mEGLSurface,mEGLSurface,mEGLContext)){
            log("eglMakeCurrent failed : "+EGL14.eglGetError());
            return false;
        }
        return true;
    }

    public boolean makeCurrent(EGLSurface draw,EGLSurface read,EGLContext context){
        if(!EGL14.eglMakeCurrent(mEGLDisplay,draw,read,context)){
            log("eglMakeCurrent failed : "+EGL14.eglGetError());
        }
        return true;
    }

    public boolean makeCurrent(EGLSurface surface,EGLContext context){
        return makeCurrent(surface,surface,context);
    }

    public boolean makeCurrent(EGLSurface surface){
        return makeCurrent(surface,mEGLContext);
    }

    public boolean makeCurrent(){
        return makeCurrent(mEGLSurface,mEGLContext);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void setPresentationTime(EGLSurface surface, long time){
        EGLExt.eglPresentationTimeANDROID(mEGLDisplay,surface,time);
    }

    public EGLSurface createGLESWithPBuffer(EGLConfigAttrs attrs,EGLContextAttrs ctxAttrs,int width,int height){
        EGLConfig config=getConfig(attrs.surfaceType(EGL14.EGL_PBUFFER_BIT));
        if(config==null){
            log("getConfig failed : "+EGL14.eglGetError());
            return null;
        }
        EGLContext eglContext=createContext(config,EGL14.EGL_NO_CONTEXT,ctxAttrs);
        if(eglContext==EGL14.EGL_NO_CONTEXT){
            log("createContext failed : "+EGL14.eglGetError());
            return null;
        }
        EGLSurface eglSurface=createPBufferSurface(config,width,height);
        if(eglSurface==EGL14.EGL_NO_SURFACE){
            log("createWindowSurface failed : "+EGL14.eglGetError());
            return null;
        }
        if(!EGL14.eglMakeCurrent(mEGLDisplay,eglSurface,eglSurface,eglContext)){
            log("eglMakeCurrent failed : "+EGL14.eglGetError());
            return null;
        }
        return eglSurface;
    }

    public void swapBuffers(EGLSurface surface){
        EGL14.eglSwapBuffers(mEGLDisplay,surface);
    }

    public boolean destroyGLES(EGLSurface surface,EGLContext context){
        EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
        EGL14.eglDestroySurface(mEGLDisplay,surface);
        EGL14.eglDestroyContext(mEGLDisplay,context);
        EGL14.eglTerminate(mEGLDisplay);
        log("gl destroy gles");
        return true;
    }

    public void destroySurface(EGLSurface surface){
        EGL14.eglDestroySurface(mEGLDisplay,surface);
    }

    public EGLDisplay getDisplay(){
        return mEGLDisplay;
    }

    //创建视频数据流的OES TEXTURE
    public static int createTextureID() {
        int[] texture = new int[1];
        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        return texture[0];
    }

    private void log(String log){
        if(isDebug){
            Log.e("EGLHelper",log);
        }
    }

}

/*
 * Created by Wuwang on 2017/10/17
 */
package com.wuwang.aavt.media;

import android.view.Surface;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class VirtualSurface{

    Surface getSurface(int width,int height){
        try {
            Class<?> mSurfaceSessionClazz=Class.forName("android.view.SurfaceSession");
            Constructor sessionCons=mSurfaceSessionClazz.getConstructor();

            Class mSurfaceControlClazz=Class.forName("android.view.SurfaceControl");
            Constructor mSurfaceControlCons=mSurfaceControlClazz.getConstructor(mSurfaceSessionClazz,String.class,int.class,int.class,int.class,int.class);
            Object mSurfaceControl=mSurfaceControlCons.newInstance(sessionCons.newInstance(),"virtualSurface",width,height,/*OPAQUE*/0x00000400,/*HIDDEN*/0x00000004);

            Class mSurfaceClazz=Class.forName("android.view.Surface");
            Constructor cons=mSurfaceClazz.getConstructor();
            Surface surface= (Surface) cons.newInstance();
            Method surfaceCopyFrom=mSurfaceClazz.getDeclaredMethod("copyFrom",mSurfaceControlClazz);
            surfaceCopyFrom.invoke(surface,mSurfaceControl);
            return surface;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

}

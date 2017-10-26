/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wuwang.aavt.media;

import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.util.concurrent.Semaphore;

/**
 * CameraProvider 相机数据
 *
 * @author wuwang
 * @version v1.0 2017:10:26 18:09
 */
public class CameraProvider implements ITextureProvider {

    private Camera mCamera;
    private int cameraId=1;
    private Semaphore mFrameSem;

    @Override
    public Point open(final SurfaceTexture surface) {
        mFrameSem=new Semaphore(0);
        mCamera=Camera.open(cameraId);
        try {
            mCamera.setPreviewTexture(surface);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    surface.setOnFrameAvailableListener(frameListener);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        Camera.Size size=mCamera.getParameters().getPreviewSize();
        mCamera.startPreview();
        return new Point(size.height,size.width);
    }

    @Override
    public void close() {
        mFrameSem.drainPermits();
        mFrameSem.release();

        mCamera.stopPreview();
        mCamera.release();
        mCamera=null;
    }

    @Override
    public boolean frame() {
        try {
            mFrameSem.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public long getTimeStamp() {
        return -1;
    }

    private SurfaceTexture.OnFrameAvailableListener frameListener=new SurfaceTexture.OnFrameAvailableListener() {

        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            mFrameSem.drainPermits();
            mFrameSem.release();
        }

    };

}

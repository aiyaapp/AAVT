package com.wuwang.aavt.media;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;

import java.io.IOException;

/*
 * Created by Wuwang on 2017/10/18
 */
public class AVCameraUnit extends AVExecUnit {

    private Camera mCamera;
    private int mCameraIndex=1;
    private AVCmd cmd;

    @Override
    public void start() {
        mCamera=Camera.open(mCameraIndex);
        if(mNextUnit!=null){
            mNextUnit.start();
            cmd=createCmd();
            cmd.cmd=AVCmd.CMD_CREATE_SURFACE|AVCmd.CMD_CALLBACK;
            mNextUnit.putCmd(cmd);
        }
    }

    @Override
    protected void onSelfCmdCallback() {
        if(cmd.cmdRet== AVCmd.CmdRet.SUCCESS&&cmd.data instanceof SurfaceTexture){
            try {
                mCamera.setPreviewTexture((SurfaceTexture) cmd.data);
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected boolean onUpperCmd() {
        return false;
    }

    @Override
    public void stop() {
        if(mCamera!=null){
            mCamera.stopPreview();
            mCamera.release();
            mCamera=null;
        }
    }

}

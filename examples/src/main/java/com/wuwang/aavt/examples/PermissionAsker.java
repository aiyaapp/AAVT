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
package com.wuwang.aavt.examples;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

/**
 * PermissionUtils
 *
 * @author wuwang
 * @version v1.0 2017:11:16 16:47
 */
public class PermissionAsker {

    private Runnable mOkRun=RUN;
    private Runnable mDeniRun=RUN;
    private int mReqCode=1;

    public PermissionAsker(){}

    public PermissionAsker(int code,Runnable ok,Runnable deni){
        this.mReqCode=code;
        this.mOkRun=ok;
        this.mDeniRun=deni;
    }

    public void setReqCode(int code){
        this.mReqCode=code;
    }

    public void setSuccedCallback(Runnable run){
        this.mOkRun=run;
    }

    public void setFailedCallback(Runnable run){
        this.mDeniRun=run;
    }

    public PermissionAsker askPermission(Activity context,String ... permission){
        int result=0;
        for (String p:permission){
            result+=ActivityCompat.checkSelfPermission(context,p);
        }
        if(result==0){
            mOkRun.run();
        }else{
            ActivityCompat.requestPermissions(context,permission,mReqCode);
        }
        return this;
    }

    public void onRequestPermissionsResult(int[] grantResults){
        boolean b=true;
        for (int a:grantResults){
            b&=(a== PackageManager.PERMISSION_GRANTED);
        }
        if (grantResults.length > 0&&b) {
            mOkRun.run();
        } else {
            mDeniRun.run();
        }
    }


    private static final Runnable RUN=new Runnable() {
        @Override
        public void run() {

        }
    };


}

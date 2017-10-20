package com.wuwang.aavt.media;

/**
 * Created by wuwang on 2017/10/20.
 */

public class AAVBean {

    public static final int MSG_OK=1;
    public static final int MSG_FAILED=-1;
    public static final int MSG_ERROR=-2;
    public static final int REQUEST_FILL=0x10000001;

    public int msg;
    public int msgDetail;
    public String msgStr;
    public Object data;

}

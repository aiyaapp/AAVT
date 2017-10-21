package com.wuwang.aavt.media;

/*
 * Created by Wuwang on 2017/10/21
 */
public class MediaConfig {

    public static final int TYPE_VIDEO=1;

    public Video mVideo=new Video();

    public class Video{
        public String mime="video/avc";
        public int width=368;
        public int height=640;
        public int frameRate=24;
        public int iframe=1;
        public int bitrate=1177600;
        public int colorFormat;
    }

}

package com.wuwang.aavt.media;

/*
 * Created by Wuwang on 2017/10/21
 */
public class MediaConfig {

    public static final int TYPE_VIDEO=1;

    public Video mVideo=new Video();
    public Audio mAudio=new Audio();

    public class Video{
        public String mime="video/avc";
        public int width=368;
        public int height=640;
        public int frameRate=24;
        public int iframe=1;
        public int bitrate=1177600;
        public int colorFormat;
    }

    public class  Audio{
        public String mime="audio/mp4a-latm";
        public int sampleRate=48000;
        public int channelCount=2;
        public int bitrate=128000;
        public int profile;
    }

}

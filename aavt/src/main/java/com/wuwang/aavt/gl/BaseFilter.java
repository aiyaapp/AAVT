package com.wuwang.aavt.gl;

import android.content.res.Resources;

public class BaseFilter extends Filter {

    public BaseFilter(Resources resource) {
        super(resource,"shader/base.vert","shader/base.frag");
    }

    public BaseFilter(String vert,String frag){
        super(null,vert,frag);
    }

    public BaseFilter(){
        super(null,"attribute vec4 aVertexCo;\n" +
                "attribute vec2 aTextureCo;\n" +
                "\n" +
                "uniform mat4 uVertexMatrix;\n" +
                "uniform mat4 uTextureMatrix;\n" +
                "\n" +
                "varying vec2 vTextureCo;\n" +
                "\n" +
                "void main(){\n" +
                "    gl_Position = uVertexMatrix*aVertexCo;\n" +
                "    vTextureCo = (uTextureMatrix*vec4(aTextureCo,0,1)).xy;\n" +
                "}",
                "precision mediump float;\n" +
                "varying vec2 vTextureCo;\n" +
                "uniform sampler2D uTexture;\n" +
                "void main() {\n" +
                "    gl_FragColor = texture2D( uTexture, vTextureCo);\n" +
                "}");
    }

    @Override
    protected void onCreate() {
        super.onCreate();
    }

}

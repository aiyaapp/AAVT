package com.wuwang.aavt.gl;

import android.content.res.Resources;
import android.opengl.GLES20;
import android.util.Log;

import com.wuwang.aavt.core.Filter;
import com.wuwang.aavt.utils.MatrixUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by aiya on 2017/9/21.
 */

public class YuvOutputFilter extends BaseFilter {

    private ByteBuffer mTempBuffer;

    private int[] lastViewPort=new int[4];

    public static final int EXPORT_TYPE_I420=1;
    public static final int EXPORT_TYPE_YV12=2;
    public static final int EXPORT_TYPE_NV12=3;
    public static final int EXPORT_TYPE_NV21=4;

    private Filter mExportFilter;
    private FrameBuffer mFrameBuffer;

    public YuvOutputFilter(int type) {
        super();
        mExportFilter=new ExportFilter(type);
        mFrameBuffer=new FrameBuffer();
        MatrixUtils.flip(mExportFilter.getVertexMatrix(),false,true);
    }

    public YuvOutputFilter(Resources res,String yuvShader){
        super();
        mExportFilter=new ExportFilter(res,yuvShader);
        mFrameBuffer=new FrameBuffer();
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        mExportFilter.create();
    }

    @Override
    protected void onSizeChanged(int width, int height) {
        super.onSizeChanged(width, height);
        mExportFilter.sizeChanged(width, height);
    }

    @Override
    protected void onDraw() {
        GLES20.glGetIntegerv(GLES20.GL_VIEWPORT,lastViewPort,0);
        GLES20.glViewport(0,0,mWidth,mHeight);
        mFrameBuffer.bindFrameBuffer(mWidth,mHeight);
        super.onDraw();
        if(mTempBuffer==null){
            mTempBuffer=ByteBuffer.allocate(mWidth*mHeight*3/2);
        }
        mFrameBuffer.unBindFrameBuffer();
        GLES20.glViewport(0,0,mWidth,mHeight);
        mExportFilter.draw(mFrameBuffer.getCacheTextureId());
        GLES20.glReadPixels(0,0,mWidth,mHeight*3/8,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,mTempBuffer);
        GLES20.glViewport(lastViewPort[0],lastViewPort[1],lastViewPort[2],lastViewPort[3]);
    }

    public void getOutput(byte[] data){
        if(mTempBuffer!=null){
            mTempBuffer.get(data);
            mTempBuffer.clear();
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        mFrameBuffer.destroyFrameBuffer();
    }

    private static class ExportShader{
        private final String HEAD="precision highp float;\n" +
                "precision highp int;\n" +
                "\n" +
                "varying vec2 vTextureCo;\n" +
                "uniform sampler2D uTexture;\n" +
                "\n" +
                "uniform float uWidth;\n" +
                "uniform float uHeight;\n"+
                "float cY(float x,float y){\n" +
                "    vec4 c=texture2D(uTexture,vec2(x,y));\n" +
                "    return c.r*0.2990+c.g*0.5870+c.b*0.1140;\n" +
                "}\n" +
                "\n" +
                "float cU(float x,float y){\n" +
                "    vec4 c=texture2D(uTexture,vec2(x,y));\n" +
                "    return -0.1471*c.r - 0.2889*c.g + 0.4360*c.b+0.5000;\n" +
                "}\n" +
                "\n" +
                "float cV(float x,float y){\n" +
                "    vec4 c=texture2D(uTexture,vec2(x,y));\n" +
                "    return 0.6150*c.r - 0.5150*c.g - 0.1000*c.b+0.5000;\n" +
                "}\n" +
                "\n" +
                "vec2 cPos(float t,float shiftx,float gy){\n" +
                "    vec2 pos=vec2(floor(uWidth*vTextureCo.x),floor(uHeight*gy));\n" +
                "    return vec2(mod(pos.x*shiftx,uWidth),(pos.y*shiftx+floor(pos.x*shiftx/uWidth))*t);\n" +
                "}\n"+
                "\n"+
                "vec4 calculateY(){\n" +
                "    vec2 pos=cPos(1.,4.,vTextureCo.y);\n" +
                "    vec4 oColor=vec4(0);\n" +
                "    float textureYPos=pos.y/uHeight;\n" +
                "    oColor[0]=cY(pos.x/uWidth,textureYPos);\n" +
                "    oColor[1]=cY((pos.x+1.)/uWidth,textureYPos);\n" +
                "    oColor[2]=cY((pos.x+2.)/uWidth,textureYPos);\n" +
                "    oColor[3]=cY((pos.x+3.)/uWidth,textureYPos);\n" +
                "    return oColor;\n" +
                "}\n"+
                "vec4 calculateU(float gy){\n" +
                "    vec2 pos=cPos(2.,8.,vTextureCo.y-gy);\n" +
                "    vec4 oColor=vec4(0);\n" +
                "    float textureYPos=pos.y/uHeight;\n" +
                "    oColor[0]= cU(pos.x/uWidth,textureYPos);\n" +
                "    oColor[1]= cU((pos.x+2.)/uWidth,textureYPos);\n" +
                "    oColor[2]= cU((pos.x+4.)/uWidth,textureYPos);\n" +
                "    oColor[3]= cU((pos.x+6.)/uWidth,textureYPos);\n" +
                "    return oColor;\n" +
                "}\n"+
                "vec4 calculateV(float gy){\n" +
                "    vec2 pos=cPos(2.,8.,vTextureCo.y-gy);\n" +
                "    vec4 oColor=vec4(0);\n" +
                "    float textureYPos=pos.y/uHeight;\n" +
                "    oColor[0]=cV(pos.x/uWidth,textureYPos);\n" +
                "    oColor[1]=cV((pos.x+2.)/uWidth,textureYPos);\n" +
                "    oColor[2]=cV((pos.x+4.)/uWidth,textureYPos);\n" +
                "    oColor[3]=cV((pos.x+6.)/uWidth,textureYPos);\n" +
                "    return oColor;\n" +
                "}\n"+
                "vec4 calculateUV(){\n" +
                "    vec2 pos=cPos(2.,4.,vTextureCo.y-0.2500);\n" +
                "    vec4 oColor=vec4(0);\n" +
                "    float textureYPos=pos.y/uHeight;\n" +
                "    oColor[0]= cU(pos.x/uWidth,textureYPos);\n" +
                "    oColor[1]= cV(pos.x/uWidth,textureYPos);\n" +
                "    oColor[2]= cU((pos.x+2.)/uWidth,textureYPos);\n" +
                "    oColor[3]= cV((pos.x+2.)/uWidth,textureYPos);\n" +
                "    return oColor;\n" +
                "}\n"+
                "vec4 calculateVU(){\n" +
                "    vec2 pos=cPos(2.,4.,vTextureCo.y-0.2500);\n" +
                "    vec4 oColor=vec4(0);\n" +
                "    float textureYPos=pos.y/uHeight;\n" +
                "    oColor[0]= cV(pos.x/uWidth,textureYPos);\n" +
                "    oColor[1]= cU(pos.x/uWidth,textureYPos);\n" +
                "    oColor[2]= cV((pos.x+2.)/uWidth,textureYPos);\n" +
                "    oColor[3]= cU((pos.x+2.)/uWidth,textureYPos);\n" +
                "    return oColor;\n" +
                "}\n";

        public String getFrag(int type){
            StringBuilder sb=new StringBuilder();
            sb.append(HEAD);
            switch (type){
                case YuvOutputFilter.EXPORT_TYPE_I420:
                    sb.append("void main() {\n" +
                            "    if(vTextureCo.y<0.2500){\n" +
                            "        gl_FragColor=calculateY();\n" +
                            "    }else if(vTextureCo.y<0.3125){\n" +
                            "        gl_FragColor=calculateU(0.2500);\n" +
                            "    }else if(vTextureCo.y<0.3750){\n" +
                            "        gl_FragColor=calculateV(0.3125);\n" +
                            "    }else{\n" +
                            "        gl_FragColor=vec4(0,0,0,0);\n" +
                            "    }\n" +
                            "}");
                    break;
                case YuvOutputFilter.EXPORT_TYPE_YV12:
                    sb.append("void main() {\n" +
                            "    if(vTextureCo.y<0.2500){\n" +
                            "        gl_FragColor=calculateY();\n" +
                            "    }else if(vTextureCo.y<0.3125){\n" +
                            "        gl_FragColor=calculateV(0.2500);\n" +
                            "    }else if(vTextureCo.y<0.3750){\n" +
                            "        gl_FragColor=calculateU(0.3125);\n" +
                            "    }else{\n" +
                            "        gl_FragColor=vec4(0,0,0,0);\n" +
                            "    }\n" +
                            "}");
                    break;
                case YuvOutputFilter.EXPORT_TYPE_NV12:
                    sb.append("void main() {\n" +
                        "    if(vTextureCo.y<0.2500){\n" +
                        "        gl_FragColor=calculateY();\n" +
                        "    }else if(vTextureCo.y<0.3750){\n" +
                        "        gl_FragColor=calculateUV();\n" +
                        "    }else{\n" +
                        "        gl_FragColor=vec4(0,0,0,0);\n" +
                        "    }\n" +
                        "}");
                    break;
                case YuvOutputFilter.EXPORT_TYPE_NV21:
                    sb.append("void main() {\n" +
                            "    if(vTextureCo.y<0.2500){\n" +
                            "        gl_FragColor=calculateY();\n" +
                            "    }else if(vTextureCo.y<0.3750){\n" +
                            "        gl_FragColor=calculateVU();\n" +
                            "    }else{\n" +
                            "        gl_FragColor=vec4(0,0,0,0);\n" +
                            "    }\n" +
                            "}");
                    break;
            }
            return sb.toString();
        }
    }

    private static class ExportFilter extends Filter{

        private int mGLWidth;
        private int mGLHeight;

        public ExportFilter(Resources resource, String frag) {
            super(resource, "shader/base.vert", "shader/convert/"+frag);
        }

        public ExportFilter(int type){
            super(null,BASE_VERT,new ExportShader().getFrag(type));
        }

        @Override
        protected void onCreate() {
            super.onCreate();
            mGLWidth=GLES20.glGetUniformLocation(mGLProgram,"uWidth");
            mGLHeight=GLES20.glGetUniformLocation(mGLProgram,"uHeight");
        }

        @Override
        protected void onSetExpandData() {
            super.onSetExpandData();
            GLES20.glUniform1f(mGLWidth,this.mWidth);
            GLES20.glUniform1f(mGLHeight,this.mHeight);
        }
    }

}

package com.wuwang.aavt.core;

import android.content.res.Resources;
import android.opengl.GLES20;

import com.wuwang.aavt.utils.GpuUtils;
import com.wuwang.aavt.utils.MatrixUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public abstract class Filter implements Renderer {

    public static final String BASE_VERT="attribute vec4 aVertexCo;\n" +
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
            "}";

    private float[] mVertexMatrix= MatrixUtils.getOriginalMatrix();
    private float[] mTextureMatrix=MatrixUtils.getOriginalMatrix();

    protected FloatBuffer mVertexBuffer;
    protected FloatBuffer mTextureBuffer;

    protected int mWidth;
    protected int mHeight;

    protected Resources mRes;
    private String mVertex;
    private String mFragment;

    protected int mGLProgram;
    protected int mGLVertexCo;
    protected int mGLTextureCo;
    protected int mGLVertexMatrix;
    protected int mGLTextureMatrix;
    protected int mGLTexture;

    private int[] mFrameTemp;

    protected Filter(Resources resource,String vertex,String fragment){
        this.mRes=resource;
        this.mVertex=vertex;
        this.mFragment=fragment;
        initBuffer();
    }

    protected void initBuffer(){
        ByteBuffer vertex=ByteBuffer.allocateDirect(32);
        vertex.order(ByteOrder.nativeOrder());
        mVertexBuffer=vertex.asFloatBuffer();
        mVertexBuffer.put(MatrixUtils.getOriginalVertexCo());
        mVertexBuffer.position(0);
        ByteBuffer texture=ByteBuffer.allocateDirect(32);
        texture.order(ByteOrder.nativeOrder());
        mTextureBuffer=texture.asFloatBuffer();
        mTextureBuffer.put(MatrixUtils.getOriginalTextureCo());
        mTextureBuffer.position(0);
    }

    public void setVertexCo(float[] vertexCo){
        mVertexBuffer.clear();
        mVertexBuffer.put(vertexCo);
        mVertexBuffer.position(0);
    }

    public void setTextureCo(float[] textureCo){
        mTextureBuffer.clear();
        mTextureBuffer.put(textureCo);
        mTextureBuffer.position(0);
    }

    public void setVertexBuffer(FloatBuffer vertexBuffer){
        this.mVertexBuffer=vertexBuffer;
    }

    public void setTextureBuffer(FloatBuffer textureBuffer){
        this.mTextureBuffer=textureBuffer;
    }

    public void setVertexMatrix(float[] matrix){
        this.mVertexMatrix=matrix;
    }

    public void setTextureMatrix(float[] matrix){
        this.mTextureMatrix=matrix;
    }

    public float[] getVertexMatrix(){
        return mVertexMatrix;
    }

    public float[] getTextureMatrix(){
        return mTextureMatrix;
    }

    protected void onCreate(){
        if(mRes!=null){
            mGLProgram= GpuUtils.createGLProgramByAssetsFile(mRes,mVertex,mFragment);
        }else{
            mGLProgram= GpuUtils.createGLProgram(mVertex,mFragment);
        }
        mGLVertexCo=GLES20.glGetAttribLocation(mGLProgram,"aVertexCo");
        mGLTextureCo=GLES20.glGetAttribLocation(mGLProgram,"aTextureCo");
        mGLVertexMatrix=GLES20.glGetUniformLocation(mGLProgram,"uVertexMatrix");
        mGLTextureMatrix=GLES20.glGetUniformLocation(mGLProgram,"uTextureMatrix");
        mGLTexture=GLES20.glGetUniformLocation(mGLProgram,"uTexture");
    }

    protected void onSizeChanged(int width,int height){

    }

    @Override
    public final void create() {
        if(mVertex!=null&&mFragment!=null){
            onCreate();
        }
    }

    @Override
    public void sizeChanged(int width, int height) {
        onSizeChanged(width, height);
        this.mWidth=width;
        this.mHeight=height;
    }

    @Override
    public final void draw(int texture) {
        onClear();
        onUseProgram();
        onSetExpandData();
        onBindTexture(texture);
        onDraw();
    }

    public int drawToTexture(int texture){
        onBindFramebuffer();
        onClear();
        onUseProgram();
        MatrixUtils.flip(mVertexMatrix,false,true);
        onSetExpandData();
        MatrixUtils.flip(mVertexMatrix,false,true);
        onBindTexture(texture);
        onDraw();
        onUnBindFrameBuffer();
        return mFrameTemp[1];
    }

    @Override
    public void destroy() {
        if(mFrameTemp!=null){
            GLES20.glDeleteFramebuffers(1,mFrameTemp,0);
            GLES20.glDeleteTextures(1,mFrameTemp,1);
            mFrameTemp=null;
        }
    }

    protected void onUseProgram(){
        GLES20.glUseProgram(mGLProgram);
    }

    protected void onDraw(){
        GLES20.glEnableVertexAttribArray(mGLVertexCo);
        GLES20.glVertexAttribPointer(mGLVertexCo,2, GLES20.GL_FLOAT, false, 0,mVertexBuffer);
        GLES20.glEnableVertexAttribArray(mGLTextureCo);
        GLES20.glVertexAttribPointer(mGLTextureCo, 2, GLES20.GL_FLOAT, false, 0, mTextureBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);
        GLES20.glDisableVertexAttribArray(mGLVertexCo);
        GLES20.glDisableVertexAttribArray(mGLTextureCo);
    }

    protected void onClear(){
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
    }

    /**
     * 设置其他扩展数据
     */
    protected void onSetExpandData(){
        GLES20.glUniformMatrix4fv(mGLVertexMatrix,1,false,mVertexMatrix,0);
        GLES20.glUniformMatrix4fv(mGLTextureMatrix,1,false,mTextureMatrix,0);
    }

    /**
     * 绑定默认纹理
     */
    protected void onBindTexture(int textureId){
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureId);
        GLES20.glUniform1i(mGLTexture,0);
    }

    protected void onBindFramebuffer(){
        if(mFrameTemp==null){
            mFrameTemp=new int[3];
            GLES20.glGenFramebuffers(1,mFrameTemp,0);
            GLES20.glGenTextures(1,mFrameTemp,1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,mFrameTemp[1]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0,GLES20.GL_RGBA, mWidth, mHeight,
                    0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
            //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_NEAREST);
            //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
            //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);
            //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);

            GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING,mFrameTemp,2);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,mFrameTemp[0]);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20.GL_TEXTURE_2D, mFrameTemp[1], 0);
        }else{
            GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING,mFrameTemp,2);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,mFrameTemp[0]);
        }
    }

    protected void onUnBindFrameBuffer(){
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,mFrameTemp[2]);
    }
}

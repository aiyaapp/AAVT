package com.wuwang.aavt.media;

import com.wuwang.aavt.core.Renderer;
import com.wuwang.aavt.gl.FrameBuffer;
import com.wuwang.aavt.gl.OesFilter;
import com.wuwang.aavt.utils.MatrixUtils;

/**
 * Created by aiya on 2017/9/12.
 */

class WrapRenderer implements Renderer{

    private Renderer mRenderer;
    private OesFilter mFilter;
    private FrameBuffer mFrameBuffer;

    public static final int TYPE_MOVE=0;
    public static final int TYPE_CAMERA=1;

    public WrapRenderer(Renderer renderer){
        this.mRenderer=renderer;
        mFrameBuffer=new FrameBuffer();
        mFilter=new OesFilter();
    }

    public void setFlag(int flag){
        if(flag==TYPE_MOVE){
            mFilter.setVertexCo(MatrixUtils.getOriginalVertexCo());
        }else if(flag==TYPE_CAMERA){
            mFilter.setVertexCo(new float[]{
                    -1.0f, -1.0f,
                    1.0f, -1.0f,
                    -1.0f, 1.0f,
                    1.0f, 1.0f,
            });
        }
    }

    public float[] getTextureMatrix(){
        return mFilter.getTextureMatrix();
    }

    @Override
    public void create() {
        mFilter.create();
        if(mRenderer!=null){
            mRenderer.create();
        }
    }

    @Override
    public void sizeChanged(int width, int height) {
        mFilter.sizeChanged(width, height);
        if(mRenderer!=null){
            mRenderer.sizeChanged(width, height);
        }
    }

    @Override
    public void draw(int texture) {
        if(mRenderer!=null){
            mRenderer.draw(mFilter.drawToTexture(texture));
        }else{
            mFilter.draw(texture);
        }
    }

    @Override
    public void destroy() {
        if(mRenderer!=null){
            mRenderer.destroy();
        }
        mFilter.destroy();
    }
}

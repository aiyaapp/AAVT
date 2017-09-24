package com.wuwang.aavt.gl;

import android.content.res.Resources;

import java.util.Iterator;
import java.util.Vector;

/**
 * Created by Administrator on 2017/9/24 0024.
 */

public class GroupFilter extends BaseFilter {

    private Vector<Filter> mGroup;
    private Vector<Filter> mTempGroup;

    public GroupFilter(Resources resource) {
        super(resource);
    }

    public GroupFilter(){
        super();
    }

    @Override
    protected void initBuffer() {
        super.initBuffer();
        mGroup=new Vector<>();
        mTempGroup=new Vector<>();
    }

    public synchronized void addFilter(Filter filter){
        mGroup.add(filter);
        mTempGroup.add(filter);
    }

    public synchronized void addFilter(int index,Filter filter){
        mGroup.add(index, filter);
        mTempGroup.add(filter);
    }

    public synchronized Filter removeFilter(int index){
        return mGroup.remove(index);
    }

    public boolean removeFilter(Filter filter){
        return mGroup.remove(filter);
    }

    public synchronized Filter element(int index){
        return mGroup.elementAt(index);
    }

    public synchronized Iterator<Filter> iterator(){
        return mGroup.iterator();
    }

    public synchronized boolean isEmpty(){
        return mGroup.isEmpty();
    }

    @Override
    protected synchronized void onCreate() {
        super.onCreate();
        for (Filter filter : mGroup) {
            filter.create();
        }
        mTempGroup.clear();
    }

    private void tempFilterInit(int width,int height){
        for (Filter filter : mTempGroup) {
            filter.create();
            filter.sizeChanged(width, height);
        }
        mTempGroup.removeAllElements();
    }

    @Override
    protected synchronized void onSizeChanged(int width, int height) {
        super.onSizeChanged(width, height);
        for (Filter filter : mGroup) {
            filter.sizeChanged(width, height);
        }
    }

    @Override
    public void draw(int texture) {
        if(mTempGroup.size()>0){
            tempFilterInit(mWidth,mHeight);
        }
        int tempTextureId=texture;
        for (int i=0;i<mGroup.size();i++){
            Filter filter=mGroup.get(i);
            tempTextureId=filter.drawToTexture(tempTextureId);
        }
        super.draw(tempTextureId);
    }

    @Override
    public int drawToTexture(int texture) {
        if(mTempGroup.size()>0){
            tempFilterInit(mWidth,mHeight);
        }
        int tempTextureId=texture;
        for (int i=0;i<mGroup.size();i++){
            Filter filter=mGroup.get(i);
            tempTextureId=filter.drawToTexture(tempTextureId);
        }
        return super.drawToTexture(tempTextureId);
    }

}

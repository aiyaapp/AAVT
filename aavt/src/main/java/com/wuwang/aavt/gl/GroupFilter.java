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
package com.wuwang.aavt.gl;

import android.content.res.Resources;

import java.util.Iterator;
import java.util.Vector;

/**
 * GroupFilter 滤镜组，将多个滤镜串联起来，合并成一个滤镜
 *
 * @author wuwang
 * @version v1.0 2017:10:31 11:53
 */
public class GroupFilter extends LazyFilter {

    private Vector<BaseFilter> mGroup;
    private Vector<BaseFilter> mTempGroup;

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

    public synchronized void addFilter(BaseFilter filter){
        mGroup.add(filter);
        mTempGroup.add(filter);
    }

    public synchronized void addFilter(int index,BaseFilter filter){
        mGroup.add(index, filter);
        mTempGroup.add(filter);
    }

    public synchronized BaseFilter removeFilter(int index){
        return mGroup.remove(index);
    }

    public boolean removeFilter(BaseFilter filter){
        return mGroup.remove(filter);
    }

    public synchronized BaseFilter element(int index){
        return mGroup.elementAt(index);
    }

    public synchronized Iterator<BaseFilter> iterator(){
        return mGroup.iterator();
    }

    public synchronized boolean isEmpty(){
        return mGroup.isEmpty();
    }

    @Override
    protected synchronized void onCreate() {
        super.onCreate();
        for (BaseFilter filter : mGroup) {
            filter.create();
        }
        mTempGroup.clear();
    }

    private void tempFilterInit(int width,int height){
        for (BaseFilter filter : mTempGroup) {
            filter.create();
            filter.sizeChanged(width, height);
        }
        mTempGroup.removeAllElements();
    }

    @Override
    protected synchronized void onSizeChanged(int width, int height) {
        super.onSizeChanged(width, height);
        for (BaseFilter filter : mGroup) {
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
            BaseFilter filter=mGroup.get(i);
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
            BaseFilter filter=mGroup.get(i);
            tempTextureId=filter.drawToTexture(tempTextureId);
        }
        return super.drawToTexture(tempTextureId);
    }

}


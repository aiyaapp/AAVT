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

package com.wuwang.aavt.media;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

/**
 * HardMediaStore 用于硬编码后的视频的存储。可参考{@link android.media.MediaMuxer}的使用。
 * 实现可直接对MediaMuxer封装，也可做其他处理，比如生成aac文件等。
 *
 * @author wuwang
 * @version v1.0 2017:10:26 18:11
 */
public interface HardMediaStore{

    /**
     * 添加指定格式到存储器中
     * @param format 媒体编码格式
     * @return 添加后的轨道索引，-1表示添加失败
     */
    int addFormat(MediaFormat format);

    /**
     * 向指定轨道中添加编码后的数据
     * @param track 轨道索引
     * @param buffer 数据
     * @param info 数据信息
     */
    void addData(int track, ByteBuffer buffer, MediaCodec.BufferInfo info);

    /**
     * 设置媒体数据的存储路径
     * @param path 路径
     */
    void setOutputPath(String path);

}

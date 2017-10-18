package com.wuwang.aavt.gl;

import android.content.res.Resources;

/**
 * Created by 15581 on 2017/9/30.
 */

public class BlackMagicFilter extends GroupFilter {

    public BlackMagicFilter(Resources resources){
        super(resources);
    }

    @Override
    protected void initBuffer() {
        super.initBuffer();
        addFilter(new GrayFilter(mRes));
        addFilter(new BaseFuncFilter(mRes,BaseFuncFilter.FILTER_GAUSS));
        addFilter(new BaseFuncFilter(mRes,BaseFuncFilter.FILTER_GAUSS));
        addFilter(new BaseFuncFilter(mRes,BaseFuncFilter.FILTER_SOBEL));
    }
}

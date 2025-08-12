package com.seewo.eraseaccelerator.util;

import android.content.Context;

/**
 * @author chenyikai
 * @date 2020-06-30
 */
public class DensityUtils {
    public static final float ROUNDING_VALUE = 0.5f;

    private DensityUtils() {
        // NOOP
    }

    /** convert dip to pixel */
    public static int dip2px(Context pContext, float pDipValue) {
        float scale = pContext.getResources().getDisplayMetrics().density;
        return (int) (pDipValue * scale + ROUNDING_VALUE);
    }

    /** convert pixel to dip */
    public static int px2dip(Context pContext, float pPxValue) {
        float scale = pContext.getResources().getDisplayMetrics().density;
        return (int) (pPxValue / scale + ROUNDING_VALUE);
    }
}

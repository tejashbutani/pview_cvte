package com.seewo.eraseaccelerator.util;

import android.animation.ObjectAnimator;
import android.util.Log;
import android.view.View;

/**
 * 有反馈在8386上有闪烁的问题
 */
public class ClearUtil {

    private static final String TAG = "ClearUtil";

    private ClearUtil() { }

    public static void clearDirty(View v) {
        Log.d(TAG, "clearDirty: ");
        ObjectAnimator animator = ObjectAnimator.ofFloat(v, "alpha", 0.99f, 1f);
        animator.setDuration(100);
        animator.start();
    }
}

package com.seewo.eraseaccelerator;

import com.seewo.eraseaccelerator.util.BoardTypeUtil;

/**
 * @author cgb
 * @date 2020-02-25
 * @desc 管理冻屏
 * 正常情况下设置系统属性是不会耗时的，正常情况下执行10000次耗时在3000ms左右（小平台上的测试结果）
 * 但是设置冻屏属性时，执行10000次耗时需要50000ms以上（小平台上的测试结果）
 * 而且频繁设置冻屏属性又时会出现单个系统设置耗时超过50ms的情况
 * 所以尽量避免频繁调用lock方法和unlock方法
 **/
public class AcceleratorLockManager {
    private static boolean mIsLock;

    /**
     * 冻屏，尽量避免频繁调用
     */
    public static void lock() {
        if (!BoardTypeUtil.isNeedFreezeScreen()) {
            return;
        }
        if (!mIsLock) {
            mIsLock = true;
            if (App.getInstance().isUseBaseWriteAccelerator()) {
                RenderAcceleratorManager.lockScreen(true);
            }
        }
    }

    /**
     * 取消冻屏，尽量避免频繁调用
     */
    public static void unLock() {
        if (!BoardTypeUtil.isNeedFreezeScreen()) {
            return;
        }
        mIsLock = false;
        if (App.getInstance().isUseBaseWriteAccelerator()) {
            RenderAcceleratorManager.lockScreen(false);
        }
    }
}

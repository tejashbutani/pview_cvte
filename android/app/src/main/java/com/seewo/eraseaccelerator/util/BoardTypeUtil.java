package com.seewo.eraseaccelerator.util;

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.seewo.easinote.accelerator.base.CvtConfigManager;
import com.seewo.eraseaccelerator.Constants;
import com.seewo.eraseaccelerator.SystemPropertiesInvoke;

/**
 * 设备类型
 *
 * @author chenyikai
 * @date 2020-11-26
 */
public class BoardTypeUtil {

    private static final String TAG = "BoardTypeUtil";
    public static boolean mIs551 = false;
    public static boolean mIs8386 = false;
    public static boolean mIs811 = false;
    public static boolean mIs9950 = false;
    public static boolean mIs972 = false;
    public static boolean mIs982 = false;
    public static boolean mIs3399 = false;

    public static boolean mIs3399NMachine;
    public static boolean mIs3399PMachine;

    public static void init() {
        Log.d(TAG, "BoardTypeUtil: ");
        String boardType = SystemPropertiesInvoke.getString(Constants.KEY_BOARD_TYPE, "");
        if (TextUtils.equals(Constants.BOARD_551, boardType)) {
            Log.d(TAG, "BOARD_551");
            mIs551 = true;
        } else if (TextUtils.equals(Constants.BOARD_811, boardType)) {
            Log.d(TAG, "BOARD_811");
            mIs811 = true;
        } else if (TextUtils.equals(Constants.BOARD_8386, boardType)) {
            Log.d(TAG, "BOARD_8386");
            mIs8386 = true;
        } else if (TextUtils.equals(Constants.BOARD_972, boardType)) {
            Log.d(TAG, "BOARD_972");
            mIs972 = true;
        } else if (TextUtils.equals(Constants.BOARD_982, boardType)) {
            Log.d(TAG, "BOARD_982");
            mIs982 = true;
        } else if (TextUtils.equals(Constants.Board_9950, boardType)) {
            Log.d(TAG, "BOARD_9950");
            mIs9950 = true;
        } else if (TextUtils.equals(Constants.BOARD_3399, boardType)) {
            Log.d(TAG, "BOARD_3399");
            mIs3399 = true;
            mIs3399NMachine = Build.VERSION_CODES.N_MR1 == Build.VERSION.SDK_INT;
            mIs3399PMachine = (28 == Build.VERSION.SDK_INT);
        }
    }

    /**
     * 是否支持Overlay
     * @return
     */
    public static boolean isSupportOverlay() {
        if (mIs3399NMachine || mIs3399PMachine || mIs9950 || mIs972) {
            return true;
        }
        return false;
    }

    public static boolean isNeedFreezeScreen() {
        return CvtConfigManager.isPlatformNeedFreeze();
    }
}
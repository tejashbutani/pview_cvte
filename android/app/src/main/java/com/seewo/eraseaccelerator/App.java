package com.seewo.eraseaccelerator;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;

import com.seewo.eraseaccelerator.util.BoardTypeUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.seewo.eraseaccelerator.R;

/**
 * 全局单例
 * @author chenyikai
 * @date 2020-06-11
 */
public class App {

    private static final String TAG = "App";

    private Context mContext;
    // 屏幕宽高
    private int mScreenWidth;
    private int mScreenHeight;
    private Rect mScreenRect;
    // 白板宽高
    private int mNoteWidth;
    private int mNoteHeight;

    private Rect mVisibleRect;// 可见的rect
    private Handler mMainHandler = new Handler(Looper.getMainLooper());

    private static App sInstance = new App();
    private Map<String, Rect> mForbidDrawArea = new HashMap<>();

    // Multi-window removed
    private boolean mIsEnablePredict = false;

    private App() {
    }

    public static App getInstance() {
        return sInstance;
    }

    public void init(Context context) {
        mContext = context;
        BoardTypeUtil.init();
        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        mScreenWidth = metrics.widthPixels;
        mScreenHeight = metrics.heightPixels;
        mScreenRect = new Rect(0, 0, mScreenWidth, mScreenHeight);

        mNoteWidth = mScreenWidth;
        mNoteHeight = mScreenHeight;
        mVisibleRect = new Rect(0, 0, mScreenWidth, mScreenHeight);
    }

    public int getScreenWidth() {
        return mScreenWidth;
    }

    public int getScreenHeight() {
        return mScreenHeight;
    }

    public void setVisibleRect(Rect rect) {
        Log.d(TAG, "setVisibleRect: " + rect);
        mVisibleRect = rect;
    }

    public Rect getVisibleRect() {
        Log.d(TAG, "getVisibleRect: " + mVisibleRect);
        return mVisibleRect;
    }

    public Rect getScreenRect() {
        Log.d(TAG, "getScreenRect: ");
        return mScreenRect;
    }

    public Handler getMainHandler() {
        return mMainHandler;
    }


    public boolean isUseAllMotionTouch() {
        return mContext.getResources().getBoolean(R.bool.is_use_all_motion_touch);
    }

    /**
     * 获取配置的值
     * @param config
     * @return
     */
    public boolean getConfig(int config) {
        return mContext.getResources().getBoolean(config);
    }

    public boolean isUseBaseWriteAccelerator() {
        return RenderAcceleratorManager.isPlatformSupport();
    }

    // setMultiWinController removed

    private boolean mAllMotionTouch = true;// 全触摸
    public void setAllMotionTouch(boolean allMotionTouch) {
        mAllMotionTouch = allMotionTouch;
    }

    public boolean getAllMotionTouch() {
        return mAllMotionTouch;
    }

    public int getTitleBarHeight() { return 0; }

    public Context getContext() {
        return mContext;
    }

    public void addForbidDrawArea(String key, Rect rect) {
        mForbidDrawArea.put(key, rect);
        refreshForbidDrawAreas();
    }

    public void removeForbidDrawArea(String key) {
        mForbidDrawArea.remove(key);
        RenderAcceleratorManager.removeForbidDrawArea(key);
        refreshForbidDrawAreas();
    }

    private void refreshForbidDrawAreas() {
        if (isUseBaseWriteAccelerator()) {
            Set<String> keys = mForbidDrawArea.keySet();
            for (String key : keys) {
                RenderAcceleratorManager.updateForbidDrawArea(key, mForbidDrawArea.get(key));
            }
        }
    }

    public boolean isCanCallBitmapRecycleMethod() {
        return !isUseBaseWriteAccelerator();
    }

    public boolean isEnablePredict() {
        return mIsEnablePredict;
    }
}

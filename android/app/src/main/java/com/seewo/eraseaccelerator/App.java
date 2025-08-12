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

import play.app.R;

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
    
    /**
     * Ensure App is initialized with context - can be called multiple times safely
     */
    public static void ensureInitialized(Context context) {
        Log.d(TAG, "ensureInitialized() called with context: " + context);
        App instance = getInstance();
        if (instance.mContext == null && context != null) {
            Log.d(TAG, "App not initialized, calling init()");
            instance.init(context);
        } else {
            Log.d(TAG, "App already initialized or context is null (current context: " + instance.mContext + ")");
        }
    }

    public void init(Context context) {
        Log.d(TAG, "App.init() called with context: " + context);
        mContext = context;
        Log.d(TAG, "mContext set to: " + mContext);
        
        try {
            BoardTypeUtil.init();
            Log.d(TAG, "BoardTypeUtil.init() completed");
            
            DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
            mScreenWidth = metrics.widthPixels;
            mScreenHeight = metrics.heightPixels;
            mScreenRect = new Rect(0, 0, mScreenWidth, mScreenHeight);
            Log.d(TAG, "Screen metrics: " + mScreenWidth + "x" + mScreenHeight);

            mNoteWidth = mScreenWidth;
            mNoteHeight = mScreenHeight;
            mVisibleRect = new Rect(0, 0, mScreenWidth, mScreenHeight);
            
            Log.d(TAG, "App.init() completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in App.init()", e);
        }
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
        Log.d(TAG, "isUseAllMotionTouch() called, mContext = " + mContext);
        if (mContext == null) {
            Log.e(TAG, "isUseAllMotionTouch(): mContext is null! Returning default value");
            return true; // Default fallback value
        }
        try {
            boolean result = mContext.getResources().getBoolean(R.bool.is_use_all_motion_touch);
            Log.d(TAG, "isUseAllMotionTouch() returning: " + result);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error in isUseAllMotionTouch()", e);
            return true; // Default fallback value
        }
    }

    /**
     * 获取配置的值
     * @param config
     * @return
     */
    public boolean getConfig(int config) {
        Log.d(TAG, "getConfig() called with config=" + config + ", mContext=" + mContext);
        if (mContext == null) {
            Log.e(TAG, "getConfig(): mContext is null! Returning default value");
            return true; // Default fallback value
        }
        try {
            boolean result = mContext.getResources().getBoolean(config);
            Log.d(TAG, "getConfig() returning: " + result);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error in getConfig()", e);
            return true; // Default fallback value
        }
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

package com.seewo.eraseaccelerator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import com.seewo.easinote.accelerator.base.CvtConfigManager;
import com.seewo.easinote.accelerator.base.IRenderAcceleratorManager;
import com.seewo.easinote.accelerator.base.StateCode;
import com.seewo.easinote.accelerator.base.RenderAcceleratorManagerConfig;
import com.seewo.easinote.accelerator.base.RenderAcceleratorManagerFactory;
import com.seewo.easinote.accelerator.base.RenderAcceleratorManagerFactory.AcceleratorType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RenderAcceleratorManager {
    public static final String TAG = "RenderAcceleratorManager";
    public static final String FORBID_DRAW_KEY_TEST = "TestSetForbidDrawLocation";
    public static final int LAYER_INDEX_BACKGROUND = 0;
    public static final int LAYER_INDEX_FOREGROUND = 1;
    public static final int LAYER_INDEX_ERASER = 2;
    public static final int LAYER_INDEX_COVER_START = LAYER_INDEX_ERASER + 1;
    private static volatile Map<String, Integer> mLayerIndexOfCoverMap = new HashMap<>();

    public static final String LAYER_KEY_COVER_PREFIX = "layer_key_cover_";


    private static IRenderAcceleratorManager mRenderAcceleratorManager;
    private static AcceleratorType mAcceleratorType = AcceleratorType.CVTRender;

    public static IRenderAcceleratorManager getAcceleratorManager() {
        if (mRenderAcceleratorManager == null) {
            mRenderAcceleratorManager = RenderAcceleratorManagerFactory.getRenderAcceleratorManagerByType(mAcceleratorType);
        }
        return mRenderAcceleratorManager;
    }

    public static void init(Context context, boolean isNeedRemoteRender) {
        if (isNeedRemoteRender) {
            mAcceleratorType = AcceleratorType.ServerRender;
        }
        RenderAcceleratorManagerConfig builder = RenderAcceleratorManagerConfig
                .obtain(context)
                .setScreenFreeze(false)
                .setUseEraserMode(true)
                .setForegroundLayerIndex(LAYER_INDEX_FOREGROUND)
                .setEraserLayerIndex(LAYER_INDEX_ERASER);
        if (App.getInstance().isEnablePredict() && isNeedRemoteRender) {
            builder.setLayerBitmapNeedUpdateToShareMemory(true);
        }
        boolean isSuccess = getAcceleratorManager().init(builder);
        logStateCode("init", isSuccess);
    }

    public static void destroy() {
        recycleAllResources();
        boolean isSuccess = getAcceleratorManager().destroy();
        logStateCode("destroy", isSuccess);
    }

    private static void recycleAllResources() {
        removeBackgroundLayer();
        removeForegroundLayer();
        removeEraserLayer();
        clearAllCover();
    }

    public static void removeBackgroundLayer() {
        removeCover(LAYER_INDEX_BACKGROUND);
    }

    public static void removeForegroundLayer() {
        removeCover(LAYER_INDEX_FOREGROUND);
    }

    public static void setForeground(Bitmap bitmap) {
        boolean isSuccess = getAcceleratorManager().setLayer(LAYER_INDEX_FOREGROUND, 0, 0, bitmap);
        logStateCode("setForeground", isSuccess);
    }

    /**
     * 设置橡皮擦图层
     *
     * @param bitmap 橡皮擦Bitmap
     * @param left   橡皮擦左上角x坐标
     * @param Top    橡皮擦左上角y坐标
     */
    public static void setEraserLayer(Bitmap bitmap, int left, int Top) {
        boolean isSuccess = getAcceleratorManager().setLayer(LAYER_INDEX_ERASER, left, Top, bitmap);
        logStateCode("setEraserLayer", isSuccess);
    }

    public static void removeEraserLayer() {
        boolean isSuccess = getAcceleratorManager().recycleLayer(LAYER_INDEX_ERASER);
        logStateCode("recycleEraserLayer", isSuccess);
    }

    public static void setEraserMode(boolean eraserMode, int w, int h) {
        getAcceleratorManager().setEraserMode(eraserMode, w, h);
    }

    public static void renderEraser(Rect lastRect, Rect curRect) {
        getAcceleratorManager().renderEraser(lastRect, curRect);
    }


    public synchronized static void setCover(String coverKey, int x, int y, Bitmap bitmap) {
        int coverIndex = LAYER_INDEX_COVER_START;
        if (mLayerIndexOfCoverMap.containsKey(coverKey) && mLayerIndexOfCoverMap.get(coverKey) != null) {
            coverIndex = mLayerIndexOfCoverMap.get(coverKey);
        } else {
            coverIndex = coverIndex + mLayerIndexOfCoverMap.size();
        }

        boolean isSuccess = getAcceleratorManager().setLayer(coverIndex, x, y, bitmap);
        if (isSuccess) {
            mLayerIndexOfCoverMap.put(coverKey, coverIndex);
        }
    }

    public synchronized static void removeCover(String coverKey) {
        int index = -1;
        if (mLayerIndexOfCoverMap.get(coverKey) != null) {
            index = mLayerIndexOfCoverMap.get(coverKey);
        }
        if (index != -1) {
            boolean isSuccess = removeCover(index);
            if (isSuccess) {
                mLayerIndexOfCoverMap.remove(coverKey);
            }
        }
    }

    private static boolean removeCover(int index) {
        boolean isSuccess = getAcceleratorManager().recycleLayer(index);

        logStateCode("removeCover", isSuccess);
        return isSuccess;
    }

    public synchronized static void clearAllCover() {
        Set<String> keys = mLayerIndexOfCoverMap.keySet();
        Set<String> keysCopy = new HashSet<>(keys);
        for (String key : keysCopy) {
            removeCover(key);
        }
    }

    public static void render(Rect dirtyRect) {
        boolean isSuccess = getAcceleratorManager().render(dirtyRect);
    }

    public static void setRenderable(boolean enable) {
        boolean isSuccess = getAcceleratorManager().setRenderable(enable);
        logStateCode("setRenderable", isSuccess);
    }

    public static void prepareToRender() {
        boolean isSuccess = getAcceleratorManager().prepareToRender();
        logStateCode("prepareToRender", isSuccess);
    }

    public static void finishRender() {
        boolean isSuccess = getAcceleratorManager().finishRender();
        logStateCode("finishRender", isSuccess);
    }

    public static void clearAll() {
        boolean isSuccess = getAcceleratorManager().cleanAll();
        logStateCode("clearAll", isSuccess);
    }

    public static void clean(Rect rect) {
        boolean isSuccess = getAcceleratorManager().clean(rect);
        logStateCode("clearAll", isSuccess);
    }

    public static void cleanAllImmediately() {
        boolean isSuccess = getAcceleratorManager().cleanAllImmediately();
        logStateCode("cleanAllImmediately", isSuccess);
    }

    public static void updateForbidDrawArea(String key, Rect rect) { }

    public static void removeForbidDrawArea(String key) { }

    public static void removeAllForbidDrawArea() { }

    public static void refreshAllForbidDrawArea() { }

    public static void lockScreen(boolean isLock) {
        getAcceleratorManager().lockScreen(isLock);
    }

    public static void setRenderBounds(Rect rect) {
        if (rect != null) {
            Log.i(TAG, " setRenderBounds rect = " + rect);
        }
        boolean isSuccess = getAcceleratorManager().setRenderBounds(rect);
        logStateCode("setRenderBounds ", isSuccess);
    }

    private static void logStateCode(String methodName, boolean isSuccess) {
        StateCode stateCode = getAcceleratorManager().getCurrentStateCode();
        if (isSuccess) {
            Log.i(TAG, "call " + methodName + " " + stateCode.toString());
        } else {
            Log.e(TAG, "call " + methodName + " " + stateCode.toString());
        }
    }

    public static boolean isPlatformSupport() {
        return CvtConfigManager.isPlatformSupport();
    }
}

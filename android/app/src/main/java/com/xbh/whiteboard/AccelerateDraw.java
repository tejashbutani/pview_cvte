package com.xbh.whiteboard;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

public class AccelerateDraw {
    public static final String TAG = AccelerateDraw.class.getSimpleName();

    private static final AccelerateDraw mfbd = new AccelerateDraw();

    //防止通过单例直接调用到加速库接口
    private AccelerateDraw() {
    }

    public static AccelerateDraw getInstance() {
        return mfbd;
    }

    static {
        // System.loadLibrary("AccelerateDraw");
        try {
            String libraryPath = Build.SUPPORTED_64_BIT_ABIS.length > 0
                ? "/system/lib64/libAccelerateDraw.so"
                : "/system/lib/libAccelerateDraw.so";
            
            System.load(libraryPath);
            Log.d(TAG, "Successfully loaded AccelerateDraw from " + libraryPath);
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load AccelerateDraw library: " + e.getMessage());
            throw new RuntimeException("Failed to load AccelerateDraw library. Make sure the app has system permissions.", e);
        }
    }

    public native String getVersion();

    //加速库初始化
    public native void accelerateInit(int w, int h);

    //加速库注销，释放资源
    public native void accelerateDeInit();

    public native void startAccelerateDraw();

    @Deprecated
    public native void refreshAccelerateDraw(int x, int y, int w, int h, Bitmap bitmap);

    public native void refreshAccelerateDrawV2(int x, int y, int w, int h, Bitmap bitmap, int bitmapX, int bitmapY,boolean isNotFilter);


    //一般是 停止加速后，再延时100ms,清空加速层数据
    public void stopAndClearAccelerate(){
        stopAccelerateDraw();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        clearAccLayer();
    }

    public native boolean stopAccelerateDraw();

    public native void clearAccLayer();


    public void clearAccLayerRect(Rect rect){
        clearAccLayerRect(rect.left,rect.top,rect.width(),rect.height());
    }

    public native void clearAccLayerRect(int x, int y, int w, int h);


    //---------  以下为过期方法，不推荐使用------
    @Deprecated
    public native void showTrackBall(boolean flag);
    
    @Deprecated
    public native void refreshSurface(Surface surface, Bitmap bitmap);

    @Deprecated
    public native void getActivityBitmap(Bitmap bitmap);

    @Deprecated
    public native void setStartEventPull(boolean flag);

    @Deprecated
    public void callback(int id, float x, float y, float width, float height, float stroke, int status, int type){
    }

}
package com.seewo.eraseaccelerator.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.seewo.eraseaccelerator.IDrawView;
import com.seewo.eraseaccelerator.IFullScreenBitmapHolder;
import com.seewo.eraseaccelerator.shapes.Pen;
import com.seewo.eraseaccelerator.util.ClearUtil;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * SurfaceView实现
 *
 * @author chenyikai
 * @date 2020-07-24
 */
public class DrawSurfaceView extends SurfaceView implements IDrawView, SurfaceHolder.Callback {
    private static final String TAG = "DrawSurfaceView";

    private Canvas mFullScreenCanvas;
    private SurfaceHolder mSurfaceHolder;
    private IFullScreenBitmapHolder mFullScreenBitmapHolder;
    private boolean isDrawing = false;
    private static final int MSG_REFRESH = 100;
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REFRESH:{
                    ClearUtil.clearDirty(DrawSurfaceView.this);
                    break;
                }
                default:
                    break;
            }
        }
    };

    public DrawSurfaceView(Context context) {
        super(context, null);
    }

    public DrawSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initSurface();
    }

    private void initSurface() {
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setFormat(PixelFormat.TRANSLUCENT);
    }

    @Override
    public void setFullScreenBitmapHolder(IFullScreenBitmapHolder bitmapHolder) {
        mFullScreenBitmapHolder = bitmapHolder;
        mFullScreenCanvas = mFullScreenBitmapHolder.getFullScreenCanvas();
    }

    @Override
    public void setRoamState(boolean isRoamState) {

    }

    @Override
    public void setRoamScale(float scale) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated: ");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged: ");
        mFullScreenCanvas = mSurfaceHolder.lockCanvas();
        if (mFullScreenBitmapHolder != null && mFullScreenCanvas != null) {
            Log.d(TAG, "drawFullScreenBitmap");
            mFullScreenBitmapHolder.drawFullScreenBitmap(mFullScreenCanvas);
            mSurfaceHolder.unlockCanvasAndPost(mFullScreenCanvas);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed: ");
        isDrawing = false;
        mFullScreenCanvas = null;
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public void onTouchDown() {
        isDrawing = true;
    }

    @Override
    public void onTouchUp() {
        isDrawing = false;
        refreshView();
    }

    @Override
    public void fullTransform(Matrix matrix) {

    }

    @Override
    public void finishRoam() {

    }

    @Override
    public void redrawAll(List<Pen> penList) {
        Log.d(TAG, "redrawAll on SurfaceView.");
        Canvas canvas = mSurfaceHolder.lockCanvas();
        for (Pen pen : penList) {
            pen.draw(canvas);
        }
        mSurfaceHolder.unlockCanvasAndPost(canvas);
    }

    @Override
    public void refreshView() {
        mFullScreenCanvas = mSurfaceHolder.lockCanvas();
        if (mFullScreenCanvas != null) {
            Log.d(TAG, "mFullScreenCanvas is not null.");
            mFullScreenBitmapHolder.drawFullScreenBitmap(mFullScreenCanvas);
            mSurfaceHolder.unlockCanvasAndPost(mFullScreenCanvas);
            Log.d(TAG, "refreshView: end");
        }
    }
}
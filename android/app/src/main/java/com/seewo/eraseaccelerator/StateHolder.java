package com.seewo.eraseaccelerator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;

import com.seewo.eraseaccelerator.data.ContentCenter;
import com.seewo.eraseaccelerator.shapes.ColorBackground;
import com.seewo.eraseaccelerator.shapes.Pen;
import com.seewo.eraseaccelerator.state.AbstractState;
import com.seewo.eraseaccelerator.state.BrushState;
import com.seewo.eraseaccelerator.view.IToolbar;

import java.util.List;

/**
 * holder BrushState or EraserState
 */
public class StateHolder implements IFullScreenBitmapHolder {
    private static final String TAG = "StateHolder";
    private Canvas mFullScreenCanvas;
    private Bitmap mFullScreenBitmap;

    private final IDrawView mDrawView;
    private final BrushState mBrushState;

    private AbstractState mCurrentState;
    private final ContentCenter mContentCenter;

    private IBackground mBackground;
    private ColorBackground mColorBackground;
    private IToolbar mToolbar;
    private Context mContext;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private Runnable mTouchUpRunnable = new Runnable() {
        @Override
        public void run() {
            mDrawView.onTouchUp();
            refresh();
            if (App.getInstance().isUseBaseWriteAccelerator()) {
                RenderAcceleratorManager.cleanAllImmediately();
            }
        }
    };

    StateHolder(Context context, IDrawView drawView, IToolbar toolbar) {
        mContext = context;
        mDrawView = drawView;
        mDrawView.setFullScreenBitmapHolder(this);
        mToolbar = toolbar;

        mContentCenter = new ContentCenter(mDrawView);
        mBrushState = new BrushState(this, mContentCenter);
        mColorBackground = new ColorBackground();
        mBackground = mColorBackground;
    }

    /**
     * open framebuffer
     */
    void onCreate(int widthPixels, int heightPixels) {
        mFullScreenBitmap = Bitmap.createBitmap(widthPixels, heightPixels, Bitmap.Config.ARGB_8888);
        mFullScreenCanvas = new Canvas(mFullScreenBitmap);

        drawBackground();

        RenderAcceleratorManager.setForeground(mFullScreenBitmap);
    }

    @Override
    public void drawBackground() {
        mBackground.draw(mFullScreenCanvas);
    }

    @Override
    public void drawBackground(Canvas canvas) {
        if (canvas == null) {
            Log.e(TAG, "fail to drawBackground, canvas == null");
            return;
        }
        mBackground.draw(canvas);
    }

    public int getBackgroundColor() {
        int color = Color.BLACK;
        if (mBackground instanceof ColorBackground) {
            color = ((ColorBackground) mBackground).getColor();
        }
        return color;
    }

    @Override
    public void drawBackgroundOnVisibleRect(Rect rect) {
        mBackground.draw(mFullScreenCanvas);
    }

    /**
     * close framebuffer
     */
    void onDestroy() {
        if (App.getInstance().isCanCallBitmapRecycleMethod() && null != mFullScreenBitmap && !mFullScreenBitmap.isRecycled()) {
            mFullScreenBitmap.recycle();
        }
        mCurrentState.destroy();
    }

    public void onResume() {
        if (App.getInstance().isUseBaseWriteAccelerator()) {
            RenderAcceleratorManager.setRenderable(true);
        }
    }

    public void onPause() {
        if (App.getInstance().isUseBaseWriteAccelerator()) {
            RenderAcceleratorManager.setRenderable(false);
        }
    }

    /**
     * @param event pointer touch event
     */
    void onTouchEvent(MotionEvent event) {
        int action = event.getAction();
//        if (mIsRoamOn) {
//            // 漫游状态
//            mCurrentState = mRoamState;
//            mCurrentState.onTouchEvent(event);
//            return;
//        }
        // 书写或者擦除的状态
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mMainHandler.removeCallbacks(mTouchUpRunnable);
                Log.d(TAG, "Touch Down---");
                mDrawView.onTouchDown();
                synchronized (this) {
                    // Force brush-only behavior
                    mCurrentState = mBrushState;
                    Log.d(TAG, "====mCurrentState is class: " + mCurrentState.getClass());
                    mCurrentState.onTouchEvent(event);
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                Log.d(TAG, "Touch Up---");
                mCurrentState.onTouchEvent(event);
                mMainHandler.postDelayed(mTouchUpRunnable, 300);
                break;
            }
            default: {
                mCurrentState.onTouchEvent(event);
                break;
            }
        }
    }

    @Override
    public Canvas getFullScreenCanvas() {
        return mFullScreenCanvas;
    }

    @Override
    public Bitmap getFullScreenBitmap() {
        return mFullScreenBitmap;
    }

    @Override
    public void drawFullScreenBitmap(Canvas canvas) {
        if (mFullScreenBitmap != null && !mFullScreenBitmap.isRecycled()) {
            canvas.drawBitmap(mFullScreenBitmap, 0, 0, null);
        }
    }

    public void refresh() {
        Log.d(TAG, "refresh: =====");
        drawBackground();
        mContentCenter.redrawAll(mFullScreenCanvas);
        mDrawView.refreshView();
    }

    public void toColorBackground(int color) {
        Log.d(TAG, "toColorBackground: ");
        mColorBackground.setBgColor(color);
        mBackground = mColorBackground;
        drawBackground();

        mContentCenter.redrawAll(mFullScreenCanvas);
        mDrawView.refreshView();
    }

    public void toImageBackground() { /* no-op */ }

    public void setStrokeWidth(float strokeWidth) {
        mBrushState.setStrokeWidth(strokeWidth);
    }

    public void setPenColor(int penColor) {
        mBrushState.setPenColor(penColor);
    }

    public void clearAllStroke() {
        mContentCenter.clear();
        mFullScreenCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        drawBackground();
        mContentCenter.redrawAll(mFullScreenCanvas);
        mDrawView.refreshView();

        if (App.getInstance().isUseBaseWriteAccelerator()) {
            RenderAcceleratorManager.clearAll();
        }
    }

    /**
     * 设置橡皮檫状态
     *
     * @param isEraserOn
     */
    public void setEraserState(boolean isEraserOn) { }

    /**
     * 设置漫游状态
     *
     * @param isRoamOn
     */
    public void setRoamState(boolean isRoamOn) { }

    /**
     * 操作浮窗
     *
     * @param isShow
     */
    public void operateFloatView(boolean isShow) { }

    // 漫游缩放的监听
    // IScaleListener removed in simplified build

    @Override
    public void endTransform(android.graphics.Matrix mFinalMatrix) {
        // 重新备份mSelectedShapes,防止极端操作的时候,获取mSelectedShapes里面对象的时候数组越界
        Log.d(TAG, "endTransform:=====");
        List<Pen> penList = mContentCenter.getAllPens();
        int size = penList.size();
        if (size <= 0) {
            return;
        }
        for (int i = 0; i < size; i++) {
            Pen pen = penList.get(i);
            if (pen == null) {
                continue;
            }
            pen.transform(mFinalMatrix);
        }
        mContentCenter.redrawAll(mFullScreenCanvas);
        mDrawView.refreshView();
    }

    @Override
    public void addDirtyRect(Rect rect) {
        RenderAcceleratorManager.render(rect);
    }

    @Override
    public void refreshDrawView() {
        mDrawView.refreshView();
    }
    
    public ContentCenter getContentCenter() {
        return mContentCenter;
    }
}

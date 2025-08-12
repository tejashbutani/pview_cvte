package com.xbh.whiteboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewTreeObserver;
import io.flutter.plugin.common.MethodChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.graphics.PointF;

//import com.xbh.whiteboard.AccelerateDraw;

public class DrawSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private final String TAG = DrawSurfaceView.class.getSimpleName();
    private final Handler handler = new Handler();
    private ViewTreeObserver.OnGlobalLayoutListener mPreDrawListener;

    private SurfaceHolder mSurfaceHolder = null;
    private Paint mPaint = null;
    private Rect mScreenRect = null;
    private Bitmap mCacheBitmap = null;//成熟区，保存的是已经绘制的笔迹
    private Bitmap mDrawBitmap = null;//刷新区，保存的是书写过程传给加速库的笔迹
    private Canvas mCacheCanvas = null;
    private Canvas mDrawCanvas = null;//书写画布

    private SparseArray<IDrawer> mPencilList = new SparseArray<>();
    private static final Object PEN_LOCKER = new Object();
//    private AccelerateDraw mAcd = AccelerateDraw.getInstance();

    private Rect mViewRect = new Rect();
    private MethodChannel methodChannel;
    private List<PointF> currentStrokePoints = new ArrayList<>();
    private boolean isDashed = false;
    private static final float DASH_LENGTH = 30f;
    private static final float GAP_LENGTH = 20f;

    public DrawSurfaceView(Context context) {
        super(context);
        init();
    }

    public DrawSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        Log.d(TAG, "DrawSurfaceView init");
        mSurfaceHolder = this.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        mScreenRect = new Rect(0, 0, Util.SCREEN_WIDTH, Util.SCREEN_HEIGHT);

        // Create cache bitmap
        mCacheBitmap = Bitmap.createBitmap(Util.SCREEN_WIDTH, Util.SCREEN_HEIGHT, Bitmap.Config.ARGB_8888);
        mCacheBitmap.eraseColor(Color.TRANSPARENT);
        mCacheCanvas = new Canvas(mCacheBitmap);

        // Create drawing bitmap
        mDrawBitmap = Bitmap.createBitmap(Util.SCREEN_WIDTH, Util.SCREEN_HEIGHT, Bitmap.Config.ARGB_8888);
        mDrawBitmap.eraseColor(Color.TRANSPARENT);
        mDrawCanvas = new Canvas(mDrawBitmap);

        // Create the pre-draw listener
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                onPreDraw();
            }
        };

        // Initialize the layout listener properly
        mPreDrawListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (handler != null) {
                    handler.removeCallbacks(runnable);
                    handler.postDelayed(runnable, 100);
                }
            }
        };

        // Add the layout listener
        getViewTreeObserver().addOnGlobalLayoutListener(mPreDrawListener);
    }

    private void onPreDraw() {
        boolean update = updateViewRect();
        if (update) {
//            mAcd.stopAndClearAccelerate();
            requestCacheDraw();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // Remove the layout listener when the view is detached
        if (mPreDrawListener != null) {
            getViewTreeObserver().removeOnGlobalLayoutListener(mPreDrawListener);
            mPreDrawListener = null;
        }
        // Clean up handler
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");
//        AccelerateDraw.getInstance().accelerateDeInit();
//        AccelerateDraw.getInstance().accelerateInit(Util.SCREEN_WIDTH, Util.SCREEN_HEIGHT);
//        AccelerateDraw.getInstance().stopAndClearAccelerate();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged width = " + width + " height=" + height);
        Util.OVERRIDE_SCREEN_WIDTH = width;
        Util.OVERRIDE_SCREEN_HEIGHT = height;
        updateViewRect();
        requestCacheDraw();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
//        mAcd.stopAndClearAccelerate();
//        mAcd.accelerateDeInit();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                MotionEventTouchDown(event);
                break;
            case MotionEvent.ACTION_MOVE:
                MotionEventTouchMove(event);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                MotionEventTouchUp(event, false);
                break;
            case MotionEvent.ACTION_UP:
                MotionEventTouchUp(event, true);
                break;
            default:
                break;
        }
        return true;
    }

    private void MotionEventTouchDown(MotionEvent event) {
        int curPointerIndex = event.getActionIndex();
        int curPointerId = event.getPointerId(curPointerIndex);
        float x = event.getX(curPointerIndex);
        float y = event.getY(curPointerIndex);
        touchDown(curPointerId, x, y);
    }

    private void touchDown(int curPointerId, float x, float y) {
        IDrawer drawer = toCreateDrawer(curPointerId);
//        mAcd.startAccelerateDraw();
        drawer.touchDown(x, y);
    }

    private void MotionEventTouchMove(MotionEvent event) {
        int pointerCount = event.getPointerCount();
        for (int i = 0; i < pointerCount; i++) {
            paintDraw(event.getPointerId(i), event.getX(i), event.getY(i));
        }
    }

    private void paintDraw(int curPointerId, float x, float y) {
        IDrawer drawer = getDrawer(curPointerId);
        if (drawer != null && drawer.touchMove(x, y)) {
            drawer.draw(this);
        }
    }

    private void MotionEventTouchUp(MotionEvent event, boolean releaseAll) {
        int curPointerIndex = event.getActionIndex();
        touchUp(event.getPointerId(curPointerIndex), event.getX(curPointerIndex), 
               event.getY(curPointerIndex), releaseAll);
    }

    private void touchUp(int curPointerId, float x, float y, boolean releaseAll) {
        synchronized (PEN_LOCKER) {
            IDrawer drawer = getDrawer(curPointerId);
            if (drawer != null && methodChannel != null) {
                // Get the points from the drawer
                List<PointF> points = drawer.getPoints(); // You'll need to add this method to IDrawer
                
                Map<String, Object> strokeData = new HashMap<>();
                List<Map<String, Double>> pointsList = new ArrayList<>();
                float density = getResources().getDisplayMetrics().density;

                for (PointF point : points) {
                    Map<String, Double> pointMap = new HashMap<>();
                    pointMap.put("x", (double) (point.x / density));
                    pointMap.put("y", (double) (point.y / density));
                    pointsList.add(pointMap);
                }

                strokeData.put("points", pointsList);
                strokeData.put("color", mPaint.getColor());
                strokeData.put("width", (double) (mPaint.getStrokeWidth() / density));

                methodChannel.invokeMethod("onStrokeComplete", strokeData);
            }

            paintUp(curPointerId, x, y);
            if (releaseAll) {
                clearAllDrawer();
                requestCacheDraw();
//                mAcd.stopAndClearAccelerate();
            }
        }
    }

    private void paintUp(int curPointerId, float x, float y) {
        IDrawer drawer = getDrawer(curPointerId);
        if (drawer != null) {
            drawer.touchUp(x, y, this);
            mPencilList.remove(curPointerId);
        }
    }

    public boolean updateViewRect() {
        int[] position = new int[2];
        getLocationOnScreen(position);
        int left = position[0];
        int top = position[1];
        int right = position[0] + getWidth();
        int bottom = position[1] + getHeight();
        if (left == mViewRect.left && top == mViewRect.top
                && right == mViewRect.right && bottom == mViewRect.bottom) {
            return false;
        }
        mViewRect.set(left, top, right, bottom);
        Log.d(TAG, "updateViewRect =" + mViewRect);
        return true;
    }

    public void refreshCache(Rect rect) {
        if (rect != null) {
            int left = mViewRect.left + rect.left;
            int top = mViewRect.top + rect.top;
//            mAcd.refreshAccelerateDrawV2(left, top, rect.width(), rect.height(),
//                    mDrawBitmap, rect.left, rect.top, false);
        }
    }

    public void requestCacheDraw() {
        synchronized (mSurfaceHolder) {
            mCacheCanvas.drawBitmap(mDrawBitmap, null, mScreenRect, null);
            mDrawBitmap.eraseColor(Color.TRANSPARENT);

            Canvas canvas = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                canvas = mSurfaceHolder.lockHardwareCanvas();
            }
            if (canvas == null) return;
            canvas.drawBitmap(mCacheBitmap, null, mScreenRect, null);

            mSurfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    private IDrawer getDrawer(int pointId) {
        return mPencilList.get(pointId);
    }

    private void clearAllDrawer() {
        mPencilList.clear();
    }

    private IDrawer toCreateDrawer(int pointId) {
        if (mPaint == null) {
            toCreatePaint();
        }
        IDrawer drawer = new Pencil(mPaint);
        mPencilList.append(pointId, drawer);
        return drawer;
    }

    private void toCreatePaint() {
        mPaint = new Paint();
        float size = 5.0f;
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setColor(Color.GREEN);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(size);
    }

    public Canvas getDrawCanvas() {
        return mDrawCanvas;
    }

    public void setMethodChannel(MethodChannel channel) {
        this.methodChannel = channel;
    }

    public void setPenColor(int color) {
        if (mPaint != null) {
            mPaint.setColor(color);
            if (Color.alpha(color) < 255) {
                // Highlighter settings
                mPaint.setStrokeCap(Paint.Cap.SQUARE);
                mPaint.setAlpha(75); // Default highlighter alpha
            } else {
                // Normal pen settings
                mPaint.setStrokeCap(Paint.Cap.ROUND);
                mPaint.setAlpha(255);
            }
        }
    }

    public void setPenWidth(float width) {
        if (mPaint != null) {
            float density = getResources().getDisplayMetrics().density;
            mPaint.setStrokeWidth(width * density);
        }
    }

    public void setDashed(boolean dashed) {
        isDashed = dashed;
        if (mPaint != null) {
            if (dashed) {
                // Set up dashed effect if needed
                mPaint.setPathEffect(new android.graphics.DashPathEffect(
                    new float[]{DASH_LENGTH, GAP_LENGTH}, 0));
            } else {
                mPaint.setPathEffect(null);
            }
        }
    }

    public void clear() {
        // Clear the canvas
        mCacheBitmap.eraseColor(Color.TRANSPARENT);
        mDrawBitmap.eraseColor(Color.TRANSPARENT);
        requestCacheDraw();
//        mAcd.stopAndClearAccelerate();
    }
}

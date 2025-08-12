package com.x;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewTreeObserver;
import io.flutter.plugin.common.MethodChannel;

import com.seewo.eraseaccelerator.IDrawView;
import com.seewo.eraseaccelerator.RenderAcceleratorManager;
import com.seewo.eraseaccelerator.StateHolder;
import com.seewo.eraseaccelerator.view.IToolbar;
import com.seewo.easinote.accelerator.base.util.SystemUnlockUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MinimalAcceleratedSurfaceView - A SurfaceView that uses the MinimalAcceleratedActivity architecture
 * for hardware-accelerated drawing with company-specific acceleration features.
 */
public class MinimalAcceleratedSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private final String TAG = MinimalAcceleratedSurfaceView.class.getSimpleName();
    private final Handler handler = new Handler();
    private ViewTreeObserver.OnGlobalLayoutListener mPreDrawListener;

    private SurfaceHolder mSurfaceHolder = null;
    private Paint mPaint = null;
    private Rect mScreenRect = null;
    private Bitmap mCacheBitmap = null; // Mature area, saves drawn strokes
    private Bitmap mDrawBitmap = null;  // Refresh area, saves strokes being written
    private Canvas mCacheCanvas = null;
    private Canvas mDrawCanvas = null;  // Drawing canvas

    private Rect mViewRect = new Rect();
    private MethodChannel methodChannel;
    private List<PointF> currentStrokePoints = new ArrayList<>();
    private boolean isDashed = false;
    private static final float DASH_LENGTH = 30f;
    private static final float GAP_LENGTH = 20f;

    // MinimalAcceleratedActivity components
    private IDrawView mDrawView;
    private StateHolder mStateHolder;
    private Context mContext;
    private boolean isAcceleratorInitialized = false;

    public MinimalAcceleratedSurfaceView(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public MinimalAcceleratedSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    private void init() {
        Log.d(TAG, "MinimalAcceleratedSurfaceView init");
        mSurfaceHolder = this.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        mScreenRect = new Rect(0, 0, Util.getScreenWidth(), Util.getScreenHeight());

        // Create cache bitmap
        mCacheBitmap = Bitmap.createBitmap(Util.getScreenWidth(), Util.getScreenHeight(), Bitmap.Config.ARGB_8888);
        mCacheBitmap.eraseColor(Color.TRANSPARENT);
        mCacheCanvas = new Canvas(mCacheBitmap);

        // Create drawing bitmap
        mDrawBitmap = Bitmap.createBitmap(Util.getScreenWidth(), Util.getScreenHeight(), Bitmap.Config.ARGB_8888);
        mDrawBitmap.eraseColor(Color.TRANSPARENT);
        mDrawCanvas = new Canvas(mDrawBitmap);

        // Initialize acceleration components
        initAccelerator();

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

    private void initAccelerator() {
        try {
            // Ensure container is registered for acceleration service
            SystemUnlockUtil.addWriteAcceleratorContainer(new WeakReference<>(mContext));

            // Init accelerator and state holder
            RenderAcceleratorManager.init(mContext, false);

            IToolbar dummyToolbar = new IToolbar() {
                @Override
                public boolean isEraserEnable() { return false; }
                @Override
                public void setStateHolder(StateHolder stateHolder) { /* no-op */ }
            };

            // Use the existing DrawSurfaceView from the seewo package for acceleration
            mDrawView = new com.seewo.eraseaccelerator.view.DrawSurfaceView(mContext);
            mStateHolder = new StateHolder(mContext, mDrawView, dummyToolbar);

            DisplayMetrics metrics = getResources().getDisplayMetrics();
            Log.d(TAG, "screen: " + metrics.widthPixels + "x" + metrics.heightPixels);
            mStateHolder.onCreate(metrics.widthPixels, metrics.heightPixels);

            isAcceleratorInitialized = true;
            Log.d(TAG, "Accelerator initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize accelerator: " + e.getMessage());
            isAcceleratorInitialized = false;
        }
    }

    private void onPreDraw() {
        boolean update = updateViewRect();
        if (update) {
            requestCacheDraw();
            if (isAcceleratorInitialized) {
                updateRenderBounds();
            }
        }
    }

    private void updateRenderBounds() {
        int[] loc = new int[2];
        getLocationOnScreen(loc);
        Rect bounds = new Rect(loc[0], loc[1], loc[0] + getWidth(), loc[1] + getHeight());
        RenderAcceleratorManager.setRenderBounds(bounds);
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
        // Clean up accelerator
        if (isAcceleratorInitialized && mStateHolder != null) {
            mStateHolder.onDestroy();
            RenderAcceleratorManager.destroy();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");
        if (isAcceleratorInitialized) {
            RenderAcceleratorManager.setRenderable(true);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged width = " + width + " height=" + height);
        Util.OVERRIDE_SCREEN_WIDTH = width;
        Util.OVERRIDE_SCREEN_HEIGHT = height;
        updateViewRect();
        requestCacheDraw();
        if (isAcceleratorInitialized) {
            updateRenderBounds();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
        if (isAcceleratorInitialized) {
            RenderAcceleratorManager.setRenderable(false);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Use accelerated touch handling if available
        if (isAcceleratorInitialized && mStateHolder != null) {
            mStateHolder.onTouchEvent(event);
        }

        // Also handle traditional touch events for stroke data extraction
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
        float x = event.getX(curPointerIndex);
        float y = event.getY(curPointerIndex);
        
        currentStrokePoints.clear();
        currentStrokePoints.add(new PointF(x, y));
    }

    private void MotionEventTouchMove(MotionEvent event) {
        int pointerCount = event.getPointerCount();
        for (int i = 0; i < pointerCount; i++) {
            float x = event.getX(i);
            float y = event.getY(i);
            currentStrokePoints.add(new PointF(x, y));
        }
    }

    private void MotionEventTouchUp(MotionEvent event, boolean releaseAll) {
        int curPointerIndex = event.getActionIndex();
        float x = event.getX(curPointerIndex);
        float y = event.getY(curPointerIndex);
        currentStrokePoints.add(new PointF(x, y));

        if (releaseAll && methodChannel != null) {
            // Send stroke data to Flutter
            Map<String, Object> strokeData = new HashMap<>();
            List<Map<String, Double>> pointsList = new ArrayList<>();
            float density = getResources().getDisplayMetrics().density;

            for (PointF point : currentStrokePoints) {
                Map<String, Double> pointMap = new HashMap<>();
                pointMap.put("x", (double) (point.x / density));
                pointMap.put("y", (double) (point.y / density));
                pointsList.add(pointMap);
            }

            strokeData.put("points", pointsList);
            strokeData.put("color", mPaint != null ? mPaint.getColor() : Color.BLACK);
            strokeData.put("width", mPaint != null ? (double) (mPaint.getStrokeWidth() / density) : 5.0);
            strokeData.put("isDashed", isDashed);

            methodChannel.invokeMethod("onStrokeComplete", strokeData);
        }

        if (releaseAll) {
            requestCacheDraw();
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
        if (mPaint == null) {
            toCreatePaint();
        }
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
        // Also update accelerated drawing if available
        if (isAcceleratorInitialized && mStateHolder != null) {
            mStateHolder.setPenColor(color);
        }
    }

    public void setPenWidth(float width) {
        if (mPaint == null) {
            toCreatePaint();
        }
        if (mPaint != null) {
            float density = getResources().getDisplayMetrics().density;
            mPaint.setStrokeWidth(width * density);
        }
        // Also update accelerated drawing if available
        if (isAcceleratorInitialized && mStateHolder != null) {
            mStateHolder.setStrokeWidth(width);
        }
    }

    public void setDashed(boolean dashed) {
        isDashed = dashed;
        if (mPaint == null) {
            toCreatePaint();
        }
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
        
        // Also clear accelerated drawing if available
        if (isAcceleratorInitialized && mStateHolder != null) {
            mStateHolder.clearAllStroke();
        }
    }

    /**
     * Resume accelerated rendering
     */
    public void onResume() {
        if (isAcceleratorInitialized) {
            RenderAcceleratorManager.setRenderable(true);
            if (mStateHolder != null) {
                mStateHolder.onResume();
            }
        }
    }

    /**
     * Pause accelerated rendering
     */
    public void onPause() {
        if (isAcceleratorInitialized) {
            if (mStateHolder != null) {
                mStateHolder.onPause();
            }
            RenderAcceleratorManager.setRenderable(false);
        }
    }
}

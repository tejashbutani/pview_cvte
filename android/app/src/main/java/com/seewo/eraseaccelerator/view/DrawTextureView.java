package com.seewo.eraseaccelerator.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.MotionEvent;

import com.seewo.eraseaccelerator.IDrawView;
import com.seewo.eraseaccelerator.IFullScreenBitmapHolder;
import com.seewo.eraseaccelerator.shapes.Pen;
import com.seewo.eraseaccelerator.StateHolder;
import com.seewo.eraseaccelerator.RenderAcceleratorManager;
import com.seewo.eraseaccelerator.data.ContentCenter;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * TextureView implementation of IDrawView with state management
 * Suitable for Flutter PlatformView integration
 */
public class DrawTextureView extends TextureView implements IDrawView, TextureView.SurfaceTextureListener {
    private static final String TAG = "DrawTextureView";
    
    private IFullScreenBitmapHolder mFullScreenBitmapHolder;
    private boolean isDrawing = false;
    private Surface mSurface;
    private StateHolder mStateHolder;
    private Context mContext;
    
    // Stroke data for Flutter communication
    private List<StrokeData> mCompletedStrokes = new ArrayList<>();
    private StrokeListener mStrokeListener;
    
    public DrawTextureView(Context context) {
        super(context);
        mContext = context;
        initTexture();
    }

    public DrawTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initTexture();
    }

    private void initTexture() {
        setSurfaceTextureListener(this);
        setOpaque(false); // Allow transparency
    }
    
    /**
     * Initialize with StateHolder for complete drawing system
     */
    public void initializeWithState() {
        // Create dummy toolbar
        IToolbar dummyToolbar = new IToolbar() {
            @Override
            public boolean isEraserEnable() { return false; }
            @Override
            public void setStateHolder(StateHolder stateHolder) { /* no-op */ }
        };
        
        // Initialize state holder
        mStateHolder = new StateHolder(mContext, this, dummyToolbar);
        
        // Initialize acceleration with error handling
        try {
            RenderAcceleratorManager.init(mContext, false);
            Log.d(TAG, "RenderAcceleratorManager initialized successfully");
        } catch (Exception e) {
            Log.w(TAG, "RenderAcceleratorManager initialization failed, continuing without acceleration: " + e.getMessage());
        }
        
        // Setup canvas dimensions
        post(() -> {
            if (getWidth() > 0 && getHeight() > 0) {
                mStateHolder.onCreate(getWidth(), getHeight());
                setRenderBounds();
            }
        });
    }
    
    private void setRenderBounds() {
        try {
            int[] loc = new int[2];
            getLocationOnScreen(loc);
            Rect bounds = new Rect(loc[0], loc[1], loc[0] + getWidth(), loc[1] + getHeight());
            RenderAcceleratorManager.setRenderBounds(bounds);
            Log.d(TAG, "Render bounds set: " + bounds);
        } catch (Exception e) {
            Log.w(TAG, "Failed to set render bounds: " + e.getMessage());
        }
    }

    @Override
    public void setFullScreenBitmapHolder(IFullScreenBitmapHolder bitmapHolder) {
        mFullScreenBitmapHolder = bitmapHolder;
    }

    @Override
    public void setRoamState(boolean isRoamState) {
        // No-op for basic implementation
    }

    @Override
    public void setRoamScale(float scale) {
        // No-op for basic implementation
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
        
        // Extract completed strokes and notify Flutter
        if (mStateHolder != null) {
            extractAndNotifyStrokes();
        }
        
        refreshView();
    }

    @Override
    public void fullTransform(Matrix matrix) {
        // No-op for basic implementation
    }

    @Override
    public void finishRoam() {
        // No-op for basic implementation
    }

    @Override
    public void redrawAll(List<Pen> penList) {
        Log.d(TAG, "redrawAll on TextureView.");
        Canvas canvas = mSurface != null ? mSurface.lockCanvas(null) : null;
        if (canvas != null) {
            try {
                // Clear canvas
                canvas.drawColor(android.graphics.Color.TRANSPARENT, 
                    android.graphics.PorterDuff.Mode.CLEAR);
                
                // Draw background
                if (mFullScreenBitmapHolder != null) {
                    mFullScreenBitmapHolder.drawBackground(canvas);
                }
                
                // Draw all pens
                for (Pen pen : penList) {
                    pen.draw(canvas);
                }
            } finally {
                mSurface.unlockCanvasAndPost(canvas);
            }
        }
    }

    @Override
    public void refreshView() {
        if (mSurface != null && mFullScreenBitmapHolder != null) {
            Canvas canvas = mSurface.lockCanvas(null);
            if (canvas != null) {
                try {
                    Log.d(TAG, "refreshView: drawing full screen bitmap");
                    mFullScreenBitmapHolder.drawFullScreenBitmap(canvas);
                } finally {
                    mSurface.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    // TextureView.SurfaceTextureListener implementation
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureAvailable: " + width + "x" + height);
        mSurface = new Surface(surface);
        
        if (mStateHolder != null) {
            mStateHolder.onCreate(width, height);
            setRenderBounds();
        }
        
        // Draw a test pattern to verify TextureView is working
        drawTestPattern();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureSizeChanged: " + width + "x" + height);
        if (mStateHolder != null) {
            setRenderBounds();
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d(TAG, "onSurfaceTextureDestroyed");
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Called when texture is updated
    }
    
    // Flutter Integration Methods
    
    /**
     * Interface for notifying Flutter about completed strokes
     */
    public interface StrokeListener {
        void onStrokesCompleted(List<StrokeData> strokes);
    }
    
    /**
     * Stroke data structure for Flutter communication
     */
    public static class StrokeData {
        public List<PointF> points;
        public float strokeWidth;
        public int color;
        public long timestamp;
        
        public StrokeData(List<PointF> points, float strokeWidth, int color) {
            this.points = new ArrayList<>(points);
            this.strokeWidth = strokeWidth;
            this.color = color;
            this.timestamp = System.currentTimeMillis();
        }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            List<Map<String, Double>> pointsList = new ArrayList<>();
            for (PointF point : points) {
                Map<String, Double> pointMap = new HashMap<>();
                pointMap.put("x", (double) point.x);
                pointMap.put("y", (double) point.y);
                pointsList.add(pointMap);
            }
            map.put("points", pointsList);
            map.put("width", (double) strokeWidth);
            map.put("color", color);
            map.put("isDashed", false);
            return map;
        }
    }
    
    public void setStrokeListener(StrokeListener listener) {
        mStrokeListener = listener;
    }
    
    /**
     * Extract stroke data from ContentCenter and notify Flutter
     */
    private void extractAndNotifyStrokes() {
        if (mStateHolder == null || mStrokeListener == null) return;
        
        try {
            // Get all pens from ContentCenter
            ContentCenter contentCenter = getContentCenter();
            if (contentCenter == null) return;
            
            List<Pen> allPens = contentCenter.getAllPens();
            List<StrokeData> newStrokes = new ArrayList<>();
            
            // Convert only new strokes (strokes added since last notification)
            int currentStrokeCount = allPens.size();
            int lastNotifiedCount = mCompletedStrokes.size();
            
            for (int i = lastNotifiedCount; i < currentStrokeCount; i++) {
                Pen pen = allPens.get(i);
                List<PointF> points = new ArrayList<>();
                
                // Extract all points from pen
                for (int j = 0; j < pen.getPointCount(); j++) {
                    points.add(pen.get(j));
                }
                
                StrokeData strokeData = new StrokeData(
                    points,
                    pen.getStroke(),
                    pen.getPaint().getColor()
                );
                
                newStrokes.add(strokeData);
                mCompletedStrokes.add(strokeData);
            }
            
            // Notify Flutter about new strokes
            if (!newStrokes.isEmpty()) {
                mStrokeListener.onStrokesCompleted(newStrokes);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting stroke data: " + e.getMessage());
        }
    }
    
    // Helper method to access ContentCenter
    private ContentCenter getContentCenter() {
        if (mStateHolder == null) return null;
        return mStateHolder.getContentCenter();
    }
    
    /**
     * Public method to handle touch events from Flutter
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG, "Touch event received: " + event.getAction() + " at (" + event.getX() + ", " + event.getY() + ")");
        
        if (mStateHolder != null) {
            mStateHolder.onTouchEvent(event);
            return true;
        }
        return super.onTouchEvent(event);
    }
    
    /**
     * Clear all strokes
     */
    public void clearAllStrokes() {
        if (mStateHolder != null) {
            mStateHolder.clearAllStroke();
            mCompletedStrokes.clear();
        }
    }
    
    /**
     * Set stroke properties
     */
    public void setStrokeWidth(float width) {
        if (mStateHolder != null) {
            mStateHolder.setStrokeWidth(width);
        }
    }
    
    public void setPenColor(int color) {
        if (mStateHolder != null) {
            mStateHolder.setPenColor(color);
        }
    }
    
    /**
     * Lifecycle management
     */
    public void onResume() {
        if (mStateHolder != null) {
            mStateHolder.onResume();
        }
        try {
            RenderAcceleratorManager.setRenderable(true);
        } catch (Exception e) {
            Log.w(TAG, "Failed to set renderable true: " + e.getMessage());
        }
    }
    
    public void onPause() {
        if (mStateHolder != null) {
            mStateHolder.onPause();
        }
        try {
            RenderAcceleratorManager.setRenderable(false);
        } catch (Exception e) {
            Log.w(TAG, "Failed to set renderable false: " + e.getMessage());
        }
    }
    
    public void onDestroy() {
        if (mStateHolder != null) {
            mStateHolder.onDestroy();
        }
        try {
            RenderAcceleratorManager.destroy();
        } catch (Exception e) {
            Log.w(TAG, "Failed to destroy RenderAcceleratorManager: " + e.getMessage());
        }
    }
    
    /**
     * Get StateHolder for advanced operations
     */
    public StateHolder getStateHolder() {
        return mStateHolder;
    }
    
    /**
     * Draw a test pattern to verify TextureView is working
     */
    private void drawTestPattern() {
        if (mSurface == null) return;
        
        Canvas canvas = null;
        try {
            canvas = mSurface.lockCanvas(null);
            if (canvas != null) {
                // Clear canvas with semi-transparent background
                canvas.drawColor(0x80FFFFFF); // Semi-transparent white
                
                // Draw test pattern
                android.graphics.Paint paint = new android.graphics.Paint();
                paint.setAntiAlias(true);
                
                // Draw a red circle in top-left
                paint.setColor(android.graphics.Color.RED);
                canvas.drawCircle(100, 100, 50, paint);
                
                // Draw a green rectangle in center
                paint.setColor(android.graphics.Color.GREEN);
                canvas.drawRect(getWidth()/2 - 100, getHeight()/2 - 50, 
                               getWidth()/2 + 100, getHeight()/2 + 50, paint);
                
                // Draw text
                paint.setColor(android.graphics.Color.BLUE);
                paint.setTextSize(48);
                canvas.drawText("TextureView Active", 50, getHeight() - 100, paint);
                
                Log.d(TAG, "Test pattern drawn successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error drawing test pattern: " + e.getMessage());
        } finally {
            if (canvas != null) {
                mSurface.unlockCanvasAndPost(canvas);
            }
        }
    }
}

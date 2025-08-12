package com.seewo.eraseaccelerator;

import android.content.Context;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

import com.seewo.easinote.accelerator.base.util.SystemUnlockUtil;
import com.seewo.eraseaccelerator.view.DrawSurfaceView;
import com.seewo.eraseaccelerator.view.IToolbar;

import java.lang.ref.WeakReference;

/**
 * Standalone component that mimics exactly the same logic as MinimalAcceleratedActivity
 * but can be used in platform views without requiring an Activity context.
 * This ensures we get the same acceleration benefits.
 */
public class AcceleratedDrawingComponent {
    private static final String TAG = "AcceleratedDrawingComponent";
    
    private IDrawView mDrawView;
    private StateHolder mStateHolder;
    private Context mContext;
    private boolean isInitialized = false;
    private ViewTreeObserver.OnGlobalLayoutListener mLayoutListener;

    public AcceleratedDrawingComponent(Context context) {
        this.mContext = context;
    }

    /**
     * Initialize the component - exactly mimics MinimalAcceleratedActivity.onCreate()
     */
    public DrawSurfaceView initialize() {
        try {
            Log.d(TAG, "Initializing AcceleratedDrawingComponent");

            // Ensure container is registered for acceleration service
            // Exactly the same as MinimalAcceleratedActivity line 34
            SystemUnlockUtil.addWriteAcceleratorContainer(new WeakReference<>(mContext));

            // Create the DrawSurfaceView - exactly the same as MinimalAcceleratedActivity line 39
            mDrawView = new DrawSurfaceView(mContext);
            ((DrawSurfaceView) mDrawView).setVisibility(View.VISIBLE);

            // Init accelerator and state holder - exactly the same as MinimalAcceleratedActivity line 43
            RenderAcceleratorManager.init(mContext, false);

            // Create dummy toolbar - exactly the same as MinimalAcceleratedActivity line 45-50
            IToolbar dummyToolbar = new IToolbar() {
                @Override
                public boolean isEraserEnable() { return false; }
                @Override
                public void setStateHolder(StateHolder stateHolder) { /* no-op */ }
            };

            // Create StateHolder - exactly the same as MinimalAcceleratedActivity line 52
            mStateHolder = new StateHolder(mContext, mDrawView, dummyToolbar);

            // Initialize with screen dimensions - exactly the same as MinimalAcceleratedActivity line 54-56
            DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
            Log.d(TAG, "screen: " + metrics.widthPixels + "x" + metrics.heightPixels);
            mStateHolder.onCreate(metrics.widthPixels, metrics.heightPixels);

            // Set render bounds - exactly the same as MinimalAcceleratedActivity line 58-67
            mLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    int[] loc = new int[2];
                    mDrawView.getView().getLocationOnScreen(loc);
                    Rect bounds = new Rect(loc[0], loc[1], loc[0] + mDrawView.getView().getWidth(), loc[1] + mDrawView.getView().getHeight());
                    RenderAcceleratorManager.setRenderBounds(bounds);
                }
            };
            ((DrawSurfaceView) mDrawView).getViewTreeObserver().addOnGlobalLayoutListener(mLayoutListener);

            isInitialized = true;
            Log.d(TAG, "AcceleratedDrawingComponent initialized successfully");

            return (DrawSurfaceView) mDrawView;

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize AcceleratedDrawingComponent: " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Handle touch events - exactly the same as MinimalAcceleratedActivity.onTouchEvent()
     */
    public boolean onTouchEvent(MotionEvent event) {
        if (mStateHolder != null) {
            mStateHolder.onTouchEvent(event);
            return true;
        }
        return false;
    }

    /**
     * Resume the component - exactly the same as MinimalAcceleratedActivity.onResume()
     */
    public void onResume() {
        if (isInitialized && mStateHolder != null) {
            RenderAcceleratorManager.setRenderable(true);
            mStateHolder.onResume();
        }
    }

    /**
     * Pause the component - exactly the same as MinimalAcceleratedActivity.onPause()
     */
    public void onPause() {
        if (isInitialized && mStateHolder != null) {
            mStateHolder.onPause();
            RenderAcceleratorManager.setRenderable(false);
        }
    }

    /**
     * Destroy the component - exactly the same as MinimalAcceleratedActivity.onDestroy()
     */
    public void onDestroy() {
        if (isInitialized) {
            try {
                if (mStateHolder != null) {
                    mStateHolder.onDestroy();
                }
                RenderAcceleratorManager.destroy();
                
                // Clean up layout listener
                if (mLayoutListener != null && mDrawView != null) {
                    ((DrawSurfaceView) mDrawView).getViewTreeObserver().removeOnGlobalLayoutListener(mLayoutListener);
                }
                
                Log.d(TAG, "AcceleratedDrawingComponent destroyed");
            } catch (Exception e) {
                Log.e(TAG, "Error during destroy: " + e.getMessage(), e);
            }
        }
    }

    // Getters for accessing the components
    public StateHolder getStateHolder() {
        return mStateHolder;
    }

    public IDrawView getDrawView() {
        return mDrawView;
    }

    public boolean isInitialized() {
        return isInitialized;
    }
}

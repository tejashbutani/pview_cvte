package com.seewo.eraseaccelerator;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import com.seewo.easinote.accelerator.base.util.SystemUnlockUtil;
import com.seewo.eraseaccelerator.view.DrawSurfaceView;
import com.seewo.eraseaccelerator.view.IToolbar;
import com.seewo.eraseaccelerator.R;
import java.lang.ref.WeakReference;

/**
 * Manager class that handles the accelerated drawing logic extracted from MinimalAcceleratedActivity
 * This allows reusing the same acceleration pipeline in Platform Views without modifying core logic
 */
public class MinimalAcceleratedViewManager {

    private static final String TAG = "MinimalAccelViewMgr";
    private IDrawView mDrawView;
    private StateHolder mStateHolder;
    private Context mContext;
    private View mRootView;
    private boolean mIsInitialized = false;
    private boolean mIsRenderable = false;

    public MinimalAcceleratedViewManager(Context context) {
        mContext = context;
    }

    /**
     * Get Activity from Context (unwrap if needed)
     */
    private Activity getActivityFromContext(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    /**
     * Initialize the accelerated drawing system - extracted from MinimalAcceleratedActivity.onCreate()
     */
    public View initialize() {
        if (mIsInitialized) {
            Log.w(TAG, "Already initialized, returning existing view");
            return mRootView;
        }

        Log.d(TAG, "Starting initialization...");
        Log.d(TAG, "Context type: " + mContext.getClass().getSimpleName());
        
        // Check App singleton state before initialization
        App preInitApp = App.getInstance();
        Log.d(TAG, "Pre-init App instance: " + preInitApp + ", context: " + preInitApp.getContext());

        try {
            // Get the Activity reference
            Activity activity = getActivityFromContext(mContext);
            if (activity == null) {
                Log.e(TAG, "Could not get Activity from context");
                return null;
            }
            Log.d(TAG, "Found Activity: " + activity.getClass().getSimpleName());

            // CRITICAL: Initialize App singleton FIRST using the Activity context (like the original)
            Log.d(TAG, "Initializing App singleton with Activity context...");
            App.ensureInitialized(activity);
            
            // Verify initialization
            App app = App.getInstance();
            Log.d(TAG, "App singleton initialized - context: " + app.getContext());
            
            if (app.getContext() == null) {
                Log.e(TAG, "CRITICAL: App context is still null after initialization!");
                return null;
            }

            // Ensure container is registered for acceleration service (like the original)
            Log.d(TAG, "Registering with SystemUnlockUtil...");
            SystemUnlockUtil.addWriteAcceleratorContainer(new WeakReference<>(activity));
            Log.d(TAG, "SystemUnlockUtil registration successful");

            // Inflate the layout
            Log.d(TAG, "Inflating layout...");
            LayoutInflater inflater = LayoutInflater.from(mContext);
            mRootView = inflater.inflate(R.layout.activity_write_accelerator, null);
            Log.d(TAG, "Layout inflated successfully");

            // Use existing SurfaceView-based draw view
            Log.d(TAG, "Finding DrawSurfaceView...");
            mDrawView = mRootView.findViewById(R.id.draw_surface_view);
            if (mDrawView == null) {
                Log.e(TAG, "DrawSurfaceView not found in layout!");
                return null;
            }
            Log.d(TAG, "DrawSurfaceView found: " + mDrawView.getClass().getSimpleName());
            
            ((DrawSurfaceView) mDrawView).setVisibility(View.VISIBLE);
            Log.d(TAG, "DrawSurfaceView visibility set to VISIBLE");
            
            // Verify the view is properly set up
            Log.d(TAG, "DrawSurfaceView details - Width: " + mDrawView.getView().getWidth() + 
                      ", Height: " + mDrawView.getView().getHeight() + 
                      ", Visibility: " + mDrawView.getView().getVisibility());

            // Init accelerator and state holder (using Activity context like the original)
            Log.d(TAG, "About to initialize RenderAcceleratorManager with Activity context...");
            Log.d(TAG, "Activity context: " + activity);
            
            try {
                Log.d(TAG, "Calling RenderAcceleratorManager.init()...");
                RenderAcceleratorManager.init(activity, false);
                Log.d(TAG, "RenderAcceleratorManager.init() completed successfully");
                
                // Check if platform is supported
                Log.d(TAG, "Checking platform support...");
                boolean platformSupported = RenderAcceleratorManager.isPlatformSupport();
                Log.d(TAG, "Platform support for acceleration: " + platformSupported);
                
                if (!platformSupported) {
                    Log.w(TAG, "Platform does not support acceleration - will use fallback drawing");
                } else {
                    Log.d(TAG, "Platform supports acceleration - acceleration should be available");
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception during RenderAcceleratorManager initialization", e);
                // Continue anyway - basic drawing should still work
            }
            
            Log.d(TAG, "RenderAcceleratorManager initialization phase completed");

            IToolbar dummyToolbar = new IToolbar() {
                @Override
                public boolean isEraserEnable() { return false; }
                @Override
                public void setStateHolder(StateHolder stateHolder) { /* no-op */ }
            };

            Log.d(TAG, "About to create StateHolder with Activity context...");
            Log.d(TAG, "StateHolder parameters - Activity: " + activity + ", DrawView: " + mDrawView + ", Toolbar: " + dummyToolbar);
            
            try {
                mStateHolder = new StateHolder(activity, mDrawView, dummyToolbar);
                Log.d(TAG, "StateHolder created successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error creating StateHolder", e);
                return null;
            }

            // Get display metrics (using Activity like the original)
            DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
            
            Log.d(TAG, "Display metrics - Width: " + metrics.widthPixels + ", Height: " + metrics.heightPixels + 
                      ", Density: " + metrics.density + ", DensityDpi: " + metrics.densityDpi);
            
            Log.d(TAG, "About to call StateHolder.onCreate...");
            try {
                mStateHolder.onCreate(metrics.widthPixels, metrics.heightPixels);
                Log.d(TAG, "StateHolder.onCreate completed successfully");
                
                // Verify the canvas is created
                if (mStateHolder.getFullScreenCanvas() != null) {
                    Log.d(TAG, "FullScreen canvas created successfully - Canvas: " + mStateHolder.getFullScreenCanvas());
                } else {
                    Log.w(TAG, "FullScreen canvas is null after onCreate");
                }
                
                if (mStateHolder.getFullScreenBitmap() != null) {
                    Log.d(TAG, "FullScreen bitmap created successfully - Bitmap: " + mStateHolder.getFullScreenBitmap() + 
                              " (" + mStateHolder.getFullScreenBitmap().getWidth() + "x" + mStateHolder.getFullScreenBitmap().getHeight() + ")");
                } else {
                    Log.w(TAG, "FullScreen bitmap is null after onCreate");
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception in StateHolder.onCreate", e);
                // Continue anyway - basic functionality might still work
            }
            
            Log.d(TAG, "StateHolder initialization phase completed");

            // Set render bounds to view rect when laid out
            Log.d(TAG, "Setting up GlobalLayoutListener for render bounds...");
            ((DrawSurfaceView) mDrawView).getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    Log.d(TAG, "OnGlobalLayout called - view layout completed");
                    int[] loc = new int[2];
                    mDrawView.getView().getLocationOnScreen(loc);
                    Rect bounds = new Rect(loc[0], loc[1], loc[0] + mDrawView.getView().getWidth(), loc[1] + mDrawView.getView().getHeight());
                    Log.d(TAG, "Calculated render bounds: " + bounds);
                    
                    try {
                        RenderAcceleratorManager.setRenderBounds(bounds);
                        Log.d(TAG, "Render bounds set successfully");
                    } catch (Exception e) {
                        Log.e(TAG, "Error setting render bounds", e);
                    }
                }
            });
            Log.d(TAG, "GlobalLayoutListener set up successfully");

            mIsInitialized = true;
            Log.d(TAG, "=== MinimalAcceleratedViewManager initialization completed successfully ===");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing MinimalAcceleratedViewManager", e);
            return null;
        }

        return mRootView;
    }

    /**
     * Handle resume lifecycle - extracted from MinimalAcceleratedActivity.onResume()
     */
    public void onResume() {
        if (!mIsInitialized) {
            Log.w(TAG, "onResume called but not initialized");
            return;
        }
        
        Log.d(TAG, "onResume starting...");
        
        // Try to enable acceleration (like the original activity)
        try {
            RenderAcceleratorManager.setRenderable(true);
            Log.d(TAG, "RenderAcceleratorManager.setRenderable(true) called");
            
            // Verify if acceleration is actually working
            boolean platformSupported = RenderAcceleratorManager.isPlatformSupport();
            Log.d(TAG, "Platform support status during onResume: " + platformSupported);
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting renderable to true", e);
        }
        
        try {
            mStateHolder.onResume();
            Log.d(TAG, "StateHolder.onResume() completed");
        } catch (Exception e) {
            Log.e(TAG, "Error in StateHolder.onResume()", e);
        }
        
        mIsRenderable = true;
        Log.d(TAG, "onResume completed - acceleration status may vary");
    }

    /**
     * Handle pause lifecycle - extracted from MinimalAcceleratedActivity.onPause()
     */
    public void onPause() {
        if (!mIsInitialized) {
            Log.w(TAG, "onPause called but not initialized");
            return;
        }
        
        Log.d(TAG, "onPause starting...");
        mStateHolder.onPause();
        RenderAcceleratorManager.setRenderable(false);
        mIsRenderable = false;
        Log.d(TAG, "onPause completed - rendering disabled");
    }

    /**
     * Handle destroy lifecycle - extracted from MinimalAcceleratedActivity.onDestroy()
     */
    public void onDestroy() {
        if (!mIsInitialized) {
            Log.w(TAG, "onDestroy called but not initialized");
            return;
        }
        
        Log.d(TAG, "onDestroy starting...");
        try {
            mStateHolder.onDestroy();
            RenderAcceleratorManager.destroy();
            mIsInitialized = false;
            mIsRenderable = false;
            Log.d(TAG, "onDestroy completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error during onDestroy", e);
        }
    }

    /**
     * Handle touch events - extracted from MinimalAcceleratedActivity.onTouchEvent()
     */
    public boolean onTouchEvent(MotionEvent event) {
        if (!mIsInitialized || mStateHolder == null) {
            Log.w(TAG, "onTouchEvent called but not properly initialized (initialized=" + mIsInitialized + ", stateHolder=" + (mStateHolder != null) + ")");
            return false;
        }
        
        int action = event.getAction();
        String actionName = "";
        switch (action) {
            case MotionEvent.ACTION_DOWN: actionName = "DOWN"; break;
            case MotionEvent.ACTION_MOVE: actionName = "MOVE"; break;
            case MotionEvent.ACTION_UP: actionName = "UP"; break;
            default: actionName = "OTHER(" + action + ")"; break;
        }
        
        Log.d(TAG, "Touch event: " + actionName + " at (" + event.getX() + ", " + event.getY() + ")");
        
        try {
            mStateHolder.onTouchEvent(event);
            Log.d(TAG, "Touch event processed successfully");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error processing touch event", e);
            return false;
        }
    }

    /**
     * Get the root view containing the drawing surface
     */
    public View getRootView() {
        return mRootView;
    }

    /**
     * Check if the manager is initialized
     */
    public boolean isInitialized() {
        return mIsInitialized;
    }

    /**
     * Check if rendering is active
     */
    public boolean isRenderable() {
        return mIsRenderable;
    }
}

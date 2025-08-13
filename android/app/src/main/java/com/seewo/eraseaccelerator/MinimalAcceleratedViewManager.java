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
import android.graphics.Color;

import com.seewo.easinote.accelerator.base.util.SystemUnlockUtil;
import com.seewo.eraseaccelerator.view.DrawSurfaceView;
import com.seewo.eraseaccelerator.view.IToolbar;

import play.app.R;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

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
    private StrokeEventListener mStrokeEventListener;

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

            // Initialize App singleton using the new ensureInitialized method
            Log.d(TAG, "Ensuring App singleton is initialized...");
            App.ensureInitialized(mContext);
            App.ensureInitialized(activity); // Try with activity context too
            
            // Verify initialization
            App app = App.getInstance();
            Log.d(TAG, "After ensureInitialized - App context: " + app.getContext());
            
            if (app.getContext() != null) {
                Log.d(TAG, "App singleton verified as initialized");
                try {
                    boolean testBool = app.isUseAllMotionTouch();
                    Log.d(TAG, "Test call to isUseAllMotionTouch(): " + testBool);
                } catch (Exception e) {
                    Log.e(TAG, "Error testing App.isUseAllMotionTouch()", e);
                }
            } else {
                Log.e(TAG, "App context is STILL null after ensureInitialized!");
                // Force initialization one more time
                Log.d(TAG, "Force initializing App with activity context...");
                app.init(activity);
                Log.d(TAG, "Force init completed, context now: " + app.getContext());
            }

            // Ensure container is registered for acceleration service
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
            // Ensure transparent background on the host container and SurfaceView
            mRootView.setBackgroundColor(Color.TRANSPARENT);
            ((DrawSurfaceView) mDrawView).setBackgroundColor(Color.TRANSPARENT);
            ((DrawSurfaceView) mDrawView).setVisibility(View.VISIBLE);
            Log.d(TAG, "DrawSurfaceView found and set to visible");

            // Init accelerator and state holder
            Log.d(TAG, "Initializing RenderAcceleratorManager...");
            try {
                RenderAcceleratorManager.init(mContext, false);
                Log.d(TAG, "RenderAcceleratorManager initialized successfully");
                
                // Check if platform is supported
                boolean platformSupported = RenderAcceleratorManager.isPlatformSupport();
                Log.d(TAG, "Platform support for acceleration: " + platformSupported);
            } catch (Exception e) {
                Log.e(TAG, "Error initializing RenderAcceleratorManager", e);
                // Continue anyway - we'll handle the failures gracefully
            }

            IToolbar dummyToolbar = new IToolbar() {
                @Override
                public boolean isEraserEnable() { return false; }
                @Override
                public void setStateHolder(StateHolder stateHolder) { /* no-op */ }
            };

            Log.d(TAG, "Creating StateHolder...");
            mStateHolder = new StateHolder(mContext, mDrawView, dummyToolbar);
            if (mStrokeEventListener != null) {
                mStateHolder.setStrokeEventListener(mStrokeEventListener);
            }
            Log.d(TAG, "StateHolder created");

            // Get display metrics
            WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics metrics = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(metrics);
            
            Log.d(TAG, "Screen dimensions: " + metrics.widthPixels + "x" + metrics.heightPixels);
            Log.d(TAG, "Calling StateHolder.onCreate...");
            try {
                mStateHolder.onCreate(metrics.widthPixels, metrics.heightPixels);
                Log.d(TAG, "StateHolder.onCreate completed successfully");
                
                // Verify the canvas is created
                if (mStateHolder.getFullScreenCanvas() != null) {
                    Log.d(TAG, "FullScreen canvas created successfully");
                } else {
                    Log.w(TAG, "FullScreen canvas is null after onCreate");
                }
                
                if (mStateHolder.getFullScreenBitmap() != null) {
                    Log.d(TAG, "FullScreen bitmap created successfully");
                } else {
                    Log.w(TAG, "FullScreen bitmap is null after onCreate");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in StateHolder.onCreate", e);
                // Continue anyway - basic functionality might still work
            }

            // Set render bounds to view rect when laid out
            ((DrawSurfaceView) mDrawView).getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    Log.d(TAG, "OnGlobalLayout called");
                    int[] loc = new int[2];
                    mDrawView.getView().getLocationOnScreen(loc);
                    Rect bounds = new Rect(loc[0], loc[1], loc[0] + mDrawView.getView().getWidth(), loc[1] + mDrawView.getView().getHeight());
                    Log.d(TAG, "Setting render bounds: " + bounds);
                    RenderAcceleratorManager.setRenderBounds(bounds);
                }
            });

            mIsInitialized = true;
            Log.d(TAG, "MinimalAcceleratedViewManager initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing MinimalAcceleratedViewManager", e);
            return null;
        }

        return mRootView;
    }

    public void setStrokeEventListener(StrokeEventListener listener) {
        this.mStrokeEventListener = listener;
        if (mStateHolder != null) {
            mStateHolder.setStrokeEventListener(listener);
        }
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
        try {
            RenderAcceleratorManager.setRenderable(true);
            Log.d(TAG, "RenderAcceleratorManager.setRenderable(true) called");
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
        Log.d(TAG, "onResume completed - rendering enabled (may have failures)");
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

    public void setStrokeEventListener(StrokeEventListener listener) {
        this.mStrokeEventListener = listener;
        if (mStateHolder != null) {
            mStateHolder.setStrokeEventListener(stroke -> {
                try {
                    int[] loc = new int[2];
                    mDrawView.getView().getLocationOnScreen(loc);
                    int width = mDrawView.getView().getWidth();
                    int height = mDrawView.getView().getHeight();

                    Map<String, Object> payload = new HashMap<>();
                    payload.put("stroke", stroke);
                    Map<String, Object> view = new HashMap<>();
                    view.put("x", loc[0]);
                    view.put("y", loc[1]);
                    view.put("width", width);
                    view.put("height", height);
                    payload.put("view", view);

                    if (mStrokeEventListener != null) {
                        mStrokeEventListener.onStrokeComplete(payload);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error enriching stroke payload", e);
                    if (mStrokeEventListener != null) {
                        mStrokeEventListener.onStrokeComplete(stroke);
                    }
                }
            });
        }
    }
}

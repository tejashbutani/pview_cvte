package com.seewo.eraseaccelerator;

import android.content.Context;
import android.content.ContextWrapper;
import android.app.Activity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.embedding.android.FlutterActivity;

/**
 * Platform View implementation that wraps the MinimalAcceleratedViewManager
 * This allows embedding the accelerated canvas in Flutter without modifying core logic
 */
public class NativeCanvasView implements PlatformView {

    private static final String TAG = "NativeCanvasView";
    private MinimalAcceleratedViewManager mViewManager;
    private View mView;
    private Context mContext;

    private MethodChannel methodChannel;

    public NativeCanvasView(Context context, int id, Object args, BinaryMessenger messenger) {
        mContext = context;
        Log.d(TAG, "Creating NativeCanvasView with id: " + id + ", args: " + args);
        Log.d(TAG, "Context type: " + context.getClass().getSimpleName());
        
        // Initialize the view manager
        Log.d(TAG, "Creating MinimalAcceleratedViewManager...");
        mViewManager = new MinimalAcceleratedViewManager(context);
        
        Log.d(TAG, "Initializing view manager...");
        mView = mViewManager.initialize();
        
        if (mView != null) {
            // Create a MethodChannel scoped to this view id using provided messenger
            if (messenger != null) {
                methodChannel = new MethodChannel(messenger, "accelerated_canvas_view_" + id);
            } else {
                Log.w(TAG, "BinaryMessenger is null; platform channel will be disabled for this view");
            }

            // Wire stroke events to Flutter
            if (methodChannel != null) {
                mViewManager.setStrokeEventListener(stroke -> {
                    try {
                        methodChannel.invokeMethod("onStrokeComplete", stroke);
                    } catch (Exception e) {
                        Log.e(TAG, "Error invoking onStrokeComplete", e);
                    }
                });
            }

            // Attach stroke event listener to forward to Flutter
            mViewManager.onResume();
            try {
                mViewManager.getClass();
            } catch (Exception ignore) {}
            Log.d(TAG, "View manager initialized successfully, setting up touch handling");
            Log.d(TAG, "Root view type: " + mView.getClass().getSimpleName());
            Log.d(TAG, "Root view size: " + mView.getWidth() + "x" + mView.getHeight());
            
            // Set up touch event handling
            mView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Log.d(TAG, "Touch event received in OnTouchListener");
                    boolean handled = mViewManager.onTouchEvent(event);
                    Log.d(TAG, "Touch event handled: " + handled);
                    return handled;
                }
            });
            
            // Start the rendering system
            Log.d(TAG, "Starting rendering system...");
            mViewManager.onResume();
            Log.d(TAG, "NativeCanvasView created and initialized successfully");
        } else {
            Log.e(TAG, "Failed to initialize view manager - mView is null");
        }
    }

    private Activity getActivityFromContext(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    @Override
    public View getView() {
        return mView;
    }

    @Override
    public void dispose() {
        Log.d(TAG, "Disposing NativeCanvasView");
        
        if (mViewManager != null) {
            // Properly clean up the view manager
            mViewManager.onPause();
            mViewManager.onDestroy();
            mViewManager = null;
        }
        
        if (mView != null) {
            mView.setOnTouchListener(null);
            mView = null;
        }
        
        Log.d(TAG, "NativeCanvasView disposed");
    }

    /**
     * Handle lifecycle events when the platform view becomes visible/invisible
     */
    public void onResume() {
        if (mViewManager != null) {
            mViewManager.onResume();
            Log.d(TAG, "NativeCanvasView resumed");
        }
    }

    public void onPause() {
        if (mViewManager != null) {
            mViewManager.onPause();
            Log.d(TAG, "NativeCanvasView paused");
        }
    }
}

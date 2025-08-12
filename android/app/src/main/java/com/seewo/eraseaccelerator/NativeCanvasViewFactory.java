package com.seewo.eraseaccelerator;

import android.content.Context;
import android.util.Log;

import io.flutter.plugin.common.StandardMessageCodec;
import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.platform.PlatformViewFactory;

/**
 * Factory class for creating NativeCanvasView instances
 * This is registered with Flutter's platform view registry
 */
public class NativeCanvasViewFactory extends PlatformViewFactory {

    private static final String TAG = "NativeCanvasViewFactory";

    public NativeCanvasViewFactory() {
        super(StandardMessageCodec.INSTANCE);
        Log.d(TAG, "NativeCanvasViewFactory created");
    }

    @Override
    public PlatformView create(Context context, int viewId, Object args) {
        Log.d(TAG, "Creating NativeCanvasView with viewId: " + viewId + ", args: " + args);
        
        try {
            return new NativeCanvasView(context, viewId, args);
        } catch (Exception e) {
            Log.e(TAG, "Error creating NativeCanvasView", e);
            throw e;
        }
    }
}

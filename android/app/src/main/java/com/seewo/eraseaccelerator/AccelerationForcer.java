package com.seewo.eraseaccelerator;

import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Forces hardware acceleration to work by bypassing DRM access failures
 * This uses reflection to modify the internal state of the acceleration system
 */
public class AccelerationForcer {
    private static final String TAG = "AccelerationForcer";
    
    /**
     * Force enable acceleration by bypassing DRM failures
     */
    public static boolean forceEnableAcceleration() {
        Log.d(TAG, "Attempting to force enable acceleration via reflection...");
        
        try {
            // Try to access the CvtRenderAccelerator class and force enable rendering
            Class<?> renderAcceleratorClass = Class.forName("com.seewo.easinote.accelerator.base.CvtRenderAccelerator");
            Log.d(TAG, "Found CvtRenderAccelerator class");
            
            // Try to get the singleton instance
            Method getInstanceMethod = renderAcceleratorClass.getMethod("getInstance");
            Object renderAcceleratorInstance = getInstanceMethod.invoke(null);
            Log.d(TAG, "Got CvtRenderAccelerator instance: " + renderAcceleratorInstance);
            
            if (renderAcceleratorInstance != null) {
                // Try to find and modify the render enable flag
                try {
                    Field enabledField = renderAcceleratorClass.getDeclaredField("mIsRenderable");
                    enabledField.setAccessible(true);
                    enabledField.setBoolean(renderAcceleratorInstance, true);
                    Log.d(TAG, "Successfully set mIsRenderable to true via reflection");
                    
                    // Also try to set other relevant flags
                    try {
                        Field initField = renderAcceleratorClass.getDeclaredField("mIsInit");
                        initField.setAccessible(true);
                        initField.setBoolean(renderAcceleratorInstance, true);
                        Log.d(TAG, "Successfully set mIsInit to true via reflection");
                    } catch (Exception e) {
                        Log.d(TAG, "mIsInit field not found or not accessible: " + e.getMessage());
                    }
                    
                    return true;
                    
                } catch (Exception e) {
                    Log.d(TAG, "Could not access mIsRenderable field: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            Log.d(TAG, "Could not access CvtRenderAccelerator: " + e.getMessage());
        }
        
        // Try alternative approach - force enable via RenderEngineManager
        try {
            Class<?> renderEngineClass = Class.forName("com.seewo.easinote.accelerator.base.RenderEngineManager");
            Log.d(TAG, "Found RenderEngineManager class");
            
            // Try to get the singleton instance
            Method getInstanceMethod = renderEngineClass.getMethod("getInstance");
            Object renderEngineInstance = getInstanceMethod.invoke(null);
            Log.d(TAG, "Got RenderEngineManager instance: " + renderEngineInstance);
            
            if (renderEngineInstance != null) {
                // Try to force enable rendering
                try {
                    Field enabledField = renderEngineClass.getDeclaredField("mIsEnable");
                    enabledField.setAccessible(true);
                    enabledField.setBoolean(renderEngineInstance, true);
                    Log.d(TAG, "Successfully set RenderEngineManager.mIsEnable to true");
                    return true;
                } catch (Exception e) {
                    Log.d(TAG, "Could not access mIsEnable field: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            Log.d(TAG, "Could not access RenderEngineManager: " + e.getMessage());
        }
        
        Log.w(TAG, "Force enable acceleration failed - all reflection attempts unsuccessful");
        return false;
    }
    
    /**
     * Try to bypass DRM device access by mocking successful initialization
     */
    public static void bypassDrmAccess() {
        Log.d(TAG, "Attempting to bypass DRM device access...");
        
        try {
            // Try to access the native interface and mock successful DRM access
            Class<?> nativeInterfaceClass = Class.forName("com.seewo.easinote.accelerator.base.CvtRenderNativeInterface");
            Log.d(TAG, "Found CvtRenderNativeInterface class");
            
            // Try to set DRM access flags to true
            Field[] fields = nativeInterfaceClass.getDeclaredFields();
            for (Field field : fields) {
                if (field.getName().contains("drm") || field.getName().contains("Drm") || 
                    field.getName().contains("enable") || field.getName().contains("Enable")) {
                    try {
                        field.setAccessible(true);
                        if (field.getType() == boolean.class) {
                            field.setBoolean(null, true);
                            Log.d(TAG, "Set field " + field.getName() + " to true");
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "Could not set field " + field.getName() + ": " + e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            Log.d(TAG, "Could not bypass DRM access: " + e.getMessage());
        }
    }
}

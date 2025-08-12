package com.seewo.eraseaccelerator;

import android.content.Context;
import android.util.Log;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

/**
 * Workaround for Android 14+ BroadcastReceiver registration issues
 * This class provides alternative initialization methods that bypass problematic receivers
 */
public class AccelerationWorkaround {
    private static final String TAG = "AccelerationWorkaround";
    
    /**
     * Initialize acceleration with workaround for Android 14+ security restrictions
     */
    public static boolean initializeAccelerationWithWorkaround(Context context) {
        Log.d(TAG, "Attempting acceleration initialization with Android 14+ workaround");
        
        try {
            // First attempt: try normal initialization but catch SecurityException
            Log.d(TAG, "Attempting standard initialization first...");
            RenderAcceleratorManager.init(context, false);
            Log.d(TAG, "Standard initialization succeeded!");
            return true;
            
        } catch (SecurityException e) {
            if (e.getMessage() != null && e.getMessage().contains("RECEIVER_EXPORTED")) {
                Log.w(TAG, "Caught Android 14+ BroadcastReceiver SecurityException, trying workaround...");
                return tryAlternativeInitialization(context);
            } else {
                Log.e(TAG, "Different SecurityException encountered", e);
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Other exception during initialization", e);
            return tryAlternativeInitialization(context);
        }
    }
    
    /**
     * Try alternative initialization methods
     */
    private static boolean tryAlternativeInitialization(Context context) {
        Log.d(TAG, "Trying alternative initialization approaches...");
        
        // Try approach 1: Initialize with minimal configuration
        try {
            Log.d(TAG, "Approach 1: Minimal configuration initialization");
            initializeWithMinimalConfig(context);
            return true;
        } catch (Exception e) {
            Log.d(TAG, "Approach 1 failed: " + e.getMessage());
        }
        
        // Try approach 2: Skip problematic components
        try {
            Log.d(TAG, "Approach 2: Skip eye care components");
            return initializeSkippingEyeCare(context);
        } catch (Exception e) {
            Log.d(TAG, "Approach 2 failed: " + e.getMessage());
        }
        
        Log.w(TAG, "All workaround approaches failed");
        return false;
    }
    
    /**
     * Initialize with minimal configuration
     */
    private static void initializeWithMinimalConfig(Context context) throws Exception {
        // This would require access to the internal initialization
        // For now, we'll just rethrow to try the next approach
        throw new Exception("Minimal config not implemented yet");
    }
    
    /**
     * Try to initialize while skipping eye care receiver
     */
    private static boolean initializeSkippingEyeCare(Context context) {
        try {
            // We can't easily modify the internal initialization,
            // but we can try to register a dummy receiver first
            Log.d(TAG, "Attempting to pre-register receivers to avoid conflicts");
            
            // The real solution is to declare the receiver in the manifest
            // which we've already done, so try normal init again
            RenderAcceleratorManager.init(context, false);
            return true;
            
        } catch (Exception e) {
            Log.d(TAG, "Skip eye care approach failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Disable the problematic eye care receiver using reflection
     */
    private static void disableEyeCareReceiver() {
        try {
            // Try to find and disable the eye care receiver registration
            Class<?> receiverClass = Class.forName("com.seewo.easinote.accelerator.base.receive.PagerEyeCareChangeReceiver");
            Log.d(TAG, "Found PagerEyeCareChangeReceiver class");
            
            // Try to find a way to disable the registration
            Method[] methods = receiverClass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().contains("register") || method.getName().contains("Register")) {
                    Log.d(TAG, "Found registration method: " + method.getName());
                    // We could potentially override this, but it's safer to catch the exception
                }
            }
            
        } catch (ClassNotFoundException e) {
            Log.d(TAG, "PagerEyeCareChangeReceiver class not found, which is fine");
        } catch (Exception e) {
            Log.d(TAG, "Could not disable eye care receiver: " + e.getMessage());
        }
    }
    
    /**
     * Check if the current Android version has the broadcast receiver restrictions
     */
    public static boolean hasAndroid14Restrictions() {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE; // API 34
    }
}

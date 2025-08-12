package com.seewo.eraseaccelerator;

import android.util.Log;

import java.lang.reflect.Method;

/**
 * 系统属性的反射读取
 *
 * @author chenyikai
 * @date 2020-07-30
 */
public class SystemPropertiesInvoke {
    private static final String TAG = "SystemPropertiesInvoke";
    private static Method setStringMethod = null;
    private static Method getStringMethod = null;

    public static void setString(final String key, final String value) {
        try {
            if (setStringMethod == null) {
                setStringMethod = Class.forName("android.os.SystemProperties")
                        .getMethod("set", String.class, String.class);
            }
            setStringMethod.invoke(null, key, value);
        } catch (Exception e) {
            Log.e(TAG, "Platform error: " + e.toString());
        }
    }

    public static String getString(String key, String def) {
        try {
            if (getStringMethod == null) {
                getStringMethod = Class.forName("android.os.SystemProperties")
                        .getMethod("get", String.class, String.class);
            }
            return String.valueOf(getStringMethod.invoke(null, key, def));
        } catch (Exception e) {
            Log.e(TAG, "Platform error: " + e.toString());
        }
        return "";
    }
}
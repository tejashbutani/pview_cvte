package com.x;

import android.content.res.Resources;
import android.util.DisplayMetrics;

/**
 * Utility class for screen dimensions and other common utilities
 */
public class Util {
    public static int SCREEN_WIDTH = 1080;
    public static int SCREEN_HEIGHT = 1920;
    public static int OVERRIDE_SCREEN_WIDTH = 0;
    public static int OVERRIDE_SCREEN_HEIGHT = 0;

    static {
        // Initialize with default screen dimensions
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        SCREEN_WIDTH = metrics.widthPixels;
        SCREEN_HEIGHT = metrics.heightPixels;
    }

    /**
     * Get effective screen width
     * @return screen width in pixels
     */
    public static int getScreenWidth() {
        return OVERRIDE_SCREEN_WIDTH > 0 ? OVERRIDE_SCREEN_WIDTH : SCREEN_WIDTH;
    }

    /**
     * Get effective screen height
     * @return screen height in pixels
     */
    public static int getScreenHeight() {
        return OVERRIDE_SCREEN_HEIGHT > 0 ? OVERRIDE_SCREEN_HEIGHT : SCREEN_HEIGHT;
    }

    /**
     * Update screen dimensions
     * @param width new width
     * @param height new height
     */
    public static void updateScreenDimensions(int width, int height) {
        SCREEN_WIDTH = width;
        SCREEN_HEIGHT = height;
    }
}

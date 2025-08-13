package com.seewo.eraseaccelerator;

import java.util.Map;

/**
 * Listener for stroke completion events to communicate with Flutter layer.
 */
public interface StrokeEventListener {
    void onStrokeComplete(Map<String, Object> stroke);
}



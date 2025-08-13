package com.seewo.eraseaccelerator

import android.content.Context
import android.view.View
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.BinaryMessenger
import com.seewo.eraseaccelerator.view.DrawTextureView
import android.util.Log

class CustomTexturePlatformView(
    context: Context, 
    id: Int, 
    creationParams: Map<String?, Any?>?,
    messenger: BinaryMessenger
) : PlatformView {

    private val textureView: DrawTextureView = DrawTextureView(context)
    private val methodChannel: MethodChannel
    
    companion object {
        private const val TAG = "CustomTexturePlatformView"
    }

    init {
        // Setup method channel for communication with Flutter
        methodChannel = MethodChannel(messenger, "custom_texture_view_$id")
        
        // Initialize the texture view with state management
        textureView.initializeWithState()
        
        // Set up stroke listener to send data back to Flutter
        textureView.setStrokeListener { strokes ->
            Log.d(TAG, "Received ${strokes.size} completed strokes")
            
            // Send each stroke to Flutter
            for (stroke in strokes) {
                try {
                    Log.d(TAG, "Sending stroke with ${stroke.points.size} points to Flutter")
                    methodChannel.invokeMethod("onStrokeComplete", stroke.toMap())
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending stroke to Flutter: ${e.message}")
                }
            }
        }
        
        // Handle method calls from Flutter
        methodChannel.setMethodCallHandler { call, result ->
            when (call.method) {
                "setStrokeWidth" -> {
                    val width = call.argument<Double>("width")?.toFloat() ?: 5.0f
                    textureView.setStrokeWidth(width)
                    result.success(null)
                }
                "setPenColor" -> {
                    val color = call.argument<Int>("color") ?: android.graphics.Color.BLACK
                    textureView.setPenColor(color)
                    result.success(null)
                }
                "clearStrokes" -> {
                    textureView.clearAllStrokes()
                    result.success(null)
                }
                "resumeDrawing" -> {
                    textureView.onResume()
                    result.success(null)
                }
                "pauseDrawing" -> {
                    textureView.onPause()
                    result.success(null)
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
        
        // Apply creation parameters if provided
        creationParams?.let { params ->
            params["color"]?.let { color ->
                if (color is Int) {
                    textureView.setPenColor(color)
                }
            }
            params["width"]?.let { width ->
                if (width is Number) {
                    textureView.setStrokeWidth(width.toFloat())
                }
            }
        }
        
        Log.d(TAG, "CustomTexturePlatformView initialized with ID: $id")
    }

    override fun getView(): View {
        return textureView
    }

    override fun dispose() {
        Log.d(TAG, "Disposing CustomTexturePlatformView")
        textureView.onDestroy()
        methodChannel.setMethodCallHandler(null)
    }
}

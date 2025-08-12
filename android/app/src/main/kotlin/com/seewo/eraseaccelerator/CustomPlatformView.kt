package com.seewo.eraseaccelerator

import android.content.Context
import android.graphics.Color
import android.view.View
import com.seewo.eraseaccelerator.view.DrawSurfaceView
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView

class CustomPlatformView(
    context: Context,
    private val methodChannel: MethodChannel,
    creationParams: Map<String, Any>?
) : PlatformView, MethodChannel.MethodCallHandler {
    
    private val drawView: DrawSurfaceView
    private val acceleratedComponent: AcceleratedDrawingComponent

    init {
        // Use the AcceleratedDrawingComponent that mimics MinimalAcceleratedActivity exactly
        acceleratedComponent = AcceleratedDrawingComponent(context)
        drawView = acceleratedComponent.initialize()
        
        // Set initial pen settings from creation params
        val color = (creationParams?.get("color") as? Number)?.toInt() ?: Color.BLACK
        val width = (creationParams?.get("width") as? Double)?.toFloat() ?: 5.0f
        
        acceleratedComponent.getStateHolder()?.setPenColor(color)
        acceleratedComponent.getStateHolder()?.setStrokeWidth(width)
        
        // Set up touch handling - exactly like MinimalAcceleratedActivity
        drawView.setOnTouchListener { _, event ->
            acceleratedComponent.onTouchEvent(event)
        }
        
        // Initialize method channel
        methodChannel.setMethodCallHandler(this)
    }

    override fun getView(): View {
        return drawView
    }

    override fun dispose() {
        methodChannel.setMethodCallHandler(null)
        // Use the AcceleratedDrawingComponent's destroy method - exactly like MinimalAcceleratedActivity
        acceleratedComponent.onDestroy()
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        val stateHolder = acceleratedComponent.getStateHolder()
        
        when (call.method) {
            "updatePenColor" -> {
                val color = (call.argument<Number>("color"))?.toInt()
                android.util.Log.d("PenSettings", "Received method call - Color: $color")
                if (color != null && stateHolder != null) {
                    stateHolder.setPenColor(color)
                    result.success(null)
                } else {
                    android.util.Log.e("PenSettings", "Invalid arguments - Color: $color")
                    result.error("INVALID_ARGUMENTS", "Color is null", null)
                }
            }
            "updatePenWidth" -> {
                val width = call.argument<Double>("width")
                android.util.Log.d("PenSettings", "Received method call Width: $width")
                if (width != null && stateHolder != null) {
                    stateHolder.setStrokeWidth(width.toFloat())
                    result.success(null)
                } else {
                    android.util.Log.e("PenSettings", "Invalid arguments - Width: $width")
                    result.error("INVALID_ARGUMENTS", "Width is null", null)
                }
            }
            "setDashed" -> {
                val isDashed = call.argument<Boolean>("dashed") ?: false
                // Note: Dashed functionality would need to be implemented in StateHolder/BrushState
                android.util.Log.d("PenSettings", "Dashed mode not yet supported in StateHolder")
                result.success(null)
            }
            "clear" -> {
                if (stateHolder != null) {
                    stateHolder.clearAllStroke()
                }
                result.success(null)
            }
            "resume" -> {
                // Use AcceleratedDrawingComponent's resume - exactly like MinimalAcceleratedActivity
                acceleratedComponent.onResume()
                result.success(null)
            }
            "pause" -> {
                // Use AcceleratedDrawingComponent's pause - exactly like MinimalAcceleratedActivity
                acceleratedComponent.onPause()
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }
}

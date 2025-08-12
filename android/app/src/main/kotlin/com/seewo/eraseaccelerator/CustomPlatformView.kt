package com.seewo.eraseaccelerator

import android.content.Context
import android.graphics.Color
import android.view.View
import com.seewo.eraseaccelerator.view.DrawSurfaceView
import com.seewo.eraseaccelerator.view.IToolbar
import com.seewo.easinote.accelerator.base.util.SystemUnlockUtil
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import java.lang.ref.WeakReference

class CustomPlatformView(
    context: Context,
    private val methodChannel: MethodChannel,
    creationParams: Map<String, Any>?
) : PlatformView, MethodChannel.MethodCallHandler {
    
    private val drawView: DrawSurfaceView
    private val stateHolder: StateHolder
    private val context: Context = context

    init {
        // Initialize the acceleration system properly
        try {
            // Register for acceleration service
            SystemUnlockUtil.addWriteAcceleratorContainer(WeakReference(context))
            
            // Initialize accelerator
            RenderAcceleratorManager.init(context, false)
            
            // Create the DrawSurfaceView
            drawView = DrawSurfaceView(context)
            
            // Create dummy toolbar (required by StateHolder)
            val dummyToolbar = object : IToolbar {
                override fun isEraserEnable(): Boolean = false
                override fun setStateHolder(stateHolder: StateHolder?) {}
            }
            
            // Create StateHolder to manage the drawing
            stateHolder = StateHolder(context, drawView, dummyToolbar)
            
            // Initialize with screen dimensions
            val metrics = context.resources.displayMetrics
            stateHolder.onCreate(metrics.widthPixels, metrics.heightPixels)
            
            // Set initial pen settings from creation params
            val color = (creationParams?.get("color") as? Number)?.toInt() ?: Color.BLACK
            val width = (creationParams?.get("width") as? Double)?.toFloat() ?: 5.0f
            
            stateHolder.setPenColor(color)
            stateHolder.setStrokeWidth(width)
            
            // Set up touch handling
            drawView.setOnTouchListener { _, event ->
                stateHolder.onTouchEvent(event)
                true
            }
            
        } catch (e: Exception) {
            android.util.Log.e("CustomPlatformView", "Failed to initialize: ${e.message}")
            throw e
        }
    }

    // Secondary init block for common initialization
    init {
        methodChannel.setMethodCallHandler(this)
    }

    override fun getView(): View {
        return drawView
    }

    override fun dispose() {
        methodChannel.setMethodCallHandler(null)
        try {
            stateHolder.onDestroy()
            RenderAcceleratorManager.destroy()
        } catch (e: Exception) {
            android.util.Log.e("CustomPlatformView", "Error during dispose: ${e.message}")
        }
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "updatePenColor" -> {
                val color = (call.argument<Number>("color"))?.toInt()
                android.util.Log.d("PenSettings", "Received method call - Color: $color")
                if (color != null) {
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
                if (width != null) {
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
                stateHolder.clearAllStroke()
                result.success(null)
            }
            "resume" -> {
                stateHolder.onResume()
                result.success(null)
            }
            "pause" -> {
                stateHolder.onPause()
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }
}

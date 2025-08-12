package play.app

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
    private val drawView: View

    init {
        drawView = DrawSurfaceView(context)
    }

    // Secondary init block for common initialization
    init {
        methodChannel.setMethodCallHandler(this)
//        drawView.setMethodChannel(methodChannel)
    }

    override fun getView(): View {
        return drawView
    }

    override fun dispose() {
        methodChannel.setMethodCallHandler(null)
        // Handle lifecycle for accelerated view
//        drawView.onPause()
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "updatePenColor" -> {
                val color = (call.argument<Number>("color"))?.toInt()
                android.util.Log.d("PenSettings", "Received method call - Color: $color")
                if (color != null) {
//                   drawView.setPenColor(color)
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
//                  drawView.setPenWidth(width.toFloat())
                    result.success(null)
                } else {
                    android.util.Log.e("PenSettings", "Invalid arguments - Width: $width")
                    result.error("INVALID_ARGUMENTS", "Width is null", null)
                }
            }
            "setDashed" -> {
                val isDashed = call.argument<Boolean>("dashed") ?: false
//                drawView.setDashed(isDashed)
                result.success(null)
            }
            "clear" -> {
//                drawView.clear()
                result.success(null)
            }
            "resume" -> {
//                drawView.onResume()
                result.success(null)
            }
            "pause" -> {
//                drawView.onPause()
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }
}

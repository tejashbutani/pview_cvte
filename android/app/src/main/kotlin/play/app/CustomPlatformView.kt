package play.app

import android.content.Context
import android.graphics.Color
import android.view.View
import com.xbh.whiteboard.DrawSurfaceView
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView

class CustomPlatformView(
    context: Context,
    private val methodChannel: MethodChannel,
    creationParams: Map<String, Any>?
) : PlatformView, MethodChannel.MethodCallHandler {
    // private val rendLibView: RendLibSurfaceView = RendLibSurfaceView(
    //     context,
    //     (creationParams?.get("color") as? Number)?.toInt() ?: Color.BLACK,
    //     (creationParams?.get("width") as? Double)?.toFloat() ?: 5.0f
    // )
    private val drawView: DrawSurfaceView = DrawSurfaceView(context).apply {
        setPenColor((creationParams?.get("color") as? Number)?.toInt() ?: Color.BLACK)
        setPenWidth((creationParams?.get("width") as? Double)?.toFloat() ?: 5.0f)
        setDashed(creationParams?.get("isDashed") as? Boolean ?: false)
    }

    init {
        methodChannel.setMethodCallHandler(this)
        // rendLibView.setMethodChannel(methodChannel)
        drawView.setMethodChannel(methodChannel)

        // // Handle initial pen settings
        // val isDashed = creationParams?.get("isDashed") as? Boolean ?: false
        // rendLibView.setDashed(isDashed)
        // rendLibView.updatePenColor(
        //     (creationParams?.get("color") as? Number)?.toInt() ?: Color.BLACK
        // )
        // rendLibView.updatePenWidth(
        //     (creationParams?.get("width") as? Double)?.toFloat() ?: 5.0f
        // )
   
    }

    override fun getView(): View {
        // return rendLibView
        return drawView
    }

    override fun dispose() {
        methodChannel.setMethodCallHandler(null)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "updatePenColor" -> {
                val color = (call.argument<Number>("color"))?.toInt()
                // android.util.Log.d("PenSettings", "Received method call - Color: $color")
                if (color != null) {
                    // rendLibView.updatePenColor(color)
                    drawView.setPenColor(color)
                    result.success(null)
                } else {
                    // android.util.Log.e("PenSettings", "Invalid arguments - Color: $color, Width: $width")
                    result.error("INVALID_ARGUMENTS", "Color is null", null)
                }
            }
            "updatePenWidth" -> {
                val width = call.argument<Double>("width")
                // android.util.Log.d("PenSettings", "Received method call - Color: $color, Width: $width")
                if (width != null) {
                    // rendLibView.updatePenWidth(width.toFloat())
                    drawView.setPenWidth(width.toFloat())
                    result.success(null)
                } else {
                    // android.util.Log.e("PenSettings", "Invalid arguments - Width: $width")
                    result.error("INVALID_ARGUMENTS", "Width is null", null)
                }
            }
            "setDashed" -> {
                val isDashed = call.argument<Boolean>("dashed") ?: false
                // rendLibView.setDashed(isDashed)
                drawView.setDashed(isDashed)
                result.success(null)
            }
            "clear" -> {
                // rendLibView.clearCanvas()
                drawView.clear()
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }
}

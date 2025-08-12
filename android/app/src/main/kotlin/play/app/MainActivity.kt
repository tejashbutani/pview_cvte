package play.app

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import android.os.Bundle
// import com.xbh.whiteboard.AccelerateDraw

class MainActivity: FlutterActivity() {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
    //    var x = AccelerateDraw.getInstance()
        flutterEngine
            .platformViewsController
            .registry
            .registerViewFactory("custom_canvas_view", CustomViewFactory(flutterEngine.dartExecutor.binaryMessenger))
    }
}

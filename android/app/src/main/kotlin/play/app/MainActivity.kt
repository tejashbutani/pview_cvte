package play.app

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import android.os.Bundle
import android.util.Log
import com.seewo.eraseaccelerator.NativeCanvasViewFactory
import com.seewo.eraseaccelerator.App
// import com.xbh.whiteboard.AccelerateDraw

class MainActivity: FlutterActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Early initialization of App singleton
        Log.d(TAG, "Early App initialization in MainActivity.onCreate()")
        App.ensureInitialized(this)
        Log.d(TAG, "Early App initialization completed")
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        // Ensure App is initialized again
        Log.d(TAG, "Ensuring App is initialized in configureFlutterEngine")
        App.ensureInitialized(this)
        
        Log.d(TAG, "Registering accelerated_canvas_view platform view factory")
        
        // Register the accelerated canvas platform view
        flutterEngine
            .platformViewsController
            .registry
            .registerViewFactory("accelerated_canvas_view", NativeCanvasViewFactory())
            
        Log.d(TAG, "Platform view factory registered successfully")
        
    }
}

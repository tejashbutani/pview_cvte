package com.seewo.eraseaccelerator

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import android.content.Intent
import android.os.Bundle
import com.seewo.eraseaccelerator.WriteAcceleratorActivity
// import com.xbh.whiteboard.AccelerateDraw

class MainActivity: FlutterActivity() {
    private val CHANNEL = "com.example.flutter_android_activity"
    
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "launchActivity" -> {
                    val intent = Intent(this, WriteAcceleratorActivity::class.java)
                    startActivity(intent)
                    result.success(null)
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
        
        // Register the TextureView factory for PlatformView integration
        flutterEngine
            .platformViewsController
            .registry
            .registerViewFactory(
                "custom_texture_view", 
                CustomTextureViewFactory(flutterEngine.dartExecutor.binaryMessenger)
            )
    }
}

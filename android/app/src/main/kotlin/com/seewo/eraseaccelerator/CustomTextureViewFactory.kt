package com.seewo.eraseaccelerator

import android.content.Context
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.common.BinaryMessenger
import android.util.Log

class CustomTextureViewFactory(private val messenger: BinaryMessenger) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {

    companion object {
        private const val TAG = "CustomTextureViewFactory"
    }

    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        Log.d(TAG, "Creating CustomTexturePlatformView with ID: $viewId")
        
        val creationParams = args as? Map<String?, Any?>
        Log.d(TAG, "Creation params: $creationParams")
        
        return CustomTexturePlatformView(context, viewId, creationParams, messenger)
    }
}

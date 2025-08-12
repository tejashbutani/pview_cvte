package com.seewo.eraseaccelerator.predict

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.seewo.eraseaccelerator.RenderAcceleratorManager
import com.seewo.predict.PredictConfig
import com.seewo.predict.PredictHelper
import com.seewo.predict.core.AbstractPredictor
import com.seewo.predict.core.PredictPerFinger

class PredictConnector(predictConfig: PredictConfig) : PredictHelper(predictConfig) {

    /**
     * optional：to solve the problem that predict handwriting appears over the toolbar or other surfaces
     */
    private val forbidRenderBounds: MutableList<Rect> = mutableListOf()

    /**
     * optional：to solve the problem that predict handwriting appears over the toolbar or other surfaces
     */
    private val realRenderBounds: Rect = Rect()


    fun onCreate(context: Context, screenWidth: Int, screenHeight: Int) {
        super.init(context, screenWidth, screenHeight)
        getPredictConfig().mPredictType = AbstractPredictor.PredictType.LINEAR
        updatePredictSolutionLimits(context)
    }

    companion object {
        const val TAG = "PredictConnector"
        private const val LAYER_KEY_COVER_PREDICT_SUFFIX: String = "layer_key_cover_PredictHelper_"

        /**
         * @param enablePredict true: open predict, false: close predict
         * @param openPenColor true: the predict handwriting will show in yellow color
         */
        fun getNoteConfig(
            enablePredict: Boolean, openPenColor: Boolean) = PredictConfig().apply {
            isEnablePredict = enablePredict
            isOpenPenColor = openPenColor
        }
    }

    override fun onRenderAcceleratorBlock(event: PredictPerFinger.RenderAcceleratorEvent, pointId: Int, dirtyRect: Rect, predictBitmap: Bitmap): Boolean {
        val layerKey = LAYER_KEY_COVER_PREDICT_SUFFIX + pointId
        when (event) {
            PredictPerFinger.RenderAcceleratorEvent.SET_COVER_AND_RENDER -> {
                if (preCheckNeedRender(dirtyRect, predictBitmap)) {
                    RenderAcceleratorManager.setCover(layerKey, dirtyRect.left, dirtyRect.top, predictBitmap)
                    RenderAcceleratorManager.render(dirtyRect)
                } else {
                    //需要将加速区域清除，避免造成脏区域划破 关联jira:https://jira.cvte.com/browse/H01231407-2415
                    RenderAcceleratorManager.removeCover(layerKey)
                }
            }
            PredictPerFinger.RenderAcceleratorEvent.REMOVE_RENDER -> {
                RenderAcceleratorManager.removeCover(layerKey)
            }
            PredictPerFinger.RenderAcceleratorEvent.RENDER -> {
                RenderAcceleratorManager.render(dirtyRect)
            }
        }
        return true
    }


    fun onDestroy() {
        updatePredictGlobalBitmap(null)
    }

    /**
     * optional：to solve the problem that predict handwriting appears over the toolbar or other surfaces
     */
    private fun preCheckNeedRender(dirtyRect: Rect, predictBitmap: Bitmap): Boolean {
        realRenderBounds.set(dirtyRect.left, dirtyRect.top, dirtyRect.left + predictBitmap.width, dirtyRect.top + predictBitmap.height)
        return forbidRenderBounds.none {
            it.contains(realRenderBounds) || Rect.intersects(it, realRenderBounds)
        }
    }
}
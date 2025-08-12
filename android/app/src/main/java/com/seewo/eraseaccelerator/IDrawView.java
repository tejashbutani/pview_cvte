package com.seewo.eraseaccelerator;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.view.View;

import com.seewo.eraseaccelerator.shapes.Pen;

import java.util.List;

/**
 * Created by user on 2016/8/27.
 */
public interface IDrawView {

	void refreshView();

	/**
	 * @param bitmapHolder holder full screen bitmap
	 */
	void setFullScreenBitmapHolder(IFullScreenBitmapHolder bitmapHolder);

    /**
     * 是否处于漫游状态
     * @param isRoamState
     */
	void setRoamState(boolean isRoamState);

    /**
     * 缩放比例
     * @param scale
     */
    void setRoamScale(float scale);

	View getView();

    void onTouchDown();

    void onTouchUp();

    /**
     * 全局转换
     * @param matrix
     */
    void fullTransform(Matrix matrix);

    void finishRoam();

    void redrawAll(List<Pen> penList);

}

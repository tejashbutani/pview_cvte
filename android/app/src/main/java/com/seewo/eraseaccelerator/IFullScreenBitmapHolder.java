package com.seewo.eraseaccelerator;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;

/**
 * Created by user on 2016/8/27.
 */
public interface IFullScreenBitmapHolder {
	/**
	 * Get the canvas witch contains full screen bitmap
	 */
	Canvas getFullScreenCanvas();

    /**
     *
     * @return
     */
	Bitmap getFullScreenBitmap();

	/**
	 * @param canvas Draw pen on canvas
	 */
	void drawFullScreenBitmap(Canvas canvas);

	void drawBackground();

	void drawBackground(Canvas canvas);

	int getBackgroundColor();

	void drawBackgroundOnVisibleRect(Rect rect);

	void endTransform(Matrix finalMatrix);

	void addDirtyRect(Rect rect);

	void refreshDrawView();
}

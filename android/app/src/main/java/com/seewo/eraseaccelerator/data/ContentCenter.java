package com.seewo.eraseaccelerator.data;

import android.graphics.Canvas;
import android.util.Log;

import com.seewo.eraseaccelerator.App;
import com.seewo.eraseaccelerator.IDrawView;
import com.seewo.eraseaccelerator.shapes.Pen;
// Removed eraser/roam utilities

import java.util.LinkedList;
import java.util.List;

/**
 * holder all pens
 */
public class ContentCenter {
	private static final String TAG = "ContentCenter";
	private final List<Pen> mPenList;
	private IDrawView mDrawView;

	public ContentCenter(IDrawView drawView) {
	    mDrawView = drawView;
		mPenList = new LinkedList<Pen>();
	}

	public void add(Pen pen) {
		mPenList.add(pen);
	}

    public void redrawAll(Canvas canvas) {
        redrawAll(canvas, false);
    }

    /**
     * 重绘
     * @param canvas
     * @param lockCanvas 是否锁住canvas
     */
	public void redrawAll(Canvas canvas, boolean lockCanvas) {
		Log.d(TAG, "redrawAll: ");
		if (lockCanvas) {
            Log.d(TAG, "redraw on SurfaceView.");
            if (mDrawView != null) {
                mDrawView.redrawAll(mPenList);
            }
        } else {
            for (Pen pen : mPenList) {
                pen.draw(canvas);
            }
        }
	}

	public void clear() {
        mPenList.clear();
    }

    // No-op: eraser support removed
    public void splitPens(Object dirtyRects, int eraserWidth) { }

    /**
     * 获取Pen的列表
     * @return
     */
	public List<Pen> getAllPens() {
	    return mPenList;
    }
}

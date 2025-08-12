package com.x;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import java.util.ArrayList;
import java.util.List;

/**
 * Pencil drawing tool implementation
 */
public class Pencil implements IDrawer {
    private Paint mPaint;
    private Path mPath;
    private List<PointF> mPoints;
    private float mLastX, mLastY;
    private static final float TOUCH_TOLERANCE = 4;

    public Pencil(Paint paint) {
        mPaint = new Paint(paint);
        mPath = new Path();
        mPoints = new ArrayList<>();
    }

    @Override
    public void touchDown(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        mLastX = x;
        mLastY = y;
        mPoints.clear();
        mPoints.add(new PointF(x, y));
    }

    @Override
    public boolean touchMove(float x, float y) {
        float dx = Math.abs(x - mLastX);
        float dy = Math.abs(y - mLastY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mLastX, mLastY, (x + mLastX) / 2, (y + mLastY) / 2);
            mLastX = x;
            mLastY = y;
            mPoints.add(new PointF(x, y));
            return true;
        }
        return false;
    }

    @Override
    public void touchUp(float x, float y, DrawSurfaceView drawView) {
        mPath.lineTo(mLastX, mLastY);
        mPoints.add(new PointF(x, y));
        
        // Draw to cache canvas
        Canvas cacheCanvas = drawView.getDrawCanvas();
        if (cacheCanvas != null) {
            cacheCanvas.drawPath(mPath, mPaint);
        }
    }

    @Override
    public void draw(DrawSurfaceView drawView) {
        Canvas drawCanvas = drawView.getDrawCanvas();
        if (drawCanvas != null) {
            drawCanvas.drawPath(mPath, mPaint);
        }
    }

    @Override
    public List<PointF> getPoints() {
        return new ArrayList<>(mPoints);
    }
}

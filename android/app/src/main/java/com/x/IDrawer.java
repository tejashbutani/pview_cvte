package com.x;

import android.graphics.PointF;
import java.util.List;

/**
 * Interface for drawing tools (pen, pencil, etc.)
 */
public interface IDrawer {
    /**
     * Called when touch down event occurs
     * @param x X coordinate
     * @param y Y coordinate
     */
    void touchDown(float x, float y);

    /**
     * Called when touch move event occurs
     * @param x X coordinate
     * @param y Y coordinate
     * @return true if drawing should be updated
     */
    boolean touchMove(float x, float y);

    /**
     * Called when touch up event occurs
     * @param x X coordinate
     * @param y Y coordinate
     * @param drawView The drawing surface view
     */
    void touchUp(float x, float y, DrawSurfaceView drawView);

    /**
     * Draw the current stroke
     * @param drawView The drawing surface view
     */
    void draw(DrawSurfaceView drawView);

    /**
     * Get all points in the current stroke
     * @return List of points
     */
    List<PointF> getPoints();
}

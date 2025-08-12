package com.xbh.whiteboard;

import android.graphics.Rect;
import android.graphics.PointF;
import java.util.List;
import java.util.ArrayList;

/**
 * Author: Wen Luo
 * Date: 2020/2/1211:04
 * Email: Wen.Luo@lango-tech.cn
 */
public interface IDrawer {
    Rect getCurrRect();
    void touchDown(float x, float y);
    boolean touchMove(float x, float y);
    boolean touchUp(float x, float y,DrawSurfaceView drawSurfaceView);
    void draw(DrawSurfaceView drawSurfaceView);
    List<PointF> getPoints();
}

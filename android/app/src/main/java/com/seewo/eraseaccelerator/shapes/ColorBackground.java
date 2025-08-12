package com.seewo.eraseaccelerator.shapes;

import android.graphics.Canvas;
import android.graphics.Color;

import com.seewo.eraseaccelerator.IBackground;

public class ColorBackground implements IBackground {
    private int mColor = Color.WHITE;

    public void setBgColor(int color) {
        mColor = color;
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawColor(mColor);
    }

    public int getColor() {
        return mColor;
    }
}
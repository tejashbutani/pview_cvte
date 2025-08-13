package com.seewo.eraseaccelerator.shapes;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;

import com.seewo.eraseaccelerator.IBackground;

public class ColorBackground implements IBackground {
    private int mColor = Color.TRANSPARENT;

    public void setBgColor(int color) {
        mColor = color;
    }

    @Override
    public void draw(Canvas canvas) {
        if (mColor == Color.TRANSPARENT) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        } else {
            canvas.drawColor(mColor);
        }
    }

    public int getColor() {
        return mColor;
    }
}
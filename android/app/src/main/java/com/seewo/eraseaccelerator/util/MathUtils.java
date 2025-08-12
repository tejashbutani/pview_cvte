package com.seewo.eraseaccelerator.util;

public class MathUtils {
    private MathUtils() {
    }

    //检查1234这样的顺序是否为顺时针
    public static boolean checkCCW(float p1x, float p1y, float p2x, float p2y,
                                   float p3x, float p3y, float p4x, float p4y) {
        return minus(p1x, p1y, p2x, p2y) +
                minus(p2x, p2y, p3x, p3y) +
                minus(p3x, p3y, p4x, p4y) +
                minus(p4x, p4y, p1x, p1y) > 0;
    }

    private static float minus(float p1x, float p1y,
                               float p2x, float p2y) {
        return (p2x - p1x) * (p2y + p1y);
    }

}

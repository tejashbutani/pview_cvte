package com.seewo.eraseaccelerator.util;

import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.RegionIterator;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 2016/8/27.
 */
public class RectUtil {
	/**
	 * input three points and then output the max rect which contains these points
	 * @param dirtyRect
	 * @param x1 X coordinate of point 1
	 * @param y1 Y coordinate of point 1
	 * @param x2 X coordinate of point 2
	 * @param y2 Y coordinate of point 2
	 * @param x3 X coordinate of point 3
	 * @param y3 Y coordinate of point 3
	 */
	public static void pointsToRect(Rect dirtyRect, int x1, int y1, int x2, int y2, int x3, int y3) {
		if (null == dirtyRect) {
			return;
		}

		dirtyRect.setEmpty();
		if (x1 <= x2) {
			dirtyRect.left = x1;
			dirtyRect.right = x2;
		} else {
			dirtyRect.left = x2;
			dirtyRect.right = x1;
		}

		if (x3 <= dirtyRect.left) {
			dirtyRect.left = x3;
		}
		if (x3 >= dirtyRect.right) {
			dirtyRect.right = x3;
		}

		if (y1 <= y2) {
			dirtyRect.top = y1;
			dirtyRect.bottom = y2;
		} else {
			dirtyRect.top = y2;
			dirtyRect.bottom = y1;
		}

		if (y3 <= dirtyRect.top) {
			dirtyRect.top = y3;
		}
		if (y3 >= dirtyRect.bottom) {
			dirtyRect.bottom = y3;
		}
	}

	public static void expandBound(float expand, Rect rect) {
		if(null != rect) {
			rect.left -= expand;
			rect.top -= expand;
			rect.right += expand;
			rect.bottom += expand;
		}
	}

	public static List<Rect> simplifyRects(List<Rect> srcRects) {
		List<Rect> tmpList = new ArrayList<>(srcRects);

		Region region = new Region();

		for (Rect rect : tmpList) {
			region.op(rect, Region.Op.UNION);
		}

		Rect rect = new Rect();
		RegionIterator iterator = new RegionIterator(region);
		while (iterator.next(rect)) {
			if(Math.abs(rect.right - rect.left) < 2 && Math.abs(rect.bottom - rect.top) < 2) {
				continue;
			}
			tmpList.add(rect);
		}

		return tmpList;
	}
}

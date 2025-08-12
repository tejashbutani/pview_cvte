package com.seewo.eraseaccelerator.state;

import android.util.Log;
import android.view.MotionEvent;

import com.seewo.eraseaccelerator.IFullScreenBitmapHolder;
import com.seewo.eraseaccelerator.data.ContentCenter;

public abstract class AbstractState {
	private static final int MAX_POINTER_COUNT = 10;

	final ContentCenter mContentCenter;
	final IFullScreenBitmapHolder mFullScreenBitmapHolder;

	AbstractState(IFullScreenBitmapHolder fullScreenBitmapHolder, ContentCenter contentCenter) {
		mFullScreenBitmapHolder = fullScreenBitmapHolder;
		mContentCenter = contentCenter;
	}

	public void onTouchEvent(MotionEvent event) {
		if (event.getPointerCount() > MAX_POINTER_COUNT) {
			return;
		}
		int action = event.getActionMasked();
		switch (action) {
			case MotionEvent.ACTION_DOWN:
				touchDown(event);
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
				touchPointerDown(event);
				break;
			case MotionEvent.ACTION_MOVE:
				touchMove(event);
				break;
			case MotionEvent.ACTION_POINTER_UP:
				touchPointerUp(event);
				break;
			case MotionEvent.ACTION_UP:
				touchUp(event);
				break;
			default:
				Log.d("AbstractState", "ignore action : " + action);
		}
	}

	void touchPointerDown(MotionEvent event){}

	void touchPointerUp(MotionEvent event) {}

	protected abstract void touchDown(MotionEvent event);

	protected abstract void touchMove(MotionEvent event);

	protected abstract void touchUp(MotionEvent event);

	public void init() {

	}

	public void destroy() {

	}
}

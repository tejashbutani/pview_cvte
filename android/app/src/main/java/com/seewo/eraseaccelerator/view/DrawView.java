package com.seewo.eraseaccelerator.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.seewo.eraseaccelerator.IDrawView;
import com.seewo.eraseaccelerator.IFullScreenBitmapHolder;
import com.seewo.eraseaccelerator.shapes.Pen;
import com.seewo.eraseaccelerator.util.ClearUtil;

import java.util.List;

public class DrawView extends View implements IDrawView {
	private static final String TAG = "DrawView";

	private boolean mIsRoamState = false;
    private IFullScreenBitmapHolder mFullScreenBitmapHolder;
    private float mScale = 1f;
    private Paint mPaint;
    private Canvas mFullScreenCanvas;
    private Bitmap mFullScreenBitmap;

	private static final int MSG_REFRESH = 100;
	private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REFRESH:{
                    ClearUtil.clearDirty(DrawView.this);
                    break;
                }
                default:
                    break;
            }
        }
    };

	public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (Build.VERSION.SDK_INT >= 28) {
            mPaint = new Paint();
            mPaint.setFilterBitmap(true);
        }
	}

	@Override
	protected void onDraw(Canvas canvas) {
		Log.d(TAG, "onDraw: ");
		if (mFullScreenBitmapHolder != null) {
            Log.d(TAG, "drawFullScreenBitmap");
			mFullScreenBitmapHolder.drawFullScreenBitmap(canvas);
		}
	}

	@Override
	public void setFullScreenBitmapHolder(IFullScreenBitmapHolder fullScreenBitmapHolder) {
		mFullScreenBitmapHolder = fullScreenBitmapHolder;
	}

    @Override
    public void setRoamState(boolean isRoamState) {
        mIsRoamState = isRoamState;
    }

    @Override
    public void setRoamScale(float scale) {
        mScale = scale;
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public void onTouchDown() {

    }

    @Override
    public void onTouchUp() {

    }

    /**
     * 整张画布移动
     */
    @Override
    public void fullTransform(Matrix matrix) {
        Log.d(TAG, "fullTransform: ");
        if (mFullScreenCanvas == null) {
            mFullScreenCanvas = mFullScreenBitmapHolder.getFullScreenCanvas();
        }
        if (mFullScreenBitmap == null) {
            mFullScreenBitmap = mFullScreenBitmapHolder.getFullScreenBitmap();
        }
        mFullScreenCanvas.drawBitmap(mFullScreenBitmap, matrix, mPaint);
    }

    @Override
    public void finishRoam() {
        refreshView();
        mFullScreenCanvas = null;
        mFullScreenBitmap = null;
    }

    @Override
    public void redrawAll(List<Pen> penList) {

    }

    @Override
    public void refreshView() {
        Log.d(TAG, "refreshView: ");
        super.invalidate();
        mHandler.sendEmptyMessageDelayed(MSG_REFRESH, 200L);
    }
}
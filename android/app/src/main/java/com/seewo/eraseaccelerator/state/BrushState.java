package com.seewo.eraseaccelerator.state;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;

import com.seewo.eraseaccelerator.App;
import com.seewo.eraseaccelerator.IFullScreenBitmapHolder;
import com.seewo.eraseaccelerator.SystemPropertiesInvoke;
import com.seewo.eraseaccelerator.RenderAcceleratorManager;
import com.seewo.eraseaccelerator.data.ContentCenter;
import com.seewo.eraseaccelerator.predict.PredictConnector;
import com.seewo.eraseaccelerator.shapes.Pen;
import com.seewo.eraseaccelerator.util.BoardTypeUtil;
import com.seewo.predict.PredictConfig;
import com.seewo.predict.PredictHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * do write
 */
public class BrushState extends AbstractState {

    private static final String TAG = "BrushState";

    public static final float DEFAULT_STROKE_WIDTH = 5;
    public static final float OPTIMIZE_MAX_STROKE_WIDTH = 50;
    private static final int MAX_POINTER_COUNT = 10;

    private final Rect mDirtyRect = new Rect();
    private final Pen[] mPens = new Pen[MAX_POINTER_COUNT];
    private final Point[] mLastPoints = new Point[MAX_POINTER_COUNT];
    /**
     * 用来临时存储计算ClipRects时的rect
     */
    private final Map<Integer, List<Rect>> mPathClipRectsTmp = new HashMap<>();
    private Path[] mPathTmps = new Path[MAX_POINTER_COUNT];
    private Region[] mFillPathRegionTmps = new Region[MAX_POINTER_COUNT];
    private Region[] mResultRegionTmps = new Region[MAX_POINTER_COUNT];
    private Rect[] mRectTmps = new Rect[MAX_POINTER_COUNT];
    private float mStrokeWidth;
    private int mPenColor = Color.parseColor("#000000");
    private Rect mWriteRect = new Rect();// 书写区域
    private PredictConnector mPredictConnector;

    public BrushState(IFullScreenBitmapHolder fullScreenBitmapHolder, ContentCenter contentCenter) {
        super(fullScreenBitmapHolder, contentCenter);
        mStrokeWidth = DEFAULT_STROKE_WIDTH;
        initPredict();
    }

    private void initPredict() {
        if (!App.getInstance().isEnablePredict()) {
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            //only support predict on Android 13 or later
            return;
        }
        App app = App.getInstance();
        // set predict config，
        // the first parameter: true means you want to use predict handwriting
        // the second parameter: if you want to show predict handwriting with different color to check predict function work or not,
        // you can set the second parameter to true
        PredictConfig predictConfig = PredictConnector.Companion.getNoteConfig(true, false);
        mPredictConnector = new PredictConnector(predictConfig);
        mPredictConnector.onCreate(app.getContext(), app.getScreenWidth(), app.getScreenHeight());
    }

    @Override
    protected void touchDown(MotionEvent event) {
        int actionIndex = event.getActionIndex();
        int pointerId = event.getPointerId(actionIndex);
        if (pointerId >= MAX_POINTER_COUNT || pointerId < 0) {
            return;
        }
        float x = (event.getX(actionIndex));
        float y = (event.getY(actionIndex));
        float pressure = event.getPressure(actionIndex);
        RenderAcceleratorManager.prepareToRender();
        allMotionTouchEnable(true);
        onNewPen(event);
        mWriteRect.set((int) event.getX(), (int) event.getY(), (int) event.getX(), (int) event.getY());
        for (int i = 0; i < event.getPointerCount(); ++i) {
            float xTemp = event.getX(i);
            float yTemp = event.getY(i);
            int pointerIdTemp = event.getPointerId(i);
            if (pointerIdTemp >= MAX_POINTER_COUNT || mPens[pointerIdTemp] == null) {
                continue;
            }
            mLastPoints[pointerIdTemp].x = (int) xTemp;
            mLastPoints[pointerIdTemp].y = (int) yTemp;
        }

        if (mPredictConnector != null) {
            mPredictConnector.onHandleWrite(PredictHelper.WriteEvent.START_WRITE, pointerId, x, y, pressure, event);
        }

    }

    @Override
    protected void touchPointerDown(MotionEvent event) {
        if (event.getPointerCount() > 2
                && mStrokeWidth > OPTIMIZE_MAX_STROKE_WIDTH
                && isLowPerformancePlatform()) {
            allMotionTouchEnable(false);
        }
        onNewPen(event);
        extendRect(event);
        int actionIndex = event.getActionIndex();
        float x = event.getX(actionIndex);
        float y = event.getY(actionIndex);
        int pointerId = event.getPointerId(actionIndex);
        float pressure = event.getPressure(actionIndex);
        if (mPredictConnector != null) {
            mPredictConnector.onHandleWrite(PredictHelper.WriteEvent.START_WRITE, pointerId, x, y, pressure, event);
        }
    }

    private void onNewPen(MotionEvent event) {
        Log.d(TAG, "onNewPen: ");
        int actionIndex = event.getActionIndex();
        float x = (event.getX(actionIndex));
        float y = (event.getY(actionIndex));
        int pointerId = event.getPointerId(actionIndex);

        mPens[pointerId] = new Pen(mStrokeWidth, mPenColor, App.getInstance().getContext());
        mLastPoints[pointerId] = new Point();

        mPens[pointerId].startStroke(x, y);

        drawPathAndAddDirtyRectForAccelerator(pointerId);

        mContentCenter.add(mPens[pointerId]);
    }

    @Override
    protected void touchMove(MotionEvent event) {
        int pointerCount = event.getPointerCount();
        for (int i = 0; i < pointerCount; ++i) {
            float x = event.getX(i);
            float y = event.getY(i);
            float pressure = event.getPressure(i);
            int pointerId = event.getPointerId(i);
            if (pointerId >= MAX_POINTER_COUNT || mPens[pointerId] == null) {
                continue;
            }
            mPens[pointerId].continueStroke(x, y, mDirtyRect);
            drawPathAndAddDirtyRectForAccelerator(pointerId);

            mLastPoints[pointerId].x = (int) x;
            mLastPoints[pointerId].y = (int) y;

            if (mPredictConnector != null) {
                mPredictConnector.onHandleWrite(PredictHelper.WriteEvent.CONTINUE_WRITE, pointerId, x, y, pressure, event, (dirtyRect, predictPen, assistPoint) -> {
                    predictPen.getMPaint().set(mPens[pointerId].getPaint());
                    return mPens[pointerId].updateSegmentPath(dirtyRect, predictPen.getMPath(), assistPoint.getX(), assistPoint.getY());
                });
            }
        }
        extendRect(event);
    }

    private void drawPathAndAddDirtyRectForAccelerator(int pointerId) {
        Pen pen = mPens[pointerId];
        if (pen == null) {
            return;
        }
        pen.drawCreatingPath(mFullScreenBitmapHolder.getFullScreenCanvas());
        List<Rect> clipRects = getClipRectsFromPath(pointerId);
        for (Rect rect : clipRects) {
            mFullScreenBitmapHolder.addDirtyRect(rect);
        }
    }

    private List<Rect> getClipRectsFromPath(int pointerId) {

        Pen pen = mPens[pointerId];

        List<Rect> clipRects = mPathClipRectsTmp.get(pointerId);
        if (clipRects == null) {
            clipRects = new ArrayList<>();
        } else {
            clipRects.clear();
        }
        if (pen == null) {
            return clipRects;
        }
        clipRects.add(pen.getClipRectFromCreatingPath());
        return clipRects;
    }

    @Override
    protected void touchPointerUp(MotionEvent event) {
        int actionIndex = event.getActionIndex();
        float x = event.getX(actionIndex);
        float y = event.getY(actionIndex);
        int pointerId = event.getPointerId(actionIndex);
        if (pointerId >= MAX_POINTER_COUNT || mPens[pointerId] == null) {
            return;
        }
        if (event.getPointerCount() <= 3
                && mStrokeWidth <= OPTIMIZE_MAX_STROKE_WIDTH
                && isLowPerformancePlatform()) {
            allMotionTouchEnable(true);
        }

        float pressure = event.getPressure(actionIndex);

        if (mPredictConnector == null) {
            mPens[pointerId].endStroke(x, y, mDirtyRect);
        } else {
            mPredictConnector.onHandleWrite(PredictHelper.WriteEvent.END_WRITE, pointerId, x, y, pressure, null, null, (pointerId1, pXFinal, pYFinal, pressure1) -> {
                mPens[pointerId1].endStroke(pXFinal, pYFinal, mDirtyRect);
                return null;
            });
        }

        drawPathAndAddDirtyRectForAccelerator(pointerId);

        extendRect(event);
    }

    @Override
    protected void touchUp(MotionEvent event) {
        extendRect(event);
        int actionIndex = event.getActionIndex();
        float x = event.getX(actionIndex);
        float y = event.getY(actionIndex);
        float pressure = event.getPressure(actionIndex);
        int pointerId = event.getPointerId(actionIndex);
        if (pointerId >= MAX_POINTER_COUNT || mPens[pointerId] == null) {
            return;
        }

        if (mPredictConnector == null) {
            mPens[pointerId].endStroke(x, y, mDirtyRect);
        } else {
            mPredictConnector.onHandleWrite(PredictHelper.WriteEvent.END_WRITE, pointerId, x, y, pressure, null, null, (pointerId1, pXFinal, pYFinal, pressure1) -> {
                mPens[pointerId1].endStroke(pXFinal, pYFinal, mDirtyRect);
                return null;
            });
        }

        drawPathAndAddDirtyRectForAccelerator(pointerId);
        RenderAcceleratorManager.finishRender();
        allMotionTouchEnable(false);
        mFullScreenBitmapHolder.refreshDrawView();
    }

    public void setStrokeWidth(float strokeWidth) {
        mStrokeWidth = strokeWidth;
    }

    public void setPenColor(int penColor) {
        mPenColor = penColor;
    }

    public Pen[] getPens() {
        return mPens;
    }

    /**
     * 扩展Rect
     *
     * @param event
     */
    private void extendRect(MotionEvent event) {
        if (mWriteRect.left > event.getX()) {
            mWriteRect.left = (int) event.getX();
        }
        if (mWriteRect.right < event.getX()) {
            mWriteRect.right = (int) event.getX();
        }
        if (mWriteRect.top > event.getY()) {
            mWriteRect.top = (int) event.getY();
        }
        if (mWriteRect.bottom < event.getY()) {
            mWriteRect.bottom = (int) event.getY();
        }
    }

    /**
     * 触摸进度设置
     *
     * @param forbid
     */
    private void allMotionTouchEnable(boolean forbid) {
        if (!App.getInstance().isUseAllMotionTouch()) {
            return;
        }
        if (!App.getInstance().getAllMotionTouch()) {
            // 工具栏上的开关
            return;
        }
        int sysValue = forbid ? 1 : 0;
        SystemPropertiesInvoke.setString("sys.motiontouch.all", Integer.toString(sysValue));
    }

    // 551与811、9950 CPU性能相对较好，可以全开历史点，还能解决笔迹被拉直问题
    private boolean isLowPerformancePlatform() {
        return !BoardTypeUtil.mIs811 && !BoardTypeUtil.mIs551 && !BoardTypeUtil.mIs9950;
    }

    @Override
    public void destroy() {
        if (mPredictConnector != null) {
            mPredictConnector.onDestroy();
        }
    }
}

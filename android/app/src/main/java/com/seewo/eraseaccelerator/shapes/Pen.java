package com.seewo.eraseaccelerator.shapes;

import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import com.seewo.eraseaccelerator.util.RectUtil;
import java.util.HashMap;
import java.util.Map;

import java.util.ArrayList;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class Pen {

    /**
     * 用于计算DIRTY_RECT时，额外添加的宽度，保证DIRTY_RECT足够大
     */
    public static final int DIRTY_RECT_EXTRA_ACCURACY = 3;

    static final int BOUNDS_WEIGHT = 1;
    private static final String TAG = "Pen";
    private float mPreviousX;
    private float mPreviousY;
    private float mLastMidX;
    private float mLastMidY;

    private final Path mPath;
    // designed for MotionEvent.ACTION_MOVE
    private final Path mCreatingPath;
    private Paint mPaint;
    protected float mStroke;
    private final ArrayList<PointF> mPoints;
    private static float sScaleFactor = 1.0f;
    private static boolean sScaleFactorInitialized = false;

    public Pen(float strokeWidth) {
        this(strokeWidth, Color.WHITE);
    }

    public Pen(float strokeWidth, int color) {
        Log.d(TAG, "Pen: " + strokeWidth);
        mPath = new Path();
        mCreatingPath = new Path();
        mPoints = new ArrayList<PointF>();

        initPaint(strokeWidth, color);
    }

    /**
     * Initialize the scaling factor based on display metrics
     * This should be called once when the application starts
     */
    public static void initializeScaleFactor(Context context) {
        if (!sScaleFactorInitialized) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics metrics = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(metrics);
            
            Log.d(TAG, "=== COORDINATE SCALING DEBUG ===");
            Log.d(TAG, "Display metrics: " + metrics.widthPixels + "x" + metrics.heightPixels);
            Log.d(TAG, "Display density: " + metrics.density);
            Log.d(TAG, "Display densityDpi: " + metrics.densityDpi);
            
            // Since the display is already 3840x2160, the issue might be coordinate system differences
            // between the Java native view and Flutter's coordinate system.
            // Let's try a density-based scaling approach instead
            
            // Standard density is 160 dpi (mdpi)
            float standardDensity = 160f;
            float densityScale = metrics.densityDpi / standardDensity;
            
            // For now, let's use density scaling to see if it helps with alignment
            sScaleFactor = 1.0f / densityScale; // Inverse scaling to normalize coordinates
            
            Log.d(TAG, "Standard density: " + standardDensity);
            Log.d(TAG, "Device density scale: " + densityScale);
            Log.d(TAG, "Applied scale factor: " + sScaleFactor);
            Log.d(TAG, "=================================");
            
            sScaleFactorInitialized = true;
        }
    }
    
    /**
     * Get current scale factor for debugging
     */
    public static float getScaleFactor() {
        return sScaleFactor;
    }
    
    /**
     * Get display metrics information
     */
    public static Map<String, Object> getDisplayInfo(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        
        Map<String, Object> displayInfo = new HashMap<>();
        displayInfo.put("widthPixels", metrics.widthPixels);
        displayInfo.put("heightPixels", metrics.heightPixels);
        displayInfo.put("density", (double) metrics.density);
        displayInfo.put("densityDpi", metrics.densityDpi);
        displayInfo.put("scaleFactor", (double) sScaleFactor);
        
        return displayInfo;
    }

    /**
     * Apply scaling to coordinates to match Flutter coordinate system
     */
    private float scaleCoordinate(float coordinate) {
        // Apply the calculated scale factor to transform coordinates
        return coordinate * sScaleFactor;
    }

    public Pen(Pen src) {
        this(src.mPaint.getStrokeWidth(), src.mPaint.getColor());
    }

    private void initPaint(float strokeWidth, int color) {
        mStroke = strokeWidth;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);

        mPaint.setColor(color);
        mPaint.setStrokeWidth(strokeWidth);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setAntiAlias(true);
        mPaint.setMaskFilter(new BlurMaskFilter(0.1f, BlurMaskFilter.Blur.SOLID));
    }

    public float getStroke() {
        return mStroke;
    }

    public void startStroke(float x, float y) {
        mPath.reset();
        mCreatingPath.reset();

        Log.d(TAG, "startStroke - Original coordinates: (" + x + ", " + y + ")");
        Log.d(TAG, "startStroke - Scale factor: " + sScaleFactor);

        // Use original coordinates for Java drawing (Canvas operations)
        mPreviousX = x;
        mPreviousY = y;
        mLastMidX = x;
        mLastMidY = y;
        mPath.moveTo(round(x), round(y));

        // Save original coordinates (scaling will be applied only when sending to Flutter)
        savePoint(x, y);
    }

    private void savePoint(float x, float y) {
        mPoints.add(new PointF(x, y));
    }

    public void continueStroke(float x, float y, Rect dirtyRect) {
        Log.d(TAG, "continueStroke - Original coordinates: (" + x + ", " + y + ")");
        
        // Use original coordinates for Java drawing (Canvas operations)
        boolean isPathChanged = updateSegmentPath(dirtyRect, mCreatingPath, x, y);
        if (isPathChanged) {
            mPath.addPath(mCreatingPath);
        }

        mLastMidX = (x + mPreviousX) / 2.0F;
        mLastMidY = (y + mPreviousY) / 2.0F;

        mPreviousX = x;
        mPreviousY = y;

        // Save original coordinates (scaling will be applied only when sending to Flutter)
        savePoint(x, y);
    }

    // avoid burr
    private float round(float f) {
        int a = (int) f;
        float precision = f - a;

        if (precision <= 0.3) {
            precision = 0;
        } else if (precision < 0.7) {
            precision = 0.5f;
        } else {
            precision = 1;
        }

        return a + precision;
    }

    public void endStroke(float x, float y, Rect dirtyRect) {
        // Use original coordinates for Java drawing (Canvas operations)
        float f1 = (x + mPreviousX) / 2.0F;
        float f2 = (y + mPreviousY) / 2.0F;

        mCreatingPath.reset();
        mCreatingPath.moveTo(round(mLastMidX), round(mLastMidY));
        mCreatingPath.quadTo(round(mPreviousX), round(mPreviousY), round(f1), round(f2));

        mPath.quadTo(round(mPreviousX), round(mPreviousY), round(f1), round(f2));

        RectUtil.pointsToRect(dirtyRect, (int) mLastMidX, (int) mLastMidY, (int) mPreviousX, (int) mPreviousY, (int) f1, (int) f2);
        RectUtil.expandBound(mPaint.getStrokeWidth(), dirtyRect);

        mPreviousX = x;
        mPreviousY = y;

        // Save original coordinates (scaling will be applied only when sending to Flutter)
        savePoint(x, y);
    }

    public boolean updateSegmentPath(Rect dirtyRect, Path segmentPath, float x, float y) {
        float f1 = (x + mPreviousX) / 2.0F;
        float f2 = (y + mPreviousY) / 2.0F;
        segmentPath.reset();
        segmentPath.moveTo(round(mLastMidX), round(mLastMidY));
        segmentPath.quadTo(round(mPreviousX), round(mPreviousY), round(f1), round(f2));
        RectUtil.pointsToRect(dirtyRect, (int) mLastMidX, (int) mLastMidY, (int) mPreviousX, (int) mPreviousY, (int) f1, (int) f2);
        RectUtil.expandBound(mPaint.getStrokeWidth() + 1, dirtyRect);
        return true;
    }

    public void draw(Canvas canvas) {
        canvas.drawPath(mPath, mPaint);
    }

    public void drawCreatingPath(Canvas canvas) {
        canvas.drawPath(mCreatingPath, mPaint);
    }

    /**
     * test method
     * @param canvas
     */
    public void drawWithTranslate(Canvas canvas) {
        Matrix matrix = new Matrix();
        matrix.setTranslate(10, 10);
        canvas.setMatrix(matrix);
        canvas.drawPath(mPath, mPaint);
    }

    public Path getCreatingPath() {
        return mCreatingPath;
    }

    public void computeBounds(RectF pathRectF) {
        mPath.computeBounds(pathRectF, false);
    }

    public Rect getBounds() {
        RectF bounds = new RectF();
        computeBounds(bounds);

        return new Rect((int)(bounds.left), (int)(bounds.top), (int)(bounds.right), (int)(bounds.bottom));
    }

    public Rect getClipRectFromCreatingPath() {

        Rect pathOutBound = new Rect();

        Path path = getCreatingPath();
        Paint paint = getPaint();
        if (path == null || paint == null) {
            return pathOutBound;
        }

        Rect creatingPathOutBoundF = getCreatingPathBounds();

        pathOutBound.left = creatingPathOutBoundF.left - DIRTY_RECT_EXTRA_ACCURACY;
        pathOutBound.top = creatingPathOutBoundF.top - DIRTY_RECT_EXTRA_ACCURACY;
        pathOutBound.right = creatingPathOutBoundF.right + DIRTY_RECT_EXTRA_ACCURACY;
        pathOutBound.bottom = creatingPathOutBoundF.bottom + DIRTY_RECT_EXTRA_ACCURACY;
        return pathOutBound;

    }

    public Rect getCreatingPathBounds() {
        RectF bounds = new RectF();
        if (mCreatingPath == null) {
            return new Rect();
        }
        mCreatingPath.computeBounds(bounds, false);
        int temp = (int) (BOUNDS_WEIGHT + getStroke()/2);

        return new Rect((int) (bounds.left - temp), (int) (bounds.top - temp),
                (int) (bounds.right + temp), (int) (bounds.bottom + temp));
    }

    // isInPolygon was only used by eraser splitting; removed with eraser

    public int getPointCount() {
        return mPoints.size();
    }

    public PointF get(int index) {
        return mPoints.get(index);
    }

    public Map<String, Object> toMap() {
        ArrayList<Map<String, Object>> pts = new ArrayList<>();
        for (PointF p : mPoints) {
            Map<String, Object> point = new HashMap<>();
            // Apply scaling only when sending coordinates to Flutter
            float scaledX = scaleCoordinate(p.x);
            float scaledY = scaleCoordinate(p.y);
            point.put("x", (double) scaledX);
            point.put("y", (double) scaledY);
            pts.add(point);
        }
        Map<String, Object> out = new HashMap<>();
        out.put("points", pts);
        out.put("color", getPaint().getColor());
        out.put("width", (double) getPaint().getStrokeWidth());
        out.put("isDashed", false);
        out.put("scaleFactor", (double) sScaleFactor);
        out.put("scaleFactorInitialized", sScaleFactorInitialized);
        
        Log.d(TAG, "=== STROKE DATA DEBUG ===");
        Log.d(TAG, "toMap - Stroke with " + mPoints.size() + " points");
        Log.d(TAG, "toMap - Scale factor: " + sScaleFactor + " (initialized: " + sScaleFactorInitialized + ")");
        if (!mPoints.isEmpty()) {
            PointF first = mPoints.get(0);
            PointF last = mPoints.get(mPoints.size() - 1);
            float scaledFirstX = scaleCoordinate(first.x);
            float scaledFirstY = scaleCoordinate(first.y);
            float scaledLastX = scaleCoordinate(last.x);
            float scaledLastY = scaleCoordinate(last.y);
            Log.d(TAG, "toMap - First point (original): (" + first.x + ", " + first.y + ")");
            Log.d(TAG, "toMap - First point (scaled): (" + scaledFirstX + ", " + scaledFirstY + ")");
            Log.d(TAG, "toMap - Last point (original): (" + last.x + ", " + last.y + ")");
            Log.d(TAG, "toMap - Last point (scaled): (" + scaledLastX + ", " + scaledLastY + ")");
            Log.d(TAG, "toMap - Java stroke bounds: " + getBounds());
        }
        Log.d(TAG, "========================");
        
        return out;
    }

    public Paint getPaint() {
        return mPaint;
    }

    public Path getPath() {
        return mPath;
    }

    /**
     * path的矩阵变换
     * @param matrix
     */
    public void transform(Matrix matrix) {
        mPath.transform(matrix);
        mapTrace(matrix);
    }

    /**
     * point的矩阵变换，避免橡皮擦擦除的异常
     * @param pMatrix
     */
    private void mapTrace(Matrix pMatrix) {
        float[] localFloatArray = new float[2];
        for (int localIndex = 0; localIndex < mPoints.size(); ++localIndex) {
            PointF point = mPoints.get(localIndex);
            if (null == point) {
                break;
            }
            localFloatArray[0] = point.x;
            localFloatArray[1] = point.y;

            pMatrix.mapPoints(localFloatArray);
            point.x = localFloatArray[0];
            point.y = localFloatArray[1];
        }
    }
}

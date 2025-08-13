package com.seewo.eraseaccelerator;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.seewo.easinote.accelerator.base.util.SystemUnlockUtil;
import com.seewo.eraseaccelerator.view.DrawSurfaceView;
import com.seewo.eraseaccelerator.view.IToolbar;
import com.seewo.eraseaccelerator.view.Toolbar;

import java.lang.ref.WeakReference;

public class WriteAcceleratorActivity extends Activity {

    private static final String TAG = "WriteAcceleratorActivity";
    private IDrawView mIDrawView;
    private StateHolder mStateHolder;
    private IToolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, " onCreate");
        if (App.getInstance().isUseBaseWriteAccelerator()) {
            SystemUnlockUtil.addWriteAcceleratorContainer(new WeakReference<>(this));
        }

        setContentView(R.layout.activity_write_accelerator);

        if (getResources().getBoolean(R.bool.is_use_surface_view)) {
            mIDrawView = findViewById(R.id.draw_surface_view);
            ((DrawSurfaceView) mIDrawView).setVisibility(View.VISIBLE);
        }

        mToolbar = new Toolbar(this);

        RenderAcceleratorManager.init(this, false);

        mStateHolder = new StateHolder(this, mIDrawView, mToolbar);

        int width = App.getInstance().getScreenWidth();
        int height = App.getInstance().getScreenHeight();
        if (width <= 0 || height <= 0) {
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            width = metrics.widthPixels;
            height = metrics.heightPixels;
        }
        Log.d(TAG, "onCreate: width:" + width + ",height:" + height);
        mStateHolder.onCreate(width, height);

        mToolbar.setStateHolder(mStateHolder);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, " onResume");
        mStateHolder.onResume();
    }

    @Override
    protected void onPause() {
        Log.i(TAG, " onPause");
        super.onPause();
        mStateHolder.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, " onDestroy");
        mStateHolder.onDestroy();
        RenderAcceleratorManager.destroy();
        super.onDestroy();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mStateHolder.onTouchEvent(event);
        return true;
    }
}

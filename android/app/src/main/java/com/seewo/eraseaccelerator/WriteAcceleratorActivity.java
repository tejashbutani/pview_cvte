package com.seewo.eraseaccelerator;

import android.app.Activity;
import android.os.Bundle;
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

        Log.d(TAG, "onCreate: width:" + App.getInstance().getScreenWidth() + ",height:" + App.getInstance().getScreenHeight());
        mStateHolder.onCreate(App.getInstance().getScreenWidth(), App.getInstance().getScreenHeight());

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

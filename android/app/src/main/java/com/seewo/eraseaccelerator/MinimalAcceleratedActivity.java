package com.seewo.eraseaccelerator;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

import com.seewo.easinote.accelerator.base.util.SystemUnlockUtil;
import com.seewo.eraseaccelerator.view.DrawSurfaceView;
import com.seewo.eraseaccelerator.view.IToolbar;

import play.app.R;

import java.lang.ref.WeakReference;

/**
 * Minimal activity using the existing accelerated drawing pipeline
 * (DrawSurfaceView + StateHolder + RenderAcceleratorManager).
 */
public class MinimalAcceleratedActivity extends Activity {

    private static final String TAG = "MinimalAccelAct";
    private IDrawView mDrawView;
    private StateHolder mStateHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ensure container is registered for acceleration service
        SystemUnlockUtil.addWriteAcceleratorContainer(new WeakReference<>(this));

        setContentView(R.layout.activity_write_accelerator);

        // Use existing SurfaceView-based draw view
        mDrawView = findViewById(R.id.draw_surface_view);
        ((DrawSurfaceView) mDrawView).setVisibility(View.VISIBLE);

        // Init accelerator and state holder
        RenderAcceleratorManager.init(this, false);

        IToolbar dummyToolbar = new IToolbar() {
            @Override
            public boolean isEraserEnable() { return false; }
            @Override
            public void setStateHolder(StateHolder stateHolder) { /* no-op */ }
        };

        mStateHolder = new StateHolder(this, mDrawView, dummyToolbar);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        Log.d(TAG, "screen: " + metrics.widthPixels + "x" + metrics.heightPixels);
        mStateHolder.onCreate(metrics.widthPixels, metrics.heightPixels);

        // Set render bounds to view rect when laid out
        ((DrawSurfaceView) mDrawView).getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int[] loc = new int[2];
                mDrawView.getView().getLocationOnScreen(loc);
                Rect bounds = new Rect(loc[0], loc[1], loc[0] + mDrawView.getView().getWidth(), loc[1] + mDrawView.getView().getHeight());
                RenderAcceleratorManager.setRenderBounds(bounds);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        RenderAcceleratorManager.setRenderable(true);
        mStateHolder.onResume();
    }

    @Override
    protected void onPause() {
        mStateHolder.onPause();
        RenderAcceleratorManager.setRenderable(false);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mStateHolder.onDestroy();
        RenderAcceleratorManager.destroy();
        super.onDestroy();
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        mStateHolder.onTouchEvent(event);
        return true;
    }
}


package com.seewo.eraseaccelerator.view;


import android.app.Activity;
import android.view.View;
import android.widget.Button;

import com.seewo.eraseaccelerator.StateHolder;

import play.app.R;

public class Toolbar implements IToolbar {

    private StateHolder mStateHolder;
    private ToolbarView mToolbarView;

    public Toolbar(final Activity activity) {
        mToolbarView = activity.findViewById(R.id.toolbar);
        initClearBtn(activity);
    }

    private void initClearBtn(Activity activity) {
        Button clearBtn = activity.findViewById(R.id.btnClear);
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mStateHolder != null) {
                    mStateHolder.clearAllStroke();
                }
            }
        });
    }

    @Override
    public boolean isEraserEnable() {
        return false;
    }

    @Override
    public void setStateHolder(StateHolder stateHolder) {
        mStateHolder = stateHolder;
    }
}

package com.seewo.eraseaccelerator;

import android.app.Application;

/**
 * 应用类
 * @author chenyikai
 * @date 2020-06-11
 */
public class AcceleratorApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        initData();
        initWriteAccelerator();
    }

    private void initWriteAccelerator() {

    }

    private void initData() {
        App.getInstance().init(this);
    }
}

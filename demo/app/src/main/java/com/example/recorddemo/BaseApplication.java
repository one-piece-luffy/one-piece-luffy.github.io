package com.example.recorddemo;

import android.app.Application;
import android.os.Handler;

/**
 * 应用基类Application(继承于框架基类Application)
 * Created by lishilin on 2016/11/29.
 */
public class BaseApplication extends Application {
    public static BaseApplication instance;

    private final Handler handler = new Handler();

    public static BaseApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
}

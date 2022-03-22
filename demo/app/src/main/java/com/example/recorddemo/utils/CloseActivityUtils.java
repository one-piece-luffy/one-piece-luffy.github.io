package com.example.recorddemo.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * activity 的管理工具
 *
 * @author : kingsly
 * @date : On 2021/3/1
 */
public class CloseActivityUtils {
    public static List<Activity> activityList = new ArrayList<>();

    /**
     * 移除当前页面
     *
     * @param context
     */
    public static void removeActivity(Activity context) {
        if (activityList.contains(context)) {
            activityList.remove(context);
        }
    }

}
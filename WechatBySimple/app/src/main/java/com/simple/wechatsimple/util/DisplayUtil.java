package com.simple.wechatsimple.util;

import android.content.Context;

/**
 * Created by shuangfeng on 17/3/17.
 * DisplayUtil
 */
public class DisplayUtil {

    private static int mStatusBarHeight = 0;

    public static int px2dp(Context context, float pxValue) {
        float density = context.getResources().getDisplayMetrics().density;//得到设备的密度
        return (int) (pxValue / density + 0.5f);
    }

    public static int dp2px(Context context, float dpValue) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * density + 0.5f);
    }

    public static int px2sp(Context context, float pxValue) {
        float scaleDensity = context.getResources().getDisplayMetrics().scaledDensity;//缩放密度
        return (int) (pxValue / scaleDensity + 0.5f);
    }

    public static int sp2px(Context context, float spValue) {
        float scaleDensity = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * scaleDensity + 0.5f);
    }

    public static int getStatusBarHeight(Context context) {
        if (mStatusBarHeight == 0) {
            int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
            mStatusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
        }
        // 获得状态栏高度
        return mStatusBarHeight;
    }
}

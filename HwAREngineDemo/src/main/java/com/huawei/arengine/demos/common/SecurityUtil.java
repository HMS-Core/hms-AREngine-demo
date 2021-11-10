/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 */

package com.huawei.arengine.demos.common;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.BadParcelableException;

/**
 * This utilize is used for security code.
 *
 * @author hw
 * @since 2021-09-7
 */
public class SecurityUtil {
    private static final String TAG = "SecurityUtil";

    /**
     * Start activity in a secure way.
     *
     * @param activity Activity
     * @param intent Intent
     * @param requestCode Request code of the start activity.
     */
    public static void safeStartActivityForResult(Activity activity, Intent intent, int requestCode) {
        try {
            activity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException ex) {
            LogUtil.error(TAG, "Exception" + ex.getClass());
        } catch (IllegalArgumentException ex) {
            LogUtil.error(TAG, "Exception" + ex.getClass());
        }
    }

    /**
     * Perform security verification on the external input uri data.
     *
     * @param activity the activity to be finished
     */
    public static void safeFinishActivity(Activity activity) {
        if (activity == null) {
            LogUtil.error(TAG, "activity is null");
            return;
        }

        try {
            activity.finish();
        } catch (BadParcelableException exception) {
            LogUtil.error(TAG, "BadParcelableException");
        }
    }
}

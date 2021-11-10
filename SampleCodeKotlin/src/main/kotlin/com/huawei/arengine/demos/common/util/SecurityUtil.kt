/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 */
package com.huawei.arengine.demos.common.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.BadParcelableException

import com.huawei.arengine.demos.common.LogUtil

/**
 * This utilize is used for security code.
 *
 * @author hw
 * @since 2021-09-7
 */
object SecurityUtil {
    private const val TAG = "SecurityUtil"

    /**
     * Start activity in a secure way.
     *
     * @param activity Activity
     * @param intent Intent
     * @param requestCode start activityµÄÇëÇóÂë
     */
    fun safeStartActivityForResult(activity: Activity, intent: Intent?, requestCode: Int) {
        try {
            activity.startActivityForResult(intent, requestCode)
        } catch (ex: ActivityNotFoundException) {
            LogUtil.error(TAG, "Exception" + ex.javaClass)
        } catch (ex: IllegalArgumentException) {
            LogUtil.error(TAG, "Exception" + ex.javaClass)
        }
    }

    /**
     * Perform security verification on the external input uri data.
     *
     * @param activity the activity to be finished
     */
    fun safeFinishActivity(activity: Activity?) {
        if (activity == null) {
            LogUtil.error(TAG, "activity is null")
            return
        }
        try {
            activity.finish()
        } catch (exception: BadParcelableException) {
            LogUtil.error(TAG, "BadParcelableException")
        }
    }
}
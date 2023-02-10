/*
 * Copyright 2023. Huawei Technologies Co., Ltd. All rights reserved.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
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
     * @param requestCode Request code of the start activity.
     */
    fun safeStartActivityForResult(activity: Activity, intent: Intent?, requestCode: Int) {
        try {
            activity.startActivityForResult(intent, requestCode)
        } catch (ex: ActivityNotFoundException) {
            LogUtil.error(TAG, "Exception ActivityNotFoundException")
        } catch (ex: IllegalArgumentException) {
            LogUtil.error(TAG, "Exception IllegalArgumentException")
        }
    }

    /**
     * Start activity in a secure way.
     *
     * @param activity Activity
     * @param intent Intent
     */
    fun safeStartActivity(activity: Activity, intent: Intent?) {
        try {
            activity.startActivity(intent)
        } catch (ex: ActivityNotFoundException) {
            LogUtil.error(TAG, "Exception ActivityNotFoundException")
        } catch (ex: IllegalArgumentException) {
            LogUtil.error(TAG, "Exception IllegalArgumentException")
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
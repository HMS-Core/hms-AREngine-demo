/**
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.huawei.arengine.demos.common.util

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.Toast
import com.huawei.arengine.demos.MainApplication
import com.huawei.arengine.demos.common.view.ConnectAppMarketActivity
import com.huawei.hiar.AREnginesApk

/**
 * common utils
 *
 * @author HW
 * @since 2020-11-04
 */
var isRemindInstall = false

inline fun <reified T> startActivityByType() {
    MainApplication.context.apply {
        startActivity(Intent(this, T::class.java))
    }
}

inline fun <reified T> getSystemService(): T? {
    return MainApplication.context.getSystemService(T::class.java)
}

inline fun <reified T : View?> findViewById(activity: Activity, id: Int): T {
    return activity.findViewById<T>(id)
}

fun isAvailableArEngine(activity: Activity): Boolean {
    val isAREngineApkReady = AREnginesApk.isAREngineApkReady(activity)
    if (!isAREngineApkReady && isRemindInstall) {
        Toast.makeText(activity, "Please agree to install.", Toast.LENGTH_LONG).show()
        activity.finish()
    }
    Log.d("isAvailableArEngine", "Is Install AR Engine Apk: $isAREngineApkReady")
    if (!isAREngineApkReady) {
        startActivityByType<ConnectAppMarketActivity>()
        isRemindInstall = true
    }
    return AREnginesApk.isAREngineApkReady(activity)
}
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
import android.content.Intent
import android.graphics.Bitmap
import android.view.MotionEvent
import android.view.View
import android.widget.Toast

import com.huawei.arengine.demos.common.LogUtil
import com.huawei.arengine.demos.common.view.ConnectAppMarketActivity
import com.huawei.arengine.demos.MainApplication
import com.huawei.hiar.AREnginesApk
import com.huawei.hiar.ARFrame
import com.huawei.hiar.ARHitResult

import java.util.Optional

/**
 * common utils
 *
 * @author HW
 * @since 2020-11-04
 */
var isRemindInstall = false

inline fun <reified T> startActivityByType() {
    MainApplication.context.apply {
        startActivity(Intent(this, T::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

inline fun <reified T> getSystemService(): T? {
    return MainApplication.context.getSystemService(T::class.java)
}

inline fun <reified T : View?> findViewById(activity: Activity, id: Int): T {
    return activity.findViewById<T>(id)
}

/**
 * Create a bitmap based on the RGB array.
 *
 * @param rgbData Byte array in RGB_888 format.
 * @param width Number of horizontal pixels.
 * @param height Number of vertical pixels.
 * @return Cube mapping bitmap.
 */
fun createBitmapImage(rgbData: ByteArray, width: Int, height: Int): Optional<Bitmap> {
    // The data passed from the AREngineServer is in the RGB_888 format.
    // The bitmap can be output only after the data is converted into the ARGB_8888 format.
    val colors: IntArray = convertRgbToArgb(rgbData)
    if (colors.isEmpty()) {
        return Optional.empty()
    }
    return try {
        Optional.ofNullable(Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888))
    } catch (e: IllegalArgumentException) {
        LogUtil.error("createBitmapImage", "Exception on the createBitmap.")
        Optional.empty()
    }
}

/**
 * Converts the pixel color in RGB_888 format to ARGB_8888 format.
 *
 * @param rgbArr Byte array of the pixel point color in RGB_888 format.
 * @return Integer array of the pixel point color in ARGB_8888 format.
 */
private fun convertRgbToArgb(rgbArr: ByteArray): IntArray {
    if (rgbArr == null) {
        LogUtil.warn("convertRgbToArgb", "rgbData is null.")
        return IntArray(0)
    }

    // Add an 8-bit transparency channel to convert RGB_888 pixels into ARGB_8888 pixels.
    // In the RGB_888 format, one pixel is stored every 3 bytes.
    // The storage sequence is R-G-B (high on the left and low on the right).
    // In the ARGB_888 format, one pixel is stored for each int.
    // The storage sequence is A-B-G-R (high on the left and low on the right).
    // The 8-bit transparency data of each pixel is stored in bits 24 to 31 of argbArr.
    // The 8-bit B data of each pixel is stored in bits 16 to 23 of argbArr.
    // The 8-bit G data of each pixel is stored in bits 8–15 of argbArr.
    // The 8-bit R data of each pixel is stored in bits 0-7 of argbArr.
    val argbArr = IntArray(rgbArr.size / 3)
    for (i in argbArr.indices) {
        argbArr[i] = ((rgbArr[i * 3].toInt() shl 16) and 0x00FF0000) or
            ((rgbArr[i * 3 + 1].toInt() shl 8) and 0x0000FF00) or
            (rgbArr[i * 3 + 2].toInt() and 0x000000FF) or
            0xFF000000.toInt()
    }
    return argbArr
}

fun isAvailableArEngine(activity: Activity): Boolean {
    val isAREngineApkReady = AREnginesApk.isAREngineApkReady(activity)
    if (!isAREngineApkReady && isRemindInstall) {
        Toast.makeText(activity, "Please agree to install.", Toast.LENGTH_LONG).show()
        activity.finish()
    }
    LogUtil.debug("isAvailableArEngine", "Is Install AR Engine Apk: $isAREngineApkReady")
    if (!isAREngineApkReady) {
        startActivityByType<ConnectAppMarketActivity>()
        isRemindInstall = true
    }
    return AREnginesApk.isAREngineApkReady(activity)
}

fun hitTest(frame: ARFrame, event: MotionEvent?): List<ARHitResult> {
    if (event == null || event.x < 0 || event.y < 0) {
        return emptyList()
    }
    return frame.hitTest(event)
}
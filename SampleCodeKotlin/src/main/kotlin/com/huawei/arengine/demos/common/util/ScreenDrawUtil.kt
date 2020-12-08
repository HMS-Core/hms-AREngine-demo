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
import android.graphics.Color
import android.widget.TextView

/**
 * show fps and screen text view
 *
 * @author HW
 * @since 2020-11-04
 */
data class FramePerSecond(var frameTotal: Int, var fps: Float, var lastFrameTime: Long)

const val UPDATE_INTERVAL = 0.5f

fun calFps(framePerSecond: FramePerSecond): Float {
    val currentTime = System.currentTimeMillis()
    framePerSecond.apply {
        ++frameTotal
        val internalTime = (currentTime - lastFrameTime) / 1000.0f
        // Convert millisecond to second.
        if (internalTime > UPDATE_INTERVAL) {
            fps = frameTotal / internalTime
            frameTotal = 0
            lastFrameTime = currentTime
        }
    }
    return framePerSecond.fps
}

fun showScreenTextView(activity: Activity, textView: TextView, text: String?) {
    activity.runOnUiThread {
        text ?: return@runOnUiThread
        textView.run {
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 0)
            // Set the size of the text displayed on the screen.
            textSize = 10f
            this.text = text
        }
    }
}
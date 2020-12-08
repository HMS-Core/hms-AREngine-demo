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
package com.huawei.arengine.demos.hand.service

import com.huawei.arengine.demos.common.util.FramePerSecond
import com.huawei.arengine.demos.common.util.calFps
import com.huawei.hiar.ARHand

/**
 * update screen text
 *
 * @author HW
 * @since 2020-11-04
 */
fun updateScreenText(screenText: StringBuilder, hand: ARHand, framePerSecond: FramePerSecond) {
    screenText.apply {
        appendln("FPS= ${calFps(framePerSecond)}")
    }.also {
        addHandNormalString(it, hand)
        addGestureCenterString(it, hand)
        addHandBoxString(it, hand)
        addHandSkeletonString(it, hand)
    }
}

private fun addHandNormalString(screenText: StringBuilder, hand: ARHand) {
    screenText.run {
        appendln("GestureType= ${hand.gestureType}")
        appendln("GestureCoordinateSystem= ${hand.gestureCoordinateSystem}")
    }
}

private fun addGestureCenterString(screenText: StringBuilder, hand: ARHand) {
    val gestureCenter = hand.gestureCenter
    screenText.run {
        appendln("gestureCenter length:[${gestureCenter.size}]")
        for (i in gestureCenter.indices) {
            appendln("gestureCenter[$i]:[${gestureCenter[i]}]")
        }
    }
}

private fun addHandBoxString(screenText: StringBuilder, hand: ARHand) {
    val gestureHandBox = hand.gestureHandBox
    screenText.run {
        appendln("GestureHandBox length:[${gestureHandBox.size}]")
        for (i in gestureHandBox.indices) {
            appendln("gesturePoints[$i]:[${gestureHandBox[i]}]")
        }
    }
}

private fun addHandSkeletonString(screenText: StringBuilder, hand: ARHand) {
    val skeletonArray = hand.handskeletonArray
    val handSkeletonConnection = hand.handSkeletonConnection
    screenText.run {
        appendln("Handtype= ${hand.handtype}")
        appendln("SkeletonCoordinateSystem= ${hand.skeletonCoordinateSystem}")
        appendln("HandSkeletonArray length:[${skeletonArray.size}]")
        appendln("HandSkeletonConnection length:[${handSkeletonConnection.size}]")
    }
}
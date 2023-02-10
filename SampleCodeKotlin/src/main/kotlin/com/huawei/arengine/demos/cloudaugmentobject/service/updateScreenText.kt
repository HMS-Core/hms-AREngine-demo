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

package com.huawei.arengine.demos.cloudaugmentobject.service

import com.huawei.arengine.demos.common.util.FramePerSecond
import com.huawei.arengine.demos.common.util.calFps
import com.huawei.hiar.ARObject
import com.huawei.hiar.ARTrackable

/**
 * update screen text
 *
 * @author HW
 * @since 2022-04-12
 */
fun updateScreenText(screenText: StringBuilder, objects: Collection<ARObject>?, framePerSecond: FramePerSecond) {
    screenText.run {
        appendLine("FPS = ${calFps(framePerSecond)}")
        appendLine("object size: ${objects?.size}")
        objects?.forEach continuing@{
            if (it.trackingState != ARTrackable.TrackingState.TRACKING)
                return@continuing
            val pose = it.centerPose ?: return@continuing
            appendLine("object state: ${it.trackingState}")
            appendLine("object name: ${it.name}")
            appendLine("object ID: ${it.objectID}")
            appendLine("arPose x: ${pose.tx()} y: ${pose.ty()} z: ${pose.tz()}")
            appendLine("arPose qx: ${pose.qx()} qy: ${pose.qy()} qz: ${pose.qz()} qw: ${pose.qw()}")
            appendLine("object anchor id: ${it.objectAnchorId}")
        }
    }
}
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

package com.huawei.arengine.demos.face.service

import com.huawei.arengine.demos.common.util.FramePerSecond
import com.huawei.arengine.demos.common.util.calFps
import com.huawei.hiar.ARFace
import com.huawei.hiar.ARLightEstimate
import com.huawei.hiar.ARTrackable

import java.util.Arrays

/**
 * update screen text
 *
 * @author HW
 * @since 2020-11-04
 */
fun updateScreenText(screenText: StringBuilder, faces: Collection<ARFace>,
    lightEstimate: ARLightEstimate?, framePerSecond: FramePerSecond) {
    var index = 1;
    screenText.run {
        appendLine("FPS= ${calFps(framePerSecond)}")
        faces.forEach continuing@{ face ->
            if (face.trackingState != ARTrackable.TrackingState.TRACKING) {
                return@continuing
            }
            val pose = face.pose ?: return@continuing
            appendLine("face $index pose information:")
            appendLine("face pose tx:[${pose.tx()}]")
            appendLine("face pose ty:[${pose.ty()}]")
            appendLine("face pose tz:[${pose.tz()}]")
            appendLine(System.lineSeparator())
            appendLine("textureCoordinates length:[${face.faceGeometry.textureCoordinates.array().size}]")
            appendLine(System.lineSeparator())
            index++
        }
        lightEstimate?.apply {
            append("PrimaryLightIntensity=")
            appendLine(lightEstimate.getPrimaryLightIntensity())
            append("PrimaryLightDirection=")
            appendLine(Arrays.toString(lightEstimate.primaryLightDirection))
            append("LightSphericalHarmonicCoefficients=")
            appendLine(Arrays.toString(lightEstimate.sphericalHarmonicCoefficients))
        }
    }
}
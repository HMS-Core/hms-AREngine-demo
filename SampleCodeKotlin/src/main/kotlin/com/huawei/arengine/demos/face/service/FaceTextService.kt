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
package com.huawei.arengine.demos.face.service

import com.huawei.arengine.demos.common.util.FramePerSecond
import com.huawei.arengine.demos.common.util.calFps
import com.huawei.hiar.ARFace

/**
 * update screen text
 *
 * @author HW
 * @since 2020-11-04
 */
fun updateScreenText(screenText: StringBuilder, face: ARFace, framePerSecond: FramePerSecond) {
    val pose = face.pose
    screenText.run {
        appendln("FPS= ${calFps(framePerSecond)}")
        appendln("face pose information:")
        appendln("face pose tx:[${pose.tx()}]")
        appendln("face pose tx:[${pose.ty()}]")
        appendln("face pose tx:[${pose.tz()}]")
        appendln("face pose tx:[${pose.qx()}]")
        appendln("face pose tx:[${pose.qy()}]")
        appendln("face pose tx:[${pose.qz()}]")
        appendln("face pose tx:[${pose.qw()}]")
        appendln("textureCoordinates length:[${face.faceGeometry.textureCoordinates.array().size}]")
    }
}
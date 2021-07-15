/**
 * Copyright 2021. Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huawei.arengine.demos.augmentedimage.util

/**
 * constant class object.
 *
 * @author HW
 * @since 2021-03-29
 */
object Constants {
    private val LS = System.lineSeparator()

    val LABEL_VERTEX = ("uniform mat2 inPlanUVMatrix;" + LS
        + "uniform mat4 inMVPMatrix;" + LS
        + "attribute vec3 inPosXZAlpha;" + LS
        + "varying vec3 varTexCoordAlpha;" + LS
        + "void main() {" + LS
        + "    vec4 tempPosition = vec4(inPosXZAlpha.x, 0.0, inPosXZAlpha.y, 1.0);" + LS
        + "    vec2 tempUV = inPlanUVMatrix * inPosXZAlpha.xy;" + LS
        + "    varTexCoordAlpha = vec3(tempUV.x + 0.5, tempUV.y + 0.5, inPosXZAlpha.z);" + LS
        + "    gl_Position = inMVPMatrix * tempPosition;" + LS
        + "}")

    val LABEL_FRAGMENT = ("precision highp float;" + LS
        + "uniform sampler2D inTexture;" + LS
        + "varying vec3 varTexCoordAlpha;" + LS
        + "void main() {" + LS
        + "    vec4 control = texture2D(inTexture, varTexCoordAlpha.xy);" + LS
        + "    gl_FragColor = vec4(control.rgb, 1.0);" + LS
        + "}")

    val LP_VERTEX = ("uniform vec4 inColor;" + LS
        + "attribute vec4 inPosition;" + LS
        + "uniform float inPointSize;" + LS
        + "varying vec4 varColor;" + LS
        + "uniform mat4 inMVPMatrix;" + LS
        + "void main() {" + LS
        + "    gl_PointSize = inPointSize;" + LS
        + "    gl_Position = inMVPMatrix * vec4(inPosition.xyz, 1.0);" + LS
        + "    varColor = inColor;" + LS
        + "}")

    val LP_FRAGMENT = ("precision mediump float;" + LS
        + "varying vec4 varColor;" + LS
        + "void main() {" + LS
        + "    gl_FragColor = varColor;" + LS
        + "}")

    const val PROJ_MATRIX_SIZE = 16

    const val PROJ_MATRIX_OFFSET = 0

    const val PROJ_MATRIX_NEAR = 0.1f

    const val PROJ_MATRIX_FAR = 100.0f

    /**
     * 3D coordinates. The coordinates have four components (x, y, z, and alpha). One float occupies 4 bytes.
     */
    const val BYTES_PER_POINT = 4 * 4

    const val INITIAL_BUFFER_POINTS = 150

    const val COORDINATE_DIMENSION = 3

    const val BYTES_PER_CORNER = 4

    const val INITIAL_POINTS_SIZE = 20

    const val IMAGE_ANGLE_MATRIX_SIZE = 4

    const val MATRIX_SCALE_SX = 1.0f

    const val MATRIX_SCALE_SY = 1.0f

    const val COORDS_PER_VERTEX = 3

    const val LABEL_WIDTH = 0.1f

    const val LABEL_HEIGHT = 0.05f

    const val TEXTURES_SIZE = 1

    const val MATRIX_SIZE = 16
}
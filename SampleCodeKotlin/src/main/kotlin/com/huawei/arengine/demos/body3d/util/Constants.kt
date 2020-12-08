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
package com.huawei.arengine.demos.body3d.util

/**
 * This class provides code and programs related to body rendering shader.
 *
 * @author hw
 * @since 2020-11-06
 */
object Constants {
    /**
     * Code for the vertex shader.
     */
    const val BODY_VERTEX = ("uniform vec4 inColor;"
        + "attribute vec4 inPosition;"
        + "uniform float inPointSize;"
        + "varying vec4 varColor;"
        + "uniform mat4 inProjectionMatrix;"
        + "uniform float inCoordinateSystem;"
        + "void main() {"
        + "    vec4 position = vec4(inPosition.xyz, 1.0);"
        + "    if (inCoordinateSystem == 2.0) {"
        + "        position = inProjectionMatrix * position;"
        + "    }"
        + "    gl_Position = position;"
        + "    varColor = inColor;"
        + "    gl_PointSize = inPointSize;"
        + "}")

    /**
     * Code for the segment shader.
     */
    const val BODY_FRAGMENT = ("precision mediump float;"
        + "varying vec4 varColor;"
        + "void main() {"
        + "    gl_FragColor = varColor;"
        + "}")

    const val PROJECTION_MATRIX_NEAR = 0.1f

    const val PROJECTION_MATRIX_FAR = 100.0f

    const val BYTES_PER_POINT = 4 * 3

    const val COORDINATE_SYSTEM_TYPE_3D_FLAG = 2.0f

    const val LINE_POINT_RATIO = 6

    const val INITIAL_POINTS_SIZE = 150
}
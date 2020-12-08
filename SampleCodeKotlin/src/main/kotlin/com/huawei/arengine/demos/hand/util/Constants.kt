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
package com.huawei.arengine.demos.hand.util

/**
 * constant class object.
 *
 * @author HW
 * @since 2020-11-06
 */
object Constants {
    /**
     * Code for the hand vertex shader.
     */
    const val HAND_VERTEX = ("uniform vec4 inColor;"
        + "attribute vec4 inPosition;"
        + "uniform float inPointSize;"
        + "varying vec4 varColor;"
        + "uniform mat4 inMVPMatrix;"
        + "void main() {"
        + "    gl_PointSize = inPointSize;"
        + "    gl_Position = inMVPMatrix * vec4(inPosition.xyz, 1.0);"
        + "    varColor = inColor;"
        + "}")

    /**
     * Code for the hand fragment shader.
     */
    const val HAND_FRAGMENT = ("precision mediump float;"
        + "varying vec4 varColor;"
        + "void main() {"
        + "    gl_FragColor = varColor;"
        + "}")

    /*
     * Number of bytes occupied by each 3D coordinate. Float data occupies 4 bytes.
     * Each skeleton point represents a 3D coordinate.
     */
    const val BYTES_PER_POINT = 4 * 3

    const val COORDINATE_DIMENSION_3D = 3

    const val INITIAL_POINTS_SIZE = 150

    const val POINT_NUM_LINE = 2

    const val PROJECTION_MATRIX_NEAR = 0.1f

    const val PROJECTION_MATRIX_FAR = 100.0f
}
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
package com.huawei.arengine.demos.face.util

/**
 * Common Constants.
 *
 * @author HW
 * @since 2020-11-06
 */
object Constants {
    const val FACE_GEOMETRY_VERTEX = ("attribute vec2 inTexCoord;"
        + "uniform mat4 inMVPMatrix;"
        + "uniform float inPointSize;"
        + "attribute vec4 inPosition;"
        + "uniform vec4 inColor;"
        + "varying vec4 varAmbient;"
        + "varying vec4 varColor;"
        + "varying vec2 varCoord;"
        + "void main() {"
        + "    varAmbient = vec4(1.0, 1.0, 1.0, 1.0);"
        + "    gl_Position = inMVPMatrix * vec4(inPosition.xyz, 1.0);"
        + "    varColor = inColor;"
        + "    gl_PointSize = inPointSize;"
        + "    varCoord = inTexCoord;"
        + "}")

    const val FACE_GEOMETRY_FRAGMENT = ("precision mediump float;"
        + "uniform sampler2D inTexture;"
        + "varying vec4 varColor;"
        + "varying vec2 varCoord;"
        + "varying vec4 varAmbient;"
        + "void main() {"
        + "    vec4 objectColor = texture2D(inTexture, vec2(varCoord.x, 1.0 - varCoord.y));"
        + "    if(varColor.x != 0.0) {"
        + "        gl_FragColor = varColor * varAmbient;"
        + "    }"
        + "    else {"
        + "        gl_FragColor = objectColor * varAmbient;"
        + "    }"
        + "}")

    /**
     * Each floating-point number occupies 4 bytes, and each point has three dimensions.
     */
    const val BYTES_PER_POINT = 4 * 3

    const val BYTES_PER_FLOAT = 4

    /**
     * Number of bytes occupied by each 2D coordinate point.
     */
    const val BYTES_PER_COORD = 4 * 2

    const val PROJECTION_MATRIX_NEAR = 0.1f

    const val PROJECTION_MATRIX_FAR = 100.0f

    const val POSITION_COMPONENTS_NUMBER = 4

    const val TEXCOORD_COMPONENTS_NUMBER = 2
}
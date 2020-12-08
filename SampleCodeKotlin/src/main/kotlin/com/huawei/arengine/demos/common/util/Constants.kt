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

/**
 * Common Constants.
 *
 * @author HW
 * @since 2020-11-06
 */
object Constants {
    /**
     * Background Texture Fragment Shader.
     */
    const val BASE_FRAGMENT = ("#extension GL_OES_EGL_image_external : require\n"
        + "precision mediump float;"
        + "varying vec2 textureCoordinate;"
        + "uniform samplerExternalOES vTexture;"
        + "void main() {"
        + "    gl_FragColor = texture2D(vTexture, textureCoordinate );"
        + "}")

    /**
     * Background Texture Vertex Shader.
     */
    const val BASE_VERTEX = ("attribute vec4 vPosition;"
        + "attribute vec2 vCoord;"
        + "uniform mat4 vMatrix;"
        + "uniform mat4 vCoordMatrix;"
        + "varying vec2 textureCoordinate;"
        + "void main(){"
        + "    gl_Position = vMatrix*vPosition;"
        + "    textureCoordinate = (vCoordMatrix*vec4(vCoord,0,1)).xy;"
        + "}")

    const val RGB_CLEAR_VALUE = 0.8157f

    const val MATRIX_SIZE = 16

    val COORDINATE_VERTEX = floatArrayOf(-1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, -1.0f)

    val COORDINATE_TEXTURE = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f)
}
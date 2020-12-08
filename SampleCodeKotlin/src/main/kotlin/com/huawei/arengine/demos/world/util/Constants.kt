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
package com.huawei.arengine.demos.world.util

import android.opengl.Matrix

/**
 * constant class object.
 *
 * @author HW
 * @since 2020-11-06
 */
object Constants {
    /**
     * Vertex shader for label rendering.
     */
    const val LABEL_VERTEX = ("uniform mat2 inPlanUVMatrix;"
        + "uniform mat4 inMVPMatrix;"
        + "attribute vec3 inPosXZAlpha;"
        + "varying vec3 varTexCoordAlpha;"
        + "void main() {"
        + "    vec4 tempPosition = vec4(inPosXZAlpha.x, 0.0, inPosXZAlpha.y, 1.0);"
        + "    vec2 tempUV = inPlanUVMatrix * inPosXZAlpha.xy;"
        + "    varTexCoordAlpha = vec3(tempUV.x + 0.5, tempUV.y + 0.5, inPosXZAlpha.z);"
        + "    gl_Position = inMVPMatrix * tempPosition;"
        + "}")

    /**
     * Fragment shader for label rendering.
     */
    const val LABEL_FRAGMENT = ("precision highp float;"
        + "uniform sampler2D inTexture;"
        + "varying vec3 varTexCoordAlpha;"
        + "void main() {"
        + "    vec4 control = texture2D(inTexture, varTexCoordAlpha.xy);"
        + "    gl_FragColor = vec4(control.rgb, 1.0);"
        + "}")

    const val OBJECT_VERTEX = ("uniform mat4 inMVPMatrix;"
        + "uniform mat4 inViewMatrix;"
        + "attribute vec3 inObjectNormalVector;"
        + "attribute vec4 inObjectPosition;"
        + "attribute vec2 inTexCoordinate;"
        + "varying vec3 varCameraNormalVector;"
        + "varying vec2 varTexCoordinate;"
        + "varying vec3 varCameraPos;"
        + "void main() {"
        + "    gl_Position = inMVPMatrix * inObjectPosition;"
        + "    varCameraNormalVector = (inViewMatrix * vec4(inObjectNormalVector, 0.0)).xyz;"
        + "    varTexCoordinate = inTexCoordinate;"
        + "    varCameraPos = (inViewMatrix * inObjectPosition).xyz;"
        + "}")

    const val OBJECT_FRAGMENT = ("precision mediump float;"
        + "uniform vec4 inLight;"
        + "uniform vec4 inObjectColor;"
        + "uniform sampler2D inObjectTexture;"
        + "varying vec3 varCameraPos;"
        + "varying vec3 varCameraNormalVector;"
        + "varying vec2 varTexCoordinate;"
        + "void main() {"
        + "    vec4 objectColor = texture2D(inObjectTexture, vec2(varTexCoordinate.x, 1.0 - varTexCoordinate.y));"
        + "    objectColor.rgb = inObjectColor.rgb / 255.0;"
        + "    vec3 viewNormal = normalize(varCameraNormalVector);"
        + "    vec3 reflectedLightDirection = reflect(inLight.xyz, viewNormal);"
        + "    vec3 normalCameraPos = normalize(varCameraPos);"
        + "    float specularStrength = max(0.0, dot(normalCameraPos, reflectedLightDirection));"
        + "    gl_FragColor.a = objectColor.a;"
        + "    float diffuse = inLight.w * 3.5 * 0.5 * (dot(viewNormal, inLight.xyz) + 1.0);"
        + "    float specular = inLight.w * pow(specularStrength, 6.0);"
        + "    gl_FragColor.rgb = objectColor.rgb * + diffuse + specular;"
        + "}")

    const val PROJ_MATRIX_NEAR = 0.1f

    const val PROJ_MATRIX_FAR = 100.0f

    const val ROTATION_ANGLE = 315.0f

    const val MATRIX_SIZE = 16

    const val SCALE_FACTOR = 0.15f

    const val COORDS_PER_VERTEX = 3

    const val LABEL_WIDTH = 0.3f

    const val LABEL_HEIGHT = 0.3f

    const val TEXTURES_SIZE = 12

    const val FLOAT_BYTE_SIZE = 4

    const val INDEX_COUNT_RATIO = 2

    const val MAX_VIRTUAL_OBJECT = 16

    val BLUE_COLORS = floatArrayOf(66.0f, 133.0f, 244.0f, 255.0f)

    val GREEN_COLORS = floatArrayOf(66.0f, 133.0f, 244.0f, 255.0f)

    /**
     * Set the default light direction.
     */
    val LIGHT_DIRECTIONS = floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f)

    /**
     * Set a scaling matrix, in which the elements of the principal diagonal is the scaling coefficient.
     */
    val FACTOR_MODEL_MATRIX by lazy {
        FloatArray(MATRIX_SIZE).also {
            Matrix.setIdentityM(it, 0)
            (0..2).forEach { i ->
                it[i * 5] = SCALE_FACTOR
            }
            // Rotate the camera along the Y axis by a certain angle.
            Matrix.rotateM(it, 0, ROTATION_ANGLE, 0f, 1f, 0f)
        }
    }
}
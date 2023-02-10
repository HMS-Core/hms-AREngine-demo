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

package com.huawei.arengine.demos.cloudaugmentobject.util

import android.opengl.GLES20

import com.huawei.arengine.demos.common.LogUtil

/**
 * Obtains the cloud 3D object recognition shader.
 *
 * @author HW
 * @since 2022-04-12
 */
object ObjectLabelShaderUtil {
    private val TAG = "ObjectLabelShaderUtil"

    /**
     * Newline character.
     */
    private val LS: String = System.lineSeparator()

    /**
     * Vertex shader used for label rendering.
     */
    private val LABEL_VERTEX = ("uniform mat2 inPlanUVMatrix;" + LS
        + "uniform mat4 inMVPMatrix;" + LS
        + "attribute vec3 inPosXZAlpha;" + LS
        + "varying vec3 varTexCoordAlpha;" + LS
        + "void main() {" + LS
        + "    vec4 tempPosition = vec4(inPosXZAlpha.x, 0.0, inPosXZAlpha.y, 1.0);" + LS
        + "    vec2 tempUV = inPlanUVMatrix * inPosXZAlpha.xy;" + LS
        + "    varTexCoordAlpha = vec3(tempUV.x + 0.5, tempUV.y + 0.5, inPosXZAlpha.z);" + LS
        + "    gl_Position = inMVPMatrix * tempPosition;" + LS
        + "}")

    /**
     * Segment shader used for label rendering.
     */
    private val LABEL_FRAGMENT = ("precision highp float;" + LS
        + "uniform sampler2D inTexture;" + LS
        + "varying vec3 varTexCoordAlpha;" + LS
        + "void main() {" + LS
        + "    vec4 control = texture2D(inTexture, varTexCoordAlpha.xy);" + LS
        + "    gl_FragColor = vec4(control.rgb, 1.0);" + LS
        + "}")

    fun createGlProgram(): Int {
        val vertex = loadShader(GLES20.GL_VERTEX_SHADER, LABEL_VERTEX)
        if (vertex == 0) {
            return 0
        }
        val fragment = loadShader(GLES20.GL_FRAGMENT_SHADER, LABEL_FRAGMENT)
        if (fragment == 0) {
            return 0
        }
        var program = GLES20.glCreateProgram()
        if (program != 0) {
            GLES20.glAttachShader(program, vertex)
            GLES20.glAttachShader(program, fragment)
            GLES20.glLinkProgram(program)
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] != GLES20.GL_TRUE) {
                LogUtil.error(TAG, "Could not link program: " + GLES20.glGetProgramInfoLog(program))
                GLES20.glDeleteProgram(program)
                program = 0
            }
        }
        return program
    }

    private fun loadShader(shaderType: Int, source: String): Int {
        var shader = GLES20.glCreateShader(shaderType)
        if (shader != 0) {
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            var compiled = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                LogUtil.error(TAG, "glError: Could not compile shader $shaderType")
                LogUtil.error(TAG, "GLES20 Error: " + GLES20.glGetShaderInfoLog(shader))
                GLES20.glDeleteShader(shader)
                shader = 0
            }
        }
        return shader
    }
}
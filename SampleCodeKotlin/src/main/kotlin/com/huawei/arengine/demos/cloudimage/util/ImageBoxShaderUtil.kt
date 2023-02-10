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

package com.huawei.arengine.demos.cloudimage.util

import android.opengl.GLES20

import com.huawei.arengine.demos.common.LogUtil

/**
 * This class provides code and programs for the shader related to cloud image.
 *
 * @author HW
 * @since 2022-03-22
 */
object ImageBoxShaderUtil {
    /**
     * Newline character.
     */
    private val LS: String = System.lineSeparator()

    /**
     * Code for the hand vertex shader.
     */
    private val VERTEX_SHADER = ("uniform vec4 inColor;" + LS
            + "attribute vec4 inPosition;" + LS
            + "uniform float inPointSize;" + LS
            + "varying vec4 varColor;" + LS
            + "uniform mat4 inMVPMatrix;" + LS
            + "void main() {" + LS
            + "    gl_PointSize = inPointSize;" + LS
            + "    gl_Position = inMVPMatrix * vec4(inPosition.xyz, 1.0);" + LS
            + "    varColor = inColor;" + LS
            + "}")

    /**
     * Code for the hand fragment shader.
     */
    private val FRAGMENT_SHADER = ("precision mediump float;" + LS
            + "varying vec4 varColor;" + LS
            + "void main() {" + LS
            + "    gl_FragColor = varColor;" + LS
            + "}")

    private val TAG = "ImageBoxShaderUtil"

    /**
     * Shader program generator.
     *
     * @return program handle
     */
    fun createGlProgram(): Int {
        val vertex = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        if (vertex == 0) {
            return 0
        }
        val fragment = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
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
            val compiled = IntArray(1)
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
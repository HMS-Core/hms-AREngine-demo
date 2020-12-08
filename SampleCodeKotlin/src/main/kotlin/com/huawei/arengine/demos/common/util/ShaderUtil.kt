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

import android.opengl.GLES20
import android.util.Log
import com.huawei.arengine.demos.common.exception.SampleAppException

private const val TAG = "ShaderUtil"

/**
 * Check OpenGL ES running exceptions and throw them when necessary.
 *
 * @param tag Exception information.
 * @param label Program label.
 */
fun checkGlError(tag: String, label: String) {
    var lastError = GLES20.GL_NO_ERROR
    var error = GLES20.glGetError()
    while (error != GLES20.GL_NO_ERROR) {
        Log.e(tag, "$label: glError $error")
        lastError = error
        error = GLES20.glGetError()
    }
    if (lastError != GLES20.GL_NO_ERROR) {
        throw SampleAppException("$label: glError $lastError")
    }
}

fun createGlProgram(vertexShader: String, fragmentShader: String): Int {
    val vertex = loadShader(GLES20.GL_VERTEX_SHADER, vertexShader)
    val fragment = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader)
    if (vertex == 0 || fragment == 0) {
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
            Log.e(TAG, "Could not link program " + GLES20.glGetProgramInfoLog(program))
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
            Log.e(TAG, "glError: Could not compile shader $shaderType")
            Log.e(TAG, "glError: " + GLES20.glGetShaderInfoLog(shader))
            GLES20.glDeleteShader(shader)
            shader = 0
        }
    }
    return shader
}
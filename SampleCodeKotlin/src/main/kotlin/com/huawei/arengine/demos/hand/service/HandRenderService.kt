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
package com.huawei.arengine.demos.hand.service

import android.opengl.GLES20
import com.huawei.arengine.demos.common.util.checkGlError
import com.huawei.arengine.demos.common.util.createGlProgram
import com.huawei.arengine.demos.hand.pojo.ShaderPojo
import com.huawei.arengine.demos.hand.util.Constants
import com.huawei.hiar.ARHand

/**
 * Rendering hand AR type related data.
 *
 * @author HW
 * @since 2020-10-10
 */
abstract class HandRenderService {
    companion object {
        private const val TAG = "HandRenderService"
    }

    protected val shaderPojo by lazy { ShaderPojo() }

    /**
     * Init render.
     */
    fun init() {
        checkGlError(TAG, "Init start.")
        val buffers = IntArray(1)
        GLES20.glGenBuffers(1, buffers, 0)
        shaderPojo.run {
            vbo = buffers[0]
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vboSize, null, GLES20.GL_DYNAMIC_DRAW)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        }
        createProgram()
        checkGlError(TAG, "Init end.")
    }

    private fun createProgram() {
        checkGlError(TAG, "Create program start.")
        shaderPojo.run {
            program = createGlProgram(Constants.HAND_VERTEX, Constants.HAND_FRAGMENT)
            position = GLES20.glGetAttribLocation(program, "inPosition")
            color = GLES20.glGetUniformLocation(program, "inColor")
            pointSize = GLES20.glGetUniformLocation(program, "inPointSize")
            modelViewProjectionMatrix = GLES20.glGetUniformLocation(program, "inMVPMatrix")
        }
        checkGlError(TAG, "Create program end.")
    }

    /**
     * Render objects, call per frame
     *
     * @param hands ARHands
     * @param projectionMatrix Camera projection matrix.
     */
    abstract fun renderHand(hands: Collection<ARHand>, projectionMatrix: FloatArray)
}
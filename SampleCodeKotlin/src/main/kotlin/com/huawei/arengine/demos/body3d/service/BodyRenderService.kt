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
package com.huawei.arengine.demos.body3d.service

import android.opengl.GLES20
import com.huawei.arengine.demos.body3d.pojo.ShaderPojo
import com.huawei.arengine.demos.body3d.util.Constants
import com.huawei.arengine.demos.common.util.checkGlError
import com.huawei.arengine.demos.common.util.createGlProgram
import com.huawei.hiar.ARBody

/**
 * Rendering body AR type related data.
 *
 * @author HW
 * @since 2020-10-10
 */
abstract class BodyRenderService {
    companion object {
        private const val TAG = "BodyRenderService"
    }

    protected val shaderPojo by lazy { ShaderPojo() }

    /**
     * Create a body skeleton shader on the GL thread.
     * This method is called when [BodyRenderController.onSurfaceCreated].
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
        checkGlError(TAG, "Create gl program start.")
        shaderPojo.run {
            program = createGlProgram(Constants.BODY_VERTEX, Constants.BODY_FRAGMENT)
            program.let {
                position = GLES20.glGetAttribLocation(it, "inPosition")
                color = GLES20.glGetUniformLocation(it, "inColor")
                pointSize = GLES20.glGetUniformLocation(it, "inPointSize")
                projectionMatrix = GLES20.glGetUniformLocation(it, "inProjectionMatrix")
                coordinateSystem = GLES20.glGetUniformLocation(it, "inCoordinateSystem")
            }
        }
        checkGlError(TAG, "Create gl program end.")
    }

    /**
     * Render objects, call per frame.
     *
     * @param bodies ARBodies.
     * @param projectionMatrix Camera projection matrix.
     */
    abstract fun renderBody(bodies: Collection<ARBody>, projectionMatrix: FloatArray)
}
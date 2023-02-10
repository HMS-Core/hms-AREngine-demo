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

package com.huawei.arengine.demos.cloudimage.service

import android.opengl.GLES20
import android.opengl.Matrix

import com.huawei.arengine.demos.cloudimage.model.ImageBox
import com.huawei.arengine.demos.cloudimage.model.ShaderPojo
import com.huawei.arengine.demos.cloudimage.util.ImageBoxShaderUtil
import com.huawei.arengine.demos.common.util.checkGlError
import com.huawei.hiar.ARAugmentedImage

/**
 * Draw cloud image box based on the coordinates of the cloud image.
 *
 * @author HW
 * @since 2022-03-22
 */
class ImageBoxRenderService {
    private val shaderPojo: ShaderPojo = ShaderPojo()

    /**
     * Create and build the shader for the image box on the OpenGL thread.
     */
    fun init() {
        checkGlError(TAG, "Init start.")
        val buffers = IntArray(1)
        GLES20.glGenBuffers(1, buffers, 0)
        shaderPojo.setVbo(buffers[0])
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, shaderPojo.getVbo())
        shaderPojo.setVboSize(INITIAL_POINTS_SIZE * BYTES_PER_POINT)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, shaderPojo.getVboSize(), null, GLES20.GL_DYNAMIC_DRAW)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        createProgram()
        checkGlError(TAG, "Init end.")
    }

    private fun createProgram() {
        checkGlError(TAG, "Create program start.")
        shaderPojo.setProgram(ImageBoxShaderUtil.createGlProgram())
        val program: Int = shaderPojo.getProgram()
        checkGlError(TAG, "program")
        shaderPojo.setPosition(GLES20.glGetAttribLocation(program, "inPosition"))
        shaderPojo.setColor(GLES20.glGetUniformLocation(program, "inColor"))
        shaderPojo.setPointSize(GLES20.glGetUniformLocation(program, "inPointSize"))
        shaderPojo.setMvpMatrix(GLES20.glGetUniformLocation(program, "inMVPMatrix"))
        checkGlError(TAG, "Create program end.")
    }

    /**
     * Draw image box to augmented image.
     *
     * @param augmentedImage Identified image to be augmented.
     * @param viewMatrix view matrix
     * @param projectionMatrix Projection matrix(4 * 4).
     */
    fun drawImageBox(augmentedImage: ARAugmentedImage, viewMatrix: FloatArray, projectionMatrix: FloatArray) {
        var vpMatrix = FloatArray(BYTES_PER_POINT)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        var imageBox = ImageBox(augmentedImage, shaderPojo)
        imageBox.draw(vpMatrix)
    }

    companion object {
        private const val TAG = "ImageBoxRenderService"

        // Number of bytes occupied by each 3D coordinate. float data occupies 4 bytes.
        private const val BYTES_PER_POINT = 4 * 4

        private const val INITIAL_POINTS_SIZE = 20
    }
}
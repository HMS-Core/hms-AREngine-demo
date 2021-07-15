/**
 * Copyright 2021. Huawei Technologies Co., Ltd. All rights reserved.
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.arengine.demos.augmentedimage.service

import android.opengl.GLES20
import android.opengl.Matrix
import com.huawei.arengine.demos.augmentedimage.pojo.ImageShaderPojo
import com.huawei.arengine.demos.augmentedimage.controller.AugmentedImageComponentDisplay
import com.huawei.arengine.demos.augmentedimage.corner.AugmentImageCorner
import com.huawei.arengine.demos.augmentedimage.util.Constants
import com.huawei.arengine.demos.augmentedimage.util.CornerType
import com.huawei.arengine.demos.common.util.checkGlError
import com.huawei.arengine.demos.common.util.createGlProgram
import com.huawei.hiar.ARAugmentedImage
import java.nio.FloatBuffer

/**
 * Draw the border of the augmented image based on the pose of the center, and the width and height
 * of the augmented image.
 *
 * @author HW
 * @since 2021-03-29
 */
class ImageKeyLineService : AugmentedImageComponentDisplay {
    companion object {
        private val TAG = "ImageKeyLineDisplay"
    }

    private val imageCornerService by lazy { AugmentImageCorner() }

    private val imageShaderPojo by lazy { ImageShaderPojo() }

    /**
     * Create and build an augmented image shader on the OpenGL thread.
     */
    override fun init() {
        val buffers = IntArray(1)
        imageShaderPojo.run {
            GLES20.glGenBuffers(1, buffers, 0)
            vbo = buffers[0]
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
            vboSize = Constants.INITIAL_BUFFER_POINTS * Constants.BYTES_PER_POINT
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vboSize, null, GLES20.GL_DYNAMIC_DRAW)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
            createProgram()
        }
        checkGlError(TAG, "Init end.")
    }

    private fun createProgram() {
        checkGlError(TAG, "Create imageKeyLine program start.")
        imageShaderPojo.run {
            program = createGlProgram(Constants.LP_VERTEX, Constants.LP_FRAGMENT)
            position = GLES20.glGetAttribLocation(program, "inPosition")
            color = GLES20.glGetUniformLocation(program, "inColor")
            modelViewProjection = GLES20.glGetUniformLocation(program, "inMVPMatrix")
        }
        checkGlError(TAG, "Create imageKeyLine program end.")
    }

    /**
     * Draw the borders of the augmented image.
     *
     * @param augmentedImage AugmentedImage object.
     * @param viewMatrix View matrix.
     * @param projectionMatrix AR camera projection matrix.
     */
    override fun onDrawFrame(augmentedImage: ARAugmentedImage, viewMatrix: FloatArray, projectionMatrix: FloatArray) {
        val vpMatrix = FloatArray(Constants.BYTES_PER_POINT)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        draw(augmentedImage, vpMatrix)
    }

    /**
     * Draw borders to augment the image.
     *
     * @param augmentedImage AugmentedImage object.
     * @param viewProjectionMatrix View projection matrix.
     */
    private fun draw(augmentedImage: ARAugmentedImage, viewProjectionMatrix: FloatArray) {
        imageCornerService.cornerPointCoordinates = FloatArray(Constants.BYTES_PER_CORNER * 4)
        CornerType.values().forEach {
            imageCornerService.createImageCorner(augmentedImage, it)
        }
        updateImageKeyLineData(imageCornerService.cornerPointCoordinates!!)
        drawImageLine(viewProjectionMatrix)
        imageCornerService.cornerPointCoordinates = null
        imageCornerService.index = 0
    }

    private fun updateImageKeyLineData(cornerPoints: FloatArray) {
        // Total number of coordinates.
        val mPointsNum = cornerPoints.size / 4
        imageShaderPojo.run {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
            numPoints = mPointsNum
            var mvboSize = vboSize
            val numPoints = numPoints
            if (mvboSize < numPoints * Constants.BYTES_PER_POINT) {
                while (mvboSize < numPoints * Constants.BYTES_PER_POINT) {
                    // If the size of VBO is insufficient to accommodate the new vertex, resize the VBO.
                    mvboSize *= 2
                }
                GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mvboSize, null, GLES20.GL_DYNAMIC_DRAW)
            }
            val cornerPointBuffer = FloatBuffer.wrap(cornerPoints)
            GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, numPoints * Constants.BYTES_PER_POINT, cornerPointBuffer)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        }
    }

    /**
     * Draw the image border.
     *
     * @param viewProjectionMatrix View projection matrix.
     */
    private fun drawImageLine(viewProjectionMatrix: FloatArray) {
        checkGlError(TAG, "Draw image box start.")
        imageShaderPojo.run {
            GLES20.glUseProgram(program)
            GLES20.glEnableVertexAttribArray(position)
            GLES20.glEnableVertexAttribArray(color)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
            GLES20.glVertexAttribPointer(
                position, Constants.COORDINATE_DIMENSION, GLES20.GL_FLOAT, false, Constants.BYTES_PER_POINT, 0)

            // Set the line color to light green.
            GLES20.glUniform4f(color, 0.56f, 0.93f, 0.56f, 0.5f)
            GLES20.glUniformMatrix4fv(modelViewProjection, 1, false, viewProjectionMatrix, 0)

            // Set the width of a rendering stroke.
            GLES20.glLineWidth(5.0f)
            GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, numPoints)
            GLES20.glDisableVertexAttribArray(position)
            GLES20.glDisableVertexAttribArray(color)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        }
        checkGlError(TAG, "Draw image box end.")
    }
}
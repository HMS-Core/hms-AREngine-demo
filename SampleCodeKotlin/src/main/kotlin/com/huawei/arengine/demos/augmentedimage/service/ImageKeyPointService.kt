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
 * Draw the vertexes and center of the augmented image.
 *
 * @author HW
 * @since 2021-03-29
 */
class ImageKeyPointService : AugmentedImageComponentDisplay {
    companion object {
        private val TAG = "ImageKeyPointDisplay"
    }

    private lateinit var centerPointCoordinates: FloatArray

    private lateinit var allPointCoordinates: FloatArray

    private val imageCornerService by lazy { AugmentImageCorner() }

    private val imageShaderPojo by lazy { ImageShaderPojo() }

    /**
     * Create and build shaders for image keypoints on the OpenGL thread.
     */
    override fun init() {
        checkGlError(TAG, "Init start.")
        val buffers = IntArray(1)
        imageShaderPojo.run {
            GLES20.glGenBuffers(1, buffers, 0)
            vbo = buffers[0]
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
            vboSize = Constants.INITIAL_POINTS_SIZE * Constants.BYTES_PER_POINT
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vboSize, null, GLES20.GL_DYNAMIC_DRAW)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
            createProgram()
        }
        checkGlError(TAG, "Init end.")
    }

    private fun createProgram() {
        imageShaderPojo.run {
            program = createGlProgram(Constants.LP_VERTEX, Constants.LP_FRAGMENT)
            position = GLES20.glGetAttribLocation(program, "inPosition")
            color = GLES20.glGetUniformLocation(program, "inColor")
            pointSize = GLES20.glGetUniformLocation(program, "inPointSize")
            modelViewProjection = GLES20.glGetUniformLocation(program, "inMVPMatrix")
        }
        checkGlError(TAG, "Create program end.")
    }

    /**
     * Draw image key points to augment the image.
     *
     * @param augmentedImage Image to be augmented.
     * @param viewMatrix View matrix.
     * @param projectionMatrix Projection matrix.
     */
    override fun onDrawFrame(augmentedImage: ARAugmentedImage, viewMatrix: FloatArray, projectionMatrix: FloatArray) {
        val vpMatrix = FloatArray(Constants.BYTES_PER_POINT)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        draw(augmentedImage, vpMatrix)
    }

    private fun draw(augmentedImage: ARAugmentedImage, viewProjectionMatrix: FloatArray) {
        createImageCenterPoint(augmentedImage)
        updateImageAllPoints(centerPointCoordinates)
        imageCornerService.cornerPointCoordinates = FloatArray(Constants.BYTES_PER_CORNER * 4)
        CornerType.values().forEach {
            imageCornerService.createImageCorner(augmentedImage, it)
        }
        mergeArray(centerPointCoordinates, imageCornerService.cornerPointCoordinates!!)
        updateImageAllPoints(allPointCoordinates)
        drawImageKeyPoint(viewProjectionMatrix)
        imageCornerService.cornerPointCoordinates = null
        imageCornerService.index = 0
    }

    /**
     * Obtain the coordinates of the center of the recognized image and write the coordinates to the
     * centerPointCoordinates array.
     *
     * @param augmentedImage Augmented image object.
     */
    private fun createImageCenterPoint(augmentedImage: ARAugmentedImage) {
        centerPointCoordinates = FloatArray(4)
        val centerPose = augmentedImage.centerPose
        centerPointCoordinates[0] = centerPose.tx()
        centerPointCoordinates[1] = centerPose.ty()
        centerPointCoordinates[2] = centerPose.tz()
        centerPointCoordinates[3] = 1.0f
    }

    /**
     * Combine the obtained central coordinate array and vertex coordinate array into an allPointCoordinates array.
     *
     * @param centerCoordinates Center coordinate array.
     * @param cornerCoordinates Four-corner coordinate array.
     */
    private fun mergeArray(centerCoordinates: FloatArray, cornerCoordinates: FloatArray) {
        allPointCoordinates = FloatArray(centerCoordinates.size + cornerCoordinates.size)
        System.arraycopy(centerCoordinates, 0, allPointCoordinates, 0, centerCoordinates.size)
        System.arraycopy(cornerCoordinates, 0, allPointCoordinates, centerCoordinates.size, cornerCoordinates.size)
    }

    /**
     * Update the key point information of the augmented image.
     *
     * @param cornerPoints Array of key points of the augmented image, including the four vertexes and center.
     */
    private fun updateImageAllPoints(cornerPoints: FloatArray) {
        checkGlError(TAG, "Update image key point data start.")
        imageShaderPojo.run {
            val mPointsNum = cornerPoints.size / 4
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
            numPoints = mPointsNum
            var mvboSize = vboSize
            val numPoints = numPoints
            if (mvboSize < numPoints * Constants.BYTES_PER_POINT) {
                while (mvboSize < numPoints * Constants.BYTES_PER_POINT) {
                    //  If the size of VBO is insufficient to accommodate the new vertex, resize the VBO.
                    mvboSize *= 2
                }
                GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mvboSize, null, GLES20.GL_DYNAMIC_DRAW)
            }
            val cornerPointBuffer = FloatBuffer.wrap(cornerPoints)
            GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, numPoints * Constants.BYTES_PER_POINT,
                cornerPointBuffer)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        }
        checkGlError(TAG, "Update image key point data end.")
    }

    private fun drawImageKeyPoint(viewProjectionMatrix: FloatArray) {
        checkGlError(TAG, "Draw image key point start.")
        imageShaderPojo.run {
            GLES20.glUseProgram(program)
            GLES20.glEnableVertexAttribArray(position)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)

            // GLES20.GL_FLOAT occupies 4 bytes.
            GLES20.glVertexAttribPointer(
                position, 4, GLES20.GL_FLOAT, false, Constants.BYTES_PER_POINT, 0)

            // Set the color of key points in the image to yellow.
            GLES20.glUniform4f(color, 255.0f / 255.0f, 241.0f / 255.0f, 67.0f / 255.0f, 1.0f)
            GLES20.glUniformMatrix4fv(modelViewProjection, 1, false, viewProjectionMatrix, 0)

            // Set the size of the key points of the image.
            GLES20.glUniform1f(pointSize, 10.0f)
            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, numPoints)
            GLES20.glDisableVertexAttribArray(position)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        }
        checkGlError(TAG, "Draw image key point end.")
    }
}
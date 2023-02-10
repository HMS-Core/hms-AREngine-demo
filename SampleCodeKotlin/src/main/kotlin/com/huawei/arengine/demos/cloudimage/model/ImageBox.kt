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

package com.huawei.arengine.demos.cloudimage.model

import android.opengl.GLES20
import com.huawei.arengine.demos.cloudimage.common.CornerType
import com.huawei.hiar.ARAugmentedImage
import com.huawei.hiar.ARPose

import java.nio.FloatBuffer

/**
 * cloud image box augmented.
 *
 * @author HW
 * @since 2022-03-14
 */
class ImageBox(private val augmentedImage: ARAugmentedImage, private val shaderPojo: ShaderPojo) {
    private val BYTES_PER_POINT = 4 * 4

    private val BYTES_PER_CORNER = 4 * 3

    private val MATRIX_COLUMNS_FIRST = 1

    private val MATRIX_COLUMNS_SECOND = 2

    private val MATRIX_COLUMNS_THIRD = 3

    private val MATRIX_COLUMNS_FOURTH = 4

    private val POINT_SIZE = 10.0f

    private val COEFFICIENTS = floatArrayOf(0.5f, 0.5f, 0.5f, 0.35f, 0.35f, 0.5f)

    private var cornerPointCoordinates: FloatArray? = null

    private var index = 0

    /**
     * draw image box to augmented image.
     *
     * @param viewProjectionMatrix view Projection Matrix
     */
    fun draw(viewProjectionMatrix: FloatArray) {
        cornerPointCoordinates = FloatArray(BYTES_PER_CORNER * MATRIX_COLUMNS_FOURTH)
        for (cornerType in CornerType.values()) {
            createImageBoxCorner(cornerType)
        }
        updateImageBoxCornerPoints(cornerPointCoordinates!!)
        drawImageBox(viewProjectionMatrix)
        cornerPointCoordinates = null
        index = 0
    }

    private fun drawImageBox(viewProjectionMatrix: FloatArray?) {
        GLES20.glUseProgram(shaderPojo!!.getProgram())
        GLES20.glEnableVertexAttribArray(shaderPojo!!.getPosition())
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, shaderPojo!!.getVbo())

        // The size of the vertex attribute is 4, and each vertex has four coordinate components
        GLES20.glVertexAttribPointer(
            shaderPojo!!.getPosition(), MATRIX_COLUMNS_FOURTH, GLES20.GL_FLOAT,
            false, BYTES_PER_POINT, 0
        )

        // Set the color of the skeleton points to blue.
        GLES20.glUniform4f(shaderPojo!!.getColor(), 0.56f, 0.93f, 0.56f, 0.5f)
        GLES20.glUniformMatrix4fv(shaderPojo!!.getMvpMatrix(), 1, false, viewProjectionMatrix, 0)

        // Set the size of the skeleton points.
        GLES20.glUniform1f(shaderPojo!!.getPointSize(), POINT_SIZE)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, shaderPojo!!.getNumPoints())
        GLES20.glDisableVertexAttribArray(shaderPojo!!.getPosition())
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    private fun createImageBoxCorner(cornerType: CornerType) {
        val coefficient = FloatArray(COEFFICIENTS.size)
        when (cornerType) {
            CornerType.LOWER_RIGHT ->
                // generate point coordinates coefficent
                generateCoefficient(coefficient, MATRIX_COLUMNS_FIRST, MATRIX_COLUMNS_FIRST)
            CornerType.UPPER_LEFT -> generateCoefficient(
                coefficient, -MATRIX_COLUMNS_FIRST, -MATRIX_COLUMNS_FIRST
            )
            CornerType.UPPER_RIGHT -> generateCoefficient(
                coefficient, MATRIX_COLUMNS_FIRST, -MATRIX_COLUMNS_FIRST
            )
            CornerType.LOWER_LEFT -> generateCoefficient(
                coefficient, -MATRIX_COLUMNS_FIRST, MATRIX_COLUMNS_FIRST
            )
        }
        val localBoundaryPoses = arrayOfNulls<ARPose>(MATRIX_COLUMNS_THIRD)
        for (i in localBoundaryPoses.indices) {
            localBoundaryPoses[i] = ARPose.makeTranslation(
                coefficient[i * MATRIX_COLUMNS_SECOND] * augmentedImage.extentX, 0.0f,
                coefficient[i * MATRIX_COLUMNS_SECOND + MATRIX_COLUMNS_FIRST] * augmentedImage.extentZ
            )
        }

        val centerPose = augmentedImage.getCenterPose()
        val composeCenterPose = arrayOfNulls<ARPose>(localBoundaryPoses.size)
        val cornerCoordinatePos = index * BYTES_PER_CORNER
        for (i in composeCenterPose.indices) {
            composeCenterPose[i] = centerPose.compose(localBoundaryPoses[i])
            cornerPointCoordinates?.let {
                it[cornerCoordinatePos + i * MATRIX_COLUMNS_FOURTH] = composeCenterPose[i]!!.tx()
                it[cornerCoordinatePos + i * MATRIX_COLUMNS_FOURTH + MATRIX_COLUMNS_FIRST] =
                    composeCenterPose[i]!!.ty()
                it[cornerCoordinatePos + i * MATRIX_COLUMNS_FOURTH + MATRIX_COLUMNS_SECOND] =
                    composeCenterPose[i]!!.tz()
                it[cornerCoordinatePos + i * MATRIX_COLUMNS_FOURTH + MATRIX_COLUMNS_THIRD] = 1.0f
            }
        }
        index++
    }

    private fun generateCoefficient(coefficient: FloatArray, coefficientX: Int, coefficientZ: Int) {
        for (i in coefficient.indices step MATRIX_COLUMNS_SECOND) {
            coefficient[i] = coefficientX * COEFFICIENTS[i]
            coefficient[i + MATRIX_COLUMNS_FIRST] = coefficientZ * COEFFICIENTS[i + MATRIX_COLUMNS_FIRST]
        }
    }

    /**
     * Update the coordinates of cloud image 4 corner points.
     *
     * @param cornerPoints 4 corner points of the image
     */
    private fun updateImageBoxCornerPoints(cornerPoints: FloatArray) {
        val mPointsNum = cornerPoints.size / MATRIX_COLUMNS_FOURTH
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, shaderPojo.getVbo())
        shaderPojo.setNumPoints(mPointsNum)
        var vboSize = shaderPojo.getVboSize()
        val numPoints = shaderPojo.getNumPoints()
        if (vboSize < shaderPojo.getNumPoints() * BYTES_PER_POINT) {
            while (vboSize < numPoints * BYTES_PER_POINT) {
                // If the size of VBO is insufficient to accommodate the new point cloud, resize the VBO.
                vboSize *= MATRIX_COLUMNS_SECOND
            }
        }
        val cornerPointBuffer = FloatBuffer.wrap(cornerPoints)
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, numPoints * BYTES_PER_POINT,
            cornerPointBuffer)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }
}
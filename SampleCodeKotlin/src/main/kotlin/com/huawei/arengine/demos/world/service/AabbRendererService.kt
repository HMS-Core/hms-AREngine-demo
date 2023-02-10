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

package com.huawei.arengine.demos.world.service

import android.opengl.GLES20
import android.opengl.Matrix

import com.huawei.arengine.demos.common.LogUtil
import com.huawei.arengine.demos.common.util.checkGlError
import com.huawei.arengine.demos.world.controller.TargetRenderController
import com.huawei.hiar.ARTarget

import java.nio.FloatBuffer

import kotlin.math.abs

/**
 * The bounding box renderer that renders a bounding box of an object identified in target semantic recognition.
 *
 * @author HW
 * @since 2022-06-16
 */
class AabbRendererService() : TargetRenderController() {
    companion object {
        private const val TAG = "AabbRendererService"

        /**
         * Size of the initialized buffer.
         */
        private const val INITIAL_BUFFER_POINTS = 150;

        /**
         * Number of vertices in the quadrilateral.
         */
        private const val SQUARE_SIZE = 4
    }

    private var linePoints = FloatArray(SQUARE_SIZE * CUBE_POINT_NUM * FLOATS_PER_POINT * MAX_BOX_NUM)

    override fun getVboArraySize(): Int {
        return INITIAL_BUFFER_POINTS * BYTES_PER_POINT
    }

    override fun updateParameters(target: ARTarget) {
        arTarget = target
        var boxMatrix = FloatArray(MATRIX_SIZE)
        target.centerPose.toMatrix(boxMatrix, 0)
        var axisAlignBoundingBox = target.axisAlignBoundingBox
        if (axisAlignBoundingBox.size < FLOATS_PER_POINT + FLOATS_PER_POINT) {
            LogUtil.error(TAG, "axisAlignBoundingBox length invalid.")
            return
        }

        extentX = abs(axisAlignBoundingBox[OFFSET_X]) * LENGTH_MULTIPLE_NUM
        extentY = abs(axisAlignBoundingBox[OFFSET_Y]) * LENGTH_MULTIPLE_NUM
        extentZ = abs(axisAlignBoundingBox[OFFSET_Z]) * LENGTH_MULTIPLE_NUM

        var baseX = axisAlignBoundingBox[OFFSET_X]
        var baseY = 0.0f
        var baseZ = axisAlignBoundingBox[OFFSET_Z]
        var scaleY = floatArrayOf(W_VALUE, W_VALUE)
        var idx = 0
        var calcVertexes = FloatArray(SQUARE_SIZE * CUBE_POINT_NUM)
        var res = FloatArray(QUATERNION_SIZE)
        var tempArray: FloatArray

        for (value in scaleY) {
            baseY = value * axisAlignBoundingBox[OFFSET_Y]

            tempArray = floatArrayOf(baseX, baseY, -baseZ, W_VALUE)
            Matrix.multiplyMV(res, 0, boxMatrix, 0, tempArray, 0)
            numericalNormalization(idx, res, calcVertexes)
            idx += FLOATS_PER_POINT

            tempArray = floatArrayOf(baseX, baseY, baseZ, W_VALUE)
            Matrix.multiplyMV(res, 0, boxMatrix, 0, tempArray, 0)
            numericalNormalization(idx, res, calcVertexes)
            idx += FLOATS_PER_POINT

            tempArray = floatArrayOf(-baseX, baseY, baseZ, W_VALUE)
            Matrix.multiplyMV(res, 0, boxMatrix, 0, tempArray, 0)
            numericalNormalization(idx, res, calcVertexes)
            idx += FLOATS_PER_POINT

            tempArray = floatArrayOf(-baseX, baseY, -baseZ, W_VALUE)
            Matrix.multiplyMV(res, 0, boxMatrix, 0, tempArray, 0)
            numericalNormalization(idx, res, calcVertexes)
            idx += FLOATS_PER_POINT
        }
        vertices = calcVertexes.clone()
    }

    override fun updateVertices(vertices: FloatArray) {
        var idx = 0
        for (index in 0 until SQUARE_SIZE * FLOATS_PER_POINT step FLOATS_PER_POINT) {
            System.arraycopy(vertices, index, linePoints, idx, FLOATS_PER_POINT)
            idx += FLOATS_PER_POINT

            val endIdx: Int = (index + FLOATS_PER_POINT) % (SQUARE_SIZE * FLOATS_PER_POINT)
            System.arraycopy(vertices, endIdx, linePoints, idx, FLOATS_PER_POINT)
            idx += FLOATS_PER_POINT
        }
        for (index in SQUARE_SIZE * FLOATS_PER_POINT until SQUARE_SIZE * FLOATS_PER_POINT
            + SQUARE_SIZE * FLOATS_PER_POINT step FLOATS_PER_POINT) {
            System.arraycopy(vertices, index, linePoints, idx, FLOATS_PER_POINT)
            idx += FLOATS_PER_POINT

            val endIdx: Int =
                (index + FLOATS_PER_POINT) % (SQUARE_SIZE * FLOATS_PER_POINT) + SQUARE_SIZE * FLOATS_PER_POINT
            System.arraycopy(vertices, endIdx, linePoints, idx, FLOATS_PER_POINT)
            idx += FLOATS_PER_POINT
        }
        for (index in 0 until SQUARE_SIZE * FLOATS_PER_POINT step FLOATS_PER_POINT) {
            System.arraycopy(vertices, index, linePoints, idx, FLOATS_PER_POINT)
            idx += FLOATS_PER_POINT

            val endIdx: Int = index + SQUARE_SIZE * FLOATS_PER_POINT
            System.arraycopy(vertices, endIdx, linePoints, idx, FLOATS_PER_POINT)
            idx += FLOATS_PER_POINT
        }
        pointNum = SQUARE_SIZE * vertices.size
        checkGlError(TAG, "before update")
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
        if (vboSize < pointNum * BYTES_PER_POINT) {
            while (vboSize < pointNum * BYTES_PER_POINT) {
                vboSize *= VBO_SIZE_GROWTH_FACTOR
            }
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vboSize, null, GLES20.GL_DYNAMIC_DRAW)
        }
        val linePointsBuffer = FloatBuffer.wrap(linePoints)
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, pointNum * BYTES_PER_POINT, linePointsBuffer)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        checkGlError(TAG, "after update")
    }

    override fun getTargetInfo(): String {
        val width = extentX * M_TO_CM + LENGTH_BASE_VALUE
        val length = extentZ * M_TO_CM + LENGTH_BASE_VALUE
        val height = extentY * M_TO_CM + LENGTH_BASE_VALUE
        var labelInfo = getTargetLabelInfo()
        if (labelInfo.isEmpty()) {
            labelInfo = "BOX(cm)"
        }
        return labelInfo + System.lineSeparator() + width.toInt().toString() + "x" + length.toInt()
            .toString() + "x" + height.toInt().toString()
    }
}
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

package com.huawei.arengine.demos.world.controller

import android.opengl.GLES20
import android.opengl.Matrix

import com.huawei.arengine.demos.common.LogUtil
import com.huawei.arengine.demos.common.util.checkGlError
import com.huawei.arengine.demos.common.util.createGlProgram
import com.huawei.arengine.demos.world.util.Constants
import com.huawei.hiar.ARTarget

/**
 * Abstract class of the target semantic renderer.
 *
 * @author HW
 * @since 2022-06-15
 */
open abstract class TargetRenderController {
    companion object {
        private const val TAG = "TargetRenderController"

        /**
         * Offset of the X coordinate.
         */
        internal const val OFFSET_X = 0

        /**
         * Offset of the Y coordinate.
         */
        internal const val OFFSET_Y = 1

        /**
         * Offset of the Z coordinate.
         */
        internal const val OFFSET_Z = 2

        /**
         * Value of the float type.
         */
        private const val BYTES_PER_FLOAT = java.lang.Float.SIZE / 8

        /**
         * Number of coordinates of each vertex.
         */
        internal const val FLOATS_PER_POINT = 3

        /**
         * Memory size occupied by each vertex.
         */
        internal const val BYTES_PER_POINT: Int = BYTES_PER_FLOAT * FLOATS_PER_POINT

        /**
         * Maximum number of coordinates of the bounding box.
         */
        internal const val MAX_BOX_NUM = 100

        /**
         * Number of vertices for quadrilateral rendering.
         */
        internal const val CUBE_POINT_NUM = 6

        /**
         * Matrix size.
         */
        internal const val MATRIX_SIZE = 16

        /**
         * Quaternion size.
         */
        internal const val QUATERNION_SIZE = 4

        /**
         * The origin of the local coordinate system is in the center of the object. The actual length
         * needs to be multiplied by 2.
         */
        internal const val LENGTH_MULTIPLE_NUM = 2.0f

        /**
         * Double the VBO size if it is too small.
         */
        internal const val VBO_SIZE_GROWTH_FACTOR = 2

        /**
         * W component.
         */
        internal const val W_VALUE = 1.0f

        /**
         * The length is converted from meters to centimeters.
         */
        internal const val M_TO_CM = 100.0f

        /**
         * The length deviation is 0.5 cm.
         */
        internal const val LENGTH_BASE_VALUE = 0.5f

        internal const val LINE_WIDTH = 7.0f

        internal const val EPSINON = 0.000001f
    }

    /**
     * Length in the X direction.
     */
    protected var extentX = 0f

    /**
     * Length in the Y direction.
     */
    protected var extentY = 0f

    /**
     * Length in the Z direction.
     */
    protected var extentZ = 0f

    /**
     * Identified ARTarget object.
     */
    protected var arTarget: ARTarget? = null

    /**
     * Circle radius.
     */
    protected var radius = 0.0f

    /**
     * VBO storage location.
     */
    protected var vbo = 0

    /**
     * VBO size.
     */
    protected var vboSize = 0

    /**
     * Vertices information.
     */
    protected var vertices: FloatArray? = null

    /**
     * Number of vertices.
     */
    protected var pointNum = 0

    private var programName = 0

    private var positionAttribute = 0

    private var modelViewProjectionUniform = 0

    private var colorUniform = 0

    /**
     * Number of vertices.
     */
    open fun createOnGlThread() {
        checkGlError(TAG, "before create")
        val buffer = IntArray(1)
        GLES20.glGenBuffers(1, buffer, 0)
        vbo = buffer[0]
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
        vboSize = getVboArraySize()
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vboSize, null, GLES20.GL_DYNAMIC_DRAW)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        checkGlError(TAG, "buffer alloc")
        programName = createGlProgram(Constants.POINTCLOUD_VERTEX, Constants.POINTCLOUD_FRAGMENT)
        GLES20.glLinkProgram(programName)
        GLES20.glUseProgram(programName)
        checkGlError(TAG, "programName")
        positionAttribute = GLES20.glGetAttribLocation(programName, "a_Position")
        colorUniform = GLES20.glGetUniformLocation(programName, "u_Color")
        modelViewProjectionUniform = GLES20.glGetUniformLocation(programName, "u_ModelViewProjection");
        checkGlError(TAG, "buffer alloc")
    }

    open fun draw(cameraViewMatrix: FloatArray, projectionMatrix: FloatArray) {
        vertices?.let { updateVertices(it) }
        checkGlError(TAG, "Before draw")
        var modelViewProjections = FloatArray(MATRIX_SIZE)
        Matrix.multiplyMM(modelViewProjections, 0, projectionMatrix, 0, cameraViewMatrix, 0)
        GLES20.glUseProgram(programName)
        GLES20.glEnableVertexAttribArray(positionAttribute)
        GLES20.glEnableVertexAttribArray(colorUniform)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
        GLES20.glLineWidth(LINE_WIDTH)
        GLES20.glVertexAttribPointer(positionAttribute, QUATERNION_SIZE, GLES20.GL_FLOAT, false, BYTES_PER_POINT, 0)

        // Set the line color.
        GLES20.glUniform4f(colorUniform, 10.0f / 255.0f, 89.0f / 255.0f, 247.0f / 255.0f, 1.0f)
        GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, modelViewProjections, 0)

        GLES20.glDrawArrays(GLES20.GL_LINES, 0, pointNum)
        GLES20.glDisableVertexAttribArray(positionAttribute)
        GLES20.glDisableVertexAttribArray(colorUniform)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        checkGlError(TAG, "Draw")
    }

    open fun numericalNormalization(startIndex: Int, res: FloatArray, fa: FloatArray) {
        for (index in 0 until res.size - 1) {
            if (fa.size <= startIndex + index || res.size <= index) {
                LogUtil.warn(TAG, "numericalNormalization index invalid.")
                return
            }
            if (Math.abs(res[res.size - 1]) <= EPSINON) {
                LogUtil.warn(TAG, "numericalNormalization res value invalid.")
                return
            }
            fa[startIndex + index] = res[index] / res[res.size - 1]
        }
    }

    open fun getTargetLabelInfo(): String {
        return if (arTarget == null) {
            ""
        } else when (arTarget!!.label) {
            ARTarget.TargetLabel.TARGET_SEAT -> "SEAT"
            ARTarget.TargetLabel.TARGET_TABLE -> "TABLE"
            else -> ""
        }
    }

    /**
     * Obtain the VBO size.
     *
     * @return VBO size.
     */
    abstract fun getVboArraySize(): Int

    /**
     * Parameters used for rendering updates.
     *
     * @param target Recognized target object.
     */
    abstract fun updateParameters(target: ARTarget)

    /**
     * Update the vertices information.
     *
     * @param vertices Vertices information.
     */
    abstract fun updateVertices(vertices: FloatArray)

    /**
     * Obtain the calculated target length, width, height, and label information.
     *
     * @return Calculated target length, width, height, and label information.
     */
    abstract fun getTargetInfo(): String
}
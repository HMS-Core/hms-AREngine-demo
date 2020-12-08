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
import com.huawei.arengine.demos.hand.util.Constants.BYTES_PER_POINT
import com.huawei.arengine.demos.hand.util.Constants.COORDINATE_DIMENSION_3D
import com.huawei.arengine.demos.hand.util.Constants.POINT_NUM_LINE
import com.huawei.hiar.ARHand
import java.nio.FloatBuffer

/**
 * Draw hand skeleton connection line based on the coordinates of the hand skeleton points..
 *
 * @author HW
 * @since 2020-10-10
 */
class HandSkeletonLineService : HandRenderService() {
    companion object {
        private const val TAG = "HandSkeletonLineService"
    }

    /**
     * Draw hand skeleton connection line.
     * This method is called when [HandRenderController.OnDrawFrame].
     *
     * @param hands ARHand data collection.
     * @param projectionMatrix ProjectionMatrix(4 * 4).
     */
    override fun renderHand(hands: Collection<ARHand>, projectionMatrix: FloatArray) {
        if (hands.isEmpty()) {
            return
        }

        for (hand in hands) {
            val handSkeletons = hand.handskeletonArray
            val handSkeletonConnections = hand.handSkeletonConnection
            if (handSkeletons.isEmpty() || handSkeletonConnections.isEmpty()) {
                continue
            }
            updateHandSkeletonLinesData(handSkeletons, handSkeletonConnections)
            drawHandSkeletonLine(projectionMatrix)
        }
    }

    /**
     * This method updates the connection data of skeleton points and is called when any frame is updated.
     *
     * @param handSkeletons Bone point data of hand.
     * @param handSkeletonConnection Data of connection between bone points of hand.
     */
    private fun updateHandSkeletonLinesData(handSkeletons: FloatArray, handSkeletonConnection: IntArray) {
        checkGlError(TAG, "Update hand skeleton lines data start.")
        // Each point is a set of 3D coordinate. Each connection line consists of two points.
        val linePoints = FloatArray(handSkeletonConnection.size * COORDINATE_DIMENSION_3D * POINT_NUM_LINE)

        /*
         * The format of HandSkeletonConnection data is [p0,p1;p0,p3;p0,p5;p1,p2].
         * handSkeletonConnection saves the node indexes. Two indexes obtain a set of connection point data.
         * Therefore, j = j + 2. This loop obtains related coordinates and saves them in linePoint.
         */
        for (i in 0..handSkeletonConnection.size step 2) {
            for (j in 0..2) {
                linePoints[i * 3 + j] = handSkeletons[3 * handSkeletonConnection[i] + j]
                linePoints[i * 3 + j + 3] = handSkeletons[3 * handSkeletonConnection[i + 1] + j]
            }
        }

        shaderPojo.run {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
            pointNum = handSkeletonConnection.size * 2
            val bytePoints = pointNum * BYTES_PER_POINT

            // If the storage space is insufficient, apply for twice the memory each time.
            if (vboSize < bytePoints) {
                while (vboSize < bytePoints) {
                    vboSize *= 2
                }
                GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vboSize, null, GLES20.GL_DYNAMIC_DRAW)
            }
            GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, pointNum * BYTES_PER_POINT,
                FloatBuffer.wrap(linePoints))
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        }
        checkGlError(TAG, "Update hand skeleton lines data end.")
    }

    /**
     * Draw hand skeleton connection line.
     *
     * @param projectionMatrix Projection matrix(4 * 4).
     */
    private fun drawHandSkeletonLine(projectionMatrix: FloatArray) {
        checkGlError(TAG, "Draw hand skeleton line start.")
        shaderPojo.run {
            GLES20.glUseProgram(program)
            GLES20.glEnableVertexAttribArray(position)
            GLES20.glEnableVertexAttribArray(color)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)

            // Set the width of the drawn line
            GLES20.glLineWidth(18.0f)

            // Represented each point by 4D coordinates in the shader.
            GLES20.glVertexAttribPointer(
                position, 4, GLES20.GL_FLOAT, false, BYTES_PER_POINT, 0)
            GLES20.glUniform4f(color, 0.0f, 0.0f, 0.0f, 1.0f)
            GLES20.glUniformMatrix4fv(modelViewProjectionMatrix, 1, false, projectionMatrix, 0)
            GLES20.glUniform1f(pointSize, 100f)
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, pointNum)
            GLES20.glDisableVertexAttribArray(position)
            GLES20.glDisableVertexAttribArray(color)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        }
        checkGlError(TAG, "Draw hand skeleton line end.")
    }
}
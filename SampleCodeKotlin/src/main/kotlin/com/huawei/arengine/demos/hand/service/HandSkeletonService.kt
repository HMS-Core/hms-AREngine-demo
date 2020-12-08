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
import android.util.Log
import com.huawei.arengine.demos.common.util.checkGlError
import com.huawei.arengine.demos.hand.util.Constants.BYTES_PER_POINT
import com.huawei.arengine.demos.hand.util.Constants.COORDINATE_DIMENSION_3D
import com.huawei.hiar.ARHand
import java.nio.FloatBuffer

/**
 * Draw hand skeleton points based on the coordinates of the hand skeleton points.
 *
 * @author HW
 * @since 2020-10-10
 */
class HandSkeletonService : HandRenderService() {
    companion object {
        private const val TAG = "HandSkeletonService"
    }

    /**
     * Draw hand skeleton points. This method is called when [HandRenderController.OnDrawFrame].
     *
     * @param hands ARHand data collection.
     * @param projectionMatrix Projection matrix(4 * 4).
     */
    override fun renderHand(hands: Collection<ARHand>, projectionMatrix: FloatArray) {
        if (hands.isEmpty()) {
            return
        }
        for (hand in hands) {
            val handSkeletons = hand.handskeletonArray
            if (handSkeletons.isEmpty()) {
                continue
            }
            updateHandSkeletonsData(handSkeletons)
            drawHandSkeletons(projectionMatrix)
        }
    }

    /**
     * Update the coordinates of hand skeleton points.
     *
     * @param handSkeletons hand Skeletons data
     */
    private fun updateHandSkeletonsData(handSkeletons: FloatArray) {
        checkGlError(TAG, "Update hand skeletons data start.")
        /*
         * Each point has a 3D coordinate. The total number of coordinates
         * is three times the number of skeleton points.
         */
        val skeletonPointsNum = handSkeletons.size / COORDINATE_DIMENSION_3D
        Log.d(TAG, "ARHand HandSkeletonNumber = $skeletonPointsNum")
        shaderPojo.run {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
            pointNum = skeletonPointsNum
            val bytePoints = pointNum * BYTES_PER_POINT
            if (vboSize < bytePoints) {
                while (vboSize < bytePoints) {
                    // If the size of VBO is insufficient to accommodate the new point cloud, resize the VBO.
                    vboSize *= 2
                }
                GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vboSize, null, GLES20.GL_DYNAMIC_DRAW)
            }
            GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, bytePoints,
                FloatBuffer.wrap(handSkeletons))
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        }
        checkGlError(TAG, "Update hand skeletons data end.")
    }

    /**
     * Draw hand skeleton points.
     *
     * @param projectionMatrix Projection matrix.
     */
    private fun drawHandSkeletons(projectionMatrix: FloatArray) {
        checkGlError(TAG, "Draw hand skeletons start.")
        shaderPojo.run {
            GLES20.glUseProgram(program)
            GLES20.glEnableVertexAttribArray(position)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)

            // The size of the vertex attribute is 4, and each vertex has four coordinate components
            GLES20.glVertexAttribPointer(
                position, 4, GLES20.GL_FLOAT, false, BYTES_PER_POINT, 0)

            // Set the color of the skeleton points to blue.
            GLES20.glUniform4f(color, 0.0f, 0.0f, 1.0f, 1.0f)
            GLES20.glUniformMatrix4fv(modelViewProjectionMatrix, 1, false, projectionMatrix, 0)

            // Set the size of the skeleton points.
            GLES20.glUniform1f(pointSize, 30.0f)
            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, pointNum)
            GLES20.glDisableVertexAttribArray(position)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        }
        checkGlError(TAG, "Draw hand skeletons end.")
    }
}
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
import com.huawei.arengine.demos.common.util.MatrixUtil.originalMatrix
import com.huawei.arengine.demos.common.util.checkGlError
import com.huawei.arengine.demos.hand.util.Constants.BYTES_PER_POINT
import com.huawei.arengine.demos.hand.util.Constants.COORDINATE_DIMENSION_3D
import com.huawei.hiar.ARHand
import com.huawei.hiar.ARTrackable
import java.nio.FloatBuffer

/**
 * This class shows how to use the hand bounding box. With this class,
 * a rectangular box bounding the hand can be displayed on the screen.
 *
 * @author HW
 * @since 2020-10-10
 */
class HandBoxService : HandRenderService() {
    companion object {
        private const val TAG = "HandBoxService"
    }

    private val mMVPMatrix by lazy { originalMatrix }

    /**
     * Render the hand bounding box and hand information.
     * This method is called when [HandRenderController.OnDrawFrame].
     *
     * @param hands Hand data.
     * @param projectionMatrix ARCamera projection matrix.
     */
    override fun renderHand(hands: Collection<ARHand>, projectionMatrix: FloatArray) {
        if (hands.isEmpty()) {
            return
        }
        for (hand in hands) {
            if (hand.trackingState == ARTrackable.TrackingState.TRACKING) {
                updateHandBoxData(hand.gestureHandBox)
                drawHandBox()
            }
        }
    }

    /**
     * Update the coordinates of the hand bounding box.
     *
     * @param handBoxPoints Gesture hand box data.
     */
    private fun updateHandBoxData(handBoxPoints: FloatArray) {
        checkGlError(TAG, "Update hand box data start.")

        // Get the four coordinates of a rectangular box bounding the hand.
        val glHandBoxPoints = floatArrayOf(
            handBoxPoints[0], handBoxPoints[1], handBoxPoints[2],
            handBoxPoints[3], handBoxPoints[1], handBoxPoints[2],
            handBoxPoints[3], handBoxPoints[4], handBoxPoints[5],
            handBoxPoints[0], handBoxPoints[4], handBoxPoints[5])
        val handBoxPointsNum = glHandBoxPoints.size / COORDINATE_DIMENSION_3D

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, shaderPojo.vbo)
        shaderPojo.run {
            pointNum = handBoxPointsNum
            val bytePoints = pointNum * BYTES_PER_POINT
            if (vboSize < bytePoints) {
                while (vboSize < bytePoints) {
                    // If the size of VBO is insufficient to accommodate the new point cloud, resize the VBO.
                    vboSize *= 2
                }
                GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vboSize, null, GLES20.GL_DYNAMIC_DRAW)
            }
            GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, bytePoints,
                FloatBuffer.wrap(glHandBoxPoints))
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        }
        checkGlError(TAG, "Update hand box data end.")
    }

    /**
     * Render the hand bounding box.
     */
    private fun drawHandBox() {
        checkGlError(TAG, "Draw hand box start.")
        shaderPojo.run {
            GLES20.glUseProgram(program)
            GLES20.glEnableVertexAttribArray(position)
            GLES20.glEnableVertexAttribArray(color)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
            GLES20.glVertexAttribPointer(
                position, COORDINATE_DIMENSION_3D, GLES20.GL_FLOAT, false, BYTES_PER_POINT, 0)
            GLES20.glUniform4f(color, 1.0f, 0.0f, 0.0f, 1.0f)
            GLES20.glUniformMatrix4fv(modelViewProjectionMatrix, 1, false, mMVPMatrix, 0)

            // Set the size of the rendering vertex.
            GLES20.glUniform1f(pointSize, 50.0f)

            // Set the width of a rendering stroke.
            GLES20.glLineWidth(18.0f)
            GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, pointNum)
            GLES20.glDisableVertexAttribArray(position)
            GLES20.glDisableVertexAttribArray(color)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        }
        checkGlError(TAG, "Draw hand box end.")
    }
}
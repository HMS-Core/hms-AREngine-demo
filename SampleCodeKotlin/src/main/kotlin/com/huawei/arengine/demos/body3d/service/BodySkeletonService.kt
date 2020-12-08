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
import com.huawei.arengine.demos.body3d.util.Constants
import com.huawei.arengine.demos.common.util.checkGlError
import com.huawei.hiar.ARBody
import com.huawei.hiar.ARCoordinateSystemType
import com.huawei.hiar.ARTrackable
import java.nio.FloatBuffer

/**
 * Obtain and pass the skeleton data to openGL ES, which will render the data and displays it on the screen.
 *
 * @author HW
 * @since 2020-10-10
 */
class BodySkeletonService : BodyRenderService() {
    companion object {
        private const val TAG = "BodySkeletonService"
    }

    private var currentPointNum = 0

    private var lastPointNum = 0

    private lateinit var skeletonPoints: FloatBuffer

    /**
     * Update the node data and draw by using OpenGL.
     * This method is called when [BodyRenderController.onDrawFrame].
     *
     * @param bodies Body data.
     * @param projectionMatrix projection matrix.
     */
    override fun renderBody(bodies: Collection<ARBody>, projectionMatrix: FloatArray) {
        for (body in bodies) {
            if (body.trackingState != ARTrackable.TrackingState.TRACKING) {
                continue
            }
            val coordinate = if (
                body.coordinateSystemType == ARCoordinateSystemType.COORDINATE_SYSTEM_TYPE_3D_CAMERA) {
                Constants.COORDINATE_SYSTEM_TYPE_3D_FLAG
            } else {
                1.0f
            }

            findValidSkeletonPoints(body)
            updateBodySkeleton()
            drawBodySkeleton(coordinate, projectionMatrix)
        }
    }

    private fun findValidSkeletonPoints(arBody: ARBody) {
        var index = 0
        val isExists: IntArray
        var validPointNum = 0
        val points: FloatArray
        val skeletonPoints: FloatArray

        /*
         * Determine whether the data returned by the algorithm is 3D human skeleton data
         * or 2D human skeleton data, and obtain valid skeleton points.
         */
        if (arBody.coordinateSystemType == ARCoordinateSystemType.COORDINATE_SYSTEM_TYPE_3D_CAMERA) {
            isExists = arBody.skeletonPointIsExist3D
            points = FloatArray(isExists.size * 3)
            skeletonPoints = arBody.skeletonPoint3D
        } else {
            isExists = arBody.skeletonPointIsExist2D
            points = FloatArray(isExists.size * 3)
            skeletonPoints = arBody.skeletonPoint2D
        }

        // Save the three coordinates of each joint point(each point has three coordinates).
        for (i in isExists.indices) {
            if (isExists[i] != 0) {
                points[index++] = skeletonPoints[3 * i]
                points[index++] = skeletonPoints[3 * i + 1]
                points[index++] = skeletonPoints[3 * i + 2]
                validPointNum++
            }
        }
        this.skeletonPoints = FloatBuffer.wrap(points)
        lastPointNum = validPointNum
    }

    private fun updateBodySkeleton() {
        checkGlError(TAG, "Update Body Skeleton data start.")
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, shaderPojo.vbo)
        currentPointNum = lastPointNum
        shaderPojo.run {
            val bytePoints = currentPointNum * Constants.BYTES_PER_POINT
            if (vboSize < bytePoints) {
                while (vboSize < bytePoints) {
                    // If the size of VBO is insufficient to accommodate the new point cloud, resize the VBO.
                    vboSize *= 2
                }
                GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vboSize, null, GLES20.GL_DYNAMIC_DRAW)
            }
            GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, bytePoints, skeletonPoints)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        }
        checkGlError(TAG, "Update Body Skeleton data end.")
    }

    private fun drawBodySkeleton(coordinate: Float, projectionMatrix: FloatArray) {
        checkGlError(TAG, "Draw body skeleton start.")
        shaderPojo.run {
            GLES20.glUseProgram(program)
            GLES20.glEnableVertexAttribArray(position)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)

            // The size of the vertex attribute is 4, and each vertex has four coordinate components.
            GLES20.glVertexAttribPointer(
                position, 4, GLES20.GL_FLOAT, false, Constants.BYTES_PER_POINT, 0)
            GLES20.glUniform4f(color, 0.0f, 0.0f, 1.0f, 1.0f)
            GLES20.glUniformMatrix4fv(this.projectionMatrix, 1, false, projectionMatrix, 0)

            // Set the size of the skeleton points.
            GLES20.glUniform1f(pointSize, 30.0f)
            GLES20.glUniform1f(coordinateSystem, coordinate)
            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, currentPointNum)
            GLES20.glDisableVertexAttribArray(position)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        }
        checkGlError(TAG, "Draw body skeleton end.")
    }
}
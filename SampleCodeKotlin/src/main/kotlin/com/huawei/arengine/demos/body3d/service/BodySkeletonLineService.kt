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
 * Gets the skeleton point connection data and pass it to OpenGL ES for rendering on the screen.
 *
 * @author HW
 * @since 2020-10-10
 */
class BodySkeletonLineService : BodyRenderService() {
    companion object {
        private const val TAG = "BodySkeletonLineService"
    }

    private var pointNum = 0

    private var pointsLineNum = 0

    private var linePoints: FloatBuffer? = null

    /**
     * Rendering lines between body bones.
     * This method is called when [BodyRenderController.onDrawFrame].
     *
     * @param bodies Bodies data.
     * @param projectionMatrix Projection matrix.
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
            findValidConnectionSkeletonLines(body)
            updateBodySkeletonLineData()
            drawSkeletonLine(coordinate, projectionMatrix)
        }
    }

    private fun findValidConnectionSkeletonLines(arBody: ARBody) {
        pointsLineNum = 0
        val connections = arBody.bodySkeletonConnection
        val linePoints = FloatArray(Constants.LINE_POINT_RATIO * connections.size)
        val coors: FloatArray
        val isExists: IntArray
        if (arBody.coordinateSystemType == ARCoordinateSystemType.COORDINATE_SYSTEM_TYPE_3D_CAMERA) {
            coors = arBody.skeletonPoint3D
            isExists = arBody.skeletonPointIsExist3D
        } else {
            coors = arBody.skeletonPoint2D
            isExists = arBody.skeletonPointIsExist2D
        }

        /*
         * Filter out valid skeleton connection lines based on the returned results,
         * which consist of indexes of two ends, for example, [p0,p1;p0,p3;p0,p5;p1,p2]. The loop takes
         * out the 3D coordinates of the end points of the valid connection line and saves them in sequence.
         */
        for (i in connections.indices step 2) {
            if (isExists[connections[i]] != 0 && isExists[connections[i + 1]] != 0) {
                linePoints[pointsLineNum * 3] = coors[3 * connections[i]]
                linePoints[pointsLineNum * 3 + 1] = coors[3 * connections[i] + 1]
                linePoints[pointsLineNum * 3 + 2] = coors[3 * connections[i] + 2]
                linePoints[pointsLineNum * 3 + 3] = coors[3 * connections[i + 1]]
                linePoints[pointsLineNum * 3 + 4] = coors[3 * connections[i + 1] + 1]
                linePoints[pointsLineNum * 3 + 5] = coors[3 * connections[i + 1] + 2]
                pointsLineNum += 2
            }
        }
        this.linePoints = FloatBuffer.wrap(linePoints)
    }

    /**
     * Update body connection data.
     */
    private fun updateBodySkeletonLineData() {
        checkGlError(TAG, "Update body skeleton line data start.")
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, shaderPojo.vbo)
        pointNum = pointsLineNum
        shaderPojo.run {
            val bytePoints = pointNum * Constants.BYTES_PER_POINT
            if (vboSize < bytePoints) {
                while (vboSize < bytePoints) {
                    // If the storage space is insufficient, allocate double the space.
                    vboSize *= 2
                }
                GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vboSize, null, GLES20.GL_DYNAMIC_DRAW)
            }
            GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, bytePoints, linePoints)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        }
        checkGlError(TAG, "Update body skeleton line data end.")
    }

    private fun drawSkeletonLine(coordinate: Float, projectionMatrix: FloatArray) {
        checkGlError(TAG, "Draw skeleton line start.")
        shaderPojo.run {
            GLES20.glUseProgram(program)
            GLES20.glEnableVertexAttribArray(position)
            GLES20.glEnableVertexAttribArray(color)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)

            // Set the width of the rendered skeleton line.
            GLES20.glLineWidth(18.0f)

            // The size of the vertex attribute is 4, and each vertex has four coordinate components.
            GLES20.glVertexAttribPointer(
                position, 4, GLES20.GL_FLOAT, false, Constants.BYTES_PER_POINT, 0)
            GLES20.glUniform4f(color, 1.0f, 0.0f, 0.0f, 1.0f)
            GLES20.glUniformMatrix4fv(this.projectionMatrix, 1, false, projectionMatrix, 0)

            // Set the size of the points.
            GLES20.glUniform1f(pointSize, 100.0f)
            GLES20.glUniform1f(coordinateSystem, coordinate)
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, pointNum)
            GLES20.glDisableVertexAttribArray(position)
            GLES20.glDisableVertexAttribArray(color)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        }
        checkGlError(TAG, "Draw skeleton line end.")
    }
}
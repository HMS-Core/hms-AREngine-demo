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
import com.huawei.arengine.demos.world.util.Constants
import com.huawei.arengine.demos.common.util.checkGlError
import com.huawei.arengine.demos.common.util.createGlProgram
import com.huawei.arengine.demos.world.pojo.PointPojo
import com.huawei.hiar.ARPointCloud

/**
 * Point cloud rendering class, including creating shader to update point cloud data and rendering.
 *
 * @author HW
 * @since 2021-04-06
 */
class PointService {
    companion object {
        private const val TAG = "PointCloudRenderer"
    }

    private val pointPojo by lazy { PointPojo() }

    /**
     * Allocates and initializes OpenGL resources needed by the plane renderer. Must be
     * called on the OpenGL thread, typically in
     */
    fun init() {
        pointPojo.run {
            val buffers = IntArray(1)
            GLES20.glGenBuffers(1, buffers, 0)
            mPointBuffer = buffers[0]
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mPointBuffer)
            mPointBufferSize = Constants.INITIAL_BUFFER_POINT_SIZE * Constants.BYTES_POINT
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mPointBufferSize, null, GLES20.GL_DYNAMIC_DRAW)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
            mProgramName = createGlProgram(Constants.POINTCLOUD_VERTEX, Constants.POINTCLOUD_FRAGMENT)
            GLES20.glUseProgram(mProgramName)
            mPositionAttribute = GLES20.glGetAttribLocation(mProgramName, "a_Position")
            mColorUniform = GLES20.glGetUniformLocation(mProgramName, "u_Color")
            mViewProjectionUniform = GLES20.glGetUniformLocation(mProgramName, "u_ModelViewProjection")
            mPointUniform = GLES20.glGetUniformLocation(mProgramName, "u_PointSize")
            checkGlError(TAG, "init success")
        }
    }

    /**
     * Update point cloud data in buffer and setting up input data in shader program and drawing, when draw the point.
     *
     * @param cloud Data types defined by HW(ARPointCloud).
     * @param cameraView Camera view data.
     * @param cameraPerspective Camera perspective data.
     */
    fun renderPoints(cloud: ARPointCloud, cameraView: FloatArray?, cameraPerspective: FloatArray?) {
        pointPojo.run {
            mPointCloud.let {
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mPointBuffer)
                mPointCloud = cloud
                mNumPoints = mPointCloud!!.points.remaining() / Constants.FLOATS_POINT
                if (mPointBufferSize < mNumPoints * Constants.BYTES_POINT) {
                    while (mPointBufferSize < mNumPoints * Constants.BYTES_POINT) {
                        mPointBufferSize *= 2 // If vertice VBO size is not big enough ,double it.
                    }
                    GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mPointBufferSize, null, GLES20.GL_DYNAMIC_DRAW)
                }
                GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, mNumPoints * Constants.BYTES_POINT, mPointCloud!!.points)
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
                val modelViewProjection = FloatArray(16)
                Matrix.multiplyMM(modelViewProjection, 0, cameraPerspective, 0, cameraView, 0)
                GLES20.glUseProgram(mProgramName)
                GLES20.glEnableVertexAttribArray(mPositionAttribute)
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mPointBuffer)
                GLES20.glVertexAttribPointer(mPositionAttribute, Constants.POSITION_COMPONENTS_NUMBERS, GLES20.GL_FLOAT, false,
                        Constants.BYTES_POINT, 0)
                GLES20.glUniform4f(mColorUniform, 255.0f / 255.0f, 241.0f / 255.0f, 67.0f / 255.0f, 1.0f)
                GLES20.glUniformMatrix4fv(mViewProjectionUniform, 1, false, modelViewProjection, 0)
                GLES20.glUniform1f(mPointUniform, 10.0f) // Set the size of Point to 10.
                GLES20.glDrawArrays(GLES20.GL_POINTS, 0, mNumPoints)
                GLES20.glDisableVertexAttribArray(mPositionAttribute)
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
            }
        }
    }
}
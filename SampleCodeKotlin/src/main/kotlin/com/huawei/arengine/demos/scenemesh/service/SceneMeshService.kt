/**
 * Copyright 2021. Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huawei.arengine.demos.scenemesh.service

import android.opengl.GLES20
import android.opengl.Matrix
import com.huawei.arengine.demos.common.LogUtil.debug
import com.huawei.arengine.demos.common.util.checkGlError
import com.huawei.arengine.demos.common.util.createGlProgram
import com.huawei.arengine.demos.scenemesh.pojo.SceneMeshPojo
import com.huawei.arengine.demos.scenemesh.util.Constants
import com.huawei.hiar.ARFrame
import com.huawei.hiar.ARSceneMesh
import javax.microedition.khronos.opengles.GL10

/**
 * Renders the scene grid, and creates the shader for updating grid data and performing rendering.
 *
 * @author hw
 * @since 2021-04-21
 */
class SceneMeshService {
    companion object {
        private const val TAG = "SceneMeshService"
    }

    private val mModelViewProjection: FloatArray = FloatArray(Constants.MODLE_VIEW_PROJ_SIZE)

    private val mSceneMeshPojo by lazy { SceneMeshPojo() }

    fun init() {
        mSceneMeshPojo.run {
            val buffers = IntArray(Constants.BUFFER_OBJECT_NUMBER)
            GLES20.glGenBuffers(Constants.BUFFER_OBJECT_NUMBER, buffers, 0)
            mVerticeVBO = buffers[0]
            mTriangleVBO = buffers[1]

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVerticeVBO)
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVerticeVBOSize * Constants.BYTES_PER_POINT, null, GLES20.GL_DYNAMIC_DRAW)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mTriangleVBO)
            GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, mTriangleVBOSize * Constants.BYTES_PER_FLOAT, null,
                GLES20.GL_DYNAMIC_DRAW)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
            checkGlError(TAG, "buffer alloc")
            mProgram = createGlProgram(Constants.SCENE_MESH_VERTEX, Constants.SCENE_MESH_FRAGMENT)
            GLES20.glUseProgram(mProgram)
            checkGlError(TAG, "program")
            mPositionAttribute = GLES20.glGetAttribLocation(mProgram, "a_Position")
            mColorUniform = GLES20.glGetUniformLocation(mProgram, "u_Color")
            mModelViewProjectionUniform = GLES20.glGetUniformLocation(mProgram, "u_ModelViewProjection")
            mPointSizeUniform = GLES20.glGetUniformLocation(mProgram, "u_PointSize")
            checkGlError(TAG, "program params")
        }
    }

    /**
     * Update the mesh data in the buffer.
     *
     * @param sceneMesh Data structure in the AR mesh scene.
     */
    private fun updateSceneMeshData(sceneMesh: ARSceneMesh) {
        mSceneMeshPojo.run {
            checkGlError(TAG, "before update")
            val meshVertices = sceneMesh.vertices
            mPointsNum = meshVertices.limit() / Constants.FLOATS_PER_POINT
            debug(TAG, "updateData: Meshsize:" + mPointsNum + "position:" + meshVertices.position()
                + " limit:" + meshVertices.limit() + " remaining:" + meshVertices.remaining())
            debug(TAG, "Vertices = ")

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVerticeVBO)
            if (mVerticeVBOSize < mPointsNum * Constants.BYTES_PER_POINT) {
                while (mVerticeVBOSize < mPointsNum * Constants.BYTES_PER_POINT) {
                    mVerticeVBOSize *= 2 // If the VBO is not large enough in size, double it.
                }
                GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVerticeVBOSize, null, GLES20.GL_DYNAMIC_DRAW)
            }
            GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, mPointsNum * Constants.BYTES_PER_POINT, meshVertices)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
            val meshTriangleIndices = sceneMesh.triangleIndices
            mTrianglesNum = meshTriangleIndices.limit() / Constants.INT_PER_TRIANGE
            debug(TAG, "updateData: MeshTrianglesize:" + mTrianglesNum + "position:"
                + meshTriangleIndices.position() + " limit:" + meshTriangleIndices.limit()
                + " remaining:" + meshTriangleIndices.remaining())

            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mTriangleVBO)
            if (mTriangleVBOSize < mTrianglesNum * Constants.BYTES_PER_POINT) {
                while (mTriangleVBOSize < mTrianglesNum * Constants.BYTES_PER_POINT) {
                    mTriangleVBOSize *= 2 // If the triangle VBO is not large enough in size, double it.
                }
                GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, mTriangleVBOSize, null, GLES20.GL_DYNAMIC_DRAW)
            }
            GLES20.glBufferSubData(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0, mTrianglesNum * Constants.BYTES_PER_POINT, meshTriangleIndices)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
            checkGlError(TAG, "after update")
        }
    }

    /**
     * Set up the input data in the shader program and in the drawing program.
     *
     * @param cameraView Camera view data.
     * @param cameraPerspective Perspective data of the camera.
     */
    fun draw(cameraView: FloatArray, cameraPerspective: FloatArray) {
        mSceneMeshPojo.run {
            checkGlError(TAG, "Before draw")
            debug(TAG, "draw: mPointsNum:$mPointsNum mTrianglesNum:$mTrianglesNum")

            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
            GLES20.glEnable(GLES20.GL_CULL_FACE)
            Matrix.multiplyMM(mModelViewProjection, 0, cameraPerspective, 0, cameraView, 0)

            // Drawing point.
            GLES20.glUseProgram(mProgram)
            GLES20.glEnableVertexAttribArray(mPositionAttribute)
            GLES20.glEnableVertexAttribArray(mColorUniform)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVerticeVBO)
            GLES20.glVertexAttribPointer(mPositionAttribute, Constants.POSITION_COMPONENTS_NUMBER, GLES20.GL_FLOAT, false, Constants.BYTES_PER_POINT, 0)
            GLES20.glUniform4f(mColorUniform, 1.0f, 0.0f, 0.0f, 1.0f)
            GLES20.glUniformMatrix4fv(mModelViewProjectionUniform, 1, false, mModelViewProjection, 0)
            GLES20.glUniform1f(mPointSizeUniform, 5.0f) // Set the point size to 5.

            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, mPointsNum)
            GLES20.glDisableVertexAttribArray(mColorUniform)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
            checkGlError(TAG, "Draw point")

            // Draw a triangle.
            GLES20.glEnable(GL10.GL_BLEND)
            GLES20.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
            GLES20.glEnableVertexAttribArray(mColorUniform)
            GLES20.glUniform4f(mColorUniform, 0.0f, 1.0f, 0.0f, 0.5f)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mTriangleVBO)

            // Each triangle has three vertices.
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, mTrianglesNum * 3, GLES20.GL_UNSIGNED_INT, 0)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
            GLES20.glDisableVertexAttribArray(mColorUniform)
            checkGlError(TAG, "Draw triangles")
            GLES20.glDisableVertexAttribArray(mPositionAttribute)

            GLES20.glDisable(GLES20.GL_CULL_FACE)
            GLES20.glDisable(GL10.GL_BLEND)
            checkGlError(TAG, "Draw after")
        }
    }

    /**
     * Displayed object, which is called for each frame.
     *
     * @param arFrame Process the AR frame.
     * @param cameraView View matrix.
     * @param cameraPerspective Camera projection matrix.
     */
    fun onDrawFrame(arFrame: ARFrame, cameraView: FloatArray, cameraPerspective: FloatArray) {
        val arSceneMesh = arFrame.acquireSceneMesh()
        updateSceneMeshData(arSceneMesh)
        arSceneMesh.release()
        draw(cameraView, cameraPerspective)
    }
}
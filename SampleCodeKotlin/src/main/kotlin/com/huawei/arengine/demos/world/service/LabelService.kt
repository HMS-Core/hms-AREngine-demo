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
package com.huawei.arengine.demos.world.service

import android.app.Activity
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import android.util.Log
import com.huawei.arengine.demos.common.util.checkGlError
import com.huawei.arengine.demos.common.util.createGlProgram
import com.huawei.arengine.demos.world.pojo.LabelShaderPojo
import com.huawei.arengine.demos.world.util.Constants
import com.huawei.arengine.demos.world.util.getPlaneBitmaps
import com.huawei.arengine.demos.world.util.getSortedPlanes
import com.huawei.hiar.ARPlane
import com.huawei.hiar.ARPose
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/**
 * This class demonstrates how to use ARPlane, including how to obtain the center point of a plane.
 * If the plane type can be identified, it is also displayed at the center of the plane. Otherwise,
 * "other" is displayed.
 *
 * @author HW
 * @since 2020-10-10
 */
class LabelService {
    companion object {
        private const val TAG = "LabelService"
    }

    private val textures by lazy { IntArray(Constants.TEXTURES_SIZE) }

    /**
     * 2 * 2 rotation matrix applied to the uv coordinates.
     */
    private val planeAngleUvMatrix by lazy {
        floatArrayOf(1.0f / Constants.LABEL_WIDTH, 0.0f, 0.0f, 1.0f / Constants.LABEL_HEIGHT)
    }

    private val labelShaderPojo by lazy { LabelShaderPojo() }

    /**
     * Create the shader program for label display in the openGL thread.
     * This method will be called when [WorldRenderController.onSurfaceCreated].
     *
     * @param activity activity.
     */
    fun init(activity: Activity) {
        checkGlError(TAG, "Init start.")
        createProgram()
        initLabelTextures(activity)
        checkGlError(TAG, "Init end.")
    }

    private fun createProgram() {
        checkGlError(TAG, "program start.")
        labelShaderPojo.run {
            program = createGlProgram(Constants.LABEL_VERTEX, Constants.LABEL_FRAGMENT)
            program.let {
                position = GLES20.glGetAttribLocation(it, "inPosXZAlpha")
                modelViewProjectionMatrix = GLES20.glGetUniformLocation(it, "inMVPMatrix")
                texture = GLES20.glGetUniformLocation(it, "inTexture")
                planeUvMatrix = GLES20.glGetUniformLocation(it, "inPlanUVMatrix")
            }
        }
        checkGlError(TAG, "program end.")
    }

    private fun initLabelTextures(activity: Activity) {
        val labelBitmaps = getPlaneBitmaps(activity)
        if (labelBitmaps.isEmpty()) {
            Log.e(TAG, "No bitmap.")
        }
        GLES20.glGenTextures(textures.size, textures, 0)
        for ((i, labelBitmap) in labelBitmaps.withIndex()) {
            // for semantic label plane
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i])

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR_MIPMAP_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, labelBitmap, 0)
            GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        }
    }

    /**
     * Render the plane type at the center of the currently identified plane.
     * This method will be called when [WorldRenderController.onDrawFrame].
     *
     * @param planes All identified planes.
     * @param cameraPose Location and pose of the current camera.
     * @param cameraProjection Projection matrix of the current camera.
     */
    fun renderLabels(planes: Collection<ARPlane>, cameraPose: ARPose, cameraProjection: FloatArray) {
        FloatArray(Constants.MATRIX_SIZE).let {
            cameraPose.inverse().toMatrix(it, 0)
            renderSortedPlanes(getSortedPlanes(planes, cameraPose), it, cameraProjection)
        }
    }

    private fun renderSortedPlanes(sortedPlanes: ArrayList<ARPlane>,
        cameraViews: FloatArray, cameraProjection: FloatArray) {
        checkGlError(TAG, "Draw sorted plans start.")
        labelShaderPojo.run {
            GLES20.glDepthMask(false)
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFuncSeparate(
                GLES20.GL_DST_ALPHA, GLES20.GL_ONE, GLES20.GL_ZERO, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            GLES20.glUseProgram(program)
            GLES20.glEnableVertexAttribArray(position)

            for (plane in sortedPlanes) {
                val planeMatrix = FloatArray(Constants.MATRIX_SIZE)
                plane.centerPose.toMatrix(planeMatrix, 0)

                abs(plane.label.ordinal).let {
                    GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + it)
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[it])
                    GLES20.glUniform1i(texture, it)
                }
                GLES20.glUniformMatrix2fv(planeUvMatrix, 1, false, planeAngleUvMatrix, 0)
                renderLabel(planeMatrix, cameraViews, cameraProjection)
            }

            GLES20.glDisableVertexAttribArray(position)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            GLES20.glDisable(GLES20.GL_BLEND)
            GLES20.glDepthMask(true)
        }
        checkGlError(TAG, "Draw sorted plans end.")
    }

    private fun LabelShaderPojo.renderLabel(planeMatrix: FloatArray,
        cameraViews: FloatArray, cameraProjection: FloatArray) {
        checkGlError(TAG, "Draw label start.")
        val modelViewMatrix = FloatArray(Constants.MATRIX_SIZE)
        val modelViewProjectionMatrix = FloatArray(Constants.MATRIX_SIZE)
        Matrix.multiplyMM(modelViewMatrix, 0, cameraViews, 0, planeMatrix, 0)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraProjection, 0, modelViewMatrix, 0)
        val halfWidth = Constants.LABEL_WIDTH / 2.0f
        val halfHeight = Constants.LABEL_HEIGHT / 2.0f
        val vertices = floatArrayOf(
            -halfWidth, -halfHeight, 1f,
            -halfWidth, halfHeight, 1f,
            halfWidth, halfHeight, 1f,
            halfWidth, -halfHeight, 1f)

        // The size of each floating point is 4 bits.
        val vetBuffer = ByteBuffer.allocateDirect(4 * vertices.size)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        vetBuffer.rewind()
        for (i in vertices.indices) {
            vetBuffer.put(vertices[i])
        }
        vetBuffer.rewind()
        // The size of each floating point is 4 bits.
        GLES20.glVertexAttribPointer(position, Constants.COORDS_PER_VERTEX, GLES20.GL_FLOAT,
            false, 4 * Constants.COORDS_PER_VERTEX, vetBuffer)
        // Set the sequence of OpenGL drawing points to generate two triangles that form a plane.
        val indices = shortArrayOf(0, 1, 2, 0, 2, 3)
        // Size of the allocated buffer.
        val idxBuffer = ByteBuffer.allocateDirect(2 * indices.size)
            .order(ByteOrder.nativeOrder()).asShortBuffer()
        idxBuffer.rewind()
        for (i in indices.indices) {
            idxBuffer.put(indices[i])
        }
        idxBuffer.rewind()
        GLES20.glUniformMatrix4fv(this.modelViewProjectionMatrix, 1, false, modelViewProjectionMatrix, 0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, idxBuffer.limit(), GLES20.GL_UNSIGNED_SHORT, idxBuffer)
        checkGlError(TAG, "Draw label end.")
    }
}
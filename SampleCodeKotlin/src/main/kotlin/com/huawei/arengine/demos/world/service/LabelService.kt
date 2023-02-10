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

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix

import com.huawei.arengine.demos.common.LogUtil
import com.huawei.arengine.demos.common.util.checkGlError
import com.huawei.arengine.demos.common.util.createGlProgram
import com.huawei.arengine.demos.common.util.LabelDisplayUtil.getMeasureQuaternion
import com.huawei.arengine.demos.world.controller.WorldRenderController
import com.huawei.arengine.demos.world.util.Constants
import com.huawei.arengine.demos.world.util.getSortedPlanes
import com.huawei.hiar.ARCamera
import com.huawei.hiar.ARPlane
import com.huawei.hiar.ARPose
import com.huawei.hiar.ARTarget
import com.huawei.hiar.ARTrackableBase

import java.nio.ByteBuffer
import java.nio.ByteOrder

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

        private const val STRAIGHT_ANGLE = 180.0f

        private const val INDEX_Y = 1

        private const val DOUBLE_NUM = 2
    }

    private var textures = IntArray(Constants.TEXTURES_SIZE)

    private var modelMatrix = FloatArray(Constants.MATRIX_SIZE)

    private var modelViewMatrix = FloatArray(Constants.MATRIX_SIZE)

    private var modelViewProjectionMatrix = FloatArray(Constants.MATRIX_SIZE)

    private var program = 0

    private var glPositionParameter = 0

    private var glModelViewProjectionMatrix = 0

    private var glTexture = 0

    private var glPlaneUvMatrix = 0

    /**
     * 2 * 2 rotation matrix applied to the uv coordinates.
     */
    private val planeAngleUvMatrix by lazy {
        floatArrayOf(1.0f / Constants.LABEL_WIDTH, 0.0f, 0.0f, 1.0f / Constants.LABEL_HEIGHT)
    }

    /**
     * Create the shader program for label display in the openGL thread.
     * This method will be called when [WorldRenderController.onSurfaceCreated].
     *
     * @param activity activity.
     */
    fun init(labelBitmaps: ArrayList<Bitmap>) {
        checkGlError(TAG, "Init start.")
        if (labelBitmaps.size == 0) {
            LogUtil.error(TAG, "No bitmap.")
        }
        createProgram()
        var idx = 0
        GLES20.glGenTextures(textures.size, textures, 0)
        for (labelBitmap in labelBitmaps) {
            // for semantic label plane
            setTextBitmap(labelBitmap, idx)
            idx++
        }
        checkGlError(TAG, "Init end.")
    }

    private fun createProgram() {
        checkGlError(TAG, "program start.")
        program = createGlProgram(Constants.LABEL_VERTEX, Constants.LABEL_FRAGMENT)
        glPositionParameter = GLES20.glGetAttribLocation(program, "inPosXZAlpha")
        glModelViewProjectionMatrix = GLES20.glGetUniformLocation(program, "inMVPMatrix")
        glTexture = GLES20.glGetUniformLocation(program, "inTexture")
        glPlaneUvMatrix = GLES20.glGetUniformLocation(program, "inPlanUVMatrix")
        checkGlError(TAG, "program end.")
    }

    private fun setTextBitmap(labelBitmap: Bitmap, idx: Int) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + idx)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[idx])

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, labelBitmap, 0)
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    /**
     * Render the plane type at the center of the currently identified plane.
     * This method will be called when [WorldRenderController.onDrawFrame].
     *
     * @param planes All identified planes.
     * @param cameraPose Location and pose of the current camera.
     * @param cameraProjection Projection matrix of the current camera.
     */
    open fun onDrawFrame(allPlanes: Collection<ARPlane>, cameraPose: ARPose, cameraProjection: FloatArray) {
        val sortedPlanes: ArrayList<ARPlane> = getSortedPlanes(allPlanes, cameraPose)
        val cameraViewMatrix = FloatArray(Constants.MATRIX_SIZE)
        cameraPose.inverse().toMatrix(cameraViewMatrix, 0)
        val trackableBases = ArrayList<ARTrackableBase>(sortedPlanes)
        drawTrackable(trackableBases, cameraViewMatrix, cameraProjection, cameraPose)
    }

    /**
     * Draw the recognized target label.
     *
     * @param target Recognized target.
     * @param bitmap Rendered image.
     * @param camera ARCamera object.
     * @param cameraProjection Projection matrix.
     */
    fun onDrawFrame(target: ARTarget, bitmap: Bitmap, camera: ARCamera, cameraProjection: FloatArray) {
        setTextBitmap(bitmap, 0)
        var cameraViewMatrix = FloatArray(Constants.MATRIX_SIZE)
        camera.getViewMatrix(cameraViewMatrix, 0)
        var trackableBases = ArrayList<ARTrackableBase>(1)
        trackableBases.add(target)
        drawTrackable(trackableBases, cameraViewMatrix, cameraProjection, camera.displayOrientedPose)
    }

    private fun drawTrackable(arTrackableBases: ArrayList<ARTrackableBase>, cameraViewMatrix: FloatArray,
        cameraProjection: FloatArray, cameraDisplayPose: ARPose) {
        checkGlError(TAG, "Draw sorted plans start.")

        GLES20.glDepthMask(false)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFuncSeparate(GLES20.GL_DST_ALPHA, GLES20.GL_ONE, GLES20.GL_ZERO, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glUseProgram(program)
        GLES20.glEnableVertexAttribArray(glPositionParameter)

        for (arTrackable in arTrackableBases) {
            var objModelMatrix = FloatArray(Constants.MATRIX_SIZE)
            var idx = 0
            if (arTrackable is ARPlane) {
                arTrackable.centerPose.toMatrix(objModelMatrix, 0)
                idx = arTrackable.label.ordinal
            }
            if (arTrackable is ARTarget) {
                objModelMatrix = getLabelModeMatrix(cameraDisplayPose, arTrackable)
            }
            System.arraycopy(objModelMatrix, 0, modelMatrix, 0, Constants.MATRIX_SIZE)
            val scaleU: Float = 1.0f / Constants.LABEL_WIDTH

            // Set the value of the plane angle uv matrix.
            planeAngleUvMatrix[0] = scaleU
            planeAngleUvMatrix[1] = 0.0f
            planeAngleUvMatrix[2] = 0.0f
            val scaleV: Float = 1.0f / Constants.LABEL_HEIGHT
            planeAngleUvMatrix[3] = scaleV
            LogUtil.debug(TAG, "Plane getLabel:$idx")
            idx = Math.abs(idx)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + idx)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[idx])
            GLES20.glUniform1i(glTexture, idx)
            GLES20.glUniformMatrix2fv(glPlaneUvMatrix, 1, false, planeAngleUvMatrix, 0)
            drawLabel(cameraViewMatrix, cameraProjection)
        }
        GLES20.glDisableVertexAttribArray(glPositionParameter)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glDepthMask(true)
        checkGlError(TAG, "Draw sorted plans end.")
    }

    private fun getLabelModeMatrix(cameraDisplayPose: ARPose, target: ARTarget): FloatArray {
        val measureQuaternion: FloatArray? = getMeasureQuaternion(cameraDisplayPose, STRAIGHT_ANGLE)
        val targetCenterPose = target.centerPose
        val topPosition = floatArrayOf(targetCenterPose.tx(), targetCenterPose.ty(), targetCenterPose.tz())
        if (target.shapeType == ARTarget.TargetShapeType.TARGET_SHAPE_BOX) {
            topPosition[INDEX_Y] += target.axisAlignBoundingBox[INDEX_Y] / DOUBLE_NUM
        }
        val measurePose = ARPose(topPosition, measureQuaternion)
        val planeMatrix = FloatArray(Constants.MATRIX_SIZE)
        measurePose.toMatrix(planeMatrix, 0)
        return planeMatrix
    }

    private fun drawLabel(cameraViews: FloatArray, cameraProjection: FloatArray) {
        checkGlError(TAG, "Draw label start.")
        Matrix.multiplyMM(modelViewMatrix, 0, cameraViews, 0, modelMatrix, 0)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraProjection, 0, modelViewMatrix, 0)
        val halfWidth: Float = Constants.LABEL_WIDTH / 2.0f
        val halfHeight: Float = Constants.LABEL_HEIGHT / 2.0f
        val vertices = floatArrayOf(
            -halfWidth, -halfHeight, 1f, -halfWidth, halfHeight, 1f, halfWidth, halfHeight, 1f, halfWidth,
            -halfHeight, 1f)

        // The size of each floating point is 4 bits.
        val vetBuffer = ByteBuffer.allocateDirect(4 * vertices.size).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vetBuffer.rewind()
        for (i in vertices.indices) {
            vetBuffer.put(vertices[i])
        }
        vetBuffer.rewind()

        // The size of each floating point is 4 bits.
        GLES20.glVertexAttribPointer(
            glPositionParameter, Constants.COORDS_PER_VERTEX, GLES20.GL_FLOAT, false,
            4 * Constants.COORDS_PER_VERTEX, vetBuffer)

        // Set the sequence of OpenGL drawing points to generate two triangles that form a plane.
        val indices = shortArrayOf(0, 1, 2, 0, 2, 3)

        // Size of the allocated buffer.
        val idxBuffer = ByteBuffer.allocateDirect(2 * indices.size).order(ByteOrder.nativeOrder()).asShortBuffer()
        idxBuffer.rewind()
        for (i in indices.indices) {
            idxBuffer.put(indices[i])
        }
        idxBuffer.rewind()
        GLES20.glUniformMatrix4fv(glModelViewProjectionMatrix, 1, false, modelViewProjectionMatrix, 0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, idxBuffer.limit(), GLES20.GL_UNSIGNED_SHORT, idxBuffer)
        checkGlError(TAG, "Draw label end.")
    }
}
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

package com.huawei.arengine.demos.cloudaugmentobject.service

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Matrix
import android.opengl.GLES20
import android.opengl.GLUtils
import android.view.View
import android.widget.TextView

import com.huawei.arengine.demos.R
import com.huawei.arengine.demos.cloudaugmentobject.util.ObjectLabelShaderUtil
import com.huawei.arengine.demos.common.util.LabelDisplayUtil
import com.huawei.arengine.demos.common.util.checkGlError
import com.huawei.hiar.ARObject
import com.huawei.hiar.ARPose
import com.huawei.hiar.ARTrackable

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Draws a label based on the recognized 3D object ARPose. The label moves with the 3D object.
 *
 * @author HW
 * @since 2022-04-12
 */
class ObjectLabelRenderService(private val mActivity: Activity) {
    companion object {
        private const val TAG = "ObjectLabelRenderService"

        private const val IMAGE_ANGLE_MATRIX_SIZE = 4

        private const val MATRIX_SCALE_SX = 1.0f

        private const val MATRIX_SCALE_SY = 1.0f

        private const val CORDS_PER_VERTEX = 3

        private const val LABEL_WIDTH = 0.1f

        private const val LABEL_HEIGHT = 0.1f

        private const val TEXTURES_SIZE = 1

        private const val MATRIX_SIZE = 16
    }

    private var modelMatrix: FloatArray? = null

    private val imageAngleUvMatrix = FloatArray(IMAGE_ANGLE_MATRIX_SIZE)

    private val modelViewProjectionMatrix = FloatArray(MATRIX_SIZE)

    private val modelViewMatrix = FloatArray(MATRIX_SIZE)

    private var textures = IntArray(TEXTURES_SIZE)

    private var mProgram = 0

    private var glTexture = 0

    private var glPositionParameter = 0

    private var glPlaneUvMatrix = 0

    private var glModelViewProjectionMatrix = 0

    private lateinit var labelTextView: TextView

    fun init() {
        labelTextView = mActivity.findViewById(R.id.image_ar_object)
        createProgram()
        initLabel()
    }

    fun onDrawFrame(arObjects: Collection<ARObject>?, viewMatrix: FloatArray,
        projectionMatrix: FloatArray, cameraPose: ARPose) {
        prepareForGl()
        arObjects?.forEach { obj ->
            if (obj.trackingState != ARTrackable.TrackingState.TRACKING)
                return@forEach
            updateImageLabelData(obj, cameraPose)
            drawLabel(viewMatrix, projectionMatrix)
        }
        recycleGl()
    }

    private fun prepareForGl() {
        GLES20.glDepthMask(false)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFuncSeparate(GLES20.GL_DST_ALPHA, GLES20.GL_ONE, GLES20.GL_ZERO, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glUseProgram(mProgram)
        GLES20.glEnableVertexAttribArray(glPositionParameter)
    }

    private fun updateImageLabelData(arObject: ARObject, cameraPose: ARPose) {
        modelMatrix = getLabelModeMatrix(cameraPose, arObject)
        val scaleU: Float = 1.0f / LABEL_WIDTH

        // Set the value of the plane angle UV matrix.
        imageAngleUvMatrix[0] = scaleU
        imageAngleUvMatrix[1] = 0.0f
        imageAngleUvMatrix[2] = 0.0f
        val scaleV: Float = 1.0f / LABEL_HEIGHT
        imageAngleUvMatrix[3] = scaleV

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
        GLES20.glUniform1i(glTexture, 0)
        GLES20.glUniformMatrix2fv(glPlaneUvMatrix, 1, false, imageAngleUvMatrix, 0)
    }

    private fun drawLabel(cameraViews: FloatArray, cameraProjection: FloatArray) {
        checkGlError(TAG, "Draw object label start.")
        android.opengl.Matrix.multiplyMM(modelViewMatrix, 0, cameraViews, 0, modelMatrix, 0)
        android.opengl.Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraProjection, 0, modelViewMatrix, 0)

        // Obtain half of the width and height as the coordinate data.
        val halfWidth: Float = LABEL_WIDTH / 2.0f
        val halfHeight: Float = LABEL_HEIGHT / 2.0f
        val vertices = floatArrayOf(
            -halfWidth, -halfHeight, 1f, -halfWidth, halfHeight,
            1f, halfWidth, halfHeight, 1f, halfWidth, -halfHeight, 1f)

        // The size of each float is 4 bytes.
        val vetBuffer = ByteBuffer.allocateDirect(4 * vertices.size).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vetBuffer.rewind()
        for (vertex in vertices) {
            vetBuffer.put(vertex)
        }
        vetBuffer.rewind()
        GLES20.glVertexAttribPointer(
            glPositionParameter, CORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 4 * CORDS_PER_VERTEX, vetBuffer)

        // Set the sequence of OpenGL drawing points to generate two triangles to form a plane.
        val indices = shortArrayOf(0, 1, 2, 0, 2, 3)

        // Size of the allocated buffer. The size of each short integer is 2 bytes.
        val idxBuffer = ByteBuffer.allocateDirect(2 * indices.size).order(ByteOrder.nativeOrder())
            .asShortBuffer()
        idxBuffer.rewind()
        for (index in indices) {
            idxBuffer.put(index)
        }
        idxBuffer.rewind()

        GLES20.glUniformMatrix4fv(glModelViewProjectionMatrix, 1, false, modelViewProjectionMatrix, 0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, idxBuffer.limit(), GLES20.GL_UNSIGNED_SHORT, idxBuffer)
        checkGlError(TAG, "Draw object label end.")
    }

    private fun initLabel() {
        checkGlError(TAG, "Update start.")
        GLES20.glGenTextures(textures.size, textures, 0)

        // Label plane.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        var labelBitmap = getImageBitmap(labelTextView)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, labelBitmap, 0);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        checkGlError(TAG, "Update end.")
    }

    private fun getImageBitmap(view: TextView): Bitmap {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        view.layout(0, 0, labelTextView.getMeasuredWidth(), labelTextView.getMeasuredHeight())
        view.isDrawingCacheEnabled = true
        view.destroyDrawingCache()
        view.buildDrawingCache()
        val bitmap = view.getDrawingCache()
        val matrix = Matrix()
        matrix.setScale(MATRIX_SCALE_SX, MATRIX_SCALE_SY)
        bitmap?.run {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
        return bitmap
    }

    private fun createProgram() {
        checkGlError(TAG, "program start.");
        mProgram = ObjectLabelShaderUtil.createGlProgram()
        glTexture = GLES20.glGetUniformLocation(mProgram, "inTexture")
        glPositionParameter = GLES20.glGetAttribLocation(mProgram, "inPosXZAlpha")
        glPlaneUvMatrix = GLES20.glGetUniformLocation(mProgram, "inPlanUVMatrix")
        glModelViewProjectionMatrix = GLES20.glGetUniformLocation(mProgram, "inMVPMatrix")
        checkGlError(TAG, "program end.");
    }

    private fun getLabelModeMatrix(cameraDisplayPose: ARPose, target: ARObject): FloatArray {
        var verticalQuaternion = LabelDisplayUtil.getMeasureQuaternion(cameraDisplayPose, 0.0f)
        val targetCenterPose: ARPose = target.centerPose
        val topPosition = floatArrayOf(targetCenterPose.tx(), targetCenterPose.ty(), targetCenterPose.tz())
        val measurePose = ARPose(topPosition, verticalQuaternion)
        val planeMatrix = FloatArray(MATRIX_SIZE)
        measurePose.toMatrix(planeMatrix, 0)
        return planeMatrix
    }

    private fun recycleGl() {
        GLES20.glDisableVertexAttribArray(glPositionParameter)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glDepthMask(true)
    }
}
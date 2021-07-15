/**
 * Copyright 2021. Huawei Technologies Co., Ltd. All rights reserved.
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.arengine.demos.augmentedimage.service

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Matrix
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix.multiplyMM
import android.view.View
import android.widget.TextView
import com.huawei.arengine.demos.R
import com.huawei.arengine.demos.augmentedimage.pojo.ImageShaderPojo
import com.huawei.arengine.demos.augmentedimage.controller.AugmentedImageComponentDisplay
import com.huawei.arengine.demos.augmentedimage.util.Constants
import com.huawei.arengine.demos.common.util.checkGlError
import com.huawei.arengine.demos.common.util.createGlProgram
import com.huawei.hiar.ARAugmentedImage
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Draw the label through the pose of the image center.
 *
 * @author HW
 * @since 2021-03-29
 */
class ImageLabelService(private val mActivity: Activity) : AugmentedImageComponentDisplay {
    companion object {
        private val TAG = "ImageLabelDisplay"
    }
    /**
     * Plane angle UV matrix, which is used to adjust the rotation angle of the label and vertical/horizontal scaling ratio.
     */
    private val imageAngleUvMatrix = FloatArray(Constants.IMAGE_ANGLE_MATRIX_SIZE)

    private val modelViewProjectionMatrix = FloatArray(Constants.MATRIX_SIZE)

    private val modelViewMatrix = FloatArray(Constants.MATRIX_SIZE)

    /**
     * Allocate a temporary matrix to reduce the number of allocations per frame.
     */
    private val modelMatrix = FloatArray(Constants.MATRIX_SIZE)

    private val textures = IntArray(Constants.TEXTURES_SIZE)

    private var glPlaneUvMatrix = 0

    private var labelTextView: TextView? = null

    private val imageShaderPojo by lazy { ImageShaderPojo() }

    /**
     * Create and build an augmented image shader on the OpenGL thread.
     */
    override fun init() {
        labelTextView = mActivity.findViewById(R.id.image_science_park)
        createProgram()
        initLabel()
    }

    private fun createProgram() {
        checkGlError(TAG, "program start.")
        imageShaderPojo.run {
            program = createGlProgram(Constants.LABEL_VERTEX, Constants.LABEL_FRAGMENT)
            texture = GLES20.glGetUniformLocation(program, "inTexture")
            position = GLES20.glGetAttribLocation(program, "inPosXZAlpha")
            glPlaneUvMatrix = GLES20.glGetUniformLocation(program, "inPlanUVMatrix")
            modelViewProjection = GLES20.glGetUniformLocation(program, "inMVPMatrix")
        }
        checkGlError(TAG, "program end.")
    }

    private fun initLabel() {
        val labelBitmap = getImageBitmap(labelTextView)
        checkGlError(TAG, "Update start.")
        GLES20.glGenTextures(textures.size, textures, 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, labelBitmap, 0)
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        checkGlError(TAG, "Update end.")
    }

    private fun getImageBitmap(view: TextView?): Bitmap? {
        view!!.run {
            isDrawingCacheEnabled = true
            measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            layout(0, 0, labelTextView!!.measuredWidth, labelTextView!!.measuredHeight)
        }
        val bitmap = view.drawingCache
        val matrix = Matrix()
        matrix.setScale(Constants.MATRIX_SCALE_SX, Constants.MATRIX_SCALE_SY)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Draw an image label to mark the identified image.
     * This method will call [AugmentedImageRenderManager.onDrawFrame] in the following cases.
     *
     * @param augmentedImage Augmented image object.
     * @param viewMatrix View matrix.
     * @param projectionMatrix AR camera projection matrix.
     */
    override fun onDrawFrame(augmentedImage: ARAugmentedImage, viewMatrix: FloatArray, projectionMatrix: FloatArray) {
        prepareForGl()
        updateImageLabelData(augmentedImage)
        drawLabel(viewMatrix, projectionMatrix)
        recycleGl()
    }

    private fun prepareForGl() {
        imageShaderPojo.run {
            GLES20.glDepthMask(false)
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFuncSeparate(GLES20.GL_DST_ALPHA, GLES20.GL_ONE, GLES20.GL_ZERO, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            GLES20.glUseProgram(program)
            GLES20.glEnableVertexAttribArray(position)
        }
    }

    /**
     * Update the label of the augmented image.
     *
     * @param augmentedImage AugmentedImage object.
     */
    private fun updateImageLabelData(augmentedImage: ARAugmentedImage) {
        val imageMatrix = FloatArray(Constants.MATRIX_SIZE)
        augmentedImage.centerPose.toMatrix(imageMatrix, 0)
        System.arraycopy(imageMatrix, 0, modelMatrix, 0, Constants.MATRIX_SIZE)
        val scaleU = 1.0f / Constants.LABEL_WIDTH

        // Set the value of the plane angle uv matrix.
        imageAngleUvMatrix[0] = scaleU
        imageAngleUvMatrix[1] = 0.0f
        imageAngleUvMatrix[2] = 0.0f
        val scaleV = 1.0f / Constants.LABEL_HEIGHT
        imageAngleUvMatrix[3] = scaleV
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
        GLES20.glUniform1i(imageShaderPojo.texture, 0)
        GLES20.glUniformMatrix2fv(glPlaneUvMatrix, 1, false, imageAngleUvMatrix, 0)
    }

    /**
     * Draw a label.
     *
     * @param cameraViews View matrix.
     * @param cameraProjection AR camera projection matrix.
     */
    private fun drawLabel(cameraViews: FloatArray, cameraProjection: FloatArray) {
        checkGlError(TAG, "Draw image label start.")
        multiplyMM(modelViewMatrix, 0, cameraViews, 0, modelMatrix, 0)
        multiplyMM(modelViewProjectionMatrix, 0, cameraProjection, 0, modelViewMatrix, 0)

        // Obtain half of the width and height as the coordinate data.
        val halfWidth = Constants.LABEL_WIDTH / 2.0f
        val halfHeight = Constants.LABEL_HEIGHT / 2.0f
        val vertices = floatArrayOf(-halfWidth, -halfHeight, 1f, -halfWidth, halfHeight, 1f, halfWidth, halfHeight, 1f, halfWidth,
            -halfHeight, 1f)

        // The size of each float is 4 bytes.
        val vetBuffer = ByteBuffer.allocateDirect(4 * vertices.size).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vetBuffer.rewind()
        vertices.indices.forEach {
            vetBuffer.put(vertices[it])
        }
        vetBuffer.rewind()
        GLES20.glVertexAttribPointer(imageShaderPojo.position, Constants.COORDS_PER_VERTEX, GLES20.GL_FLOAT, false,
            4 * Constants.COORDS_PER_VERTEX, vetBuffer)

        // Set the sequence of OpenGL drawing points to generate two triangles to form a plane.
        val indices = shortArrayOf(0, 1, 2, 0, 2, 3)

        // Size of the allocated buffer. The size of each short integer is 2 bytes.
        val idxBuffer = ByteBuffer.allocateDirect(2 * indices.size).order(ByteOrder.nativeOrder()).asShortBuffer()
        idxBuffer.rewind()
        indices.indices.forEach {
            idxBuffer.put(indices[it])
        }
        idxBuffer.rewind()
        GLES20.glUniformMatrix4fv(imageShaderPojo.modelViewProjection, 1, false, modelViewProjectionMatrix, 0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, idxBuffer.limit(), GLES20.GL_UNSIGNED_SHORT, idxBuffer)
        checkGlError(TAG, "Draw image label end.")
    }

    private fun recycleGl() {
        GLES20.glDisableVertexAttribArray(imageShaderPojo.position)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glDepthMask(true)
    }
}
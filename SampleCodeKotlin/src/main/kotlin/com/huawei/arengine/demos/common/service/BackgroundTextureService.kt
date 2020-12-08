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
package com.huawei.arengine.demos.common.service

import android.opengl.GLES11Ext
import android.opengl.GLES20
import com.huawei.arengine.demos.common.pojo.ShaderPojo
import com.huawei.arengine.demos.common.util.Constants
import com.huawei.arengine.demos.common.util.Constants.RGB_CLEAR_VALUE
import com.huawei.arengine.demos.common.util.MatrixUtil
import com.huawei.arengine.demos.common.util.checkGlError
import com.huawei.arengine.demos.common.util.createGlProgram
import com.huawei.hiar.ARFrame
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.opengles.GL10

/**
 * This is a common class for drawing camera textures that you can use to display camera images on the screen.
 *
 * @author hw
 * @since 2020-10-10
 */
class BackgroundTextureService {
    companion object {
        private const val TAG = "BackgroundTextureService"
    }

    var externalTextureId = 0
        private set

    private val shaderPojo by lazy { ShaderPojo() }

    private val projectionMatrix by lazy { FloatArray(Constants.MATRIX_SIZE) }

    private val unitMatrix by lazy { MatrixUtil.originalMatrix }

    private val verBuffer by lazy {
        ByteBuffer.allocateDirect(32).apply {
            order(ByteOrder.nativeOrder())
        }.asFloatBuffer().apply {
            put(Constants.COORDINATE_VERTEX)
            position(0)
        }
    }

    private val textureTransformedBuffer by lazy {
        ByteBuffer.allocateDirect(32).apply {
            order(ByteOrder.nativeOrder())
        }.asFloatBuffer()
    }

    private val textureBuffer by lazy {
        ByteBuffer.allocateDirect(32).apply {
            order(ByteOrder.nativeOrder())
        }.asFloatBuffer().apply {
            put(Constants.COORDINATE_TEXTURE)
            position(0)
        }
    }

    /**
     * This method is called when [android.opengl.GLSurfaceView.Renderer.onSurfaceChanged]
     * to update the projection matrix.
     *
     * @param width Width.
     * @param height Height
     */
    fun updateProjectionMatrix(width: Int, height: Int) {
        MatrixUtil.getProjectionMatrix(projectionMatrix, width, height)
    }

    /**
     * This method is called when [android.opengl.GLSurfaceView.Renderer.onSurfaceCreated]
     * to initialize the texture ID and create the OpenGL ES shader program.
     */
    fun init() {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        externalTextureId = textures[0]
        generateExternalTexture()
        createProgram()
    }

    /**
     * If the texture ID has been created externally, this method is called when
     * [android.opengl.GLSurfaceView.Renderer.onSurfaceCreated].
     *
     * @param textureId Texture id.
     */
    fun init(textureId: Int) {
        externalTextureId = textureId
        generateExternalTexture()
        createProgram()
    }

    private fun generateExternalTexture() {
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, externalTextureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST.toFloat())
    }

    private fun createProgram() {
        shaderPojo.run {
            program = createGlProgram(Constants.BASE_VERTEX, Constants.BASE_FRAGMENT)
            position = GLES20.glGetAttribLocation(program, "vPosition")
            textureCoordinate = GLES20.glGetAttribLocation(program, "vCoord")
            uniformProjectionMatrix = GLES20.glGetUniformLocation(program, "vMatrix")
            texture = GLES20.glGetUniformLocation(program, "vTexture")
            uniformUnitMatrix = GLES20.glGetUniformLocation(program, "vCoordMatrix")
        }
    }

    /**
     * Render each frame. This method is called when [android.opengl.GLSurfaceView.Renderer.onDrawFrame].
     *
     * @param frame ARFrame
     */
    fun renderBackgroundTexture(frame: ARFrame?) {
        checkGlError(TAG, "On draw frame start.")
        glClear()
        frame ?: return
        if (frame.hasDisplayGeometryChanged()) {
            frame.transformDisplayUvCoords(textureBuffer, textureTransformedBuffer)
        }
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)

        shaderPojo.run {
            GLES20.glUseProgram(program)

            // Set the texture ID.
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, externalTextureId)

            // Set the projection matrix.
            GLES20.glUniformMatrix4fv(uniformProjectionMatrix, 1, false, projectionMatrix, 0)
            GLES20.glUniformMatrix4fv(uniformUnitMatrix, 1, false, unitMatrix, 0)

            // Set the vertex.
            GLES20.glEnableVertexAttribArray(position)
            GLES20.glVertexAttribPointer(position, 2, GLES20.GL_FLOAT, false, 0, verBuffer)

            // Set the texture coordinates.
            GLES20.glEnableVertexAttribArray(textureCoordinate)
            GLES20.glVertexAttribPointer(textureCoordinate, 2, GLES20.GL_FLOAT, false,
                0, textureTransformedBuffer)

            // Number of vertices.
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            GLES20.glDisableVertexAttribArray(position)
            GLES20.glDisableVertexAttribArray(textureCoordinate)
            GLES20.glDepthMask(true)
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        }
        checkGlError(TAG, "On draw frame end.")
    }

    /**
     * Clear canvas.
     */
    private fun glClear() {
        GLES20.glClearColor(RGB_CLEAR_VALUE, RGB_CLEAR_VALUE, RGB_CLEAR_VALUE, 1.0f)
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT)
    }
}
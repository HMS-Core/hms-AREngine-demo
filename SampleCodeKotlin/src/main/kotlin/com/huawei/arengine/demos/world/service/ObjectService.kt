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

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import android.util.Log
import com.huawei.arengine.demos.MainApplication
import com.huawei.arengine.demos.common.util.MatrixUtil.normalizeVec3
import com.huawei.arengine.demos.common.util.checkGlError
import com.huawei.arengine.demos.common.util.createGlProgram
import com.huawei.arengine.demos.world.model.VirtualObject
import com.huawei.arengine.demos.world.pojo.ObjectShaderPojo
import com.huawei.arengine.demos.world.util.Constants
import com.huawei.arengine.demos.world.util.ObjectUtil
import java.io.IOException

/**
 * Draw a virtual object based on the specified parameters.
 *
 * @author HW
 * @since 2020-10-10
 */
class ObjectService {
    companion object {
        private const val TAG = "ObjectService"
    }

    private var vertexBufferId = 0

    private var indexBufferId = 0

    private var texCoordsBaseAddress = 0

    private var normalsBaseAddress = 0

    private var indexCount = 0

    private val textures by lazy { IntArray(1) }

    private val objectShaderPojo by lazy { ObjectShaderPojo() }

    /**
     * Create a shader program to read the data of the virtual object.
     * This method is called when [WorldRenderController.onSurfaceCreated]
     */
    fun
        init() {
        checkGlError(TAG, "Init start.")
        createProgram()
        initBitmapTexture()
        initObjectTexture()
        checkGlError(TAG, "Init end.")
    }

    private fun createProgram() {
        checkGlError(TAG, "Create program start.")
        objectShaderPojo.run {
            program = createGlProgram(Constants.OBJECT_VERTEX, Constants.OBJECT_FRAGMENT)
            program.let {
                modelView = GLES20.glGetUniformLocation(it, "inViewMatrix")
                modelViewProjection = GLES20.glGetUniformLocation(it, "inMVPMatrix")
                position = GLES20.glGetAttribLocation(it, "inObjectPosition")
                normalVector = GLES20.glGetAttribLocation(it, "inObjectNormalVector")
                texCoordinate = GLES20.glGetAttribLocation(it, "inTexCoordinate")
                texture = GLES20.glGetUniformLocation(it, "inObjectTexture")
                light = GLES20.glGetUniformLocation(it, "inLight")
                color = GLES20.glGetUniformLocation(it, "inObjectColor")
            }
        }
        checkGlError(TAG, "Create program end.")
    }

    private fun initBitmapTexture() {
        checkGlError(TAG, "Init gl texture data start.")
        var textureBitmap: Bitmap? = null
        try {
            MainApplication.context.assets.open("AR_logo.png").use { inputStream ->
                textureBitmap = BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Get data error!")
            return
        } catch (e: IOException) {
            Log.e(TAG, "Get data error!")
            return
        }
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, textureBitmap, 0)
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        textureBitmap?.recycle()
        checkGlError(TAG, "Init gl texture data end.")
    }

    private fun initObjectTexture() {
        val objectData = ObjectUtil.readObject() ?: return
        // Coordinate and index.
        val buffers = IntArray(2)
        GLES20.glGenBuffers(2, buffers, 0)
        vertexBufferId = buffers[0]
        indexBufferId = buffers[1]

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glGenTextures(textures.size, textures, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)

        texCoordsBaseAddress = Constants.FLOAT_BYTE_SIZE * objectData.objectIndices.limit()
        normalsBaseAddress = texCoordsBaseAddress + Constants.FLOAT_BYTE_SIZE * objectData.texCoords.limit()
        val totalBytes = normalsBaseAddress + Constants.FLOAT_BYTE_SIZE * objectData.normals.limit()

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferId)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, totalBytes, null, GLES20.GL_STATIC_DRAW)
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0,
            Constants.FLOAT_BYTE_SIZE * objectData.objectVertices.limit(), objectData.objectVertices)
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, texCoordsBaseAddress,
            Constants.FLOAT_BYTE_SIZE * objectData.texCoords.limit(), objectData.texCoords)
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, normalsBaseAddress,
            Constants.FLOAT_BYTE_SIZE * objectData.normals.limit(), objectData.normals)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBufferId)
        indexCount = objectData.indices.limit()
        GLES20.glBufferData(
            GLES20.GL_ELEMENT_ARRAY_BUFFER, Constants.INDEX_COUNT_RATIO * indexCount,
            objectData.indices, GLES20.GL_STATIC_DRAW)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
        checkGlError(TAG, "obj buffer load")
    }

    /**
     * Draw a virtual object at a specific location on a specified plane.
     * This method is called when [WorldRenderController.onDrawFrame].
     *
     * @param cameraView The viewMatrix is a 4 * 4 matrix.
     * @param cameraProjection The ProjectionMatrix is a 4 * 4 matrix.
     * @param lightIntensity The lighting intensity.
     * @param obj The virtual object.
     */
    fun renderObjects(cameraView: FloatArray?, cameraProjection: FloatArray?,
        lightIntensity: Float, obj: VirtualObject) {
        checkGlError(TAG, "onDrawFrame start.")
        val modelViewMatrix = FloatArray(Constants.MATRIX_SIZE)
        val modelViewProjectionMatrix = FloatArray(Constants.MATRIX_SIZE)
        Matrix.multiplyMM(modelViewMatrix, 0, cameraView, 0, obj.getModelAnchorMatrix(), 0)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraProjection, 0, modelViewMatrix, 0)
        objectShaderPojo.run {
            GLES20.glUseProgram(program)
            // Light direction (x, y, z, w).
            val viewLightDirections = FloatArray(4)
            Matrix.multiplyMV(viewLightDirections, 0, modelViewMatrix, 0, Constants.LIGHT_DIRECTIONS, 0)
            normalizeVec3(viewLightDirections)

            // Light direction.
            GLES20.glUniform4f(light,
                viewLightDirections[0], viewLightDirections[1], viewLightDirections[2], lightIntensity)
            val objColors = obj.getObjectColor()
            GLES20.glUniform4fv(color, 1, objColors, 0)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
            GLES20.glUniform1i(texture, 0)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferId)

            // The coordinate dimension of the read virtual object is 3.
            GLES20.glVertexAttribPointer(position, 3, GLES20.GL_FLOAT, false, 0, 0)
            // The dimension of the normal vector is 3.
            GLES20.glVertexAttribPointer(normalVector, 3, GLES20.GL_FLOAT, false, 0, normalsBaseAddress)
            // The dimension of the texture coordinate is 2.
            GLES20.glVertexAttribPointer(texCoordinate, 2, GLES20.GL_FLOAT, false, 0, texCoordsBaseAddress)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
            GLES20.glUniformMatrix4fv(modelView, 1, false, modelViewMatrix, 0)
            GLES20.glUniformMatrix4fv(modelViewProjection, 1, false, modelViewProjectionMatrix, 0)
            GLES20.glEnableVertexAttribArray(position)
            GLES20.glEnableVertexAttribArray(normalVector)
            GLES20.glEnableVertexAttribArray(texCoordinate)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBufferId)
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, 0)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
            GLES20.glDisableVertexAttribArray(position)
            GLES20.glDisableVertexAttribArray(normalVector)
            GLES20.glDisableVertexAttribArray(texCoordinate)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        }
        checkGlError(TAG, "onDrawFrame end.")
    }
}
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

package com.huawei.arengine.demos.scenemesh.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import com.huawei.arengine.demos.common.LogUtil
import com.huawei.arengine.demos.common.util.MatrixUtil.normalizeVec3
import com.huawei.arengine.demos.common.util.checkGlError
import com.huawei.arengine.demos.common.util.createGlProgram
import com.huawei.arengine.demos.scenemesh.pojo.VirtualObjectPojo
import com.huawei.arengine.demos.scenemesh.util.Constants
import de.javagl.obj.Obj
import de.javagl.obj.ObjData
import de.javagl.obj.ObjReader
import de.javagl.obj.ObjUtils
import java.io.IOException
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer
import java.nio.ByteBuffer
import java.util.Optional

/**
 * Displays objects that are loaded from the OBJ file in Open GL.
 *
 * @author HW
 * @since 2021-04-21
 */
class VirtualObjectData {
    companion object {
        private const val TAG = "VirtualObjectData"
    }

    private val mVirtualObjectPojo by lazy { VirtualObjectPojo() }

    private val mTextures = IntArray(1)

    private val mModelMatrix = FloatArray(16)

    private val mModelViewMatrix = FloatArray(16)

    private val mModelViewProjectionMatrix = FloatArray(16)

    private val mViewLightDirections = FloatArray(Constants.LIGHT_DIRECTION_SIZE)

    /**
     * Virtual object data class.
     *
     * @author HW
     * @since 2021-02-8
     */
    private class ObjectData internal constructor(var objectIndices: IntBuffer,
        var objectVertices: FloatBuffer, var indices: ShortBuffer,
        var texCoords: FloatBuffer, var normals: FloatBuffer)

    /**
     *  Initialize the cache and compile the link coloring program On GlThread.
     *
     * @param context Load the shader and the context of the following model and texture assets.
     */
    fun init(context: Context) {
        mVirtualObjectPojo.run {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glGenTextures(mTextures.size, mTextures, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0])

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            initGlImageData(context)

            var objectData: ObjectData? = null
            val objectDataOptional: Optional<ObjectData> = readObject(context)
            objectData = if (objectDataOptional.isPresent) {
                objectDataOptional.get()
            } else {
                LogUtil.error(TAG, "Read object error.")
                return
            }

            mTexCoordsBaseAddress = mVerticesBaseAddress + Constants.FLOAT_BYTE_SIZE * objectData.objectIndices.limit()
            mNormalsBaseAddress = mTexCoordsBaseAddress + Constants.FLOAT_BYTE_SIZE * objectData.texCoords.limit()
            val totalBytes: Int = mNormalsBaseAddress + Constants.FLOAT_BYTE_SIZE * objectData.normals.limit()
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferId)
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, totalBytes, null, GLES20.GL_STATIC_DRAW)
            GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, mVerticesBaseAddress,
                Constants.FLOAT_BYTE_SIZE * objectData.objectVertices.limit(), objectData.objectVertices)
            GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, mTexCoordsBaseAddress,
                Constants.FLOAT_BYTE_SIZE * objectData.texCoords.limit(), objectData.texCoords)
            GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, mNormalsBaseAddress,
                Constants.FLOAT_BYTE_SIZE * objectData.normals.limit(), objectData.normals)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndexBufferId)
            mIndexCount = objectData.indices.limit()

            // Prevent the memory from being insufficient and multiply the memory.
            GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, 2 * mIndexCount, objectData.indices, GLES20.GL_STATIC_DRAW)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)

            loadShaderAndGetOpenGLESVariable()
        }
    }

    private fun readObject(context: Context): Optional<ObjectData> {
        mVirtualObjectPojo.run {
            var obj: Obj? = null
            try {
                context.assets.open(Constants.OBJASSETNAME).use { objInputStream ->
                    obj = ObjReader.read(objInputStream)
                    obj = ObjUtils.convertToRenderable(obj)
                }
            } catch (e: java.lang.IllegalArgumentException) {
                LogUtil.error(TAG, "Get data failed!")
                return Optional.empty()
            } catch (e: IOException) {
                LogUtil.error(TAG, "Get data failed!")
                return Optional.empty()
            }

            // Each surface of the object has three vertices.
            val objectIndices = ObjData.getFaceVertexIndices(obj!!, 3)

            // Prevent the memory from being insufficient and multiply the memory.
            val indices = ByteBuffer.allocateDirect(2 * objectIndices.limit())
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
            while (objectIndices.hasRemaining()) {
                indices.put(objectIndices.get().toShort())
            }
            indices.rewind()
            val buffers = IntArray(Constants.BUFFER_OBJECT_NUMBER)
            GLES20.glGenBuffers(Constants.BUFFER_OBJECT_NUMBER, buffers, 0)
            mVertexBufferId = buffers[0]
            mIndexBufferId = buffers[1]
            mVerticesBaseAddress = 0
            val objectVertices = ObjData.getVertices(obj)
            val texCoords = ObjData.getTexCoords(obj, 2) // Set the coordinate dimension to 2.

            val normals = ObjData.getNormals(obj)
            return Optional.of(ObjectData(objectIndices, objectVertices, indices, texCoords, normals))
        }
    }

    private fun loadShaderAndGetOpenGLESVariable() {
        mVirtualObjectPojo.run {
            mProgram = createGlProgram(Constants.VIRTUAL_OBJECT_VERTEX, Constants.VIRTUAL_OBJECT_FRAGMENT)
            GLES20.glUseProgram(mProgram)

            mModelViewUniform = GLES20.glGetUniformLocation(mProgram, "u_ModelView")
            mModelViewProjectionUniform = GLES20.glGetUniformLocation(mProgram, "u_ModelViewProjection")

            mPositionAttribute = GLES20.glGetAttribLocation(mProgram, "a_Position")
            mNormalAttribute = GLES20.glGetAttribLocation(mProgram, "a_Normal")
            mTexCoordAttribute = GLES20.glGetAttribLocation(mProgram, "a_TexCoord")

            mTextureUniform = GLES20.glGetUniformLocation(mProgram, "u_Texture")

            mLightingParametersUniform = GLES20.glGetUniformLocation(mProgram, "u_LightingParameters")
            mMaterialParametersUniform = GLES20.glGetUniformLocation(mProgram, "u_MaterialParameters")
            mColorUniform = GLES20.glGetUniformLocation(mProgram, "u_ObjColor")

            Matrix.setIdentityM(mModelMatrix, 0)
        }
    }

    private fun initGlImageData(context: Context) {
        checkGlError(TAG, "Init gl texture data start.")
        var textureBitmap: Bitmap? = null
        try {
            context.assets.open(Constants.DIFFUSETEXTUREASSETNAME).use { inputStream -> textureBitmap = BitmapFactory.decodeStream(inputStream) }
        } catch (e: IllegalArgumentException) {
            LogUtil.error(TAG, "Get data error!")
            return
        } catch (e: IOException) {
            LogUtil.error(TAG, "Get data error!")
            return
        }
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, textureBitmap, 0)
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        textureBitmap?.recycle()
        checkGlError(TAG, "Init gl texture data end.")
    }

    /**
     * Update the model matrix data.
     *
     * @param modelMatrixData Model matrix data.
     * @param scaleFactor Scaling factor.
     */
    fun updateModelMatrix(modelMatrixData: FloatArray?, scaleFactor: Float) {
        val scaleMatrixs = FloatArray(16)
        Matrix.setIdentityM(scaleMatrixs, 0)

        // Set the first column of the matrix on the right to the scaling factor.
        // Scaling factor of the diagonal line of the matrix.
        scaleMatrixs[0] = scaleFactor
        scaleMatrixs[5] = scaleFactor
        scaleMatrixs[10] = scaleFactor
        Matrix.multiplyMM(mModelMatrix, 0, modelMatrixData, 0, scaleMatrixs, 0)
        Matrix.rotateM(mModelMatrix, 0, 315.0f, 0f, 1f, 0f) // Rotation 315 degrees.
    }

    /**
     * Set the material attributes.
     *
     * @param ambient Material property: environment parameter.
     * @param diffuse Material property: diffusion parameter.
     * @param specular Material property: specular parameter.
     * @param specularPower Material property: specular power parameter.
     */
    fun setMaterialProperties(ambient: Float, diffuse: Float, specular: Float, specularPower: Float) {
        mVirtualObjectPojo.run {
            mAmbient = ambient
            mDiffuse = diffuse
            mSpecular = specular
            mSpecularPower = specularPower
        }
    }

    /**
     * Draw a virtual object at a specific location on a specified plane.
     *
     * @param cameraView Camera view data.
     * @param cameraPerspective Perspective data of the camera.
     * @param lightIntensity Light intensity data.
     * @param objColor Object color.
     */
    fun draw(cameraView: FloatArray?, cameraPerspective: FloatArray?, lightIntensity: Float, objColor: String?) {
        mVirtualObjectPojo.run {
            LogUtil.debug(TAG, "Before draw Virtual Object : ")
            Matrix.multiplyMM(mModelViewMatrix, 0, cameraView, 0, mModelMatrix, 0)
            Matrix.multiplyMM(mModelViewProjectionMatrix, 0, cameraPerspective, 0, mModelViewMatrix, 0)
            GLES20.glUseProgram(mProgram)
            Matrix.multiplyMV(mViewLightDirections, 0, mModelViewMatrix, 0, Constants.LIGHT_DIRECTIONS, 0)
            normalizeVec3(mViewLightDirections)

            // The lighting direction data has three dimensions (0, 1, and 2).
            GLES20.glUniform4f(mLightingParametersUniform, mViewLightDirections[0], mViewLightDirections[1],
                mViewLightDirections[2], lightIntensity)
            when (objColor) {
                Constants.AR_TRACK_POINT_COLOR -> GLES20.glUniform4fv(mColorUniform, 1, Constants.TRACK_POINT_COLOR, 0)
                Constants.AR_TRACK_PLANE_COLOR -> GLES20.glUniform4fv(mColorUniform, 1, Constants.TRACK_PLANE_COLOR, 0)
                Constants.AR_DEFAULT_COLOR -> GLES20.glUniform4fv(mColorUniform, 1, Constants.DEFAULT_COLORS, 0)
                else -> LogUtil.error(TAG, "draw, obj color error")
            }
            GLES20.glUniform4f(mMaterialParametersUniform, mAmbient, mDiffuse, mSpecular, mSpecularPower)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0])
            GLES20.glUniform1i(mTextureUniform, 0)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferId)
            GLES20.glVertexAttribPointer(mPositionAttribute, Constants.COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0,
                mVerticesBaseAddress)
            GLES20.glVertexAttribPointer(mNormalAttribute, Constants.A_NORMAL_SIZE, GLES20.GL_FLOAT, false, 0, mNormalsBaseAddress)
            GLES20.glVertexAttribPointer(mTexCoordAttribute, Constants.A_TEXCOORD_SIZE, GLES20.GL_FLOAT, false, 0,
                mTexCoordsBaseAddress)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
            GLES20.glUniformMatrix4fv(mModelViewUniform, 1, false, mModelViewMatrix, 0)
            GLES20.glUniformMatrix4fv(mModelViewProjectionUniform, 1, false, mModelViewProjectionMatrix, 0)
            GLES20.glEnableVertexAttribArray(mPositionAttribute)
            GLES20.glEnableVertexAttribArray(mNormalAttribute)
            GLES20.glEnableVertexAttribArray(mTexCoordAttribute)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndexBufferId)
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, mIndexCount, GLES20.GL_UNSIGNED_SHORT, 0)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
            GLES20.glDisableVertexAttribArray(mPositionAttribute)
            GLES20.glDisableVertexAttribArray(mNormalAttribute)
            GLES20.glDisableVertexAttribArray(mTexCoordAttribute)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            LogUtil.debug(TAG, "After draw Virtual Object : ")
        }
    }
}
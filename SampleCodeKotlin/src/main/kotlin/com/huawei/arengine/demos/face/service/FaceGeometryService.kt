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
package com.huawei.arengine.demos.face.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import android.util.Log
import com.huawei.arengine.demos.MainApplication
import com.huawei.arengine.demos.common.util.checkGlError
import com.huawei.arengine.demos.common.util.createGlProgram
import com.huawei.arengine.demos.face.pojo.ShaderPojo
import com.huawei.arengine.demos.face.util.Constants
import com.huawei.hiar.ARCamera
import com.huawei.hiar.ARFace
import com.huawei.hiar.ARFaceGeometry
import java.io.IOException

/**
 * Get the facial geometric data and render the data on the screen.
 *
 * @author HW
 * @since 2020-10-10
 */
class FaceGeometryService {
    companion object {
        private const val TAG = "FaceGeometryService"
    }

    /**
     * Initialize the size of the vertex VBO.
     */
    private var vertexBufferSize = 8000

    /**
     * Initialize the size of the triangle VBO.
     */
    private var triangleBufferSize = 5000

    private var textureId = 0

    private var vertexId = 0

    private var triangleId = 0

    private var pointsNum = 0

    private var trianglesNum = 0

    private val shaderPojo by lazy { ShaderPojo() }

    /**
     * The size of the MVP matrix is 4 x 4.
     */
    private val modelViewProjections by lazy { FloatArray(16) }

    /**
     * Initialize the OpenGL ES rendering related to face geometry, including creating the shader program.
     * This method is called when [FaceRenderController.onSurfaceCreated].
     */
    fun init() {
        initBuffer()
        initTextureBitmap()
        createProgram()
        checkGlError(TAG, "Init end.")
    }

    private fun initBuffer() {
        val textures = IntArray(1)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]

        val buffers = IntArray(2)
        GLES20.glGenBuffers(2, buffers, 0)
        vertexId = buffers[0]
        triangleId = buffers[1]

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexId)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexBufferSize * Constants.BYTES_PER_POINT,
            null, GLES20.GL_DYNAMIC_DRAW)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, triangleId)
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, triangleBufferSize * Constants.BYTES_PER_FLOAT,
            null, GLES20.GL_DYNAMIC_DRAW)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
    }

    private fun initTextureBitmap() {
        var textureBitmap: Bitmap? = null
        try {
            MainApplication.context.assets.open("face_geometry.png").use { inputStream ->
                textureBitmap = BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Open bitmap error!")
            return
        } catch (e: IOException) {
            Log.e(TAG, "Open bitmap error!")
            return
        }
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, textureBitmap, 0)
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    private fun createProgram() {
        checkGlError(TAG, "Create gl program start.")
        shaderPojo.run {
            program = createGlProgram(Constants.FACE_GEOMETRY_VERTEX, Constants.FACE_GEOMETRY_FRAGMENT)
            positionAttribute = GLES20.glGetAttribLocation(program, "inPosition")
            colorUniform = GLES20.glGetUniformLocation(program, "inColor")
            modelViewProjectionUniform = GLES20.glGetUniformLocation(program, "inMVPMatrix")
            pointSizeUniform = GLES20.glGetUniformLocation(program, "inPointSize")
            textureUniform = GLES20.glGetUniformLocation(program, "inTexture")
            textureCoordAttribute = GLES20.glGetAttribLocation(program, "inTexCoord")
        }
        checkGlError(TAG, "Create gl program end.")
    }

    /**
     * Update the face geometric data in the buffer.
     * This method is called when [FaceRenderController.onDrawFrame].
     *
     * @param camera ARCamera.
     * @param face ARFace.
     */
    fun renderFace(camera: ARCamera, face: ARFace) {
        val faceGeometry = face.faceGeometry
        updateFaceGeometryData(faceGeometry)
        updateModelViewProjectionMatrix(camera, face)
        renderFaceGeometry()
        faceGeometry.release()
    }

    private fun updateFaceGeometryData(faceGeometry: ARFaceGeometry) {
        checkGlError(TAG, "Before update data.")
        updateFaceVertices(faceGeometry)
        updateFaceTriangles(faceGeometry)
        checkGlError(TAG, "After update data.")
    }

    private fun updateFaceVertices(faceGeometry: ARFaceGeometry) {
        val faceVertices = faceGeometry.vertices

        // Obtain the number of geometric vertices of a face.
        pointsNum = faceVertices.limit() / 3
        val textureCoordinates = faceGeometry.textureCoordinates

        // Obtain the number of geometric texture coordinates of the face (two-dimensional).
        val texNum = textureCoordinates.limit() / 2
        Log.d(TAG, "Update face geometry data: texture coordinates size: $texNum")
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexId)
        if (vertexBufferSize < (pointsNum + texNum) * Constants.BYTES_PER_POINT) {
            while (vertexBufferSize < (pointsNum + texNum) * Constants.BYTES_PER_POINT) {
                // If the capacity of the vertex VBO buffer is insufficient, expand the capacity.
                vertexBufferSize *= 2
            }
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexBufferSize, null, GLES20.GL_DYNAMIC_DRAW)
        }
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, pointsNum * Constants.BYTES_PER_POINT, faceVertices)
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, pointsNum * Constants.BYTES_PER_POINT,
            texNum * Constants.BYTES_PER_COORD, textureCoordinates)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    private fun updateFaceTriangles(faceGeometry: ARFaceGeometry) {
        trianglesNum = faceGeometry.triangleCount
        val faceTriangleIndices = faceGeometry.triangleIndices
        Log.d(TAG, "update face geometry data: faceTriangleIndices.size: " + faceTriangleIndices.limit())
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, triangleId)
        if (triangleBufferSize < trianglesNum * Constants.BYTES_PER_POINT) {
            while (triangleBufferSize < trianglesNum * Constants.BYTES_PER_POINT) {
                // If the capacity of the vertex VBO buffer is insufficient, expand the capacity.
                triangleBufferSize *= 2
            }
            GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, triangleBufferSize, null, GLES20.GL_DYNAMIC_DRAW)
        }
        GLES20.glBufferSubData(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0,
            trianglesNum * Constants.BYTES_PER_POINT, faceTriangleIndices)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    private fun updateModelViewProjectionMatrix(camera: ARCamera, face: ARFace) {
        // The size of the projection matrix is 4 * 4.
        val projectionMatrix = FloatArray(16)
        camera.getProjectionMatrix(projectionMatrix, 0,
            Constants.PROJECTION_MATRIX_NEAR, Constants.PROJECTION_MATRIX_FAR)
        val facePose = face.pose

        // The size of viewMatrix is 4 * 4.
        val facePoseViewMatrix = FloatArray(16)
        facePose.toMatrix(facePoseViewMatrix, 0)
        Matrix.multiplyMM(modelViewProjections, 0, projectionMatrix, 0, facePoseViewMatrix, 0)
    }

    /**
     * Draw face geometrical features. This method is called on each frame.
     */
    private fun renderFaceGeometry() {
        checkGlError(TAG, "render face start.")
        shaderPojo.run {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glUniform1i(textureUniform, 0)
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
            GLES20.glEnable(GLES20.GL_CULL_FACE)
            drawPoints()
            drawTriangles()
        }
        checkGlError(TAG, "render face end.")
    }

    private fun ShaderPojo.drawPoints() {
        // Draw point.
        GLES20.glUseProgram(program)
        GLES20.glEnableVertexAttribArray(positionAttribute)
        GLES20.glEnableVertexAttribArray(textureCoordAttribute)
        GLES20.glEnableVertexAttribArray(colorUniform)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexId)
        GLES20.glVertexAttribPointer(positionAttribute, Constants.POSITION_COMPONENTS_NUMBER,
            GLES20.GL_FLOAT, false, Constants.BYTES_PER_POINT, 0)
        GLES20.glVertexAttribPointer(textureCoordAttribute, Constants.TEXCOORD_COMPONENTS_NUMBER,
            GLES20.GL_FLOAT, false, Constants.BYTES_PER_COORD, 0)
        GLES20.glUniform4f(colorUniform, 1.0f, 0.0f, 0.0f, 1.0f)
        GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, modelViewProjections, 0)
        GLES20.glUniform1f(pointSizeUniform, 5.0f) // Set the size of Point to 5.
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, pointsNum)
        GLES20.glDisableVertexAttribArray(colorUniform)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    private fun ShaderPojo.drawTriangles() {
        // Draw triangles.
        GLES20.glEnableVertexAttribArray(colorUniform)

        // Clear the color and use the texture color to draw triangles.
        GLES20.glUniform4f(colorUniform, 0.0f, 0.0f, 0.0f, 0.0f)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, triangleId)

        // The number of input triangle points
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, trianglesNum * 3, GLES20.GL_UNSIGNED_INT, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
        GLES20.glDisableVertexAttribArray(colorUniform)
        GLES20.glDisableVertexAttribArray(textureCoordAttribute)
        GLES20.glDisableVertexAttribArray(positionAttribute)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
    }
}
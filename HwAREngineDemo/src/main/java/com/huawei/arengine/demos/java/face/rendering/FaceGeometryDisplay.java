/**
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huawei.arengine.demos.java.face.rendering;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import com.huawei.arengine.demos.common.ShaderUtil;
import com.huawei.hiar.ARCamera;
import com.huawei.hiar.ARFace;
import com.huawei.hiar.ARFaceGeometry;
import com.huawei.hiar.ARPose;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Get the facial geometric data and render the data on the screen.
 *
 * @author HW
 * @since 2020-03-24
 */
public class FaceGeometryDisplay {
    private static final String TAG = FaceGeometryDisplay.class.getSimpleName();

    private static final String LS = System.lineSeparator();

    private static final String FACE_GEOMETRY_VERTEX =
        "attribute vec2 inTexCoord;" + LS
        + "uniform mat4 inMVPMatrix;" + LS
        + "uniform float inPointSize;" + LS
        + "attribute vec4 inPosition;" + LS
        + "uniform vec4 inColor;" + LS
        + "varying vec4 varAmbient;" + LS
        + "varying vec4 varColor;" + LS
        + "varying vec2 varCoord;" + LS
        + "void main() {" + LS
        + "    varAmbient = vec4(1.0, 1.0, 1.0, 1.0);" + LS
        + "    gl_Position = inMVPMatrix * vec4(inPosition.xyz, 1.0);" + LS
        + "    varColor = inColor;" + LS
        + "    gl_PointSize = inPointSize;" + LS
        + "    varCoord = inTexCoord;" + LS
        + "}";

    private static final String FACE_GEOMETRY_FRAGMENT =
        "precision mediump float;" + LS
        + "uniform sampler2D inTexture;" + LS
        + "varying vec4 varColor;" + LS
        + "varying vec2 varCoord;" + LS
        + "varying vec4 varAmbient;" + LS
        + "void main() {" + LS
        + "    vec4 objectColor = texture2D(inTexture, vec2(varCoord.x, 1.0 - varCoord.y));" + LS
        + "    if(varColor.x != 0.0) {" + LS
        + "        gl_FragColor = varColor * varAmbient;" + LS
        + "    }" + LS
        + "    else {" + LS
        + "        gl_FragColor = objectColor * varAmbient;" + LS
        + "    }" + LS
        + "}";

    // Number of bytes occupied by each 3D coordinate point.
    // Each floating-point number occupies 4 bytes, and each point has three dimensions.
    private static final int BYTES_PER_POINT = 4 * 3;

    // Number of bytes occupied by each 2D coordinate point.
    private static final int BYTES_PER_COORD = 4 * 2;

    private static final int BUFFER_OBJECT_NUMBER = 2;

    private static final int POSITION_COMPONENTS_NUMBER = 4;

    private static final int TEXCOORD_COMPONENTS_NUMBER = 2;

    private static final float PROJECTION_MATRIX_NEAR = 0.1f;

    private static final float PROJECTION_MATRIX_FAR = 100.0f;

    private int mVerticeId;

    private int mVerticeBufferSize = 8000; // Initialize the size of the vertex VBO.

    private int mTriangleId;

    private int mTriangleBufferSize = 5000; // Initialize the size of the triangle VBO.

    private int mProgram;

    private int mTextureName;

    private int mPositionAttribute;

    private int mColorUniform;

    private int mModelViewProjectionUniform;

    private int mPointSizeUniform;

    private int mTextureUniform;

    private int mTextureCoordAttribute;

    private int mPointsNum = 0;

    private int mTrianglesNum = 0;

    // The size of the MVP matrix is 4 x 4.
    private float[] mModelViewProjections = new float[16];

    /**
     * Initialize the OpenGL ES rendering related to face geometry, including creating the shader program.
     * This method is called when {@link WorldRenderManage#onSurfaceCreated}.
     *
     * @param context Context.
     */
    void init(Context context) {
        ShaderUtil.checkGlError(TAG, "Init start.");
        int[] texNames = new int[1];
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glGenTextures(1, texNames, 0);
        mTextureName = texNames[0];

        int[] buffers = new int[BUFFER_OBJECT_NUMBER];
        GLES20.glGenBuffers(BUFFER_OBJECT_NUMBER, buffers, 0);
        mVerticeId = buffers[0];
        mTriangleId = buffers[1];

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVerticeId);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVerticeBufferSize * BYTES_PER_POINT, null, GLES20.GL_DYNAMIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mTriangleId);

        // Each floating-point number occupies 4 bytes.
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, mTriangleBufferSize * 4, null,
            GLES20.GL_DYNAMIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureName);

        createProgram();
        Bitmap textureBitmap;
        try (InputStream inputStream = context.getAssets().open("face_geometry.png")) {
            textureBitmap = BitmapFactory.decodeStream(inputStream);
        } catch (IllegalArgumentException | IOException e) {
            Log.e(TAG, "Open bitmap error!");
            return;
        }

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, textureBitmap, 0);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        ShaderUtil.checkGlError(TAG, "Init end.");
    }

    private void createProgram() {
        ShaderUtil.checkGlError(TAG, "Create gl program start.");
        mProgram = createGlProgram();
        mPositionAttribute = GLES20.glGetAttribLocation(mProgram, "inPosition");
        mColorUniform = GLES20.glGetUniformLocation(mProgram, "inColor");
        mModelViewProjectionUniform = GLES20.glGetUniformLocation(mProgram, "inMVPMatrix");
        mPointSizeUniform = GLES20.glGetUniformLocation(mProgram, "inPointSize");
        mTextureUniform = GLES20.glGetUniformLocation(mProgram, "inTexture");
        mTextureCoordAttribute = GLES20.glGetAttribLocation(mProgram, "inTexCoord");
        ShaderUtil.checkGlError(TAG, "Create gl program end.");
    }

    private static int createGlProgram() {
        int vertex = loadShader(GLES20.GL_VERTEX_SHADER, FACE_GEOMETRY_VERTEX);
        if (vertex == 0) {
            return 0;
        }
        int fragment = loadShader(GLES20.GL_FRAGMENT_SHADER, FACE_GEOMETRY_FRAGMENT);
        if (fragment == 0) {
            return 0;
        }
        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertex);
            GLES20.glAttachShader(program, fragment);
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Could not link program: " + GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    private static int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (0 != shader) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e(TAG, "glError: Could not compile shader " + shaderType);
                Log.e(TAG, "GLES20 Error: " + GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    /**
     * Update the face geometric data in the buffer.
     * This method is called when {@link WorldRenderManage#onDrawFrame}.
     *
     * @param camera ARCamera.
     * @param face ARFace.
     */
    public void onDrawFrame(ARCamera camera, ARFace face) {
        ARFaceGeometry faceGeometry = face.getFaceGeometry();
        updateFaceGeometryData(faceGeometry);
        updateModelViewProjectionData(camera, face);
        drawFaceGeometry();
        faceGeometry.release();
    }

    private void updateFaceGeometryData(ARFaceGeometry faceGeometry) {
        ShaderUtil.checkGlError(TAG, "Before update data.");
        FloatBuffer faceVertices = faceGeometry.getVertices();

        // Obtain the number of geometric vertices of a face.
        mPointsNum = faceVertices.limit() / 3;

        FloatBuffer textureCoordinates = faceGeometry.getTextureCoordinates();

        // Obtain the number of geometric texture coordinates of the
        // face (the texture coordinates are two-dimensional).
        int texNum = textureCoordinates.limit() / 2;
        Log.d(TAG, "Update face geometry data: texture coordinates size:" + texNum);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVerticeId);
        if (mVerticeBufferSize < (mPointsNum + texNum) * BYTES_PER_POINT) {
            while (mVerticeBufferSize < (mPointsNum + texNum) * BYTES_PER_POINT) {
                // If the capacity of the vertex VBO buffer is insufficient, expand the capacity.
                mVerticeBufferSize *= 2;
            }
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVerticeBufferSize, null, GLES20.GL_DYNAMIC_DRAW);
        }
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, mPointsNum * BYTES_PER_POINT, faceVertices);

        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, mPointsNum * BYTES_PER_POINT, texNum * BYTES_PER_COORD,
            textureCoordinates);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        mTrianglesNum = faceGeometry.getTriangleCount();
        IntBuffer faceTriangleIndices = faceGeometry.getTriangleIndices();
        Log.d(TAG, "update face geometry data: faceTriangleIndices.size: " + faceTriangleIndices.limit());

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mTriangleId);
        if (mTriangleBufferSize < mTrianglesNum * BYTES_PER_POINT) {
            while (mTriangleBufferSize < mTrianglesNum * BYTES_PER_POINT) {
                // If the capacity of the vertex VBO buffer is insufficient, expand the capacity.
                mTriangleBufferSize *= 2;
            }
            GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, mTriangleBufferSize, null, GLES20.GL_DYNAMIC_DRAW);
        }
        GLES20.glBufferSubData(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0, mTrianglesNum * BYTES_PER_POINT, faceTriangleIndices);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        ShaderUtil.checkGlError(TAG, "After update data.");
    }

    private void updateModelViewProjectionData(ARCamera camera, ARFace face) {
        // The size of the projection matrix is 4 * 4.
        float[] projectionMatrix = new float[16];
        camera.getProjectionMatrix(projectionMatrix, 0, PROJECTION_MATRIX_NEAR, PROJECTION_MATRIX_FAR);
        ARPose facePose = face.getPose();

        // The size of viewMatrix is 4 * 4.
        float[] facePoseViewMatrix = new float[16];

        facePose.toMatrix(facePoseViewMatrix, 0);
        Matrix.multiplyMM(mModelViewProjections, 0, projectionMatrix, 0, facePoseViewMatrix, 0);
    }

    /**
     * Draw face geometrical features. This method is called on each frame.
     */
    private void drawFaceGeometry() {
        ShaderUtil.checkGlError(TAG, "Before draw.");
        Log.d(TAG, "Draw face geometry: mPointsNum: " + mPointsNum + " mTrianglesNum: " + mTrianglesNum);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureName);
        GLES20.glUniform1i(mTextureUniform, 0);
        ShaderUtil.checkGlError(TAG, "Init texture.");

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        // Draw point.
        GLES20.glUseProgram(mProgram);
        GLES20.glEnableVertexAttribArray(mPositionAttribute);
        GLES20.glEnableVertexAttribArray(mTextureCoordAttribute);
        GLES20.glEnableVertexAttribArray(mColorUniform);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVerticeId);
        GLES20.glVertexAttribPointer(mPositionAttribute, POSITION_COMPONENTS_NUMBER, GLES20.GL_FLOAT, false,
            BYTES_PER_POINT, 0);
        GLES20.glVertexAttribPointer(mTextureCoordAttribute, TEXCOORD_COMPONENTS_NUMBER, GLES20.GL_FLOAT, false,
            BYTES_PER_COORD, 0);
        GLES20.glUniform4f(mColorUniform, 1.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glUniformMatrix4fv(mModelViewProjectionUniform, 1, false, mModelViewProjections, 0);
        GLES20.glUniform1f(mPointSizeUniform, 5.0f); // Set the size of Point to 5.
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, mPointsNum);
        GLES20.glDisableVertexAttribArray(mColorUniform);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        ShaderUtil.checkGlError(TAG, "Draw point.");

        // Draw triangles.
        GLES20.glEnableVertexAttribArray(mColorUniform);

        // Clear the color and use the texture color to draw triangles.
        GLES20.glUniform4f(mColorUniform, 0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mTriangleId);

        // The number of input triangle points
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mTrianglesNum * 3, GLES20.GL_UNSIGNED_INT, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        GLES20.glDisableVertexAttribArray(mColorUniform);
        ShaderUtil.checkGlError(TAG, "Draw triangles.");

        GLES20.glDisableVertexAttribArray(mTextureCoordAttribute);
        GLES20.glDisableVertexAttribArray(mPositionAttribute);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        ShaderUtil.checkGlError(TAG, "Draw after.");
    }
}
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

package com.huawei.arengine.demos.common;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

import com.huawei.hiar.ARFrame;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * This is a common class for drawing camera textures that you can use to display camera images on the screen.
 *
 * @author hw
 * @since 2020-03-25
 */
public class TextureDisplay {
    private static final String TAG = TextureDisplay.class.getSimpleName();

    private static final String LS = System.lineSeparator();

    private static final String BASE_FRAGMENT =
        "#extension GL_OES_EGL_image_external : require" + LS
        + "precision mediump float;" + LS
        + "varying vec2 textureCoordinate;" + LS
        + "uniform samplerExternalOES vTexture;" + LS
        + "void main() {" + LS
        + "    gl_FragColor = texture2D(vTexture, textureCoordinate );" + LS
        + "}";

    private static final String BASE_VERTEX =
        "attribute vec4 vPosition;" + LS
        + "attribute vec2 vCoord;" + LS
        + "uniform mat4 vMatrix;" + LS
        + "uniform mat4 vCoordMatrix;" + LS
        + "varying vec2 textureCoordinate;" + LS
        + "void main(){" + LS
        + "    gl_Position = vMatrix*vPosition;" + LS
        + "    textureCoordinate = (vCoordMatrix*vec4(vCoord,0,1)).xy;" + LS
        + "}";

    // Coordinates of a vertex.
    private static final float[] POS = {-1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, -1.0f};

    // Texture coordinates.
    private static final float[] COORD = {0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f};

    private static final int MATRIX_SIZE = 16;

    private static final float RGB_CLEAR_VALUE = 0.8157f;

    private int mExternalTextureId;

    private int mProgram;

    private int mPosition;

    private int mCoord;

    private int mMatrix;

    private int mTexture;

    private int mCoordMatrix;

    private FloatBuffer mVerBuffer;

    private FloatBuffer mTexTransformedBuffer;

    private FloatBuffer mTexBuffer;

    private float[] mProjectionMatrix = new float[MATRIX_SIZE];

    private float[] coordMatrixs;

    /**
     * The constructor is a texture rendering utility class, used to create a texture rendering object.
     */
    public TextureDisplay() {
        coordMatrixs = MatrixUtil.getOriginalMatrix();
        initBuffers();
    }

    /**
     * This method is called when {@link android.opengl.GLSurfaceView.Renderer#onSurfaceChanged}
     * to update the projection matrix.
     *
     * @param width Width.
     * @param height Height
     */
    public void onSurfaceChanged(int width, int height) {
        MatrixUtil.getProjectionMatrix(mProjectionMatrix, width, height);
    }

    /**
     * This method is called when {@link android.opengl.GLSurfaceView.Renderer#onSurfaceCreated}
     * to initialize the texture ID and create the OpenGL ES shader program.
     */
    public void init() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mExternalTextureId = textures[0];
        generateExternalTexture();
        createProgram();
    }

    /**
     * If the texture ID has been created externally, this method is called when
     * {@link android.opengl.GLSurfaceView.Renderer#onSurfaceCreated}.
     *
     * @param textureId Texture id.
     */
    public void init(int textureId) {
        mExternalTextureId = textureId;
        generateExternalTexture();
        createProgram();
    }

    /**
     * Obtain the texture ID.
     *
     * @return Texture id.
     */
    public int getExternalTextureId() {
        return mExternalTextureId;
    }

    /**
     * Render each frame. This method is called when {@link android.opengl.GLSurfaceView.Renderer#onDrawFrame}.
     *
     * @param frame ARFrame
     */
    public void onDrawFrame(ARFrame frame) {
        ShaderUtil.checkGlError(TAG, "On draw frame start.");
        if (frame == null) {
            return;
        }
        if (frame.hasDisplayGeometryChanged()) {
            frame.transformDisplayUvCoords(mTexBuffer, mTexTransformedBuffer);
        }
        clear();

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthMask(false);

        GLES20.glUseProgram(mProgram);

        // Set the texture ID.
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mExternalTextureId);

        // Set the projection matrix.
        GLES20.glUniformMatrix4fv(mMatrix, 1, false, mProjectionMatrix, 0);

        GLES20.glUniformMatrix4fv(mCoordMatrix, 1, false, coordMatrixs, 0);

        // Set the vertex.
        GLES20.glEnableVertexAttribArray(mPosition);
        GLES20.glVertexAttribPointer(mPosition, 2, GLES20.GL_FLOAT, false, 0, mVerBuffer);

        // Set the texture coordinates.
        GLES20.glEnableVertexAttribArray(mCoord);
        GLES20.glVertexAttribPointer(mCoord, 2, GLES20.GL_FLOAT, false, 0, mTexTransformedBuffer);

        // Number of vertices.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mPosition);
        GLES20.glDisableVertexAttribArray(mCoord);

        GLES20.glDepthMask(true);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        ShaderUtil.checkGlError(TAG, "On draw frame end.");
    }

    private void generateExternalTexture() {
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mExternalTextureId);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);
    }

    private void createProgram() {
        mProgram = createGlProgram();
        mPosition = GLES20.glGetAttribLocation(mProgram, "vPosition");
        mCoord = GLES20.glGetAttribLocation(mProgram, "vCoord");
        mMatrix = GLES20.glGetUniformLocation(mProgram, "vMatrix");
        mTexture = GLES20.glGetUniformLocation(mProgram, "vTexture");
        mCoordMatrix = GLES20.glGetUniformLocation(mProgram, "vCoordMatrix");
    }

    private static int createGlProgram() {
        int vertex = loadShader(GLES20.GL_VERTEX_SHADER, BASE_VERTEX);
        if (vertex == 0) {
            return 0;
        }
        int fragment = loadShader(GLES20.GL_FRAGMENT_SHADER, BASE_FRAGMENT);
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
                glError("Could not link program:" + GLES20.glGetProgramInfoLog(program));
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
                glError("Could not compile shader:" + shaderType);
                glError("GLES20 Error:" + GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    private static void glError(Object index) {
        Log.e(TAG, "glError: " + index);
    }

    private void initBuffers() {
        // Initialize the size of the vertex buffer.
        ByteBuffer byteBufferForVer = ByteBuffer.allocateDirect(32);
        byteBufferForVer.order(ByteOrder.nativeOrder());
        mVerBuffer = byteBufferForVer.asFloatBuffer();
        mVerBuffer.put(POS);
        mVerBuffer.position(0);

        // Initialize the size of the texture buffer.
        ByteBuffer byteBufferForTex = ByteBuffer.allocateDirect(32);
        byteBufferForTex.order(ByteOrder.nativeOrder());
        mTexBuffer = byteBufferForTex.asFloatBuffer();
        mTexBuffer.put(COORD);
        mTexBuffer.position(0);

        // Initialize the size of the transformed texture buffer.
        ByteBuffer byteBufferForTransformedTex = ByteBuffer.allocateDirect(32);
        byteBufferForTransformedTex.order(ByteOrder.nativeOrder());
        mTexTransformedBuffer = byteBufferForTransformedTex.asFloatBuffer();
    }

    /**
     * Clear canvas.
     */
    private void clear() {
        GLES20.glClearColor(RGB_CLEAR_VALUE, RGB_CLEAR_VALUE, RGB_CLEAR_VALUE, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);
    }
}
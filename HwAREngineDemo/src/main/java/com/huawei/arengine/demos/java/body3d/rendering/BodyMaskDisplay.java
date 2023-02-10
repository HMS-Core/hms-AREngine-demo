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

package com.huawei.arengine.demos.java.body3d.rendering;

import android.opengl.GLES20;
import android.opengl.GLES30;

import com.huawei.arengine.demos.common.BaseBackgroundDisplay;
import com.huawei.arengine.demos.common.MatrixUtil;
import com.huawei.arengine.demos.common.ShaderUtil;
import com.huawei.hiar.ARFrame;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Class for custom camera previews, which is used to implement human body drawing based on certain confidence.
 *
 * @author HW
 * @since 2022-08-17
 */
public class BodyMaskDisplay implements BaseBackgroundDisplay {
    private static final String TAG = "BodyMaskDisplay";

    /**
     * Coordinates of a vertex.
     */
    private static final float[] VERTEX_COORDINATE =
        {-1.0f, 1.0f, 0.0f, -1.0f, -1.0f, 0.0f, +1.0f, +1.0f, 0.0f, +1.0f, -1.0f, 0.0f};

    /**
     * Texture coordinates.
     */
    private static final float[] TEXTURE_COORDINATE = {0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f};

    private static final int MATRIX_SIZE = 16;

    private static final float RGB_CLEAR_VALUE = 0.8157f;

    private static final int TEXCOORDS_PER_VERTEX = 2;

    private static final int COORDS_PER_VERTEX = 3;

    private static final int FLOAT_SIZE = 4;

    private static final int NUM_VERTICES_INITIAL = 4;

    private static final int GLDRAW_ARRAYS_COUNT = 4;

    private static final int INVALID_TEXTURE_ID = -1;

    private FloatBuffer mVertexBuffer;

    private FloatBuffer mTexTransformedBuffer;

    private FloatBuffer mTextureBuffer;

    private float[] mProjectionMatrix = new float[MATRIX_SIZE];

    private float[] mCoordMatrixs;

    private int mPosition;

    private int mCoord;

    private int mMatrix;

    private int mCoordMatrix;

    private int mUseMaskParam;

    private int mMaskParam;

    private int mProgram;

    private int mMaskTextureId = INVALID_TEXTURE_ID;

    private int mTextureId = INVALID_TEXTURE_ID;

    /**
     * Constructor.
     */
    public BodyMaskDisplay() {
        mCoordMatrixs = MatrixUtil.getOriginalMatrix();
        initBuffers();
    }

    /**
     * Called when the projection matrix is updated {@link android.opengl.GLSurfaceView.Renderer#onSurfaceChanged}.
     *
     * @param width Width.
     * @param height Height
     */
    @Override
    public void onSurfaceChanged(int width, int height) {
        MatrixUtil.getProjectionMatrix(mProjectionMatrix, width, height);
    }

    /**
     * Initialize the texture ID and create the OpenGL ES shader program.
     */
    @Override
    public void init() {
        ShaderUtil.checkGlError(TAG, "Init start.");
        int[] texture = new int[1];
        GLES20.glGenTextures(1, texture, 0);
        mTextureId = texture[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
        setGlTextureParameters();

        int[] maskTexture = new int[1];
        GLES20.glGenTextures(1, maskTexture, 0);
        mMaskTextureId = maskTexture[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mMaskTextureId);
        setGlTextureParameters();
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        createProgram();
        ShaderUtil.checkGlError(TAG, "Init end.");
    }

    @Override
    public void init(int textureId) {
    }

    /**
     * Set texture parameters.
     */
    public void setGlTextureParameters() {
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
    }

    @Override
    public int getExternalTextureId() {
        return mTextureId;
    }

    /**
     * Render each frame, which is called when {@link android.opengl.GLSurfaceView.Renderer#onDrawFrame}.
     *
     * @param frame Frame.
     */
    @Override
    public void onDrawFrame(ARFrame frame) {
        onDrawFrame(frame, null, 0, 0);
    }

    /**
     * Render each frame, which is called when {@link android.opengl.GLSurfaceView.Renderer#onDrawFrame}.
     *
     * @param frame Frame.
     * @param maskBuffer Mask buffer.
     * @param maskWidth Mask width.
     * @param maskHeight Mask height.
     */
    public void onDrawFrame(ARFrame frame, FloatBuffer maskBuffer, int maskWidth, int maskHeight) {
        ShaderUtil.checkGlError(TAG, "On draw frame start.");
        if (frame == null) {
            return;
        }
        if (frame.hasDisplayGeometryChanged()) {
            frame.transformDisplayUvCoords(mTextureBuffer, mTexTransformedBuffer);
        }
        clear();
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthMask(false);
        GLES20.glUseProgram(mProgram);

        // Set the texture ID.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mMaskTextureId);
        GLES20.glUniform1i(mMaskParam, 1);

        if (maskBuffer != null) {
            GLES20.glUniform1i(mUseMaskParam, 1);
            GLES30.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES30.GL_R32F, maskWidth, maskHeight, 0, GLES30.GL_RED,
                GLES20.GL_FLOAT, maskBuffer);
        } else {
            GLES20.glUniform1i(mUseMaskParam, 0);
        }

        // Set the projection matrix.
        GLES20.glUniformMatrix4fv(mMatrix, 1, false, mProjectionMatrix, 0);
        GLES20.glUniformMatrix4fv(mCoordMatrix, 1, false, mCoordMatrixs, 0);

        // Set the vertex.
        GLES20.glEnableVertexAttribArray(mPosition);
        GLES20.glVertexAttribPointer(mPosition, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, mVertexBuffer);

        // Set the texture coordinates.
        GLES20.glEnableVertexAttribArray(mCoord);
        GLES20.glVertexAttribPointer(mCoord, TEXCOORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, mTexTransformedBuffer);

        // Number of vertices.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, GLDRAW_ARRAYS_COUNT);
        GLES20.glDisableVertexAttribArray(mPosition);
        GLES20.glDisableVertexAttribArray(mCoord);

        GLES20.glDepthMask(true);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        ShaderUtil.checkGlError(TAG, "On draw frame end.");
    }

    /**
     * Obtain the texture ID.
     *
     * @return Texture Id.
     */
    public int getTextureId() {
        return mTextureId;
    }

    private void createProgram() {
        ShaderUtil.checkGlError(TAG, "Create gl program start.");
        mProgram = BodyShaderUtil.createShadowGlProgram();
        mPosition = GLES20.glGetAttribLocation(mProgram, "a_Position");
        mCoord = GLES20.glGetAttribLocation(mProgram, "a_TexCoord");
        mMatrix = GLES20.glGetUniformLocation(mProgram, "vMatrix");
        mCoordMatrix = GLES20.glGetUniformLocation(mProgram, "vCoordMatrix");
        mMaskParam = GLES20.glGetUniformLocation(mProgram, "u_Mask");
        mUseMaskParam = GLES20.glGetUniformLocation(mProgram, "u_UseMask");
        ShaderUtil.checkGlError(TAG, "Create gl program end.");
    }

    private void initBuffers() {
        // Initialize the size of the vertex buffer.
        ByteBuffer byteBufferForVer = ByteBuffer.allocateDirect(VERTEX_COORDINATE.length * FLOAT_SIZE);
        byteBufferForVer.order(ByteOrder.nativeOrder());
        mVertexBuffer = byteBufferForVer.asFloatBuffer();
        mVertexBuffer.put(VERTEX_COORDINATE);
        mVertexBuffer.position(0);

        // Initialize the size of the texture buffer.
        ByteBuffer byteBufferForTex =
            ByteBuffer.allocateDirect(NUM_VERTICES_INITIAL * TEXCOORDS_PER_VERTEX * FLOAT_SIZE);
        byteBufferForTex.order(ByteOrder.nativeOrder());
        mTextureBuffer = byteBufferForTex.asFloatBuffer();
        mTextureBuffer.put(TEXTURE_COORDINATE);
        mTextureBuffer.position(0);

        // Initialize the size of the converted texture buffer.
        ByteBuffer byteBufferForTransformedTex =
            ByteBuffer.allocateDirect(NUM_VERTICES_INITIAL * TEXCOORDS_PER_VERTEX * FLOAT_SIZE);
        byteBufferForTransformedTex.order(ByteOrder.nativeOrder());
        mTexTransformedBuffer = byteBufferForTransformedTex.asFloatBuffer();
    }

    /**
     * Clear the canvas.
     */
    private void clear() {
        GLES20.glClearColor(RGB_CLEAR_VALUE, RGB_CLEAR_VALUE, RGB_CLEAR_VALUE, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);
    }
}

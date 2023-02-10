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

package com.huawei.arengine.demos.java.augmentedimage.rendering;

import android.opengl.GLES20;
import android.opengl.Matrix;

import com.huawei.arengine.demos.common.ShaderUtil;
import com.huawei.hiar.ARAugmentedImage;

import java.nio.FloatBuffer;

/**
 * Draw the border of the augmented image based on the pose of the center,
 * and the width and height of the augmented image.
 *
 * @author HW
 * @since 2021-02-04
 */
public class ImageKeyLineDisplay implements AugmentedImageComponentDisplay {
    private static final String TAG = ImageKeyLineDisplay.class.getSimpleName();

    /**
     * 3D coordinates. The coordinates have four components (x, y, z, and alpha).
     * One float occupies 4 bytes.
     */
    private static final int BYTES_PER_POINT = 4 * 4;

    private static final int INITIAL_BUFFER_POINTS = 150;

    private static final int COORDINATE_DIMENSION = 3;

    private static final int BYTES_PER_CORNER = 4;

    private float[] mCornerPointCoordinates;

    private int mVboSize = INITIAL_BUFFER_POINTS * BYTES_PER_POINT;

    private int mModelViewProjectionMatrix;

    private int mProgram;

    private int mPosition;

    private int mColor;

    private int mVbo;

    private int mNumPoints = 0;

    private int mIndex = 0;

    /**
     * Create and build the augmented image shader on the OpenGL thread.
     */
    @Override
    public void init() {
        ShaderUtil.checkGlError(TAG, "Init start.");
        int[] vboBuffers = new int[1];
        GLES20.glGenBuffers(1, vboBuffers, 0);
        mVbo = vboBuffers[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo);
        createProgram();
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVboSize, null, GLES20.GL_DYNAMIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        ShaderUtil.checkGlError(TAG, "Init end.");
    }

    private void createProgram() {
        ShaderUtil.checkGlError(TAG, "Create imageKeyLine program start.");
        mProgram = ShaderUtil.getGlProgram();
        mPosition = GLES20.glGetAttribLocation(mProgram, "inPosition");
        mColor = GLES20.glGetUniformLocation(mProgram, "inColor");
        mModelViewProjectionMatrix = GLES20.glGetUniformLocation(mProgram, "inMVPMatrix");
        ShaderUtil.checkGlError(TAG, "Create imageKeyLine program end.");
    }

    /**
     * Draw the borders of the augmented image.
     *
     * @param augmentedImage AugmentedImage object.
     * @param viewMatrix View matrix.
     * @param projectionMatrix AR camera projection matrix.
     */
    @Override
    public void onDrawFrame(ARAugmentedImage augmentedImage, float[] viewMatrix, float[] projectionMatrix) {
        float[] vpMatrix = new float[BYTES_PER_POINT];
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        draw(augmentedImage, vpMatrix);
    }

    /**
     * Draw borders to augment the image.
     *
     * @param augmentedImage AugmentedImage object.
     * @param viewProjectionMatrix View projection matrix.
     */
    private void draw(ARAugmentedImage augmentedImage, float[] viewProjectionMatrix) {
        mCornerPointCoordinates = new float[BYTES_PER_CORNER * 4];
        for (CornerType cornerType : CornerType.values()) {
            createImageCorner(augmentedImage, cornerType);
        }

        updateImageKeyLineData(mCornerPointCoordinates);
        drawImageLine(viewProjectionMatrix);
        mCornerPointCoordinates = null;
        mIndex = 0;
    }

    /**
     * Obtain the four vertexes of the augmented image.
     *
     * @param augmentedImage AugmentedImage object.
     * @param cornerType Corner type (upper left, lower left, upper right, or lower right).
     */
    private void createImageCorner(ARAugmentedImage augmentedImage, CornerType cornerType) {
        mCornerPointCoordinates = createImageCorner(augmentedImage, cornerType, mIndex,
            mCornerPointCoordinates);
        mIndex++;
    }

    private void updateImageKeyLineData(float[] cornerPoint) {
        // Total number of coordinates.
        mNumPoints = cornerPoint.length / 4;
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo);
        int vboSize = mVboSize;
        if (vboSize < mNumPoints * BYTES_PER_POINT) {
            while (vboSize < mNumPoints * BYTES_PER_POINT) {
                // If the size of VBO is insufficient to accommodate the new vertex, resize the VBO.
                vboSize = vboSize * 2;
            }
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vboSize, null, GLES20.GL_DYNAMIC_DRAW);
        }
        FloatBuffer cornerPointBuffer = FloatBuffer.wrap(cornerPoint);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, mNumPoints * BYTES_PER_POINT, cornerPointBuffer);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    /**
     * Draw the image border.
     *
     * @param viewProjectionMatrix View projection matrix.
     */
    private void drawImageLine(float[] viewProjectionMatrix) {
        ShaderUtil.checkGlError(TAG, "Draw image box start.");
        GLES20.glUseProgram(mProgram);
        GLES20.glEnableVertexAttribArray(mPosition);
        GLES20.glEnableVertexAttribArray(mColor);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo);
        GLES20.glVertexAttribPointer(
            mPosition, COORDINATE_DIMENSION, GLES20.GL_FLOAT, false, BYTES_PER_POINT, 0);
        GLES20.glUniform4f(mColor, 0.56f, 0.93f, 0.56f, 0.5f);
        GLES20.glUniformMatrix4fv(mModelViewProjectionMatrix, 1, false, viewProjectionMatrix, 0);

        // Set the width of a rendering stroke.
        GLES20.glLineWidth(5.0f);
        GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, mNumPoints);
        GLES20.glDisableVertexAttribArray(mPosition);
        GLES20.glDisableVertexAttribArray(mColor);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        ShaderUtil.checkGlError(TAG, "Draw image box end.");
    }
}

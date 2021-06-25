/**
 * Copyright 2021. Huawei Technologies Co., Ltd. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.arengine.demos.java.augmentedimage.rendering;

import android.opengl.GLES20;
import android.opengl.Matrix;

import com.huawei.arengine.demos.common.ShaderUtil;
import com.huawei.hiar.ARAugmentedImage;
import com.huawei.hiar.ARPose;

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

    /**
     * 0.5 indicates half of the edge length.
     * The four corners of an image can be obtained by using this parameter and the enums.
     */
    private static final float[] COEFFICIENTS = {0.5f, 0.5f};

    private float[] cornerPointCoordinates;

    private int mVboSize = INITIAL_BUFFER_POINTS * BYTES_PER_POINT;

    private int mModelViewProjectionMatrix;

    private int mProgram;

    private int mPosition;

    private int mColor;

    private int mVbo;

    private int mNumPoints = 0;

    private int index = 0;

    /**
     * Create and build the augmented image shader on the OpenGL thread.
     */
    @Override
    public void init() {
        ShaderUtil.checkGlError(TAG, "Init start.");
        int[] buffers = new int[1];
        GLES20.glGenBuffers(1, buffers, 0);
        mVbo = buffers[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo);
        createProgram();
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVboSize, null, GLES20.GL_DYNAMIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        ShaderUtil.checkGlError(TAG, "Init end.");
    }

    private void createProgram() {
        ShaderUtil.checkGlError(TAG, "Create imageKeyLine program start.");
        mProgram = ImageShaderUtil.getImageKeyMsgProgram();
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
        cornerPointCoordinates = new float[BYTES_PER_CORNER * 4];
        for (CornerType cornerType : CornerType.values()) {
            createImageCorner(augmentedImage, cornerType);
        }

        updateImageKeyLineData(cornerPointCoordinates);
        drawImageLine(viewProjectionMatrix);
        cornerPointCoordinates = null;
        index = 0;
    }

    /**
     * Obtain the four vertexes of the augmented image.
     *
     * @param augmentedImage AugmentedImage object.
     * @param cornerType Corner type (upper left, lower left, upper right, or lower right).
     */
    private void createImageCorner(ARAugmentedImage augmentedImage, CornerType cornerType) {
        ARPose localBoundaryPose;
        float[] coefficient = new float[COEFFICIENTS.length];
        switch (cornerType) {
            case LOWER_RIGHT:
                // Generate the point coordinate coefficient.
                generateCoefficent(coefficient, 1, 1);
                break;
            case UPPER_LEFT:
                generateCoefficent(coefficient, -1, -1);
                break;
            case UPPER_RIGHT:
                generateCoefficent(coefficient, 1, -1);
                break;
            case LOWER_LEFT:
                generateCoefficent(coefficient, -1, 1);
                break;
            default:
                break;
        }

        localBoundaryPose = ARPose.makeTranslation(coefficient[0] * augmentedImage.getExtentX(), 0.0f,
            coefficient[1] * augmentedImage.getExtentZ());

        ARPose centerPose = augmentedImage.getCenterPose();
        ARPose composeCenterPose;
        int cornerCoordinatePos = index * BYTES_PER_CORNER;
        composeCenterPose = centerPose.compose(localBoundaryPose);
        cornerPointCoordinates[cornerCoordinatePos] = composeCenterPose.tx();
        cornerPointCoordinates[cornerCoordinatePos + 1] = composeCenterPose.ty();
        cornerPointCoordinates[cornerCoordinatePos + 2] = composeCenterPose.tz();
        cornerPointCoordinates[cornerCoordinatePos + 3] = 1.0f;
        index++;
    }

    private void generateCoefficent(float[] coefficient, int coefficentX, int coefficentZ) {
        for (int i = 0; i < coefficient.length; i += 2) {
            coefficient[i] = coefficentX * COEFFICIENTS[i];
            coefficient[i + 1] = coefficentZ * COEFFICIENTS[i + 1];
        }
    }

    private void updateImageKeyLineData(float[] cornerPoints) {
        // Total number of coordinates.
        int mPointsNum = cornerPoints.length / 4;
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo);
        mNumPoints = mPointsNum;
        int vboSize = mVboSize;
        int numPoints = mNumPoints;
        if (vboSize < mNumPoints * BYTES_PER_POINT) {
            while (vboSize < numPoints * BYTES_PER_POINT) {
                // If the size of VBO is insufficient to accommodate the new vertex, resize the VBO.
                vboSize *= 2;
            }
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vboSize, null, GLES20.GL_DYNAMIC_DRAW);
        }
        FloatBuffer cornerPointBuffer = FloatBuffer.wrap(cornerPoints);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, numPoints * BYTES_PER_POINT, cornerPointBuffer);
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

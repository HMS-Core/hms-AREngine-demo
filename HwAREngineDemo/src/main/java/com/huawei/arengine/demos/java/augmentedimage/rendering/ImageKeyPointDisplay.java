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
 * Draw the vertexes and center of the augmented image.
 *
 * @author HW
 * @since 2021-02-04
 */
public class ImageKeyPointDisplay implements AugmentedImageComponentDisplay {
    private static final float[] COEFFICIENTS = {0.5f, 0.5f};

    private static final String TAG = ImageKeyPointDisplay.class.getSimpleName();

    /**
     * 3D coordinates. The coordinates have four components (x, y, z, and alpha).
     * One float occupies 4 bytes.
     */
    private static final int BYTES_PER_POINT = 4 * 4;

    private static final int INITIAL_POINTS_SIZE = 20;

    private static final int BYTES_PER_CORNER = 4;

    private float[] centerPointCoordinates;

    private float[] cornerPointCoordinates;

    private float[] allPointCoordinates;

    private int mVbo;

    private int mVboSize;

    private int mProgram;

    private int mPosition;

    private int mMvpMatrix;

    private int mColor;

    private int mPointSize;

    private int mNumPoints;

    private int index = 0;

    /**
     * Create and build shaders for image keypoints on the OpenGL thread.
     */
    @Override
    public void init() {
        ShaderUtil.checkGlError(TAG, "Init start.");
        int[] buffers = new int[1];
        GLES20.glGenBuffers(1, buffers, 0);
        mVbo = buffers[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo);
        mVboSize = INITIAL_POINTS_SIZE * BYTES_PER_POINT;
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVboSize, null, GLES20.GL_DYNAMIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        createProgram();
        ShaderUtil.checkGlError(TAG, "Init end.");
    }

    private void createProgram() {
        ShaderUtil.checkGlError(TAG, "Create gl program start.");
        mProgram = ImageShaderUtil.getImageKeyMsgProgram();
        mPosition = GLES20.glGetAttribLocation(mProgram, "inPosition");
        mColor = GLES20.glGetUniformLocation(mProgram, "inColor");
        mPointSize = GLES20.glGetUniformLocation(mProgram, "inPointSize");
        mMvpMatrix = GLES20.glGetUniformLocation(mProgram, "inMVPMatrix");
        ShaderUtil.checkGlError(TAG, "Create program end.");
    }

    /**
     * Draw image key points to augment the image.
     *
     * @param augmentedImage Image to be augmented.
     * @param viewMatrix View matrix.
     * @param projectionMatrix Projection matrix.
     */
    @Override
    public void onDrawFrame(ARAugmentedImage augmentedImage, float[] viewMatrix, float[] projectionMatrix) {
        float[] vpMatrix = new float[BYTES_PER_POINT];
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        draw(augmentedImage, vpMatrix);
    }

    private void draw(ARAugmentedImage augmentedImage, float[] viewProjectionMatrix) {
        createImageCenterPoint(augmentedImage);
        updateImageAllPoints(centerPointCoordinates);

        cornerPointCoordinates = new float[BYTES_PER_CORNER * 4];
        for (CornerType cornerType : CornerType.values()) {
            createImageCorner(augmentedImage, cornerType);
        }
        mergeArray(centerPointCoordinates, cornerPointCoordinates);

        updateImageAllPoints(allPointCoordinates);
        drawImageKeyPoint(viewProjectionMatrix);
        cornerPointCoordinates = null;
        index = 0;
    }

    /**
     * Obtain the coordinates of the center of the recognized image and
     * write the coordinates to the centerPointCoordinates array.
     *
     * @param augmentedImage  Augmented image object.
     */
    private void createImageCenterPoint(ARAugmentedImage augmentedImage) {
        centerPointCoordinates = new float[4];
        ARPose centerPose = augmentedImage.getCenterPose();
        centerPointCoordinates[0] = centerPose.tx();
        centerPointCoordinates[1] = centerPose.ty();
        centerPointCoordinates[2] = centerPose.tz();
        centerPointCoordinates[3] = 1.0f;
    }

    /**
     * Obtain the vertex coordinates of the four corners of the augmented image and
     * write them to the cornerPointCoordinates array.
     *
     * @param augmentedImage Augmented image object.
     * @param cornerType Corner type.
     */
    private void createImageCorner(ARAugmentedImage augmentedImage, CornerType cornerType) {
        ARPose localBoundaryPose;
        float[] coefficient = new float[COEFFICIENTS.length];
        switch (cornerType) {
            case LOWER_RIGHT:
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

    /**
     * Combine the obtained central coordinate array and vertex coordinate array
     * into an allPointCoordinates array.
     *
     * @param centerCoordinates Center coordinate array.
     * @param cornerCoordinates Four-corner coordinate array.
     */
    private void mergeArray(float[] centerCoordinates, float[] cornerCoordinates) {
        allPointCoordinates = new float[centerCoordinates.length + cornerCoordinates.length];
        System.arraycopy(centerCoordinates, 0, allPointCoordinates, 0, centerCoordinates.length);
        System.arraycopy(cornerCoordinates, 0, allPointCoordinates, centerCoordinates.length, cornerCoordinates.length);
    }

    /**
     * Update the key point information of the augmented image.
     *
     * @param cornerPoints Array of key points of the augmented image,
     *        including the four vertexes and center.
     */
    private void updateImageAllPoints(float[] cornerPoints) {
        ShaderUtil.checkGlError(TAG, "Update image key point data start.");
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
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, mNumPoints * BYTES_PER_POINT,
            cornerPointBuffer);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        ShaderUtil.checkGlError(TAG, "Update image key point data end.");
    }

    private void drawImageKeyPoint(float[] viewProjectionMatrix) {
        ShaderUtil.checkGlError(TAG, "Draw image key point start.");
        GLES20.glUseProgram(mProgram);
        GLES20.glEnableVertexAttribArray(mPosition);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo);
        GLES20.glVertexAttribPointer(
            mPosition, 4, GLES20.GL_FLOAT, false, BYTES_PER_POINT, 0);

        // Set the color of key points in the image to yellow.
        GLES20.glUniform4f(mColor, 255.0f / 255.0f, 241.0f / 255.0f, 67.0f / 255.0f, 1.0f);
        GLES20.glUniformMatrix4fv(mMvpMatrix, 1, false, viewProjectionMatrix, 0);

        // Set the size of the key points of the image.
        GLES20.glUniform1f(mPointSize, 10.0f);
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, mNumPoints);
        GLES20.glDisableVertexAttribArray(mPosition);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        ShaderUtil.checkGlError(TAG, "Draw image key point end.");
    }
}
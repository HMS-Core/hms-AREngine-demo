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

import android.app.Activity;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.widget.TextView;

import com.huawei.arengine.demos.R;
import com.huawei.arengine.demos.common.ShaderUtil;
import com.huawei.hiar.ARAugmentedImage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Draw the label through the pose of the image center.
 *
 * @author HW
 * @since 2021-02-04
 */
public class ImageLabelDisplay implements AugmentedImageComponentDisplay {
    private static final String TAG = ImageLabelDisplay.class.getSimpleName();

    private static final int IMAGE_ANGLE_MATRIX_SIZE = 4;

    private static final int COORDS_PER_VERTEX = 3;

    private static final float LABEL_WIDTH = 0.1f;

    private static final float LABEL_HEIGHT = 0.05f;

    private static final int TEXTURES_SIZE = 1;

    private static final int MATRIX_SIZE = 16;

    /**
     * Plane angle UV matrix, which is used to adjust the rotation angle ofz
     * the label and vertical/horizontal scaling ratio.
     */
    private final float[] mImageAngleUvMatrix = new float[IMAGE_ANGLE_MATRIX_SIZE];

    private final int[] mTextures = new int[TEXTURES_SIZE];

    private final float[] mModelViewProjectionMatrix = new float[MATRIX_SIZE];

    private final float[] mModelViewMatrix = new float[MATRIX_SIZE];

    /**
     * Allocate a temporary matrix to reduce the number of allocations per frame.
     */
    private final float[] mModelMatrix = new float[MATRIX_SIZE];

    private final Activity mActivity;

    private int mGlPositionParameter;

    private int mProgram;

    private int mGlModelViewProjectionMatrix;

    private int mGlPlaneUvMatrix;

    private int mGlTexture;

    private TextView mLabelTextView;

    /**
     * Pass the activity.
     *
     * @param activity Activity
     */
    public ImageLabelDisplay(Activity activity) {
        mActivity = activity;
    }

    /**
     * Create and build an augmented image shader on the OpenGL thread.
     */
    @Override
    public void init() {
        mLabelTextView = mActivity.findViewById(R.id.image_science_park);
        createProgram();
        ShaderUtil.initLabel(TAG, mTextures, mLabelTextView);
    }

    private void createProgram() {
        ShaderUtil.checkGlError(TAG, "program start.");
        mProgram = ShaderUtil.getLabelProgram();
        mGlTexture = GLES20.glGetUniformLocation(mProgram, "inTexture");
        mGlPositionParameter = GLES20.glGetAttribLocation(mProgram, "inPosXZAlpha");
        mGlPlaneUvMatrix = GLES20.glGetUniformLocation(mProgram, "inPlanUVMatrix");
        mGlModelViewProjectionMatrix = GLES20.glGetUniformLocation(mProgram, "inMVPMatrix");
        ShaderUtil.checkGlError(TAG, "program end.");
    }

    /**
     * Draw an image label to mark the identified image.
     * This method will call {@link AugmentedImageRendererManager#onDrawFrame} in the following cases.
     *
     * @param augmentedImage Augmented image object.
     * @param viewMatrix View matrix.
     * @param projectionMatrix AR camera projection matrix.
     */
    @Override
    public void onDrawFrame(final ARAugmentedImage augmentedImage, float[] viewMatrix, float[] projectionMatrix) {
        prepareForGl();
        updateImageLabelData(augmentedImage);
        drawLabel(viewMatrix, projectionMatrix);
        recycleGl();
    }

    private void prepareForGl() {
        GLES20.glDepthMask(false);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFuncSeparate(GLES20.GL_DST_ALPHA, GLES20.GL_ONE, GLES20.GL_ZERO, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glUseProgram(mProgram);
        GLES20.glEnableVertexAttribArray(mGlPositionParameter);
    }

    /**
     * Update the label information of the augmented image.
     *
     * @param augmentedImage AugmentedImage object.
     */
    private void updateImageLabelData(ARAugmentedImage augmentedImage) {
        float[] imageMatrix = new float[MATRIX_SIZE];
        augmentedImage.getCenterPose().toMatrix(imageMatrix, 0);
        System.arraycopy(imageMatrix, 0, mModelMatrix, 0, MATRIX_SIZE);

        float scaleU = 1.0f / LABEL_WIDTH;

        // Set the value of the UV matrix.
        mImageAngleUvMatrix[0] = scaleU;
        mImageAngleUvMatrix[1] = 0.0f;
        mImageAngleUvMatrix[2] = 0.0f;
        float scaleV = 1.0f / LABEL_HEIGHT;
        mImageAngleUvMatrix[3] = scaleV;

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);
        GLES20.glUniform1i(mGlTexture, 0);
        GLES20.glUniformMatrix2fv(mGlPlaneUvMatrix, 1, false, mImageAngleUvMatrix, 0);
    }

    /**
     * Draw a label.
     *
     * @param cameraViews View matrix.
     * @param cameraProjection AR camera projection matrix.
     */
    private void drawLabel(float[] cameraViews, float[] cameraProjection) {
        ShaderUtil.checkGlError(TAG, "Draw image label start.");
        Matrix.multiplyMM(mModelViewMatrix, 0, cameraViews, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mModelViewProjectionMatrix, 0, cameraProjection, 0, mModelViewMatrix, 0);

        // Obtain half of the width and height as the coordinate data.
        float halfWidth = LABEL_WIDTH / 2.0f;
        float halfHeight = LABEL_HEIGHT / 2.0f;
        float[] vertices = {-halfWidth, -halfHeight, 1, -halfWidth, halfHeight, 1, halfWidth, halfHeight, 1, halfWidth,
            -halfHeight, 1};

        // The size of each float is 4 bytes.
        FloatBuffer vetBuffer =
            ByteBuffer.allocateDirect(4 * vertices.length).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vetBuffer.rewind();
        for (float vertex : vertices) {
            vetBuffer.put(vertex);
        }
        vetBuffer.rewind();
        GLES20.glVertexAttribPointer(mGlPositionParameter, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false,
            4 * COORDS_PER_VERTEX, vetBuffer);

        // Set the sequence of OpenGL drawing points to generate two triangles to form a plane.
        short[] indices = {0, 1, 2, 0, 2, 3};

        // Size of the allocated buffer. The size of each short integer is 2 bytes.
        ShortBuffer idxBuffer =
            ByteBuffer.allocateDirect(2 * indices.length).order(ByteOrder.nativeOrder()).asShortBuffer();
        idxBuffer.rewind();
        for (int i = 0; i < indices.length; ++i) {
            idxBuffer.put(indices[i]);
        }
        idxBuffer.rewind();

        GLES20.glUniformMatrix4fv(mGlModelViewProjectionMatrix, 1, false, mModelViewProjectionMatrix, 0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, idxBuffer.limit(), GLES20.GL_UNSIGNED_SHORT, idxBuffer);
        ShaderUtil.checkGlError(TAG, "Draw image label end.");
    }

    private void recycleGl() {
        GLES20.glDisableVertexAttribArray(mGlPositionParameter);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glDepthMask(true);
    }
}

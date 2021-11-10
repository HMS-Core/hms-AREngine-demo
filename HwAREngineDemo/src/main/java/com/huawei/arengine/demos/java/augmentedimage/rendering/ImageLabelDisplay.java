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

import android.app.Activity;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.view.View;
import android.widget.TextView;

import com.huawei.arengine.demos.R;
import com.huawei.arengine.demos.common.LogUtil;
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

    private static final float MATRIX_SCALE_SX = 1.0f;

    private static final float MATRIX_SCALE_SY = 1.0f;

    private static final int COORDS_PER_VERTEX = 3;

    private static final float LABEL_WIDTH = 0.1f;

    private static final float LABEL_HEIGHT = 0.05f;

    private static final int TEXTURES_SIZE = 1;

    private static final int MATRIX_SIZE = 16;

    /**
     * Plane angle UV matrix, which is used to adjust the rotation angle of
     * the label and vertical/horizontal scaling ratio.
     */
    private final float[] imageAngleUvMatrix = new float[IMAGE_ANGLE_MATRIX_SIZE];

    private final float[] modelViewProjectionMatrix = new float[MATRIX_SIZE];

    private final float[] modelViewMatrix = new float[MATRIX_SIZE];

    /**
     * Allocate a temporary matrix to reduce the number of allocations per frame.
     */
    private final float[] modelMatrix = new float[MATRIX_SIZE];

    private final int[] textures = new int[TEXTURES_SIZE];

    private int mProgram;

    private int glPositionParameter;

    private int glModelViewProjectionMatrix;

    private int glTexture;

    private int glPlaneUvMatrix;

    private final Activity mActivity;

    private TextView labelTextView;

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
        labelTextView = mActivity.findViewById(R.id.image_science_park);
        createProgram();
        initLabel();
    }

    private void initLabel() {
        ShaderUtil.checkGlError(TAG, "Update start.");
        GLES20.glGenTextures(textures.length, textures, 0);

        // Label plane.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        Bitmap labelBitmap = getImageBitmap(labelTextView);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, labelBitmap, 0);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        ShaderUtil.checkGlError(TAG, "Update end.");
    }

    private Bitmap getImageBitmap(TextView view) {
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, labelTextView.getMeasuredWidth(), labelTextView.getMeasuredHeight());
        view.setDrawingCacheEnabled(true);
        view.destroyDrawingCache();
        view.buildDrawingCache();
        Bitmap bitmap = view.getDrawingCache();
        LogUtil.debug(TAG, "Image bitmap create start!");
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.setScale(MATRIX_SCALE_SX, MATRIX_SCALE_SY);
        if (bitmap != null) {
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
        LogUtil.debug(TAG, "Image bitmap create end!");
        return bitmap;
    }

    private void createProgram() {
        ShaderUtil.checkGlError(TAG, "program start.");
        mProgram = ImageShaderUtil.getLabelProgram();
        glTexture = GLES20.glGetUniformLocation(mProgram, "inTexture");
        glPositionParameter = GLES20.glGetAttribLocation(mProgram, "inPosXZAlpha");
        glPlaneUvMatrix = GLES20.glGetUniformLocation(mProgram, "inPlanUVMatrix");
        glModelViewProjectionMatrix = GLES20.glGetUniformLocation(mProgram, "inMVPMatrix");
        ShaderUtil.checkGlError(TAG, "program end.");
    }

    /**
     * Draw an image label to mark the identified image.
     * This method will call {@link AugmentedImageRenderManager#onDrawFrame} in the following cases.
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
        GLES20.glEnableVertexAttribArray(glPositionParameter);
    }

    /**
     * Update the label information of the augmented image.
     *
     * @param augmentedImage AugmentedImage object.
     */
    private void updateImageLabelData(ARAugmentedImage augmentedImage) {
        float[] imageMatrix = new float[MATRIX_SIZE];
        augmentedImage.getCenterPose().toMatrix(imageMatrix, 0);
        System.arraycopy(imageMatrix, 0, modelMatrix, 0, MATRIX_SIZE);

        float scaleU = 1.0f / LABEL_WIDTH;

        // Set the value of the UV matrix.
        imageAngleUvMatrix[0] = scaleU;
        imageAngleUvMatrix[1] = 0.0f;
        imageAngleUvMatrix[2] = 0.0f;
        float scaleV = 1.0f / LABEL_HEIGHT;
        imageAngleUvMatrix[3] = scaleV;

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glUniform1i(glTexture, 0);
        GLES20.glUniformMatrix2fv(glPlaneUvMatrix, 1, false, imageAngleUvMatrix, 0);
    }

    /**
     * Draw a label.
     *
     * @param cameraViews View matrix.
     * @param cameraProjection AR camera projection matrix.
     */
    private void drawLabel(float[] cameraViews, float[] cameraProjection) {
        ShaderUtil.checkGlError(TAG, "Draw image label start.");
        Matrix.multiplyMM(modelViewMatrix, 0, cameraViews, 0, modelMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraProjection, 0, modelViewMatrix, 0);

        // Obtain half of the width and height as the coordinate data.
        float halfWidth = LABEL_WIDTH / 2.0f;
        float halfHeight = LABEL_HEIGHT / 2.0f;
        float[] vertices = {-halfWidth, -halfHeight, 1, -halfWidth, halfHeight, 1, halfWidth, halfHeight, 1, halfWidth,
            -halfHeight, 1};

        // The size of each float is 4 bytes.
        FloatBuffer vetBuffer =
            ByteBuffer.allocateDirect(4 * vertices.length).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vetBuffer.rewind();
        for (int i = 0; i < vertices.length; ++i) {
            vetBuffer.put(vertices[i]);
        }
        vetBuffer.rewind();
        GLES20.glVertexAttribPointer(glPositionParameter, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false,
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

        GLES20.glUniformMatrix4fv(glModelViewProjectionMatrix, 1, false, modelViewProjectionMatrix, 0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, idxBuffer.limit(), GLES20.GL_UNSIGNED_SHORT, idxBuffer);
        ShaderUtil.checkGlError(TAG, "Draw image label end.");
    }

    private void recycleGl() {
        GLES20.glDisableVertexAttribArray(glPositionParameter);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glDepthMask(true);
    }
}

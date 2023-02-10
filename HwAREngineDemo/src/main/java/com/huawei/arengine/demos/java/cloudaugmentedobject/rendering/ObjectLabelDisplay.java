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

package com.huawei.arengine.demos.java.cloudaugmentedobject.rendering;

import android.app.Activity;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.widget.TextView;

import com.huawei.arengine.demos.R;
import com.huawei.arengine.demos.common.LabelDisplayUtil;
import com.huawei.arengine.demos.common.ShaderUtil;
import com.huawei.hiar.ARObject;
import com.huawei.hiar.ARPose;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Collection;

/**
 * Draws a label based on the recognized 3D object ARPose. The label moves with the 3D object.
 *
 * @author HW
 * @since 2021-04-02
 */
public class ObjectLabelDisplay implements ObjectRelatedDisplay {
    private static final String TAG = ObjectLabelDisplay.class.getSimpleName();

    private static final int IMAGE_ANGLE_MATRIX_SIZE = 4;

    private static final int COORDS_PER_VERTEX = 3;

    private static final float LABEL_WIDTH = 0.1f;

    private static final float LABEL_HEIGHT = 0.1f;

    private static final int MATRIX_SIZE = 16;

    private static final int TEXTURES_SIZE = 1;

    /**
     * Plane angle UV matrix, which is used to adjust the rotation angle of the label and
     * vertical/horizontal scaling ratio.
     */
    private final float[] mImageAngleUvMatrix = new float[IMAGE_ANGLE_MATRIX_SIZE];

    private final float[] mModelViewMatrix = new float[MATRIX_SIZE];

    private final float[] mModelViewProjectionMatrix = new float[MATRIX_SIZE];

    /**
     * Allocate a temporary matrix to reduce the number of allocations per frame.
     */
    private final float[] mModelMatrix = new float[MATRIX_SIZE];

    private final int[] mTextures = new int[TEXTURES_SIZE];

    private final Activity mActivity;

    private int mLabelProgram;

    private int mGlPositionParameter;

    private int mGlTexture;

    private int mGlPlaneUvMatrix;

    private int mGlModelViewProjectionMatrix;

    private TextView mLabelTextView;

    /**
     * Pass the activity.
     *
     * @param activity Activity
     */
    public ObjectLabelDisplay(Activity activity) {
        mActivity = activity;
    }

    /**
     * Create and build an augmented image shader on the OpenGL thread.
     */
    @Override
    public void init() {
        mLabelTextView = mActivity.findViewById(R.id.image_ar_object);
        createProgram();
        ShaderUtil.initLabel(TAG, mTextures, mLabelTextView);
    }

    private void createProgram() {
        ShaderUtil.checkGlError(TAG, "program start.");
        mLabelProgram = ShaderUtil.getLabelProgram();
        mGlTexture = GLES20.glGetUniformLocation(mLabelProgram, "inTexture");
        mGlPositionParameter = GLES20.glGetAttribLocation(mLabelProgram, "inPosXZAlpha");
        mGlPlaneUvMatrix = GLES20.glGetUniformLocation(mLabelProgram, "inPlanUVMatrix");
        mGlModelViewProjectionMatrix = GLES20.glGetUniformLocation(mLabelProgram, "inMVPMatrix");
        ShaderUtil.checkGlError(TAG, "program end.");
    }

    /**
     * Draw an image label to mark the recognized 3D object.
     * This method will call {@link ObjectRendererManager#onDrawFrame} in the following cases.
     *
     * @param arObjects 3D object.
     * @param viewMatrix View matrix.
     * @param projectionMatrix AR camera projection matrix.
     * @param cameraPose Location and pose of the current camera.
     */
    @Override
    public void onDrawFrame(Collection<ARObject> arObjects, float[] viewMatrix, float[] projectionMatrix,
        ARPose cameraPose) {
        prepareForGl();
        for (ARObject arObject : arObjects) {
            updateImageLabelData(arObject, cameraPose);
            drawLabel(viewMatrix, projectionMatrix);
        }
        recycleGl();
    }

    /**
     * Update the label of the 3D object.
     *
     * @param arObject AR object.
     * @param cameraPose Location and pose of the current camera.
     */
    private void updateImageLabelData(ARObject arObject, ARPose cameraPose) {
        float[] imageMatrix = getLabelModeMatrix(cameraPose, arObject);
        System.arraycopy(imageMatrix, 0, mModelMatrix, 0, MATRIX_SIZE);

        float scaleU = 1.0f / LABEL_WIDTH;

        // Set the value of the plane angle UV matrix.
        int index = 0;
        mImageAngleUvMatrix[index] = scaleU;
        mImageAngleUvMatrix[++index] = 0.0f;
        mImageAngleUvMatrix[++index] = 0.0f;
        float scaleV = 1.0f / LABEL_HEIGHT;
        mImageAngleUvMatrix[++index] = scaleV;

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);
        GLES20.glUniform1i(mGlTexture, 0);
        GLES20.glUniformMatrix2fv(mGlPlaneUvMatrix, 1, false, mImageAngleUvMatrix, 0);
    }

    private void prepareForGl() {
        GLES20.glDepthMask(false);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFuncSeparate(GLES20.GL_DST_ALPHA, GLES20.GL_ONE, GLES20.GL_ZERO, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glUseProgram(mLabelProgram);
        GLES20.glEnableVertexAttribArray(mGlPositionParameter);
    }

    /**
     * Calculate the rotation angle of the label plane so that the label is displayed upwards.
     *
     * @param cameraDisplayPose Pose of the camera in the world coordinate system.
     * @param target Pose of the target center.
     * @return label Plane matrix.
     */
    private float[] getLabelModeMatrix(ARPose cameraDisplayPose, ARObject target) {
        float[] verticalQuaternion = LabelDisplayUtil.getMeasureQuaternion(cameraDisplayPose, 0);
        ARPose targetCenterPose = target.getCenterPose();
        float[] topPosition = new float[] {targetCenterPose.tx(), targetCenterPose.ty(), targetCenterPose.tz()};
        ARPose measurePose = new ARPose(topPosition, verticalQuaternion);
        float[] planeMatrix = new float[MATRIX_SIZE];
        measurePose.toMatrix(planeMatrix, 0);
        return planeMatrix;
    }

    /**
     * Draw a label.
     *
     * @param cameraViews View matrix.
     * @param cameraProjection AR camera projection matrix.
     */
    private void drawLabel(float[] cameraViews, float[] cameraProjection) {
        ShaderUtil.checkGlError(TAG, "Draw object label start.");
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
        for (short index : indices) {
            idxBuffer.put(index);
        }
        idxBuffer.rewind();

        GLES20.glUniformMatrix4fv(mGlModelViewProjectionMatrix, 1, false, mModelViewProjectionMatrix, 0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, idxBuffer.limit(), GLES20.GL_UNSIGNED_SHORT, idxBuffer);
        ShaderUtil.checkGlError(TAG, "Draw object label end.");
    }

    private void recycleGl() {
        GLES20.glDisableVertexAttribArray(mGlPositionParameter);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glDepthMask(true);
    }
}
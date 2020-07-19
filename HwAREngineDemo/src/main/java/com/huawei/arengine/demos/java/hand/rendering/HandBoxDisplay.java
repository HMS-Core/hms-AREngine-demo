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

package com.huawei.arengine.demos.java.hand.rendering;

import android.opengl.GLES20;
import android.util.Log;

import com.huawei.arengine.demos.common.MatrixUtil;
import com.huawei.arengine.demos.common.ShaderUtil;
import com.huawei.hiar.ARHand;
import com.huawei.hiar.ARTrackable;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Collection;

/**
 * This class shows how to use the hand bounding box. With this class,
 * a rectangular box bounding the hand can be displayed on the screen.
 *
 * @author HW
 * @since 2020-03-16
 */
class HandBoxDisplay implements HandRelatedDisplay {
    private static final String TAG = HandBoxDisplay.class.getSimpleName();

    // Number of bytes occupied by each 3D coordinate. Float data occupies 4 bytes.
    // Each skeleton point represents a 3D coordinate.
    private static final int BYTES_PER_POINT = 4 * 3;
    private static final int INITIAL_BUFFER_POINTS = 150;
    private static final int COORDINATE_DIMENSION = 3;

    private int mVbo;

    private int mVboSize = INITIAL_BUFFER_POINTS * BYTES_PER_POINT;

    private int mProgram;

    private int mPosition;

    private int mColor;

    private int mModelViewProjectionMatrix;

    private int mPointSize;

    private int mNumPoints = 0;

    private float[] mMVPMatrix;

    /**
     * Create and build a shader for the hand gestures on the OpenGL thread,
     * which is called when {@link HandRenderManager#onSurfaceCreated}.
     */
    @Override
    public void init() {
        ShaderUtil.checkGlError(TAG, "Init start.");
        mMVPMatrix = MatrixUtil.getOriginalMatrix();
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
        ShaderUtil.checkGlError(TAG, "Create program start.");
        mProgram = HandShaderUtil.createGlProgram();
        mPosition = GLES20.glGetAttribLocation(mProgram, "inPosition");
        mColor = GLES20.glGetUniformLocation(mProgram, "inColor");
        mPointSize = GLES20.glGetUniformLocation(mProgram, "inPointSize");
        mModelViewProjectionMatrix = GLES20.glGetUniformLocation(mProgram, "inMVPMatrix");
        ShaderUtil.checkGlError(TAG, "Create program start.");
    }

    /**
     * Render the hand bounding box and hand information.
     * This method is called when {@link HandRenderManager#onDrawFrame}.
     *
     * @param hands Hand data.
     * @param projectionMatrix ARCamera projection matrix.
     */
    @Override
    public void onDrawFrame(Collection<ARHand> hands, float[] projectionMatrix) {
        if (hands.size() == 0) {
            return;
        }
        if (projectionMatrix != null) {
            Log.d(TAG, "Camera projection matrix: " + Arrays.toString(projectionMatrix));
        }
        for (ARHand hand : hands) {
            float[] gestureHandBoxPoints = hand.getGestureHandBox();
            if (hand.getTrackingState() == ARTrackable.TrackingState.TRACKING) {
                updateHandBoxData(gestureHandBoxPoints);
                drawHandBox();
            }
        }
    }

    /**
     * Update the coordinates of the hand bounding box.
     *
     * @param gesturePoints Gesture hand box data.
     */
    private void updateHandBoxData(float[] gesturePoints) {
        ShaderUtil.checkGlError(TAG, "Update hand box data start.");
        float[] glGesturePoints = {
            // Get the four coordinates of a rectangular box bounding the hand.
            gesturePoints[0], gesturePoints[1], gesturePoints[2],
            gesturePoints[3], gesturePoints[1], gesturePoints[2],
            gesturePoints[3], gesturePoints[4], gesturePoints[5],
            gesturePoints[0], gesturePoints[4], gesturePoints[5],
        };
        int gesturePointsNum = glGesturePoints.length / COORDINATE_DIMENSION;

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo);

        mNumPoints = gesturePointsNum;
        if (mVboSize < mNumPoints * BYTES_PER_POINT) {
            while (mVboSize < mNumPoints * BYTES_PER_POINT) {
                // If the size of VBO is insufficient to accommodate the new point cloud, resize the VBO.
                mVboSize *= 2;
            }
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVboSize, null, GLES20.GL_DYNAMIC_DRAW);
        }
        Log.d(TAG, "gesture.getGestureHandPointsNum()" + mNumPoints);
        FloatBuffer mVertices = FloatBuffer.wrap(glGesturePoints);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, mNumPoints * BYTES_PER_POINT,
            mVertices);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        ShaderUtil.checkGlError(TAG, "Update hand box data end.");
    }

    /**
     * Render the hand bounding box.
     */
    private void drawHandBox() {
        ShaderUtil.checkGlError(TAG, "Draw hand box start.");
        GLES20.glUseProgram(mProgram);
        GLES20.glEnableVertexAttribArray(mPosition);
        GLES20.glEnableVertexAttribArray(mColor);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo);
        GLES20.glVertexAttribPointer(
            mPosition, COORDINATE_DIMENSION, GLES20.GL_FLOAT, false, BYTES_PER_POINT, 0);
        GLES20.glUniform4f(mColor, 1.0f, 0.0f, 0.0f, 1.0f);

        GLES20.glUniformMatrix4fv(mModelViewProjectionMatrix, 1, false, mMVPMatrix, 0);

        // Set the size of the rendering vertex.
        GLES20.glUniform1f(mPointSize, 50.0f);

        // Set the width of a rendering stroke.
        GLES20.glLineWidth(18.0f);
        GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, mNumPoints);
        GLES20.glDisableVertexAttribArray(mPosition);
        GLES20.glDisableVertexAttribArray(mColor);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        ShaderUtil.checkGlError(TAG, "Draw hand box end.");
    }
}

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

import com.huawei.arengine.demos.common.ShaderUtil;
import com.huawei.hiar.ARHand;

import java.nio.FloatBuffer;
import java.util.Collection;

/**
 * Draw hand skeleton connection line based on the coordinates of the hand skeleton points..
 *
 * @author HW
 * @since 2020-03-09
 */
class HandSkeletonLineDisplay implements HandRelatedDisplay {
    private static final String TAG = HandSkeletonLineDisplay.class.getSimpleName();

    // Number of bytes occupied by each 3D coordinate.
    // Float data occupies 4 bytes. Each skeleton point represents a 3D coordinate
    private static final int BYTES_PER_POINT = 4 * 3;

    private static final int INITIAL_BUFFER_POINTS = 150;

    private static final float JOINT_POINT_SIZE = 100f;

    private int mVbo;

    private int mVboSize = INITIAL_BUFFER_POINTS * BYTES_PER_POINT;

    private int mProgram;

    private int mPosition;

    private int mModelViewProjectionMatrix;

    private int mColor;

    private int mPointSize;

    private int mPointsNum = 0;

    /**
     * Create and build a shader for the hand skeleton line on the OpenGL thread,
     * which is called when {@link HandRenderManager#onSurfaceCreated}.
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
        ShaderUtil.checkGlError(TAG, "Create program start.");
        mProgram = HandShaderUtil.createGlProgram();
        ShaderUtil.checkGlError(TAG, "program");
        mPosition = GLES20.glGetAttribLocation(mProgram, "inPosition");
        mColor = GLES20.glGetUniformLocation(mProgram, "inColor");
        mPointSize = GLES20.glGetUniformLocation(mProgram, "inPointSize");
        mModelViewProjectionMatrix = GLES20.glGetUniformLocation(mProgram, "inMVPMatrix");
        ShaderUtil.checkGlError(TAG, "Create program end.");
    }

    /**
     * Draw hand skeleton connection line.
     * This method is called when {@link HandRenderManager#onDrawFrame}.
     *
     * @param hands ARHand data collection.
     * @param projectionMatrix ProjectionMatrix(4 * 4).
     */
    @Override
    public void onDrawFrame(Collection<ARHand> hands, float[] projectionMatrix) {
        // Verify external input. If the hand data is empty, the projection matrix is empty,
        // or the projection matrix is not 4 * 4, rendering is not performed.
        if (hands.isEmpty() || projectionMatrix == null || projectionMatrix.length != 16) {
            Log.e(TAG, "onDrawFrame Illegal external input!");
            return;
        }
        for (ARHand hand : hands) {
            float[] handSkeletons = hand.getHandskeletonArray();
            int[] handSkeletonConnections = hand.getHandSkeletonConnection();
            if (handSkeletons.length == 0 || handSkeletonConnections.length == 0) {
                continue;
            }
            updateHandSkeletonLinesData(handSkeletons, handSkeletonConnections);
            drawHandSkeletonLine(projectionMatrix);
        }
    }

    /**
     * This method updates the connection data of skeleton points and is called when any frame is updated.
     *
     * @param handSkeletons Bone point data of hand.
     * @param handSkeletonConnection Data of connection between bone points of hand.
     */
    private void updateHandSkeletonLinesData(float[] handSkeletons, int[] handSkeletonConnection) {
        ShaderUtil.checkGlError(TAG, "Update hand skeleton lines data start.");
        int pointsLineNum = 0;

        // Each point is a set of 3D coordinate. Each connection line consists of two points.
        float[] linePoint = new float[handSkeletonConnection.length * 3 * 2];

        // The format of HandSkeletonConnection data is [p0,p1;p0,p3;p0,p5;p1,p2].
        // handSkeletonConnection saves the node indexes. Two indexes obtain a set
        // of connection point data. Therefore, j = j + 2. This loop obtains related
        // coordinates and saves them in linePoint.
        for (int j = 0; j < handSkeletonConnection.length; j += 2) {
            linePoint[pointsLineNum * 3] = handSkeletons[3 * handSkeletonConnection[j]];
            linePoint[pointsLineNum * 3 + 1] = handSkeletons[3 * handSkeletonConnection[j] + 1];
            linePoint[pointsLineNum * 3 + 2] = handSkeletons[3 * handSkeletonConnection[j] + 2];
            linePoint[pointsLineNum * 3 + 3] = handSkeletons[3 * handSkeletonConnection[j + 1]];
            linePoint[pointsLineNum * 3 + 4] = handSkeletons[3 * handSkeletonConnection[j + 1] + 1];
            linePoint[pointsLineNum * 3 + 5] = handSkeletons[3 * handSkeletonConnection[j + 1] + 2];
            pointsLineNum += 2;
        }
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo);
        mPointsNum = pointsLineNum;

        // If the storage space is insufficient, apply for twice the memory each time.
        if (mVboSize < mPointsNum * BYTES_PER_POINT) {
            while (mVboSize < mPointsNum * BYTES_PER_POINT) {
                mVboSize *= 2;
            }
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVboSize, null, GLES20.GL_DYNAMIC_DRAW);
        }
        FloatBuffer linePoints = FloatBuffer.wrap(linePoint);
        Log.d(TAG, "Skeleton skeleton line points num: " + mPointsNum);
        Log.d(TAG, "Skeleton line points: " + linePoints.toString());
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, mPointsNum * BYTES_PER_POINT,
            linePoints);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        ShaderUtil.checkGlError(TAG, "Update hand skeleton lines data end.");
    }

    /**
     * Draw hand skeleton connection line.
     *
     * @param projectionMatrix Projection matrix(4 * 4).
     */
    private void drawHandSkeletonLine(float[] projectionMatrix) {
        ShaderUtil.checkGlError(TAG, "Draw hand skeleton line start.");
        GLES20.glUseProgram(mProgram);
        GLES20.glEnableVertexAttribArray(mPosition);
        GLES20.glEnableVertexAttribArray(mColor);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo);

        // Set the width of the drawn line
        GLES20.glLineWidth(18.0f);

        // Represented each point by 4D coordinates in the shader.
        GLES20.glVertexAttribPointer(
            mPosition, 4, GLES20.GL_FLOAT, false, BYTES_PER_POINT, 0);
        GLES20.glUniform4f(mColor, 0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glUniformMatrix4fv(mModelViewProjectionMatrix, 1, false, projectionMatrix, 0);

        GLES20.glUniform1f(mPointSize, JOINT_POINT_SIZE);

        GLES20.glDrawArrays(GLES20.GL_LINES, 0, mPointsNum);
        GLES20.glDisableVertexAttribArray(mPosition);
        GLES20.glDisableVertexAttribArray(mColor);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        ShaderUtil.checkGlError(TAG, "Draw hand skeleton line end.");
    }
}
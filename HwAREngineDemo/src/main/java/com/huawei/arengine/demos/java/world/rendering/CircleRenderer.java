/**
 * Copyright 2021. Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huawei.arengine.demos.java.world.rendering;

import android.opengl.GLES20;
import android.opengl.Matrix;

import com.huawei.arengine.demos.common.ShaderUtil;
import com.huawei.hiar.ARTarget;

import java.nio.FloatBuffer;

/**
 * A renderer for circles, configured to render a circle identified in target semantic recognition.
 *
 * @author HW
 * @since 2021-07-21
 */
public class CircleRenderer extends TargetRenderer {
    private static final String TAG = CircleRenderer.class.getSimpleName();

    private static final int INITIAL_BUFFER_POINTS = 300;

    private static final int SQUARE_SIZE = 50;

    private static final int ELLIPSE_SIZE = 50;

    private static final int MULTI_NUM = 2;

    private float[] linePoints = new float[SQUARE_SIZE * CUBE_POINT_NUM * FLOATS_PER_POINT * MAX_BOX_NUM];

    @Override
    public int getVboSize() {
        return INITIAL_BUFFER_POINTS * BYTES_PER_POINT;
    }

    @Override
    public void updateVertices(float[] vertices) {
        int idx = 0;
        for (int index = 0; index < ELLIPSE_SIZE * FLOATS_PER_POINT; index += FLOATS_PER_POINT) {
            System.arraycopy(vertices, index, linePoints, idx, FLOATS_PER_POINT);
            idx = idx + FLOATS_PER_POINT;

            int endIdx = (index + FLOATS_PER_POINT) % (ELLIPSE_SIZE * FLOATS_PER_POINT);
            System.arraycopy(vertices, endIdx, linePoints, idx, FLOATS_PER_POINT);
            idx = idx + FLOATS_PER_POINT;
        }

        pointNum = ELLIPSE_SIZE * vertices.length;

        ShaderUtil.checkGlError(TAG, "before update");
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);

        if (vboSize < pointNum * BYTES_PER_POINT) {
            while (vboSize < pointNum * BYTES_PER_POINT) {
                vboSize *= VBO_SIZE_GROWTH_FACTOR;
            }
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vboSize, null, GLES20.GL_DYNAMIC_DRAW);
        }

        FloatBuffer linePointBuffer = FloatBuffer.wrap(linePoints);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, pointNum * BYTES_PER_POINT, linePointBuffer);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        ShaderUtil.checkGlError(TAG, "after update");
    }

    @Override
    public void updateParameters(ARTarget target) {
        arTarget = target;
        float[] boxMatrix = new float[MATRIX_SIZE];
        target.getCenterPose().toMatrix(boxMatrix, 0);

        float[] axisAlignBoundingBox = target.getAxisAlignBoundingBox();
        radius = target.getRadius();

        extentX = Math.abs(axisAlignBoundingBox[OFFSET_X]) * LENGTH_MULTIPLE_NUM;
        extentY = Math.abs(axisAlignBoundingBox[OFFSET_Y]) * LENGTH_MULTIPLE_NUM;
        extentZ = Math.abs(axisAlignBoundingBox[OFFSET_Z]) * LENGTH_MULTIPLE_NUM;

        float[] in = new float[QUATERNION_SIZE];
        int idx = 0;
        float[] res = new float[QUATERNION_SIZE];
        float[] calcVertexes = new float[ELLIPSE_SIZE * FLOATS_PER_POINT];
        for (int index = 0; index < ELLIPSE_SIZE; index++) {
            in[OFFSET_X] = radius * (float) Math.cos(MULTI_NUM * Math.PI / ELLIPSE_SIZE * index);
            in[OFFSET_Y] = 0f;
            in[OFFSET_Z] = radius * (float) Math.sin(MULTI_NUM * Math.PI / ELLIPSE_SIZE * index);
            in[in.length - 1] = W_VALUE;
            Matrix.multiplyMV(res, 0, boxMatrix, 0, in, 0);
            numericalNormalization(idx, res, calcVertexes);
            idx = idx + FLOATS_PER_POINT;
        }

        vertices = calcVertexes.clone();
    }

    @Override
    public String getTargetInfo() {
        int radius = (int) (this.radius * M_TO_CM + LENGTH_BASE_VALUE);
        String labelInfo = getTargetLabelInfo();
        if (!labelInfo.isEmpty()) {
            labelInfo = labelInfo + System.lineSeparator();
        }
        return labelInfo + "RADIUS(cm):" + radius;
    }
}

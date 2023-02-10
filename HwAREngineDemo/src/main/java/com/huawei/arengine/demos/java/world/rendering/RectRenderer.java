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

package com.huawei.arengine.demos.java.world.rendering;

import android.opengl.GLES20;
import android.opengl.Matrix;

import com.huawei.arengine.demos.common.ShaderUtil;
import com.huawei.hiar.ARTarget;

import java.nio.FloatBuffer;

/**
 * A renderer for rectangles, configured to render a rectangle identified in target semantic recognition.
 *
 * @author HW
 * @since 2021-07-21
 */
public class RectRenderer extends TargetRenderer {
    private static final String TAG = RectRenderer.class.getSimpleName();

    private static final int MAX_BOX_NUM = 100;

    private static final int SQUARE_SIZE = 4;

    private static final int INITIAL_BUFFER_POINTS = 150;

    private final float[] linePoints = new float[SQUARE_SIZE * CUBE_POINT_NUM * FLOATS_PER_POINT * MAX_BOX_NUM];

    @Override
    public int getVboSize() {
        return INITIAL_BUFFER_POINTS * BYTES_PER_POINT;
    }

    @Override
    public void updateVertices(float[] vertices) {
        int idx = 0;
        for (int index = 0; index < SQUARE_SIZE * FLOATS_PER_POINT; index += FLOATS_PER_POINT) {
            System.arraycopy(vertices, index, linePoints, idx, FLOATS_PER_POINT);
            idx = idx + FLOATS_PER_POINT;

            int endIdx = (index + FLOATS_PER_POINT) % (SQUARE_SIZE * FLOATS_PER_POINT);
            System.arraycopy(vertices, endIdx, linePoints, idx, FLOATS_PER_POINT);
            idx = idx + FLOATS_PER_POINT;
        }

        pointNum = SQUARE_SIZE * vertices.length;

        ShaderUtil.checkGlError(TAG, "before update");
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        updateBufferSizeIfNeeded();
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

        extentX = Math.abs(axisAlignBoundingBox[OFFSET_X]) * LENGTH_MULTIPLE_NUM;
        extentY = Math.abs(axisAlignBoundingBox[OFFSET_Y]) * LENGTH_MULTIPLE_NUM;
        extentZ = Math.abs(axisAlignBoundingBox[OFFSET_Z]) * LENGTH_MULTIPLE_NUM;

        float[] res = new float[QUATERNION_SIZE];
        float baseX = axisAlignBoundingBox[OFFSET_X];
        float baseY = axisAlignBoundingBox[OFFSET_Y];
        float baseZ = axisAlignBoundingBox[OFFSET_Z];

        float[] in = new float[] {baseX, baseY, -baseZ, W_VALUE};
        Matrix.multiplyMV(res, 0, boxMatrix, 0, in, 0);
        float[] calcVertexes = new float[SQUARE_SIZE * CUBE_POINT_NUM];
        int idx = 0;
        numericalNormalization(idx, res, calcVertexes);
        idx = idx + FLOATS_PER_POINT;

        in = new float[] {baseX, baseY, baseZ, W_VALUE};
        Matrix.multiplyMV(res, 0, boxMatrix, 0, in, 0);
        numericalNormalization(idx, res, calcVertexes);
        idx = idx + FLOATS_PER_POINT;

        in = new float[] {-baseX, baseY, baseZ, W_VALUE};
        Matrix.multiplyMV(res, 0, boxMatrix, 0, in, 0);
        numericalNormalization(idx, res, calcVertexes);
        idx = idx + FLOATS_PER_POINT;

        in = new float[] {-baseX, baseY, -baseZ, W_VALUE};
        Matrix.multiplyMV(res, 0, boxMatrix, 0, in, 0);
        numericalNormalization(idx, res, calcVertexes);

        vertices = calcVertexes.clone();
    }

    @Override
    public String getTargetInfo() {
        int width = (int) (extentX * M_TO_CM + LENGTH_BASE_VALUE);
        int length = (int) (extentZ * M_TO_CM + LENGTH_BASE_VALUE);
        String labelInfo = getTargetLabelInfo();
        if (labelInfo.isEmpty()) {
            labelInfo = "RECT(cm)";
        }
        return labelInfo + System.lineSeparator() + width + "x" + length;
    }
}

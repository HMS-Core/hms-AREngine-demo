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

import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.arengine.demos.common.ShaderUtil;
import com.huawei.arengine.demos.common.WorldShaderUtil;
import com.huawei.hiar.ARTarget;

/**
 * Abstract class of the target semantic renderer.
 *
 * @author HW
 * @since 2021-07-21
 */
public abstract class TargetRenderer {
    /**
     * Offset of the X coordinate.
     */
    protected static final int OFFSET_X = 0;

    /**
     * Offset of the Y coordinate.
     */
    protected static final int OFFSET_Y = 1;

    /**
     * Offset of the Z coordinate.
     */
    protected static final int OFFSET_Z = 2;

    /**
     * Value of the float type.
     */
    protected static final int BYTES_PER_FLOAT = Float.SIZE / 8;

    /**
     * Number of coordinates of each vertex.
     */
    protected static final int FLOATS_PER_POINT = 3;

    /**
     * Memory size occupied by each vertex.
     */
    protected static final int BYTES_PER_POINT = BYTES_PER_FLOAT * FLOATS_PER_POINT;

    /**
     * Maximum number of coordinates of the bounding box.
     */
    protected static final int MAX_BOX_NUM = 100;

    /**
     * Number of vertices for quadrilateral rendering.
     */
    protected static final int CUBE_POINT_NUM = 6;

    /**
     * Matrix size.
     */
    protected static final int MATRIX_SIZE = 16;

    /**
     * Quaternion size.
     */
    protected static final int QUATERNION_SIZE = 4;

    /**
     * The origin of the local coordinate system is in the center of the object. The actual length
     * needs to be multiplied by 2.
     */
    protected static final float LENGTH_MULTIPLE_NUM = 2.0f;

    /**
     * Double the VBO size if it is too small.
     */
    protected static final int VBO_SIZE_GROWTH_FACTOR = 2;

    /**
     * W component.
     */
    protected static final float W_VALUE = 1.0f;

    /**
     * The length is converted from meters to centimeters.
     */
    protected static final float M_TO_CM = 100.0f;

    /**
     * The length deviation is 0.5 cm.
     */
    protected static final float LENGTH_BASE_VALUE = 0.5f;

    private static final String TAG = TargetRenderer.class.getSimpleName();

    private static final float LINE_WIDTH = 7.0f;

    private static final float EPSINON = 0.000001f;

    /**
     * Length in the X direction.
     */
    protected float extentX;

    /**
     * Length in the Y direction.
     */
    protected float extentY;

    /**
     * Length in the Z direction.
     */
    protected float extentZ;

    /**
     * Identified ARTarget object.
     */
    protected ARTarget arTarget;

    /**
     * Circle radius.
     */
    protected float radius = 0.0f;

    /**
     * VBO storage location.
     */
    protected int vbo;

    /**
     * VBO size.
     */
    protected int vboSize;

    /**
     * Vertices information.
     */
    protected float[] vertices = null;

    /**
     * Number of vertices.
     */
    protected int pointNum = 0;

    private int programName;

    private int positionAttribute;

    private int modelViewProjectionUniform;

    private int colorUniform;

    /**
     * Number of vertices.
     */
    public void createOnGlThread() {
        ShaderUtil.checkGlError(TAG, "before create");
        int[] buffer = new int[1];
        GLES20.glGenBuffers(1, buffer, 0);
        vbo = buffer[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);

        vboSize = getVboSize();
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vboSize, null, GLES20.GL_DYNAMIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        ShaderUtil.checkGlError(TAG, "buffer alloc");

        programName = WorldShaderUtil.getPointCloudProgram();
        GLES20.glLinkProgram(programName);
        GLES20.glUseProgram(programName);

        ShaderUtil.checkGlError(TAG, "program");

        positionAttribute = GLES20.glGetAttribLocation(programName, "a_Position");
        colorUniform = GLES20.glGetUniformLocation(programName, "u_Color");
        modelViewProjectionUniform = GLES20.glGetUniformLocation(programName, "u_ModelViewProjection");

        ShaderUtil.checkGlError(TAG, "program params");
    }

    /**
     * Render an image.
     *
     * @param cameraViewMatrix View matrix.
     * @param projectionMatrix Projection matrix.
     */
    public void draw(float[] cameraViewMatrix, float[] projectionMatrix) {
        LogUtil.debug(TAG, "draw start");

        updateVertices(vertices);

        ShaderUtil.checkGlError(TAG, "Before draw");

        float[] modelViewProjections = new float[MATRIX_SIZE];
        Matrix.multiplyMM(modelViewProjections, 0, projectionMatrix, 0, cameraViewMatrix, 0);
        GLES20.glUseProgram(programName);
        GLES20.glEnableVertexAttribArray(positionAttribute);
        GLES20.glEnableVertexAttribArray(colorUniform);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glLineWidth(LINE_WIDTH);
        GLES20.glVertexAttribPointer(positionAttribute, QUATERNION_SIZE, GLES20.GL_FLOAT, false, BYTES_PER_POINT, 0);

        // Set the line color.
        GLES20.glUniform4f(colorUniform, 10.0f / 255.0f, 89.0f / 255.0f, 247.0f / 255.0f, 1.0f);
        GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, modelViewProjections, 0);

        GLES20.glDrawArrays(GLES20.GL_LINES, 0, pointNum);
        GLES20.glDisableVertexAttribArray(positionAttribute);
        GLES20.glDisableVertexAttribArray(colorUniform);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        LogUtil.debug(TAG, "draw end");

        ShaderUtil.checkGlError(TAG, "Draw");
    }

    void updateBufferSizeIfNeeded() {
        if (vboSize < pointNum * BYTES_PER_POINT) {
            while (vboSize < pointNum * BYTES_PER_POINT) {
                vboSize *= VBO_SIZE_GROWTH_FACTOR;
            }
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vboSize, null, GLES20.GL_DYNAMIC_DRAW);
        }
    }

    /**
     * Normalize the vertices data.
     *
     * @param startIndex Start coordinates of the vertex.
     * @param res Coordinate data that needs to be normalized.
     * @param fa Target array.
     */
    protected void numericalNormalization(int startIndex, float[] res, float[] fa) {
        for (int index = 0; index < res.length - 1; index++) {
            if ((fa.length <= (startIndex + index)) || (res.length <= index)) {
                LogUtil.warn(TAG, "numericalNormalization index invalid.");
                return;
            }
            if (Math.abs(res[res.length - 1]) <= EPSINON) {
                LogUtil.warn(TAG, "numericalNormalization res value invalid.");
                return;
            }
            fa[startIndex + index] = res[index] / res[res.length - 1];
        }
    }

    /**
     * Obtain the label of the target object.
     *
     * @return Label info.
     */
    protected String getTargetLabelInfo() {
        if (arTarget == null) {
            return "";
        }
        switch (arTarget.getLabel()) {
            case TARGET_SEAT:
                return "SEAT";
            case TARGET_TABLE:
                return "TABLE";
            default:
                return "";
        }
    }

    /**
     * Obtain the VBO size.
     *
     * @return VBO size.
     */
    public abstract int getVboSize();

    /**
     * Parameters used for rendering updates.
     *
     * @param target Recognized target object.
     */
    public abstract void updateParameters(ARTarget target);

    /**
     * Update the vertices information.
     *
     * @param vertices Vertices information.
     */
    public abstract void updateVertices(float[] vertices);

    /**
     * Obtain the calculated target length, width, height, and label information.
     *
     * @return Calculated target length, width, height, and label information.
     */
    public abstract String getTargetInfo();
}

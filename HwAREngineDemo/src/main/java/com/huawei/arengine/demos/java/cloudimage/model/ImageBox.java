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

package com.huawei.arengine.demos.java.cloudimage.model;

import android.opengl.GLES20;

import com.huawei.arengine.demos.java.cloudimage.common.CornerType;
import com.huawei.hiar.ARAugmentedImage;
import com.huawei.hiar.ARPose;

import java.nio.FloatBuffer;

/**
 * cloud image box augmented.
 *
 * @author HW
 * @since 2021-08-24
 */
public class ImageBox {
    // Number of bytes occupied by each 3D coordinate. float data occupies 4 bytes.
    private static final int BYTES_PER_POINT = 4 * 4;

    private static final int BYTES_PER_CORNER = 4 * 3;

    private static final int MATRIX_COLUMNS_FIRST = 1;

    private static final int MATRIX_COLUMNS_SECOND = 2;

    private static final int MATRIX_COLUMNS_THIRD = 3;

    private static final int MATRIX_COLUMNS_FOURTH = 4;

    private static final float POINT_SIZE = 10.0f;

    private static final float[] COEFFICIENTS = {0.5f, 0.5f, 0.5f, 0.35f, 0.35f, 0.5f};

    private ShaderPojo shaderPojo;

    private ARAugmentedImage augmentedImage;

    private float[] cornerPointCoordinates;

    private int index = 0;

    /**
     * constructor to augmented image.
     *
     * @param augmentedImage image augmented image.
     * @param shaderPojo shader args object to draw image box.
     */
    public ImageBox(ARAugmentedImage augmentedImage, ShaderPojo shaderPojo) {
        this.shaderPojo = shaderPojo;
        this.augmentedImage = augmentedImage;
    }

    /**
     * draw image box to augmented image.
     *
     * @param viewProjectionMatrix view Projection Matrix
     */
    public void draw(float[] viewProjectionMatrix) {
        cornerPointCoordinates = new float[BYTES_PER_CORNER * MATRIX_COLUMNS_FOURTH];
        for (CornerType cornerType : CornerType.values()) {
            createImageBoxCorner(cornerType);
        }

        updateImageBoxCornerPoints(cornerPointCoordinates);
        drawImageBox(viewProjectionMatrix);
        cornerPointCoordinates = null;
        index = 0;
    }

    private void drawImageBox(float[] viewProjectionMatrix) {
        GLES20.glUseProgram(shaderPojo.getProgram());
        GLES20.glEnableVertexAttribArray(shaderPojo.getPosition());
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, shaderPojo.getVbo());

        // The size of the vertex attribute is 4, and each vertex has four coordinate components
        GLES20.glVertexAttribPointer(shaderPojo.getPosition(), MATRIX_COLUMNS_FOURTH, GLES20.GL_FLOAT, false,
            BYTES_PER_POINT, 0);

        // Set the color of the skeleton points to blue.
        GLES20.glUniform4f(shaderPojo.getColor(), 0.56f, 0.93f, 0.56f, 0.5f);
        GLES20.glUniformMatrix4fv(shaderPojo.getMvpMatrix(), 1, false, viewProjectionMatrix, 0);

        // Set the size of the skeleton points.
        GLES20.glUniform1f(shaderPojo.getPointSize(), POINT_SIZE);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, shaderPojo.getNumPoints());
        GLES20.glDisableVertexAttribArray(shaderPojo.getPosition());
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    private void createImageBoxCorner(CornerType cornerType) {
        // every corner point number
        ARPose[] localBoundaryPoses = new ARPose[MATRIX_COLUMNS_THIRD];
        float[] coefficient = new float[COEFFICIENTS.length];
        switch (cornerType) {
            case LOWER_RIGHT:
                // generate point coordinates coefficent
                generateCoefficent(coefficient, MATRIX_COLUMNS_FIRST, MATRIX_COLUMNS_FIRST);
                break;
            case UPPER_LEFT:
                generateCoefficent(coefficient, -MATRIX_COLUMNS_FIRST, -MATRIX_COLUMNS_FIRST);
                break;
            case UPPER_RIGHT:
                generateCoefficent(coefficient, MATRIX_COLUMNS_FIRST, -MATRIX_COLUMNS_FIRST);
                break;
            case LOWER_LEFT:
                generateCoefficent(coefficient, -MATRIX_COLUMNS_FIRST, MATRIX_COLUMNS_FIRST);
                break;
            default:
                break;
        }
        for (int i = 0; i < localBoundaryPoses.length; i++) {
            localBoundaryPoses[i] =
                ARPose.makeTranslation(coefficient[i * MATRIX_COLUMNS_SECOND] * augmentedImage.getExtentX(), 0.0f,
                    coefficient[i * MATRIX_COLUMNS_SECOND + MATRIX_COLUMNS_FIRST] * augmentedImage.getExtentZ());
        }

        ARPose centerPose = augmentedImage.getCenterPose();
        ARPose[] composeCenterPose = new ARPose[localBoundaryPoses.length];
        int cornerCoordinatePos = index * BYTES_PER_CORNER;
        for (int i = 0; i < composeCenterPose.length; ++i) {
            composeCenterPose[i] = centerPose.compose(localBoundaryPoses[i]);
            cornerPointCoordinates[cornerCoordinatePos + i * MATRIX_COLUMNS_FOURTH] = composeCenterPose[i].tx();
            cornerPointCoordinates[cornerCoordinatePos + i * MATRIX_COLUMNS_FOURTH + MATRIX_COLUMNS_FIRST] =
                composeCenterPose[i].ty();
            cornerPointCoordinates[cornerCoordinatePos + i * MATRIX_COLUMNS_FOURTH + MATRIX_COLUMNS_SECOND] =
                composeCenterPose[i].tz();
            cornerPointCoordinates[cornerCoordinatePos + i * MATRIX_COLUMNS_FOURTH + MATRIX_COLUMNS_THIRD] = 1.0f;
        }
        index++;
    }

    private void generateCoefficent(float[] coefficient, int coefficentX, int coefficentZ) {
        for (int i = 0; i < coefficient.length; i += MATRIX_COLUMNS_SECOND) {
            coefficient[i] = coefficentX * COEFFICIENTS[i];
            coefficient[i + MATRIX_COLUMNS_FIRST] = coefficentZ * COEFFICIENTS[i + MATRIX_COLUMNS_FIRST];
        }
    }

    /**
     * Update the coordinates of cloud image 4 corner points.
     *
     * @param cornerPoints 4 corner points of the image
     */
    private void updateImageBoxCornerPoints(float[] cornerPoints) {
        // Each point has an 3D coordinate. The total number of coordinates
        int mPointsNum = cornerPoints.length / MATRIX_COLUMNS_FOURTH;
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, shaderPojo.getVbo());
        shaderPojo.setNumPoints(mPointsNum);
        int vboSize = shaderPojo.getVboSize();
        int numPoints = shaderPojo.getNumPoints();
        if (vboSize < shaderPojo.getNumPoints() * BYTES_PER_POINT) {
            while (vboSize < numPoints * BYTES_PER_POINT) {
                // If the size of VBO is insufficient to accommodate the new point cloud, resize the VBO.
                vboSize *= MATRIX_COLUMNS_SECOND;
            }
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vboSize, null, GLES20.GL_DYNAMIC_DRAW);
        }
        FloatBuffer cornerPointBuffer = FloatBuffer.wrap(cornerPoints);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, numPoints * BYTES_PER_POINT, cornerPointBuffer);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }
}

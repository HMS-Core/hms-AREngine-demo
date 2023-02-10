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

import com.huawei.hiar.ARAugmentedImage;
import com.huawei.hiar.ARPose;

/**
 * API for rendering the augmented image.
 *
 * @author HW
 * @since 2021-02-04
 */
public interface AugmentedImageComponentDisplay {
    /**
     * Initialize the renderer.
     */
    void init();

    /**
     * Render the augmented image object, which is called for each frame.
     *
     * @param augmentedImage AugmentedImage object.
     * @param viewMatrix View matrix.
     * @param projectionMatrix Camera projection matrix.
     */
    void onDrawFrame(ARAugmentedImage augmentedImage, float[] viewMatrix, float[] projectionMatrix);

    /**
     * Obtain the four vertexes of the augmented image.
     *
     * @param augmentedImage AugmentedImage object.
     * @param cornerType Corner type (upper left, lower left, upper right, or lower right).
     * @param index Subscript for a function to run periodically.
     * @param cornerPointCoordinates Write the angular coordinates.
     * @return Array of four image vertices.
     */
    default float[] createImageCorner(ARAugmentedImage augmentedImage, CornerType cornerType, int index,
        float[] cornerPointCoordinates) {

        // 0.5 indicates half of the edge length.
        // The four corners of an image can be obtained by using this parameter and the enums.
        float[] coefficients = {0.5f, 0.5f};
        float[] coefficient = new float[coefficients.length];
        switch (cornerType) {
            case LOWER_RIGHT:
                // Generate the point coordinate coefficient.
                generateCoefficent(coefficient, 1, 1, coefficients);
                break;
            case UPPER_LEFT:
                generateCoefficent(coefficient, -1, -1, coefficients);
                break;
            case UPPER_RIGHT:
                generateCoefficent(coefficient, 1, -1, coefficients);
                break;
            case LOWER_LEFT:
                generateCoefficent(coefficient, -1, 1, coefficients);
                break;
            default:
                break;
        }
        ARPose localBoundaryPose = ARPose.makeTranslation(coefficient[0] * augmentedImage.getExtentX(), 0.0f,
            coefficient[1] * augmentedImage.getExtentZ());

        ARPose centerPose = augmentedImage.getCenterPose();
        int bytesPerCorner = 4;
        int cornerCoordinatePos = index * bytesPerCorner;
        ARPose composeCenterPose = centerPose.compose(localBoundaryPose);
        cornerPointCoordinates[cornerCoordinatePos] = composeCenterPose.tx();
        cornerPointCoordinates[++cornerCoordinatePos] = composeCenterPose.ty();
        cornerPointCoordinates[++cornerCoordinatePos] = composeCenterPose.tz();
        cornerPointCoordinates[++cornerCoordinatePos] = 1.0f;
        return cornerPointCoordinates;
    }

    /**
     * Generate the point coordinate coefficient.
     *
     * @param coefficient Azimuthal angle array.
     * @param coefficentX Coordinate coefficient x.
     * @param coefficentZ Coordinate coefficient z.
     * @param coefficients You can obtain the four corners of the image using this parameter.
     */
    default void generateCoefficent(float[] coefficient, int coefficentX, int coefficentZ, float[] coefficients) {
        for (int i = 0; i < coefficient.length; i += 2) {
            coefficient[i] = coefficentX * coefficients[i];
            coefficient[i + 1] = coefficentZ * coefficients[i + 1];
        }
    }
}

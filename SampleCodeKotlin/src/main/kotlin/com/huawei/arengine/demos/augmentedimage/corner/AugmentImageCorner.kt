/*
 * Copyright 2023. Huawei Technologies Co., Ltd. All rights reserved.
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.arengine.demos.augmentedimage.corner

import com.huawei.arengine.demos.augmentedimage.util.Constants
import com.huawei.arengine.demos.augmentedimage.util.CornerType
import com.huawei.hiar.ARAugmentedImage
import com.huawei.hiar.ARPose

/**
 * Obtains the coordinate array of the four vertices of an image.
 *
 * @author HW
 * @since 2021-03-29
 */
class AugmentImageCorner {
    companion object {
        /**
         * 0.5f indicates half of the width and height, which are combined with enumerated values to
         * form the coordinate data.
         */
        private val COEFFICIENTS = floatArrayOf(0.5f, 0.5f)
    }

    var cornerPointCoordinates: FloatArray? = null

    var index = 0

    /**
     * Obtain the vertex coordinates of the four corners of the augmented image and write them to the
     * cornerPointCoordinates array.
     *
     * @param augmentedImage Augmented image object.
     * @param cornerType Corner type.
     */
    fun createImageCorner(augmentedImage: ARAugmentedImage, cornerType: CornerType) {
        val localBoundaryPose: ARPose
        val coefficient = FloatArray(COEFFICIENTS.size)
        when (cornerType) {
            CornerType.LOWER_RIGHT -> generateCoefficent(coefficient, 1, 1)
            CornerType.UPPER_LEFT -> generateCoefficent(coefficient, -1, -1)
            CornerType.UPPER_RIGHT -> generateCoefficent(coefficient, 1, -1)
            CornerType.LOWER_LEFT -> generateCoefficent(coefficient, -1, 1)
            else -> {
            }
        }
        localBoundaryPose = ARPose.makeTranslation(coefficient[0] * augmentedImage.extentX, 0.0f,
            coefficient[1] * augmentedImage.extentZ)
        val centerPose = augmentedImage.centerPose
        val composeCenterPose: ARPose
        val cornerCoordinatePos = index * Constants.BYTES_PER_CORNER
        composeCenterPose = centerPose.compose(localBoundaryPose)

        // The coordinates of each vertex consist of the x, y, and z coordinates and factors.
        cornerPointCoordinates!![cornerCoordinatePos] = composeCenterPose.tx()
        cornerPointCoordinates!![cornerCoordinatePos + 1] = composeCenterPose.ty()
        cornerPointCoordinates!![cornerCoordinatePos + 2] = composeCenterPose.tz()
        cornerPointCoordinates!![cornerCoordinatePos + 3] = 1.0f
        index++
    }

    fun generateCoefficent(coefficient: FloatArray, coefficentX: Int, coefficentZ: Int) {
        var i = 0
        while (i < coefficient.size) {
            coefficient[i] = coefficentX * COEFFICIENTS[i]
            coefficient[i + 1] = coefficentZ * COEFFICIENTS[i + 1]

            // Use the center of the recognized image as the origin of the coordinate axis and calculate the coordinates
            // of each vertex on the xoz plane. The value of y is a constant and does not need to be calculated.
            i += 2
        }
    }
}
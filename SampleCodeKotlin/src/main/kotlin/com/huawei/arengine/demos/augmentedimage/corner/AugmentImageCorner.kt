/**
 * Copyright 2021. Huawei Technologies Co., Ltd. All rights reserved.
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
 * 获取图像四角顶点的坐标数组。
 *
 * @author HW
 * @since 2021-03-29
 */
class AugmentImageCorner {
    companion object {
        //0.5f表示宽、高的一半，与枚举值组合成坐标数据。
        private val COEFFICIENTS = floatArrayOf(0.5f, 0.5f)
    }

    var cornerPointCoordinates: FloatArray? = null

    var index = 0

    /**
     * 获取增强图片的四角顶点坐标，写入cornerPointCoordinates数组。
     *
     * @param augmentedImage 增强图像对象。
     * @param cornerType 边角类型。
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

        //每个顶点的坐标又x,y,z坐标及系数组成。
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

            //以识别到的图片中心点为坐标轴原点，在xoz平面上计算出每个四角顶点的坐标，y值为常量无需计算。
            i += 2
        }
    }
}
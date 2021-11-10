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

package com.huawei.arengine.demos.java.utils;

import android.graphics.Bitmap;

import com.huawei.arengine.demos.common.LogUtil;

import java.util.Optional;

/**
 * Public utility class.
 *
 * @author HW
 * @since 2021-09-04
 */
public class CommonUtil {
    private static final String TAG = CommonUtil.class.getSimpleName();

    /**
     * Privatization construction method.
     */
    private CommonUtil() {
    }

    /**
     * Create a bitmap based on the RGB array.
     *
     * @param rgbData Byte array in RGB_888 format.
     * @param width Number of horizontal pixels.
     * @param height Number of vertical pixels.
     * @return Cube mapping bitmap.
     */
    public static Optional<Bitmap> createBitmapImage(byte[] rgbData, int width, int height) {
        // The data passed from the AREngineServer is in the RGB_888 format.
        // The bitmap can be output only after the data is converted into the ARGB_8888 format.
        int[] colors = convertRgbToArgb(rgbData);
        if (colors.length == 0) {
            LogUtil.warn(TAG, "colors length is 0.");
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888));
        } catch (IllegalArgumentException e) {
            LogUtil.error(TAG, "Exception on the createBitmap.");
            return Optional.empty();
        }
    }

    /**
     * Converts the pixel color in RGB_888 format to ARGB_8888 format.
     *
     * @param rgbArr Byte array of the pixel point color in RGB_888 format.
     * @return Integer array of the pixel point color in ARGB_8888 format.
     */
    private static int[] convertRgbToArgb(byte[] rgbArr) {
        if (rgbArr == null) {
            LogUtil.warn(TAG, "rgbData is null.");
            return new int[0];
        }

        // Add an 8-bit transparency channel to convert RGB_888 pixels into ARGB_8888 pixels.
        // In the RGB_888 format, one pixel is stored every 3 bytes.
        // The storage sequence is R-G-B (high on the left and low on the right).
        // In the ARGB_888 format, one pixel is stored for each int.
        // The storage sequence is A-B-G-R (high on the left and low on the right).
        // The 8-bit transparency data of each pixel is stored in bits 24 to 31 of argbArr.
        // The 8-bit B data of each pixel is stored in bits 16 to 23 of argbArr.
        // The 8-bit G data of each pixel is stored in bits 8–15 of argbArr.
        // The 8-bit R data of each pixel is stored in bits 0-7 of argbArr.
        int[] argbArr = new int[rgbArr.length / 3];
        for (int i = 0; i < argbArr.length; ++i) {
            argbArr[i] = 0xFF000000 | (rgbArr[i * 3] << 16 & 0x00FF0000) | (rgbArr[i * 3 + 1] << 8 & 0x0000FF00)
                | (rgbArr[i * 3 + 2] & 0x000000FF);
        }
        return argbArr;
    }
}

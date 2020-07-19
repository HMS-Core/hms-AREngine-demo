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

package com.huawei.arengine.demos.common;

import android.opengl.Matrix;

/**
 * Matrix utility class.
 *
 * @author HW
 * @since 2020-03-29
 */
public class MatrixUtil {
    private static final int MATRIX_SIZE = 16;

    private MatrixUtil() {
    }

    /**
     * Get the matrix of a specified type.
     *
     * @param matrix Results of matrix obtained.
     * @param width Width.
     * @param height Height.
     */
    public static void getProjectionMatrix(float[] matrix, int width, int height) {
        if (height > 0 && width > 0) {
            float[] projection = new float[MATRIX_SIZE];
            float[] camera = new float[MATRIX_SIZE];

            // Calculate the orthographic projection matrix.
            Matrix.orthoM(projection, 0, -1, 1, -1, 1, 1, 3);
            Matrix.setLookAtM(camera, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);
            Matrix.multiplyMM(matrix, 0, projection, 0, camera, 0);
        }
    }

    /**
     * Three-dimensional data standardization method, which divides each
     * number by the root of the sum of squares of all numbers.
     *
     * @param vector Three-dimensional vector.
     */
    public static void normalizeVec3(float[] vector) {
        // This data has three dimensions(0,1,2)
        float length = 1.0f / (float) Math.sqrt(vector[0] * vector[0] + vector[1] * vector[1] + vector[2] * vector[2]);
        vector[0] *= length;
        vector[1] *= length;
        vector[2] *= length;
    }

    /**
     * Provide a 4 * 4 unit matrix.
     *
     * @return Returns matrix as an array.
     */
    public static float[] getOriginalMatrix() {
        return new float[] {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
    }
}
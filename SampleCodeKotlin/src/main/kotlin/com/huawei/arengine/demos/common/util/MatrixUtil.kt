/**
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.huawei.arengine.demos.common.util

import android.opengl.Matrix
import com.huawei.arengine.demos.common.util.Constants.MATRIX_SIZE
import kotlin.math.sqrt

/**
 * Matrix utility class.
 *
 * @author HW
 * @since 2020-10-10
 */
object MatrixUtil {
    /**
     * Provide a 4 * 4 unit matrix.
     *
     * @return Returns matrix as an array.
     */
    val originalMatrix: FloatArray
        get() = floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)

    /**
     * Get the matrix of a specified type.
     *
     * @param projectionMatrix Results of matrix obtained.
     * @param width Width.
     * @param height Height.
     */
    fun getProjectionMatrix(projectionMatrix: FloatArray, width: Int, height: Int) {
        if (height <= 0 || width <= 0) {
            return
        }

        val projection = FloatArray(MATRIX_SIZE)
        val camera = FloatArray(MATRIX_SIZE)

        // Calculate the orthographic projection matrix.
        Matrix.orthoM(projection, 0, -1f, 1f, -1f, 1f, 1f, 3f)
        Matrix.setLookAtM(camera, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(projectionMatrix, 0, projection, 0, camera, 0)
    }

    /**
     * Three-dimensional data standardization method, which divides each
     * number by the root of the sum of squares of all numbers.
     *
     * @param vec Three-dimensional vector.
     */
    fun normalizeVec3(vec: FloatArray) {
        // This data has three dimensions(0,1,2)
        val factor = 1.0f / sqrt(vec[0] * vec[0] + vec[1] * vec[1] + (vec[2] * vec[2]).toDouble()).toFloat()
        for (i in 0..2) {
            vec[i] *= factor
        }
    }
}
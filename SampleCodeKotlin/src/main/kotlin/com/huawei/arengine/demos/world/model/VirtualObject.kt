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
package com.huawei.arengine.demos.world.model

import android.opengl.Matrix
import com.huawei.arengine.demos.world.util.Constants
import com.huawei.hiar.ARAnchor

/**
 * This class provides attributes of the virtual object and necessary methods related to virtual object rendering.
 *
 * @author HW
 * @since 2020-10-10
 */
class VirtualObject(private var arAnchor: ARAnchor, private var objectColor: FloatArray) {
    /**
     * Whether the current virtual object is in a selected state.
     */
    private var isSelected = false

    /**
     * Set the selection status of the current object by passing true or false,
     * where true indicates that the object is selected, and false indicates not.
     *
     * @param isSelected Whether the selection is successful.
     */
    fun setSelected(isSelected: Boolean) {
        this.isSelected = isSelected
    }

    /**
     * Update the anchor information in the virtual object corresponding to the class.
     *
     * @param newAnchor Data provided by AR Engine, describing the pose.
     */
    fun setArAnchor(newAnchor: ARAnchor) {
        arAnchor.detach()
        arAnchor = newAnchor
    }

    /**
     * Obtain the anchor information of a virtual object corresponding to the class.
     *
     * @return ARAnchor(provided by AREngine)
     */
    fun getArAnchor() = arAnchor

    /**
     *  Obtain the anchor information of a virtual object corresponding to the class.
     *
     * @return Color of the virtual object, returned in an array with a length of 4.
     */
    fun getObjectColor(): FloatArray {
        return objectColor.copyOf().also {
            if (isSelected) {
                (0..2).forEach { i ->
                    it[i] = 255.0f - it[i]
                }
                it[3] = objectColor[3]
            }
        }
    }

    /**
     * Obtain the anchor matrix data of the current virtual object.
     *
     * @return Anchor matrix data of the current virtual object.
     */
    fun getModelAnchorMatrix(): FloatArray {
        val modelMatrix = FloatArray(Constants.MATRIX_SIZE)
        arAnchor.pose.toMatrix(modelMatrix, 0)
        return FloatArray(Constants.MATRIX_SIZE).also {
            Matrix.multiplyMM(it, 0, modelMatrix, 0, Constants.FACTOR_MODEL_MATRIX, 0)
        }
    }
}
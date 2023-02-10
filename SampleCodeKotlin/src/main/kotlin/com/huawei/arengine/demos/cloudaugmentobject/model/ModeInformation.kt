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

package com.huawei.arengine.demos.cloudaugmentobject.model

/**
 * 3D cloud recognition model class.
 *
 * @author HW
 * @since 2022-04-20
 */
class ModeInformation(val information: String?, val continents: String?) {
    private var modelInformation: String? = null

    private var continent: String? = null

    init {
        modelInformation = information ?: ""
        continent = continents ?: ""
    }

    /**
     * Obtains the modelName.
     *
     * @return modelName.
     */
    fun getContinent(): String {
        return continent ?: ""
    }

    /**
     * Obtain the model information, including modelName, appId, and appSecret.
     *
     * @return Return the model information.
     */
    fun getModelInformation(): String {
        return modelInformation ?: ""
    }
}
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

package com.huawei.arengine.demos.augmentedimage.controller

import com.huawei.hiar.ARAugmentedImage

/**
 * 渲染增强图像接口。
 *
 * @author HW
 * @since 2021-03-29
 */
interface AugmentedImageComponentDisplay {
    /**
     * 初始化渲染器。
     */
    fun init()

    /**
     * 渲染增强图像对象，每帧调用。
     *
     * @param augmentedImage AugmentedImage对象。
     * @param viewMatrix 视图矩阵。
     * @param projectionMatrix 相机投影矩阵。
     */
    fun onDrawFrame(augmentedImage: ARAugmentedImage, viewMatrix: FloatArray, projectionMatrix: FloatArray)
}
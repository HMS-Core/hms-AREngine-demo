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

package com.huawei.arengine.demos.augmentedimage.pojo

/**
 * shader program index
 *
 * @author HW
 * @since 2021-03-29
 */
data class ImageShaderPojo(var program: Int = 0, var position: Int = 0,
    var modelView: Int = 0, var modelViewProjection: Int = 0,
    var normalVector: Int = 0, var texCoordinate: Int = 0,
    var texture: Int = 0, var light: Int = 0, var color: Int = 0,
    var vbo: Int = 0, var numPoints: Int = 0, var vboSize: Int = 0,
    var index: Int = 0, var pointSize:Int = 0)
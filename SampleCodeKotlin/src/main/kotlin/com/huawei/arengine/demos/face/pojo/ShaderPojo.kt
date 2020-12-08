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
package com.huawei.arengine.demos.face.pojo

/**
 * shader program index
 *
 * @author HW
 * @since 2020-11-05
 */
data class ShaderPojo(var program: Int = 0, var positionAttribute: Int = 0,
    var modelViewProjectionUniform: Int = 0, var colorUniform: Int = 0,
    var pointSizeUniform: Int = 0, var textureUniform: Int = 0, var textureCoordAttribute: Int = 0)
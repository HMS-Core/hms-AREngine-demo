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
package com.huawei.arengine.demos.hand.pojo

import com.huawei.arengine.demos.hand.util.Constants.BYTES_PER_POINT
import com.huawei.arengine.demos.hand.util.Constants.INITIAL_POINTS_SIZE

/**
 * shader program index
 *
 * @author HW
 * @since 2020-11-05
 */
data class ShaderPojo(var vbo: Int = 0, var vboSize: Int = BYTES_PER_POINT * INITIAL_POINTS_SIZE,
    var program: Int = 0, var position: Int = 0,
    var modelViewProjectionMatrix: Int = 0, var color: Int = 0,
    var pointSize: Int = 0, var pointNum: Int = 0)
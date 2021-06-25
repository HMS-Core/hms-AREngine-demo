/**
 * Copyright 2021. Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huawei.arengine.demos.world.pojo

import com.huawei.hiar.ARPointCloud

/**
 * Gesture event management class for storing and creating gestures.
 *
 * @author HW
 * @since 2021-04-06
 */
data class PointPojo(var mProgramName: Int = 0, var mPointBuffer: Int = 0,
    var mPointBufferSize: Int = 0, var mPositionAttribute: Int = 0,
    var mViewProjectionUniform: Int = 0, var mPointUniform: Int = 0,
    var mColorUniform: Int = 0, var mNumPoints: Int = 0, var mPointCloud: ARPointCloud? = null)
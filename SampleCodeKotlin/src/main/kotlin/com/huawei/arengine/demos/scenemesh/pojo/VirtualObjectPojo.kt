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

package com.huawei.arengine.demos.scenemesh.pojo

import com.huawei.arengine.demos.scenemesh.util.Constants

/**
 * VirtualObject program index
 *
 * @author HW
 * @since 2021-04-20
 */
data class VirtualObjectPojo(var mVertexBufferId: Int = 0, var mVerticesBaseAddress: Int = 0,
    var mTexCoordsBaseAddress: Int = 0, var mNormalsBaseAddress: Int = 0,
    var mIndexBufferId: Int = 0, var mIndexCount: Int = 0,
    var mProgram: Int = 0, var mModelViewUniform: Int = 0,
    var mModelViewProjectionUniform: Int = 0, var mPositionAttribute: Int = 0,
    var mNormalAttribute: Int = 0, var mTexCoordAttribute: Int = 0,
    var mTextureUniform: Int = 0, var mLightingParametersUniform: Int = 0,
    var mMaterialParametersUniform: Int = 0, var mColorUniform: Int = 0,
    var mAmbient: Float = Constants.OBJECT_AMBIENT, var mDiffuse: Float = Constants.OBJECT_DIFFUSE,
    var mSpecular: Float = Constants.OBJECT_SPECULAR, var mSpecularPower: Float = Constants.OBJECT_SPECULARPOWER)
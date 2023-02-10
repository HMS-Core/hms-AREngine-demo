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

package com.huawei.arengine.demos.scenemesh.pojo

/**
 * SceneMesh program index
 *
 * @author HW
 * @since 2021-04-21
 */
data class SceneMeshPojo(
    var mVerticeVBO: Int = 0, var mVerticeVBOSize: Int = 8000,
    var mTriangleVBO: Int = 0, var mTriangleVBOSize: Int = 5000,
    var mProgram: Int = 0, var mPositionAttribute: Int = 0, var mModelViewProjectionUniform: Int = 0,
    var mPointsNum: Int = 0,
    var mTrianglesNum: Int = 0
)
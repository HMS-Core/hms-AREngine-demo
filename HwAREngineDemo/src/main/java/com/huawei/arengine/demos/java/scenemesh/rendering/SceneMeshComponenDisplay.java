/**
 * Copyright 2021. Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huawei.arengine.demos.java.scenemesh.rendering;

import android.content.Context;

import com.huawei.hiar.ARFrame;

/**
 * 网格场景渲染接口类
 *
 * @author hw
 * @since 2021-01-26
 */
interface SceneMeshComponenDisplay {
    /**
     * 初始化渲染。
     *
     * @param context 上下文信息。
     */
    void init(Context context);

    /**
     * 呈现对象，每帧调用。
     *
     * @param arFrame 处理AR帧。
     * @param cameraView 投影矩阵。
     * @param cameraPerspective 摄像机投影矩阵。
     */
    void onDrawFrame(ARFrame arFrame, float[] cameraView, float[] cameraPerspective);
}

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

package com.huawei.arengine.demos.java.scenemesh.rendering;

import android.content.Context;

import com.huawei.hiar.ARFrame;

/**
 * Mesh scene rendering API class.
 *
 * @author hw
 * @since 2021-01-26
 */
interface SceneMeshComponenDisplay {
    /**
     * Initialize the rendering.
     *
     * @param context Context information.
     */
    void init(Context context);

    /**
     * Displayed object, which is called for each frame.
     *
     * @param arFrame Process the AR frame.
     * @param cameraView Projection matrix.
     * @param cameraPerspective Camera projection matrix.
     */
    void onDrawFrame(ARFrame arFrame, float[] cameraView, float[] cameraPerspective);
}

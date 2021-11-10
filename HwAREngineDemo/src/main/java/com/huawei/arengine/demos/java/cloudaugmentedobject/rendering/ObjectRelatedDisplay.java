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

package com.huawei.arengine.demos.java.cloudaugmentedobject.rendering;

import com.huawei.hiar.ARObject;
import com.huawei.hiar.ARPose;

import java.util.Collection;

/**
 * Draws and displays data related to 3D object.
 *
 * @author HW
 * @since 2021-04-02
 */
public interface ObjectRelatedDisplay {
    /**
     * Init render.
     */
    void init();

    /**
     * Render objects, call per frame.
     *
     * @param arObjects arObjects
     * @param viewMatrix Camera view matrix.
     * @param projectionMatrix Camera projection matrix.
     * @param cameraPose Camera display oriented pose.
     */
    void onDrawFrame(Collection<ARObject> arObjects, float[] viewMatrix, float[] projectionMatrix, ARPose cameraPose);
}

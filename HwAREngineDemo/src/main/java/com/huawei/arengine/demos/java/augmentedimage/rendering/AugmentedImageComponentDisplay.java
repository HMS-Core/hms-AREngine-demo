/**
 * Copyright 2021. Huawei Technologies Co., Ltd. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.arengine.demos.java.augmentedimage.rendering;

import com.huawei.hiar.ARAugmentedImage;

/**
 * API for rendering the augmented image.
 *
 * @author HW
 * @since 2021-02-04
 */
public interface AugmentedImageComponentDisplay {
    /**
     * Initialize the renderer.
     */
    void init();

    /**
     * Render the augmented image object, which is called for each frame.
     *
     * @param augmentedImage AugmentedImage object.
     * @param viewMatrix View matrix.
     * @param projectionMatrix Camera projection matrix.
     */
    void onDrawFrame(ARAugmentedImage augmentedImage, float[] viewMatrix, float[] projectionMatrix);
}

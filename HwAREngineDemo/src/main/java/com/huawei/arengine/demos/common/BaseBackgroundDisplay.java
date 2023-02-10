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

package com.huawei.arengine.demos.common;

import com.huawei.hiar.ARFrame;

/**
 * Camera preview API.
 *
 * @author HW
 * @since 2022-09-15
 */
public interface BaseBackgroundDisplay {
    /**
     * Change the size of the camera preview window when the surface window changes.
     *
     * @param width Width of the camera preview window.
     * @param height Height of the camera preview window.
     */
    void onSurfaceChanged(int width, int height);

    /**
     * Initialize OpenGL when onSurfaceCreated is called.
     */
    void init();

    /**
     * Initialize OpenGL with the texture ID.
     *
     * @param textureId Texture ID.
     */
    void init(int textureId);

    /**
     * Return the texture ID bound to OpenGL.
     *
     * @return Texture ID.
     */
    int getExternalTextureId();

    /**
     * Implement OpenGL rendering.
     *
     * @param frame AR frame.
     */
    void onDrawFrame(ARFrame frame);
}

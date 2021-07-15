/**
 * Copyright 2021. Huawei Technologies Co., Ltd. All rights reserved.
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.arengine.demos.augmentedimage.util

/**
 * List the corners of an image.
 *
 * @author hw
 * @since 2021-03-29
 */
enum class CornerType {
    /**
     * Upper left corner.
     */
    UPPER_LEFT,

    /**
     * Upper right corner.
     */
    UPPER_RIGHT,

    /**
     * Lower left corner.
     */
    LOWER_RIGHT,

    /**
     * Lower right corner.
     */
    LOWER_LEFT
}
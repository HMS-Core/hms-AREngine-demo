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

package com.huawei.arengine.demos.scenemesh.controller

import com.huawei.hiar.ARAnchor

/**
 * Colored AR anchor.
 *
 * @author hw
 * @since 2021-04-21
 */
class ColoredArAnchor(val anchor: ARAnchor, val color: String) {
    companion object {
        private const val TAG = "ColoredArAnchor"
    }
}
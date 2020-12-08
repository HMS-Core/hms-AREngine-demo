/**
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
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
package com.huawei.arengine.demos.common.service

/**
 * This class is used to display information on the screen. Before using this function,
 * you need to set listening, where the information is processed. In this sample, a method
 * for displaying information on the screen is created in the UI thread.
 *
 * @author HW
 * @since 2020-10-10
 */
class TextService {
    private lateinit var textInfoListener: (text: String?) -> Unit

    /**
     * Display the string information. This method is called in each frame
     * when [android.opengl.GLSurfaceView.Renderer.onDrawFrame].
     *
     * @param textInfo String builder.
     */
    fun drawText(textInfo: StringBuilder?) {
        if (textInfo == null) {
            textInfoListener(null)
        } else {
            textInfoListener(textInfo.toString())
        }
    }

    /**
     * Set the listener to display information in the UI thread. This method is called
     * when [android.opengl.GLSurfaceView.Renderer.onSurfaceCreated].
     *
     * @param listener OnTextInfoChangeListener.
     */
    fun setListener(listener: (text: String?) -> Unit) {
        textInfoListener = listener
    }
}
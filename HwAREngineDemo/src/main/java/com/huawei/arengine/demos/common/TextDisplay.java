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

/**
 * This class is used to display information on the screen. Before using this function,
 * you need to set listening, where the information is processed. In this sample, a method
 * for displaying information on the screen is created in the UI thread.
 *
 * @author HW
 * @since 2020-03-16
 */
public class TextDisplay {
    private static final String TAG = "TextDisplay";

    private OnTextInfoChangeListener mTextInfoListener;

    /**
     * Display the string information. This method is called in each frame
     * when {@link android.opengl.GLSurfaceView.Renderer#onDrawFrame}.
     *
     * @param sb String.
     */
    public void onDrawFrame(String sb) {
        showTextInfo(sb);
    }

    /**
     * Set the listener to display information in the UI thread. This method is called
     * when {@link android.opengl.GLSurfaceView.Renderer#onSurfaceCreated}.
     *
     * @param textInfoListener OnTextInfoChangeListener.
     */
    public void setListener(OnTextInfoChangeListener textInfoListener) {
        mTextInfoListener = textInfoListener;
    }

    /**
     * Listen to the text change and execute corresponding methods.
     *
     * @author HW
     * @since 2020-03-16
     */
    public interface OnTextInfoChangeListener {
        /**
         * Display the given text.
         *
         * @param text Text to be displayed.
         * @param positionX X-coordinates of points
         * @param positionY Y-coordinates of points
         */
        void onTextInfoChanged(String text, float positionX, float positionY);
    }

    private void showTextInfo(String text) {
        if (mTextInfoListener != null) {
            mTextInfoListener.onTextInfoChanged(text, 0, 0);
        } else {
            LogUtil.error(TAG, "listener was not initialized.");
        }
    }
}

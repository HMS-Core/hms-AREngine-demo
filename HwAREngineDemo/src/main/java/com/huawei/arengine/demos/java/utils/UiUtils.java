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

package com.huawei.arengine.demos.java.utils;

import android.app.Activity;
import android.graphics.Color;
import android.widget.TextView;

/**
 * Used for some common UI display processing.
 *
 * @author HW
 * @since 2022-08-25
 */
public class UiUtils {
    /**
     * Create a text display thread that is used for text update tasks.
     *
     * @param textView The TextView
     * @param activity The Activity
     * @param text Gesture information displayed on the screen
     * @param positionX The left padding in pixels.
     * @param positionY The right padding in pixels.
     */
    public static void showTypeTextView(Activity activity, TextView textView, final String text, final float positionX,
        final float positionY) {
        if (activity == null || textView == null) {
            return;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setTextColor(Color.WHITE);

                // Set the font size.
                textView.setTextSize(10f);
                if (text != null) {
                    textView.setText(text);
                    textView.setPadding((int) positionX, (int) positionY, 0, 0);
                } else {
                    textView.setText("");
                }
            }
        });
    }
}

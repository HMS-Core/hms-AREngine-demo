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

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.GestureDetector;
import android.view.MotionEvent;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Listen to and control some gestures.
 *
 * @author HW
 * @since 2022-10-17
 */
public class GestureDetectorUtils {
    /**
     * Initialize the gesture listener.
     *
     * @param context The Context
     * @param tag Log printing tag.
     * @param surfaceView Control for previewing the drawing.
     * @param queuedSingleTaps Gesture type queue.
     */
    public static void initGestureDetector(Context context, String tag, GLSurfaceView surfaceView,
        ArrayBlockingQueue<GestureEvent> queuedSingleTaps) {
        GestureDetector gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent motionEvent) {
                onGestureEvent(GestureEvent.createDoubleTapEvent(motionEvent), tag, queuedSingleTaps);
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
                onGestureEvent(GestureEvent.createSingleTapConfirmEvent(motionEvent), tag, queuedSingleTaps);
                return true;
            }

            @Override
            public boolean onDown(MotionEvent motionEvent) {
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                onGestureEvent(GestureEvent.createScrollEvent(e1, e2, distanceX, distanceY), tag, queuedSingleTaps);
                return true;
            }
        });

        if (surfaceView == null) {
            LogUtil.warn(tag, "surfaceView params is invalid");
            return;
        }
        surfaceView.setOnTouchListener((view, event) -> gestureDetector.onTouchEvent(event));
    }

    private static void onGestureEvent(GestureEvent gestureEvent, String tag,
        ArrayBlockingQueue<GestureEvent> queuedSingleTaps) {
        boolean isSuccess = queuedSingleTaps.offer(gestureEvent);
        if (isSuccess) {
            LogUtil.debug(tag, "Successfully joined the queue.");
        } else {
            LogUtil.warn(tag, "Failed to join queue.");
        }
    }
}

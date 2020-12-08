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
package com.huawei.arengine.demos.world.util

import android.view.MotionEvent
import com.huawei.arengine.demos.world.pojo.GestureEvent

/**
 * Gesture event management class for storing and creating gestures.
 *
 * @author HW
 * @since 2020-11-08
 */
object GestureEventFactory {
    /**
     * Define the constant 1, indicating that the gesture type is SCROLL.
     */
    const val GESTURE_EVENT_TYPE_SCROLL = 1

    /**
     * Define the constant 2, indicating that the gesture type is SINGLE TAP CONFIRMED.
     */
    const val GESTURE_EVENT_TYPE_SINGLE_TAP_CONFIRMED = 2

    /**
     * Define the constant 3, indicating that the gesture type is DOUBLE TAP.
     */
    const val GESTURE_EVENT_TYPE_DOUBLE_TAP = 3

    /**
     * Create a gesture type: SINGLE TAP CONFIRMED.
     *
     * @param motionEvent The gesture motion event: SINGLE TAP CONFIRM.
     * @return GestureEvent(SINGLE TAP CONFIRM).
     */
    fun createSingleTapConfirmEvent(motionEvent: MotionEvent): GestureEvent {
        return GestureEvent(type = GESTURE_EVENT_TYPE_SINGLE_TAP_CONFIRMED, eventFirst = motionEvent)
    }

    /**
     * Create a gesture type: DOUBLE TAP.
     *
     * @param motionEvent The gesture motion event: DOUBLE TAP.
     * @return GestureEvent(DOUBLE TAP).
     */
    fun createDoubleTapEvent(motionEvent: MotionEvent): GestureEvent {
        return GestureEvent(type = GESTURE_EVENT_TYPE_DOUBLE_TAP, eventFirst = motionEvent)
    }

    /**
     * Create a gesture type: SCROLL.
     *
     * @param e1 The first down motion event that started the scrolling.
     * @param e2 The second down motion event that ended the scrolling.
     * @param distanceX The distance along the X axis that has been scrolled since the last call to onScroll.
     * @param distanceY The distance along the Y axis that has been scrolled since the last call to onScroll.
     * @return GestureEvent(SCROLL).
     */
    fun createScrollEvent(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): GestureEvent {
        return GestureEvent(GESTURE_EVENT_TYPE_SCROLL, distanceX, distanceY, e1, e2)
    }
}
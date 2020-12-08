/**
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huawei.arengine.demos.java.world;

import android.view.MotionEvent;

/**
 * Gesture event management class for storing and creating gestures.
 *
 * @author HW
 * @since 2019-06-13
 */
public class GestureEvent {
    /**
     * Define the constant 0, indicating an unknown gesture type.
     */
    public static final int GESTURE_EVENT_TYPE_UNKNOW = 0;

    /**
     * Define the constant 1, indicating that the gesture type is DOWN.
     */
    public static final int GESTURE_EVENT_TYPE_DOWN = 1;

    /**
     * Define the constant 2, indicating that the gesture type is SINGLETAPUP.
     */
    public static final int GESTURE_EVENT_TYPE_SINGLETAPUP = 2;

    /**
     * Define the constant 3, indicating that the gesture type is SCROLL.
     */
    public static final int GESTURE_EVENT_TYPE_SCROLL = 3;

    /**
     * Define the constant 4, indicating that the gesture type is SINGLETAPCONFIRMED.
     */
    public static final int GESTURE_EVENT_TYPE_SINGLETAPCONFIRMED = 4;

    /**
     * Define the constant 5, indicating that the gesture type is DOUBLETAP.
     */
    public static final int GESTURE_EVENT_TYPE_DOUBLETAP = 5;

    private int type;

    private MotionEvent eventFirst;

    private MotionEvent eventSecond;

    private float distanceX;

    private float distanceY;

    private GestureEvent() {
    }

    public float getDistanceX() {
        return distanceX;
    }

    public float getDistanceY() {
        return distanceY;
    }

    public int getType() {
        return type;
    }

    public MotionEvent getEventFirst() {
        return eventFirst;
    }

    public MotionEvent getEventSecond() {
        return eventSecond;
    }

    /**
     * Create a gesture type: DOWN.
     *
     * @param motionEvent The gesture motion event: DOWN.
     * @return GestureEvent.
     */
    static GestureEvent createDownEvent(MotionEvent motionEvent) {
        GestureEvent ret = new GestureEvent();
        ret.type = GESTURE_EVENT_TYPE_DOWN;
        ret.eventFirst = motionEvent;
        return ret;
    }

    /**
     * Create a gesture type: SINGLETAPUP.
     *
     * @param motionEvent The gesture motion event: SINGLETAPUP.
     * @return GestureEvent(SINGLETAPUP).
     */
    static GestureEvent createSingleTapUpEvent(MotionEvent motionEvent) {
        GestureEvent ret = new GestureEvent();
        ret.type = GESTURE_EVENT_TYPE_SINGLETAPUP;
        ret.eventFirst = motionEvent;
        return ret;
    }

    /**
     * Create a gesture type: SINGLETAPCONFIRM.
     *
     * @param motionEvent The gesture motion event: SINGLETAPCONFIRM.
     * @return GestureEvent(SINGLETAPCONFIRM).
     */
    public static GestureEvent createSingleTapConfirmEvent(MotionEvent motionEvent) {
        GestureEvent ret = new GestureEvent();
        ret.type = GESTURE_EVENT_TYPE_SINGLETAPCONFIRMED;
        ret.eventFirst = motionEvent;
        return ret;
    }

    /**
     * Create a gesture type: DOUBLETAP.
     *
     * @param motionEvent The gesture motion event: DOUBLETAP.
     * @return GestureEvent(DOUBLETAP).
     */
    public static GestureEvent createDoubleTapEvent(MotionEvent motionEvent) {
        GestureEvent ret = new GestureEvent();
        ret.type = GESTURE_EVENT_TYPE_DOUBLETAP;
        ret.eventFirst = motionEvent;
        return ret;
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
    public static GestureEvent createScrollEvent(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        GestureEvent ret = new GestureEvent();
        ret.type = GESTURE_EVENT_TYPE_SCROLL;
        ret.eventFirst = e1;
        ret.eventSecond = e2;
        ret.distanceX = distanceX;
        ret.distanceY = distanceY;
        return ret;
    }
}
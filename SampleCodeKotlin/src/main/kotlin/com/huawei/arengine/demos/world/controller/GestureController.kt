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
package com.huawei.arengine.demos.world.controller

import android.view.GestureDetector
import android.view.MotionEvent
import com.huawei.arengine.demos.MainApplication
import com.huawei.arengine.demos.world.model.VirtualObject
import com.huawei.arengine.demos.world.pojo.GestureEvent
import com.huawei.arengine.demos.world.service.HitTestService
import com.huawei.arengine.demos.world.util.GestureEventFactory
import com.huawei.hiar.ARCamera
import com.huawei.hiar.ARFrame
import java.util.concurrent.ArrayBlockingQueue

/**
 * the class control gesture on surfaceView.
 *
 * @author HW
 * @since 2020-11-08
 */
class GestureController {
    val tapQueue by lazy { ArrayBlockingQueue<GestureEvent>(2) }

    val virtualObjects: ArrayList<VirtualObject> = ArrayList()

    val gestureDetector by lazy {
        GestureDetector(MainApplication.context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(motionEvent: MotionEvent): Boolean {
                tapQueue.offer(GestureEventFactory.createDoubleTapEvent(motionEvent))
                return true
            }

            override fun onSingleTapConfirmed(motionEvent: MotionEvent): Boolean {
                tapQueue.offer(GestureEventFactory.createSingleTapConfirmEvent(motionEvent))
                return true
            }

            override fun onDown(motionEvent: MotionEvent): Boolean {
                return true
            }

            override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                tapQueue.offer(GestureEventFactory.createScrollEvent(e1, e2, distanceX, distanceY))
                return true
            }
        })
    }

    private val hitTestService by lazy { HitTestService(tapQueue, virtualObjects) }

    fun handleGestureEvent(
        arFrame: ARFrame, arCamera: ARCamera, projectionMatrix: FloatArray, viewMatrix: FloatArray) {
        hitTestService.handleGestureEvent(arFrame, arCamera, projectionMatrix, viewMatrix)
    }

    fun setSurfaceSize(width: Float, height: Float) {
        hitTestService.setSurfaceSize(width, height)
    }
}
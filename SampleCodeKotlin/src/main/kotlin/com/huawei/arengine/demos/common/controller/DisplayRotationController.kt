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
package com.huawei.arengine.demos.common.controller

import android.hardware.display.DisplayManager
import android.hardware.display.DisplayManager.DisplayListener
import android.view.WindowManager
import com.huawei.arengine.demos.common.util.getSystemService
import com.huawei.hiar.ARSession

/**
 * Device rotation manager, which is used by the demo to adapt to device rotations
 *
 * @author HW
 * @since 2020-10-10
 */
class DisplayRotationController : DisplayListener {
    /**
     * Check whether the current device is rotated.
     */
    var isDeviceRotation = false

    private var viewPx = 0

    private var viewPy = 0

    private val display by lazy {
        getSystemService<WindowManager>()?.defaultDisplay
    }

    /**
     * Register a listener on display changes.
     * This method can be called when onResume is called for an activity.
     */
    fun registerDisplayListener() {
        getSystemService<DisplayManager>()?.registerDisplayListener(this, null)
    }

    /**
     * Deregister a listener on display changes.
     * This method can be called when onPause is called for an activity.
     */
    fun unregisterDisplayListener() {
        getSystemService<DisplayManager>()?.unregisterDisplayListener(this)
    }

    /**
     * When a device is rotated, the viewfinder size and whether the device is rotated
     * should be updated to correctly display the geometric information returned by the
     * AR Engine. This method should be called when onSurfaceChanged.
     *
     * @param width Width of the surface updated by the device.
     * @param height Height of the surface updated by the device.
     */
    fun updateViewportRotation(width: Int, height: Int) {
        viewPx = width
        viewPy = height
        isDeviceRotation = true
    }

    /**
     * If the device is rotated, update the device window of the current ARSession.
     * This method can be called when onDrawFrame is called.
     *
     * @param session [ARSession] object.
     */
    fun updateArSessionDisplayGeometry(session: ARSession?) {
        display?.rotation?.let { session?.setDisplayGeometry(it, viewPx, viewPy) }
        isDeviceRotation = false
    }

    override fun onDisplayAdded(displayId: Int) {}

    override fun onDisplayRemoved(displayId: Int) {}

    override fun onDisplayChanged(displayId: Int) {
        isDeviceRotation = true
    }
}
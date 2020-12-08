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
package com.huawei.arengine.demos.hand.controller

import android.app.Activity
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import com.huawei.arengine.demos.R
import com.huawei.arengine.demos.common.controller.DisplayRotationController
import com.huawei.arengine.demos.common.exception.SampleAppException
import com.huawei.arengine.demos.common.service.BackgroundTextureService
import com.huawei.arengine.demos.common.service.TextService
import com.huawei.arengine.demos.common.util.FramePerSecond
import com.huawei.arengine.demos.common.util.findViewById
import com.huawei.arengine.demos.common.util.showScreenTextView
import com.huawei.arengine.demos.hand.service.HandBoxService
import com.huawei.arengine.demos.hand.service.HandRenderService
import com.huawei.arengine.demos.hand.service.HandSkeletonLineService
import com.huawei.arengine.demos.hand.service.HandSkeletonService
import com.huawei.arengine.demos.hand.service.updateScreenText
import com.huawei.arengine.demos.hand.util.Constants
import com.huawei.hiar.ARFrame
import com.huawei.hiar.ARHand
import com.huawei.hiar.ARSession
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * This class shows how to render data obtained from HUAWEI AR Engine.
 *
 * @author HW
 * @since 2020-10-10
 */
class HandRenderController(private val activity: Activity,
    private val displayRotationController: DisplayRotationController) : GLSurfaceView.Renderer {
    companion object {
        private const val TAG = "HandRenderController"
    }

    private var arSession: ARSession? = null

    private val fps by lazy { FramePerSecond(0, 0f, 0) }

    private val backgroundTextureService by lazy { BackgroundTextureService() }

    private val handTextService by lazy { TextService() }

    private val handRenderServices by lazy {
        ArrayList<HandRenderService>().apply {
            add(HandBoxService())
            add(HandSkeletonService())
            add(HandSkeletonLineService())
        }
    }

    /**
     * Set the ARSession object, which is used to obtain the latest data in the onDrawFrame method.
     *
     * @param arSession ARSession.
     */
    fun setArSession(arSession: ARSession?) {
        this.arSession = arSession
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        // Clear the original color and set a new color.
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        backgroundTextureService.init()
        for (handRenderService in handRenderServices) {
            handRenderService.init()
        }
        handTextService.setListener { text ->
            showScreenTextView(activity, findViewById(activity, R.id.handTextView), text)
        }
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        backgroundTextureService.updateProjectionMatrix(width, height)
        GLES20.glViewport(0, 0, width, height)
        displayRotationController.updateViewportRotation(width, height)
    }

    override fun onDrawFrame(unused: GL10) {
        // Clear the color buffer and notify the driver not to load the data of the previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        arSession ?: return
        displayRotationController.run {
            if (isDeviceRotation) {
                updateArSessionDisplayGeometry(arSession)
            }
        }
        try {
            val frame = arSession?.run {
                setCameraTextureName(backgroundTextureService.externalTextureId)
                update()
            } ?: return
            backgroundTextureService.renderBackgroundTexture(frame)

            renderHand(frame)
        } catch (e: SampleAppException) {
            Log.e(TAG, "Exception on the OpenGL thread!")
        }
    }

    private fun renderHand(frame: ARFrame) {
        val hands = arSession?.getAllTrackables(ARHand::class.java)
        if (hands == null || hands.isEmpty()) {
            handTextService.drawText(null)
            return
        }
        for (hand in hands) {
            // Update the hand recognition information to be displayed on the screen.
            StringBuilder().let {
                updateScreenText(it, hand, fps)
                // Display hand recognition information on the screen.
                handTextService.drawText(it)
            }
        }

        // Obtain the projection matrix through ARCamera.
        val projectionMatrix = FloatArray(16)
        frame.camera?.getProjectionMatrix(projectionMatrix, 0, Constants.PROJECTION_MATRIX_NEAR,
            Constants.PROJECTION_MATRIX_FAR)
        for (handRenderService in handRenderServices) {
            handRenderService.renderHand(hands, projectionMatrix)
        }
    }
}
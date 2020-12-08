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
package com.huawei.arengine.demos.body3d.controller

import android.app.Activity
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import com.huawei.arengine.demos.R
import com.huawei.arengine.demos.body3d.service.BodyRenderService
import com.huawei.arengine.demos.body3d.service.BodySkeletonLineService
import com.huawei.arengine.demos.body3d.service.BodySkeletonService
import com.huawei.arengine.demos.body3d.service.updateScreenText
import com.huawei.arengine.demos.body3d.util.Constants
import com.huawei.arengine.demos.common.controller.DisplayRotationController
import com.huawei.arengine.demos.common.exception.SampleAppException
import com.huawei.arengine.demos.common.service.BackgroundTextureService
import com.huawei.arengine.demos.common.service.TextService
import com.huawei.arengine.demos.common.util.FramePerSecond
import com.huawei.arengine.demos.common.util.findViewById
import com.huawei.arengine.demos.common.util.showScreenTextView
import com.huawei.hiar.ARBody
import com.huawei.hiar.ARFrame
import com.huawei.hiar.ARSession
import com.huawei.hiar.ARTrackable
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * This class renders personal data obtained by the AR Engine.
 *
 * @author HW
 * @since 2020-10-10
 */
class BodyRenderController(private val mActivity: Activity,
    private val displayRotationController: DisplayRotationController) : GLSurfaceView.Renderer {
    companion object {
        private const val TAG = "BodyRenderController"
    }

    private var mSession: ARSession? = null

    private val fps by lazy { FramePerSecond(0, 0f, 0) }

    private val bodyRenderServices by lazy {
        ArrayList<BodyRenderService>().apply {
            add(BodySkeletonService())
            add(BodySkeletonLineService())
        }
    }

    private val backgroundTextureService by lazy { BackgroundTextureService() }

    private val bodyTextService by lazy { TextService() }

    /**
     * Set the AR session to be updated in onDrawFrame to obtain the latest data.
     *
     * @param arSession ARSession.
     */
    fun setArSession(arSession: ARSession?) {
        mSession = arSession
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        // Clear the color and set the window color.
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        bodyRenderServices.forEach {
            it.init()
        }
        backgroundTextureService.init()
        bodyTextService.setListener { text ->
            showScreenTextView(mActivity, findViewById(mActivity, R.id.bodyTextView), text)
        }
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        backgroundTextureService.updateProjectionMatrix(width, height)
        GLES20.glViewport(0, 0, width, height)
        displayRotationController.updateViewportRotation(width, height)
    }

    override fun onDrawFrame(unused: GL10) {
        // Clear the screen to notify the driver not to load pixels of the previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        mSession ?: return
        displayRotationController.run {
            if (isDeviceRotation) {
                updateArSessionDisplayGeometry(mSession)
            }
        }
        try {
            val frame = mSession?.run {
                setCameraTextureName(backgroundTextureService.externalTextureId)
                update()
            }
            backgroundTextureService.renderBackgroundTexture(frame)
            renderBody(frame)
        } catch (exception: SampleAppException) {
            Log.e(TAG, "Exception on the OpenGL thread!")
        }
    }

    private fun renderBody(frame: ARFrame?) {
        val bodies = mSession?.getAllTrackables(ARBody::class.java)
        if (bodies == null || bodies.isEmpty()) {
            bodyTextService.drawText(null)
            return
        }
        bodies.forEach { body ->
            if (body.trackingState == ARTrackable.TrackingState.TRACKING) {
                // Update the body recognition information to be displayed on the screen.
                StringBuilder().let {
                    updateScreenText(it, body, fps)
                    // Display the updated body information on the screen.
                    bodyTextService.drawText(it)
                }
            }
        }

        // The size of the projection matrix is 4 * 4.
        val projectionMatrix = FloatArray(16)

        // Obtain the projection matrix of ARCamera.
        frame?.camera?.getProjectionMatrix(projectionMatrix, 0, Constants.PROJECTION_MATRIX_NEAR,
            Constants.PROJECTION_MATRIX_FAR)
        bodyRenderServices.forEach { bodyRenderService ->
            bodyRenderService.renderBody(bodies, projectionMatrix)
        }
    }
}
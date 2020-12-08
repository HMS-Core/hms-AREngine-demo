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

import android.app.Activity
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.View
import android.widget.TextView
import com.huawei.arengine.demos.R
import com.huawei.arengine.demos.common.controller.DisplayRotationController
import com.huawei.arengine.demos.common.exception.SampleAppException
import com.huawei.arengine.demos.common.service.BackgroundTextureService
import com.huawei.arengine.demos.common.service.TextService
import com.huawei.arengine.demos.common.util.FramePerSecond
import com.huawei.arengine.demos.common.util.findViewById
import com.huawei.arengine.demos.common.util.showScreenTextView
import com.huawei.arengine.demos.world.service.LabelService
import com.huawei.arengine.demos.world.service.ObjectService
import com.huawei.arengine.demos.world.service.updateScreenText
import com.huawei.arengine.demos.world.util.Constants
import com.huawei.hiar.ARFrame
import com.huawei.hiar.ARLightEstimate
import com.huawei.hiar.ARPlane
import com.huawei.hiar.ARSession
import com.huawei.hiar.ARTrackable
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * This class provides rendering management related to the world scene, including
 * label rendering and virtual object rendering management.
 *
 * @author HW
 * @since 2020-10-10
 */
class WorldRenderController(private val activity: Activity,
    private val displayRotationController: DisplayRotationController,
    private val gestureController: GestureController) : GLSurfaceView.Renderer {
    companion object {
        private const val TAG = "WorldRenderController"
    }

    private var arSession: ARSession? = null

    private val fps by lazy { FramePerSecond(0, 0f, 0) }

    private val backgroundTextureService by lazy { BackgroundTextureService() }

    private val worldTextService by lazy { TextService() }

    private val labelService by lazy { LabelService() }

    private val objectService by lazy { ObjectService() }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        // Set the window color.
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        backgroundTextureService.init()
        objectService.init()
        labelService.init(activity)
        worldTextService.setListener { text ->
            showScreenTextView(activity, findViewById(activity, R.id.wordTextView), text)
        }
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        backgroundTextureService.updateProjectionMatrix(width, height)
        GLES20.glViewport(0, 0, width, height)
        displayRotationController.updateViewportRotation(width, height)
        gestureController.setSurfaceSize(width.toFloat(), height.toFloat())
    }

    override fun onDrawFrame(unused: GL10) {
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

            StringBuilder().let {
                updateScreenText(it, fps)
                worldTextService.drawText(it)
            }

            renderLabelAndObjects(frame)
        } catch (e: SampleAppException) {
            Log.e(TAG, "Exception on the ArDemoRuntimeException!")
        } catch (t: Throwable) {
            Log.e(TAG, "Exception on the OpenGL thread: ", t)
        }
    }

    private fun renderLabelAndObjects(frame: ARFrame) {
        val planes = arSession?.getAllTrackables(ARPlane::class.java) ?: return
        for (plane in planes) {
            if (plane.type != ARPlane.PlaneType.UNKNOWN_FACING
                && plane.trackingState == ARTrackable.TrackingState.TRACKING) {
                hideLoadingMessage()
                break
            }
        }

        val projectionMatrix = FloatArray(16)
        val viewMatrix = FloatArray(16)
        frame.camera.apply {
            // Do not perform anything when the object is not tracked.
            if (trackingState != ARTrackable.TrackingState.TRACKING) {
                return
            }
            getProjectionMatrix(projectionMatrix, 0, Constants.PROJ_MATRIX_NEAR, Constants.PROJ_MATRIX_FAR)
            getViewMatrix(viewMatrix, 0)
            labelService.renderLabels(planes, displayOrientedPose, projectionMatrix)
        }.also {
            gestureController.handleGestureEvent(frame, it, projectionMatrix, viewMatrix)
        }

        var lightPixelIntensity = 1f
        frame.lightEstimate.run {
            if (state != ARLightEstimate.State.NOT_VALID) {
                lightPixelIntensity = pixelIntensity
            }
        }
        drawAllObjects(projectionMatrix, viewMatrix, lightPixelIntensity)
        return
    }

    private fun drawAllObjects(projectionMatrix: FloatArray, viewMatrix: FloatArray, lightPixelIntensity: Float) {
        val ite = gestureController.virtualObjects.iterator()
        while (ite.hasNext()) {
            val obj = ite.next()
            obj.getArAnchor().run {
                if (trackingState == ARTrackable.TrackingState.STOPPED) {
                    ite.remove()
                }
                if (trackingState == ARTrackable.TrackingState.TRACKING) {
                    objectService.renderObjects(viewMatrix, projectionMatrix, lightPixelIntensity, obj)
                }
            }
        }
    }

    private fun hideLoadingMessage() {
        activity.runOnUiThread {
            findViewById<TextView>(activity, R.id.searchingTextView).visibility = View.GONE
        }
    }

    /**
     * Set ARSession, which will update and obtain the latest data in OnDrawFrame.
     *
     * @param arSession ARSession.
     */
    fun setArSession(arSession: ARSession?) {
        this.arSession = arSession
    }
}
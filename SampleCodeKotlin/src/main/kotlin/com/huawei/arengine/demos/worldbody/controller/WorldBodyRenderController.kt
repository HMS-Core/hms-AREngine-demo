/**
 * Copyright 2021. Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huawei.arengine.demos.worldbody

import android.app.Activity
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.view.View
import android.widget.TextView
import com.huawei.arengine.demos.R
import com.huawei.arengine.demos.body3d.service.BodyRenderService
import com.huawei.arengine.demos.body3d.service.BodySkeletonLineService
import com.huawei.arengine.demos.body3d.service.BodySkeletonService
import com.huawei.arengine.demos.common.LogUtil
import com.huawei.arengine.demos.common.controller.DisplayRotationController
import com.huawei.arengine.demos.common.exception.SampleAppException
import com.huawei.arengine.demos.common.service.BackgroundTextureService
import com.huawei.arengine.demos.common.service.TextService
import com.huawei.arengine.demos.common.util.*
import com.huawei.arengine.demos.common.view.ConnectAppMarketActivity
import com.huawei.arengine.demos.world.controller.GestureController
import com.huawei.arengine.demos.world.model.VirtualObject
import com.huawei.arengine.demos.world.service.LabelService
import com.huawei.arengine.demos.world.service.ObjectService
import com.huawei.arengine.demos.world.service.PointService
import com.huawei.arengine.demos.world.util.Constants
import com.huawei.hiar.ARFrame
import com.huawei.hiar.ARSession
import com.huawei.hiar.ARTrackable
import com.huawei.hiar.ARWorldBodyTrackingConfig
import com.huawei.hiar.ARConfigBase
import com.huawei.hiar.ARBody
import com.huawei.hiar.exceptions.ARUnavailableClientSdkTooOldException
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException
import com.huawei.hiar.exceptions.ARUnavailableServiceNotInstalledException
import com.huawei.hiar.exceptions.ARUnSupportedConfigurationException
import com.huawei.hiar.exceptions.ARCameraNotAvailableException
import kotlinx.android.synthetic.main.world_java_activity_main.surfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * 这个类用来管理和绘制WorldBody的图形数据，包括人体骨骼、点云、虚拟物体
 *
 * @author HW
 * @since 2021-04-08
 */
class WorldBodyRenderController(private val activity: Activity,
    private val displayRotationController: DisplayRotationController,
    private val gestureController: GestureController) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "WorldBodyRenderController"
    }

    var arSession: ARSession? = null

    private val backgroundTextureService by lazy { BackgroundTextureService() }

    private val pointService by lazy { PointService() }

    private val labelService by lazy { LabelService() }

    private val textService by lazy { TextService() }

    private val objectService by lazy { ObjectService() }

    private val bodyRenderServices by lazy {
        ArrayList<BodyRenderService>().apply {
            add(BodySkeletonService())
            add(BodySkeletonLineService())
        }
    }

    private var frames = 0

    private var lastInterval: Long = 0

    private var fps = 0f

    fun startArSession() {
        arSession?.let {
            resumeSurfaceView()
            return
        }
        var message: String? = null
        try {
            if (!isAvailableArEngine(activity)) {
                activity.finish()
                return
            }
            arSession = ARSession(activity)
            ARWorldBodyTrackingConfig(arSession).apply {
                focusMode = ARConfigBase.FocusMode.AUTO_FOCUS
                semanticMode = ARWorldBodyTrackingConfig.SEMANTIC_PLANE
            }.also {
                arSession?.configure(it)
            }
        } catch (e: ARUnavailableServiceNotInstalledException) {
            startActivityByType<ConnectAppMarketActivity>()
        } catch (e: ARUnavailableServiceApkTooOldException) {
            message = "Please update HuaweiARService.apk"
        } catch (e: ARUnavailableClientSdkTooOldException) {
            message = "Please update this app"
        } catch (e: ARUnSupportedConfigurationException) {
            message = "The configuration is not supported by the device!"
        } catch (e: Exception) {
            message = "exception throw"
        }
        message?.let {
            arSession?.stop()
            arSession = null
            return
        }
        resumeSurfaceView()
    }

    private fun resumeSurfaceView() {
        if (!isSuccessResumeSession()) return
        displayRotationController.registerDisplayListener()
        activity.surfaceView.onResume()
    }

    private fun isSuccessResumeSession(): Boolean {
        return try {
            arSession?.resume()
            true
        } catch (e: ARCameraNotAvailableException) {
            arSession = null
            false
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (arSession == null) {
            return
        }
        if (displayRotationController.isDeviceRotation) {
            displayRotationController.updateArSessionDisplayGeometry(arSession)
        }
        try {
            arSession!!.setCameraTextureName(backgroundTextureService.externalTextureId)
            val arFrame: ARFrame = arSession!!.update()
            LogUtil.debug(TAG, "mTextureDisplay.onDrawFrame(arFrame);")
            backgroundTextureService.renderBackgroundTexture(arFrame)
            val arCamera = arFrame.camera

            if (arCamera.trackingState == ARTrackable.TrackingState.PAUSED) {
                LogUtil.debug(TAG, "Camera TrackingState Paused: ")
                showSearchingMessage(View.VISIBLE)
                return
            }

            // The size of the projection matrix is 4 * 4.
            val projectionMatrix = FloatArray(16)
            arCamera.getProjectionMatrix(projectionMatrix, 0, Constants.PROJ_MATRIX_NEAR, Constants.PROJ_MATRIX_FAR)

            // The size of ViewMatrix is 4 * 4.
            val viewMatrix = FloatArray(16)
            arCamera.getViewMatrix(viewMatrix, 0)
            showSearchingMessage(View.GONE)
            gestureController.handleGestureEvent(arFrame, arCamera, projectionMatrix, viewMatrix)
            // handleGestureEvent(arFrame, arCamera)
            drawAllObjects(projectionMatrix, viewMatrix)
            val arPointCloud = arFrame.acquirePointCloud()
            pointService.renderPoints(arPointCloud, viewMatrix, projectionMatrix)
            val bodies: Collection<ARBody> = arSession!!.getAllTrackables<ARBody>(ARBody::class.java)
            val sb = StringBuilder()
            updateMessageData(sb, bodies)
            textService.drawText(sb)
            for (bodyRelatedService in bodyRenderServices) {
                bodyRelatedService.renderBody(bodies, projectionMatrix)
            }
            LogUtil.debug(TAG, "after worldBody display.")
        } catch (e: SampleAppException) {
            LogUtil.error(TAG, "Exception on the ArDemoRuntimeException!")
        } catch (t: Throwable) {
            // This prevents the app from crashing due to unhandled exceptions.
            LogUtil.error(TAG, "Exception on the OpenGL thread")
        }
    }

    private fun showSearchingMessage(state: Int) {
        if (findViewById<TextView>(activity, R.id.searchingTextView).visibility != state) {
            activity.runOnUiThread(Runnable {
                findViewById<TextView>(activity, R.id.searchingTextView).visibility = state
            })
        }
    }

    private fun drawAllObjects(projectionMatrix: FloatArray, viewMatrix: FloatArray) {
        val ite: MutableIterator<VirtualObject> = gestureController.virtualObjects.iterator()
        while (ite.hasNext()) {
            val obj: VirtualObject = ite.next()
            if (obj.getArAnchor().trackingState == ARTrackable.TrackingState.STOPPED) {
                ite.remove()
            }
            if (obj.getArAnchor().trackingState == ARTrackable.TrackingState.TRACKING) {
                // Light intensity 1.
                objectService.renderObjects(viewMatrix, projectionMatrix, 1.0f, obj)
            }
        }
    }

    private fun doFpsCalculate(): Float {
        ++frames
        val timeNow = System.currentTimeMillis()

        // Convert millisecond to second.
        if ((timeNow - lastInterval) / 1000.0f > 0.5f) {
            fps = frames / ((timeNow - lastInterval) / 1000.0f)
            frames = 0
            lastInterval = timeNow
        }
        return fps
    }

    /**
     * Update the information to be displayed on the screen.
     *
     * @param sb String buffer.
     * @param bodies identified ARBody.
     */
    private fun updateMessageData(sb: StringBuilder, bodies: Collection<ARBody>) {
        val fpsResult: Float = doFpsCalculate()
        sb.append("FPS=").append(fpsResult).append(System.lineSeparator())
        var trackingBodySum = 0
        for (body in bodies) {
            if (body.trackingState != ARTrackable.TrackingState.TRACKING) {
                continue
            }
            trackingBodySum++
            sb.append("body action: ").append(body.bodyAction).append(System.lineSeparator())
        }
        sb.append("tracking body sum: ").append(trackingBodySum).append(System.lineSeparator())
        sb.append("Virtual Object number: ").append(gestureController.virtualObjects.size).append(System.lineSeparator())
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        backgroundTextureService.updateProjectionMatrix(width, height)
        GLES20.glViewport(0, 0, width, height)
        displayRotationController.updateViewportRotation(width, height)
        gestureController.setSurfaceSize(width.toFloat(), height.toFloat())
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        backgroundTextureService.init()
        pointService.init()
        labelService.init(activity)
        objectService.init()
        bodyRenderServices.forEach {
            it.init()
        }
        textService.setListener { text ->
            showScreenTextView(activity, findViewById(activity, R.id.wordTextView), text)
        }
    }
}
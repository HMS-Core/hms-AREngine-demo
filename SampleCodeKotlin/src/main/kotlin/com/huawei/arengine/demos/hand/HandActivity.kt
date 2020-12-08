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
package com.huawei.arengine.demos.hand

import android.app.Activity
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.huawei.arengine.demos.R
import com.huawei.arengine.demos.common.controller.DisplayRotationController
import com.huawei.arengine.demos.common.util.isAvailableArEngine
import com.huawei.arengine.demos.common.util.startActivityByType
import com.huawei.arengine.demos.common.view.ConnectAppMarketActivity
import com.huawei.arengine.demos.hand.controller.HandRenderController
import com.huawei.hiar.ARConfigBase
import com.huawei.hiar.ARHandTrackingConfig
import com.huawei.hiar.ARSession
import com.huawei.hiar.exceptions.ARCameraNotAvailableException
import com.huawei.hiar.exceptions.ARUnSupportedConfigurationException
import com.huawei.hiar.exceptions.ARUnavailableClientSdkTooOldException
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException
import com.huawei.hiar.exceptions.ARUnavailableServiceNotInstalledException
import kotlinx.android.synthetic.main.hand_activity_main.handSurfaceView

/**
 * Identify hand information and output the identified gesture type, and coordinates of
 * the left hand, right hand, and palm bounding box. When there are multiple hands in an
 * image, only the recognition results and coordinates from the clearest captured hand,
 * with the highest degree of confidence, are sent back. This feature supports front and
 * rear cameras.
 *
 * @author HW
 * @since 2020-10-10
 */
class HandActivity : Activity() {
    companion object {
        private const val TAG = "HandActivity"
    }

    private var arSession: ARSession? = null

    private val displayRotationController by lazy { DisplayRotationController() }

    private val handRenderController by lazy {
        HandRenderController(this, displayRotationController)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hand_activity_main)
        initUi()
    }

    private fun initUi() {
        handSurfaceView.run {
            // Keep the OpenGL ES running context.
            preserveEGLContextOnPause = true
            // Set the OpenGLES version.
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            setRenderer(handRenderController)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()
        arSession?.let {
            resumeView()
            return
        }
        var message: String? = null
        try {
            if (!isAvailableArEngine(this)) {
                finish()
                return
            }
            arSession = ARSession(this)
            ARHandTrackingConfig(arSession).apply {
                cameraLensFacing = ARConfigBase.CameraLensFacing.FRONT
                powerMode = ARConfigBase.PowerMode.ULTRA_POWER_SAVING
                enableItem = ARConfigBase.ENABLE_DEPTH.toLong()
            }.also {
                arSession?.configure(it)
            }
            handRenderController.setArSession(arSession)
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
            Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            stopArSession()
            return
        }
        resumeView()
    }

    private fun resumeView() {
        if (!isSuccessResumeSession()) return
        displayRotationController.registerDisplayListener()
        handSurfaceView.onResume()
    }

    private fun isSuccessResumeSession(): Boolean {
        return try {
            arSession?.resume()
            true
        } catch (e: ARCameraNotAvailableException) {
            Toast.makeText(this, "Camera open failed, please restart the app", Toast.LENGTH_LONG).show()
            arSession = null
            false
        }
    }

    private fun stopArSession() {
        Log.i(TAG, "stopArSession start.")
        arSession?.stop()
        arSession = null
        Log.i(TAG, "stopArSession end.")
    }

    override fun onPause() {
        Log.i(TAG, "onPause start.")
        super.onPause()
        displayRotationController.unregisterDisplayListener()
        handSurfaceView.onPause()
        arSession?.pause()
        Log.i(TAG, "onPause end.")
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy start.")
        super.onDestroy()
        arSession?.stop()
        arSession = null
        Log.i(TAG, "onDestroy end.")
    }

    override fun onWindowFocusChanged(isHasFocus: Boolean) {
        super.onWindowFocusChanged(isHasFocus)
        if (!isHasFocus) return
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }
}
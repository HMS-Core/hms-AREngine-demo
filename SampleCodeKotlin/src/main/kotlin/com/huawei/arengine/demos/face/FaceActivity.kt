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
package com.huawei.arengine.demos.face


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
import com.huawei.arengine.demos.face.controller.CameraController
import com.huawei.arengine.demos.face.controller.FaceRenderController
import com.huawei.hiar.ARConfigBase
import com.huawei.hiar.ARFaceTrackingConfig
import com.huawei.hiar.ARSession
import com.huawei.hiar.exceptions.ARCameraNotAvailableException
import com.huawei.hiar.exceptions.ARUnSupportedConfigurationException
import com.huawei.hiar.exceptions.ARUnavailableClientSdkTooOldException
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException
import com.huawei.hiar.exceptions.ARUnavailableServiceNotInstalledException
import kotlinx.android.synthetic.main.face_activity_main.faceSurfaceView

/**
 * This demo shows the capabilities of HUAWEI AR Engine to recognize faces, including facial
 * features and facial expressions. In addition, this demo shows how an app can open the camera
 * to display preview.Currently, only apps of the ARface type can open the camera. If you want
 * to the app to open the camera, set isOpenCameraOutside = true in this file.
 *
 * @author HW
 * @since 2020-10-10
 */
class FaceActivity : Activity() {
    companion object {
        private const val TAG = "FaceActivity"
    }

    private val isOpenCameraOutside = true

    private var arSession: ARSession? = null

    private lateinit var arConfig: ARConfigBase

    private val cameraController by lazy { CameraController(this) }

    private val displayRotationController by lazy { DisplayRotationController() }

    private val faceRenderController by lazy {
        FaceRenderController(this, displayRotationController, isOpenCameraOutside)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.face_activity_main)
        initUi()
    }

    private fun initUi() {
        faceSurfaceView.run {
            preserveEGLContextOnPause = true
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            setRenderer(faceRenderController)
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
            arConfig = ARFaceTrackingConfig(arSession).apply {
                powerMode = ARConfigBase.PowerMode.POWER_SAVING
                if (isOpenCameraOutside) {
                    imageInputMode = ARConfigBase.ImageInputMode.EXTERNAL_INPUT_ALL
                }
            }.also { arSession?.configure(it) }
            arSession?.let {
                faceRenderController.setArSession(it)
                cameraController.arSession = it
                cameraController.arConfig = arConfig
            }
        } catch (e: ARUnavailableServiceNotInstalledException) {
            startActivityByType<ConnectAppMarketActivity>()
        } catch (e: ARUnavailableServiceApkTooOldException) {
            message = "Please update AREngineServer.apk"
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
        if (isOpenCameraOutside) {
            cameraController.run {
                startCameraService()
                faceRenderController.mTextureId = textureId
            }
        }
        faceSurfaceView.onResume()
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
        Log.i(TAG, "Stop session start.")
        arSession?.stop()
        arSession = null
        Log.i(TAG, "Stop session end.")
    }

    public override fun onPause() {
        Log.i(TAG, "onPause start.")
        super.onPause()
        if (isOpenCameraOutside) {
            cameraController.run {
                closeCamera()
                stopCameraThread()
                cameraService = null
            }
        }
        displayRotationController.unregisterDisplayListener()
        arSession?.pause()
        faceSurfaceView.onPause()
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
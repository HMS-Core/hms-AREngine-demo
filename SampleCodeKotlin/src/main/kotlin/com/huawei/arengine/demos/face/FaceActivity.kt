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
package com.huawei.arengine.demos.face

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast

import com.huawei.arengine.demos.common.LogUtil
import com.huawei.arengine.demos.common.controller.DisplayRotationController
import com.huawei.arengine.demos.common.service.PermissionManageService
import com.huawei.arengine.demos.common.util.isAvailableArEngine
import com.huawei.arengine.demos.common.view.BaseActivity
import com.huawei.arengine.demos.databinding.FaceActivityMainBinding
import com.huawei.arengine.demos.face.controller.CameraController
import com.huawei.arengine.demos.face.controller.FaceRenderController
import com.huawei.hiar.ARConfigBase
import com.huawei.hiar.ARFaceTrackingConfig
import com.huawei.hiar.ARSession
import com.huawei.hiar.exceptions.ARCameraNotAvailableException

/**
 * This demo shows the capabilities of HUAWEI AR Engine to recognize faces, including facial
 * features and facial expressions. In addition, this demo shows how an app can open the camera
 * to display preview.Currently, only apps of the ARface type can open the camera. If you want
 * to the app to open the camera, set isOpenCameraOutside = true in this file.
 *
 * @author HW
 * @since 2020-10-10
 */
class FaceActivity : BaseActivity() {
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

    private lateinit var faceActivityBinding: FaceActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        faceActivityBinding = FaceActivityMainBinding.inflate(layoutInflater)
        setContentView(faceActivityBinding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        initUi()
    }

    private fun initUi() {
        faceActivityBinding.faceSurfaceView.run {
            preserveEGLContextOnPause = true
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            setRenderer(faceRenderController)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    override fun onResume() {
        LogUtil.debug(TAG, "onResume")
        super.onResume()
        if (!PermissionManageService.hasPermission()) {
            finish()
        }
        errorMessage = null
        arSession?.let {
            resumeView()
            return
        }
        try {
            if (!isAvailableArEngine(this)) {
                finish()
                return
            }
            arSession = ARSession(this.applicationContext)
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
        } catch (capturedException: Exception) {
            setMessageWhenError(capturedException)
        }
        errorMessage?.let {
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
        faceActivityBinding.faceSurfaceView.onResume()
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
        LogUtil.info(TAG, "Stop session start.")
        arSession?.stop()
        arSession = null
        LogUtil.info(TAG, "Stop session end.")
    }

    public override fun onPause() {
        LogUtil.info(TAG, "onPause start.")
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
        faceActivityBinding.faceSurfaceView.onPause()
        LogUtil.info(TAG, "onPause end.")
    }

    override fun onDestroy() {
        LogUtil.info(TAG, "onDestroy start.")
        super.onDestroy()
        arSession?.stop()
        arSession = null
        LogUtil.info(TAG, "onDestroy end.")
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
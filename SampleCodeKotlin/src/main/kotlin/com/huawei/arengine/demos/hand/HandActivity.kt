/*
 * Copyright 2023. Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huawei.arengine.demos.hand

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast

import com.huawei.arengine.demos.R
import com.huawei.arengine.demos.common.LogUtil
import com.huawei.arengine.demos.common.controller.DisplayRotationController
import com.huawei.arengine.demos.common.service.PermissionManageService
import com.huawei.arengine.demos.common.util.isAvailableArEngine
import com.huawei.arengine.demos.common.view.BaseActivity
import com.huawei.arengine.demos.databinding.HandActivityMainBinding
import com.huawei.arengine.demos.hand.controller.HandRenderController
import com.huawei.hiar.ARConfigBase
import com.huawei.hiar.ARHandTrackingConfig
import com.huawei.hiar.ARSession
import com.huawei.hiar.exceptions.ARCameraNotAvailableException

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
class HandActivity : BaseActivity() {
    companion object {
        private const val TAG = "HandActivity"
    }

    private var arSession: ARSession? = null

    private var cameraFacingButton: Button? = null

    private var mCameraLensFacing: ARConfigBase.CameraLensFacing = ARConfigBase.CameraLensFacing.FRONT

    private val displayRotationController by lazy { DisplayRotationController() }

    private val handRenderController by lazy {
        HandRenderController(this, displayRotationController)
    }

    private lateinit var handActivityBinding: HandActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handActivityBinding = HandActivityMainBinding.inflate(layoutInflater)
        setContentView(handActivityBinding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        initUi()
    }

    private fun initUi() {
        handActivityBinding.handSurfaceView.run {
            // Keep the OpenGL ES running context.
            preserveEGLContextOnPause = true
            // Set the OpenGLES version.
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            setRenderer(handRenderController)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
        cameraFacingButton = findViewById(R.id.cameraFacingButton)
        initCameraFacingClickListener()
    }

    private fun initCameraFacingClickListener() {
        cameraFacingButton?.setOnClickListener {
            cameraFacingButton?.isEnabled = false
            stopArSession()
            handActivityBinding.handSurfaceView.onPause()
            arSession?.pause()
            mCameraLensFacing = if (mCameraLensFacing == ARConfigBase.CameraLensFacing.FRONT) {
                ARConfigBase.CameraLensFacing.REAR
            } else {
                ARConfigBase.CameraLensFacing.FRONT
            }
            setArConfig()
            cameraFacingButton?.isEnabled = true
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
        setArConfig()
    }

    private fun setArConfig() {
        try {
            if (!isAvailableArEngine(this)) {
                finish()
                return
            }
            arSession = ARSession(this.applicationContext)
            ARHandTrackingConfig(arSession).apply {
                cameraLensFacing = mCameraLensFacing
                powerMode = ARConfigBase.PowerMode.ULTRA_POWER_SAVING
                enableItem = ARConfigBase.ENABLE_DEPTH.toLong()
                arConfigBase = this
            }.also {
                arSession?.configure(it)
            }
        } catch (capturedException: Exception) {
            // This prevents the app from crashing due to unhandled exceptions.
            setMessageWhenError(capturedException)
        } finally {
            showCapabilitySupportInfo()
        }
        errorMessage?.let {
            Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            stopArSession()
            return
        }
        handRenderController.setArSession(arSession)
        resumeView()
    }

    private fun resumeView() {
        if (!isSuccessResumeSession()) return
        displayRotationController.registerDisplayListener()
        handActivityBinding.handSurfaceView.onResume()
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
        LogUtil.info(TAG, "stopArSession start.")
        arSession?.stop()
        arSession = null
        LogUtil.info(TAG, "stopArSession end.")
    }

    override fun onPause() {
        LogUtil.info(TAG, "onPause start.")
        super.onPause()
        displayRotationController.unregisterDisplayListener()
        handActivityBinding.handSurfaceView.onPause()
        arSession?.pause()
        LogUtil.info(TAG, "onPause end.")
    }

    override fun onDestroy() {
        LogUtil.info(TAG, "onDestroy start.")
        super.onDestroy()
        arSession?.stop()
        arSession = null
        LogUtil.info(TAG, "onDestroy end.")
    }

    private fun showCapabilitySupportInfo() {
        if (arConfigBase == null) {
            LogUtil.warn(TAG, "showCapabilitySupportInfo arConfigBase is null.")
            return
        }
        val runningDetectionInfo =
            if (arConfigBase!!.enableItem and ARConfigBase.ENABLE_DEPTH.toLong() != 0L) "3D" else "2D"
        Toast.makeText(this, String.format("%s detection mode is enabled.", runningDetectionInfo), Toast.LENGTH_LONG)
            .show()
    }
}
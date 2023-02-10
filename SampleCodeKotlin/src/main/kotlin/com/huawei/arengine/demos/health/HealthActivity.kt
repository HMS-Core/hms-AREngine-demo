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

package com.huawei.arengine.demos.health

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast

import com.huawei.arengine.demos.common.LogUtil
import com.huawei.arengine.demos.common.controller.DisplayRotationController
import com.huawei.arengine.demos.common.service.PermissionManageService
import com.huawei.arengine.demos.common.util.SecurityUtil
import com.huawei.arengine.demos.common.util.isAvailableArEngine
import com.huawei.arengine.demos.common.view.BaseActivity
import com.huawei.arengine.demos.databinding.HealthActivityMainBinding
import com.huawei.arengine.demos.health.controller.HealthRenderController
import com.huawei.arengine.demos.health.util.Constants
import com.huawei.hiar.ARConfigBase
import com.huawei.hiar.ARFaceTrackingConfig
import com.huawei.hiar.ARSession
import com.huawei.hiar.exceptions.ARCameraNotAvailableException
import com.huawei.hiar.listener.FaceHealthCheckStateEvent
import com.huawei.hiar.listener.FaceHealthServiceListener

import java.util.EventObject

/**
 * Shows the usage of APIs related to health data monitoring.
 *
 * @author HW
 * @since 2021-11-23
 */
class HealthActivity : BaseActivity() {
    companion object {
        private const val TAG = "HealthActivity"
    }

    private var arSession: ARSession? = null

    private val displayRotationController by lazy {
        DisplayRotationController()
    }

    private val healthRenderController by lazy {
        HealthRenderController(this, this, displayRotationController)
    }

    private lateinit var healthActivityBinding: HealthActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        healthActivityBinding = HealthActivityMainBinding.inflate(layoutInflater)
        setContentView(healthActivityBinding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        initUi()
    }

    private fun initUi() {
        healthActivityBinding.healthSurfaceView.run {
            preserveEGLContextOnPause = true
            setEGLContextClientVersion(Constants.EGL_CLIENT_VERSION)
            setEGLConfigChooser(Constants.EGL_BUFFER_BIT, Constants.EGL_BUFFER_BIT,
                Constants.EGL_BUFFER_BIT, Constants.EGL_BUFFER_BIT, Constants.EGL_BUFFER_DEPTH, 0)
            healthRenderController.setHealthParamTable(healthActivityBinding.healthParamTable)
            setRenderer(healthRenderController)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    override fun onResume() {
        LogUtil.debug(TAG, "onResume")
        super.onResume()
        if (!PermissionManageService.hasPermission()) {
            SecurityUtil.safeFinishActivity(this)
        }
        errorMessage = null
        arSession?.let {
            resumeView()
            return
        }
        try {
            if (!isAvailableArEngine(this)) {
                SecurityUtil.safeFinishActivity(this)
                return
            }
            arSession = ARSession(this.applicationContext)
            ARFaceTrackingConfig(arSession).apply {
                enableItem = ARConfigBase.ENABLE_HEALTH_DEVICE.toLong()
                faceDetectMode = ARConfigBase.FaceDetectMode.HEALTH_ENABLE_DEFAULT.enumValue
                arConfigBase = this
            }.also {
                arSession?.configure(it)
            }
            setHealthServiceListener()
        } catch (capturedException: Exception) {
            // Capture and classify exceptions and process them in a unified manner.
            setMessageWhenError(capturedException)
        } finally {
            showCapabilitySupportInfo()
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
        healthRenderController.setArSession(arSession)
        healthActivityBinding.healthSurfaceView.onResume()
    }

    private fun stopArSession() {
        LogUtil.info(TAG, "Stop session start.")
        arSession?.stop()
        arSession = null
        LogUtil.info(TAG, "Stop session end.")
    }

    private fun isSuccessResumeSession(): Boolean {
        return try {
            arSession?.resume()
            true
        } catch (exception: ARCameraNotAvailableException) {
            Toast.makeText(this, "Camera open failed, please restart the app", Toast.LENGTH_LONG).show()
            arSession = null
            false
        }
    }

    override fun onPause() {
        LogUtil.info(TAG, "onPause start.")
        super.onPause()
        displayRotationController.unregisterDisplayListener()
        healthActivityBinding.healthSurfaceView.onPause()
        arSession?.pause()
        LogUtil.info(TAG, "onPause end.")
    }

    override fun onDestroy() {
        LogUtil.info(TAG, "onDestroy start.")
        super.onDestroy()
        super.onDestroy()
        arSession?.stop()
        arSession = null
        LogUtil.info(TAG, "onDestroy end.")
    }

    private fun setHealthServiceListener() {
        arSession?.addServiceListener(object : FaceHealthServiceListener {
            override fun handleEvent(eventObject: EventObject) {
                if (eventObject !is FaceHealthCheckStateEvent) {
                    return
                }
                val faceHealthCheckState = eventObject.faceHealthCheckState
                runOnUiThread {
                    healthActivityBinding.healthCheckStatus.setText(faceHealthCheckState.toString())
                }
            }

            override fun handleProcessProgressEvent(progress: Int) {
                healthRenderController.setHealthCheckProgress(progress)
                runOnUiThread {
                    setProgressTips(progress)
                }
            }
        })
    }

    private fun setProgressTips(progress: Int) {
        val progressTips = if (progress < Constants.MAX_PROGRESS) "processing" else "finish"
        healthActivityBinding.processTips.setText(progressTips)
        healthActivityBinding.healthProgressBar.setProgress(progress)
    }

    /**
     * For HUAWEI AR Engine 3.18 or later versions, you can call configure of ARSession, then call getEnableItem to
     * check whether the current device supports health check.
     */
    private fun showCapabilitySupportInfo() {
        if (arConfigBase == null) {
            LogUtil.warn(TAG, "showCapabilitySupportInfo mArFaceTrackingConfig is null.")
            return
        }
        if (arConfigBase!!.enableItem and ARConfigBase.ENABLE_HEALTH_DEVICE.toLong() == 0L) {
            Toast.makeText(this, "The device does not support ENABLE_HEALTH_DEVICE.", Toast.LENGTH_LONG).show()
        }
    }
}
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

package com.huawei.arengine.demos.body3d

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast

import com.huawei.arengine.demos.body3d.controller.BodyRenderController
import com.huawei.arengine.demos.common.LogUtil
import com.huawei.arengine.demos.common.controller.DisplayRotationController
import com.huawei.arengine.demos.common.service.PermissionManageService
import com.huawei.arengine.demos.common.util.isAvailableArEngine
import com.huawei.arengine.demos.common.view.BaseActivity
import com.huawei.arengine.demos.databinding.Body3dActivityMainBinding
import com.huawei.hiar.ARBodyTrackingConfig
import com.huawei.hiar.ARConfigBase
import com.huawei.hiar.ARSession
import com.huawei.hiar.exceptions.ARCameraNotAvailableException

/**
 * The sample code demonstrates the capability of HUAWEI AR Engine to identify
 * body skeleton points and output human body features such as limb endpoints,
 * body posture, and skeleton.
 *
 * @author HW
 * @since 2020-10-10
 */
class BodyActivity : BaseActivity() {
    companion object {
        private const val TAG = "BodyActivity"
    }

    private var arSession: ARSession? = null

    private val displayRotationController by lazy { DisplayRotationController() }

    private val bodyRenderController by lazy {
        BodyRenderController(this, displayRotationController)
    }

    private lateinit var bodyActivityBinding: Body3dActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        LogUtil.debug(TAG, "onCreate start")
        super.onCreate(savedInstanceState)
        bodyActivityBinding = Body3dActivityMainBinding.inflate(layoutInflater)
        setContentView(bodyActivityBinding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        initUi()
        LogUtil.debug(TAG, "onCreate end")
    }

    private fun initUi() {
        bodyActivityBinding.bodySurfaceView.run {
            preserveEGLContextOnPause = true
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            setRenderer(bodyRenderController)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    override fun onResume() {
        LogUtil.debug(TAG, "onResume start")
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
            ARBodyTrackingConfig(arSession).apply {
                enableItem = (ARConfigBase.ENABLE_DEPTH or ARConfigBase.ENABLE_MASK).toLong()
                arConfigBase = this
            }.also { arSession?.configure(it) }
        } catch (capturedException: Exception) {
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
        LogUtil.debug(TAG, "onResume end")
    }

    private fun resumeView() {
        if (!isSuccessResumeSession()) return
        bodyRenderController.setArSession(arSession)
        displayRotationController.registerDisplayListener()
        bodyActivityBinding.bodySurfaceView.onResume()
    }

    private fun isSuccessResumeSession(): Boolean {
        return try {
            arSession?.resume()
            true
        } catch (e: ARCameraNotAvailableException) {
            Toast.makeText(this, "Camera open failed, please restart the app",
                Toast.LENGTH_LONG).show()
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

    override fun onPause() {
        LogUtil.info(TAG, "onPause start.")
        super.onPause()
        displayRotationController.unregisterDisplayListener()
        bodyActivityBinding.bodySurfaceView.onPause()
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
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
import com.huawei.arengine.demos.databinding.WorldJavaActivityMainBinding
import com.huawei.arengine.demos.world.controller.GestureController
import com.huawei.arengine.demos.worldbody.controller.WorldBodyRenderController
import com.huawei.hiar.ARConfigBase
import com.huawei.hiar.ARSession
import com.huawei.hiar.ARWorldBodyTrackingConfig
import com.huawei.hiar.exceptions.ARCameraNotAvailableException

/**
 * This AR example shows how to use the WorldBody AR scene of HUAWEI AR Engine,
 * including how to identify planes, use the click function, and identify
 * specific images.
 *
 * @author HW
 * @since 2021-04-08
 */
class WorldBodyActivity : BaseActivity() {
    companion object {
        private const val TAG = "WorldBodyActivity"
    }

    private var arSession: ARSession? = null

    private val displayRotationController by lazy { DisplayRotationController() }

    private val gestureController by lazy { GestureController() }

    private val worldbodyRenderController by lazy {
        WorldBodyRenderController(this, displayRotationController, gestureController)
    }

    private lateinit var worldActivityBinding: WorldJavaActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        worldActivityBinding = WorldJavaActivityMainBinding.inflate(layoutInflater)
        setContentView(worldActivityBinding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        initUi()
    }

    /**
     * Initializes the UI and sets gestureController, worldbodyRenderController, and GL parameters.
     */
    private fun initUi() {
        worldActivityBinding.surfaceView.apply {
            setOnTouchListener { v, event ->
                v.performClick()
                gestureController.gestureDetector.onTouchEvent(event)
            }
            preserveEGLContextOnPause = true
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            setRenderer(worldbodyRenderController)
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
            ARWorldBodyTrackingConfig(arSession).apply {
                focusMode = ARConfigBase.FocusMode.AUTO_FOCUS
                semanticMode = ARWorldBodyTrackingConfig.SEMANTIC_PLANE
            }.also {
                arSession?.configure(it)
            }
            worldbodyRenderController.setArSession(arSession)
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
        worldActivityBinding.surfaceView.onResume()
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
        worldActivityBinding.surfaceView.onPause()
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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        hasFocus.let {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }
}
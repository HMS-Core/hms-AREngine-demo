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

package com.huawei.arengine.demos.world

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast

import com.huawei.arengine.demos.R
import com.huawei.arengine.demos.common.LogUtil
import com.huawei.arengine.demos.common.controller.DisplayRotationController
import com.huawei.arengine.demos.common.service.PermissionManageService
import com.huawei.arengine.demos.common.util.isAvailableArEngine
import com.huawei.arengine.demos.common.view.BaseActivity
import com.huawei.arengine.demos.databinding.WorldJavaActivityMainBinding
import com.huawei.arengine.demos.world.controller.GestureController
import com.huawei.arengine.demos.world.controller.WorldRenderController
import com.huawei.hiar.ARConfigBase
import com.huawei.hiar.ARSession
import com.huawei.hiar.ARWorldTrackingConfig
import com.huawei.hiar.exceptions.ARCameraNotAvailableException
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException

/**
 * This AR example shows how to use the world AR scene of HUAWEI AR Engine,
 * including how to identify planes, use the click function, and identify
 * specific images.
 *
 * @author HW
 * @since 2020-10-10
 */
class WorldActivity : BaseActivity() {
    companion object {
        private const val TAG = "WorldActivity"

        private const val DEFAULT_MAX_MAP_SIZE = 800

        private const val BUTTON_REPEAT_CLICK_INTERVAL_TIME = 2000L

        private const val MSG_ENV_LIGHT_BUTTON_CLICK_ENABLE = 1

        private const val MSG_ENV_TEXTURE_BUTTON_CLICK_ENABLE = 2

        private const val MSG_CONFIG_MAP_SIZE_BUTTON_CLICK_ENABLE = 3
    }

    private var arSession: ARSession? = null

    private val gestureController by lazy { GestureController() }

    private val displayRotationController by lazy { DisplayRotationController() }

    private lateinit var mConfig: ARWorldTrackingConfig

    private lateinit var worldRenderController: WorldRenderController

    private lateinit var worldActivityBinding: WorldJavaActivityMainBinding

    private var mIsEnvLightModeOpen = false

    private var mIsEnvTextureModeOpen = false

    private val handler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_ENV_LIGHT_BUTTON_CLICK_ENABLE -> worldActivityBinding.btnEnvLightMode.setEnabled(true)
                MSG_ENV_TEXTURE_BUTTON_CLICK_ENABLE -> worldActivityBinding.btnEnvTextureMode.setEnabled(true)
                MSG_CONFIG_MAP_SIZE_BUTTON_CLICK_ENABLE -> worldActivityBinding.btnConfigSession.setClickable(true)
                else -> LogUtil.info(TAG, "handleMessage default in WorldActivity.")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        worldActivityBinding = WorldJavaActivityMainBinding.inflate(layoutInflater)
        setContentView(worldActivityBinding.root)
        worldRenderController = WorldRenderController(this, displayRotationController,
            gestureController, worldActivityBinding)
        initUi()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        worldActivityBinding.surfaceView.apply {
            setOnTouchListener { v, event ->
                v.performClick()
                gestureController.gestureDetector.onTouchEvent(event)
            }
            preserveEGLContextOnPause = true
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            setRenderer(worldRenderController)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    private fun initUi() {
        worldActivityBinding.btnEnvLightMode.setOnClickListener(View.OnClickListener {
            it.isEnabled = false
            mIsEnvLightModeOpen = !mIsEnvLightModeOpen
            val lightingMode = refreshLightMode(mIsEnvLightModeOpen, ARConfigBase.LIGHT_MODE_ENVIRONMENT_LIGHTING)
            refreshConfig(lightingMode)
            handler.sendEmptyMessageDelayed(MSG_ENV_LIGHT_BUTTON_CLICK_ENABLE, BUTTON_REPEAT_CLICK_INTERVAL_TIME)
        })
        worldActivityBinding.btnEnvTextureMode.setOnClickListener(View.OnClickListener {
            it.isEnabled = false
            mIsEnvTextureModeOpen = !mIsEnvTextureModeOpen
            refreshEnvTextureLayout();
            val lightingMode = refreshLightMode(mIsEnvTextureModeOpen, ARConfigBase.LIGHT_MODE_ENVIRONMENT_TEXTURE)
            refreshConfig(lightingMode)
            handler.sendEmptyMessageDelayed(MSG_ENV_TEXTURE_BUTTON_CLICK_ENABLE, BUTTON_REPEAT_CLICK_INTERVAL_TIME)
        })
        worldActivityBinding.btnConfigSession.setOnClickListener(View.OnClickListener {
            it.isClickable = false
            resetAndConfigArSession()
            handler.sendEmptyMessageDelayed(MSG_CONFIG_MAP_SIZE_BUTTON_CLICK_ENABLE, BUTTON_REPEAT_CLICK_INTERVAL_TIME)
        })
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
            mConfig = ARWorldTrackingConfig(arSession)
            refreshConfig(ARConfigBase.LIGHT_MODE_NONE)
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
        worldRenderController.setArSession(arSession)
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

    private fun refreshConfig(lightingMode: Int) {
        if (mConfig == null || arSession == null) {
            return
        }
        try {
            mConfig.focusMode = ARConfigBase.FocusMode.AUTO_FOCUS
            mConfig.semanticMode = ARWorldTrackingConfig.SEMANTIC_PLANE or ARWorldTrackingConfig.SEMANTIC_TARGET
            mConfig.lightingMode = lightingMode
            arSession?.let {
                it.configure(mConfig)
            }
        } catch (capturedException: ARUnavailableServiceApkTooOldException) {
            setMessageWhenError(capturedException)
        } finally {
            showEffectiveConfigInfo(lightingMode)
        }
        worldRenderController.setArWorldTrackingConfig(mConfig)
        worldRenderController.setArSession(arSession)
        LogUtil.debug(TAG, "set config")
    }

    private fun showEffectiveConfigInfo(lightingMode: Int) {
        if (mConfig == null || arSession == null) {
            LogUtil.warn(TAG, "showEffectiveConfigInfo params invalid.")
            return
        }
        var toastMsg = ""
        when (arSession!!.supportedSemanticMode) {
            ARWorldTrackingConfig.SEMANTIC_NONE -> toastMsg =
                "The running environment does not support the semantic mode."
            ARWorldTrackingConfig.SEMANTIC_PLANE -> toastMsg =
                "The running environment supports only the plane semantic mode."
            ARWorldTrackingConfig.SEMANTIC_TARGET -> toastMsg =
                "The running environment supports only the target semantic mode."
        }
        if (lightingMode and ARConfigBase.LIGHT_MODE_ENVIRONMENT_LIGHTING != 0
            && mConfig.lightingMode and ARConfigBase.LIGHT_MODE_ENVIRONMENT_LIGHTING == 0) {
            toastMsg += "The running environment does not support LIGHT_MODE_ENVIRONMENT_LIGHTING."
        }
        val maxMapSize: Long = getMaxMapSize()
        if (maxMapSize != 0L && maxMapSize != DEFAULT_MAX_MAP_SIZE.toLong()) {
            toastMsg += "Config max map size:$maxMapSize"
        }
        if (!toastMsg.isEmpty()) {
            Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show()
        }
    }

    private fun refreshLightMode(isOpen: Boolean, changeMode: Int): Int {
        val lightMode = getCurrentLightingMode()
        return if (isOpen) {
            lightMode or changeMode
        } else {
            lightMode and changeMode.inv()
        }
    }

    private fun getCurrentLightingMode(): Int {
        var curLightMode = ARConfigBase.LIGHT_MODE_AMBIENT_INTENSITY
        if (mConfig == null) {
            return curLightMode
        }
        try {
            curLightMode = mConfig.lightingMode
        } catch (exception: ARUnavailableServiceApkTooOldException) {
            LogUtil.warn(TAG, "getCurrentLightMode catch ARUnavailableServiceApkTooOldException.")
        }
        return curLightMode
    }

    private fun getInputMaxMapSize(): Long {
        var maxMapSize = 0L
        val editText = findViewById<EditText>(R.id.text_max_size) ?: return maxMapSize
        try {
            maxMapSize = editText.text.toString().toInt().toLong()
        } catch (exception: NumberFormatException) {
            LogUtil.debug(TAG, "getInputMaxMapSize catch:" + exception.javaClass)
        }
        return maxMapSize
    }

    private fun getMaxMapSize(): Long {
        var maxMapSize = 0L
        if (mConfig == null) {
            LogUtil.warn(TAG, "getMaxMapSize mConfig invalid.")
            return maxMapSize
        }
        try {
            maxMapSize = mConfig.maxMapSize
        } catch (exception: ARUnavailableServiceApkTooOldException) {
            LogUtil.warn(TAG, "getMaxMapSize catch:" + exception.javaClass)
        }
        return maxMapSize
    }

    private fun setMaxMapSize() {
        if (arSession == null || mConfig == null) {
            LogUtil.warn(TAG, "setMaxMapSize mArSession or mConfig invalid.")
            return
        }
        val maxMapSize: Long = getInputMaxMapSize()
        if (maxMapSize == 0L) {
            return
        }
        try {
            mConfig.maxMapSize = maxMapSize
        } catch (exception: ARUnavailableServiceApkTooOldException) {
            Toast.makeText(
                this@WorldActivity.applicationContext,
                "The current AR Engine version does not support the setMaxMapSize method.", Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun resetAndConfigArSession() {
        var lightMode = getCurrentLightingMode()
        stopArSession()
        worldRenderController.setArSession(arSession)
        arSession = ARSession(this@WorldActivity.applicationContext)
        mConfig = ARWorldTrackingConfig(arSession)
        setMaxMapSize()
        refreshConfig(lightMode)
        arSession?.resume()
        worldRenderController.setArSession(arSession)
        displayRotationController.onDisplayChanged(0)
    }

    private fun refreshEnvTextureLayout() {
        if (mIsEnvTextureModeOpen) {
            worldActivityBinding.imgEnvTexture.setVisibility(View.VISIBLE)
        } else {
            worldActivityBinding.imgEnvTexture.setVisibility(View.GONE)
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
}
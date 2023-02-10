/*
 * Copyright 2023. Huawei Technologies Co., Ltd. All rights reserved.
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.arengine.demos.augmentedimage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast

import com.huawei.arengine.demos.augmentedimage.controller.AugmentedImageRenderController
import com.huawei.arengine.demos.common.LogUtil
import com.huawei.arengine.demos.common.controller.DisplayRotationController
import com.huawei.arengine.demos.common.service.PermissionManageService
import com.huawei.arengine.demos.common.util.isAvailableArEngine
import com.huawei.arengine.demos.common.view.BaseActivity
import com.huawei.arengine.demos.databinding.AugmentImageActivityMainBinding
import com.huawei.hiar.ARAugmentedImageDatabase
import com.huawei.hiar.ARConfigBase
import com.huawei.hiar.ARImageTrackingConfig
import com.huawei.hiar.ARSession
import com.huawei.hiar.exceptions.ARCameraNotAvailableException

import java.io.IOException
import java.util.Optional

/**
 * This code demonstrates the image augmentation capability of AR Engine, including obtaining the center
 * of an augmented image, and evaluating the width and height of the physical image on the x axis and z
 * axis with the image center as the origin.
 *
 * @author HW
 * @since 2021-03-29
 */
class AugmentedImageActivity : BaseActivity() {
    companion object {
        private const val TAG = "AugmentedImageActivity"

        private const val OPENGLES_VERSION = 2
    }

    private var mArSession: ARSession? = null

    private val displayRotationController by lazy { DisplayRotationController() }

    private val augmentedImageRenderController by lazy {
        AugmentedImageRenderController(this, displayRotationController)
    }

    private lateinit var augmentImageBinding: AugmentImageActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        augmentImageBinding = AugmentImageActivityMainBinding.inflate(layoutInflater)
        setContentView(augmentImageBinding.root)
        initUi()
    }

    private fun initUi() {
        augmentImageBinding.ImageSurfaceview.run {
            preserveEGLContextOnPause = true
            setEGLContextClientVersion(OPENGLES_VERSION)
            // Configure the EGL, including the bit and depth of the color buffer.
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            setRenderer(augmentedImageRenderController)
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
        mArSession?.let {
            resumeView()
            return
        }
        try {
            if (!isAvailableArEngine(this)) {
                finish()
                return
            }
            mArSession = ARSession(this.applicationContext)
            ARImageTrackingConfig(mArSession).apply {
                focusMode = ARConfigBase.FocusMode.AUTO_FOCUS
                if (!setupInitAugmentedImageDatabase(this)) {
                    LogUtil.error(TAG, "Could not setup augmented image database")
                }
            }.also {
                mArSession?.configure(it)
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
        augmentedImageRenderController.run {
            setImageTrackOnly(true)
            setArSession(mArSession)
        }
        displayRotationController.registerDisplayListener()
        augmentImageBinding.ImageSurfaceview.onResume()
    }

    private fun isSuccessResumeSession(): Boolean {
        return try {
            mArSession?.resume()
            true
        } catch (e: ARCameraNotAvailableException) {
            Toast.makeText(this, "Camera open failed, please restart the app", Toast.LENGTH_LONG).show()
            mArSession = null
            false
        }
    }

    /**
     *  Initialize the augmented image database.
     *
     * @param config Configures sessions for image recognition and tracking. In this example, sets the
     * augmented image database.
     * @return Returns whether the database is successfully created.
     */
    private fun setupInitAugmentedImageDatabase(config: ARImageTrackingConfig): Boolean {
        val augmentedImageBitmap = loadAugmentedImageBitmap()
        if (!augmentedImageBitmap.isPresent) {
            return false
        }
        val augmentedImageDatabase = ARAugmentedImageDatabase(mArSession)
        augmentedImageDatabase.addImage("Tech Park", augmentedImageBitmap.get())
        config.augmentedImageDatabase = augmentedImageDatabase
        return true
    }

    private fun loadAugmentedImageBitmap(): Optional<Bitmap> {
        try {
            assets.open("image_default.png").use { `is` -> return Optional.of(BitmapFactory.decodeStream(`is`)) }
        } catch (e: IOException) {
            LogUtil.error(TAG, "IO exception loading augmented image bitmap.")
        }
        return Optional.empty()
    }

    private fun stopArSession() {
        LogUtil.debug(TAG, "stopArSession start.")
        mArSession?.stop()
        mArSession = null
        LogUtil.debug(TAG, "stopArSession end.")
    }

    override fun onPause() {
        LogUtil.debug(TAG, "onPause start.")
        super.onPause()
        displayRotationController.unregisterDisplayListener()
        augmentImageBinding.ImageSurfaceview.onPause()
        mArSession?.pause()
        LogUtil.debug(TAG, "onPause end.")
    }

    override fun onDestroy() {
        LogUtil.debug(TAG, "onDestroy start.")
        super.onDestroy()
        mArSession?.stop()
        mArSession = null
        LogUtil.debug(TAG, "onDestroy end.")
    }
}
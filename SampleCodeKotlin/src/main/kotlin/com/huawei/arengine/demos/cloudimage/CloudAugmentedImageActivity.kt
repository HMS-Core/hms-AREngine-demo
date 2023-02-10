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

package com.huawei.arengine.demos.cloudimage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast

import com.huawei.arengine.demos.MainApplication.Companion.context
import com.huawei.arengine.demos.cloudimage.controller.AugmentedImageRenderController
import com.huawei.arengine.demos.common.util.JsonUtil
import com.huawei.arengine.demos.common.LogUtil
import com.huawei.arengine.demos.common.controller.DisplayRotationController
import com.huawei.arengine.demos.common.service.PermissionManageService
import com.huawei.arengine.demos.common.util.SecurityUtil
import com.huawei.arengine.demos.common.view.BaseActivity
import com.huawei.arengine.demos.databinding.CloudImageActivityMainBinding
import com.huawei.hiar.ARSession
import com.huawei.hiar.ARImageTrackingConfig
import com.huawei.hiar.ARConfigBase
import com.huawei.hiar.ARAugmentedImageDatabase
import com.huawei.hiar.common.CloudServiceState
import com.huawei.hiar.exceptions.ARCameraNotAvailableException
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException
import com.huawei.hiar.listener.CloudServiceEvent
import com.huawei.hiar.listener.CloudServiceListener

import java.io.IOException
import java.util.Optional
import java.util.EventObject

/**
 * This sample code shows the capability of Huawei AR Engine to identify images through the cloud. In addition, the
 * sample code shows the general process of developing the cloud image recognition app, including how to establish a
 * session with the AR Engine, how to jump to the App Center when no AR Engine is detected on the mobile phone, how to
 * establish connection with the cloud, and how to obtain additional information about the cloud image after obtaining
 * the cloud image.
 *
 * @author HW
 * @since 2022-03-14
 */
class CloudAugmentedImageActivity : BaseActivity() {
    companion object {
        private const val TAG = "CloudAugmentedImageActivity"

        private val OPENGLES_VERSION = 2
    }

    private var arSession: ARSession? = null

    private val displayRotationController by lazy {
        DisplayRotationController()
    }

    private val augmentedImageRenderController by lazy {
        AugmentedImageRenderController(this, displayRotationController)
    }

    var isRemindInstall = false

    private lateinit var cloudImageActivityBinding: CloudImageActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cloudImageActivityBinding = CloudImageActivityMainBinding.inflate(layoutInflater)
        setContentView(cloudImageActivityBinding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        init()
    }

    private fun init() {
        cloudImageActivityBinding.cloudImageSurfaceview.run {
            preserveEGLContextOnPause = true
            setEGLContextClientVersion(OPENGLES_VERSION)

            // Set the EGL configuration chooser, including for the number of
            // bits of the color buffer and the number of depth bits.
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
        arSession?.let {
            resumeView()
            return
        }
        try {
            if(!arEngineAbilityCheck()) {
                SecurityUtil.safeFinishActivity(this)
                return
            }

            arSession = ARSession(this.applicationContext)
            ARImageTrackingConfig(arSession).apply {
                focusMode = ARConfigBase.FocusMode.AUTO_FOCUS
                enableItem = ARConfigBase.ENABLE_CLOUD_AUGMENTED_IMAGE.toLong()
                arConfigBase = this
                if (!setupInitAugmentedImageDatabase(this)) {
                    LogUtil.error(TAG, "Could not setup augmented image database")
                }
            }.also {
                arSession?.configure(it)
            }
        } catch (capturedException: Exception) {
            setMessageWhenError(capturedException)
        } finally {
            showCapabilitySupportInfo()
        }
        errorMessage?.let {
            stopArSession()
            return
        }
        resumeView()
    }

    private fun resumeView() {
        if (!isSuccessResumeSession()) return
        augmentedImageRenderController.run {
            setImageTrackOnly(true)
            setArSession(arSession)
        }
        displayRotationController.registerDisplayListener()
        cloudImageActivityBinding.cloudImageSurfaceview.onResume()
    }

    private fun isSuccessResumeSession(): Boolean {
        return try {
            arSession?.resume()
            setCloudServiceStateListener()
            signWithAppId()
            true
        } catch (e: ARCameraNotAvailableException) {
            Toast.makeText(this, "Camera open failed, please restart the app", Toast.LENGTH_LONG).show()
            arSession = null
            false
        } catch (capturedException: ARUnavailableServiceApkTooOldException) {
            setMessageWhenError(capturedException)
            arSession = null
            false
        }
    }

    private fun setCloudServiceStateListener() {
        arSession?.run {
            addServiceListener(CloudImageServiceListener())
        }
    }

    private fun signWithAppId() {
        if (arSession == null) {
            LogUtil.error(TAG, "session is null")
            return
        }
        val mAuthJson = JsonUtil.getJson("cloud_image.json", context)
        arSession!!.setCloudServiceAuthInfo(mAuthJson)
    }

    private fun setupInitAugmentedImageDatabase(config: ARImageTrackingConfig): Boolean {
        val augmentedImageBitmap = loadAugmentedImageBitmap()
        if (!augmentedImageBitmap.isPresent) {
            return false
        }
        val augmentedImageDatabase = ARAugmentedImageDatabase(arSession)
        augmentedImageDatabase.addImage("image_1", augmentedImageBitmap.get())

        config.augmentedImageDatabase = augmentedImageDatabase
        return true
    }

    private fun loadAugmentedImageBitmap(): Optional<Bitmap> {
        Log.d(TAG, "loadAugmentedImageBitmap")
        try {
            assets.open("image_default.png").use { `is` ->
                return Optional.of(BitmapFactory.decodeStream(`is`))
            }
        } catch (e: IOException) {
            Log.e(
                TAG, "IO exception loading augmented image bitmap."
            )
        }
        return Optional.empty()
    }

    private fun stopArSession() {
        Log.d(TAG, "stopArSession start.")
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        if (arSession != null) {
            arSession!!.stop()
            arSession = null
        }
        Log.d(TAG, "stopArSession end.")
    }

    override fun onPause() {
        Log.d(TAG, "onPause start.")
        super.onPause()
        arSession?.apply {
            displayRotationController.unregisterDisplayListener()
            cloudImageActivityBinding.cloudImageSurfaceview.onPause()
            pause()
        }
        Log.d(TAG, "onPause end.")
    }

    override fun onDestroy() {
        LogUtil.debug(TAG, "onDestroy start.")
        super.onDestroy()
        arSession?.stop()
        arSession = null
        LogUtil.debug(TAG, "onDestroy end.")
    }

    private class CloudImageServiceListener() : CloudServiceListener {
        override fun handleEvent(eventObject: EventObject?) {
            var state: CloudServiceState? = null
            if (eventObject is CloudServiceEvent) {
                state = eventObject.cloudServiceState
            }
            if (state == null) {
                return
            }
            LogUtil.debug(TAG, "CloudImageServiceListener state = " + state)
            var tipMsg = ""
            when (state) {
                CloudServiceState.CLOUD_SERVICE_ERROR_NETWORK_UNAVAILABLE ->
                    tipMsg = "network unavailable"
                CloudServiceState.CLOUD_SERVICE_ERROR_CLOUD_SERVICE_UNAVAILABLE ->
                    tipMsg = "cloud service unavailable"
                CloudServiceState.CLOUD_SERVICE_ERROR_NOT_AUTHORIZED ->
                    tipMsg = "cloud service not authorized"
                CloudServiceState.CLOUD_SERVICE_ERROR_SERVER_VERSION_TOO_OLD ->
                    tipMsg = "cloud server version too old"
                CloudServiceState.CLOUD_SERVICE_ERROR_TIME_EXHAUSTED ->
                    tipMsg = "time exhausted"
                CloudServiceState.CLOUD_SERVICE_ERROR_INTERNAL ->
                    tipMsg = "cloud service gallery invalid"
                CloudServiceState.CLOUD_IMAGE_ERROR_IMAGE_GALLERY_INVALID ->
                    tipMsg = "cloud image error, cloud service gallery invalid"
                CloudServiceState.CLOUD_IMAGE_ERROR_IMAGE_RECOGNIZE_FAILE ->
                    tipMsg = "cloud image recognize fail"
                else -> {
                }
            }
            tipMsg.isEmpty().apply {
                return
            }
            Toast.makeText(context, tipMsg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCapabilitySupportInfo() {
        if (arConfigBase == null) {
            LogUtil.warn(TAG, "showCapabilitySupportInfo arConfigBase is null.")
            return
        }
        if (arConfigBase!!.enableItem and ARConfigBase.ENABLE_CLOUD_AUGMENTED_IMAGE.toLong() == 0L) {
            Toast.makeText(this, "The device does not support ENABLE_CLOUD_AUGMENTED_IMAGE.",
                Toast.LENGTH_LONG).show()
        }
    }
}
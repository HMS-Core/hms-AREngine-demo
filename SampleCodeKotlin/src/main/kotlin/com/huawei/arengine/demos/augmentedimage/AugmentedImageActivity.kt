/**
 * Copyright 2021. Huawei Technologies Co., Ltd. All rights reserved.
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

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.huawei.arengine.demos.R
import com.huawei.arengine.demos.augmentedimage.controller.AugmentedImageRenderController
import com.huawei.arengine.demos.common.LogUtil
import com.huawei.arengine.demos.common.controller.DisplayRotationController
import com.huawei.arengine.demos.common.service.PermissionManageService
import com.huawei.arengine.demos.common.util.isAvailableArEngine
import com.huawei.arengine.demos.common.view.ConnectAppMarketActivity
import com.huawei.hiar.*
import com.huawei.hiar.exceptions.ARCameraNotAvailableException
import com.huawei.hiar.exceptions.ARUnSupportedConfigurationException
import com.huawei.hiar.exceptions.ARUnavailableClientSdkTooOldException
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException
import com.huawei.hiar.exceptions.ARUnavailableServiceNotInstalledException
import java.io.IOException
import kotlinx.android.synthetic.main.augment_image_activity_main.ImageSurfaceview
import java.util.*

/**
 * 本代码展示了HUAWEI AR Engine的增强图像能力，包括图形识别
 * 、获取增强图像的中心点、获取以图像中心为坐标原点、在x轴，z
 * 轴上评估出物理图片的宽高信息。
 *
 * @author HW
 * @since 2021-03-29
 */
class AugmentedImageActivity : Activity() {
    companion object {
        private const val TAG = "AugmentedImageActivity"

        private const val OPENGLES_VERSION = 2
    }

    private var mArSession: ARSession? = null

    private val displayRotationController by lazy { DisplayRotationController() }

    private val augmentedImageRenderController by lazy {
        AugmentedImageRenderController(this, displayRotationController)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.augment_image_activity_main)
        initUi()
    }

    private fun initUi() {
        ImageSurfaceview.run {
            preserveEGLContextOnPause = true
            setEGLContextClientVersion(OPENGLES_VERSION)
            // 配置EGL，包括颜色buffer的比特位和深度位数。
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
        mArSession?.let {
            resumeView()
            return
        }
        var errorMessage: String? = null
        try {
            if (!isAvailableArEngine(this)) {
                finish()
                return
            }
            mArSession = ARSession(this)
            ARImageTrackingConfig(mArSession).apply {
                focusMode = ARConfigBase.FocusMode.AUTO_FOCUS
                if(!setupInitAugmentedImageDatabase(this)) {
                    LogUtil.error(TAG, "Could not setup augmented image database")
                }
            }.also {
                mArSession?.configure(it)
            }
            augmentedImageRenderController.run {
                setImageTrackOnly(true)
                setArSession(mArSession)
            }
        } catch (capturedException: ARUnavailableServiceNotInstalledException) {
            startActivity(Intent(this, ConnectAppMarketActivity::class.java))
        } catch (capturedException: ARUnavailableServiceApkTooOldException) {
            errorMessage = "Please update HuaweiARService.apk"
        } catch (capturedException: ARUnavailableClientSdkTooOldException) {
            errorMessage = "Please update this app"
        } catch (capturedException: ARUnSupportedConfigurationException) {
            errorMessage = "The configuration is not supported by the device!"
        } catch (capturedException: Exception) {
            errorMessage = "unknown exception throws!"
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
        ImageSurfaceview.onResume()
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
     * 初始化增强图像功能的数据库。
     *
     * @param config 用于Image图像识别和跟踪时配置session，此处用于设置增强图像数据库。
     * @return 创建数据库是否成功。
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
        mArSession!!.stop()
        mArSession = null
        LogUtil.debug(TAG, "stopArSession end.")
    }

    override fun onPause() {
        LogUtil.debug(TAG, "onPause start.")
        super.onPause()
        displayRotationController.unregisterDisplayListener()
        ImageSurfaceview.onPause()
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

    override fun onWindowFocusChanged(isHasFocus: Boolean) {
        super.onWindowFocusChanged(isHasFocus)
        if (isHasFocus) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }
}
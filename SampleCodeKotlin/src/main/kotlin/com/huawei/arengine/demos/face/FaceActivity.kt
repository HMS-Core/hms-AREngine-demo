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

package com.huawei.arengine.demos.face

import android.hardware.camera2.CameraCharacteristics
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Size
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

import com.huawei.arengine.demos.R
import com.huawei.arengine.demos.common.ListDialog
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
import com.huawei.hiar.exceptions.ARFatalException
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException
import com.huawei.hiar.exceptions.ARUnavailableServiceNotInstalledException
import com.huawei.hiar.exceptions.ARUnavailableClientSdkTooOldException
import com.huawei.hiar.exceptions.ARUnSupportedConfigurationException

import kotlin.math.abs

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

    private val BUTTON_REPEAT_CLICK_INTERVAL_TIME = 1000L

    private val RATIO_4_TO_3 = 4.toFloat() / 3f

    private val EPSINON = 0.000001f

    private lateinit var arConfig: ARFaceTrackingConfig

    private val OPEN_MULTI_FACE_MODE = "OpenMultiFaceMode"

    private val OPEN_SINGLE_FACE_WITH_LIGHT_MODE = "OpenSingleFaceWithLightMode"

    private var cameraLensFacing = CameraCharacteristics.LENS_FACING_FRONT

    private var button: Button? = null

    private var cameraFacingButton: Button? = null

    private var ratioButton: Button? = null

    private var textView: TextView? = null

    private var isCameraInit = false

    private val MSG_OPEN_MULTI_FACE_MODE = 1

    private val MSG_OPEN_SINGLE_FACE_WITH_LIGHT_MODE = 2

    private var faceMode: Int = MSG_OPEN_SINGLE_FACE_WITH_LIGHT_MODE

    private var cameraController : CameraController? = null

    private val previewSizes : ArrayList<Size> = ArrayList<Size>()

    private var keyList : ArrayList<String> = ArrayList<String>()

    private var previewWidth = 0

    private var previewHeight = 0

    private val displayRotationController by lazy { DisplayRotationController() }

    private val faceRenderController by lazy {
        FaceRenderController(this, displayRotationController)
    }

    private lateinit var faceActivityBinding: FaceActivityMainBinding

    private val mHandler: Handler = object: Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_OPEN_MULTI_FACE_MODE -> button!!.setText(OPEN_SINGLE_FACE_WITH_LIGHT_MODE)
                MSG_OPEN_SINGLE_FACE_WITH_LIGHT_MODE -> button!!.setText(OPEN_MULTI_FACE_MODE)
                else -> LogUtil.info(TAG, "handleMessage default in FaceActivity.")
            }
            button!!.isEnabled = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        faceActivityBinding = FaceActivityMainBinding.inflate(layoutInflater)
        setContentView(faceActivityBinding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        initUi()
    }

    private fun initUi() {
        textView = findViewById(R.id.faceTextView)
        button = findViewById(R.id.faceButton)
        cameraFacingButton = findViewById(R.id.cameraFacingButton)
        ratioButton = findViewById(R.id.ratioButton)
        initClickListener()
        initCameraFacingClickListener()
        initRatioClickListener()
        faceActivityBinding.faceSurfaceView.run {
            preserveEGLContextOnPause = true
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            setRenderer(faceRenderController)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
        getPreviewSizeList()
    }

    override fun onResume() {
        LogUtil.debug(TAG, "onResume")
        super.onResume()
        if (!PermissionManageService.hasPermission()) {
            finish()
        }
        errorMessage = null
        arSession?.let {
            if (!isAvailableArEngine(this)) {
                finish()
                return
            }
        }
        setArConfig(faceMode, false)
        displayRotationController.registerDisplayListener()
        faceRenderController.setOpenCameraOutsideFlag(isOpenCameraOutside)
        faceActivityBinding.faceSurfaceView.onResume()
    }

    private fun setArConfig(faceMode: Int, isModifyPreviewSize: Boolean) {
        LogUtil.debug(TAG, "setArConfig")
        try {
            if (arSession == null) {
                arSession = ARSession(this.applicationContext)
            }
            arConfig = ARFaceTrackingConfig(arSession)
            arConfig?.apply {
                powerMode = ARConfigBase.PowerMode.POWER_SAVING
                if (isModifyPreviewSize) {
                    arConfig.setPreviewSize(previewWidth, previewHeight)
                }
                if (faceMode == MSG_OPEN_SINGLE_FACE_WITH_LIGHT_MODE) {
                    setLightingMode(ARConfigBase.LIGHT_MODE_ENVIRONMENT_LIGHTING)
                } else {
                    setFaceDetectMode(ARConfigBase.FaceDetectMode.FACE_ENABLE_MULTIFACE.enumValue
                        or ARConfigBase.FaceDetectMode.FACE_ENABLE_DEFAULT.enumValue)
                }
                if (isOpenCameraOutside) {
                    imageInputMode = ARConfigBase.ImageInputMode.EXTERNAL_INPUT_ALL
                }
                setCameraLensFacing(if (cameraLensFacing == CameraCharacteristics.LENS_FACING_FRONT)
                    ARConfigBase.CameraLensFacing.FRONT else ARConfigBase.CameraLensFacing.REAR)
            }.also { arSession?.configure(it) }
        } catch (capturedException: ARFatalException) {
            setMessageWhenError(capturedException)
        } catch (capturedException: ARUnavailableServiceNotInstalledException) {
            setMessageWhenError(capturedException)
        } catch (capturedException: ARUnavailableServiceApkTooOldException) {
            setMessageWhenError(capturedException)
        } catch (capturedException: ARUnavailableClientSdkTooOldException) {
            setMessageWhenError(capturedException)
        } catch (capturedException: ARUnSupportedConfigurationException) {
            setMessageWhenError(capturedException)
        } finally {
            showCapabilitySupportInfo(faceMode);
        }
        errorMessage?.let {
            Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            stopArSession()
            return
        }
        if (!isSuccessResumeSession()) return
        startCamera()
        faceRenderController.setArSession(arSession)
        faceRenderController.setConfig(arConfig)
        displayRotationController.onDisplayChanged(0)
        isCameraInit = false
    }

    private fun startCamera() {
        if (cameraController == null) {
            cameraController = CameraController(this)
        }
        arSession?.let {
            cameraController?.arSession = it
            cameraController?.arConfig = arConfig
        }
        if (isOpenCameraOutside) {
            cameraController?.run {
                startCameraService(cameraLensFacing)
                faceRenderController.textureId = textureId
            }
        }
    }

    private fun initCameraFacingClickListener() {
        cameraFacingButton!!.setOnClickListener(View.OnClickListener {
            if (!isCameraInit) {
                LogUtil.debug(TAG, "camera preview not finish")
                return@OnClickListener
            }
            arSession?.pause()
            arSession?.stop()
            arSession = null
            stopCamera()
            cameraLensFacing = if (cameraLensFacing == CameraCharacteristics.LENS_FACING_FRONT)
                CameraCharacteristics.LENS_FACING_BACK else CameraCharacteristics.LENS_FACING_FRONT
            setArConfig(faceMode, false)
            getPreviewSizeList()
        })
    }

    private fun initClickListener() {
        button!!.setOnClickListener(View.OnClickListener {
            button!!.isEnabled = false
            arSession?.apply {
                stop()
                arSession = null
            }
            stopCamera()
            if (button!!.text.toString() == OPEN_MULTI_FACE_MODE) {
                faceMode = MSG_OPEN_MULTI_FACE_MODE
                setArConfig(faceMode, false)
                mHandler.sendEmptyMessageDelayed(MSG_OPEN_MULTI_FACE_MODE,
                    BUTTON_REPEAT_CLICK_INTERVAL_TIME)
                return@OnClickListener
            }
            if (button!!.text.toString() == OPEN_SINGLE_FACE_WITH_LIGHT_MODE) {
                faceMode = MSG_OPEN_SINGLE_FACE_WITH_LIGHT_MODE
                setArConfig(faceMode, false)
                mHandler.sendEmptyMessageDelayed(MSG_OPEN_SINGLE_FACE_WITH_LIGHT_MODE,
                    BUTTON_REPEAT_CLICK_INTERVAL_TIME)
            }
        })
    }

    private fun getPreviewSizeList() {
        if (cameraController == null) {
            cameraController = CameraController(this)
        }
        val supportedPreviewSizes: Array<Size?>? = cameraController!!.getPreviewSizeList(cameraLensFacing)
        previewSizes.clear()
        keyList.clear()
        for (option in supportedPreviewSizes!!) {
            if (option == null) {
                continue
            }
            if (abs(option.width / option.height.toFloat() - RATIO_4_TO_3) > EPSINON) {
                continue
            }
            previewSizes.add(option)
            keyList.add(option.width.toString() + "×" + option.height)
        }
    }

    private fun initRatioClickListener() {
        ratioButton!!.setOnClickListener { view: View? ->
            val lisDialog = ListDialog()
            lisDialog.showDialogList(this, keyList)
            val listener = object : ListDialog.DialogOnItemClickListener {
                override fun onItemClick(position: Int) {
                    previewWidth = previewSizes[position].width
                    previewHeight = previewSizes[position].height
                    stopArSession()
                    setArConfig(faceMode, true)
                }
            }
            lisDialog.setDialogOnItemClickListener(listener)
        }
    }

    private fun stopCamera() {
        if (!isOpenCameraOutside) {
            return
        }
        cameraController?.apply {
            LogUtil.info(TAG, "Stop camera start.")
            closeCamera()
            stopCameraThread()
            cameraController = null
            LogUtil.info(TAG, "Stop camera end.")
        }
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
            cameraController?.run {
                closeCamera()
                stopCameraThread()
                cameraController = null
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

    private fun showCapabilitySupportInfo(faceMode: Int) {
        if (arConfig == null) {
            LogUtil.warn(TAG, "showCapabilitySupportInfo arConfigBase is null.")
            return
        }
        var toastStr = ""
        try {
            toastStr = if (faceMode == MSG_OPEN_SINGLE_FACE_WITH_LIGHT_MODE) {
                if (arConfig.getLightingMode() and ARConfigBase.LIGHT_MODE_ENVIRONMENT_LIGHTING == 0)
                    "The device does not support LIGHT_MODE_ENVIRONMENT_LIGHTING." else ""
            } else {
                if ((arConfig.getFaceDetectMode() and
                    ARConfigBase.FaceDetectMode.FACE_ENABLE_MULTIFACE.enumValue) == 0L)
                    "The device does not support FACE_ENABLE_MULTIFACE." else ""
            }
        } catch (capturedException: ARUnavailableServiceApkTooOldException) {
            LogUtil.debug(TAG, "show capability support info has exception:" + capturedException.javaClass)
        }
        if (toastStr.isEmpty()) {
            return
        }
        Toast.makeText(this, toastStr, Toast.LENGTH_LONG).show()
    }

    fun resetCameraStatus() {
        isCameraInit = true
    }
}
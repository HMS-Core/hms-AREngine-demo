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

package com.huawei.arengine.demos.cloudaugmentobject

import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast

import com.huawei.arengine.demos.R.id.btn_change_mode_id
import com.huawei.arengine.demos.cloudaugmentobject.controller.AugmentedObjectRenderController
import com.huawei.arengine.demos.cloudaugmentobject.model.ModeInformation
import com.huawei.arengine.demos.common.ListDialog
import com.huawei.arengine.demos.common.LogUtil
import com.huawei.arengine.demos.common.controller.DisplayRotationController
import com.huawei.arengine.demos.common.service.PermissionManageService
import com.huawei.arengine.demos.common.util.JsonUtil
import com.huawei.arengine.demos.common.util.JsonUtil.defaultAppId
import com.huawei.arengine.demos.common.util.JsonUtil.readApplicationMessage
import com.huawei.arengine.demos.common.util.JsonUtil.writeApplicationMessage
import com.huawei.arengine.demos.common.util.SecurityUtil
import com.huawei.arengine.demos.common.view.BaseActivity
import com.huawei.arengine.demos.databinding.CloudObjectActivityMainBinding
import com.huawei.hiar.ARConfigBase
import com.huawei.hiar.ARSession
import com.huawei.hiar.ARWorldTrackingConfig
import com.huawei.hiar.common.CloudServiceState
import com.huawei.hiar.exceptions.ARCameraNotAvailableException
import com.huawei.hiar.exceptions.ARFatalException
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException
import com.huawei.hiar.exceptions.ARUnavailableServiceNotInstalledException
import com.huawei.hiar.exceptions.ARUnavailableClientSdkTooOldException
import com.huawei.hiar.exceptions.ARUnSupportedConfigurationException
import com.huawei.hiar.listener.CloudServiceEvent
import com.huawei.hiar.listener.CloudServiceListener

import java.util.EventObject

/**
 * Demonstrates how to use AR Engine to recognize cloud 3D objects, including recognizing the 3D objects
 * and obtaining their pose, name, and ID.
 *
 * @author HW
 * @since 2022-04-12
 */
class CloudAugmentObjectActivity : BaseActivity() {
    companion object {
        private const val TAG = "CloudAugmentedObjectActivity"

        private const val OPENGLES_VERSION = 2
    }

    private var modeIdList: List<ModeInformation>? = null

    private var arSession: ARSession? = null

    private lateinit var context: Context

    private val displayRotationController by lazy {
        DisplayRotationController()
    }

    private val augmentedObjectRenderController by lazy {
        AugmentedObjectRenderController(this, displayRotationController)
    }

    private lateinit var cloudObjectActivityBinding: CloudObjectActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = this
        cloudObjectActivityBinding = CloudObjectActivityMainBinding.inflate(layoutInflater)
        setContentView(cloudObjectActivityBinding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        init()
    }

    private fun init() {
        cloudObjectActivityBinding.objectSurfaceview.run {
            preserveEGLContextOnPause = true
            setEGLContextClientVersion(OPENGLES_VERSION)

            // Configure the EGL, including the bit and depth of the color buffer.
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            setRenderer(augmentedObjectRenderController)
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
            if (!arEngineAbilityCheck()) {
                SecurityUtil.safeFinishActivity(this)
                return
            }

            arSession = ARSession(this.applicationContext)
            ARWorldTrackingConfig(arSession).apply {
                focusMode = ARConfigBase.FocusMode.AUTO_FOCUS
                enableItem = ARConfigBase.ENABLE_CLOUD_OBJECT_RECOGNITION.toLong()
                arConfigBase = this
            }.also {
                arSession?.configure(it)
            }
        } catch (exception: ARCameraNotAvailableException) {
            LogUtil.debug(TAG, "Exception: " + exception.javaClass)
            Toast.makeText(this, "Camera open failed, please restart the app", Toast.LENGTH_LONG).show();
            arSession = null;
            return;
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
            showCapabilitySupportInfo();
        }
        errorMessage?.apply {
            stopArSession()
            return
        }
        resumeView()
        LogUtil.debug(TAG, "onResume end")
    }

    private fun resumeView() {
        if (!isSuccessResumeSession()) return
        augmentedObjectRenderController.run {
            setArSession(arSession)
        }
        displayRotationController.registerDisplayListener()
        cloudObjectActivityBinding.objectSurfaceview.onResume()
    }

    private fun isSuccessResumeSession(): Boolean {
        return try {
            arSession?.apply {
                resume()
                addServiceListener(CloudObjectServiceListener())
                setCloudServiceAuthInfo(signWithAppId())
            }
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

    private fun signWithAppId(): String {
        var authJson = readApplicationMessage(context)
        if (authJson.isEmpty()) {
            authJson = defaultAppId(context, "mode_id.json")
        }
        writeApplicationMessage(context, authJson)
        return authJson
    }

    override fun onPause() {
        Log.d(TAG, "onPause start.")
        super.onPause()
        arSession?.apply {
            displayRotationController.unregisterDisplayListener()
            cloudObjectActivityBinding.objectSurfaceview.onPause()
            pause()
        }
        Log.d(TAG, "onPause end.")
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

    override fun onDestroy() {
        LogUtil.debug(TAG, "onDestroy start.")
        super.onDestroy()
        arSession?.stop()
        arSession = null
        LogUtil.debug(TAG, "onDestroy end.")
    }

    private inner class CloudObjectServiceListener : CloudServiceListener {
        override fun handleEvent(eventObject: EventObject?) {
            var state: CloudServiceState? = null
            if (eventObject is CloudServiceEvent) {
                state = eventObject.cloudServiceState
            }
            state ?: return
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
                CloudServiceState.CLOUD_OBJECT_ERROR_OBJECT_MODEL_INVALID ->
                    tipMsg = "cloud object error, object invalid"
                CloudServiceState.CLOUD_OBJECT_ERROR_OBJECT_RECOGNIZE_FAILE ->
                    tipMsg = "network unavailable"
            }
            if (tipMsg.isEmpty()) {
                return
            }
            runOnUiThread {
                kotlin.run {
                    Toast.makeText(context, tipMsg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun onClick(view: View) {
        when (view.id) {
            btn_change_mode_id -> initDialogList()
        }
    }

    private fun initDialogList() {
        var keyList: ArrayList<String> = arrayListOf()
        if (modeIdList == null) {
            val jsonString: String = JsonUtil.getJson("mode_id.json", context)
            modeIdList = JsonUtil.json2List(jsonString)
        }
        if (modeIdList == null) {
            LogUtil.warn(TAG, "modeIdList == null")
        }
        for (mode in modeIdList!!) {
            if (mode == null) {
                continue
            }
            keyList.add(mode.getContinent())
        }
        val lisDialog = ListDialog()
        lisDialog.showDialogList(context, keyList)
        val listener = object : ListDialog.DialogOnItemClickListener {
            override fun onItemClick(position: Int) {
                writeApplicationMessage(context, modeIdList!![position].getModelInformation())
                finish()
            }
        }
        lisDialog.setDialogOnItemClickListener(listener)
    }

    private fun showCapabilitySupportInfo() {
        if (arConfigBase == null) {
            LogUtil.warn(TAG, "showCapabilitySupportInfo arConfigBase is null.")
            return
        }

        if (arConfigBase!!.enableItem and ARConfigBase.ENABLE_CLOUD_OBJECT_RECOGNITION.toLong() == 0L) {
            Toast.makeText(
                this, "The device does not support ENABLE_CLOUD_OBJECT_RECOGNITION.",
                Toast.LENGTH_LONG).show()
        }
    }
}
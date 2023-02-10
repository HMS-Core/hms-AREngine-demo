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

package com.huawei.arengine.demos.common.view

import android.app.Activity
import android.content.Intent
import android.os.BadParcelableException
import android.view.View
import android.view.WindowInsetsController
import android.widget.Toast

import com.huawei.arengine.demos.common.LogUtil
import com.huawei.arengine.demos.common.util.SecurityUtil
import com.huawei.arengine.demos.common.util.startActivityByType
import com.huawei.hiar.ARConfigBase
import com.huawei.hiar.AREnginesApk
import com.huawei.hiar.exceptions.ARUnSupportedConfigurationException
import com.huawei.hiar.exceptions.ARUnavailableClientSdkTooOldException
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException
import com.huawei.hiar.exceptions.ARUnavailableServiceNotInstalledException

import java.util.Objects

/**
 * Custom activity base class, which is used to start the server update dialog box and handle returned events.
 *
 * @author hw
 * @since 2021-08-21
 */
open class BaseActivity : Activity() {
    /**
     * Error information about AR session initialization.
     */
    protected var errorMessage: String? = null

    /**
     * Save config used to configure sessions.
     */
    protected var arConfigBase: ARConfigBase? = null

    private var isRemindInstall = false

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        LogUtil.debug(TAG, "result from ConnectAppMarketActivity start");
        super.onActivityResult(requestCode, resultCode, data)
        if (data == null) {
            LogUtil.error(TAG, "date is null")
            SecurityUtil.safeFinishActivity(this)
            return
        }
        if (requestCode != UPDATE_SERVER_REQUEST_CODE || resultCode != UPDATE_SERVER_RESULT_CODE) {
            LogUtil.error(TAG, "requestCode error or resultCode error")
            return
        }
        try {
            if (data.getIntExtra(RESULT_MESSAGE, RESULT_CODE_CANCEL) == RESULT_CODE_CANCEL) {
                SecurityUtil.safeFinishActivity(this)
            }
        } catch (exception: BadParcelableException) {
            LogUtil.error(TAG, "BadParcelableException")
        }
        LogUtil.debug(TAG, "result from ConnectAppMarketActivity end");
    }

    /**
     * Dialog box for starting the server update.
     */
    protected fun startUpdateActivityForResult() {
        LogUtil.debug(TAG, "open ConnectAppMarketActivity start");
        SecurityUtil.safeStartActivityForResult(this,
            Intent(this, ConnectAppMarketActivity::class.java),
            UPDATE_SERVER_REQUEST_CODE)
        LogUtil.debug(TAG, "open ConnectAppMarketActivity end");
    }

    /**
     * Input the captured exception items and output the corresponding exception information.
     *
     * @param catchException Captured exception.
     */
    protected fun setMessageWhenError(catchException: Exception?) {
        LogUtil.debug(TAG, "setMessage start");
        if (catchException is ARUnavailableServiceNotInstalledException
            || catchException is ARUnavailableServiceApkTooOldException) {
            LogUtil.debug(TAG, "Update AR Engine service.")
            errorMessage = "Update AR Engine service.";
            startUpdateActivityForResult()
            return
        }

        if (catchException is ARUnavailableClientSdkTooOldException) {
            errorMessage = "Please update this SDK"
            return
        }
        if (catchException is ARUnSupportedConfigurationException) {
            if (Objects.equals(catchException.message, ARConfigBase.AR_TYPE_UNSUPPORTED_MESSAGE)) {
                errorMessage =
                    "The configuration is not supported by the device! " + ARConfigBase.AR_TYPE_UNSUPPORTED_MESSAGE;
                return
            }
            errorMessage =
                "The device does not support some sub-capabilities. Reconfigure the sub-capabilities or use " +
                    "the configurations that have taken effect."
            return
        }
        errorMessage = "exception throw:" + catchException?.javaClass;
        LogUtil.debug(TAG, "setMessage end");
    }

    protected fun arEngineAbilityCheck(): Boolean {
        val isInstallArEngineApk = AREnginesApk.isAREngineApkReady(this)
        if (!isInstallArEngineApk && isRemindInstall) {
            Toast.makeText(this, "Please agree to install.", Toast.LENGTH_SHORT).show()
            SecurityUtil.safeFinishActivity(this)
        }
        if (!isInstallArEngineApk) {
            startActivityByType<ConnectAppMarketActivity>()
            isRemindInstall = true
        }
        return AREnginesApk.isAREngineApkReady(this)
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

    companion object {
        /**
         * Request code of the start activity.
         */
        const val UPDATE_SERVER_REQUEST_CODE = 101

        /**
         * Return code of the finish activity.
         */
        const val UPDATE_SERVER_RESULT_CODE = 102

        /**
         * Return value of the finish activity, which indicates cancellation.
         */
        const val RESULT_CODE_CANCEL = 111

        /**
         * Return value of the finish activity, which indicates confirmation.
         */
        const val RESULT_CODE_INSTALL = 112

        /**
         * Name of the return value before the finish activity.
         */
        const val RESULT_MESSAGE = "result"
        private val TAG = BaseActivity::class.java.simpleName
    }
}

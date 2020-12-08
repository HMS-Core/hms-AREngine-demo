/**
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
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
package com.huawei.arengine.demos.common.view

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.huawei.arengine.demos.R
import com.huawei.arengine.demos.R.string.arengine_cancel
import com.huawei.arengine.demos.R.string.arengine_install
import com.huawei.arengine.demos.R.string.arengine_install_app
import com.huawei.hiar.exceptions.ARFatalException

/**
 * This activity is used to redirect the user to AppGallery and install the AR Engine server.
 * This activity is called when the AR Engine is not installed.
 *
 * @author HW
 * @since 2020-10-10
 */
class ConnectAppMarketActivity : Activity() {
    companion object {
        private const val TAG = "ConnectAppMarketActivity"

        private const val ACTION_HUAWEI_DOWNLOAD_QUIK = "com.huawei.appmarket.intent.action.AppDetail"

        private const val HUAWEI_MARTKET_NAME = "com.huawei.appmarket"

        private const val PACKAGE_NAME_KEY = "APP_PACKAGENAME"

        private const val PACKAGENAME_ARSERVICE = "com.huawei.arengine.service"
    }

    private lateinit var dialog: AlertDialog.Builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection_app_market)
        showSuggestiveDialog()
    }

    override fun onResume() {
        dialog.show()
        super.onResume()
    }

    private fun showSuggestiveDialog() {
        dialog = AlertDialog.Builder(this)
        showAppMarket()
    }

    private fun showAppMarket() {
        dialog.run {
            setMessage(arengine_install_app)
            setNegativeButton(arengine_cancel) { _, _ ->
                finish()
            }
            setPositiveButton(arengine_install) { _, _ ->
                try {
                    downLoadArServiceApp()
                    finish()
                } catch (e: ActivityNotFoundException) {
                    throw ARFatalException("Failed to launch ARInstallActivity")
                }
            }
            setOnCancelListener { finish() }
        }
    }

    private fun downLoadArServiceApp() {
        try {
            Intent(ACTION_HUAWEI_DOWNLOAD_QUIK).apply {
                putExtra(PACKAGE_NAME_KEY, PACKAGENAME_ARSERVICE)
                setPackage(HUAWEI_MARTKET_NAME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }.let { startActivity(it) }
        } catch (e: SecurityException) {
            Log.w(TAG, "the target app has no permission of media")
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "the target activity is not found!")
        }
    }
}
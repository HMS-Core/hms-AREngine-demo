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

package com.huawei.arengine.demos.common;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import com.huawei.arengine.demos.R;
import com.huawei.hiar.exceptions.ARFatalException;

/**
 * This activity is used to redirect the user to AppGallery and install the AR Engine server.
 * This activity is called when the AR Engine is not installed.
 *
 * @author HW
 * @since 2020-03-31
 */
public class ConnectAppMarketActivity extends Activity {
    private static final String TAG = ConnectAppMarketActivity.class.getSimpleName();

    private static final String ACTION_HUAWEI_DOWNLOAD_QUIK = "com.huawei.appmarket.intent.action.AppDetail";

    private static final String HUAWEI_MARTKET_NAME = "com.huawei.appmarket";

    private static final String PACKAGE_NAME_KEY = "APP_PACKAGENAME";

    private static final String PACKAGENAME_ARSERVICE = "com.huawei.arengine.service";

    private static final int UPDATE_SERVER_REQUEST_CODE = 101;

    private static final int UPDATE_SERVER_RESULT_CODE = 102;

    private static final int RESULT_CODE_CANCEL = 111;

    private static final int RESULT_CODE_INSTALL = 112;

    private static final String RESULT_MESSAGE = "result";

    private AlertDialog.Builder dialog;

    private Intent mIntent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LogUtil.debug(TAG, "onCreate start start");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection_app_market);
        showSuggestiveDialog();
        LogUtil.debug(TAG, "onCreate start end");
    }

    @Override
    protected void onResume() {
        LogUtil.debug(TAG, "onResume start");
        if (dialog != null) {
            LogUtil.debug(TAG, "show dialog.");
            dialog.show();
        }
        if (mIntent != null) {
            setResult(UPDATE_SERVER_RESULT_CODE, getIntent().putExtra(RESULT_MESSAGE, RESULT_CODE_INSTALL));
            SecurityUtil.safeFinishActivity(ConnectAppMarketActivity.this);
        }
        super.onResume();
        LogUtil.debug(TAG, "onResume end");
    }

    private void showSuggestiveDialog() {
        LogUtil.debug(TAG, "Show education dialog.");
        dialog = new AlertDialog.Builder(this).setCancelable(false);
        showAppMarket();
    }

    private void showAppMarket() {
        dialog.setMessage(R.string.arengine_install_app);
        dialog.setNegativeButton(R.string.arengine_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int index) {
                LogUtil.debug(TAG, "negative button click");
                setResult(UPDATE_SERVER_RESULT_CODE, getIntent().putExtra(RESULT_MESSAGE, RESULT_CODE_CANCEL));
                finish();
            }
        });
        dialog.setPositiveButton(R.string.arengine_install, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int index) {
                try {
                    LogUtil.debug(TAG, "arengine_install onClick.");
                    downLoadArServiceApp();
                } catch (ActivityNotFoundException exception) {
                    throw new ARFatalException("Failed to launch ARInstallActivity");
                }
            }
        });
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                dialog = null;
            }
        });
    }

    private void downLoadArServiceApp() {
        try {
            mIntent = new Intent(ACTION_HUAWEI_DOWNLOAD_QUIK);
            mIntent.putExtra(PACKAGE_NAME_KEY, PACKAGENAME_ARSERVICE);
            mIntent.setPackage(HUAWEI_MARTKET_NAME);
            mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(mIntent);
            LogUtil.debug(TAG, "turn to app market");
        } catch (SecurityException e) {
            LogUtil.warn(TAG, "the target app has no permission of media");
        } catch (ActivityNotFoundException e) {
            LogUtil.warn(TAG, "the target activity is not found: " + e.getMessage());
        }
    }
}
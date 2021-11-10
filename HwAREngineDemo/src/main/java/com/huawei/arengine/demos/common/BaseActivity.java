/**
 * Copyright 2021. Huawei Technologies Co., Ltd. All rights reserved.
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
import android.content.Intent;
import android.os.BadParcelableException;

import com.huawei.hiar.exceptions.ARUnSupportedConfigurationException;
import com.huawei.hiar.exceptions.ARUnavailableClientSdkTooOldException;
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException;
import com.huawei.hiar.exceptions.ARUnavailableServiceNotInstalledException;

/**
 * Custom activity base class, which is used to start the server update dialog box and handle returned events.
 *
 * @author hw
 * @since 2021-08-21
 */
public class BaseActivity extends Activity {
    /**
     * Request code of the start activity.
     */
    public static final int UPDATE_SERVER_REQUEST_CODE = 101;

    /**
     * Return code of the finish activity.
     */
    public static final int UPDATE_SERVER_RESULT_CODE = 102;

    /**
     * Return value of the finish activity, which indicates cancellation.
     */
    public static final int RESULT_CODE_CANCEL = 111;

    /**
     * Return value of the finish activity, which indicates confirmation
     */
    public static final int RESULT_CODE_INSTALL = 112;

    /**
     * Name of the return value before the finish activity.
     */
    public static final String RESULT_MESSAGE = "result";

    private static final String TAG = BaseActivity.class.getSimpleName();

    /**
     * Error information about AR session initialization.
     */
    protected String errorMessage = null;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {
            LogUtil.error(TAG, "date is null");
            SecurityUtil.safeFinishActivity(this);
            return;
        }
        if (requestCode != UPDATE_SERVER_REQUEST_CODE || resultCode != UPDATE_SERVER_RESULT_CODE) {
            LogUtil.error(TAG, "requestCode error or resultCode error");
            return;
        }
        try {
            if (data.getIntExtra(RESULT_MESSAGE, RESULT_CODE_CANCEL) == RESULT_CODE_CANCEL) {
                SecurityUtil.safeFinishActivity(this);
            }
        } catch (BadParcelableException exception) {
            LogUtil.error(TAG, "BadParcelableException");
        }
    }

    /**
     * Dialog box for starting the server update.
     */
    protected void startUpdateActivityForResult() {
        SecurityUtil.safeStartActivityForResult(this,
            new Intent(this, com.huawei.arengine.demos.common.ConnectAppMarketActivity.class),
            UPDATE_SERVER_REQUEST_CODE);
    }


    /**
     * Input the captured exception items and output the corresponding exception information.
     *
     * @param catchException Captured exception.
     */
    protected void setMessageWhenError(Exception catchException) {
        if (catchException instanceof ARUnavailableServiceNotInstalledException) {
            startUpdateActivityForResult();
            return;
        }
        if (catchException instanceof ARUnavailableServiceApkTooOldException) {
            errorMessage = "Please update HuaweiARService.apk";
            return;
        }
        if (catchException instanceof ARUnavailableClientSdkTooOldException) {
            errorMessage = "Please update this SDK";
            return;
        }
        if (catchException instanceof ARUnSupportedConfigurationException) {
            errorMessage = "The configuration is not supported by the device!";
            return;
        }
        errorMessage = "exception throw";
    }
}

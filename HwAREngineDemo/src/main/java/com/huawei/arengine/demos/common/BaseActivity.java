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
import android.content.Intent;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.os.BadParcelableException;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.huawei.arengine.demos.java.utils.CommonUtil;
import com.huawei.hiar.ARAugmentedImageDatabase;
import com.huawei.hiar.ARConfigBase;
import com.huawei.hiar.AREnginesApk;
import com.huawei.hiar.ARImageTrackingConfig;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.ARWorldBodyTrackingConfig;
import com.huawei.hiar.ARWorldTrackingConfig;
import com.huawei.hiar.exceptions.ARCameraNotAvailableException;
import com.huawei.hiar.exceptions.ARFatalException;
import com.huawei.hiar.exceptions.ARSessionPausedException;
import com.huawei.hiar.exceptions.ARUnSupportedConfigurationException;
import com.huawei.hiar.exceptions.ARUnavailableClientSdkTooOldException;
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException;
import com.huawei.hiar.exceptions.ARUnavailableServiceNotInstalledException;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

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

    private static final String DEFAULT_IMAGE = "image_default.png";

    /**
     * Error information about AR session initialization.
     */
    protected String errorMessage = null;

    /**
     * Save config used to configure sessions.
     */
    protected ARConfigBase mArConfigBase;

    /**
     * Device rotation control
     */
    protected DisplayRotationManager mDisplayRotationManager;

    /**
     * An implementation of SurfaceView, which uses a dedicated surface to display OpenGL rendering.
     */
    protected GLSurfaceView mSurfaceView;

    /**
     * Session instance.
     */
    protected ARSession mArSession;

    private boolean isRemindInstall = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDisplayRotationManager = new DisplayRotationManager(this);
    }

    @Override
    protected void onResume() {
        LogUtil.debug(TAG, "onResume start.");
        super.onResume();
        errorMessage = null;
        if (!PermissionManager.isPermissionEnable(this)) {
            SecurityUtil.safeFinishActivity(this);
        }
        if (!arEngineAbilityCheck()) {
            SecurityUtil.safeFinishActivity(this);
            return;
        }
        LogUtil.debug(TAG, "onResume end.");
    }

    /**
     * Start or resume the ARSession of AR Engine.
     *
     * @param render AR feature rendering instance.
     * @param <T> AR feature rendering class that inherits BaseRenderManager.
     */
    protected <T extends BaseRendererManager> void sessionResume(T render) {
        if (mArSession == null || mSurfaceView == null) {
            LogUtil.debug("sessionResume", "mArSession == null");
            return;
        }
        try {
            mArSession.resume();
        } catch (ARCameraNotAvailableException e) {
            Toast.makeText(this, "Camera open failed, please restart the app", Toast.LENGTH_LONG).show();
            mArSession = null;
            return;
        }
        render.setArSession(mArSession);
        mSurfaceView.onResume();
        mDisplayRotationManager.registerDisplayListener();
    }

    @Override
    protected void onPause() {
        LogUtil.info(TAG, "onPause start.");
        super.onPause();
        if (mArSession != null) {
            mDisplayRotationManager.unregisterDisplayListener();
            mSurfaceView.onPause();
            mArSession.pause();
        }
        LogUtil.info(TAG, "onPause end.");
    }

    /**
     * Stop the ARSession and display exception information when an unrecoverable exception occurs.
     */
    protected void stopArSession() {
        LogUtil.info(TAG, "stopArSession start.");
        if (!TextUtils.isEmpty(errorMessage)) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }
        if (mArSession == null) {
            return;
        }
        try {
            mArSession.pause();
            mArSession.stop();
        } catch (ARSessionPausedException | ARFatalException exception) {
            LogUtil.warn(TAG, "stopArSession catch exception:" + exception.getClass());
        }
        mArSession = null;
        LogUtil.info(TAG, "stopArSession end.");
    }

    @Override
    protected void onDestroy() {
        LogUtil.debug(TAG, "onDestroy start.");
        super.onDestroy();
        if (mArSession != null) {
            mArSession.stop();
            mArSession = null;
        }
        LogUtil.debug(TAG, "onDestroy end.");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogUtil.debug(TAG, "result from ConnectAppMarketActivity start");
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
        LogUtil.debug(TAG, "result from ConnectAppMarketActivity end");
    }

    /**
     * Dialog box for starting the server update.
     */
    protected void startUpdateActivityForResult() {
        LogUtil.debug(TAG, "open ConnectAppMarketActivity start");
        SecurityUtil.safeStartActivityForResult(this,
            new Intent(this, com.huawei.arengine.demos.common.ConnectAppMarketActivity.class),
            UPDATE_SERVER_REQUEST_CODE);
        LogUtil.debug(TAG, "open ConnectAppMarketActivity end");
    }

    /**
     * Input the captured exception items and output the corresponding exception information.
     *
     * @param catchException Captured exception.
     */
    protected void setMessageWhenError(Exception catchException) {
        LogUtil.debug(TAG, "setMessage start");
        if (catchException instanceof ARUnavailableServiceNotInstalledException
            || (catchException instanceof ARUnavailableServiceApkTooOldException)) {
            LogUtil.debug(TAG, "Update AR Engine service.");
            errorMessage = "Update AR Engine service.";
            startUpdateActivityForResult();
            return;
        }

        if (catchException instanceof ARUnavailableClientSdkTooOldException) {
            errorMessage = "Please update this SDK";
            return;
        }
        if (catchException instanceof ARUnSupportedConfigurationException) {
            if (Objects.equals(catchException.getMessage(), ARConfigBase.AR_TYPE_UNSUPPORTED_MESSAGE)) {
                errorMessage =
                    "The configuration is not supported by the device! " + ARConfigBase.AR_TYPE_UNSUPPORTED_MESSAGE;
                return;
            }
            errorMessage = "The device does not support some sub-capabilities. Reconfigure the sub-capabilities or use "
                + "the configurations that have taken effect.";
            return;
        }
        errorMessage = "exception throw:" + catchException.getClass();
        LogUtil.debug(TAG, "setMessage end");
    }

    /**
     * Check whether HUAWEI AR Engine server (com.huawei.arengine.service) is installed on the current device.
     * If not, redirect the user to HUAWEI AppGallery for installation.
     *
     * @return true:AR Engine ready.
     */
    protected boolean arEngineAbilityCheck() {
        boolean isInstallArEngineApk = AREnginesApk.isAREngineApkReady(this);
        if (!isInstallArEngineApk && isRemindInstall) {
            Toast.makeText(this, "Please agree to install.", Toast.LENGTH_LONG).show();
            SecurityUtil.safeFinishActivity(this);
        }
        LogUtil.debug(TAG, "Is Install AR Engine Apk: " + isInstallArEngineApk);
        if (!isInstallArEngineApk) {
            startActivity(new Intent(this, com.huawei.arengine.demos.common.ConnectAppMarketActivity.class));
            isRemindInstall = true;
        }
        return AREnginesApk.isAREngineApkReady(this);
    }

    /**
     * For HUAWEI AR Engine 3.18 or later versions, you can call configure of ARSession, then call getEnableItem to
     * check whether the current device supports health check.
     */
    protected void showCapabilitySupportInfo() {
        if (mArConfigBase == null) {
            LogUtil.warn(TAG, "showCapabilitySupportInfo arConfigBase is null.");
            return;
        }
        String runningDetectionInfo = (mArConfigBase.getEnableItem() & ARConfigBase.ENABLE_DEPTH) != 0 ? "3D" : "2D";

        Toast
            .makeText(this, String.format(Locale.ROOT, "%s detection mode is enabled.", runningDetectionInfo),
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onWindowFocusChanged(boolean isHasFocus) {
        super.onWindowFocusChanged(isHasFocus);
        if (isHasFocus) {
            getWindow().getDecorView()
                .setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    /**
     * Initialize the augmented image database.
     *
     * @param config Configures sessions for image recognition and tracking.
     *        In this example, sets the augmented image database.
     * @param name Name of the ARAugmentedImageBitmap
     * @return Returns whether the database is successfully created.
     */
    protected boolean setupInitAugmentedImageDatabase(ARConfigBase config, String name) {
        Optional<Bitmap> augmentedImageBitmap = CommonUtil.loadAugmentedImageBitmap(this, DEFAULT_IMAGE);
        if (!augmentedImageBitmap.isPresent()) {
            return false;
        }

        ARAugmentedImageDatabase augmentedImageDatabase = new ARAugmentedImageDatabase(mArSession);
        augmentedImageDatabase.addImage(name, augmentedImageBitmap.get());
        if (config instanceof ARImageTrackingConfig) {
            ((ARImageTrackingConfig) config).setAugmentedImageDatabase(augmentedImageDatabase);
        } else if (config instanceof ARWorldTrackingConfig) {
            ((ARWorldTrackingConfig) config).setAugmentedImageDatabase(augmentedImageDatabase);
        } else if (config instanceof ARWorldBodyTrackingConfig) {
            ((ARWorldBodyTrackingConfig) config).setAugmentedImageDatabase(augmentedImageDatabase);
        } else {
            return false;
        }
        return true;
    }
}

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

package com.huawei.arengine.demos.java.health;

import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;

import com.huawei.arengine.demos.R;
import com.huawei.arengine.demos.common.BaseActivity;
import com.huawei.arengine.demos.common.DisplayRotationManager;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.arengine.demos.common.PermissionManager;
import com.huawei.arengine.demos.java.health.rendering.HealthRenderManager;
import com.huawei.hiar.ARConfigBase;
import com.huawei.hiar.AREnginesApk;
import com.huawei.hiar.ARFaceTrackingConfig;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.common.FaceHealthCheckState;
import com.huawei.hiar.exceptions.ARCameraNotAvailableException;
import com.huawei.hiar.listener.FaceHealthCheckStateEvent;
import com.huawei.hiar.listener.FaceHealthServiceListener;

import java.util.EventObject;

/**
 * Shows the usage of APIs related to health data monitoring.
 *
 * @author HW
 * @since 2020-08-03
 */
public class HealthActivity extends BaseActivity {
    private static final String TAG = HealthActivity.class.getSimpleName();

    private static final int MAX_PROGRESS = 100;

    private GLSurfaceView mGlSurfaceView;

    private ARSession mArSession;

    private ARFaceTrackingConfig mArFaceTrackingConfig;

    private boolean isRemindInstall = false;

    private HealthRenderManager mHealthRenderManager;

    private DisplayRotationManager mDisplayRotationManager;

    private ProgressBar mHealthProgressBar;

    private TextView mProgressTips;

    private TextView mHealthCheckStatusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        setContentView(R.layout.health_activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mHealthProgressBar = findViewById(R.id.health_progress_bar);
        mGlSurfaceView = findViewById(R.id.healthSurfaceView);
        mProgressTips = findViewById(R.id.process_tips);
        mHealthCheckStatusTextView = findViewById(R.id.health_check_status);
        mDisplayRotationManager = new DisplayRotationManager(this);

        mGlSurfaceView.setPreserveEGLContextOnPause(true);

        // Set the OpenGLES version.
        mGlSurfaceView.setEGLContextClientVersion(2);

        // Set the EGL configuration chooser, including for the
        // number of bits of the color buffer and the number of depth bits.
        mGlSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mHealthRenderManager = new HealthRenderManager(this, this);
        mHealthRenderManager.setDisplayRotationManage(mDisplayRotationManager);
        TableLayout mHealthParamTable = findViewById(R.id.health_param_table);
        mHealthRenderManager.setHealthParamTable(mHealthParamTable);
        mGlSurfaceView.setRenderer(mHealthRenderManager);
        mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    protected void onResume() {
        LogUtil.debug(TAG, "onResume");
        super.onResume();
        if (!PermissionManager.hasPermission(this)) {
            this.finish();
        }
        errorMessage = null;
        if (mArSession == null) {
            try {
                if (!arEngineAbilityCheck()) {
                    finish();
                    return;
                }
                mArSession = new ARSession(this.getApplicationContext());
                mArFaceTrackingConfig = new ARFaceTrackingConfig(mArSession);
                mArFaceTrackingConfig.setEnableItem(ARConfigBase.ENABLE_HEALTH_DEVICE);
                mArFaceTrackingConfig
                    .setFaceDetectMode(ARConfigBase.FaceDetectMode.HEALTH_ENABLE_DEFAULT.getEnumValue());
                mArSession.configure(mArFaceTrackingConfig);
                setHealthServiceListener();
            } catch (Exception capturedException) {
                setMessageWhenError(capturedException);
            }
            if (errorMessage != null) {
                stopArSession();
                return;
            }
        }
        try {
            mArSession.resume();
        } catch (ARCameraNotAvailableException e) {
            Toast.makeText(this, "Camera open failed, please restart the app", Toast.LENGTH_LONG).show();
            mArSession = null;
            return;
        }
        mDisplayRotationManager.registerDisplayListener();
        mHealthRenderManager.setArSession(mArSession);
        mGlSurfaceView.onResume();
    }

    private void stopArSession() {
        LogUtil.info(TAG, "Stop session start.");
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        if (mArSession != null) {
            mArSession.stop();
            mArSession = null;
        }
        LogUtil.info(TAG, "Stop session end.");
    }

    /**
     * Check whether HUAWEI AR Engine server (com.huawei.arengine.service) is installed on the current device.
     * If not, redirect the user to HUAWEI AppGallery for installation.
     *
     * @return true:AR Engine ready
     */
    private boolean arEngineAbilityCheck() {
        boolean isInstallArEngineApk = AREnginesApk.isAREngineApkReady(this);
        if (!isInstallArEngineApk && isRemindInstall) {
            Toast.makeText(this, "Please agree to install.", Toast.LENGTH_LONG).show();
            finish();
        }
        LogUtil.debug(TAG, "Is Install AR Engine Apk: " + isInstallArEngineApk);
        if (!isInstallArEngineApk) {
            startActivity(new Intent(this, com.huawei.arengine.demos.common.ConnectAppMarketActivity.class));
            isRemindInstall = true;
        }
        return AREnginesApk.isAREngineApkReady(this);
    }

    @Override
    protected void onPause() {
        LogUtil.info(TAG, "onPause start.");
        super.onPause();
        if (mArSession != null) {
            mDisplayRotationManager.unregisterDisplayListener();
            mGlSurfaceView.onPause();
            mArSession.pause();
            LogUtil.info(TAG, "Session paused!");
        }
        LogUtil.info(TAG, "onPause end.");
    }

    @Override
    protected void onDestroy() {
        LogUtil.info(TAG, "onDestroy start.");
        super.onDestroy();
        if (mArSession != null) {
            mArSession.stop();
            mArSession = null;
        }
        LogUtil.info(TAG, "onDestroy end.");
    }

    @Override
    public void onWindowFocusChanged(boolean isHasFocus) {
        LogUtil.debug(TAG, "onWindowFocusChanged");
        super.onWindowFocusChanged(isHasFocus);
        if (isHasFocus) {
            getWindow().getDecorView()
                .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private void setHealthServiceListener() {
        mArSession.addServiceListener(new FaceHealthServiceListener() {
            @Override
            public void handleEvent(EventObject eventObject) {
                if (!(eventObject instanceof FaceHealthCheckStateEvent)) {
                    return;
                }
                final FaceHealthCheckState faceHealthCheckState =
                    ((FaceHealthCheckStateEvent) eventObject).getFaceHealthCheckState();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mHealthCheckStatusTextView.setText(faceHealthCheckState.toString());
                    }
                });
            }

            @Override
            public void handleProcessProgressEvent(final int progress) {
                mHealthRenderManager.setHealthCheckProgress(progress);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setProgressTips(progress);
                    }
                });
            }
        });
    }

    private void setProgressTips(int progress) {
        String progressTips = "processing";
        if (progress >= MAX_PROGRESS) {
            progressTips = "finish";
        }
        mProgressTips.setText(progressTips);
        mHealthProgressBar.setProgress(progress);
    }
}

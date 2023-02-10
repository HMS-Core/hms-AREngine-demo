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

package com.huawei.arengine.demos.java.health;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;

import com.huawei.arengine.demos.R;
import com.huawei.arengine.demos.common.BaseActivity;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.arengine.demos.java.health.rendering.HealthRendererManager;
import com.huawei.hiar.ARConfigBase;
import com.huawei.hiar.ARFaceTrackingConfig;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.common.FaceHealthCheckState;
import com.huawei.hiar.exceptions.ARFatalException;
import com.huawei.hiar.exceptions.ARUnSupportedConfigurationException;
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException;
import com.huawei.hiar.exceptions.ARUnavailableServiceNotInstalledException;
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

    private HealthRendererManager mHealthRendererManager;

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
        mSurfaceView = findViewById(R.id.healthSurfaceView);
        mProgressTips = findViewById(R.id.process_tips);
        mHealthCheckStatusTextView = findViewById(R.id.health_check_status);

        mSurfaceView.setPreserveEGLContextOnPause(true);

        // Set the OpenGLES version.
        mSurfaceView.setEGLContextClientVersion(2);

        // Set the EGL configuration chooser, including for the
        // number of bits of the color buffer and the number of depth bits.
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mHealthRendererManager = new HealthRendererManager(this);
        mHealthRendererManager.setDisplayRotationManager(mDisplayRotationManager);
        TableLayout mHealthParamTable = findViewById(R.id.health_param_table);
        mHealthRendererManager.setHealthParamTable(mHealthParamTable);
        mSurfaceView.setRenderer(mHealthRendererManager);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    protected void onResume() {
        LogUtil.debug(TAG, "onResume");
        super.onResume();
        if (mArSession == null) {
            try {
                mArSession = new ARSession(this.getApplicationContext());
                ARFaceTrackingConfig config = new ARFaceTrackingConfig(mArSession);
                config.setEnableItem(ARConfigBase.ENABLE_HEALTH_DEVICE);
                config.setFaceDetectMode(ARConfigBase.FaceDetectMode.HEALTH_ENABLE_DEFAULT.getEnumValue());
                mArConfigBase = config;
                mArSession.configure(mArConfigBase);
                setHealthServiceListener();
            } catch (ARFatalException | ARUnSupportedConfigurationException | ARUnavailableServiceNotInstalledException
                | ARUnavailableServiceApkTooOldException capturedException) {
                setMessageWhenError(capturedException);
            } finally {
                showCapabilitySupportInfo();
            }
            if (errorMessage != null) {
                stopArSession();
                return;
            }
        }
        sessionResume(mHealthRendererManager);
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
                mHealthRendererManager.setHealthCheckProgress(progress);
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

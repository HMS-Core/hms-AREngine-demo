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

package com.huawei.arengine.demos.java.hand;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.huawei.arengine.demos.R;
import com.huawei.arengine.demos.common.BaseActivity;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.arengine.demos.java.hand.rendering.HandRendererManager;
import com.huawei.hiar.ARConfigBase;
import com.huawei.hiar.ARHandTrackingConfig;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.exceptions.ARFatalException;
import com.huawei.hiar.exceptions.ARUnSupportedConfigurationException;
import com.huawei.hiar.exceptions.ARUnavailableClientSdkTooOldException;
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException;
import com.huawei.hiar.exceptions.ARUnavailableServiceNotInstalledException;

/**
 * Identify hand information and output the identified gesture type, and coordinates of
 * the left hand, right hand, and palm bounding box. When there are multiple hands in an
 * image, only the recognition results and coordinates from the clearest captured hand,
 * with the highest degree of confidence, are sent back. This feature supports front and
 * rear cameras.
 *
 * @author HW
 * @since 2020-03-09
 */
public class HandActivity extends BaseActivity {
    private static final String TAG = HandActivity.class.getSimpleName();

    private HandRendererManager mHandRendererManager;

    private Button mCameraFacingButton;

    private ARConfigBase.CameraLensFacing mCameraLensFacing = ARConfigBase.CameraLensFacing.FRONT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hand_activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mSurfaceView = findViewById(R.id.handSurfaceview);

        mCameraFacingButton = findViewById(R.id.cameraFacingButton);
        initCameraFacingClickListener();
        // Keep the OpenGL ES running context.
        mSurfaceView.setPreserveEGLContextOnPause(true);

        // Set the OpenGLES version.
        mSurfaceView.setEGLContextClientVersion(2);

        // Set the EGL configuration chooser, including for the
        // number of bits of the color buffer and the number of depth bits.
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        mHandRendererManager = new HandRendererManager(this);
        mHandRendererManager.setDisplayRotationManager(mDisplayRotationManager);
        TextView textView = findViewById(R.id.handTextView);
        mHandRendererManager.setTextView(textView);

        mSurfaceView.setRenderer(mHandRendererManager);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    protected void onResume() {
        LogUtil.debug(TAG, "onResume");
        super.onResume();
        if (mArSession == null) {
            setArConfig();
        }
        sessionResume(mHandRendererManager);
    }

    private void initCameraFacingClickListener() {
        mCameraFacingButton.setOnClickListener(view -> {
            mCameraFacingButton.setEnabled(false);
            mCameraLensFacing = mCameraLensFacing == ARConfigBase.CameraLensFacing.FRONT
                ? ARConfigBase.CameraLensFacing.REAR
                : ARConfigBase.CameraLensFacing.FRONT;
            stopArSession();
            mSurfaceView.onPause();
            setArConfig();
            sessionResume(mHandRendererManager);
            mCameraFacingButton.setEnabled(true);
        });
    }
    private void setArConfig() {
        try {
            mArSession = new ARSession(this.getApplicationContext());
            ARHandTrackingConfig config = new ARHandTrackingConfig(mArSession);
            config.setCameraLensFacing(mCameraLensFacing);
            config.setPowerMode(ARConfigBase.PowerMode.ULTRA_POWER_SAVING);
            mArConfigBase = config;
            mArConfigBase.setEnableItem(ARConfigBase.ENABLE_DEPTH);
            mArSession.configure(mArConfigBase);
        } catch (ARFatalException | ARUnavailableServiceNotInstalledException
            | ARUnavailableServiceApkTooOldException | ARUnavailableClientSdkTooOldException
            | ARUnSupportedConfigurationException capturedException) {
            setMessageWhenError(capturedException);
        } finally {
            showCapabilitySupportInfo();
        }
        if (errorMessage != null) {
            stopArSession();
            return;
        }
    }
}
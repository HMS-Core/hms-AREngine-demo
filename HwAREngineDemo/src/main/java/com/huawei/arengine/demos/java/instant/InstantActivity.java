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

package com.huawei.arengine.demos.java.instant;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.huawei.arengine.demos.R;
import com.huawei.arengine.demos.common.BaseActivity;
import com.huawei.arengine.demos.common.GestureDetectorUtils;
import com.huawei.arengine.demos.common.GestureEvent;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.arengine.demos.java.instant.rendering.InstantRendererManager;
import com.huawei.hiar.ARConfigBase;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.ARInstantTrackingConfig;
import com.huawei.hiar.exceptions.ARFatalException;
import com.huawei.hiar.exceptions.ARUnSupportedConfigurationException;
import com.huawei.hiar.exceptions.ARUnavailableClientSdkTooOldException;
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException;
import com.huawei.hiar.exceptions.ARUnavailableServiceNotInstalledException;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * This AR example shows how to use the Instant AR scene of HUAWEI AR Engine,
 * including how to use the click function, drag function, rotation function, and scale function.
 *
 * @author HW
 * @since 2022-09-15
 */
public class InstantActivity extends BaseActivity {
    private static final String TAG = "InstantActivity";

    private static final int MOTIONEVENT_QUEUE_CAPACITY = 2;

    private static final int OPENGLES_VERSION = 2;

    private InstantRendererManager mInstantRendererManager;

    private ArrayBlockingQueue<GestureEvent> mQueuedSingleTaps = new ArrayBlockingQueue<>(MOTIONEVENT_QUEUE_CAPACITY);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LogUtil.info(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.instant_activity_main);
        mSurfaceView = findViewById(R.id.surfaceview);
        GestureDetectorUtils.initGestureDetector(this, TAG, mSurfaceView, mQueuedSingleTaps);

        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(OPENGLES_VERSION);

        // Set the EGL configuration chooser, including for the number of
        // bits of the color buffer and the number of depth bits.
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        mInstantRendererManager = new InstantRendererManager(this);
        mInstantRendererManager.setDisplayRotationManager(mDisplayRotationManager);
        TextView textView = findViewById(R.id.instantTextView);
        mInstantRendererManager.setTextView(textView);
        mInstantRendererManager.setQueuedSingleTaps(mQueuedSingleTaps);

        mSurfaceView.setRenderer(mInstantRendererManager);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    protected void onResume() {
        LogUtil.debug(TAG, "onResume");
        super.onResume();
        if (mArSession == null) {
            try {
                mArSession = new ARSession(this.getApplicationContext());
                mArConfigBase = new ARInstantTrackingConfig(mArSession);
                mArConfigBase.setFocusMode(ARConfigBase.FocusMode.AUTO_FOCUS);
                mArSession.configure(mArConfigBase);
            } catch (ARUnSupportedConfigurationException arUnSupportedConfigurationException) {
                startUpdateActivityForResult();
                return;
            } catch (ARUnavailableServiceNotInstalledException | ARUnavailableServiceApkTooOldException
                | ARUnavailableClientSdkTooOldException | ARFatalException capturedException) {
                setMessageWhenError(capturedException);
            }

            if (errorMessage != null) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                stopArSession();
                return;
            }
        }

        sessionResume(mInstantRendererManager);
    }

    @Override
    protected void onPause() {
        LogUtil.info(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        LogUtil.debug(TAG, "onDestroy");
        super.onDestroy();
    }
}
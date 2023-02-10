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

package com.huawei.arengine.demos.java.worldbody;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

import com.huawei.arengine.demos.R;
import com.huawei.arengine.demos.common.BaseActivity;
import com.huawei.arengine.demos.common.GestureDetectorUtils;
import com.huawei.arengine.demos.common.GestureEvent;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.arengine.demos.java.worldbody.rendering.WorldBodyRendererManager;
import com.huawei.hiar.ARConfigBase;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.ARWorldBodyTrackingConfig;
import com.huawei.hiar.exceptions.ARFatalException;
import com.huawei.hiar.exceptions.ARUnSupportedConfigurationException;
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException;
import com.huawei.hiar.exceptions.ARUnavailableServiceNotInstalledException;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * This demo shows how to use the world body AR type.
 *
 * @author HW
 * @since 2021-03-22
 */
public class WorldBodyActivity extends BaseActivity {
    private static final String TAG = WorldBodyActivity.class.getSimpleName();

    private static final int MOTIONEVENT_QUEUE_CAPACITY = 2;

    private static final int OPENGLES_VERSION = 2;

    private WorldBodyRendererManager mWorldBodyRendererManager;

    private ArrayBlockingQueue<GestureEvent> mQueuedSingleTaps = new ArrayBlockingQueue<>(MOTIONEVENT_QUEUE_CAPACITY);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.world_body_java_activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mSurfaceView = findViewById(R.id.world_body_surfaceview);
        GestureDetectorUtils.initGestureDetector(this, TAG, mSurfaceView, mQueuedSingleTaps);

        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(OPENGLES_VERSION);

        // Set the EGL configuration chooser, including for the number of
        // bits of the color buffer and the number of depth bits.
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        mWorldBodyRendererManager = new WorldBodyRendererManager(this);
        mWorldBodyRendererManager.setDisplayRotationManager(mDisplayRotationManager);
        TextView textView = findViewById(R.id.wordBodyTextView);
        mWorldBodyRendererManager.setTextView(textView);
        mWorldBodyRendererManager.setQueuedSingleTaps(mQueuedSingleTaps);

        mSurfaceView.setRenderer(mWorldBodyRendererManager);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    protected void onResume() {
        LogUtil.debug(TAG, "onResume");
        super.onResume();
        if (mArSession == null) {
            try {
                mArSession = new ARSession(this.getApplicationContext());
                mArConfigBase = new ARWorldBodyTrackingConfig(mArSession);
                mArConfigBase.setFocusMode(ARConfigBase.FocusMode.AUTO_FOCUS);
                mArConfigBase.setEnableItem(ARConfigBase.ENABLE_DEPTH | ARConfigBase.ENABLE_MASK);
                mArSession.configure(mArConfigBase);
            } catch (ARUnSupportedConfigurationException | ARUnavailableServiceApkTooOldException
                | ARUnavailableServiceNotInstalledException | ARFatalException capturedException) {
                setMessageWhenError(capturedException);
            } finally {
                showCapabilitySupportInfo();
            }
            if (errorMessage != null) {
                stopArSession();
                return;
            }
        }
        sessionResume(mWorldBodyRendererManager);
    }
}
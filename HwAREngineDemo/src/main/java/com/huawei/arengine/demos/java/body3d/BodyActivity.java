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

package com.huawei.arengine.demos.java.body3d;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.huawei.arengine.demos.R;
import com.huawei.arengine.demos.common.BaseActivity;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.arengine.demos.java.body3d.rendering.BodyRendererManager;
import com.huawei.hiar.ARBodyTrackingConfig;
import com.huawei.hiar.ARConfigBase;
import com.huawei.hiar.ARSession;

/**
 * The sample code demonstrates the capability of HUAWEI AR Engine to identify
 * body skeleton points and output human body features such as limb endpoints,
 * body posture, and skeleton.
 *
 * @author HW
 * @since 2020-04-01
 */
public class BodyActivity extends BaseActivity {
    private static final String TAG = BodyActivity.class.getSimpleName();

    private static final String OPEN_BODYMASK = "EnableBodyMask";

    private static final String CLOSE_BODYMASK = "DisableBodyMask";

    private BodyRendererManager mBodyRendererManager;

    private Button mBodyMask;

    private boolean mIsBodyMaskEnable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LogUtil.debug(TAG, "onCreate start");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.body3d_activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mSurfaceView = findViewById(R.id.bodySurfaceview);
        mBodyMask = findViewById(R.id.bodymask);

        // Keep the OpenGL ES running context.
        mSurfaceView.setPreserveEGLContextOnPause(true);

        // Set the OpenGLES version.
        mSurfaceView.setEGLContextClientVersion(2);

        // Set the EGL configuration chooser, including for the
        // number of bits of the color buffer and the number of depth bits.
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        mBodyRendererManager = new BodyRendererManager(this);
        mBodyRendererManager.setDisplayRotationManager(mDisplayRotationManager);
        TextView textView = findViewById(R.id.bodyTextView);
        mBodyRendererManager.setTextView(textView);

        mSurfaceView.setRenderer(mBodyRendererManager);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        uiInit();
        LogUtil.debug(TAG, "onCreate end");
    }

    @Override
    protected void onResume() {
        LogUtil.debug(TAG, "onResume start");
        super.onResume();
        if (mArSession == null) {
            try {
                mArSession = new ARSession(this.getApplicationContext());
                mArConfigBase = new ARBodyTrackingConfig(mArSession);
                mArConfigBase.setEnableItem(ARConfigBase.ENABLE_DEPTH | ARConfigBase.ENABLE_MASK);
                mArConfigBase.setFocusMode(ARConfigBase.FocusMode.AUTO_FOCUS);
                mArSession.configure(mArConfigBase);
            } catch (Exception capturedException) {
                setMessageWhenError(capturedException);
            } finally {
                showCapabilitySupportInfo();
            }
            if (errorMessage != null) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                LogUtil.error(TAG, "Creating session");
                if (mArSession != null) {
                    mArSession.stop();
                    mArSession = null;
                }
                return;
            }
        }
        mBodyRendererManager
            .setBodyMask(((mArConfigBase.getEnableItem() & ARConfigBase.ENABLE_MASK) != 0) && mIsBodyMaskEnable);
        sessionResume(mBodyRendererManager);
    }

    private void uiInit() {
        mBodyMask.setOnClickListener(v -> {
            if (OPEN_BODYMASK.equals(mBodyMask.getText().toString())) {
                mIsBodyMaskEnable = true;
                mBodyMask.setText(CLOSE_BODYMASK);
            } else {
                mIsBodyMaskEnable = false;
                mBodyMask.setText(OPEN_BODYMASK);
            }
            if (mArConfigBase == null) {
                LogUtil.error(TAG, "mArConfigBase is null");
                return;
            }
            mBodyRendererManager
                .setBodyMask(((mArConfigBase.getEnableItem() & ARConfigBase.ENABLE_MASK) != 0) && mIsBodyMaskEnable);
        });
    }
}
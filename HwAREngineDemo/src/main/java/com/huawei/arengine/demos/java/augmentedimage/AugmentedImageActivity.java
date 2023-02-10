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

package com.huawei.arengine.demos.java.augmentedimage;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.WindowManager;

import com.huawei.arengine.demos.R;
import com.huawei.arengine.demos.common.BaseActivity;
import com.huawei.arengine.demos.common.DisplayRotationManager;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.arengine.demos.java.augmentedimage.rendering.AugmentedImageRendererManager;
import com.huawei.hiar.ARConfigBase;
import com.huawei.hiar.ARImageTrackingConfig;
import com.huawei.hiar.ARSession;

/**
 * This code demonstrates the augmented image capability of AR Engine, including recognizing the image,
 * obtaining the center of the augmented image, and evaluating the width and height of the physical
 * image on the x axis and z axis with the image center as the origin.
 *
 * @author HW
 * @since 2021-02-04
 */
public class AugmentedImageActivity extends BaseActivity {
    private static final String TAG = AugmentedImageActivity.class.getSimpleName();

    private static final int OPENGLES_VERSION = 2;

    private AugmentedImageRendererManager mArFeatureRendererManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.augment_image_activity_main);
        init();
    }

    private void init() {
        mSurfaceView = findViewById(R.id.ImageSurfaceview);
        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(OPENGLES_VERSION);

        // Configure the EGL, including the bit and depth of the color buffer.
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mDisplayRotationManager = new DisplayRotationManager(this);
        mArFeatureRendererManager = new AugmentedImageRendererManager(this);
        mArFeatureRendererManager.setDisplayRotationManager(mDisplayRotationManager);
        mSurfaceView.setRenderer(mArFeatureRendererManager);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    protected void onResume() {
        LogUtil.debug(TAG, "onResume start");
        super.onResume();
        if (mArSession == null) {
            try {
                mArSession = new ARSession(this.getApplicationContext());
                mArConfigBase = new ARImageTrackingConfig(mArSession);
                mArConfigBase.setFocusMode(ARConfigBase.FocusMode.AUTO_FOCUS);
                if (!setupInitAugmentedImageDatabase(mArConfigBase, "Tech Park")) {
                    LogUtil.error(TAG, "Could not setup augmented image database");
                }
                mArSession.configure(mArConfigBase);

                // Use setImageTrackOnly(false) here if you use ARWorldTrackingConfig to configure sessions.
                mArFeatureRendererManager.isImageTrackOnly(true);
            } catch (Exception capturedException) {
                setMessageWhenError(capturedException);
            }
            if (errorMessage != null) {
                stopArSession();
                return;
            }
        }
        sessionResume(mArFeatureRendererManager);
    }
}

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

package com.huawei.arengine.demos.java.cloudimage;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.huawei.arengine.demos.R;
import com.huawei.arengine.demos.common.BaseActivity;
import com.huawei.arengine.demos.common.JsonUtil;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.arengine.demos.java.cloudimage.controller.AugmentedImageRendererController;
import com.huawei.arengine.demos.java.utils.CommonUtil;
import com.huawei.hiar.ARConfigBase;
import com.huawei.hiar.ARImageTrackingConfig;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.common.CloudServiceState;
import com.huawei.hiar.exceptions.ARFatalException;
import com.huawei.hiar.exceptions.ARUnSupportedConfigurationException;
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException;
import com.huawei.hiar.exceptions.ARUnavailableServiceNotInstalledException;
import com.huawei.hiar.listener.CloudServiceEvent;
import com.huawei.hiar.listener.CloudServiceListener;

import java.util.EventObject;

/**
 * This sample code shows the capability of Huawei AR Engine to identify images through the cloud. In addition, the
 * sample code shows the general process of developing the cloud image recognition app, including how to establish a
 * session with the AR Engine, how to jump to the App Center when no AR Engine is detected on the mobile phone, how to
 * establish connection with the cloud, and how to obtain additional information about the cloud image after obtaining
 * the cloud image.
 *
 * @author HW
 * @since 2021-08-24
 */
public class CloudAugmentedImageActivity extends BaseActivity {
    private static final String TAG = CloudAugmentedImageActivity.class.getSimpleName();

    private static final int OPENGLES_VERSION = 2;

    private Context context;

    private AugmentedImageRendererController augmentedImageRendererController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cloud_image_activity_main);
        init();
    }

    private void init() {
        context = this.getApplicationContext();
        mSurfaceView = findViewById(R.id.cloudImageSurfaceview);
        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(OPENGLES_VERSION);

        // Set the EGL configuration chooser, including for the number of
        // bits of the color buffer and the number of depth bits.
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        augmentedImageRendererController = new AugmentedImageRendererController(this);
        augmentedImageRendererController.setDisplayRotationManager(mDisplayRotationManager);

        mSurfaceView.setRenderer(augmentedImageRendererController);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume start");
        super.onResume();
        if (mArSession == null) {
            try {
                mArSession = new ARSession(this.getApplicationContext());
                mArConfigBase = new ARImageTrackingConfig(mArSession);
                mArConfigBase.setFocusMode(ARConfigBase.FocusMode.AUTO_FOCUS);
                mArConfigBase.setEnableItem(ARConfigBase.ENABLE_CLOUD_AUGMENTED_IMAGE);
                if (!setupInitAugmentedImageDatabase(mArConfigBase, "image_1")) {
                    Log.e(TAG, "Could not setup augmented image database");
                }
                mArSession.configure(mArConfigBase);
                augmentedImageRendererController.isImageTrackOnly(true);
                setCloudServiceStateListener();
                signWithAppId();
            } catch (ARUnSupportedConfigurationException | ARUnavailableServiceNotInstalledException
                | ARUnavailableServiceApkTooOldException | ARFatalException capturedException) {
                setMessageWhenError(capturedException);
            } finally {
                showCapabilitySupportInfo();
            }
            if (errorMessage != null) {
                stopArSession();
                return;
            }
        }
        sessionResume(augmentedImageRendererController);
    }

    /**
     * Send application message to service.
     */
    private void signWithAppId() {
        if (mArSession == null) {
            LogUtil.error(TAG, "session is null");
            return;
        }
        String mAuthJson = JsonUtil.getJson("cloud_image.json", context);
        mArSession.setCloudServiceAuthInfo(mAuthJson);
    }

    private void setCloudServiceStateListener() {
        mArSession.addServiceListener(new CloudImageServiceListener());
    }

    /**
     * Cloud Service Listener
     *
     * @author hw
     * @since 2021-08-24
     */
    private class CloudImageServiceListener implements CloudServiceListener {
        @Override
        public void handleEvent(EventObject eventObject) {
            CloudServiceState state = null;
            if (eventObject instanceof CloudServiceEvent) {
                CloudServiceEvent cloudServiceEvent = (CloudServiceEvent) eventObject;
                state = cloudServiceEvent.getCloudServiceState();
            }
            String tipMsg = CommonUtil.cloudServiceErrorMessage(state);
            if (TextUtils.isEmpty(tipMsg)) {
                return;
            }
            runOnUiThread(() -> Toast.makeText(context, tipMsg, Toast.LENGTH_SHORT).show());
        }
    }
}

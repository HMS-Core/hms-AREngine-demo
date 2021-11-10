/**
 * Copyright 2021. Huawei Technologies Co., Ltd. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.arengine.demos.java.cloudimage;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.huawei.arengine.demos.R;
import com.huawei.arengine.demos.common.BaseActivity;
import com.huawei.arengine.demos.common.DisplayRotationManager;
import com.huawei.arengine.demos.common.JsonUtil;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.arengine.demos.common.SecurityUtil;
import com.huawei.arengine.demos.java.cloudimage.controller.AugmentedImageRenderController;
import com.huawei.hiar.ARAugmentedImageDatabase;
import com.huawei.hiar.ARConfigBase;
import com.huawei.hiar.AREnginesApk;
import com.huawei.hiar.ARImageTrackingConfig;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.common.CloudServiceState;
import com.huawei.hiar.exceptions.ARCameraNotAvailableException;
import com.huawei.hiar.exceptions.ARFatalException;
import com.huawei.hiar.listener.CloudServiceEvent;
import com.huawei.hiar.listener.CloudServiceListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.EventObject;
import java.util.Optional;

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

    private GLSurfaceView glSurfaceView;

    private Context context;

    private AugmentedImageRenderController augmentedImageRenderController;

    private DisplayRotationManager displayRotationManager;

    private boolean isRemindInstall = false;

    private ARSession arSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cloud_image_activity_main);
        init();
    }

    private void init() {
        context = this.getApplicationContext();
        glSurfaceView = findViewById(R.id.cloudImageSurfaceview);
        displayRotationManager = new DisplayRotationManager(this);

        glSurfaceView.setPreserveEGLContextOnPause(true);
        glSurfaceView.setEGLContextClientVersion(OPENGLES_VERSION);

        // Set the EGL configuration chooser, including for the number of
        // bits of the color buffer and the number of depth bits.
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        augmentedImageRenderController = new AugmentedImageRenderController(this, this);
        augmentedImageRenderController.setDisplayRotationManager(displayRotationManager);

        glSurfaceView.setRenderer(augmentedImageRenderController);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume start");
        super.onResume();
        errorMessage = null;
        if (arSession == null) {
            try {
                if (!arEngineAbilityCheck()) {
                    finish();
                    return;
                }
                arSession = new ARSession(this.getApplicationContext());
                ARImageTrackingConfig config = new ARImageTrackingConfig(arSession);
                config.setFocusMode(ARConfigBase.FocusMode.AUTO_FOCUS);
                config.setEnableItem(ARConfigBase.ENABLE_CLOUD_AUGMENTED_IMAGE);
                if (!setupInitAugmentedImageDatabase(config)) {
                    Log.e(TAG, "Could not setup augmented image database");
                }
                arSession.configure(config);
                augmentedImageRenderController.setImageTrackOnly(true);
                augmentedImageRenderController.setArSession(arSession);
            } catch (Exception capturedException) {
                setMessageWhenError(capturedException);
            }
            if (errorMessage != null) {
                stopArSession();
                return;
            }
        }
        try {
            arSession.resume();
            displayRotationManager.registerDisplayListener();
            glSurfaceView.onResume();
            setCloudServiceStateListener();
            signWithAppId();
        } catch (ARCameraNotAvailableException e) {
            Toast.makeText(this, "Camera open failed, please restart the app", Toast.LENGTH_LONG).show();
            arSession = null;
        } catch (ARFatalException e) {
            LogUtil.error(TAG, "ARFatalException in AREngine.");
            Toast
                .makeText(this, "This feature cannot run properly. Check the logs to determine the cause",
                    Toast.LENGTH_LONG)
                .show();
            SecurityUtil.safeFinishActivity(this);
        }
    }

    /**
     * Send application message to service.
     */
    private void signWithAppId() {
        if (arSession == null) {
            LogUtil.error(TAG, "session is null");
            return;
        }
        String mAuthJson = JsonUtil.getJson("cloud_image.json", context);
        arSession.setCloudServiceAuthInfo(mAuthJson);
    }

    private void setCloudServiceStateListener() {
        arSession.addServiceListener(new CloudImageServiceListener());
    }

    private boolean setupInitAugmentedImageDatabase(ARImageTrackingConfig config) {
        Optional<Bitmap> augmentedImageBitmap = loadAugmentedImageBitmap();
        if (!augmentedImageBitmap.isPresent()) {
            return false;
        }

        ARAugmentedImageDatabase augmentedImageDatabase = new ARAugmentedImageDatabase(arSession);
        augmentedImageDatabase.addImage("image_1", augmentedImageBitmap.get());

        config.setAugmentedImageDatabase(augmentedImageDatabase);
        return true;
    }

    private Optional<Bitmap> loadAugmentedImageBitmap() {
        try (InputStream is = getAssets().open("image_default.png")) {
            return Optional.of(BitmapFactory.decodeStream(is));
        } catch (IOException e) {
            Log.e(TAG, "IO exception loading augmented image bitmap.");
        }
        return Optional.empty();
    }

    /**
     * Check whether HUAWEI AR Engine server (com.huawei.arengine.service) is installed on
     * the current device. If not, redirect the user to HUAWEI AppGallery for installation.
     *
     * @return whether HUAWEI AR Engine server is installed.
     */
    private boolean arEngineAbilityCheck() {
        boolean isInstallArEngineApk = AREnginesApk.isAREngineApkReady(this);
        if (!isInstallArEngineApk && isRemindInstall) {
            Toast.makeText(this, "Please agree to install.", Toast.LENGTH_LONG).show();
            finish();
        }
        Log.d(TAG, "Is Install AR Engine Apk: " + isInstallArEngineApk);
        if (!isInstallArEngineApk) {
            startActivity(new Intent(this, com.huawei.arengine.demos.common.ConnectAppMarketActivity.class));
            isRemindInstall = true;
        }
        return AREnginesApk.isAREngineApkReady(this);
    }

    private void stopArSession() {
        Log.d(TAG, "stopArSession start.");
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        if (arSession != null) {
            arSession.stop();
            arSession = null;
        }
        Log.d(TAG, "stopArSession end.");
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause start.");
        super.onPause();
        if (arSession != null) {
            displayRotationManager.unregisterDisplayListener();
            glSurfaceView.onPause();
            arSession.pause();
        }
        Log.d(TAG, "onPause end.");
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy start.");
        if (arSession != null) {
            arSession.stop();
            arSession = null;
        }
        super.onDestroy();
        Log.d(TAG, "onDestroy end.");
    }

    @Override
    public void onWindowFocusChanged(boolean isHasFocus) {
        super.onWindowFocusChanged(isHasFocus);
        if (isHasFocus) {
            getWindow().getDecorView()
                .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
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
            if (state == null) {
                return;
            }
            Log.d(TAG, "handleEvent: CloudImage :" + state);
            String tipMsg = "";
            switch (state) {
                case CLOUD_SERVICE_ERROR_NETWORK_UNAVAILABLE:
                    tipMsg = "network unavailable";
                    break;
                case CLOUD_SERVICE_ERROR_CLOUD_SERVICE_UNAVAILABLE:
                    tipMsg = "cloud service unavailable";
                    break;
                case CLOUD_SERVICE_ERROR_NOT_AUTHORIZED:
                    tipMsg = "cloud service not authorized";
                    break;
                case CLOUD_SERVICE_ERROR_SERVER_VERSION_TOO_OLD:
                    tipMsg = "cloud server version too old";
                    break;
                case CLOUD_SERVICE_ERROR_TIME_EXHAUSTED:
                    tipMsg = "time exhausted";
                    break;
                case CLOUD_SERVICE_ERROR_INTERNAL:
                    tipMsg = "cloud service gallery invalid";
                    break;
                case CLOUD_IMAGE_ERROR_IMAGE_GALLERY_INVALID:
                    tipMsg = "cloud image error, cloud service gallery invalid";
                    break;
                case CLOUD_IMAGE_ERROR_IMAGE_RECOGNIZE_FAILE:
                    tipMsg = "cloud image recognize fail";
                    break;
                default:
                    break;
            }
            if (tipMsg.isEmpty()) {
                return;
            }
            final String finalTipMsg = tipMsg;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, finalTipMsg, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}

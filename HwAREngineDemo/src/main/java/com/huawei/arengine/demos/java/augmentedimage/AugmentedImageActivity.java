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

package com.huawei.arengine.demos.java.augmentedimage;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.huawei.arengine.demos.R;
import com.huawei.arengine.demos.common.DisplayRotationManager;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.arengine.demos.common.PermissionManager;
import com.huawei.arengine.demos.java.augmentedimage.rendering.AugmentedImageRenderManager;
import com.huawei.hiar.ARAugmentedImageDatabase;
import com.huawei.hiar.ARConfigBase;
import com.huawei.hiar.AREnginesApk;
import com.huawei.hiar.ARImageTrackingConfig;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.exceptions.ARCameraNotAvailableException;
import com.huawei.hiar.exceptions.ARUnSupportedConfigurationException;
import com.huawei.hiar.exceptions.ARUnavailableClientSdkTooOldException;
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException;
import com.huawei.hiar.exceptions.ARUnavailableServiceNotInstalledException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * This code demonstrates the augmented image capability of AR Engine, including recognizing the image,
 * obtaining the center of the augmented image, and evaluating the width and height of the physical
 * image on the x axis and z axis with the image center as the origin.
 *
 * @author HW
 * @since 2021-02-04
 */
public class AugmentedImageActivity extends Activity {
    private static final String TAG = AugmentedImageActivity.class.getSimpleName();

    private static final int OPENGLES_VERSION = 2;

    private boolean isRemindInstall = false;

    private AugmentedImageRenderManager augmentedImageRenderController;

    private DisplayRotationManager mDisplayRotationManager;

    private GLSurfaceView glSurfaceView;

    private String errorMessage = null;

    private ARSession mArSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.augment_image_activity_main);
        init();
    }

    private void init() {
        glSurfaceView = findViewById(R.id.ImageSurfaceview);
        glSurfaceView.setPreserveEGLContextOnPause(true);
        glSurfaceView.setEGLContextClientVersion(OPENGLES_VERSION);

        // Configure the EGL, including the bit and depth of the color buffer.
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mDisplayRotationManager = new DisplayRotationManager(this);
        augmentedImageRenderController = new AugmentedImageRenderManager(this);
        augmentedImageRenderController.setDisplayRotationManager(mDisplayRotationManager);
        glSurfaceView.setRenderer(augmentedImageRenderController);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    protected void onResume() {
        LogUtil.debug(TAG, "onResume start");
        super.onResume();
        if (!PermissionManager.hasPermission(this)) {
            this.finish();
        }
        errorMessage = null;
        if (mArSession == null) {
            if (!arEngineAbilityCheck()) {
                finish();
                return;
            }
            try {
                mArSession = new ARSession(this);
                ARImageTrackingConfig config = new ARImageTrackingConfig(mArSession);
                config.setFocusMode(ARConfigBase.FocusMode.AUTO_FOCUS);
                if (!setupInitAugmentedImageDatabase(config)) {
                    LogUtil.error(TAG, "Could not setup augmented image database");
                }
                mArSession.configure(config);
                augmentedImageRenderController.setImageTrackOnly(true);
                augmentedImageRenderController.setArSession(mArSession);
            } catch (ARUnavailableServiceNotInstalledException capturedException) {
                startActivity(new Intent(this, com.huawei.arengine.demos.common.ConnectAppMarketActivity.class));
            } catch (ARUnavailableServiceApkTooOldException capturedException) {
                errorMessage = "Please update HuaweiARService.apk";
            } catch (ARUnavailableClientSdkTooOldException capturedException) {
                errorMessage = "Please update this app";
            } catch (ARUnSupportedConfigurationException capturedException) {
                errorMessage = "The configuration is not supported by the device!";
            } catch (Exception capturedException) {
                errorMessage = "unknown exception throws!";
            }
            if (errorMessage != null) {
                stopArSession();
                return;
            }
        }
        try {
            mArSession.resume();
            mDisplayRotationManager.registerDisplayListener();
            glSurfaceView.onResume();
        } catch (ARCameraNotAvailableException e) {
            Toast.makeText(this, "Camera open failed, please restart the app", Toast.LENGTH_LONG).show();
            mArSession = null;
        }
    }

    /**
     * Initialize the augmented image database.
     *
     * @param config Configures sessions for image recognition and tracking.
     *        In this example, sets the augmented image database.
     * @return Returns whether the database is successfully created.
     */
    private boolean setupInitAugmentedImageDatabase(ARImageTrackingConfig config) {
        Optional<Bitmap> augmentedImageBitmap = loadAugmentedImageBitmap();
        if (!augmentedImageBitmap.isPresent()) {
            return false;
        }

        ARAugmentedImageDatabase augmentedImageDatabase = new ARAugmentedImageDatabase(mArSession);
        augmentedImageDatabase.addImage("Tech Park", augmentedImageBitmap.get());
        config.setAugmentedImageDatabase(augmentedImageDatabase);
        return true;
    }

    private Optional<Bitmap> loadAugmentedImageBitmap() {
        try (InputStream is = getAssets().open("image_default.png")) {
            return Optional.of(BitmapFactory.decodeStream(is));
        } catch (IOException e) {
            LogUtil.error(TAG, "IO exception loading augmented image bitmap.");
        }
        return Optional.empty();
    }

    /**
     * Check whether the AR Engine service (com.huawei.arengine.service) is installed on the
     * current device. If not, redirect to the HUAWEI AppGallery to install it.
     *
     * @return Indicates whether the AR Engine server is installed.
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

    private void stopArSession() {
        LogUtil.debug(TAG, "stopArSession start.");
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        if (mArSession != null) {
            mArSession.stop();
            mArSession = null;
        }
        LogUtil.debug(TAG, "stopArSession end.");
    }

    @Override
    protected void onPause() {
        LogUtil.debug(TAG, "onPause start.");
        super.onPause();
        if (mArSession != null) {
            mDisplayRotationManager.unregisterDisplayListener();
            glSurfaceView.onPause();
            mArSession.pause();
        }
        LogUtil.debug(TAG, "onPause end.");
    }

    @Override
    protected void onDestroy() {
        LogUtil.debug(TAG, "onDestroy start.");
        if (mArSession != null) {
            mArSession.stop();
            mArSession = null;
        }
        super.onDestroy();
        LogUtil.debug(TAG, "onDestroy end.");
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
}

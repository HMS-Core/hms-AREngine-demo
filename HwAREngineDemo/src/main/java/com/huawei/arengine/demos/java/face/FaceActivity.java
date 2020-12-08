/**
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huawei.arengine.demos.java.face;

import android.app.Activity;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.huawei.arengine.demos.R;
import com.huawei.arengine.demos.common.DisplayRotationManager;
import com.huawei.arengine.demos.java.face.rendering.FaceRenderManager;
import com.huawei.hiar.ARConfigBase;
import com.huawei.hiar.AREnginesApk;
import com.huawei.hiar.ARFaceTrackingConfig;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.exceptions.ARCameraNotAvailableException;
import com.huawei.hiar.exceptions.ARUnSupportedConfigurationException;
import com.huawei.hiar.exceptions.ARUnavailableClientSdkTooOldException;
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException;
import com.huawei.hiar.exceptions.ARUnavailableServiceNotInstalledException;

import java.util.List;

/**
 * This demo shows the capabilities of HUAWEI AR Engine to recognize faces, including facial
 * features and facial expressions. In addition, this demo shows how an app can open the camera
 * to display preview.Currently, only apps of the ARface type can open the camera. If you want
 * to the app to open the camera, set isOpenCameraOutside = true in this file.
 *
 * @author HW
 * @since 2020-03-18
 */
public class FaceActivity extends Activity {
    private static final String TAG = FaceActivity.class.getSimpleName();

    private ARSession mArSession;

    private GLSurfaceView glSurfaceView;

    private FaceRenderManager mFaceRenderManager;

    private DisplayRotationManager mDisplayRotationManager;

    private boolean isOpenCameraOutside = false;

    private CameraHelper mCamera;

    private Surface mPreViewSurface;

    private Surface mVgaSurface;

    private Surface mMetaDataSurface;

    private Surface mDepthSurface;

    private ARConfigBase mArConfig;

    private TextView mTextView;

    private String message = null;

    private boolean isRemindInstall = false;

    // The initial texture ID is -1.
    private int textureId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.face_activity_main);
        mTextView = findViewById(R.id.faceTextView);
        glSurfaceView = findViewById(R.id.faceSurfaceview);

        mDisplayRotationManager = new DisplayRotationManager(this);

        glSurfaceView.setPreserveEGLContextOnPause(true);

        // Set the OpenGLES version.
        glSurfaceView.setEGLContextClientVersion(2);

        // Set the EGL configuration chooser, including for the
        // number of bits of the color buffer and the number of depth bits.
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        mFaceRenderManager = new FaceRenderManager(this, this);
        mFaceRenderManager.setDisplayRotationManage(mDisplayRotationManager);
        mFaceRenderManager.setTextView(mTextView);

        glSurfaceView.setRenderer(mFaceRenderManager);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        mDisplayRotationManager.registerDisplayListener();
        Exception exception = null;
        message = null;
        if (mArSession == null) {
            try {
                if (!arEngineAbilityCheck()) {
                    finish();
                    return;
                }
                mArSession = new ARSession(this);
                mArConfig = new ARFaceTrackingConfig(mArSession);

                mArConfig.setPowerMode(ARConfigBase.PowerMode.POWER_SAVING);

                if (isOpenCameraOutside) {
                    mArConfig.setImageInputMode(ARConfigBase.ImageInputMode.EXTERNAL_INPUT_ALL);
                }
                mArSession.configure(mArConfig);
            } catch (Exception capturedException) {
                exception = capturedException;
                setMessageWhenError(capturedException);
            }
            if (message != null) {
                stopArSession(exception);
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
        setCamera();
        mFaceRenderManager.setArSession(mArSession);
        mFaceRenderManager.setOpenCameraOutsideFlag(isOpenCameraOutside);
        mFaceRenderManager.setTextureId(textureId);
        glSurfaceView.onResume();
    }

    /**
     * Check whether HUAWEI AR Engine server (com.huawei.arengine.service) is installed on the current device.
     * If not, redirect the user to HUAWEI AppGallery for installation.
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

    private void setMessageWhenError(Exception catchException) {
        if (catchException instanceof ARUnavailableServiceNotInstalledException) {
            startActivity(new Intent(this, com.huawei.arengine.demos.common.ConnectAppMarketActivity.class));
        } else if (catchException instanceof ARUnavailableServiceApkTooOldException) {
            message = "Please update HuaweiARService.apk";
        } else if (catchException instanceof ARUnavailableClientSdkTooOldException) {
            message = "Please update this app";
        } else if (catchException instanceof ARUnSupportedConfigurationException) {
            message = "The configuration is not supported by the device!";
        } else {
            message = "exception throw";
        }
    }

    private void stopArSession(Exception exception) {
        Log.i(TAG, "Stop session start.");
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Creating session error ", exception);
        if (mArSession != null) {
            mArSession.stop();
            mArSession = null;
        }
        Log.i(TAG, "Stop session end.");
    }

    private void setCamera() {
        if (isOpenCameraOutside && mCamera == null) {
            Log.i(TAG, "new Camera");
            DisplayMetrics dm = new DisplayMetrics();
            mCamera = new CameraHelper(this);
            mCamera.setupCamera(dm.widthPixels, dm.heightPixels);
        }

        // Check whether setCamera is called for the first time.
        if (isOpenCameraOutside) {
            if (textureId != -1) {
                mArSession.setCameraTextureName(textureId);
                initSurface();
            } else {
                int[] textureIds = new int[1];
                GLES20.glGenTextures(1, textureIds, 0);
                textureId = textureIds[0];
                mArSession.setCameraTextureName(textureId);
                initSurface();
            }

            SurfaceTexture surfaceTexture = new SurfaceTexture(textureId);
            mCamera.setPreviewTexture(surfaceTexture);
            mCamera.setPreViewSurface(mPreViewSurface);
            mCamera.setVgaSurface(mVgaSurface);
            mCamera.setDepthSurface(mDepthSurface);
            if (!mCamera.openCamera()) {
                String showMessage = "Open camera filed!";
                Log.e(TAG, showMessage);
                Toast.makeText(this, showMessage, Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void initSurface() {
        List<ARConfigBase.SurfaceType> surfaceTypeList = mArConfig.getImageInputSurfaceTypes();
        List<Surface> surfaceList = mArConfig.getImageInputSurfaces();

        Log.i(TAG, "surfaceList size : " + surfaceList.size());
        int size = surfaceTypeList.size();
        for (int i = 0; i < size; i++) {
            ARConfigBase.SurfaceType type = surfaceTypeList.get(i);
            Surface surface = surfaceList.get(i);
            if (ARConfigBase.SurfaceType.PREVIEW.equals(type)) {
                mPreViewSurface = surface;
            } else if (ARConfigBase.SurfaceType.VGA.equals(type)) {
                mVgaSurface = surface;
            } else if (ARConfigBase.SurfaceType.METADATA.equals(type)) {
                mMetaDataSurface = surface;
            } else if (ARConfigBase.SurfaceType.DEPTH.equals(type)) {
                mDepthSurface = surface;
            } else {
                Log.i(TAG, "Unknown type.");
            }
            Log.i(TAG, "list[" + i + "] get surface : " + surface + ", type : " + type);
        }
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause start.");
        super.onPause();
        if (isOpenCameraOutside) {
            if (mCamera != null) {
                mCamera.closeCamera();
                mCamera.stopCameraThread();
                mCamera = null;
            }
        }

        if (mArSession != null) {
            mDisplayRotationManager.unregisterDisplayListener();
            glSurfaceView.onPause();
            mArSession.pause();
            Log.i(TAG, "Session paused!");
        }
        Log.i(TAG, "onPause end.");
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy start.");
        super.onDestroy();
        if (mArSession != null) {
            Log.i(TAG, "Session onDestroy!");
            mArSession.stop();
            mArSession = null;
            Log.i(TAG, "Session stop!");
        }
        Log.i(TAG, "onDestroy end.");
    }

    @Override
    public void onWindowFocusChanged(boolean isHasFocus) {
        Log.d(TAG, "onWindowFocusChanged");
        super.onWindowFocusChanged(isHasFocus);
        if (isHasFocus) {
            getWindow().getDecorView()
                .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }
}
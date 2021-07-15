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

package com.huawei.arengine.demos.java.cloudaugmentedobject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.huawei.arengine.demos.R;
import com.huawei.arengine.demos.common.DisplayRotationManager;
import com.huawei.arengine.demos.common.JsonUtil;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.arengine.demos.common.PermissionManager;
import com.huawei.arengine.demos.java.cloudaugmentedobject.rendering.ObjectRenderManager;
import com.huawei.hiar.ARConfigBase;
import com.huawei.hiar.AREnginesApk;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.ARWorldTrackingConfig;
import com.huawei.hiar.common.CloudServiceState;
import com.huawei.hiar.exceptions.ARCameraNotAvailableException;
import com.huawei.hiar.exceptions.ARUnSupportedConfigurationException;
import com.huawei.hiar.exceptions.ARUnavailableClientSdkTooOldException;
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException;
import com.huawei.hiar.exceptions.ARUnavailableServiceNotInstalledException;
import com.huawei.hiar.listener.CloudServiceEvent;
import com.huawei.hiar.listener.CloudServiceListener;

import java.util.EventObject;

/**
 * Demonstrates how to use AR Engine to recognize cloud 3D objects, including recognizing the 3D objects
 * and obtaining their pose, name, and ID.
 *
 * @author HW
 * @since 2021-02-04
 */
public class CloudAugmentedObjectActivity extends Activity {
    private static final String TAG = CloudAugmentedObjectActivity.class.getSimpleName();

    private static final int OPENGLES_VERSION = 2;

    private static final int AR_ENGINE_SERVICE_CALL = 10001;

    /**
     * Hms login.
     */
    private static final int REQUEST_SIGN_IN_LOGIN = 1002;

    private boolean isRemindInstall = false;

    private ObjectRenderManager objectRenderManager;

    private DisplayRotationManager mDisplayRotationManager;

    private GLSurfaceView glSurfaceView;

    private String errorMessage = null;

    private Context context;

    private ARSession mArSession;

    private Handler mHandler = new Handler() {
        String tipMsg = "";

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AR_ENGINE_SERVICE_CALL:
                    if (msg.obj instanceof String) {
                        tipMsg = (String) msg.obj;
                    }
                    break;
                default:
                    break;
            }
            if (tipMsg.length() > 0) {
                Toast.makeText(context, tipMsg, Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.cloud_augment_object_activity_main);
        init();
    }

    private void init() {
        context = getApplicationContext();
        glSurfaceView = findViewById(R.id.ObjectSurfaceview);
        glSurfaceView.setPreserveEGLContextOnPause(true);
        glSurfaceView.setEGLContextClientVersion(OPENGLES_VERSION);

        // Configure the EGL, including the bit and depth of the color buffer.
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mDisplayRotationManager = new DisplayRotationManager(this);

        objectRenderManager = new ObjectRenderManager(this);
        objectRenderManager.setDisplayRotationManager(mDisplayRotationManager);
        glSurfaceView.setRenderer(objectRenderManager);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    /**
     * Send application message to service.
     */
    private void signWithAppId() {
        String mAuthJson = JsonUtil.getJson("appid.json", context);
        Log.d(TAG, "mAuthJson : " + mAuthJson);
        mArSession.setCloudServiceAuthInfo(mAuthJson);
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
                ARWorldTrackingConfig config = new ARWorldTrackingConfig(mArSession);
                config.setFocusMode(ARConfigBase.FocusMode.AUTO_FOCUS);
                config.setEnableItem(ARConfigBase.ENABLE_CLOUD_OBJECT_RECOGNITION);

                mArSession.configure(config);
                objectRenderManager.setArSession(mArSession);
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
        setCloudServiceStateListener();
        signWithAppId();
    }

    /**
     * Check whether HUAWEI AR Engine server (com.huawei.arengine.service) is installed on the current device.
     * If not, redirect the user to HUAWEI AppGallery for installation.
     *
     * @return true:AR Engine ready
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

    private void setCloudServiceStateListener() {
        mArSession.addServiceListener(new CloudImageServiceListener());
    }

    /**
     * Cloud Service Listener
     *
     * @author hw
     * @since 2021-04-20
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
                case CLOUD_OBJECT_ERROR_OBJECT_MODEL_INVALID:
                    tipMsg = "cloud object error, object invalid";
                    break;
                case CLOUD_OBJECT_ERROR_OBJECT_RECOGNIZE_FAILE:
                    tipMsg = "cloud object recognize fail";
                    break;
                default:
            }
            mHandler.sendMessage(mHandler.obtainMessage(AR_ENGINE_SERVICE_CALL, tipMsg));
        }
    }
}

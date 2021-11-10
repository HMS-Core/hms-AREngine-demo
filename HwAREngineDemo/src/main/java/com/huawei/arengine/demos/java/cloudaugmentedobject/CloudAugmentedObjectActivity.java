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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.huawei.arengine.demos.R;
import com.huawei.arengine.demos.common.BaseActivity;
import com.huawei.arengine.demos.common.DisplayRotationManager;
import com.huawei.arengine.demos.common.JsonUtil;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.arengine.demos.common.PermissionManager;
import com.huawei.arengine.demos.common.SecurityUtil;
import com.huawei.arengine.demos.java.cloudaugmentedobject.rendering.ObjectRenderManager;
import com.huawei.hiar.ARConfigBase;
import com.huawei.hiar.AREnginesApk;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.ARWorldTrackingConfig;
import com.huawei.hiar.common.CloudServiceState;
import com.huawei.hiar.exceptions.ARCameraNotAvailableException;
import com.huawei.hiar.exceptions.ARFatalException;
import com.huawei.hiar.listener.CloudServiceEvent;
import com.huawei.hiar.listener.CloudServiceListener;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

/**
 * Demonstrates how to use AR Engine to recognize cloud 3D objects, including recognizing the 3D objects
 * and obtaining their pose, name, and ID.
 *
 * @author HW
 * @since 2021-02-04
 */
public class CloudAugmentedObjectActivity extends BaseActivity {
    private static final String TAG = CloudAugmentedObjectActivity.class.getSimpleName();

    private static final int OPENGLES_VERSION = 2;

    private static final int AR_ENGINE_SERVICE_CALL = 10001;

    private static final int LIST_MAX_SIZE = 1024;

    private boolean isRemindInstall = false;

    private ObjectRenderManager objectRenderManager;

    private DisplayRotationManager displayRotationManager;

    private GLSurfaceView glSurfaceView;

    private Context context;

    private ARSession arSession;

    private AlertDialog.Builder builder;

    private Dialog dialog;

    private ListView dialogList;

    private List<ModeInformation> modeIdList;

    private TextView changeMode;

    private Handler handler = new Handler() {
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
        changeMode = findViewById(R.id.change_mode_id);
        changeMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialogList();
            }
        });

        // Configure the EGL, including the bit and depth of the color buffer.
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        displayRotationManager = new DisplayRotationManager(this);

        objectRenderManager = new ObjectRenderManager(this);
        objectRenderManager.setDisplayRotationManager(displayRotationManager);
        glSurfaceView.setRenderer(objectRenderManager);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    /**
     * Send application message to service.
     */
    private void signWithAppIdNew() {
        String authJson = JsonUtil.readApplicationMessage(context);
        if (authJson.isEmpty()) {
            LogUtil.debug(TAG, "mAuthJson is isEmpty, select first item in list ");
            String jsonString = JsonUtil.getJson("mode_id.json", context);
            List<ModeInformation> modeList = JsonUtil.json2List(jsonString);
            if (modeList.size() <= 0) {
                LogUtil.error(TAG, "sign error, get application message error");
                Toast.makeText(context, "sign error, get application message error", Toast.LENGTH_SHORT).show();
                return;
            }
            authJson = modeList.get(0).getModeInformation();
            JsonUtil.writeApplicationMessage(context, authJson);
        }
        arSession.setCloudServiceAuthInfo(authJson);
    }

    /**
     * Display the list of countries/regions. Switch the modelName based on the current network environment.
     */
    private void showDialogList() {
        List<String> keyList = new ArrayList<>(LIST_MAX_SIZE);
        if (modeIdList == null) {
            String jsonString = JsonUtil.getJson("mode_id.json", context);
            modeIdList = JsonUtil.json2List(jsonString);
        }
        for (ModeInformation mode : modeIdList) {
            if (mode == null) {
                continue;
            }
            keyList.add(mode.getContinents());
        }
        builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_list, null);
        dialogList = view.findViewById(R.id.select_dialog_listview);
        builder.setView(view).setCancelable(false);

        dialogList.setAdapter(new ArrayAdapter<String>(CloudAugmentedObjectActivity.this, R.layout.dialog_list_item,
            R.id.list_item, keyList));
        dialogList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                JsonUtil.writeApplicationMessage(context, modeIdList.get(position).getModeInformation());
                dialog.dismiss();
                finish();
            }
        });
        dialog = builder.show();
    }

    @Override
    protected void onResume() {
        LogUtil.debug(TAG, "onResume start");
        super.onResume();
        if (!PermissionManager.hasPermission(this)) {
            this.finish();
        }
        errorMessage = null;
        if (arSession == null) {
            if (!arEngineAbilityCheck()) {
                finish();
                return;
            }
            try {
                arSession = new ARSession(this.getApplicationContext());
                ARWorldTrackingConfig config = new ARWorldTrackingConfig(arSession);
                config.setFocusMode(ARConfigBase.FocusMode.AUTO_FOCUS);
                config.setEnableItem(ARConfigBase.ENABLE_CLOUD_OBJECT_RECOGNITION);

                arSession.configure(config);
                objectRenderManager.setArSession(arSession);
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
            signWithAppIdNew();
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
     * Check whether HUAWEI AR Engine server (com.huawei.arengine.service) is installed on the current device.
     * If not, redirect the user to HUAWEI AppGallery for installation.
     *
     * @return true:AR Engine ready.
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
        if (arSession != null) {
            arSession.stop();
            arSession = null;
        }
        LogUtil.debug(TAG, "stopArSession end.");
    }

    @Override
    protected void onPause() {
        LogUtil.debug(TAG, "onPause start.");
        super.onPause();
        if (arSession != null) {
            displayRotationManager.unregisterDisplayListener();
            glSurfaceView.onPause();
            arSession.pause();
        }
        LogUtil.debug(TAG, "onPause end.");
    }

    @Override
    protected void onDestroy() {
        LogUtil.debug(TAG, "onDestroy start.");
        if (arSession != null) {
            arSession.stop();
            arSession = null;
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
        arSession.addServiceListener(new CloudImageServiceListener());
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
            LogUtil.debug(TAG, "handleEvent: CloudImage :" + state);
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
            handler.sendMessage(handler.obtainMessage(AR_ENGINE_SERVICE_CALL, tipMsg));
        }
    }
}

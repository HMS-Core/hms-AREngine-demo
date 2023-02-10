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

package com.huawei.arengine.demos.java.cloudaugmentedobject;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.huawei.arengine.demos.R;
import com.huawei.arengine.demos.common.BaseActivity;
import com.huawei.arengine.demos.common.JsonUtil;
import com.huawei.arengine.demos.common.ListDialog;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.arengine.demos.java.cloudaugmentedobject.rendering.ObjectRendererManager;
import com.huawei.arengine.demos.java.utils.CommonUtil;
import com.huawei.hiar.ARConfigBase;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.ARWorldTrackingConfig;
import com.huawei.hiar.common.CloudServiceState;
import com.huawei.hiar.exceptions.ARFatalException;
import com.huawei.hiar.exceptions.ARUnSupportedConfigurationException;
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException;
import com.huawei.hiar.exceptions.ARUnavailableServiceNotInstalledException;
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

    private ObjectRendererManager objectRendererManager;

    private Context context;

    private List<ModeInformation> modeIdList;

    private TextView changeMode;

    private Handler handler = new Handler(Looper.getMainLooper()) {
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
        mSurfaceView = findViewById(R.id.ObjectSurfaceview);
        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(OPENGLES_VERSION);
        changeMode = findViewById(R.id.change_mode_id);
        changeMode.setOnClickListener(view -> showDialogList());

        // Configure the EGL, including the bit and depth of the color buffer.
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        objectRendererManager = new ObjectRendererManager(this);
        objectRendererManager.setDisplayRotationManager(mDisplayRotationManager);
        TextView textView = findViewById(R.id.cloudAugmentObjectTextView);
        objectRendererManager.setTextView(textView);
        mSurfaceView.setRenderer(objectRendererManager);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
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
        mArSession.setCloudServiceAuthInfo(authJson);
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
        ListDialog dialogUtils = new ListDialog();
        dialogUtils.setDialogOnItemClickListener(new ListDialog.DialogOnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                JsonUtil.writeApplicationMessage(context, modeIdList.get(position).getModeInformation());
                finish();
            }
        });
        dialogUtils.showDialogList(CloudAugmentedObjectActivity.this, keyList);
    }

    @Override
    protected void onResume() {
        LogUtil.debug(TAG, "onResume start");
        super.onResume();
        if (mArSession == null) {
            try {
                mArSession = new ARSession(this.getApplicationContext());
                mArConfigBase = new ARWorldTrackingConfig(mArSession);
                mArConfigBase.setFocusMode(ARConfigBase.FocusMode.AUTO_FOCUS);
                mArConfigBase.setEnableItem(ARConfigBase.ENABLE_CLOUD_OBJECT_RECOGNITION);
                mArSession.configure(mArConfigBase);
                setCloudServiceStateListener();
                signWithAppIdNew();
            } catch (ARUnavailableServiceNotInstalledException | ARUnavailableServiceApkTooOldException
                | ARUnSupportedConfigurationException | ARFatalException capturedException) {
                setMessageWhenError(capturedException);
            } finally {
                showCapabilitySupportInfo();
            }
            if (errorMessage != null) {
                stopArSession();
                return;
            }
        }
        sessionResume(objectRendererManager);
    }

    private void setCloudServiceStateListener() {
        mArSession.addServiceListener(new CloudObjectServiceListener());
    }

    /**
     * Cloud Service Listener
     *
     * @author hw
     * @since 2021-04-20
     */
    private class CloudObjectServiceListener implements CloudServiceListener {
        @Override
        public void handleEvent(EventObject event) {
            CloudServiceState state = null;
            if (event instanceof CloudServiceEvent) {
                CloudServiceEvent cloudServiceEvent = (CloudServiceEvent) event;
                state = cloudServiceEvent.getCloudServiceState();
            }
            String tipMsg = CommonUtil.cloudServiceErrorMessage(state);
            if (TextUtils.isEmpty(tipMsg)) {
                return;
            }
            handler.sendMessage(handler.obtainMessage(AR_ENGINE_SERVICE_CALL, tipMsg));
        }
    }
}

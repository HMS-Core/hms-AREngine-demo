/**
 * Copyright 2021. Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huawei.arengine.demos.java.world;

import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.huawei.arengine.demos.R;
import com.huawei.arengine.demos.common.BaseActivity;
import com.huawei.arengine.demos.common.DisplayRotationManager;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.arengine.demos.common.PermissionManager;
import com.huawei.arengine.demos.java.world.rendering.WorldRenderManager;
import com.huawei.hiar.ARConfigBase;
import com.huawei.hiar.AREnginesApk;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.ARWorldTrackingConfig;
import com.huawei.hiar.exceptions.ARCameraNotAvailableException;
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * This AR example shows how to use the world AR scene of HUAWEI AR Engine,
 * including how to identify planes, use the click function, and identify
 * specific images.
 *
 * @author HW
 * @since 2020-04-05
 */
public class WorldActivity extends BaseActivity {
    private static final String TAG = WorldActivity.class.getSimpleName();

    private static final String UPDATE_TOAST_MSG = "Please update HUAWEI AR Engine app in the AppGallery.";

    private static final int MOTIONEVENT_QUEUE_CAPACITY = 2;

    private static final int OPENGLES_VERSION = 2;

    private static final long BUTTON_REPEAT_CLICK_INTERVAL_TIME = 2000L;

    private static final int MSG_ENV_LIGHT_BUTTON_CLICK_ENABLE = 1;

    private static final int MSG_ENV_TEXTURE_BUTTON_CLICK_ENABLE = 2;

    private ARSession mArSession;

    private GLSurfaceView mSurfaceView;

    private ToggleButton mEnvLightingBtn;

    private ToggleButton mEnvTextureBtn;

    private RelativeLayout mEnvTextureLayout;

    private WorldRenderManager mWorldRenderManager;

    private GestureDetector mGestureDetector;

    private DisplayRotationManager mDisplayRotationManager;

    private ARWorldTrackingConfig mConfig;

    private ArrayBlockingQueue<GestureEvent> mQueuedSingleTaps = new ArrayBlockingQueue<>(MOTIONEVENT_QUEUE_CAPACITY);

    private boolean isRemindInstall = false;

    private boolean mEnvLightModeOpen = false;

    private boolean mEnvTextureModeOpen = false;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ENV_LIGHT_BUTTON_CLICK_ENABLE:
                    mEnvLightingBtn.setEnabled(true);
                    break;
                case MSG_ENV_TEXTURE_BUTTON_CLICK_ENABLE:
                    mEnvTextureBtn.setEnabled(true);
                    break;
                default:
                    LogUtil.info(TAG, "handleMessage default in WorldActivity.");
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LogUtil.info(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.world_java_activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initViews();
        initClickListener();
        mDisplayRotationManager = new DisplayRotationManager(this);
        initGestureDetector();

        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(OPENGLES_VERSION);

        // Set the EGL configuration chooser, including for the number of
        // bits of the color buffer and the number of depth bits.
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        mWorldRenderManager = new WorldRenderManager(this, this);
        mWorldRenderManager.setDisplayRotationManage(mDisplayRotationManager);
        mWorldRenderManager.setQueuedSingleTaps(mQueuedSingleTaps);

        mSurfaceView.setRenderer(mWorldRenderManager);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    private void initViews() {
        mSurfaceView = findViewById(R.id.surfaceview);
        mEnvLightingBtn = findViewById(R.id.btn_env_light_mode);
        mEnvTextureBtn = findViewById(R.id.btn_env_texture_mode);
        mEnvTextureLayout = findViewById(R.id.img_env_texture);
    }

    private void initClickListener() {
        mEnvLightingBtn.setOnCheckedChangeListener((compoundButton, b) -> {
            mEnvLightingBtn.setEnabled(false);
            handler.sendEmptyMessageDelayed(MSG_ENV_LIGHT_BUTTON_CLICK_ENABLE,
                    BUTTON_REPEAT_CLICK_INTERVAL_TIME);
            mEnvLightModeOpen = !mEnvLightModeOpen;
            int lightingMode = refreshLightMode(mEnvLightModeOpen, ARConfigBase.LIGHT_MODE_ENVIRONMENT_LIGHTING);
            refreshConfig(lightingMode);
        });

        mEnvTextureBtn.setOnCheckedChangeListener((compoundButton, b) -> {
            mEnvTextureBtn.setEnabled(false);
            handler.sendEmptyMessageDelayed(MSG_ENV_TEXTURE_BUTTON_CLICK_ENABLE,
                    BUTTON_REPEAT_CLICK_INTERVAL_TIME);
            mEnvTextureModeOpen = !mEnvTextureModeOpen;
            refreshEnvTextureLayout();
            int lightingMode = refreshLightMode(mEnvTextureModeOpen, ARConfigBase.LIGHT_MODE_ENVIRONMENT_TEXTURE);
            refreshConfig(lightingMode);
        });
    }

    private void refreshEnvTextureLayout() {
        if (mEnvTextureModeOpen) {
            mEnvTextureLayout.setVisibility(View.VISIBLE);
        } else {
            mEnvTextureLayout.setVisibility(View.GONE);
        }
    }

    private int refreshLightMode(boolean isOpen, int changeMode) {
        LogUtil.info(TAG, "isOPen = " + isOpen + "changeMode = " + changeMode);
        int curLightMode = mConfig.getLightingMode();
        curLightMode = isOpen ? (curLightMode | changeMode) : (curLightMode & ~changeMode);
        return curLightMode;
    }

    private void initGestureDetector() {
        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent motionEvent) {
                onGestureEvent(GestureEvent.createDoubleTapEvent(motionEvent));
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
                onGestureEvent(GestureEvent.createSingleTapConfirmEvent(motionEvent));
                return true;
            }

            @Override
            public boolean onDown(MotionEvent motionEvent) {
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                onGestureEvent(GestureEvent.createScrollEvent(e1, e2, distanceX, distanceY));
                return true;
            }
        });

        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mGestureDetector.onTouchEvent(event);
            }
        });
    }

    private void onGestureEvent(GestureEvent e) {
        boolean offerResult = mQueuedSingleTaps.offer(e);
        if (offerResult) {
            LogUtil.debug(TAG, "Successfully joined the queue.");
        } else {
            LogUtil.debug(TAG, "Failed to join queue.");
        }
    }

    @Override
    protected void onResume() {
        LogUtil.debug(TAG, "onResume");
        super.onResume();
        if (!PermissionManager.hasPermission(this)) {
            this.finish();
        }
        errorMessage = null;
        if (mArSession == null) {
            try {
                if (!arEngineAbilityCheck()) {
                    finish();
                    return;
                }
                mArSession = new ARSession(this.getApplicationContext());
                mConfig = new ARWorldTrackingConfig(mArSession);
                refreshConfig(ARConfigBase.LIGHT_MODE_ENVIRONMENT_LIGHTING | ARConfigBase.LIGHT_MODE_ENVIRONMENT_TEXTURE);
            } catch (Exception capturedException) {
                setMessageWhenError(capturedException);
            }

            if (errorMessage != null) {
                stopArSession();
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
        mSurfaceView.onResume();
    }

    private void refreshConfig(int lightingMode) {
        try {
            mConfig.setFocusMode(ARConfigBase.FocusMode.AUTO_FOCUS);
            mConfig.setSemanticMode(ARWorldTrackingConfig.SEMANTIC_PLANE | ARWorldTrackingConfig.SEMANTIC_TARGET);
            mConfig.setLightingMode(lightingMode);
            mArSession.configure(mConfig);
        } catch (ARUnavailableServiceApkTooOldException capturedException) {
            Toast.makeText(this, "Please update HuaweiARService.apk", Toast.LENGTH_LONG).show();
        } finally {
            mWorldRenderManager.setArSession(mArSession);
            mWorldRenderManager.setArWorldTrackingConfig(mConfig);
            showSemanticModeSupportedInfo();
        }
    }

    private void showSemanticModeSupportedInfo() {
        String toastMsg = "";
        switch (mArSession.getSupportedSemanticMode()) {
            case ARWorldTrackingConfig.SEMANTIC_NONE:
                toastMsg = "The running environment does not support the semantic mode.";
                break;
            case ARWorldTrackingConfig.SEMANTIC_PLANE:
                toastMsg = "The running environment supports only the plane semantic mode.";
                break;
            case ARWorldTrackingConfig.SEMANTIC_TARGET:
                toastMsg = "The running environment supports only the target semantic mode.";
                break;
            default:
                break;
        }
        if (!toastMsg.isEmpty()) {
            Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();
        }
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
        LogUtil.info(TAG, "stopArSession start.");
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        if (mArSession != null) {
            mArSession.stop();
            mArSession = null;
        }
        LogUtil.info(TAG, "stopArSession end.");
    }

    @Override
    protected void onPause() {
        LogUtil.info(TAG, "onPause start.");
        super.onPause();
        if (mArSession != null) {
            mDisplayRotationManager.unregisterDisplayListener();
            mSurfaceView.onPause();
            mArSession.pause();
        }
        LogUtil.info(TAG, "onPause end.");
    }

    @Override
    protected void onDestroy() {
        LogUtil.info(TAG, "onDestroy start.");
        if (mArSession != null) {
            mArSession.stop();
            mArSession = null;
        }
        if (mWorldRenderManager != null) {
            mWorldRenderManager.releaseARAnchor();
        }
        super.onDestroy();
        LogUtil.info(TAG, "onDestroy end.");
    }

    @Override
    public void onWindowFocusChanged(boolean isHasFocus) {
        LogUtil.debug(TAG, "onWindowFocusChanged");
        super.onWindowFocusChanged(isHasFocus);
        if (isHasFocus) {
            getWindow().getDecorView()
                .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }
}
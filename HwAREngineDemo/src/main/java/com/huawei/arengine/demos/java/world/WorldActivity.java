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

package com.huawei.arengine.demos.java.world;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.huawei.arengine.demos.R;
import com.huawei.arengine.demos.common.BaseActivity;
import com.huawei.arengine.demos.common.GestureDetectorUtils;
import com.huawei.arengine.demos.common.GestureEvent;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.arengine.demos.java.world.rendering.WorldRendererManager;
import com.huawei.hiar.ARConfigBase;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.ARWorldTrackingConfig;
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

    private static final int MSG_CONFIG_MAP_SIZE_BUTTON_CLICK_ENABLE = 3;

    private static final int DEFAULT_MAX_MAP_SIZE = 800;

    private ToggleButton mEnvLightingBtn;

    private ToggleButton mEnvTextureBtn;

    private RelativeLayout mEnvTextureLayout;

    private Button mConfigMaxSizeButton;

    private WorldRendererManager mWorldRendererManager;

    private ArrayBlockingQueue<GestureEvent> mQueuedSingleTaps = new ArrayBlockingQueue<>(MOTIONEVENT_QUEUE_CAPACITY);

    private boolean mIsEnvLightModeOpen = false;

    private boolean mIsEnvTextureModeOpen = false;

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ENV_LIGHT_BUTTON_CLICK_ENABLE:
                    mEnvLightingBtn.setEnabled(true);
                    break;
                case MSG_ENV_TEXTURE_BUTTON_CLICK_ENABLE:
                    mEnvTextureBtn.setEnabled(true);
                    break;
                case MSG_CONFIG_MAP_SIZE_BUTTON_CLICK_ENABLE:
                    mConfigMaxSizeButton.setClickable(true);
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
        GestureDetectorUtils.initGestureDetector(this, TAG, mSurfaceView, mQueuedSingleTaps);

        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(OPENGLES_VERSION);

        // Set the EGL configuration chooser, including for the number of
        // bits of the color buffer and the number of depth bits.
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        mWorldRendererManager = new WorldRendererManager(this);
        mWorldRendererManager.setDisplayRotationManager(mDisplayRotationManager);
        TextView textView = findViewById(R.id.wordTextView);
        mWorldRendererManager.setTextView(textView);
        mWorldRendererManager.setQueuedSingleTaps(mQueuedSingleTaps);

        mSurfaceView.setRenderer(mWorldRendererManager);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    private void initViews() {
        mSurfaceView = findViewById(R.id.surfaceview);
        mEnvLightingBtn = findViewById(R.id.btn_env_light_mode);
        mEnvTextureBtn = findViewById(R.id.btn_env_texture_mode);
        mEnvTextureLayout = findViewById(R.id.img_env_texture);
        mConfigMaxSizeButton = findViewById(R.id.btn_config_session);
    }

    private void initClickListener() {
        mEnvLightingBtn.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            mEnvLightingBtn.setEnabled(false);
            handler.sendEmptyMessageDelayed(MSG_ENV_LIGHT_BUTTON_CLICK_ENABLE, BUTTON_REPEAT_CLICK_INTERVAL_TIME);
            mIsEnvLightModeOpen = !mIsEnvLightModeOpen;
            int lightingMode = refreshLightMode(mIsEnvLightModeOpen, ARConfigBase.LIGHT_MODE_ENVIRONMENT_LIGHTING);
            refreshConfig(lightingMode);
        });

        mEnvTextureBtn.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            mEnvTextureBtn.setEnabled(false);
            handler.sendEmptyMessageDelayed(MSG_ENV_TEXTURE_BUTTON_CLICK_ENABLE, BUTTON_REPEAT_CLICK_INTERVAL_TIME);
            mIsEnvTextureModeOpen = !mIsEnvTextureModeOpen;
            refreshEnvTextureLayout();
            int lightingMode = refreshLightMode(mIsEnvTextureModeOpen, ARConfigBase.LIGHT_MODE_ENVIRONMENT_TEXTURE);
            refreshConfig(lightingMode);
        });

        mConfigMaxSizeButton.setOnClickListener(view -> {
            mConfigMaxSizeButton.setClickable(false);
            resetAndConfigArSession();
            handler.sendEmptyMessageDelayed(MSG_CONFIG_MAP_SIZE_BUTTON_CLICK_ENABLE, BUTTON_REPEAT_CLICK_INTERVAL_TIME);
        });
    }

    private long getInputMaxMapSize() {
        long maxMapSize = 0L;
        EditText editText = findViewById(R.id.text_max_size);
        if (editText == null) {
            return maxMapSize;
        }
        try {
            maxMapSize = Integer.parseInt(editText.getText().toString());
        } catch (NumberFormatException exception) {
            LogUtil.debug(TAG, "getInputMaxMapSize catch:" + exception.getClass());
        }
        return maxMapSize;
    }

    private long getMaxMapSize() {
        long maxMapSize = 0L;
        if (mArConfigBase == null) {
            LogUtil.warn(TAG, "getMaxMapSize mConfig invalid.");
            return maxMapSize;
        }
        try {
            maxMapSize = mArConfigBase.getMaxMapSize();
        } catch (ARUnavailableServiceApkTooOldException exception) {
            LogUtil.warn(TAG, "getMaxMapSize catch:" + exception.getClass());
        }
        return maxMapSize;
    }

    private void setMaxMapSize() {
        if (mArSession == null || mArConfigBase == null) {
            LogUtil.warn(TAG, "setMaxMapSize mArSession or mConfig invalid.");
            return;
        }
        long maxMapSize = getInputMaxMapSize();
        if (maxMapSize == 0) {
            return;
        }
        try {
            mArConfigBase.setMaxMapSize(maxMapSize);
        } catch (ARUnavailableServiceApkTooOldException exception) {
            Toast
                .makeText(WorldActivity.this.getApplicationContext(),
                    "The current AR Engine version does not support the setMaxMapSize method.", Toast.LENGTH_LONG)
                .show();
        }
    }

    private void resetAndConfigArSession() {
        stopArSession();
        mWorldRendererManager.setArSession(mArSession);
        mArSession = new ARSession(WorldActivity.this.getApplicationContext());
        mArConfigBase = new ARWorldTrackingConfig(mArSession);
        setMaxMapSize();
        int lightMode = getCurrentLightingMode();
        refreshConfig(lightMode);
        mArSession.resume();
        mWorldRendererManager.setArSession(mArSession);
        mDisplayRotationManager.onDisplayChanged(0);
    }

    private void refreshEnvTextureLayout() {
        if (mIsEnvTextureModeOpen) {
            mEnvTextureLayout.setVisibility(View.VISIBLE);
        } else {
            mEnvTextureLayout.setVisibility(View.GONE);
        }
    }

    private int refreshLightMode(boolean isOpen, int changeMode) {
        LogUtil.info(TAG, "isOPen = " + isOpen + "changeMode = " + changeMode);
        int lightMode = getCurrentLightingMode();
        return isOpen ? (lightMode | changeMode) : (lightMode & ~changeMode);
    }

    private int getCurrentLightingMode() {
        int curLightMode = ARConfigBase.LIGHT_MODE_AMBIENT_INTENSITY;
        if (mArConfigBase == null) {
            return curLightMode;
        }
        try {
            curLightMode = mArConfigBase.getLightingMode();
        } catch (ARUnavailableServiceApkTooOldException exception) {
            LogUtil.warn(TAG, "getCurrentLightMode catch ARUnavailableServiceApkTooOldException.");
        }
        return curLightMode;
    }

    @Override
    protected void onResume() {
        LogUtil.debug(TAG, "onResume");
        super.onResume();
        if (mArSession == null) {
            try {
                mArSession = new ARSession(this.getApplicationContext());
                mArConfigBase = new ARWorldTrackingConfig(mArSession);
                refreshConfig(ARConfigBase.LIGHT_MODE_NONE);
            } catch (Exception capturedException) {
                setMessageWhenError(capturedException);
            }

            if (errorMessage != null) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                stopArSession();
                return;
            }
        }

        sessionResume(mWorldRendererManager);
    }

    private void refreshConfig(int lightingMode) {
        if (mArConfigBase == null || mArSession == null) {
            return;
        }
        try {
            mArConfigBase.setFocusMode(ARConfigBase.FocusMode.AUTO_FOCUS);
            mArConfigBase.setSemanticMode(ARWorldTrackingConfig.SEMANTIC_PLANE | ARWorldTrackingConfig.SEMANTIC_TARGET);
            mArConfigBase.setLightingMode(lightingMode);
            mArSession.configure(mArConfigBase);
        } catch (ARUnavailableServiceApkTooOldException capturedException) {
            setMessageWhenError(capturedException);
        } finally {
            showEffectiveConfigInfo(lightingMode);
        }
        mWorldRendererManager.setArWorldTrackingConfig(mArConfigBase);
    }

    /**
     * For HUAWEI AR Engine 3.18 or later versions, you can call configure of ARSession, then call getLightingMode to
     * check whether the current device supports ambient light detection.
     *
     * @param lightingMode Lighting estimate mode enabled.
     */
    private void showEffectiveConfigInfo(int lightingMode) {
        if (mArConfigBase == null || mArSession == null) {
            LogUtil.warn(TAG, "showEffectiveConfigInfo params invalid.");
            return;
        }
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
        if (((lightingMode & ARConfigBase.LIGHT_MODE_ENVIRONMENT_LIGHTING) != 0)
            && ((mArConfigBase.getLightingMode() & ARConfigBase.LIGHT_MODE_ENVIRONMENT_LIGHTING) == 0)) {
            toastMsg += "The running environment does not support LIGHT_MODE_ENVIRONMENT_LIGHTING.";
        }
        long maxMapSize = getMaxMapSize();
        if (maxMapSize != 0 && maxMapSize != DEFAULT_MAX_MAP_SIZE) {
            toastMsg += "Config max map size:" + maxMapSize;
        }
        if (!toastMsg.isEmpty()) {
            Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        LogUtil.info(TAG, "onDestroy start.");
        if (mWorldRendererManager != null) {
            mWorldRendererManager.releaseARAnchor();
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        super.onDestroy();
        LogUtil.info(TAG, "onDestroy end.");
    }
}
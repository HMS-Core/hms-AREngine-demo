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

package com.huawei.arengine.demos.java.scenemesh;

import android.app.Activity;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.huawei.arengine.demos.R;
import com.huawei.arengine.demos.common.DisplayRotationManager;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.arengine.demos.common.PermissionManager;
import com.huawei.arengine.demos.java.scenemesh.rendering.SceneMeshRenderManager;
import com.huawei.hiar.ARConfigBase;
import com.huawei.hiar.AREnginesApk;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.ARWorldTrackingConfig;
import com.huawei.hiar.exceptions.ARCameraNotAvailableException;
import com.huawei.hiar.exceptions.ARUnSupportedConfigurationException;
import com.huawei.hiar.exceptions.ARUnavailableClientSdkTooOldException;
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException;
import com.huawei.hiar.exceptions.ARUnavailableServiceNotInstalledException;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * 本示例演示了AR Engine中网格场景的使用方法。
 *
 * @author HW
 * @since 2021-01-25
 */
public class SceneMeshActivity extends Activity {
    private static final String TAG = SceneMeshActivity.class.getSimpleName();

    private static final int CONFIG_CHOOSER_RED_SIZE = 8;

    private static final int CONFIG_CHOOSER_GREEN_SIZE = 8;

    private static final int CONFIG_CHOOSER_BLUE_SIZE = 8;

    private static final int CONFIG_CHOOSER_ALPHA_SIZE = 8;

    private static final int CONFIG_CHOOSER_DEPTH_SIZE = 16;

    private static final int CONFIG_CHOOSER_STENCIL_SIZE = 0;

    private static final int OPENGLES_VERSION = 2;

    private static final int BLOCK_QUEUE_CAPACITY = 2;

    private ARSession mArSession;

    private GLSurfaceView mSurfaceView;

    private SceneMeshRenderManager mSceneMeshRenderManager;

    private DisplayRotationManager mDisplayRotationManager;

    private ArrayBlockingQueue<MotionEvent> mQueuedSingleTaps = new ArrayBlockingQueue<>(BLOCK_QUEUE_CAPACITY);

    private GestureDetector mGestureDetector;

    private boolean isRemindInstall = false;

    private String errorMessage = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scenemesh_activity_main);
        mSurfaceView = findViewById(R.id.surfaceview);
        mDisplayRotationManager = new DisplayRotationManager(this);

        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent event) {
                onSingleTap(event);
                return true;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
        });

        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                LogUtil.debug(TAG, "setOnTouchListener, mSurfaceView get touch");
                return mGestureDetector.onTouchEvent(event);
            }
        });

        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(OPENGLES_VERSION); // Set the version of openGLES.
        mSurfaceView.setEGLConfigChooser(CONFIG_CHOOSER_RED_SIZE, CONFIG_CHOOSER_GREEN_SIZE, CONFIG_CHOOSER_BLUE_SIZE,
            CONFIG_CHOOSER_ALPHA_SIZE, CONFIG_CHOOSER_DEPTH_SIZE,
            CONFIG_CHOOSER_STENCIL_SIZE); // Alpha used for plane blending.

        mSceneMeshRenderManager = new SceneMeshRenderManager(this, this);
        mSceneMeshRenderManager.setDisplayRotationManage(mDisplayRotationManager);
        mSceneMeshRenderManager.setQueuedSingleTaps(mQueuedSingleTaps);

        mSurfaceView.setRenderer(mSceneMeshRenderManager);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    /**
     * 点击事件记录方法，将点击事件存入列表。
     *
     * @param event 点击事件。
     */
    private void onSingleTap(MotionEvent event) {
        LogUtil.debug(TAG, "onSingleTap, add MotionEvent to mQueuedSingleTaps" + event.toString());
        boolean result = mQueuedSingleTaps.offer(event);
        if (!result) {
            LogUtil.error(TAG, "The message queue is full. No more messages can be added.");
        }
    }

    @Override
    protected void onResume() {
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
                ARConfigBase config = new ARWorldTrackingConfig(mArSession);
                config.setFocusMode(ARConfigBase.FocusMode.AUTO_FOCUS);
                config.setEnableItem(ARConfigBase.ENABLE_MESH | ARConfigBase.ENABLE_DEPTH);
                mArSession.configure(config);
                mSceneMeshRenderManager.setArSession(mArSession);

                // 检测当前手机相机是否为深度相机。
                if ((config.getEnableItem() & ARConfigBase.ENABLE_MESH) == 0) {
                    findViewById(R.id.scene_mesh_searchingTextView).setVisibility(View.GONE);
                    throw new ARUnSupportedConfigurationException();
                }
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
            mDisplayRotationManager.registerDisplayListener();
        } catch (ARCameraNotAvailableException e) {
            Toast.makeText(this, "Camera open failed, please restart the app", Toast.LENGTH_LONG).show();
            mArSession = null;
            return;
        }
        mSurfaceView.onResume();
        mDisplayRotationManager.registerDisplayListener();
    }

    /**
     * 输入捕获的异常项，并输出相应的异常信息。
     *
     * @param catchException 捕获到的异常。
     */
    private void setMessageWhenError(Exception catchException) {
        if (catchException instanceof ARUnavailableServiceNotInstalledException) {
            startActivity(new Intent(this, com.huawei.arengine.demos.common.ConnectAppMarketActivity.class));
        } else if (catchException instanceof ARUnavailableServiceApkTooOldException) {
            errorMessage = "Please update HuaweiARService.apk";
        } else if (catchException instanceof ARUnavailableClientSdkTooOldException) {
            errorMessage = "Please update this app";
        } else if (catchException instanceof ARUnSupportedConfigurationException) {
            errorMessage = "The configuration is not supported by the device!";
        } else {
            errorMessage = "unknown exception throws!";
        }
    }

    /**
     * 检查当前设备是否安装AR Engine引擎(com.huawei.arengine.service)。
     * 如果没有，请重定向到华为应用市场进行安装。
     *
     * @return Boolean 如果当前设备安装了AR Engine引擎，返回true，否则返回false。
     */
    private boolean arEngineAbilityCheck() {
        boolean isInstallArEngineApk = AREnginesApk.isAREngineApkReady(this);
        if (!isInstallArEngineApk && isRemindInstall) {
            Toast.makeText(this, "Please agree to install.", Toast.LENGTH_LONG).show();
            finish();
        }
        LogUtil.debug(TAG, "Is install AREngine apk: " + isInstallArEngineApk);
        if (!isInstallArEngineApk) {
            startActivity(new Intent(this, com.huawei.arengine.demos.common.ConnectAppMarketActivity.class));
            isRemindInstall = true;
        }
        return AREnginesApk.isAREngineApkReady(this);
    }

    /**
     * 停止AR Session。
     */
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
        super.onPause();
        LogUtil.info(TAG, "onPause start.");
        if (mArSession != null) {
            mSurfaceView.onPause();
            mArSession.pause();
        }

        if (mArSession != null) {
            mDisplayRotationManager.unregisterDisplayListener();
            mSurfaceView.onPause();
            mArSession.pause();
            LogUtil.info(TAG, "Session paused!");
        }
        LogUtil.info(TAG, "onPause end.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtil.info(TAG, "onDestroy start.");
        if (mArSession != null) {
            mArSession.stop();
            mArSession = null;
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
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }
}

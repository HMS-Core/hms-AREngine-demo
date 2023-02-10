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

package com.huawei.arengine.demos.java.scenemesh;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.huawei.arengine.demos.R;
import com.huawei.arengine.demos.common.BaseActivity;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.arengine.demos.java.scenemesh.rendering.SceneMeshRendererManager;
import com.huawei.hiar.ARConfigBase;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.ARWorldTrackingConfig;
import com.huawei.hiar.exceptions.ARUnSupportedConfigurationException;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * The following example demonstrates how to use the grid in AR Engine.
 *
 * @author HW
 * @since 2021-01-25
 */
public class SceneMeshActivity extends BaseActivity {
    private static final String TAG = SceneMeshActivity.class.getSimpleName();

    private static final int CONFIG_CHOOSER_RED_SIZE = 8;

    private static final int CONFIG_CHOOSER_GREEN_SIZE = 8;

    private static final int CONFIG_CHOOSER_BLUE_SIZE = 8;

    private static final int CONFIG_CHOOSER_ALPHA_SIZE = 8;

    private static final int CONFIG_CHOOSER_DEPTH_SIZE = 16;

    private static final int CONFIG_CHOOSER_STENCIL_SIZE = 0;

    private static final int OPENGLES_VERSION = 2;

    private static final int BLOCK_QUEUE_CAPACITY = 2;

    private SceneMeshRendererManager mSceneMeshRendererManager;

    private ArrayBlockingQueue<MotionEvent> mQueuedSingleTaps = new ArrayBlockingQueue<>(BLOCK_QUEUE_CAPACITY);

    private GestureDetector mGestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scenemesh_activity_main);
        mSurfaceView = findViewById(R.id.surfaceview);

        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent event) {
                onSingleTap(event);
                return true;
            }

            @Override
            public boolean onDown(MotionEvent event) {
                return true;
            }
        });

        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                LogUtil.debug(TAG, "setOnTouchListener, mSurfaceView get touch");
                return mGestureDetector.onTouchEvent(event);
            }
        });

        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(OPENGLES_VERSION); // Set the version of openGLES.
        mSurfaceView.setEGLConfigChooser(CONFIG_CHOOSER_RED_SIZE, CONFIG_CHOOSER_GREEN_SIZE, CONFIG_CHOOSER_BLUE_SIZE,
            CONFIG_CHOOSER_ALPHA_SIZE, CONFIG_CHOOSER_DEPTH_SIZE,
            CONFIG_CHOOSER_STENCIL_SIZE); // Alpha used for plane blending.

        mSceneMeshRendererManager = new SceneMeshRendererManager(this);
        mSceneMeshRendererManager.setDisplayRotationManager(mDisplayRotationManager);
        TextView textView = findViewById(R.id.fpsTextView);
        mSceneMeshRendererManager.setTextView(textView);
        mSceneMeshRendererManager.setQueuedSingleTaps(mQueuedSingleTaps);

        mSurfaceView.setRenderer(mSceneMeshRendererManager);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    /**
     * Click event recording method, which saves the click event to the list.
     *
     * @param event event Click event.
     */
    private void onSingleTap(MotionEvent event) {
        LogUtil.debug(TAG, "onSingleTap, add MotionEvent to mQueuedSingleTaps" + event.toString());
        boolean isSuccess = mQueuedSingleTaps.offer(event);
        if (!isSuccess) {
            LogUtil.error(TAG, "The message queue is full. No more messages can be added.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mArSession == null) {
            try {
                mArSession = new ARSession(this.getApplicationContext());
                mArConfigBase = new ARWorldTrackingConfig(mArSession);
                mArConfigBase.setFocusMode(ARConfigBase.FocusMode.AUTO_FOCUS);
                mArConfigBase.setEnableItem(ARConfigBase.ENABLE_MESH | ARConfigBase.ENABLE_DEPTH);
                mArSession.configure(mArConfigBase);

                // Detect whether the current mobile phone camera is a depth camera.
                if ((mArConfigBase.getEnableItem() & ARConfigBase.ENABLE_MESH) == 0) {
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
        sessionResume(mSceneMeshRendererManager);
    }
}

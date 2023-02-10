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

package com.huawei.arengine.demos.java.scenemesh.rendering;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.huawei.arengine.demos.R;
import com.huawei.arengine.demos.common.ArDemoRuntimeException;
import com.huawei.arengine.demos.common.BaseRendererManager;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.hiar.ARTrackable;

import java.util.concurrent.ArrayBlockingQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Provides rendering manager related to external scenes, including virtual object rendering management.
 *
 * @author hw
 * @since 2021-01-25
 */
public class SceneMeshRendererManager extends BaseRendererManager implements BaseRendererManager.BaseRenderer {
    private static final String TAG = SceneMeshRendererManager.class.getSimpleName();

    private TextView mSearchingTextView;

    private SceneMeshDisplay mSceneMesh = new SceneMeshDisplay();

    private HitResultDisplay mHitResultDisplay = new HitResultDisplay();

    /**
     * Scene grid rendering class, which creates the shader for updating grid data and performing rendering.
     *
     * @param activity Activity
     */
    public SceneMeshRendererManager(Activity activity) {
        mActivity = activity;
        mSearchingTextView = activity.findViewById(R.id.scene_mesh_searchingTextView);
        setRenderer(this);
    }

    @Override
    public void surfaceCreated(GL10 gl, EGLConfig config) {
        mHitResultDisplay.init(mActivity);
        mSceneMesh.init(mActivity);
    }

    @Override
    public void surfaceChanged(GL10 gl, int width, int height) {
    }

    @Override
    public void drawFrame(GL10 gl) {
        mTextDisplay.onDrawFrame(String.valueOf(doFpsCalculate()));
        try {
            if (mArCamera.getTrackingState() == ARTrackable.TrackingState.PAUSED) {
                LogUtil.debug(TAG, "Camera TrackingState Paused: ");
                showSearchingMessage(View.VISIBLE);
                return;
            }
            showSearchingMessage(View.GONE);

            // Draw a grid.
            mSceneMesh.onDrawFrame(mArFrame, mViewMatrix, mProjectionMatrix);

            // Process the click event and add a virtual model.
            mHitResultDisplay.onDrawFrame(mArFrame, mViewMatrix, mProjectionMatrix);
        } catch (ArDemoRuntimeException e) {
            LogUtil.error(TAG, "Exception on the ArDemoRuntimeException!");
        } catch (Throwable t) {
            // Prevent apps from crashing due to unprocessed exceptions.
            LogUtil.error(TAG, "Exception on the OpenGL thread." + t.getClass());
        }
    }

    /**
     * Set a gesture type queue.
     *
     * @param queuedSingleTaps Gesture type queue.
     */
    public void setQueuedSingleTaps(ArrayBlockingQueue<MotionEvent> queuedSingleTaps) {
        if (queuedSingleTaps == null) {
            LogUtil.error(TAG, "setQueuedSingleTaps, queuedSingleTaps is null!");
            return;
        }
        if (mHitResultDisplay == null) {
            LogUtil.error(TAG, "setQueuedSingleTaps, mHitResultDisplay is null!");
            return;
        }
        mHitResultDisplay.setQueuedSingleTaps(queuedSingleTaps);
    }

    private void showSearchingMessage(final int state) {
        final int viewState = state;
        if (mSearchingTextView == null) {
            return;
        }
        if (mSearchingTextView.getVisibility() != viewState) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mSearchingTextView.setVisibility(viewState);
                }
            });
        }
    }
}

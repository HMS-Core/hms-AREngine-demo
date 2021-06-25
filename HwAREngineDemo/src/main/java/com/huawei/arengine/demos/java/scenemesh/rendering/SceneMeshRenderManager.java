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

package com.huawei.arengine.demos.java.scenemesh.rendering;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.huawei.arengine.demos.R;
import com.huawei.arengine.demos.common.ArDemoRuntimeException;
import com.huawei.arengine.demos.common.DisplayRotationManager;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.arengine.demos.common.TextureDisplay;
import com.huawei.hiar.ARCamera;
import com.huawei.hiar.ARFrame;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.ARTrackable;

import java.util.concurrent.ArrayBlockingQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 提供与外界场景相关的渲染管理，包括虚拟对象渲染管理。
 *
 * @author hw
 * @since 2021-01-25
 */
public class SceneMeshRenderManager implements GLSurfaceView.Renderer {
    private static final String TAG = SceneMeshRenderManager.class.getSimpleName();

    private static final float FPS_TEXT_SIZE = 10f;

    private static final int PROJ_MATRIX_OFFSET = 0;

    private static final float PROJ_MATRIX_NEAR = 0.1f;

    private static final float PROJ_MATRIX_FAR = 100.0f;

    private Activity mActivity;

    private Context mContext;

    private TextView mTextView;

    private TextView mSearchingTextView;

    private ARSession mArSession;

    private DisplayRotationManager mDisplayRotationManager;

    private float updateInterval = 0.5f;

    private long lastInterval;

    private int frames = 0;

    private float fps;

    private TextureDisplay mBackgroundDisplay = new TextureDisplay();

    private SceneMeshDisplay mSceneMesh = new SceneMeshDisplay();

    private HitResultDisplay mHitResultDisplay = new HitResultDisplay();

    /**
     * 场景网格渲染类，包括创建着色器以更新网格数据和渲染。
     *
     * @param activity 活动。
     * @param context 上下文信息。
     */
    public SceneMeshRenderManager(Activity activity, Context context) {
        mActivity = activity;
        mContext = context;
        mTextView = activity.findViewById(R.id.fpsTextView);
        mSearchingTextView = activity.findViewById(R.id.scene_mesh_searchingTextView);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // 设置窗口颜色。
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        mBackgroundDisplay.init();
        mHitResultDisplay.init(mContext);
        mSceneMesh.init(mContext);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        mBackgroundDisplay.onSurfaceChanged(width, height);
        mDisplayRotationManager.updateViewportRotation(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        showFpsTextView(String.valueOf(doFpsCalculate()));
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        if (mArSession == null) {
            return;
        }
        if (mDisplayRotationManager.getDeviceRotation()) {
            mDisplayRotationManager.updateArSessionDisplayGeometry(mArSession);
        }
        try {
            mArSession.setCameraTextureName(mBackgroundDisplay.getExternalTextureId());
            ARFrame arFrame = mArSession.update();
            ARCamera arCamera = arFrame.getCamera();
            mBackgroundDisplay.onDrawFrame(arFrame);
            if (arCamera.getTrackingState() == ARTrackable.TrackingState.PAUSED) {
                LogUtil.debug(TAG, "Camera TrackingState Paused: ");
                showSearchingMessage(View.VISIBLE);
                return;
            }
            float[] projmtxs = new float[16];
            arCamera.getProjectionMatrix(projmtxs, PROJ_MATRIX_OFFSET, PROJ_MATRIX_NEAR, PROJ_MATRIX_FAR);

            float[] viewmtxs = new float[16];
            arCamera.getViewMatrix(viewmtxs, 0);
            showSearchingMessage(View.GONE);

            // 网格绘制。
            mSceneMesh.onDrawFrame(arFrame, viewmtxs, projmtxs);

            // 处理点击事件，添加虚拟模型。
            mHitResultDisplay.onDrawFrame(arFrame, viewmtxs, projmtxs);
        } catch (ArDemoRuntimeException e) {
            LogUtil.error(TAG, "Exception on the ArDemoRuntimeException!");
        } catch (Throwable t) {
            // 防止应用程序因未处理的异常而崩溃。
            LogUtil.error(TAG, "Exception on the OpenGL thread.");
        }
    }

    /**
     * 设置“AR Session”，将更新并获取“On Draw Frame”中的最新数据。
     *
     * @param arSession AR会话。
     */
    public void setArSession(ARSession arSession) {
        if (arSession == null) {
            LogUtil.error(TAG, "setSession error, arSession is null!");
            return;
        }
        mArSession = arSession;
    }

    /**
     * 设置“DisplayRotationManage”对象，该对象将在“onSurfaceChanged”和“onDrawFrame”中使用。
     *
     * @param displayRotationManager DisplayRotationManage为自定义对象。
     */
    public void setDisplayRotationManage(DisplayRotationManager displayRotationManager) {
        if (displayRotationManager == null) {
            LogUtil.error(TAG, "SetDisplayRotationManage error, displayRotationManage is null!");
            return;
        }
        mDisplayRotationManager = displayRotationManager;
    }

    /**
     * 设置手势类型队列。
     *
     * @param queuedSingleTaps 手势类型队列。
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

    /**
     * 展示文本视图。
     *
     * @param text 需要展示的文本。
     */
    private void showFpsTextView(final String text) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextView.setTextColor(Color.WHITE);

                // 设置屏幕显示的字体大小。
                mTextView.setTextSize(FPS_TEXT_SIZE);
                if (text != null) {
                    mTextView.setText(text);
                } else {
                    mTextView.setText("");
                }
            }
        });
    }

    float doFpsCalculate() {
        ++frames;
        long timeNow = System.currentTimeMillis();

        // 将毫秒转换为秒。
        if (((timeNow - lastInterval) / 1000.0f) > updateInterval) {
            fps = frames / ((timeNow - lastInterval) / 1000.0f);
            frames = 0;
            lastInterval = timeNow;
        }
        return fps;
    }
}

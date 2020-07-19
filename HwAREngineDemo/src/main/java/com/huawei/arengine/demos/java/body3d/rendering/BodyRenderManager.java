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

package com.huawei.arengine.demos.java.body3d.rendering;

import android.app.Activity;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.widget.TextView;

import com.huawei.arengine.demos.common.ArDemoRuntimeException;
import com.huawei.arengine.demos.common.DisplayRotationManager;
import com.huawei.arengine.demos.common.TextDisplay;
import com.huawei.arengine.demos.common.TextureDisplay;
import com.huawei.hiar.ARBody;
import com.huawei.hiar.ARCamera;
import com.huawei.hiar.ARFrame;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.ARTrackable;

import java.util.ArrayList;
import java.util.Collection;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * This class renders personal data obtained by the AR Engine.
 *
 * @author HW
 * @since 2020-03-21
 */
public class BodyRenderManager implements GLSurfaceView.Renderer {
    private static final String TAG = BodyRenderManager.class.getSimpleName();

    private static final int PROJECTION_MATRIX_OFFSET = 0;

    private static final float PROJECTION_MATRIX_NEAR = 0.1f;

    private static final float PROJECTION_MATRIX_FAR = 100.0f;

    private static final float UPDATE_INTERVAL = 0.5f;

    private int frames = 0;

    private long lastInterval;

    private ARSession mSession;

    private float fps;

    private Activity mActivity;

    private TextView mTextView;

    private TextureDisplay mTextureDisplay = new TextureDisplay();

    private TextDisplay mTextDisplay = new TextDisplay();

    private ArrayList<BodyRelatedDisplay> mBodyRelatedDisplays = new ArrayList<>();

    private DisplayRotationManager mDisplayRotationManager;

    /**
     * The constructor passes activity.
     * This method will be called when {@link Activity#onCreate}.
     *
     * @param activity Activity
     */
    public BodyRenderManager(Activity activity) {
        mActivity = activity;
        BodyRelatedDisplay bodySkeletonDisplay = new BodySkeletonDisplay();
        BodyRelatedDisplay bodySkeletonLineDisplay = new BodySkeletonLineDisplay();
        mBodyRelatedDisplays.add(bodySkeletonDisplay);
        mBodyRelatedDisplays.add(bodySkeletonLineDisplay);
    }

    /**
     * Set the AR session to be updated in onDrawFrame to obtain the latest data.
     *
     * @param arSession ARSession.
     */
    public void setArSession(ARSession arSession) {
        if (arSession == null) {
            Log.e(TAG, "Set session error, arSession is null!");
            return;
        }
        mSession = arSession;
    }

    /**
     * Set displayRotationManage, which is used in onSurfaceChanged and onDrawFrame.
     *
     * @param displayRotationManager DisplayRotationManage.
     */
    public void setDisplayRotationManage(DisplayRotationManager displayRotationManager) {
        if (displayRotationManager == null) {
            Log.e(TAG, "Set display rotation manage error, display rotation manage is null!");
            return;
        }
        mDisplayRotationManager = displayRotationManager;
    }

    /**
     * Set TextView, which is called in the UI thread to display data correctly.
     *
     * @param textView TextView.
     */
    public void setTextView(TextView textView) {
        if (textView == null) {
            Log.e(TAG, "Set textView error, text view is null!");
            return;
        }
        mTextView = textView;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Clear the color and set the window color.
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        for (BodyRelatedDisplay bodyRelatedDisplay : mBodyRelatedDisplays) {
            bodyRelatedDisplay.init();
        }
        mTextureDisplay.init();
        mTextDisplay.setListener(new TextDisplay.OnTextInfoChangeListener() {
            @Override
            public void textInfoChanged(String text, float positionX, float positionY) {
                showBodyTypeTextView(text, positionX, positionY);
            }
        });
    }

    /**
     * The OpenGL thread calls back the UI thread to display text.
     *
     * @param text Gesture information displayed on the screen
     * @param positionX The left padding in pixels.
     * @param positionY The right padding in pixels.
     */
    private void showBodyTypeTextView(final String text, final float positionX, final float positionY) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextView.setTextColor(Color.WHITE);

                // Set the font size.
                mTextView.setTextSize(10f);
                if (text != null) {
                    mTextView.setText(text);
                    mTextView.setPadding((int) positionX, (int) positionY, 0, 0);
                } else {
                    mTextView.setText("");
                }
            }
        });
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        mTextureDisplay.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
        mDisplayRotationManager.updateViewportRotation(width, height);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        // Clear the screen to notify the driver not to load pixels of the previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (mSession == null) {
            return;
        }
        if (mDisplayRotationManager.getDeviceRotation()) {
            mDisplayRotationManager.updateArSessionDisplayGeometry(mSession);
        }

        try {
            mSession.setCameraTextureName(mTextureDisplay.getExternalTextureId());
            ARFrame frame = mSession.update();

            // The size of the projection matrix is 4 * 4.
            float[] projectionMatrix = new float[16];
            ARCamera camera = frame.getCamera();

            // Obtain the projection matrix of ARCamera.
            camera.getProjectionMatrix(projectionMatrix, PROJECTION_MATRIX_OFFSET, PROJECTION_MATRIX_NEAR,
                PROJECTION_MATRIX_FAR);
            mTextureDisplay.onDrawFrame(frame);
            Collection<ARBody> bodies = mSession.getAllTrackables(ARBody.class);
            if (bodies.size() == 0) {
                mTextDisplay.onDrawFrame(null);
                return;
            }
            for (ARBody body : bodies) {
                if (body.getTrackingState() != ARTrackable.TrackingState.TRACKING) {
                    continue;
                }

                // Update the body recognition information to be displayed on the screen.
                StringBuilder sb = new StringBuilder();
                updateMessageData(sb, body);

                // Display the updated body information on the screen.
                mTextDisplay.onDrawFrame(sb);
            }
            for (BodyRelatedDisplay bodyRelatedDisplay : mBodyRelatedDisplays) {
                bodyRelatedDisplay.onDrawFrame(bodies, projectionMatrix);
            }
        } catch (ArDemoRuntimeException e) {
            Log.e(TAG, "Exception on the ArDemoRuntimeException!");
        } catch (Throwable t) {
            // This prevents the app from crashing due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread");
        }
    }

    /**
     * Update gesture-related data for display.
     *
     * @param sb String buffer.
     * @param body ARBody
     */
    private void updateMessageData(StringBuilder sb, ARBody body) {
        float fpsResult = doFpsCalculate();
        sb.append("FPS=").append(fpsResult).append(System.lineSeparator());
        int bodyAction = body.getBodyAction();
        sb.append("bodyAction=").append(bodyAction).append(System.lineSeparator());
    }

    private float doFpsCalculate() {
        ++frames;
        long timeNow = System.currentTimeMillis();

        // Convert millisecond to second.
        if (((timeNow - lastInterval) / 1000.0f) > UPDATE_INTERVAL) {
            fps = frames / ((timeNow - lastInterval) / 1000.0f);
            frames = 0;
            lastInterval = timeNow;
        }
        return fps;
    }
}
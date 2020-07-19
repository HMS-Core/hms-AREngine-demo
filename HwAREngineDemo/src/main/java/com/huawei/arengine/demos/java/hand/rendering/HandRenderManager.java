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

package com.huawei.arengine.demos.java.hand.rendering;

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
import com.huawei.hiar.ARCamera;
import com.huawei.hiar.ARFrame;
import com.huawei.hiar.ARHand;
import com.huawei.hiar.ARSession;

import java.util.ArrayList;
import java.util.Collection;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * This class shows how to render data obtained from HUAWEI AR Engine.
 *
 * @author HW
 * @since 2020-03-21
 */
public class HandRenderManager implements GLSurfaceView.Renderer {
    private static final String TAG = HandRenderManager.class.getSimpleName();

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

    private ArrayList<HandRelatedDisplay> mHandRelatedDisplays = new ArrayList<>();

    private DisplayRotationManager mDisplayRotationManager;

    /**
     * The constructor that passes context and activity. The method will be called when {@link Activity#onCreate}.
     *
     * @param activity Activity
     */
    public HandRenderManager(Activity activity) {
        mActivity = activity;
        HandRelatedDisplay handBoxDisplay = new HandBoxDisplay();
        HandRelatedDisplay mHandSkeletonDisplay = new HandSkeletonDisplay();
        HandRelatedDisplay mHandSkeletonLineDisplay = new HandSkeletonLineDisplay();
        mHandRelatedDisplays.add(handBoxDisplay);
        mHandRelatedDisplays.add(mHandSkeletonDisplay);
        mHandRelatedDisplays.add(mHandSkeletonLineDisplay);
    }

    /**
     * Set the ARSession object, which is used to obtain the latest data in the onDrawFrame method.
     *
     * @param arSession ARSession.
     */
    public void setArSession(ARSession arSession) {
        if (arSession == null) {
            Log.e(TAG, "set session error, arSession is null!");
            return;
        }
        mSession = arSession;
    }

    /**
     * Set the DisplayRotationManage object, which is used in onSurfaceChanged and onDrawFrame.
     *
     * @param displayRotationManager DisplayRotationManage.
     */
    public void setDisplayRotationManage(DisplayRotationManager displayRotationManager) {
        if (displayRotationManager == null) {
            Log.e(TAG, "SetDisplayRotationManage error, displayRotationManage is null!");
            return;
        }
        mDisplayRotationManager = displayRotationManager;
    }

    /**
     * Set the TextView object, which is called in the UI thread to display text.
     *
     * @param textView TextView.
     */
    public void setTextView(TextView textView) {
        if (textView == null) {
            Log.e(TAG, "Set text view error, textView is null!");
            return;
        }
        mTextView = textView;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Clear the original color and set a new color.
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        for (HandRelatedDisplay handRelatedDisplay : mHandRelatedDisplays) {
            handRelatedDisplay.init();
        }
        mTextureDisplay.init();
        mTextDisplay.setListener(new TextDisplay.OnTextInfoChangeListener() {
            @Override
            public void textInfoChanged(String text, float positionX, float positionY) {
                showHandTypeTextView(text, positionX, positionY);
            }
        });
    }

    /**
     * Create a text display thread that is used for text update tasks.
     *
     * @param text Gesture information displayed on the screen
     * @param positionX The left padding in pixels.
     * @param positionY The right padding in pixels.
     */
    private void showHandTypeTextView(final String text, final float positionX, final float positionY) {
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
        // Clear the color buffer and notify the driver not to load the data of the previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (mSession == null) {
            return;
        }
        if (mDisplayRotationManager.getDeviceRotation()) {
            mDisplayRotationManager.updateArSessionDisplayGeometry(mSession);
        }

        try {
            mSession.setCameraTextureName(mTextureDisplay.getExternalTextureId());
            ARFrame arFrame = mSession.update();
            ARCamera arCamera = arFrame.getCamera();

            // The size of the projection matrix is 4 * 4.
            float[] projectionMatrix = new float[16];

            // Obtain the projection matrix through ARCamera.
            arCamera.getProjectionMatrix(projectionMatrix, PROJECTION_MATRIX_OFFSET, PROJECTION_MATRIX_NEAR,
                PROJECTION_MATRIX_FAR);
            mTextureDisplay.onDrawFrame(arFrame);
            Collection<ARHand> hands = mSession.getAllTrackables(ARHand.class);
            if (hands.size() == 0) {
                mTextDisplay.onDrawFrame(null);
                return;
            }
            for (ARHand hand : hands) {
                // Update the hand recognition information to be displayed on the screen.
                StringBuilder sb = new StringBuilder();
                updateMessageData(sb, hand);

                // Display hand recognition information on the screen.
                mTextDisplay.onDrawFrame(sb);
            }
            for (HandRelatedDisplay handRelatedDisplay : mHandRelatedDisplays) {
                handRelatedDisplay.onDrawFrame(hands, projectionMatrix);
            }
        } catch (ArDemoRuntimeException e) {
            Log.e(TAG, "Exception on the ArDemoRuntimeException!");
        } catch (Throwable t) {
            // This prevents the app from crashing due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }

    /**
     * Update gesture-related information.
     *
     * @param sb String buffer.
     * @param hand ARHand.
     */
    private void updateMessageData(StringBuilder sb, ARHand hand) {
        float fpsResult = doFpsCalculate();
        sb.append("FPS=").append(fpsResult).append(System.lineSeparator());
        addHandNormalStringBuffer(sb, hand);
        addGestureActionStringBuffer(sb, hand);
        addGestureCenterStringBuffer(sb, hand);
        float[] gestureHandBoxPoints = hand.getGestureHandBox();

        sb.append("GestureHandBox length:[").append(gestureHandBoxPoints.length).append("]")
            .append(System.lineSeparator());
        for (int i = 0; i < gestureHandBoxPoints.length; i++) {
            Log.i(TAG, "gesturePoints:" + gestureHandBoxPoints[i]);
            sb.append("gesturePoints[").append(i).append("]:[").append(gestureHandBoxPoints[i]).append("]")
                .append(System.lineSeparator());
        }
        addHandSkeletonStringBuffer(sb, hand);
    }

    private void addHandNormalStringBuffer(StringBuilder sb, ARHand hand) {
        sb.append("GestureType=").append(hand.getGestureType()).append(System.lineSeparator());
        sb.append("GestureCoordinateSystem=").append(hand.getGestureCoordinateSystem()).append(System.lineSeparator());
        float[] gestureOrientation = hand.getGestureOrientation();
        sb.append("gestureOrientation length:[").append(gestureOrientation.length).append("]")
            .append(System.lineSeparator());
        for (int i = 0; i < gestureOrientation.length; i++) {
            Log.i(TAG, "gestureOrientation:" + gestureOrientation[i]);
            sb.append("gestureOrientation[").append(i).append("]:[").append(gestureOrientation[i])
                .append("]").append(System.lineSeparator());
        }
        sb.append(System.lineSeparator());
    }

    private void addGestureActionStringBuffer(StringBuilder sb, ARHand hand) {
        int[] gestureAction = hand.getGestureAction();
        sb.append("gestureAction length:[").append(gestureAction.length).append("]").append(System.lineSeparator());
        for (int i = 0; i < gestureAction.length; i++) {
            Log.i(TAG, "GestureAction:" + gestureAction[i]);
            sb.append("gestureAction[").append(i).append("]:[").append(gestureAction[i])
                .append("]").append(System.lineSeparator());
        }
        sb.append(System.lineSeparator());
    }

    private void addGestureCenterStringBuffer(StringBuilder sb, ARHand hand) {
        float[] gestureCenter = hand.getGestureCenter();
        sb.append("gestureCenter length:[").append(gestureCenter.length).append("]").append(System.lineSeparator());
        for (int i = 0; i < gestureCenter.length; i++) {
            Log.i(TAG, "GestureCenter:" + gestureCenter[i]);
            sb.append("gestureCenter[").append(i).append("]:[").append(gestureCenter[i])
                .append("]").append(System.lineSeparator());
        }
        sb.append(System.lineSeparator());
    }

    private void addHandSkeletonStringBuffer(StringBuilder sb, ARHand hand) {
        sb.append(System.lineSeparator()).append("Handtype=").append(hand.getHandtype())
            .append(System.lineSeparator());
        sb.append("SkeletonCoordinateSystem=").append(hand.getSkeletonCoordinateSystem());
        sb.append(System.lineSeparator());
        float[] skeletonArray = hand.getHandskeletonArray();
        sb.append("HandskeletonArray length:[").append(skeletonArray.length).append("]")
            .append(System.lineSeparator());
        Log.i(TAG, "SkeletonArray.length:" + skeletonArray.length);
        for (int i = 0; i < skeletonArray.length; i++) {
            Log.i(TAG, "SkeletonArray:" + skeletonArray[i]);
        }
        sb.append(System.lineSeparator());
        int[] handSkeletonConnection = hand.getHandSkeletonConnection();
        sb.append("HandSkeletonConnection length:[").append(handSkeletonConnection.length)
            .append("]").append(System.lineSeparator());
        Log.i(TAG, "handSkeletonConnection.length:" + handSkeletonConnection.length);
        for (int i = 0; i < handSkeletonConnection.length; i++) {
            Log.i(TAG, "handSkeletonConnection:" + handSkeletonConnection[i]);
        }
        sb.append(System.lineSeparator()).append("-----------------------------------------------------");
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

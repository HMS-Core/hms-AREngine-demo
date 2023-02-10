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

package com.huawei.arengine.demos.java.hand.rendering;

import android.app.Activity;

import com.huawei.arengine.demos.common.ArDemoRuntimeException;
import com.huawei.arengine.demos.common.BaseRendererManager;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.hiar.ARHand;
import com.huawei.hiar.exceptions.ARDeadlineExceededException;
import com.huawei.hiar.exceptions.ARFatalException;
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException;

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
public class HandRendererManager extends BaseRendererManager implements BaseRendererManager.BaseRenderer {
    private static final String TAG = HandRendererManager.class.getSimpleName();

    private ArrayList<HandRelatedDisplay> mHandRelatedDisplays = new ArrayList<>();

    /**
     * The constructor that passes context and activity. The method will be called when {@link Activity#onCreate}.
     *
     * @param activity Activity
     */
    public HandRendererManager(Activity activity) {
        mActivity = activity;
        HandRelatedDisplay handBoxDisplay = new HandBoxDisplay();
        HandRelatedDisplay mHandSkeletonDisplay = new HandSkeletonDisplay();
        HandRelatedDisplay mHandSkeletonLineDisplay = new HandSkeletonLineDisplay();
        mHandRelatedDisplays.add(handBoxDisplay);
        mHandRelatedDisplays.add(mHandSkeletonDisplay);
        mHandRelatedDisplays.add(mHandSkeletonLineDisplay);
        setRenderer(this);
    }

    @Override
    public void surfaceCreated(GL10 gl, EGLConfig config) {
        for (HandRelatedDisplay handRelatedDisplay : mHandRelatedDisplays) {
            handRelatedDisplay.init();
        }
    }

    @Override
    public void surfaceChanged(GL10 unused, int width, int height) {
    }

    @Override
    public void drawFrame(GL10 unused) {
        try {
            Collection<ARHand> hands = mSession.getAllTrackables(ARHand.class);
            if (hands.size() == 0) {
                mTextDisplay.onDrawFrame("");
                return;
            }
            for (ARHand hand : hands) {
                // Update the hand recognition information to be displayed on the screen.
                StringBuilder sb = new StringBuilder();
                updateMessageData(sb, hand);

                // Display hand recognition information on the screen.
                mTextDisplay.onDrawFrame(sb.toString());
            }
            for (HandRelatedDisplay handRelatedDisplay : mHandRelatedDisplays) {
                handRelatedDisplay.onDrawFrame(hands, mProjectionMatrix);
            }
        } catch (ArDemoRuntimeException e) {
            LogUtil.error(TAG, "Exception on the ArDemoRuntimeException!");
        } catch (ARFatalException | IllegalArgumentException | ARDeadlineExceededException |
            ARUnavailableServiceApkTooOldException t) {
            // This prevents the app from crashing due to unhandled exceptions.
            LogUtil.error(TAG, "Exception on the OpenGL thread");
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

        sb.append("GestureHandBox length:[")
            .append(gestureHandBoxPoints.length)
            .append("]")
            .append(System.lineSeparator());
        for (int i = 0; i < gestureHandBoxPoints.length; i++) {
            LogUtil.info(TAG, "gesturePoints:" + gestureHandBoxPoints[i]);
            sb.append("gesturePoints[")
                .append(i)
                .append("]:[")
                .append(gestureHandBoxPoints[i])
                .append("]")
                .append(System.lineSeparator());
        }
        addHandSkeletonStringBuffer(sb, hand);
    }

    private void addHandNormalStringBuffer(StringBuilder sb, ARHand hand) {
        sb.append("GestureType=").append(hand.getGestureType()).append(System.lineSeparator());
        sb.append("GestureCoordinateSystem=").append(hand.getGestureCoordinateSystem()).append(System.lineSeparator());
        float[] gestureOrientation = hand.getGestureOrientation();
        sb.append("gestureOrientation length:[")
            .append(gestureOrientation.length)
            .append("]")
            .append(System.lineSeparator());
        for (int i = 0; i < gestureOrientation.length; i++) {
            LogUtil.info(TAG, "gestureOrientation:" + gestureOrientation[i]);
            sb.append("gestureOrientation[")
                .append(i)
                .append("]:[")
                .append(gestureOrientation[i])
                .append("]")
                .append(System.lineSeparator());
        }
        sb.append(System.lineSeparator());
    }

    private void addGestureActionStringBuffer(StringBuilder sb, ARHand hand) {
        int[] gestureAction = hand.getGestureAction();
        sb.append("gestureAction length:[").append(gestureAction.length).append("]").append(System.lineSeparator());
        for (int i = 0; i < gestureAction.length; i++) {
            LogUtil.info(TAG, "GestureAction:" + gestureAction[i]);
            sb.append("gestureAction[")
                .append(i)
                .append("]:[")
                .append(gestureAction[i])
                .append("]")
                .append(System.lineSeparator());
        }
        sb.append(System.lineSeparator());
    }

    private void addGestureCenterStringBuffer(StringBuilder sb, ARHand hand) {
        float[] gestureCenter = hand.getGestureCenter();
        sb.append("gestureCenter length:[").append(gestureCenter.length).append("]").append(System.lineSeparator());
        for (int i = 0; i < gestureCenter.length; i++) {
            LogUtil.info(TAG, "GestureCenter:" + gestureCenter[i]);
            sb.append("gestureCenter[")
                .append(i)
                .append("]:[")
                .append(gestureCenter[i])
                .append("]")
                .append(System.lineSeparator());
        }
        sb.append(System.lineSeparator());
    }

    private void addHandSkeletonStringBuffer(StringBuilder sb, ARHand hand) {
        sb.append(System.lineSeparator()).append("Handtype=").append(hand.getHandtype()).append(System.lineSeparator());
        sb.append("SkeletonCoordinateSystem=").append(hand.getSkeletonCoordinateSystem());
        sb.append(System.lineSeparator());
        float[] skeletonArray = hand.getHandskeletonArray();
        sb.append("HandskeletonArray length:[").append(skeletonArray.length).append("]").append(System.lineSeparator());
        LogUtil.info(TAG, "SkeletonArray.length:" + skeletonArray.length);
        for (int i = 0; i < skeletonArray.length; i++) {
            LogUtil.info(TAG, "SkeletonArray:" + skeletonArray[i]);
        }
        sb.append(System.lineSeparator());
        int[] handSkeletonConnection = hand.getHandSkeletonConnection();
        sb.append("HandSkeletonConnection length:[")
            .append(handSkeletonConnection.length)
            .append("]")
            .append(System.lineSeparator());
        LogUtil.info(TAG, "handSkeletonConnection.length:" + handSkeletonConnection.length);
        for (int i = 0; i < handSkeletonConnection.length; i++) {
            LogUtil.info(TAG, "handSkeletonConnection:" + handSkeletonConnection[i]);
        }
        sb.append(System.lineSeparator()).append("-----------------------------------------------------");
    }
}

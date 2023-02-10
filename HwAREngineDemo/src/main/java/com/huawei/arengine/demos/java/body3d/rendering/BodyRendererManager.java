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

package com.huawei.arengine.demos.java.body3d.rendering;

import android.app.Activity;
import android.util.Size;

import com.huawei.arengine.demos.common.ArDemoRuntimeException;
import com.huawei.arengine.demos.common.BaseRendererManager;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.hiar.ARBody;
import com.huawei.hiar.ARTrackable;
import com.huawei.hiar.exceptions.ARDeadlineExceededException;
import com.huawei.hiar.exceptions.ARFatalException;
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException;

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
public class BodyRendererManager extends BaseRendererManager implements BaseRendererManager.BaseRenderer {
    private static final String TAG = BodyRendererManager.class.getSimpleName();

    private ArrayList<BodyRelatedDisplay> mBodyRelatedDisplays = new ArrayList<>();

    private boolean mIsWithMaskData = false;

    private ARBody mBody;

    /**
     * The constructor passes activity.
     * This method will be called when {@link Activity#onCreate}.
     *
     * @param activity Activity
     */
    public BodyRendererManager(Activity activity) {
        mActivity = activity;
        BodyRelatedDisplay bodySkeletonDisplay = new BodySkeletonDisplay();
        BodyRelatedDisplay bodySkeletonLineDisplay = new BodySkeletonLineDisplay();
        mBodyRelatedDisplays.add(bodySkeletonDisplay);
        mBodyRelatedDisplays.add(bodySkeletonLineDisplay);
        setRenderer(this);
        useDefaultBackGround(new BodyMaskDisplay());
    }

    /**
     * Human body confidence switch.
     *
     * @param isMaskEnable true: enable; false: disable
     */
    public void setBodyMask(boolean isMaskEnable) {
        mIsWithMaskData = isMaskEnable;
    }

    @Override
    public void surfaceCreated(GL10 gl, EGLConfig config) {
        for (BodyRelatedDisplay bodyRelatedDisplay : mBodyRelatedDisplays) {
            bodyRelatedDisplay.init();
        }
    }
    @Override
    public void surfaceChanged(GL10 unused, int width, int height) {
    }
    @Override
    public void drawFrame(GL10 unused) {
        try {
            Collection<ARBody> bodies = mSession.getAllTrackables(ARBody.class);
            boolean hasBodyTracking = false;
            for (ARBody body : bodies) {
                if (body.getTrackingState() != ARTrackable.TrackingState.TRACKING) {
                    continue;
                }

                mBody = body;
                hasBodyTracking = true;
            }

            // Update the body recognition information to be displayed on the screen.
            StringBuilder sb = new StringBuilder();
            updateMessageData(sb, mBody);
            Size textureSize = mSession.getCameraConfig().getTextureDimensions();
            if (mIsWithMaskData && hasBodyTracking && mBackgroundDisplay instanceof BodyMaskDisplay) {
                ((BodyMaskDisplay) mBackgroundDisplay).onDrawFrame(mArFrame, mBody.getMaskConfidence(),
                    textureSize.getWidth(), textureSize.getHeight());
            }

            // Display the updated body information on the screen.
            mTextDisplay.onDrawFrame(sb.toString());

            for (BodyRelatedDisplay bodyRelatedDisplay : mBodyRelatedDisplays) {
                bodyRelatedDisplay.onDrawFrame(bodies, mProjectionMatrix);
            }
        } catch (ArDemoRuntimeException e) {
            LogUtil.error(TAG, "Exception on the ArDemoRuntimeException!");
        } catch (ARFatalException | IllegalArgumentException | ARDeadlineExceededException |
            ARUnavailableServiceApkTooOldException t) {
            // This prevents the app from crashing due to unhandled exceptions.
            LogUtil.error(TAG, "Exception on the OpenGL thread, " + t.getClass());
        }
    }

    /**
     * Update gesture-related data for display.
     *
     * @param sb String buffer.
     * @param body ARBody
     */
    private void updateMessageData(StringBuilder sb, ARBody body) {
        if (body == null) {
            return;
        }
        float fpsResult = doFpsCalculate();
        sb.append("FPS=").append(fpsResult).append(System.lineSeparator());
        int bodyAction = body.getBodyAction();
        sb.append("bodyAction=").append(bodyAction).append(System.lineSeparator());
    }
}
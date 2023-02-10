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

package com.huawei.arengine.demos.java.instant.rendering;

import android.app.Activity;
import android.widget.SeekBar;

import com.huawei.arengine.demos.R;
import com.huawei.arengine.demos.common.ArDemoRuntimeException;
import com.huawei.arengine.demos.common.BaseRendererManager;
import com.huawei.arengine.demos.common.GestureEvent;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.arengine.demos.common.ObjectDisplay;
import com.huawei.arengine.demos.common.VirtualObject;
import com.huawei.arengine.demos.java.utils.CommonUtil;
import com.huawei.hiar.ARCamera;
import com.huawei.hiar.ARFrame;
import com.huawei.hiar.ARLightEstimate;
import com.huawei.hiar.ARTrackable;
import com.huawei.hiar.exceptions.ARFatalException;
import com.huawei.hiar.exceptions.ARSessionPausedException;

import java.util.concurrent.ArrayBlockingQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * This class provides rendering management related to the instant scene, including
 * label rendering and virtual object rendering management.
 *
 * @author HW
 * @since 2022-09-15
 */
public class InstantRendererManager extends BaseRendererManager implements BaseRendererManager.BaseRenderer {
    private static final String TAG = "InstantRendererManager";

    private static final float[] GREEN_COLORS = new float[] {66.0f, 244.0f, 133.0f, 255.0f};

    private ObjectDisplay mObjectDisplay = new ObjectDisplay();

    private ArrayBlockingQueue<GestureEvent> mQueuedSingleTaps;

    private VirtualObject mSelectedObj = null;

    private VirtualObject mVirtualObject = null;

    private SeekBar mScaleSeekBar = null;

    private SeekBar mRotationSeekBar = null;

    /**
     * The constructor passes activity.
     *
     * @param activity Activity.
     */
    public InstantRendererManager(Activity activity) {
        mActivity = activity;
        setRenderer(this);
        initSeekBar(activity);
    }

    /**
     * Set a gesture type queue.
     *
     * @param queuedSingleTaps Gesture type queue.
     */
    public void setQueuedSingleTaps(ArrayBlockingQueue<GestureEvent> queuedSingleTaps) {
        if (queuedSingleTaps == null) {
            LogUtil.error(TAG, "setQueuedSingleTaps error, queuedSingleTaps is null!");
            return;
        }
        mQueuedSingleTaps = queuedSingleTaps;
    }

    @Override
    public void surfaceCreated(GL10 gl, EGLConfig config) {
        mObjectDisplay.init(mActivity);
    }

    @Override
    public void surfaceChanged(GL10 unused, int width, int height) {
        mObjectDisplay.setSize(width, height);
    }

    @Override
    public void drawFrame(GL10 unused) {
        try {
            updateInstantObjPose();
            StringBuilder sb = new StringBuilder();
            updateMessageData(sb);
            mTextDisplay.onDrawFrame(sb.toString());
            handleGestureEvent(mArFrame, mArCamera, mProjectionMatrix, mViewMatrix);
            ARLightEstimate lightEstimate = mArFrame.getLightEstimate();
            float lightPixelIntensity = 1.0f;
            if (lightEstimate.getState() != ARLightEstimate.State.NOT_VALID) {
                lightPixelIntensity = lightEstimate.getPixelIntensity();
            }
            drawAllObjects(mProjectionMatrix, mViewMatrix, lightPixelIntensity);
        } catch (ArDemoRuntimeException e) {
            LogUtil.error(TAG, "Exception on the ArDemoRuntimeException!");
        } catch (ARFatalException | ARSessionPausedException t) {
            LogUtil.error(TAG, "Exception on the OpenGL thread. Name:" + t.getClass());
        }
    }

    private void updateInstantObjPose() {
        if (mSession == null) {
            return;
        }

        mSession.getAllAnchors().stream().forEach(anchor -> {
            if (mVirtualObject == null) {
                mVirtualObject = new VirtualObject(anchor.getPose(), GREEN_COLORS);
            } else {
                mVirtualObject.setArPose(anchor.getPose());
            }
        });
    }

    private void initSeekBar(Activity activity) {
        mScaleSeekBar = activity.findViewById(R.id.scaleSeekBar);
        mScaleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mSelectedObj != null) {
                    mSelectedObj.updateScaleFactor(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        mRotationSeekBar = activity.findViewById(R.id.rotationSeekBar);
        mRotationSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mSelectedObj != null) {
                    mSelectedObj.updateRotation(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void drawAllObjects(float[] projectionMatrix, float[] viewMatrix, float lightPixelIntensity) {
        if (mVirtualObject == null) {
            return;
        }
        mObjectDisplay.onDrawFrame(viewMatrix, projectionMatrix, lightPixelIntensity, mVirtualObject);
    }

    private void updateMessageData(StringBuilder sb) {
        float fpsResult = doFpsCalculate();
        sb.append("FPS = ").append(fpsResult).append(System.lineSeparator());
    }

    private void handleGestureEvent(ARFrame arFrame, ARCamera arCamera, float[] projectionMatrix, float[] viewMatrix) {
        GestureEvent event = mQueuedSingleTaps.poll();
        if (event == null) {
            return;
        }

        if (arCamera.getTrackingState() != ARTrackable.TrackingState.TRACKING) {
            LogUtil.warn(TAG, "Object is not tracking state.");
            return;
        }

        switch (event.getType()) {
            case GestureEvent.GESTURE_EVENT_TYPE_DOUBLETAP: {
                doWhenEventTypeDoubleTap(viewMatrix, projectionMatrix, event);
                break;
            }
            case GestureEvent.GESTURE_EVENT_TYPE_SCROLL: {
                if (mSelectedObj == null) {
                    LogUtil.info(TAG, "Selected object is null when instant scroll event.");
                    break;
                }
                CommonUtil.hitTest(arFrame, event.getEventSecond());
                break;
            }
            case GestureEvent.GESTURE_EVENT_TYPE_SINGLETAPCONFIRMED: {
                // Do not perform anything when an object is selected.
                if (mSelectedObj != null) {
                    mSelectedObj.setIsSelected(false);
                    mSelectedObj = null;
                }
                CommonUtil.hitTest(arFrame, event.getEventFirst());
                break;
            }
            default: {
                LogUtil.error(TAG, "Unknown motion event type, and do nothing.");
            }
        }
    }

    private void doWhenEventTypeDoubleTap(float[] viewMatrix, float[] projectionMatrix, GestureEvent event) {
        LogUtil.info(TAG, "Double tap event.");
        if (mSelectedObj != null) {
            mSelectedObj.setIsSelected(false);
            mSelectedObj = null;
        }

        if (mObjectDisplay.hitTest(viewMatrix, projectionMatrix, mVirtualObject, event.getEventFirst())) {
            mVirtualObject.setIsSelected(true);
            mSelectedObj = mVirtualObject;
        }
    }
}
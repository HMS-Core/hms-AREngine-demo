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

package com.huawei.arengine.demos.java.worldbody.rendering;

import android.app.Activity;
import android.view.MotionEvent;

import com.huawei.arengine.demos.common.ArDemoRuntimeException;
import com.huawei.arengine.demos.common.BaseRendererManager;
import com.huawei.arengine.demos.common.GestureEvent;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.arengine.demos.common.ObjectDisplay;
import com.huawei.arengine.demos.common.VirtualObject;
import com.huawei.arengine.demos.java.body3d.rendering.BodyRelatedDisplay;
import com.huawei.arengine.demos.java.body3d.rendering.BodySkeletonDisplay;
import com.huawei.arengine.demos.java.body3d.rendering.BodySkeletonLineDisplay;
import com.huawei.arengine.demos.java.world.rendering.PointCloudRenderer;
import com.huawei.hiar.ARBody;
import com.huawei.hiar.ARCamera;
import com.huawei.hiar.ARFrame;
import com.huawei.hiar.ARHitResult;
import com.huawei.hiar.ARPlane;
import com.huawei.hiar.ARPoint;
import com.huawei.hiar.ARPointCloud;
import com.huawei.hiar.ARTrackable;
import com.huawei.hiar.exceptions.ARDeadlineExceededException;
import com.huawei.hiar.exceptions.ARFatalException;
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * World body AR type use presentation.
 *
 * @author HW
 * @since 2021-03-22
 */
public class WorldBodyRendererManager extends BaseRendererManager implements BaseRendererManager.BaseRenderer {
    private static final String TAG = WorldBodyRendererManager.class.getSimpleName();

    private static final float[] BLUE_COLORS = new float[] {66.0f, 133.0f, 244.0f, 255.0f};

    private ObjectDisplay mObjectDisplay = new ObjectDisplay();

    private PointCloudRenderer mPointCloud = new PointCloudRenderer();

    private ArrayBlockingQueue<GestureEvent> mQueuedSingleTaps;

    private ArrayList<VirtualObject> mVirtualObjects = new ArrayList<>();

    private ArrayList<BodyRelatedDisplay> mBodyRelatedDisplays = new ArrayList<>();

    /**
     * The constructor passes context and activity. This method will be called when {@link Activity#onCreate}.
     *
     * @param activity Activity
     */
    public WorldBodyRendererManager(Activity activity) {
        mActivity = activity;
        BodyRelatedDisplay bodySkeletonDisplay = new BodySkeletonDisplay();
        BodyRelatedDisplay bodySkeletonLineDisplay = new BodySkeletonLineDisplay();
        mBodyRelatedDisplays.add(bodySkeletonDisplay);
        mBodyRelatedDisplays.add(bodySkeletonLineDisplay);
        setRenderer(this);
    }

    /**
     * Set a gesture type queue.
     *
     * @param queuedSingleTaps Gesture type queue.
     */
    public void setQueuedSingleTaps(ArrayBlockingQueue<GestureEvent> queuedSingleTaps) {
        if (queuedSingleTaps == null) {
            LogUtil.error(TAG, "setQueuedSingleTaps params is invalid!");
            return;
        }
        mQueuedSingleTaps = queuedSingleTaps;
    }

    @Override
    public void surfaceCreated(GL10 gl, EGLConfig config) {
        for (BodyRelatedDisplay bodyRelatedDisplay : mBodyRelatedDisplays) {
            bodyRelatedDisplay.init();
        }
        mObjectDisplay.init(mActivity);
        mPointCloud.init(mActivity);
    }

    @Override
    public void surfaceChanged(GL10 unused, int width, int height) {
        mObjectDisplay.setSize(width, height);
    }

    @Override
    public void drawFrame(GL10 unused) {
        try {
            handleGestureEvent(mArFrame, mArCamera);
            drawAllObjects(mProjectionMatrix, mViewMatrix);
            ARPointCloud arPointCloud = mArFrame.acquirePointCloud();
            mPointCloud.onDrawFrame(arPointCloud, mViewMatrix, mProjectionMatrix);
            Collection<ARBody> bodies = mSession.getAllTrackables(ARBody.class);

            StringBuilder sb = new StringBuilder();
            updateMessageData(sb, bodies);
            mTextDisplay.onDrawFrame(sb.toString());

            for (BodyRelatedDisplay bodyRelatedDisplay : mBodyRelatedDisplays) {
                bodyRelatedDisplay.onDrawFrame(bodies, mProjectionMatrix);
            }
            LogUtil.debug(TAG, "after worldBody display.");
        } catch (ArDemoRuntimeException e) {
            LogUtil.error(TAG, "Exception on the ArDemoRuntimeException!");
        } catch (ARFatalException | IllegalArgumentException | ARDeadlineExceededException |
            ARUnavailableServiceApkTooOldException t) {
            // This prevents the app from crashing due to unhandled exceptions.
            LogUtil.error(TAG, "Exception on the OpenGL thread");
        }
    }

    private void drawAllObjects(float[] projectionMatrix, float[] viewMatrix) {
        Iterator<VirtualObject> ite = mVirtualObjects.iterator();
        while (ite.hasNext()) {
            VirtualObject obj = ite.next();
            if (obj.getAnchor().getTrackingState() == ARTrackable.TrackingState.STOPPED) {
                ite.remove();
            }
            if (obj.getAnchor().getTrackingState() == ARTrackable.TrackingState.TRACKING) {
                // Light intensity 1.
                mObjectDisplay.onDrawFrame(viewMatrix, projectionMatrix, 1.0f, obj);
            }
        }
    }

    /**
     * Update the information to be displayed on the screen.
     *
     * @param sb String buffer.
     * @param bodies identified ARBody.
     */
    private void updateMessageData(StringBuilder sb, Collection<ARBody> bodies) {
        float fpsResult = doFpsCalculate();
        sb.append("FPS=").append(fpsResult).append(System.lineSeparator());
        int trackingBodySum = 0;
        for (ARBody body : bodies) {
            if (body.getTrackingState() != ARTrackable.TrackingState.TRACKING) {
                continue;
            }
            trackingBodySum++;
            sb.append("body action: ").append(body.getBodyAction()).append(System.lineSeparator());
        }
        sb.append("tracking body sum: ").append(trackingBodySum).append(System.lineSeparator());
        sb.append("Virtual Object number: ").append(mVirtualObjects.size()).append(System.lineSeparator());
    }

    private void handleGestureEvent(ARFrame arFrame, ARCamera arCamera) {
        GestureEvent event = mQueuedSingleTaps.poll();
        if (event == null) {
            return;
        }

        // Do not perform anything when the object is not tracked.
        if (arCamera.getTrackingState() != ARTrackable.TrackingState.TRACKING) {
            return;
        }

        int eventType = event.getType();
        if (eventType == GestureEvent.GESTURE_EVENT_TYPE_SINGLETAPCONFIRMED) {
            MotionEvent tap = event.getEventFirst();
            ARHitResult hitResult = hitTest4Result(arFrame, arCamera, tap);
            if (hitResult == null) {
                return;
            }
            doWhenEventTypeSingleTap(hitResult);
        }
    }

    private void doWhenEventTypeSingleTap(ARHitResult hitResult) {
        // The hit results are sorted by distance. Only the nearest hit point is valid.
        // Set the number of stored objects to 10 to avoid the overload of rendering and AR Engine.
        if (mVirtualObjects.size() >= 16) {
            mVirtualObjects.get(0).getAnchor().detach();
            mVirtualObjects.remove(0);
        }

        ARTrackable currentTrackable = hitResult.getTrackable();
        if (currentTrackable instanceof ARPoint || currentTrackable instanceof ARPlane) {
            mVirtualObjects.add(new VirtualObject(hitResult.createAnchor(), BLUE_COLORS));
        } else {
            LogUtil.info(TAG, "Hit result is not plane or point.");
        }
    }
}
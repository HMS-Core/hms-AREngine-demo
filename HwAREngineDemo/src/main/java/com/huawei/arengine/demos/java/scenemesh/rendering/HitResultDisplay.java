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

import android.content.Context;
import android.view.MotionEvent;

import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.hiar.ARFrame;
import com.huawei.hiar.ARHitResult;
import com.huawei.hiar.ARLightEstimate;
import com.huawei.hiar.ARPlane;
import com.huawei.hiar.ARPoint;
import com.huawei.hiar.ARPose;
import com.huawei.hiar.ARTrackable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Draws a virtual object based on specified parameters.
 *
 * @author hw
 * @since 2021-01-26
 */
public class HitResultDisplay implements SceneMeshComponenDisplay {
    private static final String TAG = HitResultDisplay.class.getSimpleName();

    private static final float MATERIAL_AMBIENT = 0.0f;

    private static final float MATERIAL_DIFFUSE = 3.5f;

    private static final float MATERIAL_SPECULAR = 1.0f;

    private static final float MATERIAL_SPECULAI_POWER = 6.0f;

    private static final int BLOCK_QUEUE_CAPACITY = 2;

    private final float[] mAnchorMatrixs = new float[16];

    private float mScaleFactor = 0.15f;

    private VirtualObjectData mVirtualObject = new VirtualObjectData();

    private ArrayList<ColoredArAnchor> mAnchors = new ArrayList<>();

    private ArrayBlockingQueue<MotionEvent> mQueuedSingleTaps = new ArrayBlockingQueue<>(BLOCK_QUEUE_CAPACITY);

    @Override
    public void init(Context context) {
        mVirtualObject.init(context);
        mVirtualObject.setMaterialProperties(MATERIAL_AMBIENT, MATERIAL_DIFFUSE, MATERIAL_SPECULAR,
            MATERIAL_SPECULAI_POWER);
    }

    @Override
    public void onDrawFrame(ARFrame arFrame, float[] viewmtxs, float[] projmtxs) {
        handleTap(arFrame);
        ARLightEstimate le = arFrame.getLightEstimate();
        float lightIntensity = 1;
        if (le.getState() != ARLightEstimate.State.NOT_VALID) {
            lightIntensity = le.getPixelIntensity();
        }
        Iterator<ColoredArAnchor> ite = mAnchors.iterator();
        LogUtil.debug(TAG, "Anchor cnt is : " + mAnchors.size());
        while (ite.hasNext()) {
            ColoredArAnchor coloredAnchor = ite.next();
            if (coloredAnchor.getAnchor().getTrackingState() == ARTrackable.TrackingState.STOPPED) {
                ite.remove();
                continue;
            }
            if (coloredAnchor.getAnchor().getTrackingState() == ARTrackable.TrackingState.TRACKING) {
                coloredAnchor.getAnchor().getPose().toMatrix(mAnchorMatrixs, 0);
                showArPose(coloredAnchor);
                mVirtualObject.updateModelMatrix(mAnchorMatrixs, mScaleFactor);
                mVirtualObject.draw(viewmtxs, projmtxs, lightIntensity, coloredAnchor.getColor());
            }
        }
    }

    /**
     * Set a gesture type queue.
     *
     * @param queuedSingleTaps Gesture type queue.
     */
    public void setQueuedSingleTaps(ArrayBlockingQueue<MotionEvent> queuedSingleTaps) {
        if (queuedSingleTaps == null) {
            LogUtil.error(TAG, "setSession error, arSession is null!");
            return;
        }
        mQueuedSingleTaps = queuedSingleTaps;
    }

    /**
     * This is because the click frequency is usually lower than the frame rate.
     * Only one click is processed for each frame.
     *
     * @param frame Frame to be processed.
     */
    private void handleTap(ARFrame frame) {
        MotionEvent tap = mQueuedSingleTaps.poll();

        if (tap == null) {
            return;
        }

        LogUtil.debug(TAG, "handleTap, tap not null");

        ARHitResult hitResult = null;
        ARTrackable trackable = null;
        boolean isHasHitFlag = false;

        List<ARHitResult> hitTestResults = frame.hitTest(tap);
        for (int i = 0; i < hitTestResults.size(); i++) {
            // Check whether a plane is hit and whether it is hit in a plane polygon.
            ARHitResult hitResultTemp = hitTestResults.get(i);
            trackable = hitResultTemp.getTrackable();
            if (trackable instanceof ARPoint
                && ((ARPoint) trackable).getOrientationMode() == ARPoint.OrientationMode.ESTIMATED_SURFACE_NORMAL) {
                isHasHitFlag = true;
                hitResult = hitResultTemp;
            }
        }
        if (!isHasHitFlag || hitResult == null) {
            LogUtil.debug(TAG, "Mesh hit fail!!: ");
            return;
        }

        // Sort by depth. Only the nearest hit on the plane or on the directional point is considered.
        // Limit the number of objects that can be created.
        // This prevents the rendering system and AR Engine from being overloaded.
        if (mAnchors.size() >= 16) {
            mAnchors.get(0).getAnchor().detach();
            mAnchors.remove(0);
        }

        // Assign a color to the object for display based on the trackable type attached to the anchor point.
        // AR_TRACKABLE_POINT is blue and AR_TRACKABLE_PLANE is green.
        String objColor;
        trackable = hitResult.getTrackable();
        if (trackable instanceof ARPoint) {
            objColor = ColoredArAnchor.AR_TRACK_POINT_COLOR;
        } else if (trackable instanceof ARPlane) {
            objColor = ColoredArAnchor.AR_TRACK_PLANE_COLOR;
        } else {
            objColor = ColoredArAnchor.AR_DEFAULT_COLOR;
        }

        // Add an anchor to notify AR Engine that it should track the location in space.
        // Create the anchor on the plane so that the 3D model can be placed in the correct position
        // relative to the external environment and plane.
        mAnchors.add(new ColoredArAnchor(hitResult.createAnchor(), objColor));

        LogUtil.debug(TAG, "Add anchor Success!!: ");
    }

    /**
     * Displays the AR posture.
     *
     * @param coloredAnchor AR anchor for coloring.
     */
    private void showArPose(ColoredArAnchor coloredAnchor) {
        ARPose anchorPose = coloredAnchor.getAnchor().getPose();
        String anchorPoseStr = "tx = " + anchorPose.tx() + " ty = " + anchorPose.ty() + " tz = " + anchorPose.tz()
            + " qx = " + anchorPose.qx() + " qy = " + anchorPose.qy() + " qz = " + anchorPose.qz() + " qw = "
            + anchorPose.qw();
        LogUtil.debug(TAG, "Anchor Pose is :" + anchorPoseStr);
    }
}

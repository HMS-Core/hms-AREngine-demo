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
 * 根据指定参数绘制虚拟对象。
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
     * 设置手势类型队列。
     *
     * @param queuedSingleTaps 手势类型队列。
     */
    public void setQueuedSingleTaps(ArrayBlockingQueue<MotionEvent> queuedSingleTaps) {
        if (queuedSingleTaps == null) {
            LogUtil.error(TAG, "setSession error, arSession is null!");
            return;
        }
        mQueuedSingleTaps = queuedSingleTaps;
    }

    /**
     * 因为与帧速率相比，点击频率通常较低。每帧只处理一次点击。
     *
     * @param frame 需要处理的帧。
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
            // 检查是否有平面被击中，以及是否在平面多边形内被击中
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

        // 按深度排序。只考虑在平面或定向点上最近的撞击。限制创建的对象数量。
        // 这样可以避免渲染系统和AREngine都过载。
        if (mAnchors.size() >= 16) {
            mAnchors.get(0).getAnchor().detach();
            mAnchors.remove(0);
        }

        // 根据此锚点附加的可跟踪类型，为对象分配颜色以进行呈现。
        // AR_TRACKABLE_POINT为蓝色，AR_TRACKABLE_PLANE为绿色。
        String objColor;
        trackable = hitResult.getTrackable();
        if (trackable instanceof ARPoint) {
            objColor = ColoredArAnchor.AR_TRACK_POINT_COLOR;
        } else if (trackable instanceof ARPlane) {
            objColor = ColoredArAnchor.AR_TRACK_PLANE_COLOR;
        } else {
            objColor = ColoredArAnchor.AR_DEFAULT_COLOR;
        }

        // 添加一个锚点通知AREngine ，它应该在空间中跟踪这个位置。
        // 在平面上创建这个锚点，以便将3D模型置于相对于外界和平面的正确位置。
        mAnchors.add(new ColoredArAnchor(hitResult.createAnchor(), objColor));

        LogUtil.debug(TAG, "Add anchor Success!!: ");
    }

    /**
     * 显示AR姿势。
     *
     * @param coloredAnchor 着色的AR锚点。
     */
    private void showArPose(ColoredArAnchor coloredAnchor) {
        ARPose anchorPose = coloredAnchor.getAnchor().getPose();
        String anchorPoseStr = "tx = " + anchorPose.tx() + " ty = " + anchorPose.ty() + " tz = " + anchorPose.tz()
            + " qx = " + anchorPose.qx() + " qy = " + anchorPose.qy() + " qz = " + anchorPose.qz() + " qw = "
            + anchorPose.qw();
        LogUtil.debug(TAG, "Anchor Pose is :" + anchorPoseStr);
    }
}

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

package com.huawei.arengine.demos.scenemesh.service

import android.content.Context
import android.view.MotionEvent
import com.huawei.arengine.demos.common.LogUtil
import com.huawei.arengine.demos.scenemesh.controller.ColoredArAnchor
import com.huawei.arengine.demos.scenemesh.util.Constants
import com.huawei.hiar.ARLightEstimate
import com.huawei.hiar.ARFrame
import com.huawei.hiar.ARTrackable
import com.huawei.hiar.ARHitResult
import com.huawei.hiar.ARPoint
import com.huawei.hiar.ARPlane
import java.util.concurrent.ArrayBlockingQueue

/**
 * Draws a virtual object based on specified parameters.
 *
 * @author hw
 * @since 2021-04-21
 */
class HitResultService {
    companion object {
        private const val TAG = "HitResultService"
    }

    private val mVirtualObject: VirtualObjectData = VirtualObjectData()

    private var mQueuedSingleTaps = ArrayBlockingQueue<MotionEvent>(Constants.BLOCK_QUEUE_CAPACITY)

    private val mAnchorMatrixs = FloatArray(16)

    private val mScaleFactor = 0.15f

    private val mAnchors: ArrayList<ColoredArAnchor> = ArrayList<ColoredArAnchor>()

    fun init(context: Context?) {
        context?.let { mVirtualObject.init(it) }
        mVirtualObject.setMaterialProperties(Constants.MATERIAL_AMBIENT, Constants.MATERIAL_DIFFUSE,
            Constants.MATERIAL_SPECULAR, Constants.MATERIAL_SPECULAI_POWER)
    }

    fun onDrawFrame(arFrame: ARFrame, viewmtxs: FloatArray?, projmtxs: FloatArray?) {
        handleTap(arFrame)
        val le = arFrame.lightEstimate
        var lightIntensity = 1f
        if (le.state != ARLightEstimate.State.NOT_VALID) {
            lightIntensity = le.pixelIntensity
        }
        val ite: MutableIterator<ColoredArAnchor> = mAnchors.iterator()
        LogUtil.debug(TAG, "Anchor cnt is : " + mAnchors.size)
        while (ite.hasNext()) {
            val coloredAnchor: ColoredArAnchor = ite.next()
            if (coloredAnchor.anchor.trackingState == ARTrackable.TrackingState.STOPPED) {
                ite.remove()
                continue
            }
            if (coloredAnchor.anchor.trackingState == ARTrackable.TrackingState.TRACKING) {
                coloredAnchor.anchor.pose.toMatrix(mAnchorMatrixs, 0)
                showArPose(coloredAnchor)
                mVirtualObject.updateModelMatrix(mAnchorMatrixs, mScaleFactor)
                mVirtualObject.draw(viewmtxs, projmtxs, lightIntensity, coloredAnchor.color)
            }
        }
    }

    /**
     * Displays the AR posture.
     *
     * @param coloredAnchor AR anchor for coloring.
     */
    private fun showArPose(coloredAnchor: ColoredArAnchor) {
        val anchorPose = coloredAnchor.anchor.pose
        val anchorPoseStr = ("tx = " + anchorPose.tx() + " ty = " + anchorPose.ty() + " tz = " + anchorPose.tz()
            + " qx = " + anchorPose.qx() + " qy = " + anchorPose.qy() + " qz = " + anchorPose.qz() + " qw = "
            + anchorPose.qw())
        LogUtil.debug(TAG, "Anchor Pose is :$anchorPoseStr")
    }

    private fun handleTap(arFrame: ARFrame) {
        val tap: MotionEvent = mQueuedSingleTaps.poll() ?: return

        LogUtil.debug(TAG, "handleTap, tap not null")

        var hitResult: ARHitResult? = null
        var trackable: ARTrackable? = null
        var isHasHitFlag = false

        val hitTestResults: List<ARHitResult> = arFrame.hitTest(tap)
        for (i in hitTestResults.indices) {
            // Check whether a plane is hit and whether it is hit in a plane polygon.
            val hitResultTemp = hitTestResults[i]
            trackable = hitResultTemp.trackable
            if (trackable is ARPoint
                && (trackable as ARPoint).orientationMode == ARPoint.OrientationMode.ESTIMATED_SURFACE_NORMAL) {
                isHasHitFlag = true
                hitResult = hitResultTemp
            }
        }
        if (!isHasHitFlag || hitResult == null) {
            LogUtil.debug(TAG, "Mesh hit fail!!: ")
            return
        }

        // Sort by depth. Only the nearest hit on the plane or on the directional point is considered.
        // Limit the number of objects that can be created.
        // This prevents the rendering system and AR Engine from being overloaded.
        if (mAnchors.size >= 16) {
            mAnchors[0].anchor.detach()
            mAnchors.removeAt(0)
        }

        // Assign a color to the object for display based on the trackable type attached to the anchor point.
        // AR_TRACKABLE_POINT is blue and AR_TRACKABLE_PLANE is green.
        val objColor: String
        trackable = hitResult.trackable
        objColor = when (trackable) {
            is ARPoint -> {
                Constants.AR_TRACK_POINT_COLOR
            }
            is ARPlane -> {
                Constants.AR_TRACK_PLANE_COLOR
            }
            else -> {
                Constants.AR_DEFAULT_COLOR
            }
        }

        // Add an anchor to notify AR Engine that it should track the location in space.
        // Create the anchor on the plane so that the 3D model can be placed in the correct position relative
        // to the external environment and plane.
        mAnchors.add(ColoredArAnchor(hitResult.createAnchor(), objColor))

        LogUtil.debug(TAG, "Add anchor Success!!: ")
    }

    /**
     * Set a gesture type queue.
     *
     * @param queuedSingleTaps Gesture type queue.
     */
    fun setQueuedSingleTaps(queuedSingleTaps: ArrayBlockingQueue<MotionEvent>?) {
        if (queuedSingleTaps == null) {
            LogUtil.error(TAG, "setSession error, arSession is null!")
            return
        }
        mQueuedSingleTaps = queuedSingleTaps
    }
}
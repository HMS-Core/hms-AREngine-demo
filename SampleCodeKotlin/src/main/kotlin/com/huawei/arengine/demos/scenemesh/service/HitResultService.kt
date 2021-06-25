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
 * 根据指定参数绘制虚拟对象。
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
     * 显示AR姿势。
     *
     * @param coloredAnchor 着色的AR锚点。
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
            // 检查是否有平面被击中，以及是否在平面多边形内被击中
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

        // 按深度排序。只考虑在平面或定向点上最近的撞击。限制创建的对象数量。
        // 这样可以避免渲染系统和AREngine都过载。
        if (mAnchors.size >= 16) {
            mAnchors[0].anchor.detach()
            mAnchors.removeAt(0)
        }

        // 根据此锚点附加的可跟踪类型，为对象分配颜色以进行呈现。
        // AR_TRACKABLE_POINT为蓝色，AR_TRACKABLE_PLANE为绿色。
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

        // 添加一个锚点通知AREngine ，它应该在空间中跟踪这个位置。
        // 在平面上创建这个锚点，以便将3D模型置于相对于外界和平面的正确位置。
        mAnchors.add(ColoredArAnchor(hitResult.createAnchor(), objColor))

        LogUtil.debug(TAG, "Add anchor Success!!: ")
    }

    /**
     * 设置手势类型队列。
     *
     * @param queuedSingleTaps 手势类型队列。
     */
    fun setQueuedSingleTaps(queuedSingleTaps: ArrayBlockingQueue<MotionEvent>?) {
        if (queuedSingleTaps == null) {
            LogUtil.error(TAG, "setSession error, arSession is null!")
            return
        }
        mQueuedSingleTaps = queuedSingleTaps
    }
}
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

package com.huawei.arengine.demos.common.util

import com.huawei.arengine.demos.common.LogUtil
import com.huawei.hiar.ARPose

/**
 * Obtain the utility class of the AR pose rotation quaternion.
 *
 * @author HW
 * @since 2022-04-14
 */
object LabelDisplayUtil {
    private val TAG = "LabelDisplayUtil"

    private val INDEX_X = 0

    private val INDEX_Y = 1

    private val INDEX_Z = 2

    private val DOUBLE_NUM = 2

    private val QUATERNION_SIZE = 4

    private val VERTICAL_INFO_PLANE = 90.0f

    private val STRAIGHT_ANGLE = 180.0f

    private val SOUTH_POLE_SINGULARITY = -1.0f

    private val NORTH_POLE_SINGULARITY = 1.0f

    private val GIMBAL_LOCK_NUM = 0.0000001f

    fun getMeasureQuaternion(cameraDisplayPose: ARPose, angle: Float): FloatArray? {
        var measureQuaternion: FloatArray? = null
        val camRot = cameraDisplayPose.extractRotation()
        if (camRot == null) {
            LogUtil.error(TAG, "getMeasureQuaternion camRot null!")
            return measureQuaternion
        }
        var camEul = getEulerAngles(cameraDisplayPose)

        val perRad: Float = STRAIGHT_ANGLE / Math.PI.toFloat()
        val anglesX: Float = VERTICAL_INFO_PLANE / perRad - camEul[1] / DOUBLE_NUM
        val anglesY = camEul[0] * perRad / perRad
        val anglesZ = angle / perRad
        val qx = ((Math.sin((anglesY / DOUBLE_NUM).toDouble()).toFloat()
            * Math.sin((anglesZ / DOUBLE_NUM).toDouble()).toFloat()
            * Math.cos((anglesX / DOUBLE_NUM).toDouble()).toFloat())
            + (Math.cos((anglesY / DOUBLE_NUM).toDouble()).toFloat()
            * Math.cos((anglesZ / DOUBLE_NUM).toDouble()).toFloat()
            * Math.sin((anglesX / DOUBLE_NUM).toDouble()).toFloat()))
        val qy = ((Math.sin((anglesY / DOUBLE_NUM).toDouble()).toFloat()
            * Math.cos((anglesZ / DOUBLE_NUM).toDouble()).toFloat()
            * Math.cos((anglesX / DOUBLE_NUM).toDouble()).toFloat())
            + (Math.cos((anglesY / DOUBLE_NUM).toDouble()).toFloat()
            * Math.sin((anglesZ / DOUBLE_NUM).toDouble()).toFloat()
            * Math.sin((anglesX / DOUBLE_NUM).toDouble()).toFloat()))
        val qz = ((Math.cos((anglesY / DOUBLE_NUM).toDouble()).toFloat()
            * Math.sin((anglesZ / DOUBLE_NUM).toDouble()).toFloat()
            * Math.cos((anglesX / DOUBLE_NUM).toDouble()).toFloat())
            - (Math.sin((anglesY / DOUBLE_NUM).toDouble()).toFloat()
            * Math.cos((anglesZ / DOUBLE_NUM).toDouble()).toFloat()
            * Math.sin((anglesX / DOUBLE_NUM).toDouble()).toFloat()))
        val qw = ((Math.cos((anglesY / DOUBLE_NUM).toDouble()).toFloat()
            * Math.cos((anglesZ / DOUBLE_NUM).toDouble()).toFloat()
            * Math.cos((anglesX / DOUBLE_NUM).toDouble()).toFloat())
            - (Math.sin((anglesY / DOUBLE_NUM).toDouble()).toFloat()
            * Math.sin((anglesZ / DOUBLE_NUM).toDouble()).toFloat()
            * Math.sin((anglesX / DOUBLE_NUM).toDouble()).toFloat()))

        val verticalQuaternion = floatArrayOf(qx, qy, qz, qw)
        val verticalPose = ARPose.makeRotation(verticalQuaternion)
        measureQuaternion = FloatArray(QUATERNION_SIZE)

        verticalPose.getRotationQuaternion(measureQuaternion, 0)
        return measureQuaternion
    }

    fun getEulerAngles(pose: ARPose): FloatArray {
        var quaternion: FloatArray = floatArrayOf(pose.qx(), pose.qy(), pose.qz())
        var quaternionW = pose.qw()
        var squareW = quaternionW * quaternionW
        var squareX = quaternion[INDEX_X] * quaternion[INDEX_X]
        var squareY = quaternion[INDEX_Y] * quaternion[INDEX_Y]
        var squareZ = quaternion[INDEX_Z] * quaternion[INDEX_Z]
        var psign = -DOUBLE_NUM * (-quaternionW * quaternion[INDEX_X] + quaternion[INDEX_Y] * quaternion[INDEX_Z])

        var pitch = 0.0f
        var yaw = 0.0f
        var roll = 0.0f

        if (psign < SOUTH_POLE_SINGULARITY + GIMBAL_LOCK_NUM) {
            // Antarctic singularity.
            pitch = (-Math.PI).toFloat()
            roll = Math.atan2(
                (DOUBLE_NUM * (-quaternion[INDEX_Y] * quaternion[INDEX_X]
                    + quaternionW * quaternion[INDEX_Z])).toDouble(),
                (squareW + squareX - squareY - squareZ).toDouble()).toFloat()
        } else if (psign > NORTH_POLE_SINGULARITY - GIMBAL_LOCK_NUM) {
            // Arctic singularity.
            yaw = 0.0f
            pitch = Math.PI.toFloat() * DOUBLE_NUM
            roll = Math.atan2(
                (DOUBLE_NUM * (-quaternion[INDEX_Y] * quaternion[INDEX_X]
                    + quaternionW * quaternion[INDEX_Z])).toDouble(),
                (squareW + squareX - squareY - squareZ).toDouble()).toFloat()
        } else {
            yaw = (-Math.atan2(
                (-DOUBLE_NUM * (quaternionW * quaternion[INDEX_Y]
                    + quaternion[INDEX_X] * quaternion[INDEX_Z])).toDouble(),
                (squareW + squareZ - squareY - squareX).toDouble())).toFloat()
            pitch = Math.asin(psign.toDouble()).toFloat()
            roll = Math.atan2(
                (DOUBLE_NUM * (quaternionW * quaternion[INDEX_Z]
                    + quaternion[INDEX_Y] * quaternion[INDEX_X])).toDouble(),
                (squareW + squareY - squareX - squareZ).toDouble()).toFloat()
        }

        return floatArrayOf(yaw, pitch, roll)
    }
}
/**
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.huawei.arengine.demos.world.util

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Pair
import android.view.View
import android.widget.TextView
import com.huawei.arengine.demos.R
import com.huawei.arengine.demos.common.util.findViewById
import com.huawei.hiar.ARPlane
import com.huawei.hiar.ARPose
import com.huawei.hiar.ARTrackable

/**
 * function class
 *
 * @author HW
 * @since 2020-11-06
 */
const val MATRIX_SCALE_SX = -1.0f

const val MATRIX_SCALE_SY = -1.0f

val planeIds = setOf(R.id.plane_other, R.id.plane_wall,
    R.id.plane_floor, R.id.plane_seat, R.id.plane_table,
    R.id.plane_ceiling, R.id.plane_door, R.id.plane_window, R.id.plane_bed)

/**
 * Calculate the distance between a point in a space and a plane. This method is used
 * to calculate the distance between a camera in a space and a specified plane.
 *
 * @param planePose ARPose of a plane.
 * @param cameraPose ARPose of a camera.
 * @return Calculation results.
 */
fun calculateDistancePlaneToCamera(planePose: ARPose, cameraPose: ARPose): Float {
    // Store the normal vector of the current plane.
    return FloatArray(3).let {
        planePose.getTransformedAxis(1, 1.0f, it, 0)

        // Calculate the distance from the camera to the plane. If the value is negative, it indicates that
        // the camera is behind the plane (the normal vector distinguishes the front side from the back side).
        val axisX = (cameraPose.tx() - planePose.tx()) * it[0]
        val axisY = (cameraPose.ty() - planePose.ty()) * it[1]
        val axisZ = (cameraPose.tz() - planePose.tz()) * it[2]
        axisX + axisY + axisZ
    }

}

fun getPlaneBitmaps(activity: Activity): ArrayList<Bitmap> {
    return ArrayList<Bitmap>().apply {
        planeIds.forEach { id ->
            add(getPlaneBitmap(activity, id))
        }
    }
}

private fun getPlaneBitmap(activity: Activity, id: Int): Bitmap {
    val view = findViewById<TextView>(activity, id)
    view.run {
        isDrawingCacheEnabled = true
        measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        layout(0, 0, view.measuredWidth, view.measuredHeight)
    }
    val bitmap = view.drawingCache
    val matrix = Matrix()
    matrix.setScale(MATRIX_SCALE_SX, MATRIX_SCALE_SY)
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

fun getSortedPlanes(planes: Collection<ARPlane>, cameraPose: ARPose): ArrayList<ARPlane> {
    // Planes must be sorted by the distance from the camera so that we can first draw the closer planes,
    // and have them block the further planes.
    val pairPlanes = ArrayList<Pair<ARPlane, Float>>(planes.size)
    for (plane in planes) {
        if (plane.type == ARPlane.PlaneType.UNKNOWN_FACING
            || plane.trackingState != ARTrackable.TrackingState.TRACKING || plane.subsumedBy != null) {
            continue
        }
        val distance = calculateDistancePlaneToCamera(plane.centerPose, cameraPose)
        pairPlanes.add(Pair(plane, distance))
    }
    return sortPlanes(pairPlanes)
}


private fun sortPlanes(pairPlanes: ArrayList<Pair<ARPlane, Float>>): ArrayList<ARPlane> {
    val sortedPlanes = ArrayList<ARPlane>()
    pairPlanes.apply {
        sortWith(Comparator { planeA, planeB -> planeA.second.compareTo(planeB.second) })
    }.forEach {
        sortedPlanes.add(it.first)
    }
    return sortedPlanes
}
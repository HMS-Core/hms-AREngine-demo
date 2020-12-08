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
package com.huawei.arengine.demos.world.service

import android.opengl.Matrix
import android.util.Log
import android.view.MotionEvent
import com.huawei.arengine.demos.world.model.VirtualObject
import com.huawei.arengine.demos.world.pojo.GestureEvent
import com.huawei.arengine.demos.world.util.Constants
import com.huawei.arengine.demos.world.util.GestureEventFactory
import com.huawei.arengine.demos.world.util.ObjectUtil
import com.huawei.arengine.demos.world.util.calculateDistancePlaneToCamera
import com.huawei.hiar.ARCamera
import com.huawei.hiar.ARFrame
import com.huawei.hiar.ARHitResult
import com.huawei.hiar.ARPlane
import com.huawei.hiar.ARPoint
import java.util.concurrent.ArrayBlockingQueue

/**
 * the class control gesture on surfaceView.
 *
 * @author HW
 * @since 2020-11-08
 */
class HitTestService(private val tapQueue: ArrayBlockingQueue<GestureEvent>,
    private val virtualObjects: ArrayList<VirtualObject>) {
    companion object {
        private const val TAG = "GestureService"
    }

    private var surfaceWidth = 0f

    private var surfaceHeight = 0f

    private var selectedObject: VirtualObject? = null

    /**
     * If the surface size is changed, update the changed size of the record synchronously.
     * This method is called when [WorldRenderController.onSurfaceChanged].
     *
     * @param width Surface's width.
     * @param height Surface's height.
     */
    fun setSurfaceSize(width: Float, height: Float) {
        this.surfaceWidth = width
        this.surfaceHeight = height
    }

    fun handleGestureEvent(arFrame: ARFrame, arCamera: ARCamera,
        projectionMatrix: FloatArray, viewMatrix: FloatArray) {
        val event = tapQueue.poll() ?: return

        when (event.type) {
            GestureEventFactory.GESTURE_EVENT_TYPE_DOUBLE_TAP -> {
                handleDoubleTapEvent(viewMatrix, projectionMatrix, event)
            }
            GestureEventFactory.GESTURE_EVENT_TYPE_SCROLL -> {
                selectedObject?.run {
                    setArAnchor(hitTest4Result(arFrame, arCamera, event.eventSecond).createAnchor())
                }
            }
            GestureEventFactory.GESTURE_EVENT_TYPE_SINGLE_TAP_CONFIRMED -> {
                // Do not perform anything when an object is selected.
                selectedObject?.setSelected(false)
                selectedObject = null
                handleSingleTapEvent(hitTest4Result(arFrame, arCamera, event.eventFirst))
            }
            else -> {
                Log.e(TAG, "Unknown motion event type, and do nothing.")
            }
        }
    }

    private fun handleDoubleTapEvent(viewMatrix: FloatArray, projectionMatrix: FloatArray, event: GestureEvent) {
        selectedObject?.setSelected(false)
        selectedObject = null
        for (obj in virtualObjects) {
            if (isHit(viewMatrix, projectionMatrix, obj, event.eventFirst)) {
                obj.setSelected(true)
                selectedObject = obj
                break
            }
        }
    }

    private fun handleSingleTapEvent(hitResult: ARHitResult) {
        /**
         * The hit results are sorted by distance. Only the nearest hit point is valid.
         * Set the number of stored objects to 10 to avoid the overload of rendering and AR Engine.
         */
        virtualObjects.run {
            if (size > Constants.MAX_VIRTUAL_OBJECT) {
                get(0).getArAnchor().detach()
                removeAt(0)
            }
        }
        when (hitResult.trackable) {
            is ARPoint -> {
                virtualObjects.add(VirtualObject(hitResult.createAnchor(), Constants.BLUE_COLORS))
            }
            is ARPlane -> {
                virtualObjects.add(VirtualObject(hitResult.createAnchor(), Constants.GREEN_COLORS))
            }
            else -> {
                Log.i(TAG, "Hit result is not plane or point.")
            }
        }
    }

    private fun hitTest4Result(frame: ARFrame, camera: ARCamera, event: MotionEvent?): ARHitResult {
        lateinit var hitResult: ARHitResult
        for (arHitResult in frame.hitTest(event)) {
            // Determine whether the hit point is within the plane polygon.
            arHitResult ?: continue
            val trackable = arHitResult.trackable
            val isPlanHitJudge = (trackable is ARPlane && trackable.isPoseInPolygon(arHitResult.hitPose)
                && calculateDistancePlaneToCamera(arHitResult.hitPose, camera.pose) > 0)
            val isPointHitJudge = (trackable is ARPoint
                && trackable.orientationMode == ARPoint.OrientationMode.ESTIMATED_SURFACE_NORMAL)

            // Select points on the plane preferentially.
            if (isPlanHitJudge || isPointHitJudge) {
                hitResult = arHitResult
                if (trackable is ARPlane) {
                    break
                }
            }
        }
        return hitResult
    }

    /**
     * Check whether the virtual object is clicked.
     *
     * @param cameraView The viewMatrix 4 * 4.
     * @param cameraPerspective The ProjectionMatrix 4 * 4.
     * @param obj The virtual object data.
     * @param event The gesture event.
     * @return Return the click result for determining whether the input virtual object is clicked
     */
    private fun isHit(cameraView: FloatArray?, cameraPerspective: FloatArray?,
        obj: VirtualObject, event: MotionEvent?): Boolean {
        event ?: return false
        val modelViewMatrix = FloatArray(Constants.MATRIX_SIZE)
        val modelViewProjectionMatrix = FloatArray(Constants.MATRIX_SIZE)
        Matrix.multiplyMM(modelViewMatrix, 0, cameraView, 0, obj.getModelAnchorMatrix(), 0)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraPerspective, 0, modelViewMatrix, 0)

        // Calculate the coordinates of the smallest bounding box in the coordinate system of the device screen.
        val screenPos = calculateScreenPos(ObjectUtil.boundingBox[0], ObjectUtil.boundingBox[1],
            ObjectUtil.boundingBox[2], modelViewProjectionMatrix)
        // Record the largest bounding rectangle of an object (minX/minY/maxX/maxY).
        val boundarys = floatArrayOf(screenPos[0], screenPos[0], screenPos[1], screenPos[1])

        // Determine whether a screen position corresponding to (maxX, maxY, maxZ) is clicked.
        findMaximum(boundarys, intArrayOf(3, 4, 5), modelViewProjectionMatrix)
        if (hitTestSuccess(event, boundarys)) return true

        // Determine whether a screen position corresponding to (minX, minY, maxZ) is clicked.
        findMaximum(boundarys, intArrayOf(0, 1, 5), modelViewProjectionMatrix)
        if (hitTestSuccess(event, boundarys)) return true

        // Determine whether a screen position corresponding to (minX, maxY, minZ) is clicked.
        findMaximum(boundarys, intArrayOf(0, 4, 2), modelViewProjectionMatrix)
        if (hitTestSuccess(event, boundarys)) return true

        // Determine whether a screen position corresponding to (minX, maxY, maxZ) is clicked.
        findMaximum(boundarys, intArrayOf(0, 4, 5), modelViewProjectionMatrix)
        if (hitTestSuccess(event, boundarys)) return true

        // Determine whether a screen position corresponding to (maxX, minY, minZ) is clicked.
        findMaximum(boundarys, intArrayOf(3, 1, 2), modelViewProjectionMatrix)
        if (hitTestSuccess(event, boundarys)) return true

        // Determine whether a screen position corresponding to (maxX, minY, maxZ) is clicked.
        findMaximum(boundarys, intArrayOf(3, 1, 5), modelViewProjectionMatrix)
        if (hitTestSuccess(event, boundarys)) return true

        // Determine whether a screen position corresponding to (maxX, maxY, maxZ) is clicked.
        findMaximum(boundarys, intArrayOf(3, 4, 2), modelViewProjectionMatrix)
        return hitTestSuccess(event, boundarys)
    }

    private fun hitTestSuccess(event: MotionEvent, boundarys: FloatArray): Boolean {
        return (event.x > boundarys[0] && event.x < boundarys[1]
            && event.y > boundarys[2] && event.y < boundarys[3])
    }

    private fun findMaximum(minXmaxXminYmaxY: FloatArray, index: IntArray, modelViewProjectionMatrix: FloatArray) {
        // The size of minXmaxXminYmaxY is 4, and the size of index is 3.
        val screenPos = calculateScreenPos(ObjectUtil.boundingBox[index[0]],
            ObjectUtil.boundingBox[index[1]], ObjectUtil.boundingBox[index[2]], modelViewProjectionMatrix)
        if (screenPos[0] < minXmaxXminYmaxY[0]) {
            minXmaxXminYmaxY[0] = screenPos[0]
        }
        if (screenPos[0] > minXmaxXminYmaxY[1]) {
            minXmaxXminYmaxY[1] = screenPos[0]
        }
        if (screenPos[1] < minXmaxXminYmaxY[2]) {
            minXmaxXminYmaxY[2] = screenPos[1]
        }
        if (screenPos[1] > minXmaxXminYmaxY[3]) {
            minXmaxXminYmaxY[3] = screenPos[1]
        }
    }

    /**
     * Convert the input coordinates to the plane coordinate system.
     *
     * @param coordinateX Float x coordinate
     * @param coordinateY Float x coordinate
     * @param coordinateZ Float x coordinate
     * @return the coordinate values in the clip coordinate system
     */
    private fun calculateScreenPos(coordinateX: Float, coordinateY: Float, coordinateZ: Float,
        modelViewProjectionMatrix: FloatArray): FloatArray {
        // The coordinates of the point are four-dimensional (x, y, z, w).
        val vector = floatArrayOf(coordinateX, coordinateY, coordinateZ, 1.0f)

        // Store the coordinate values in the clip coordinate system.
        val results = FloatArray(4)
        Matrix.multiplyMV(results, 0, modelViewProjectionMatrix, 0, vector, 0)
        results[0] = ((results[0] / results[3] + 1.0f) * surfaceWidth) / 2.0f
        results[1] = ((1.0f - results[1] / results[3]) * surfaceHeight) / 2.0f
        results[2] /= results[3]
        results[3] = 1.0f
        return results
    }
}
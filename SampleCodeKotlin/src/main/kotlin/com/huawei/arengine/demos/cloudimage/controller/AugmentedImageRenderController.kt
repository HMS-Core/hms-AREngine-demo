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

package com.huawei.arengine.demos.cloudimage.controller

import android.app.Activity
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Pair
import android.widget.Toast

import com.huawei.arengine.demos.cloudimage.service.ImageBoxRenderService
import com.huawei.arengine.demos.common.LogUtil
import com.huawei.arengine.demos.common.controller.DisplayRotationController
import com.huawei.arengine.demos.common.exception.ArDemoRuntimeException
import com.huawei.arengine.demos.common.service.BackgroundTextureService
import com.huawei.hiar.ARSession
import com.huawei.hiar.ARAugmentedImage
import com.huawei.hiar.ARAnchor
import com.huawei.hiar.ARFrame
import com.huawei.hiar.ARTrackable

import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * cloud-enhanced image rendering controller, configured to render an augmented image.
 *
 * @author HW
 * @since 2022-03-14
 */
class AugmentedImageRenderController(private val activity: Activity,
    private val mDisplayRotationManager: DisplayRotationController) : GLSurfaceView.Renderer {
    private val TAG: String = "AugmentedImageRenderController"

    private val PROJ_MATRIX_SIZE = 16

    private val PROJ_MATRIX_OFFSET = 0

    private val PROJ_MATRIX_NEAR = 0.1f

    private val PROJ_MATRIX_FAR = 100.0f

    private val LIST_MAX_SIZE = 10

    private val mImageBoxRenderService: ImageBoxRenderService = ImageBoxRenderService()

    private val mBackgroundDisplay by lazy { BackgroundTextureService() }

    private var mSession: ARSession? = null

    private var mCurrentImageId = ""

    private var mIsImageTrackOnly = false

    /**
     * Augmented image and its associated center pose anchor, keyed by index of the augmented image in the database.
     */
    private val augmentedImageMap: HashMap<Int, Pair<ARAugmentedImage, ARAnchor>> =
        HashMap(LIST_MAX_SIZE)

    /**
     * Set ARSession, which will update and obtain the latest data in OnDrawFrame.
     *
     * @param arSession ARSession.
     */
    fun setArSession(arSession: ARSession?) {
        mSession = arSession
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Set the window color.
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        mBackgroundDisplay.init()
        mImageBoxRenderService.init()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        mBackgroundDisplay.updateProjectionMatrix(width, height)
        GLES20.glViewport(0, 0, width, height)
        mDisplayRotationManager.updateViewportRotation(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (mSession == null) {
            return
        }
        if (mDisplayRotationManager.isDeviceRotation) {
            mDisplayRotationManager.updateArSessionDisplayGeometry(mSession)
        }
        try {
            mSession!!.setCameraTextureName(mBackgroundDisplay.externalTextureId)
            val arFrame = mSession!!.update()
            mBackgroundDisplay.renderBackgroundTexture(arFrame)
            renderImage(arFrame)
        } catch (exception: ArDemoRuntimeException) {
            LogUtil.error(TAG, "Exception on the ArDemoRuntimeException!");
            return
        } catch (throwable: Throwable) {
            // This prevents the app from crashing due to unhandled exceptions.
            LogUtil.error(TAG, "Exception on the OpenGL thread." + throwable::class.java);
            return
        }
    }

    private fun renderImage(frame: ARFrame?) {
        frame ?: return
        val arCamera = frame.camera

        // If not tracking, don't draw image.
        if (arCamera.trackingState == ARTrackable.TrackingState.PAUSED) {
            LogUtil.info(TAG, "draw background PAUSED!")
            return
        }

        // Get projection matrix.
        val projectionMatrix = FloatArray(PROJ_MATRIX_SIZE)
        arCamera.getProjectionMatrix(
            projectionMatrix,
            PROJ_MATRIX_OFFSET,
            PROJ_MATRIX_NEAR,
            PROJ_MATRIX_FAR
        )

        // Get view matrix and draw.
        val viewMatrix = FloatArray(PROJ_MATRIX_SIZE)
        if (mIsImageTrackOnly) {
            Matrix.setIdentityM(viewMatrix, 0)
        } else {
            arCamera.getViewMatrix(viewMatrix, 0)
        }

        // Visualize augmented images.
        drawAugmentedImages(frame, projectionMatrix, viewMatrix)
    }

    private fun drawAugmentedImages(arFrame: ARFrame, projmtx: FloatArray, viewmtx: FloatArray) {
        val updatedAugmentedImages: Collection<ARAugmentedImage> =
            arFrame.getUpdatedTrackables(ARAugmentedImage::class.java)

        // Iterate to update augmentedImageMap, remove elements we cannot draw.
        for (augmentedImage in updatedAugmentedImages) {
            when (augmentedImage.trackingState) {
                ARTrackable.TrackingState.PAUSED -> {
                    // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
                    // but not yet tracked.
                }
                ARTrackable.TrackingState.TRACKING -> initTrackingImages(augmentedImage)
                ARTrackable.TrackingState.STOPPED -> augmentedImageMap.remove(augmentedImage.index)
            }
        }

        // Draw all images in augmentedImageMap
        for (pair in augmentedImageMap.values) {
            val augmentedImage = pair.first
            if (augmentedImage.trackingState == ARTrackable.TrackingState.TRACKING) {
                mImageBoxRenderService.drawImageBox(augmentedImage, viewmtx, projmtx)
            }
        }
    }

    private fun initTrackingImages(augmentedImage: ARAugmentedImage) {
        // Create new anchor for newly found images.
        if (!augmentedImageMap.containsKey(augmentedImage.index)) {
            var centerPoseAnchor: ARAnchor? = null
            if (!mIsImageTrackOnly) {
                centerPoseAnchor = augmentedImage.createAnchor(augmentedImage.centerPose)
            }
            augmentedImageMap.put(augmentedImage.index, Pair.create(augmentedImage, centerPoseAnchor))
        }

        if (!mCurrentImageId.equals(augmentedImage.cloudImageId)) {
            mCurrentImageId = augmentedImage.cloudImageId
            val tipsMsg: String = augmentedImage.cloudImageMetadata
            if (tipsMsg.isEmpty()) {
                return
            }
            activity.runOnUiThread(Runnable {
                Toast.makeText(activity, tipsMsg, Toast.LENGTH_SHORT).show()
            })
        }
    }

    /**
     * Set the type of the tracked object. If the value is true, only the image object is tracked.
     *
     * @param isOnlyImageTrack Boolean variable. If the value is true, only the image object is tracked.
     */
    fun setImageTrackOnly(isOnlyImageTrack: Boolean) {
        mIsImageTrackOnly = isOnlyImageTrack
    }
}
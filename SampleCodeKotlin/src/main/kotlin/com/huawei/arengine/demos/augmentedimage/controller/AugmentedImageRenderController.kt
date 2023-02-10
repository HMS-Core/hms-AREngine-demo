/*
 * Copyright 2023. Huawei Technologies Co., Ltd. All rights reserved.
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.arengine.demos.augmentedimage.controller

import android.app.Activity
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Pair
import com.huawei.arengine.demos.augmentedimage.service.ImageKeyLineService
import com.huawei.arengine.demos.augmentedimage.service.ImageKeyPointService
import com.huawei.arengine.demos.augmentedimage.service.ImageLabelService
import com.huawei.arengine.demos.augmentedimage.util.Constants
import com.huawei.arengine.demos.common.LogUtil
import com.huawei.arengine.demos.common.LogUtil.debug
import com.huawei.arengine.demos.common.LogUtil.info
import com.huawei.arengine.demos.common.controller.DisplayRotationController
import com.huawei.arengine.demos.common.service.BackgroundTextureService
import com.huawei.hiar.*
import com.huawei.hiar.exceptions.ARSessionPausedException
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Augmented image rendering manager, configured to render the image.
 *
 * @author HW
 * @since 2021-03-29
 */
class AugmentedImageRenderController(private val mActivity: Activity,
    private val displayRotationController: DisplayRotationController) : GLSurfaceView.Renderer {
    companion object {
        private val TAG = "AugmentedImageRenderController"
    }

    private var mSession: ARSession? = null

    private var isImageTrackOnly = false

    private val imageRelatedDisplays by lazy {
        ArrayList<AugmentedImageComponentDisplay>().apply {
            add(ImageKeyPointService())
            add(ImageKeyLineService())
            add(ImageLabelService(mActivity))
        }
    }

    private val backgroundDisplay by lazy { BackgroundTextureService() }

    /**
     * Anchors of the augmented image and its related center, which is controlled by the index key of
     * the augmented image in the database.
     */
    private var augmentedImageMap: MutableMap<Int, Pair<ARAugmentedImage, ARAnchor?>?> = HashMap()

    /**
     * Set the ARSession, which updates and obtains the latest data from OnDrawFrame.
     *
     * @param arSession ARSession.
     */
    fun setArSession(arSession: ARSession?) {
        mSession = arSession
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        // Set the window color.
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        backgroundDisplay.init()
        imageRelatedDisplays.forEach {
            it.init()
        }
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        backgroundDisplay.updateProjectionMatrix(width, height)
        GLES20.glViewport(0, 0, width, height)
        displayRotationController.updateViewportRotation(width, height)
    }

    override fun onDrawFrame(gl: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        mSession ?: return
        displayRotationController.run {
            if (isDeviceRotation) {
                updateArSessionDisplayGeometry(mSession)
            }
        }
        mSession!!.setCameraTextureName(backgroundDisplay.externalTextureId)
        val arFrame: ARFrame;
        try {
            arFrame = mSession!!.update()
        } catch (exception: ARSessionPausedException) {
            LogUtil.error(TAG, "Invoke session.resume before invoking Session.update.")
            return;
        } catch (exception: Exception) {
            // This prevents the app from crashing due to unhandled exceptions.
            LogUtil.warn(TAG, "Exception on the OpenGL thread, " + exception.javaClass)
            return
        }
        val arCamera = arFrame.camera
        backgroundDisplay.renderBackgroundTexture(arFrame)

        // If tracking is not set, the augmented image is not drawn.
        if (arCamera.trackingState == ARTrackable.TrackingState.PAUSED) {
            info(TAG, "Draw background paused!")
            return
        }

        // Obtain the projection matrix.
        val projectionMatrix = FloatArray(Constants.PROJ_MATRIX_SIZE)
        arCamera.getProjectionMatrix(projectionMatrix, Constants.PROJ_MATRIX_OFFSET,
            Constants.PROJ_MATRIX_NEAR, Constants.PROJ_MATRIX_FAR)

        // Obtain the view matrix.
        val viewMatrix = FloatArray(Constants.PROJ_MATRIX_SIZE)
        if (isImageTrackOnly) {
            Matrix.setIdentityM(viewMatrix, 0)
        } else {
            arCamera.getViewMatrix(viewMatrix, 0)
        }

        // Draw the augmented image.
        drawAugmentedImages(arFrame, projectionMatrix, viewMatrix)
    }

    private fun drawAugmentedImages(frame: ARFrame, projmtx: FloatArray, viewmtx: FloatArray) {
        val updatedAugmentedImages = frame.getUpdatedTrackables(ARAugmentedImage::class.java)
        debug(TAG, "drawAugmentedImages: Updated augment image is " + updatedAugmentedImages.size)

        // Iteratively update the augmented image mapping and remove the elements that cannot be drawn.
        updatedAugmentedImages.forEach {
            when (it.trackingState) {
                ARTrackable.TrackingState.PAUSED -> {
                }
                ARTrackable.TrackingState.TRACKING -> initTrackingImages(it)
                ARTrackable.TrackingState.STOPPED -> augmentedImageMap.remove(it.index)
                else -> {
                }
            }
        }

        // Map the anchor to the AugmentedImage object and draw all augmentation effects.
        augmentedImageMap.values.forEach {
            val augmentedImage = it!!.first
            if (augmentedImage.trackingState == ARTrackable.TrackingState.TRACKING) {
                imageRelatedDisplays.forEach{
                    it.onDrawFrame(augmentedImage, viewmtx, projmtx)
                }
            }
        }
    }

    private fun initTrackingImages(augmentedImage: ARAugmentedImage) {
        // Create an anchor for the newly found image and bind it to the image object.
        if (!augmentedImageMap.containsKey(augmentedImage.index)) {
            var centerPoseAnchor: ARAnchor ?= null
            if (!isImageTrackOnly) {
                centerPoseAnchor = augmentedImage.createAnchor(augmentedImage.centerPose)
            }
            augmentedImageMap[augmentedImage.index]= Pair.create(augmentedImage, centerPoseAnchor)
        }
    }

    fun setImageTrackOnly(isOnlyImageTrack: Boolean) {
        isImageTrackOnly = isOnlyImageTrack
    }
}
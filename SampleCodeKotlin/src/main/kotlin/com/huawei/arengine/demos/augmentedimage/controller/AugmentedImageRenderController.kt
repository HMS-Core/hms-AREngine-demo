/**
 * Copyright 2021. Huawei Technologies Co., Ltd. All rights reserved.
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
import com.huawei.arengine.demos.common.LogUtil.debug
import com.huawei.arengine.demos.common.LogUtil.info
import com.huawei.arengine.demos.common.controller.DisplayRotationController
import com.huawei.arengine.demos.common.service.BackgroundTextureService
import com.huawei.hiar.*
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * 增强图像渲染管理器，用于渲染图像。
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
     * 增强图像及其相关中心位置锚点，由增强图像在数据库中的索引键控。
     */
    private var augmentedImageMap: MutableMap<Int, Pair<ARAugmentedImage, ARAnchor?>?> = HashMap()

    /**
     * 设置ARSession,ARSession会更新并获取OnDrawFrame中的最新数据。
     *
     * @param arSession ARSession.
     */
    fun setArSession(arSession: ARSession?) {
        mSession = arSession
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        // 设置窗口颜色。
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
        val arFrame = mSession!!.update()
        val arCamera = arFrame.camera
        backgroundDisplay.renderBackgroundTexture(arFrame)

        // 如果未跟踪，则不绘制增强图像。
        if (arCamera.trackingState == ARTrackable.TrackingState.PAUSED) {
            info(TAG, "Draw background paused!")
            return
        }

        // 获取投影矩阵。
        val projectionMatrix = FloatArray(Constants.PROJ_MATRIX_SIZE)
        arCamera.getProjectionMatrix(projectionMatrix, Constants.PROJ_MATRIX_OFFSET,
            Constants.PROJ_MATRIX_NEAR, Constants.PROJ_MATRIX_FAR)

        // 获取视图矩阵。
        val viewMatrix = FloatArray(Constants.PROJ_MATRIX_SIZE)
        if (isImageTrackOnly) {
            Matrix.setIdentityM(viewMatrix, 0)
        } else {
            arCamera.getViewMatrix(viewMatrix, 0)
        }

        // 绘制增强图像。
        drawAugmentedImages(arFrame, projectionMatrix, viewMatrix)
    }

    private fun drawAugmentedImages(frame: ARFrame, projmtx: FloatArray, viewmtx: FloatArray) {
        val updatedAugmentedImages = frame.getUpdatedTrackables(ARAugmentedImage::class.java)
        debug(TAG, "drawAugmentedImages: Updated augment image is " + updatedAugmentedImages.size)

        // 迭代更新增强图像映射，移除无法绘制的元素。
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

        // 根据锚点映射到AugmentedImage对象，绘制所有增强效果。
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
        // 为新找到的图像创建锚点并与图像对象绑定。
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
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

package com.huawei.arengine.demos.face.controller

import android.app.Activity
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.opengl.GLES20
import android.util.DisplayMetrics
import android.util.Size
import android.view.Surface

import com.huawei.arengine.demos.common.LogUtil
import com.huawei.arengine.demos.face.service.CameraService
import com.huawei.hiar.ARConfigBase
import com.huawei.hiar.ARSession

/**
 * This class provide services related to the camera.
 *
 * @author HW
 * @since 2020-10-10
 */
class CameraController(private val activity: Activity) {
    companion object {
        private const val TAG = "CameraController"
    }

    var textureId = -1

    var cameraService: CameraService? = null

    private var preViewSurface: Surface? = null

    private var vgaSurface: Surface? = null

    private var metaDataSurface: Surface? = null

    private var depthSurface: Surface? = null

    lateinit var arSession: ARSession

    lateinit var arConfig: ARConfigBase

    fun startCameraService(cameraLensFacing: Int) {
        if (cameraService == null) {
            LogUtil.info(TAG, "new Camera")
            val displayMetrics = DisplayMetrics()
            cameraService = CameraService().apply {
                setupCamera(displayMetrics.widthPixels, displayMetrics.heightPixels, cameraLensFacing)
            }
        }

        // Check whether createCameraService is called for the first time.
        if (textureId == -1) {
            val textureIds = IntArray(1)
            GLES20.glGenTextures(1, textureIds, 0)
            textureId = textureIds[0]
        }
        arSession.setCameraTextureName(textureId)
        initSurface()
        val surfaceTexture = SurfaceTexture(textureId)
        cameraService?.run {
            setPreviewTexture(surfaceTexture)
            setPreViewSurface(preViewSurface)
            setVgaSurface(vgaSurface)
            setDepthSurface(depthSurface)
            if (!openCamera()) {
                LogUtil.error(TAG, "Open camera failed!")
                activity.finish()
            }
        }
    }

    private fun initSurface() {
        val surfaceTypeList: MutableList<ARConfigBase.SurfaceType> = arConfig.imageInputSurfaceTypes
        val surfaceList = arConfig.imageInputSurfaces
        LogUtil.info(TAG, "surfaceList size : " + surfaceList.size)
        val size = surfaceTypeList.size
        for (i in 0 until size) {
            val type = surfaceTypeList[i]
            val surface = surfaceList[i]
            when (type) {
                ARConfigBase.SurfaceType.PREVIEW -> preViewSurface = surface
                ARConfigBase.SurfaceType.VGA -> vgaSurface = surface
                ARConfigBase.SurfaceType.METADATA -> metaDataSurface = surface
                ARConfigBase.SurfaceType.DEPTH -> depthSurface = surface
                else -> LogUtil.info(TAG, "Unknown type.")
            }
            LogUtil.info(TAG, "list[$i] get surface : $surface, type : $type")
        }
    }

    fun closeCamera() {
        cameraService?.closeCamera()
    }

    fun stopCameraThread() {
        cameraService?.stopCameraThread()
    }

    /**
     * Obtain the preview size list.
     *
     * @param cameraFacing Camera facing, facing back is 1, facing front is 0.
     * @return Preview size list.
     */
    fun getPreviewSizeList(cameraFacing: Int): Array<Size?>? {
        if (activity.getSystemService(Context.CAMERA_SERVICE) !is CameraManager) {
            LogUtil.error(TAG, "Set upCamera error. service invalid!")
            return arrayOfNulls(0)
        }
        val cameraManager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        if (cameraManager == null) {
            LogUtil.error(TAG, "Set upCamera error. cameraManager == null")
            return arrayOfNulls(0)
        }
        try {
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val cameraLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    ?: continue
                if (cameraLensFacing != cameraFacing) {
                    continue
                }
                val maps = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                if (maps?.getOutputSizes(SurfaceTexture::class.java) == null) {
                    continue
                }
                return maps.getOutputSizes(SurfaceTexture::class.java)
            }
        } catch (e: CameraAccessException) {
            LogUtil.error(TAG, "Set upCamera error")
        }
        return arrayOfNulls(0)
    }
}
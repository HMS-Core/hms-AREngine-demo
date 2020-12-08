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
package com.huawei.arengine.demos.face.controller

import android.app.Activity
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
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

    fun startCameraService() {
        if (cameraService == null) {
            Log.i(TAG, "new Camera")
            val displayMetrics = DisplayMetrics()
            cameraService = CameraService().apply {
                setupCamera(displayMetrics.widthPixels, displayMetrics.heightPixels)
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
                Log.e(TAG, "Open camera failed!")
                activity.finish()
            }
        }
    }

    private fun initSurface() {
        val surfaceTypeList: MutableList<ARConfigBase.SurfaceType> = arConfig.imageInputSurfaceTypes
        val surfaceList = arConfig.imageInputSurfaces
        Log.i(TAG, "surfaceList size : " + surfaceList.size)
        val size = surfaceTypeList.size
        for (i in 0 until size) {
            val type = surfaceTypeList[i]
            val surface = surfaceList[i]
            when (type) {
                ARConfigBase.SurfaceType.PREVIEW -> preViewSurface = surface
                ARConfigBase.SurfaceType.VGA -> vgaSurface = surface
                ARConfigBase.SurfaceType.METADATA -> metaDataSurface = surface
                ARConfigBase.SurfaceType.DEPTH -> depthSurface = surface
                else -> Log.i(TAG, "Unknown type.")
            }
            Log.i(TAG, "list[$i] get surface : $surface, type : $type")
        }
    }

    fun closeCamera() {
        cameraService?.closeCamera()
    }

    fun stopCameraThread() {
        cameraService?.stopCameraThread()
    }
}
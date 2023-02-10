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

package com.huawei.arengine.demos.cloudaugmentobject.controller

import android.app.Activity
import android.opengl.GLES20
import android.opengl.GLSurfaceView

import com.huawei.arengine.demos.R
import com.huawei.arengine.demos.cloudaugmentobject.service.ObjectLabelRenderService
import com.huawei.arengine.demos.cloudaugmentobject.service.updateScreenText
import com.huawei.arengine.demos.common.LogUtil
import com.huawei.arengine.demos.common.controller.DisplayRotationController
import com.huawei.arengine.demos.common.exception.ArDemoRuntimeException
import com.huawei.arengine.demos.common.service.BackgroundTextureService
import com.huawei.arengine.demos.common.service.TextService
import com.huawei.arengine.demos.common.util.FramePerSecond
import com.huawei.arengine.demos.common.util.showScreenTextView
import com.huawei.hiar.ARFrame
import com.huawei.hiar.ARObject
import com.huawei.hiar.ARSession

import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * This class renders object data obtained by the AR Engine.
 *
 * @author HW
 * @since 2022-04-12
 */
class AugmentedObjectRenderController(private val activity: Activity,
    private val displayRotationManager: DisplayRotationController) : GLSurfaceView.Renderer {
    companion object {
        private const val TAG = "AugmentedObjectRenderController"

        private const val PROJ_MATRIX_OFFSET = 0

        private const val PROJ_MATRIX_NEAR = 0.1f

        private const val PROJ_MATRIX_FAR = 100.0f
    }

    private var arSession: ARSession? = null

    private val fps by lazy { FramePerSecond(0, 0f, 0) }

    private val backgroundTextureService by lazy { BackgroundTextureService() }

    private val objectTextDisplay by lazy { TextService() }

    private val mObjectRelatedDisplay by lazy { ObjectLabelRenderService(activity) }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        backgroundTextureService.init()
        objectTextDisplay.setListener { text ->
            showScreenTextView(activity, activity.findViewById(R.id.cloudAugmentObjectTextView), text)
        }
        mObjectRelatedDisplay.init()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        backgroundTextureService.updateProjectionMatrix(width, height)
        GLES20.glViewport(0, 0, width, height)
        displayRotationManager.updateViewportRotation(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (arSession == null) {
            LogUtil.debug(TAG, "arSession != null ")
        }
        arSession ?: return

        displayRotationManager.run {
            if (isDeviceRotation)
                updateArSessionDisplayGeometry(arSession)
        }

        try {
            val arFrame = arSession?.run {
                setCameraTextureName(backgroundTextureService.externalTextureId)
                update()
            } ?: return
            backgroundTextureService.renderBackgroundTexture(arFrame)
            renderObject(arFrame)
        } catch (exception: ArDemoRuntimeException) {
            LogUtil.error(TAG, "Exception on the ArDemoRuntimeException!");
        } catch (throwable: Throwable) {
            // This prevents the app from crashing due to unhandled exceptions.
            LogUtil.error(TAG, "Exception on the OpenGL thread. " + throwable.javaClass);
        }
    }

    private fun renderObject(arFrame: ARFrame) {
        val objects = arSession?.getAllTrackables(ARObject::class.java)

        // The size of the projection matrix is 4 * 4.
        var projectionMatrix = FloatArray(16)

        // The size of the view matrix is 4 * 4.
        var viewMatrix = FloatArray(16)

        val camera = arFrame.camera
        camera?.apply {
            getProjectionMatrix(projectionMatrix, PROJ_MATRIX_OFFSET, PROJ_MATRIX_NEAR, PROJ_MATRIX_FAR)
            getViewMatrix(viewMatrix, 0)
        }
        mObjectRelatedDisplay.onDrawFrame(objects, viewMatrix, projectionMatrix, camera.displayOrientedPose)
        StringBuilder().let {
            updateScreenText(it, objects, fps)
            objectTextDisplay.drawText(it)
        }
    }

    fun setArSession(arSession: ARSession?) {
        this.arSession = arSession
    }
}
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
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import com.huawei.arengine.demos.R
import com.huawei.arengine.demos.common.controller.DisplayRotationController
import com.huawei.arengine.demos.common.exception.SampleAppException
import com.huawei.arengine.demos.common.service.BackgroundTextureService
import com.huawei.arengine.demos.common.service.TextService
import com.huawei.arengine.demos.common.util.FramePerSecond
import com.huawei.arengine.demos.common.util.findViewById
import com.huawei.arengine.demos.common.util.showScreenTextView
import com.huawei.arengine.demos.face.service.FaceGeometryService
import com.huawei.arengine.demos.face.service.updateScreenText
import com.huawei.hiar.ARFace
import com.huawei.hiar.ARFrame
import com.huawei.hiar.ARSession
import com.huawei.hiar.ARTrackable.TrackingState
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * This class manages rendering related to facial data.
 *
 * @author HW
 * @since 2020-10-10
 */
class FaceRenderController(private val activity: Activity,
    private var displayRotationController: DisplayRotationController,
    private var isOpenCameraOutside: Boolean) : GLSurfaceView.Renderer {
    companion object {
        private const val TAG = "FaceRenderController"
    }

    /**
     * Initialize the texture ID.
     */
    var mTextureId: Int = -1

    private var arSession: ARSession? = null

    private val fps by lazy { FramePerSecond(0, 0f, 0) }

    private val backgroundTextureService by lazy { BackgroundTextureService() }

    private val faceGeometryService by lazy { FaceGeometryService() }

    private val faceTextService by lazy { TextService() }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        Log.i(TAG, "On surface created textureId= $mTextureId")
        if (isOpenCameraOutside) {
            backgroundTextureService.init(mTextureId)
        } else {
            backgroundTextureService.init()
        }
        faceGeometryService.init()
        faceTextService.setListener { text ->
            showScreenTextView(activity, findViewById(activity, R.id.faceTextView), text)
        }
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        backgroundTextureService.updateProjectionMatrix(width, height)
        GLES20.glViewport(0, 0, width, height)
        displayRotationController.updateViewportRotation(width, height)
    }

    override fun onDrawFrame(unused: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        arSession ?: return

        displayRotationController.run {
            if (isDeviceRotation) {
                updateArSessionDisplayGeometry(arSession)
            }
        }
        try {
            val frame = arSession?.run {
                setCameraTextureName(backgroundTextureService.externalTextureId)
                update()
            } ?: return
            backgroundTextureService.renderBackgroundTexture(frame)
            renderFace(frame)
        } catch (exception: SampleAppException) {
            Log.e(TAG, "Exception on the ArDemoRuntimeException!")
        }
    }

    private fun renderFace(frame: ARFrame) {
        val faces = arSession?.getAllTrackables(ARFace::class.java)
        if (faces == null || faces.isEmpty()) {
            faceTextService.drawText(null)
            return
        }
        val camera = frame.camera
        faces.forEach { face ->
            if (face.trackingState == TrackingState.TRACKING) {
                camera?.let { faceGeometryService.renderFace(it, face) }
                StringBuilder().let {
                    updateScreenText(it, face, fps)
                    faceTextService.drawText(it)
                }
            }
        }
    }

    /**
     * Set the ARSession object, which is used to obtain the latest data in the onDrawFrame method.
     *
     * @param arSession ARSession.
     */
    fun setArSession(arSession: ARSession?) {
        this.arSession = arSession
    }
}

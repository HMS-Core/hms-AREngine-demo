/**
 * Copyright 2021. Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huawei.arengine.demos.scenemesh.controller

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import com.huawei.arengine.demos.R
import com.huawei.arengine.demos.common.LogUtil
import com.huawei.arengine.demos.common.controller.DisplayRotationController
import com.huawei.arengine.demos.common.exception.SampleAppException
import com.huawei.arengine.demos.common.service.BackgroundTextureService
import com.huawei.arengine.demos.common.util.findViewById
import com.huawei.arengine.demos.scenemesh.service.HitResultService
import com.huawei.arengine.demos.scenemesh.service.SceneMeshService
import com.huawei.arengine.demos.scenemesh.util.Constants
import com.huawei.hiar.ARFrame
import com.huawei.hiar.ARSession
import com.huawei.hiar.ARTrackable
import java.util.concurrent.ArrayBlockingQueue
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class SceneMeshRenderController(private val mActivity: Activity,
    private val mContext: Context,
    private var mDisplayRotationController: DisplayRotationController) : GLSurfaceView.Renderer {
    companion object {
        private const val TAG = "SceneMeshRenderController"
    }

    private val mBackgroundTextureService by lazy { BackgroundTextureService() }

    private val mHitResultService by lazy { HitResultService() }

    private val mSceneMeshService by lazy { SceneMeshService() }

    private var mArSession: ARSession? = null

    private val updateInterval = 0.5f

    private var lastInterval: Long = 0

    private var frames = 0

    private var fps = 0f

    override fun onDrawFrame(gl: GL10?) {
        showFpsTextView(doFpsCalculate().toString())
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (mArSession == null) {
            return
        }
        if (mDisplayRotationController.isDeviceRotation) {
            mDisplayRotationController.updateArSessionDisplayGeometry(mArSession)
        }
        try {
            mArSession!!.setCameraTextureName(mBackgroundTextureService.externalTextureId)
            val arFrame: ARFrame = mArSession!!.update()
            val arCamera = arFrame.camera
            mBackgroundTextureService.renderBackgroundTexture(arFrame)
            if (arCamera.trackingState == ARTrackable.TrackingState.PAUSED) {
                LogUtil.debug(TAG, "Camera TrackingState Paused: ")
                showSearchingMessage(View.VISIBLE)
                return
            }
            val projmtxs = FloatArray(16)
            arCamera.getProjectionMatrix(projmtxs, Constants.PROJ_MATRIX_OFFSET, Constants.PROJ_MATRIX_NEAR, Constants.PROJ_MATRIX_FAR)
            val viewmtxs = FloatArray(16)
            arCamera.getViewMatrix(viewmtxs, 0)
            showSearchingMessage(View.GONE)

            // Draw a grid.
            mSceneMeshService.onDrawFrame(arFrame, viewmtxs, projmtxs)

            // Process the click event and add a virtual model.
            mHitResultService.onDrawFrame(arFrame, viewmtxs, projmtxs)
        } catch (e: SampleAppException) {
            LogUtil.error(TAG, "Exception on the ArDemoRuntimeException!")
        } catch (t: Throwable) {
            // Prevent apps from crashing due to unprocessed exceptions.
            LogUtil.error(TAG, "Exception on the OpenGL thread.")
        }
    }

    private fun showSearchingMessage(state: Int) {
        if (findViewById<TextView>(mActivity, R.id.scene_mesh_searchingTextView).visibility != state) {
            mActivity.runOnUiThread(Runnable {
                findViewById<TextView>(mActivity, R.id.scene_mesh_searchingTextView).visibility = state
            })
        }
    }

    private fun doFpsCalculate(): Float {
        ++frames
        val timeNow = System.currentTimeMillis()

        // Convert millisecond to second.
        if ((timeNow - lastInterval) / 1000.0f > updateInterval) {
            fps = frames / ((timeNow - lastInterval) / 1000.0f)
            frames = 0
            lastInterval = timeNow
        }
        return fps
    }

    /**
     * Display the text view.
     *
     * @param text Text to display.
     */
    private fun showFpsTextView(text: String?) {
        mActivity.runOnUiThread(Runnable {
            findViewById<TextView>(mActivity, R.id.fpsTextView).setTextColor(Color.WHITE)

            // Set the font size.
            findViewById<TextView>(mActivity, R.id.fpsTextView).setTextSize(Constants.FPS_TEXT_SIZE)
            if (text != null) {
                findViewById<TextView>(mActivity, R.id.fpsTextView).setText(text)
            } else {
                findViewById<TextView>(mActivity, R.id.fpsTextView).setText("")
            }
        })
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        mBackgroundTextureService.updateProjectionMatrix(width, height)
        mDisplayRotationController.updateViewportRotation(width, height)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Set the window color.
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        mBackgroundTextureService.init()
        mHitResultService.init(mContext)
        mSceneMeshService.init()
    }

    /**
     * Set the AR Session to update and obtain the latest data in onDrawFrame.
     *
     * @param arSession AR session.
     */
    fun setArSession(arSession: ARSession?) {
        if (arSession == null) {
            LogUtil.error(TAG, "setSession error, arSession is null!")
            return
        }
        mArSession = arSession
    }

    /**
     * Set the DisplayRotationManage object, which is used in onSurfaceChanged and onDrawFrame.
     *
     * @param displayRotationController DisplayRotationManage is the customized object.
     */
    fun setDisplayRotationController(displayRotationController: DisplayRotationController?) {
        if (displayRotationController == null) {
            LogUtil.error(TAG, "SetDisplayRotationManage error, displayRotationManage is null!")
            return
        }
        mDisplayRotationController = displayRotationController
    }

    /**
     * Set a gesture type queue.
     *
     * @paramparam queuedSingleTaps Gesture type queue.
     */
    fun setQueuedSingleTaps(queuedSingleTaps: ArrayBlockingQueue<MotionEvent>?) {
        if (queuedSingleTaps == null) {
            LogUtil.error(TAG, "setQueuedSingleTaps, queuedSingleTaps is null!")
            return
        }
        mHitResultService.let {
            mHitResultService.setQueuedSingleTaps(queuedSingleTaps)
        }
    }
}
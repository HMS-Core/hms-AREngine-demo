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

package com.huawei.arengine.demos.health.controller

import android.app.Activity
import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView

import com.huawei.arengine.demos.common.LogUtil
import com.huawei.arengine.demos.common.controller.DisplayRotationController
import com.huawei.arengine.demos.common.service.BackgroundTextureService
import com.huawei.arengine.demos.health.util.Constants
import com.huawei.hiar.ARFace
import com.huawei.hiar.ARSession
import com.huawei.hiar.ARTrackable
import com.huawei.hiar.exceptions.ARFatalException
import com.huawei.hiar.exceptions.ARSessionPausedException

import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Contains logics related to background rendering and health data updating.
 *
 * @author HW
 * @since 2021-11-23
 */
class HealthRenderController(private val mActivity: Activity, private val mContext: Context,
    private val mDisplayRotationController: DisplayRotationController) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "HealthRenderController"
    }

    private var mArSession: ARSession? = null

    private val mBackgroundTextureService by lazy {
        BackgroundTextureService()
    }

    private var mHealthParamTable: TableLayout? = null

    private var mProgress = 0

    override fun onSurfaceCreated(gl10: GL10, eglConfig: EGLConfig) {
        mBackgroundTextureService.init()
    }

    override fun onSurfaceChanged(gl10: GL10, width: Int, height: Int) {
        mBackgroundTextureService.updateProjectionMatrix(width, height)
        GLES20.glViewport(0, 0, width, height)
        mDisplayRotationController.updateViewportRotation(width, height)
    }

    override fun onDrawFrame(gl10: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        mArSession ?: return
        mDisplayRotationController.run {
            if (isDeviceRotation) {
                updateArSessionDisplayGeometry(mArSession)
            }
        }
        try {
            mArSession!!.setCameraTextureName(mBackgroundTextureService.externalTextureId)
            val frame = mArSession!!.update()
            mBackgroundTextureService.renderBackgroundTexture(frame)
            val faces = mArSession!!.getAllTrackables(ARFace::class.java)
            if (faces.size == 0) {
                return
            }
            for (face in faces) {
                if (face.trackingState != ARTrackable.TrackingState.TRACKING) {
                    continue
                }
                val healthParams = face.healthParameters
                if (mProgress < Constants.MAX_PROGRESS) {
                    updateHealthParamTable(healthParams)
                }
            }
        } catch (e: ARSessionPausedException) {
            LogUtil.error(TAG, "Exception on the ARSessionPausedException!")
        } catch (e: ARFatalException) {
            LogUtil.error(TAG, "Exception on the ARFatalException!")
        } catch (t: Throwable) {
            // This prevents the app from crashing due to unhandled exceptions.
            LogUtil.error(TAG, "Exception on the OpenGL thread.")
        }
    }

    /**
     * Setting the health check progress.
     *
     * @param progress Progress Information.
     */
    fun setHealthCheckProgress(progress: Int) {
        mProgress = progress
    }

    /**
     * Set an ARSession. The input ARSession will be called in onDrawFrame
     * to obtain the latest data. This method is called when [Activity.onResume].
     *
     * @param arSession ARSession.
     */
    fun setArSession(arSession: ARSession?) {
        this.mArSession = arSession
    }

    /**
     * Setting the TableLayout Used for Health Display.
     *
     * @param tableLayout TableLayout.
     */
    fun setHealthParamTable(tableLayout: TableLayout?) {
        if (tableLayout == null) {
            LogUtil.error(TAG, "Set health parameter table failed, tableLayout is null")
            return
        }
        mHealthParamTable = tableLayout
    }

    private fun updateHealthParamTable(healthParams: HashMap<ARFace.HealthParameter, Float>) {
        mActivity.runOnUiThread {
            mHealthParamTable!!.removeAllViews()
            val heatRateTableRow = initTableRow(ARFace.HealthParameter.PARAMETER_HEART_RATE.toString(),
                healthParams.getOrDefault(ARFace.HealthParameter.PARAMETER_HEART_RATE, 0.0f).toString())
            mHealthParamTable!!.addView(heatRateTableRow)
            val breathRateTableRow = initTableRow(ARFace.HealthParameter.PARAMETER_BREATH_RATE.toString(),
                healthParams.getOrDefault(ARFace.HealthParameter.PARAMETER_BREATH_RATE, 0.0f).toString())
            mHealthParamTable!!.addView(breathRateTableRow)
        }
    }

    private fun initTableRow(keyStr: String, valueStr: String): TableRow {
        val textViewKey = TextView(mContext)
        val textViewValue = TextView(mContext)
        textViewKey.text = keyStr
        textViewValue.text = valueStr
        textViewValue.setPadding(Constants.PADDING_VALUE, 0, 0, 0)
        val tableRow = TableRow(mContext)
        tableRow.addView(textViewKey)
        tableRow.addView(textViewValue)
        return tableRow
    }
}
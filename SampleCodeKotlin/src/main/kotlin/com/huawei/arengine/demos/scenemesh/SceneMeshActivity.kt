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

package com.huawei.arengine.demos.scenemesh

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast

import com.huawei.arengine.demos.R
import com.huawei.arengine.demos.common.LogUtil
import com.huawei.arengine.demos.common.controller.DisplayRotationController
import com.huawei.arengine.demos.common.service.PermissionManageService
import com.huawei.arengine.demos.common.util.findViewById
import com.huawei.arengine.demos.common.util.isAvailableArEngine
import com.huawei.arengine.demos.common.view.BaseActivity
import com.huawei.arengine.demos.databinding.ScenemeshActivityMainBinding
import com.huawei.arengine.demos.scenemesh.controller.SceneMeshRenderController
import com.huawei.arengine.demos.scenemesh.util.Constants
import com.huawei.hiar.ARConfigBase
import com.huawei.hiar.ARSession
import com.huawei.hiar.ARWorldTrackingConfig
import com.huawei.hiar.exceptions.*

import java.util.concurrent.ArrayBlockingQueue

/**
 * The following example demonstrates how to use the grid in AR Engine.
 *
 * @author HW
 * @since 2021-04-21
 */
class SceneMeshActivity : BaseActivity() {
    companion object {
        private const val TAG = "SceneMeshActivity"
    }

    private val mDisplayRotationController by lazy { DisplayRotationController() }

    private val mSceneMeshRenderController by lazy {
        SceneMeshRenderController(this, this, mDisplayRotationController)
    }

    private val mQueuedSingleTaps = ArrayBlockingQueue<MotionEvent>(Constants.BLOCK_QUEUE_CAPACITY)

    private var mGestureDetector: GestureDetector? = null

    private var mArSession: ARSession? = null

    private lateinit var sceneMeshActivityBinding: ScenemeshActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sceneMeshActivityBinding = ScenemeshActivityMainBinding.inflate(layoutInflater)
        setContentView(sceneMeshActivityBinding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        init()
    }

    override fun onResume() {
        super.onResume()
        if (!PermissionManageService.hasPermission()) {
            finish()
        }
        errorMessage = null
        if (mArSession == null) {
            if (!isAvailableArEngine(this)) {
                finish()
                return
            }
            try {
                mArSession = ARSession(this.applicationContext)
                val config: ARConfigBase = ARWorldTrackingConfig(mArSession)
                config.focusMode = ARConfigBase.FocusMode.AUTO_FOCUS
                config.enableItem = (ARConfigBase.ENABLE_MESH or ARConfigBase.ENABLE_DEPTH.toLong().toInt()).toLong()
                mArSession!!.configure(config)
                mSceneMeshRenderController.setArSession(mArSession)

                // Detect whether the current mobile phone camera is a depth camera.
                if (config.enableItem and ARConfigBase.ENABLE_MESH.toLong() == 0L) {
                    findViewById<TextView>(this, R.id.scene_mesh_searchingTextView).visibility = View.GONE
                    throw ARUnSupportedConfigurationException()
                }
            } catch (capturedException: Exception) {
                setMessageWhenError(capturedException)
            }
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                stopArSession()
                return
            }
        }
        try {
            mArSession!!.resume()
            mDisplayRotationController.registerDisplayListener()
        } catch (e: ARCameraNotAvailableException) {
            Toast.makeText(this, "Camera open failed, please restart the app", Toast.LENGTH_LONG).show()
            mArSession = null
            return
        }
        sceneMeshActivityBinding.surfaceview.onResume()
        mDisplayRotationController.registerDisplayListener()
    }

    override fun onPause() {
        super.onPause()
        LogUtil.info(TAG, "onPause start.")
        if (mArSession != null) {
            sceneMeshActivityBinding.surfaceview.onPause()
            mArSession!!.pause()
        }
        if (mArSession != null) {
            mDisplayRotationController.unregisterDisplayListener()
            sceneMeshActivityBinding.surfaceview.onPause()
            mArSession!!.pause()
            LogUtil.info(TAG, "Session paused!")
        }
        LogUtil.info(TAG, "onPause end.")
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtil.info(TAG, "onDestroy start.")
        if (mArSession != null) {
            mArSession!!.stop()
            mArSession = null
        }
        super.onDestroy()
        LogUtil.info(TAG, "onDestroy end.")
    }

    override fun onWindowFocusChanged(isHasFocus: Boolean) {
        LogUtil.debug(TAG, "onWindowFocusChanged")
        super.onWindowFocusChanged(isHasFocus)
        if (isHasFocus) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    /**
     * Stop the AR session.
     */
    private fun stopArSession() {
        LogUtil.info(TAG, "stopArSession start.")
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        if (mArSession != null) {
            mArSession!!.stop()
            mArSession = null
        }
        LogUtil.info(TAG, "stopArSession end.")
    }

    private fun init() {
        mSceneMeshRenderController.setDisplayRotationController(mDisplayRotationController)
        mSceneMeshRenderController.setQueuedSingleTaps(mQueuedSingleTaps)
        mGestureDetector = GestureDetector(this, object : SimpleOnGestureListener() {
            override fun onSingleTapUp(event: MotionEvent): Boolean {
                onSingleTap(event)
                return true
            }

            override fun onDown(e: MotionEvent): Boolean {
                return true
            }
        })
        sceneMeshActivityBinding.surfaceview.apply {
            setOnTouchListener { v, event ->
                v.performClick()
                mGestureDetector!!.onTouchEvent(event)
            }
            preserveEGLContextOnPause = true
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            setRenderer(mSceneMeshRenderController)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    /**
     * Click event recording method, which saves the click event to the list.
     *
     * @param event Click event.
     */
    private fun onSingleTap(event: MotionEvent) {
        LogUtil.debug(TAG, "onSingleTap, add MotionEvent to mQueuedSingleTaps$event")
        val result: Boolean = mQueuedSingleTaps.offer(event)
        if (!result) {
            LogUtil.error(TAG, "The message queue is full. No more messages can be added.")
        }
    }
}
/**
 * Copyright 2021. Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huawei.arengine.demos.worldbody

import android.app.Activity
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import com.huawei.arengine.demos.R
import com.huawei.arengine.demos.common.controller.DisplayRotationController
import com.huawei.arengine.demos.common.service.PermissionManageService
import com.huawei.arengine.demos.world.controller.GestureController
import kotlinx.android.synthetic.main.world_java_activity_main.surfaceView

/**
 * This AR example shows how to use the WorldBody AR scene of HUAWEI AR Engine,
 * including how to identify planes, use the click function, and identify
 * specific images.
 *
 * @author HW
 * @since 2021-04-08
 */
class WorldBodyActivity : Activity() {

    companion object {
        private const val TAG = "WorldBodyActivity"
    }

    private val displayRotationController by lazy { DisplayRotationController() }

    private val gestureController by lazy { GestureController() }

    private val worldbodyRenderController by lazy {
        WorldBodyRenderController(this, displayRotationController, gestureController)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.world_java_activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        initUi()
    }

    /**
     * Initializes the UI and sets gestureController, worldbodyRenderController, and GL parameters.
     */
    private fun initUi() {
        surfaceView.apply {
            setOnTouchListener { v, event ->
                v.performClick()
                gestureController.gestureDetector.onTouchEvent(event)
            }
            preserveEGLContextOnPause = true
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            setRenderer(worldbodyRenderController)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    override fun onResume() {
        super.onResume()
        if (!PermissionManageService.hasPermission()) {
            finish()
        }
        worldbodyRenderController.startArSession()
    }

    override fun onPause() {
        super.onPause()
        displayRotationController.unregisterDisplayListener()
        surfaceView.onPause()
        worldbodyRenderController.arSession?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        worldbodyRenderController.arSession?.stop()
        worldbodyRenderController.arSession = null
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        hasFocus.let {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }
}
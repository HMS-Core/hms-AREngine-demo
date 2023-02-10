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

package com.huawei.arengine.demos.world.controller

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Matrix
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.view.View
import android.widget.ImageView
import android.widget.TextView

import com.huawei.arengine.demos.R
import com.huawei.arengine.demos.common.LogUtil
import com.huawei.arengine.demos.common.controller.DisplayRotationController
import com.huawei.arengine.demos.common.exception.SampleAppException
import com.huawei.arengine.demos.common.service.BackgroundTextureService
import com.huawei.arengine.demos.common.service.TextService
import com.huawei.arengine.demos.common.util.calFps
import com.huawei.arengine.demos.common.util.createBitmapImage
import com.huawei.arengine.demos.common.util.findViewById
import com.huawei.arengine.demos.common.util.FramePerSecond
import com.huawei.arengine.demos.common.util.showScreenTextView
import com.huawei.arengine.demos.databinding.WorldJavaActivityMainBinding
import com.huawei.arengine.demos.world.service.LabelService
import com.huawei.arengine.demos.world.service.ObjectService
import com.huawei.arengine.demos.world.service.PointService
import com.huawei.arengine.demos.world.util.Constants
import com.huawei.arengine.demos.world.util.ObjectUtil
import com.huawei.hiar.ARCamera
import com.huawei.hiar.ARConfigBase
import com.huawei.hiar.ARFrame
import com.huawei.hiar.ARLightEstimate
import com.huawei.hiar.ARPlane
import com.huawei.hiar.ARSession
import com.huawei.hiar.ARTarget
import com.huawei.hiar.ARTrackable
import com.huawei.hiar.ARWorldTrackingConfig

import java.util.Arrays

import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

import kotlin.collections.ArrayList

/**
 * This class provides rendering management related to the world scene, including
 * label rendering and virtual object rendering management.
 *
 * @author HW
 * @since 2020-10-10
 */
class WorldRenderController(private val activity: Activity,
    private val displayRotationController: DisplayRotationController,
    private val gestureController: GestureController,
    private val worldActivityBinding: WorldJavaActivityMainBinding) : GLSurfaceView.Renderer {
    companion object {
        private const val TAG = "WorldRenderController"

        private const val MATRIX_SCALE_SX = -1.0f

        private const val MATRIX_SCALE_SY = -1.0f

        private const val SIDE_LENGTH = 128

        private const val LIGHTING_CUBE_MAP_SINGLE_FACE_SIZE = SIDE_LENGTH * SIDE_LENGTH * 3

        private const val LIGHTING_CUBE_MAP_SIZE = LIGHTING_CUBE_MAP_SINGLE_FACE_SIZE * 6
    }

    private var arSession: ARSession? = null

    private val fps by lazy { FramePerSecond(0, 0f, 0) }

    private val backgroundTextureService by lazy { BackgroundTextureService() }

    private val worldTextService by lazy { TextService() }

    private val labelService by lazy { LabelService() }

    private val pointCloudRenderer by lazy { PointService() }

    private val objectService by lazy { ObjectService() }

    private val targetRenderManager by lazy { TargetRenderManager() }

    private lateinit var mArWorldTrackingConfig : ARWorldTrackingConfig

    private var mHaveSetEnvTextureData = false

    private var mUpdateIndex = 0

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        // Set the window color.
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        backgroundTextureService.init()
        objectService.init()
        labelService.init(getPlaneBitmaps())
        pointCloudRenderer.init()
        worldTextService.setListener { text ->
            showScreenTextView(activity, findViewById(activity, R.id.wordTextView), text)
        }
        targetRenderManager.init()
        targetRenderManager.initTargetLabelDisplay(getTargetLabelBitmaps(""))
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        backgroundTextureService.updateProjectionMatrix(width, height)
        GLES20.glViewport(0, 0, width, height)
        displayRotationController.updateViewportRotation(width, height)
        gestureController.setSurfaceSize(width.toFloat(), height.toFloat())
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

            // Set the environment texture probe and mode after the camera is initialized.
            setEnvTextureData()

            StringBuilder().let {
                updateScreenText(frame, it)
                worldTextService.drawText(it)
            }

            // projectionMatrix、viewMatrix 4 * 4 matrix.
            val projectionMatrix = FloatArray(16)
            val viewMatrix = FloatArray(16)
            frame.camera.apply {
                // Do not perform anything when the object is not tracked.
                if (trackingState != ARTrackable.TrackingState.TRACKING) {
                    return
                }
                getProjectionMatrix(projectionMatrix, 0, Constants.PROJ_MATRIX_NEAR, Constants.PROJ_MATRIX_FAR)
                getViewMatrix(viewMatrix, 0)
            }.also {
                gestureController.handleGestureEvent(frame, it, projectionMatrix, viewMatrix)
            }
            drawTarget(frame.camera, viewMatrix, projectionMatrix)
            renderLabelAndObjects(frame, viewMatrix, projectionMatrix)
            pointCloudRenderer.renderPoints(frame.acquirePointCloud(), viewMatrix, projectionMatrix)
            getEnvironmentTexture(frame.lightEstimate)
        } catch (e: SampleAppException) {
            LogUtil.error(TAG, "Exception on the ArDemoRuntimeException!")
        } catch (t: Throwable) {
            LogUtil.error(TAG, "Exception on the OpenGL thread: $t")
        }
    }

    private fun drawTarget(camera: ARCamera, cameraView: FloatArray, cameraPerspective: FloatArray) {
        val allEntities = arSession?.getAllTrackables(ARTarget::class.java) ?: return
        if (camera.trackingState != ARTrackable.TrackingState.TRACKING) {
            LogUtil.debug(TAG, "ARCamera isn't TRACKING.")
            return
        }
        var targetRenderer: TargetRenderController
        for (target in allEntities) {
            if (target.trackingState != ARTrackable.TrackingState.TRACKING
                || target.shapeType == ARTarget.TargetShapeType.TARGET_SHAPE_INVALID) {
                continue
            }
            targetRenderer = targetRenderManager.getTargetRenderByType(target.shapeType)
            targetRenderer.apply {
                updateParameters(target)
                draw(cameraView, cameraPerspective)
            }
            val targetLabelDisplay = targetRenderManager.getTargetLabelDisplay()
            targetLabelDisplay.onDrawFrame(
                target, getTargetLabelBitmaps(targetRenderer.getTargetInfo())[0], camera,
                cameraPerspective)
        }
    }

    private fun renderLabelAndObjects(frame: ARFrame, viewMatrix: FloatArray, projectionMatrix: FloatArray) {
        val planes = arSession?.getAllTrackables(ARPlane::class.java) ?: return
        for (plane in planes) {
            if (plane.type != ARPlane.PlaneType.UNKNOWN_FACING
                && plane.trackingState == ARTrackable.TrackingState.TRACKING) {
                hideLoadingMessage()
                break
            }
        }
        labelService.onDrawFrame(planes, frame.camera.displayOrientedPose, projectionMatrix)
        drawAllObjects(projectionMatrix, viewMatrix, getPixelIntensity(frame.lightEstimate))
        return
    }

    /**
     * Draw a virtual object.
     *
     * @param projectionMatrix Projection matrix.
     * @param viewMatrix View matrix.
     * @param lightPixelIntensity Light intensity parameter.
     */
    private fun drawAllObjects(projectionMatrix: FloatArray, viewMatrix: FloatArray, lightPixelIntensity: Float) {
        val ite = gestureController.virtualObjects.iterator()
        while (ite.hasNext()) {
            val obj = ite.next()
            obj.getArAnchor().run {
                if (trackingState == ARTrackable.TrackingState.STOPPED) {
                    ite.remove()
                }
                if (trackingState == ARTrackable.TrackingState.TRACKING) {
                    objectService.renderObjects(viewMatrix, projectionMatrix, lightPixelIntensity, obj)
                }
            }
        }
    }

    private fun setEnvTextureData() {
        if (!mHaveSetEnvTextureData) {
            val boundBox: FloatArray = ObjectUtil.boundingBox
            arSession?.let {
                it.setEnvironmentTextureProbe(boundBox)
                it.setEnvironmentTextureUpdateMode(ARSession.EnvironmentTextureUpdateMode.AUTO)
            }
            mHaveSetEnvTextureData = true
        }
    }

    private fun getPixelIntensity(lightEstimate: ARLightEstimate): Float {
        var lightPixelIntensity = 1f

        // Obtain the pixel light intensity when the light intensity mode is enabled.
        if (mArWorldTrackingConfig.lightingMode and ARConfigBase.LIGHT_MODE_AMBIENT_INTENSITY != 0) {
            lightPixelIntensity = lightEstimate.pixelIntensity
        }
        return lightPixelIntensity
    }

    private fun getEnvironmentTexture(lightEstimate: ARLightEstimate) {
        if ((worldActivityBinding.searchingTextView.visibility != View.GONE)
            || lightEstimate.state != ARLightEstimate.State.VALID) {
            return
        }

        // Obtain the environment texture data when the environment texture mode is enabled.
        if (mArWorldTrackingConfig.lightingMode and ARConfigBase.LIGHT_MODE_ENVIRONMENT_TEXTURE == 0) {
            return
        }
        val byteBuffer = lightEstimate.acquireEnvironmentTexture() ?: return

        // Update the environment texture every 10 frames.
        if ((mUpdateIndex % 10) == 0) {
            val bytes = ByteArray(LIGHTING_CUBE_MAP_SIZE)
            byteBuffer[bytes]
            activity.runOnUiThread(Runnable {
                updateTextureDisplay(bytes)
            })
            mUpdateIndex = 0
        }
        mUpdateIndex++
    }

    private fun updateTextureDisplay(bytes: ByteArray) {
        val cubeMapImgView = arrayOf<ImageView>(
            worldActivityBinding.imgEnvTextureRight, worldActivityBinding.imgEnvTextureLeft,
            worldActivityBinding.imgEnvTextureTop, worldActivityBinding.imgEnvTextureBottom,
            worldActivityBinding.imgEnvTextureFront, worldActivityBinding.imgEnvTextureBack)

        // The environment texture is a cube mapping diagram, including six surfaces.
        // The side 0 indicates the right side, and the side 1 indicates the left side.
        // The side 2 indicates the top side, and the side 3 indicates the bottom side.
        // The side 4 indicates the front side, and the side 5 indicates the rear side.
        for (i in 0..5) {
            val cubeMapFace = createBitmapImage(
                bytes.copyOfRange(LIGHTING_CUBE_MAP_SINGLE_FACE_SIZE * i,
                    LIGHTING_CUBE_MAP_SINGLE_FACE_SIZE * (i + 1)), SIDE_LENGTH, SIDE_LENGTH)
            cubeMapFace.ifPresent(cubeMapImgView[i]::setImageBitmap)
        }
    }

    private fun getPlaneBitmaps(): ArrayList<Bitmap> {
        val bitmaps = ArrayList<Bitmap>()
        bitmaps.add(getPlaneBitmap(R.id.plane_other))
        bitmaps.add(getPlaneBitmap(R.id.plane_wall))
        bitmaps.add(getPlaneBitmap(R.id.plane_floor))
        bitmaps.add(getPlaneBitmap(R.id.plane_seat))
        bitmaps.add(getPlaneBitmap(R.id.plane_table))
        bitmaps.add(getPlaneBitmap(R.id.plane_ceiling))
        bitmaps.add(getPlaneBitmap(R.id.plane_door))
        bitmaps.add(getPlaneBitmap(R.id.plane_window))
        bitmaps.add(getPlaneBitmap(R.id.plane_bed))
        return bitmaps
    }

    private fun getTargetLabelBitmaps(textStr: String): ArrayList<Bitmap> {
        val bitmaps = ArrayList<Bitmap>(1)
        val view: TextView = activity.findViewById(R.id.target_measure)
        if (view == null) {
            LogUtil.error(TAG, "getTargetLabelBitmaps id invalid.")
            return bitmaps
        }
        if (!textStr.isEmpty()) {
            activity.runOnUiThread {
                kotlin.run {
                    view.text = textStr
                }
            }
        }
        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        view.isDrawingCacheEnabled = true
        view.destroyDrawingCache()
        view.buildDrawingCache()
        LogUtil.debug(TAG, "Image bitmap create start!")
        val matrix = Matrix()
        matrix.setScale(MATRIX_SCALE_SX, MATRIX_SCALE_SY)
        var bitmap = view.drawingCache
        if (bitmap != null) {
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
        LogUtil.debug(TAG, "Image bitmap create end!")
        bitmaps.add(bitmap)
        return bitmaps
    }

    private fun getPlaneBitmap(id: Int): Bitmap {
        val view: TextView = activity.findViewById(id)
        view.isDrawingCacheEnabled = true
        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        var bitmap = view.drawingCache
        val matrix = Matrix()
        matrix.setScale(MATRIX_SCALE_SX, MATRIX_SCALE_SY)
        if (bitmap != null) {
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
        return bitmap
    }

    private fun hideLoadingMessage() {
        activity.runOnUiThread {
            findViewById<TextView>(activity, R.id.searchingTextView).visibility = View.GONE
        }
    }

    /**
     * Set ARSession, which will update and obtain the latest data in OnDrawFrame.
     *
     * @param arSession ARSession.
     */
    fun setArSession(arSession: ARSession?) {
        this.arSession = arSession
    }

    /**
     * Set ARWorldTrackingConfig to obtain the configuration mode.
     *
     * @param arConfig ARWorldTrackingConfig.
     */
    fun setArWorldTrackingConfig(arConfig: ARWorldTrackingConfig) {
        if (arConfig == null) {
            LogUtil.error(TAG, "setArWorldTrackingConfig error, arConfig is null!")
            return
        }
        mArWorldTrackingConfig = arConfig
    }

    private fun updateScreenText(arFrame: ARFrame, sb: StringBuilder) {
        sb.append("FPS= ${calFps(fps)}").append(System.lineSeparator())
        val lightEstimate = arFrame.lightEstimate
        if (worldActivityBinding.searchingTextView.getVisibility() != View.GONE
            || lightEstimate.state != ARLightEstimate.State.VALID) {
            return
        }

        // Obtain the estimated light data when the light intensity mode is enabled.
        if (mArWorldTrackingConfig.lightingMode and ARConfigBase.LIGHT_MODE_AMBIENT_INTENSITY != 0) {
            sb.append("PixelIntensity=").append(lightEstimate.pixelIntensity).append(System.lineSeparator())
        }

        // Obtain the texture data when the environment texture mode is enabled.
        if (mArWorldTrackingConfig.lightingMode and ARConfigBase.LIGHT_MODE_ENVIRONMENT_LIGHTING != 0) {
            sb.append("PrimaryLightIntensity=").append(lightEstimate.primaryLightIntensity)
                .append(System.lineSeparator())
            sb.append("PrimaryLightDirection=").append(Arrays.toString(lightEstimate.primaryLightDirection))
                .append(System.lineSeparator())
            sb.append("PrimaryLightColor=").append(Arrays.toString(lightEstimate.primaryLightColor))
                .append(System.lineSeparator())
            sb.append("LightShadowType=").append(lightEstimate.lightShadowType).append(System.lineSeparator())
            sb.append("LightShadowStrength=").append(lightEstimate.shadowStrength).append(System.lineSeparator())
            sb.append("LightSphericalHarmonicCoefficients=")
                .append(Arrays.toString(lightEstimate.sphericalHarmonicCoefficients))
                .append(System.lineSeparator())
        }
    }
}
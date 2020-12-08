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
package com.huawei.arengine.demos.face.service

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.core.app.ActivityCompat
import com.huawei.arengine.demos.MainApplication
import com.huawei.arengine.demos.common.exception.SampleAppException
import java.io.Serializable
import java.lang.Long.signum
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

/**
 * Provide services related to the camera, including starting and stopping
 * the camera thread, and opening and closing the camera.
 *
 * @author HW
 * @since 2020-10-10
 */
class CameraService {
    companion object {
        private const val TAG = "CameraService"
    }

    init {
        startCameraThread()
    }

    private var mCameraDevice: CameraDevice? = null

    private var mCameraCaptureSession: CameraCaptureSession? = null

    private var mCameraThread: HandlerThread? = null

    private var mCameraHandler: Handler? = null

    private var mSurfaceTexture: SurfaceTexture? = null

    private var mVgaSurface: Surface? = null

    private var mDepthSurface: Surface? = null

    private var mPreViewSurface: Surface? = null

    private lateinit var mCameraId: String

    private lateinit var mPreviewSize: Size

    private lateinit var mCaptureRequestBuilder: CaptureRequest.Builder

    private val mCameraOpenCloseLock = Semaphore(1)

    private val mStateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            mCameraDevice = camera
            Log.i(TAG, "CameraDevice onOpened!")
            startPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            mCameraOpenCloseLock.release()
            camera.close()
            Log.i(TAG, "CameraDevice onDisconnected!")
            mCameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            mCameraOpenCloseLock.release()
            camera.close()
            Log.i(TAG, "CameraDevice onError!")
            mCameraDevice = null
        }
    }

    /**
     * Obtain the preview size and camera ID.
     *
     * @param width Device screen width, in pixels.
     * @param height Device screen height, in pixels.
     */
    fun setupCamera(width: Int, height: Int) {
        val cameraManager = MainApplication.context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val cameraLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (cameraLensFacing == null || cameraLensFacing != CameraCharacteristics.LENS_FACING_FRONT) {
                    continue
                }
                val maps = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                if (maps?.getOutputSizes(SurfaceTexture::class.java) == null) {
                    continue
                }
                mPreviewSize = getOptimalSize(maps.getOutputSizes(SurfaceTexture::class.java), width, height)
                mCameraId = id
                Log.i(TAG, "Preview width = " + mPreviewSize.width + ", height = "
                    + mPreviewSize.height + ", cameraId = " + mCameraId)
                break
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Set upCamera error")
        }
    }

    /**
     * Start the camera thread.
     */
    private fun startCameraThread() {
        mCameraThread = HandlerThread("CameraThread").apply {
            start()
            looper?.let {
                mCameraHandler = Handler(it)
            }
        }
    }

    /**
     * Close the camera thread.
     * This method will be called when [FaceActivity.onPause].
     */
    fun stopCameraThread() {
        mCameraThread?.quitSafely()
        try {
            mCameraThread?.join()
            mCameraThread = null
            mCameraHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "StopCameraThread error")
        }
    }

    /**
     * Launch the camera.
     *
     * @return Open success or failure.
     */
    fun openCamera(): Boolean {
        Log.i(TAG, "OpenCamera!")
        val cameraManager =
            if (MainApplication.context.getSystemService(Context.CAMERA_SERVICE) is CameraManager) {
                MainApplication.context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            } else {
                return false
            }
        try {
            if (ActivityCompat.checkSelfPermission(MainApplication.context, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
                return false
            }

            // 2500 is the maximum waiting time.
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw SampleAppException("Time out waiting to lock camera opening.")
            }
            cameraManager.openCamera(mCameraId, mStateCallback, mCameraHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "OpenCamera error.")
            return false
        } catch (e: InterruptedException) {
            Log.e(TAG, "OpenCamera error.")
            return false
        }
        return true
    }

    /**
     * Close the camera.
     */
    fun closeCamera() {
        try {
            mCameraOpenCloseLock.acquire()
            stopPreview()
            mCameraDevice?.close()
            mCameraDevice = null
        } catch (e: InterruptedException) {
            throw SampleAppException("Interrupted while trying to lock camera closing.")
        } finally {
            mCameraOpenCloseLock.release()
        }
    }

    private fun getOptimalSize(sizeMap: Array<Size>, width: Int, height: Int): Size {
        val max = Math.max(width, height)
        val min = Math.min(width, height)
        val sizeList: MutableList<Size> = ArrayList()
        for (option in sizeMap) {
            if (option.width > max && option.height > min) {
                sizeList.add(option)
            }
        }
        return if (sizeList.size == 0) {
            sizeMap[0]
        } else Collections.min(sizeList, CalculatedAreaDifference())
    }

    /**
     * Calculate the area difference.
     *
     * @author HW
     * @since 2020-10-10
     */
    internal class CalculatedAreaDifference : Comparator<Size>, Serializable {
        override fun compare(lhs: Size, rhs: Size): Int {
            return signum(lhs.width * lhs.height - rhs.width * rhs.height.toLong())
        }

        companion object {
            private const val serialVersionUID = 7710120461881073428L
        }
    }

    /**
     * Set the texture of the surface.
     *
     * @param surfaceTexture Surface texture.
     */
    fun setPreviewTexture(surfaceTexture: SurfaceTexture) {
        mSurfaceTexture = surfaceTexture
    }

    /**
     * Set the preview surface.
     *
     * @param surface Surface.
     */
    fun setPreViewSurface(surface: Surface?) {
        mPreViewSurface = surface
    }

    /**
     * Set the VGA surface.
     *
     * @param surface Surface
     */
    fun setVgaSurface(surface: Surface?) {
        mVgaSurface = surface
    }

    /**
     * Set the depth surface.
     *
     * @param surface Surface.
     */
    fun setDepthSurface(surface: Surface?) {
        mDepthSurface = surface
    }

    private fun startPreview() {
        Log.i(TAG, "StartPreview!")
        mSurfaceTexture?.setDefaultBufferSize(mPreviewSize.width, mPreviewSize.height)
        if (mCameraDevice == null) {
            Log.i(TAG, "mCameraDevice is null!")
            return
        }
        try {
            mCaptureRequestBuilder = mCameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)!!
            val surfaces: MutableList<Surface> = ArrayList()
            mPreViewSurface?.let {
                surfaces.add(it)
            }
            mVgaSurface?.let {
                surfaces.add(it)
            }
            mDepthSurface?.let {
                surfaces.add(it)
            }
            captureSession(surfaces)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "StartPreview error")
        }
    }

    private fun captureSession(surfaces: List<Surface>) {
        try {
            mCameraDevice?.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    try {
                        mPreViewSurface?.let {
                            mCaptureRequestBuilder.addTarget(it)
                        }
                        mVgaSurface?.let {
                            mCaptureRequestBuilder.addTarget(it)
                        }
                        mDepthSurface?.let {
                            mCaptureRequestBuilder.addTarget(it)
                        }

                        // Set the number of frames to 30.
                        val fpsRange = Range(30, 30)
                        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
                        val captureRequests: MutableList<CaptureRequest> = ArrayList()
                        captureRequests.add(mCaptureRequestBuilder.build())
                        mCameraCaptureSession = session
                        mCameraCaptureSession?.setRepeatingBurst(captureRequests, null, mCameraHandler)
                        mCameraOpenCloseLock.release()
                    } catch (e: CameraAccessException) {
                        Log.e(TAG, "CaptureSession onConfigured error")
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}
                override fun onClosed(session: CameraCaptureSession) {
                    Log.i(TAG, "CameraCaptureSession stopped!")
                }
            }, mCameraHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "CaptureSession error")
        }
    }

    private fun stopPreview() {
        Log.i(TAG, "Stop CameraCaptureSession begin!")
        mCameraCaptureSession?.close()
        mCameraCaptureSession = null
        Log.i(TAG, "Stop CameraCaptureSession end!")
    }
}
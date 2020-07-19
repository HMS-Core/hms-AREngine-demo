/**
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huawei.arengine.demos.java.face;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.huawei.arengine.demos.common.ArDemoRuntimeException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Provide services related to the camera, including starting and stopping
 * the camera thread, and opening and closing the camera.
 *
 * @author HW
 * @since 2020-03-15
 */
public class CameraHelper {
    private static final String TAG = CameraHelper.class.getSimpleName();

    private CameraDevice mCameraDevice;

    private CameraCaptureSession mCameraCaptureSession;

    private String mCameraId;

    private Surface mVgaSurface;

    private Activity mActivity;

    private Size mPreviewSize;

    private HandlerThread mCameraThread;

    private Handler mCameraHandler;

    private SurfaceTexture mSurfaceTexture;

    private Surface mDepthSurface;

    private CaptureRequest.Builder mCaptureRequestBuilder;

    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private Surface mPreViewSurface;

    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            Log.i(TAG, "CameraDevice onOpened!");
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            mCameraOpenCloseLock.release();
            camera.close();
            Log.i(TAG, "CameraDevice onDisconnected!");
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            mCameraOpenCloseLock.release();
            camera.close();
            Log.i(TAG, "CameraDevice onError!");
            mCameraDevice = null;
        }
    };

    /**
     * The constructor..
     *
     * @param activity Activity.
     */
    CameraHelper(Activity activity) {
        mActivity = activity;
        startCameraThread();
    }

    /**
     * Obtain the preview size and camera ID.
     *
     * @param width Device screen width, in pixels.
     * @param height Device screen height, in pixels.
     */
    void setupCamera(int width, int height) {
        CameraManager cameraManager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String id : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                Integer cameraLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (cameraLensFacing == null) {
                    continue;
                }
                if (cameraLensFacing != CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                StreamConfigurationMap maps =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (maps == null || maps.getOutputSizes(SurfaceTexture.class) == null) {
                    continue;
                }
                mPreviewSize = getOptimalSize(maps.getOutputSizes(SurfaceTexture.class), width, height);
                mCameraId = id;
                Log.i(TAG, "Preview width = " + mPreviewSize.getWidth() + ", height = "
                    + mPreviewSize.getHeight() + ", cameraId = " + mCameraId);
                break;
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Set upCamera error");
        }
    }

    /**
     * Start the camera thread.
     */
    private void startCameraThread() {
        mCameraThread = new HandlerThread("CameraThread");
        mCameraThread.start();
        if (mCameraThread.getLooper() != null) {
            Log.e(TAG, "startCameraThread mCameraThread.getLooper() null!");
            return;
        }
        mCameraHandler = new Handler(mCameraThread.getLooper());
    }

    /**
     * Close the camera thread.
     * This method will be called when {@link FaceActivity#onPause}.
     */
    void stopCameraThread() {
        mCameraThread.quitSafely();
        try {
            mCameraThread.join();
            mCameraThread = null;
            mCameraHandler = null;
        } catch (InterruptedException e) {
            Log.e(TAG, "StopCameraThread error");
        }
    }

    /**
     * Launch the camera.
     *
     * @return Open success or failure.
     */
    boolean openCamera() {
        Log.i(TAG, "OpenCamera!");
        CameraManager cameraManager = null;
        if (mActivity.getSystemService(Context.CAMERA_SERVICE) instanceof CameraManager) {
            cameraManager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        } else {
            return false;
        }
        try {
            if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }

            // 2500 is the maximum waiting time.
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new ArDemoRuntimeException("Time out waiting to lock camera opening.");
            }
            cameraManager.openCamera(mCameraId, mStateCallback, mCameraHandler);
        } catch (CameraAccessException | InterruptedException e) {
            Log.e(TAG, "OpenCamera error.");
            return false;
        }
        return true;
    }

    /**
     * Close the camera.
     */
    void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            Log.i(TAG, "Stop CameraCaptureSession begin!");
            stopPreview();
            Log.i(TAG, "Stop CameraCaptureSession stopped!");
            if (mCameraDevice != null) {
                Log.i(TAG, "Stop Camera!");
                mCameraDevice.close();
                mCameraDevice = null;
                Log.i(TAG, "Stop Camera stopped!");
            }
        } catch (InterruptedException e) {
            throw new ArDemoRuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    private Size getOptimalSize(Size[] sizeMap, int width, int height) {
        int max = Math.max(width, height);
        int min = Math.min(width, height);
        List<Size> sizeList = new ArrayList<>();
        for (Size option : sizeMap) {
            if (option.getWidth() > max && option.getHeight() > min) {
                sizeList.add(option);
            }
        }
        if (sizeList.size() == 0) {
            return sizeMap[0];
        }
        return Collections.min(sizeList, new CalculatedAreaDifference());
    }

    /**
     * Calculate the area difference.
     *
     * @author HW
     * @since 2020-03-15
     */
    static class CalculatedAreaDifference implements Comparator<Size>, Serializable {
        private static final long serialVersionUID = 7710120461881073428L;

        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
        }
    }

    /**
     * Set the texture of the surface.
     *
     * @param surfaceTexture Surface texture.
     */
    void setPreviewTexture(SurfaceTexture surfaceTexture) {
        mSurfaceTexture = surfaceTexture;
    }

    /**
     * Set the preview surface.
     *
     * @param surface Surface.
     */
    void setPreViewSurface(Surface surface) {
        mPreViewSurface = surface;
    }

    /**
     * Set the VGA surface.
     *
     * @param surface Surface
     */
    void setVgaSurface(Surface surface) {
        mVgaSurface = surface;
    }

    /**
     * Set the depth surface.
     *
     * @param surface Surface.
     */
    void setDepthSurface(Surface surface) {
        mDepthSurface = surface;
    }

    private void startPreview() {
        if (mSurfaceTexture == null) {
            Log.i(TAG, "mSurfaceTexture is null!");
            return;
        }
        Log.i(TAG, "StartPreview!");
        mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

        if (mCameraDevice == null) {
            Log.i(TAG, "mCameraDevice is null!");
            return;
        }
        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            List<Surface> surfaces = new ArrayList<Surface>();
            if (mPreViewSurface != null) {
                surfaces.add(mPreViewSurface);
            }
            if (mVgaSurface != null) {
                surfaces.add(mVgaSurface);
            }
            if (mDepthSurface != null) {
                surfaces.add(mDepthSurface);
            }
            captureSession(surfaces);
        } catch (CameraAccessException e) {
            Log.e(TAG, "StartPreview error");
        }
    }

    private void captureSession(List<Surface> surfaces) {
        try {
            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        if (mCameraDevice == null) {
                            Log.w(TAG, "CameraDevice stop!");
                            return;
                        }
                        if (mPreViewSurface != null) {
                            mCaptureRequestBuilder.addTarget(mPreViewSurface);
                        }
                        if (mVgaSurface != null) {
                            mCaptureRequestBuilder.addTarget(mVgaSurface);
                        }
                        if (mDepthSurface != null) {
                            mCaptureRequestBuilder.addTarget(mDepthSurface);
                        }

                        // Set the number of frames to 30.
                        Range<Integer> fpsRange = new Range<Integer>(30, 30);
                        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);
                        List<CaptureRequest> captureRequests = new ArrayList<>();
                        captureRequests.add(mCaptureRequestBuilder.build());
                        mCameraCaptureSession = session;
                        mCameraCaptureSession.setRepeatingBurst(captureRequests, null, mCameraHandler);
                        mCameraOpenCloseLock.release();
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "CaptureSession onConfigured error");
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                }

                @Override
                public void onClosed(@NonNull CameraCaptureSession session) {
                    Log.i(TAG, "CameraCaptureSession stopped!");
                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "CaptureSession error");
        }
    }

    private void stopPreview() {
        if (mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        } else {
            Log.i(TAG, "mCameraCaptureSession is null!");
        }
    }
}

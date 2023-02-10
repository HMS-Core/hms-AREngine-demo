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

package com.huawei.arengine.demos.java.face;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.huawei.arengine.demos.R;
import com.huawei.arengine.demos.common.BaseActivity;
import com.huawei.arengine.demos.common.ListDialog;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.arengine.demos.common.SecurityUtil;
import com.huawei.arengine.demos.java.face.rendering.FaceRendererManager;
import com.huawei.hiar.ARConfigBase;
import com.huawei.hiar.ARFaceTrackingConfig;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.exceptions.ARCameraNotAvailableException;
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException;

import java.util.ArrayList;
import java.util.List;

/**
 * This demo shows the capabilities of HUAWEI AR Engine to recognize faces, including facial
 * features and facial expressions. In addition, this demo shows how an app can open the camera
 * to display preview.Currently, only apps of the ARface type can open the camera. If you want
 * to the app to open the camera, set isOpenCameraOutside = true in this file.
 *
 * @author HW
 * @since 2020-03-18
 */
public class FaceActivity extends BaseActivity {
    private static final String TAG = FaceActivity.class.getSimpleName();

    private static final int MSG_OPEN_MULTI_FACE_MODE = 1;

    private static final int MSG_OPEN_SINGLE_FACE_WITH_LIGHT_MODE = 2;

    private static final long BUTTON_REPEAT_CLICK_INTERVAL_TIME = 1000L;

    private static final String OPEN_MULTI_FACE_MODE = "OpenMultiFaceMode";

    private static final String OPEN_SINGLE_FACE_WITH_LIGHT_MODE = "OpenSingleFaceWithLightMode";

    private static final float RATIO_4_TO_3 = (float) 4 / 3f;

    private static final float EPSINON = 0.000001f;

    private int mFaceMode = MSG_OPEN_SINGLE_FACE_WITH_LIGHT_MODE;

    private int mCameraLensFacing = CameraCharacteristics.LENS_FACING_FRONT;

    private FaceRendererManager mFaceRendererManager;

    private boolean isOpenCameraOutside = false;

    private CameraHelper mCamera;

    private Surface mPreViewSurface;

    private Surface mVgaSurface;

    private Surface mMetaDataSurface;

    private Surface mDepthSurface;

    private Button mButton;

    private Button mCameraFacingButton;

    private Button mRatioButton;

    private volatile boolean mIsCameraInit = false;

    private List<Size> mPreviewSizes;

    private List<String> mKeyList;

    private int mPreviewWidth;

    private int mPreviewHeight;

    /**
     * The initial texture ID is -1.
     */
    private int textureId = -1;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_OPEN_MULTI_FACE_MODE:
                    mButton.setText(OPEN_SINGLE_FACE_WITH_LIGHT_MODE);
                    break;
                case MSG_OPEN_SINGLE_FACE_WITH_LIGHT_MODE:
                    mButton.setText(OPEN_MULTI_FACE_MODE);
                    break;
                default:
                    LogUtil.info(TAG, "handleMessage default in FaceActivity.");
                    break;
            }
            mButton.setEnabled(true);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.face_activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mButton = findViewById(R.id.faceButton);
        mCameraFacingButton = findViewById(R.id.cameraFacingButton);
        mSurfaceView = findViewById(R.id.faceSurfaceview);
        mRatioButton = findViewById(R.id.ratioButton);
        getPreviewSizeList();
        initClickListener();
        initCameraFacingClickListener();

        mSurfaceView.setPreserveEGLContextOnPause(true);

        // Set the OpenGLES version.
        mSurfaceView.setEGLContextClientVersion(2);

        // Set the EGL configuration chooser, including for the
        // number of bits of the color buffer and the number of depth bits.
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        mFaceRendererManager = new FaceRendererManager(this);
        mFaceRendererManager.setDisplayRotationManager(mDisplayRotationManager);
        TextView textView = findViewById(R.id.faceTextView);
        mFaceRendererManager.setTextView(textView);

        mSurfaceView.setRenderer(mFaceRendererManager);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    private void initClickListener() {
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mButton.setEnabled(false);
                if (mArSession != null) {
                    mArSession.pause();
                    mArSession.stop();
                    mArSession = null;
                }
                stopCamera();
                if (mButton.getText().toString().equals(OPEN_MULTI_FACE_MODE)) {
                    mFaceMode = MSG_OPEN_MULTI_FACE_MODE;
                    setArConfig(mFaceMode, false);
                    mHandler.sendEmptyMessageDelayed(MSG_OPEN_MULTI_FACE_MODE, BUTTON_REPEAT_CLICK_INTERVAL_TIME);
                    return;
                }
                if (mButton.getText().toString().equals(OPEN_SINGLE_FACE_WITH_LIGHT_MODE)) {
                    mFaceMode = MSG_OPEN_SINGLE_FACE_WITH_LIGHT_MODE;
                    setArConfig(mFaceMode, false);
                    mHandler.sendEmptyMessageDelayed(MSG_OPEN_SINGLE_FACE_WITH_LIGHT_MODE,
                        BUTTON_REPEAT_CLICK_INTERVAL_TIME);
                }
            }
        });
    }

    private void initCameraFacingClickListener() {
        mCameraFacingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mIsCameraInit) {
                    LogUtil.debug(TAG, "camera preview not finish");
                    return;
                }
                if (mArSession != null) {
                    mArSession.pause();
                    mArSession.stop();
                    mArSession = null;
                }
                stopCamera();
                mCameraLensFacing = mCameraLensFacing == CameraCharacteristics.LENS_FACING_FRONT
                    ? CameraCharacteristics.LENS_FACING_BACK : CameraCharacteristics.LENS_FACING_FRONT;
                setArConfig(mFaceMode, false);
                getPreviewSizeList();
            }
        });

        mRatioButton.setOnClickListener(view -> showDialog());
    }

    private void showDialog() {
        if (mPreviewSizes == null || mPreviewSizes.size() == 0) {
            return;
        }
        ListDialog dialogUtils = new ListDialog();
        dialogUtils.setDialogOnItemClickListener(position -> {
            mPreviewWidth = mPreviewSizes.get(position).getWidth();
            mPreviewHeight = mPreviewSizes.get(position).getHeight();
            stopArSession();
            setArConfig(mFaceMode, true);
        });
        dialogUtils.showDialogList(FaceActivity.this, mKeyList);
    }

    private void getPreviewSizeList() {
        if (mCamera == null) {
            mCamera = new CameraHelper(this);
        }
        Size[] supportedPreviewSizes = mCamera.getPreviewSizeList(mCameraLensFacing);
        mPreviewSizes = new ArrayList<>();
        mKeyList = new ArrayList<>();
        for (Size option : supportedPreviewSizes) {
            if (Math.abs(option.getWidth() / (float) option.getHeight() - RATIO_4_TO_3) < EPSINON) {
                mPreviewSizes.add(option);
                mKeyList.add(option.getWidth() + "×" + option.getHeight());
            }
        }
    }

    private void setArConfig(int faceMode, boolean isModifyPreviewSize) {
        try {
            if (mArSession == null) {
                mArSession = new ARSession(this.getApplicationContext());
            }
            ARFaceTrackingConfig config = new ARFaceTrackingConfig(mArSession);
            config.setPowerMode(ARConfigBase.PowerMode.POWER_SAVING);
            if (isModifyPreviewSize) {
                config.setPreviewSize(mPreviewWidth, mPreviewHeight);
            }
            if (faceMode == MSG_OPEN_SINGLE_FACE_WITH_LIGHT_MODE) {
                config.setLightingMode(ARConfigBase.LIGHT_MODE_ENVIRONMENT_LIGHTING);
            } else {
                config.setFaceDetectMode(ARConfigBase.FaceDetectMode.FACE_ENABLE_MULTIFACE.getEnumValue()
                    | ARConfigBase.FaceDetectMode.FACE_ENABLE_DEFAULT.getEnumValue());
            }
            if (isOpenCameraOutside) {
                mArConfigBase.setImageInputMode(ARConfigBase.ImageInputMode.EXTERNAL_INPUT_ALL);
            }
            config.setCameraLensFacing(mCameraLensFacing == CameraCharacteristics.LENS_FACING_FRONT
                ? ARFaceTrackingConfig.CameraLensFacing.FRONT : ARFaceTrackingConfig.CameraLensFacing.REAR);
            mArConfigBase = config;
            mArSession.configure(mArConfigBase);
        } catch (Exception capturedException) {
            setMessageWhenError(capturedException);
        } finally {
            showCapabilitySupportInfo(faceMode);
        }
        if (errorMessage != null) {
            stopArSession();
            return;
        }
        try {
            mArSession.resume();
        } catch (ARCameraNotAvailableException capturedException) {
            Toast.makeText(this, "Camera open failed, please restart the app", Toast.LENGTH_LONG).show();
            mArSession = null;
            return;
        }
        setCamera();
        mFaceRendererManager.setArSession(mArSession);
        mFaceRendererManager.setArConfigBase(mArConfigBase);
        mDisplayRotationManager.onDisplayChanged(0);
        mIsCameraInit = false;
    }

    @Override
    protected void onResume() {
        LogUtil.debug(TAG, "onResume");
        super.onResume();
        setArConfig(mFaceMode, false);
        mDisplayRotationManager.registerDisplayListener();
        mFaceRendererManager.setOpenCameraOutsideFlag(isOpenCameraOutside);
        mFaceRendererManager.setTextureId(textureId);
        mSurfaceView.onResume();
    }

    private void setCamera() {
        if (!isOpenCameraOutside) {
            return;
        }
        if (mCamera == null) {
            LogUtil.info(TAG, "new Camera");
            DisplayMetrics dm = new DisplayMetrics();
            mCamera = new CameraHelper(this);
            mCamera.setupCamera(dm.widthPixels, dm.heightPixels, mCameraLensFacing);
        }

        // Check whether setCamera is called for the first time.
        if (textureId == -1) {
            int[] textureIds = new int[1];
            GLES20.glGenTextures(1, textureIds, 0);
            textureId = textureIds[0];
        }
        mArSession.setCameraTextureName(textureId);
        initSurface();

        SurfaceTexture surfaceTexture = new SurfaceTexture(textureId);
        mCamera.setPreviewTexture(surfaceTexture);
        mCamera.setPreViewSurface(mPreViewSurface);
        mCamera.setVgaSurface(mVgaSurface);
        mCamera.setDepthSurface(mDepthSurface);
        if (!mCamera.openCamera()) {
            String showMessage = "Open camera failed!";
            LogUtil.error(TAG, showMessage);
            Toast.makeText(this, showMessage, Toast.LENGTH_LONG).show();
            SecurityUtil.safeFinishActivity(this);
        }
    }

    private void stopCamera() {
        if (!isOpenCameraOutside) {
            return;
        }
        if (mCamera != null) {
            LogUtil.info(TAG, "Stop camera start.");
            mCamera.closeCamera();
            mCamera.stopCameraThread();
            mCamera = null;
            LogUtil.info(TAG, "Stop camera end.");
        }
    }

    private void initSurface() {
        List<ARConfigBase.SurfaceType> surfaceTypeList = mArConfigBase.getImageInputSurfaceTypes();
        List<Surface> surfaceList = mArConfigBase.getImageInputSurfaces();

        LogUtil.info(TAG, "surfaceList size : " + surfaceList.size());
        int size = surfaceTypeList.size();
        for (int i = 0; i < size; i++) {
            ARConfigBase.SurfaceType type = surfaceTypeList.get(i);
            Surface surface = surfaceList.get(i);
            if (ARConfigBase.SurfaceType.PREVIEW.equals(type)) {
                mPreViewSurface = surface;
            } else if (ARConfigBase.SurfaceType.VGA.equals(type)) {
                mVgaSurface = surface;
            } else if (ARConfigBase.SurfaceType.METADATA.equals(type)) {
                mMetaDataSurface = surface;
            } else if (ARConfigBase.SurfaceType.DEPTH.equals(type)) {
                mDepthSurface = surface;
            } else {
                LogUtil.info(TAG, "Unknown type.");
            }
            LogUtil.info(TAG, "list[" + i + "] get surface : " + surface + ", type : " + type);
        }
    }

    @Override
    protected void onDestroy() {
        LogUtil.info(TAG, "onDestroy start.");
        super.onDestroy();
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        LogUtil.info(TAG, "onDestroy end.");
    }

    /**
     * Reset the camera status to indicate that the process from camera initialization to preview is complete.
     */
    public void resetCameraStatus() {
        mIsCameraInit = true;
    }

    /**
     * For HUAWEI AR Engine 3.18 or later versions, you can call configure of ARSession, then call getLightingMode and
     * getFaceDetectMode respectively to check whether the current device supports ambient light detection and
     * multi-face recognition.
     *
     * @param faceMode Facial recognition mode.
     */
    private void showCapabilitySupportInfo(int faceMode) {
        if (mArConfigBase == null) {
            LogUtil.warn(TAG, "showCapabilitySupportInfo arConfigBase is null.");
            return;
        }

        String toastStr = "";
        try {
            if (faceMode == MSG_OPEN_SINGLE_FACE_WITH_LIGHT_MODE) {
                toastStr = (mArConfigBase.getLightingMode() & ARConfigBase.LIGHT_MODE_ENVIRONMENT_LIGHTING) == 0
                    ? "The device does not support LIGHT_MODE_ENVIRONMENT_LIGHTING." : "";
            }
            if (faceMode != MSG_OPEN_SINGLE_FACE_WITH_LIGHT_MODE && mArConfigBase instanceof ARFaceTrackingConfig) {
                toastStr = (((ARFaceTrackingConfig) mArConfigBase).getFaceDetectMode()
                    & ARConfigBase.FaceDetectMode.FACE_ENABLE_MULTIFACE.getEnumValue()) == 0
                        ? "The device does not support FACE_ENABLE_MULTIFACE." : "";
            }
        } catch (ARUnavailableServiceApkTooOldException capturedException) {
            LogUtil.debug(TAG, "show capability support info has exception:" + capturedException.getClass());
        }

        if (toastStr.isEmpty()) {
            return;
        }
        Toast.makeText(this, toastStr, Toast.LENGTH_LONG).show();
    }
}
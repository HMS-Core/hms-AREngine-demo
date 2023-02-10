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

package com.huawei.arengine.demos.java.face.rendering;

import android.app.Activity;

import com.huawei.arengine.demos.common.ArDemoRuntimeException;
import com.huawei.arengine.demos.common.BaseRendererManager;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.arengine.demos.java.face.FaceActivity;
import com.huawei.hiar.ARConfigBase;
import com.huawei.hiar.ARFace;
import com.huawei.hiar.ARFrame;
import com.huawei.hiar.ARLightEstimate;
import com.huawei.hiar.ARPose;
import com.huawei.hiar.ARTrackable.TrackingState;
import com.huawei.hiar.exceptions.ARDeadlineExceededException;
import com.huawei.hiar.exceptions.ARFatalException;
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException;

import java.util.Arrays;
import java.util.Collection;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * This class manages rendering related to facial data.
 *
 * @author HW
 * @since 2020-03-21
 */
public class FaceRendererManager extends BaseRendererManager implements BaseRendererManager.BaseRenderer {
    private static final String TAG = FaceRendererManager.class.getSimpleName();

    private ARConfigBase mArConfigBase;

    private boolean isOpenCameraOutside = true;

    /**
     * Initialize the texture ID.
     */
    private int mTextureId = -1;

    private FaceGeometryDisplay mFaceGeometryDisplay = new FaceGeometryDisplay();

    /**
     * The constructor initializes context and activity.
     * This method will be called when {@link Activity#onCreate}.
     *
     * @param activity Activity
     */
    public FaceRendererManager(Activity activity) {
        mActivity = activity;
        setRenderer(this);
    }

    /**
     * Set ARConfigBase to obtain the configuration mode.
     *
     * @param arConfig ARConfigBase。
     */
    public void setArConfigBase(ARConfigBase arConfig) {
        if (arConfig == null) {
            LogUtil.error(TAG, "setArFaceTrackingConfig error, arConfig is null.");
            return;
        }
        mArConfigBase = arConfig;
    }

    /**
     * Set the external camera open flag. If the value is true, the app opens the camera
     * by itself and creates a texture ID during background rendering. Otherwise, the camera
     * is opened by AR Engine. This method is called when {@link Activity#onResume}.
     *
     * @param isOpenCameraOutsideFlag Flag indicating the mode of opening the camera.
     */
    public void setOpenCameraOutsideFlag(boolean isOpenCameraOutsideFlag) {
        isOpenCameraOutside = isOpenCameraOutsideFlag;
    }

    /**
     * Set the texture ID for background rendering. This method will be called when {@link Activity#onResume}.
     *
     * @param textureId Texture ID.
     */
    public void setTextureId(int textureId) {
        mTextureId = textureId;
    }

    @Override
    public void surfaceCreated(GL10 gl, EGLConfig config) {
        if (isOpenCameraOutside) {
            mBackgroundDisplay.init(mTextureId);
        } else {
            mBackgroundDisplay.init();
        }
        LogUtil.info(TAG, "On surface created textureId= " + mTextureId);

        mFaceGeometryDisplay.init(mActivity);
    }

    @Override
    public void surfaceChanged(GL10 unused, int width, int height) {
    }

    @Override
    public void drawFrame(GL10 unused) {
        try {
            resetCameraStatus();
            Collection<ARFace> faces = mSession.getAllTrackables(ARFace.class);
            if (faces.size() == 0) {
                mTextDisplay.onDrawFrame("");
                return;
            }
            LogUtil.debug(TAG, "Face number: " + faces.size());
            for (ARFace face : faces) {
                if (face.getTrackingState() == TrackingState.TRACKING) {
                    mFaceGeometryDisplay.onDrawFrame(mArCamera, face);
                }
            }
            StringBuilder sb = new StringBuilder();
            float fpsResult = doFpsCalculate();
            updateMessageData(sb, fpsResult, faces, mArFrame);
            mTextDisplay.onDrawFrame(sb.toString());
        } catch (ArDemoRuntimeException e) {
            LogUtil.error(TAG, "Exception on the ArDemoRuntimeException!");
        } catch (ARFatalException | IllegalArgumentException | ARDeadlineExceededException |
            ARUnavailableServiceApkTooOldException t) {
            // This prevents the app from crashing due to unhandled exceptions.
            LogUtil.error(TAG, "Exception on the OpenGL thread, " + t.getClass());
        }
    }

    private void updateMessageData(StringBuilder sb, float fpsResult, Collection<ARFace> faces, ARFrame frame) {
        sb.append("FPS= ").append(fpsResult).append(System.lineSeparator());
        int index = 1;
        for (ARFace face : faces) {
            if (face.getTrackingState() != TrackingState.TRACKING) {
                continue;
            }
            ARPose pose = face.getPose();
            if (pose == null) {
                continue;
            }
            sb.append("face " + index + " pose information:");
            sb.append("face pose tx:[").append(pose.tx()).append("]").append(System.lineSeparator());
            sb.append("face pose ty:[").append(pose.ty()).append("]").append(System.lineSeparator());
            sb.append("face pose tz:[").append(pose.tz()).append("]").append(System.lineSeparator());
            sb.append(System.lineSeparator());

            float[] textureCoordinates = face.getFaceGeometry().getTextureCoordinates().array();
            sb.append("textureCoordinates length:[ ").append(textureCoordinates.length).append(" ]");
            sb.append(System.lineSeparator()).append(System.lineSeparator());
            index++;

            ARLightEstimate lightEstimate = frame.getLightEstimate();
            if (lightEstimate == null) {
                LogUtil.warn(TAG, "lightEstimate is null.");
                continue;
            }

            // Obtain the data of main light source and ambient light
            // when the ambient lighting estimation mode is enabled.
            if ((mArConfigBase.getLightingMode() & ARConfigBase.LIGHT_MODE_ENVIRONMENT_LIGHTING) != 0) {
                sb.append("PrimaryLightIntensity=")
                    .append(lightEstimate.getPrimaryLightIntensity())
                    .append(System.lineSeparator());
                sb.append("PrimaryLightDirection=")
                    .append(Arrays.toString(lightEstimate.getPrimaryLightDirection()))
                    .append(System.lineSeparator());
                sb.append("LightSphericalHarmonicCoefficients=")
                    .append(Arrays.toString(lightEstimate.getSphericalHarmonicCoefficients()))
                    .append(System.lineSeparator());
            }
        }
    }

    private void resetCameraStatus() {
        if (mActivity instanceof FaceActivity) {
            ((FaceActivity) mActivity).resetCameraStatus();
        }
    }
}
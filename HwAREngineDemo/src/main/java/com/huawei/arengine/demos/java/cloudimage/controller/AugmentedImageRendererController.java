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

package com.huawei.arengine.demos.java.cloudimage.controller;

import android.app.Activity;
import android.opengl.Matrix;
import android.util.Log;

import com.huawei.arengine.demos.common.ArDemoRuntimeException;
import com.huawei.arengine.demos.common.BaseRendererManager;
import com.huawei.arengine.demos.common.ImageRendererManager;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.arengine.demos.java.cloudimage.service.ImageBoxRenderService;
import com.huawei.hiar.ARAugmentedImage;
import com.huawei.hiar.ARTrackable;
import com.huawei.hiar.exceptions.ARDeadlineExceededException;
import com.huawei.hiar.exceptions.ARFatalException;
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * cloud-enhanced image rendering controller, configured to render an augmented image.
 *
 * @author HW
 * @since 2021-08-24
 */
public class AugmentedImageRendererController extends ImageRendererManager
    implements BaseRendererManager.BaseRenderer, ImageRendererManager.OnDrawImageListener {
    private static final String TAG = AugmentedImageRendererController.class.getSimpleName();

    private ImageBoxRenderService mImageBoxRenderService = new ImageBoxRenderService();

    /**
     * The constructor passes context and activity. This method will be called when {@link Activity#onCreate}.
     *
     * @param activity Activity
     */
    public AugmentedImageRendererController(Activity activity) {
        mActivity = activity;
        setDrawImageListener(this);
        setRenderer(this);
    }

    @Override
    public void surfaceCreated(GL10 gl, EGLConfig config) {
        mImageBoxRenderService.init();
    }

    @Override
    public void surfaceChanged(GL10 gl, int width, int height) {
    }

    @Override
    public void drawFrame(GL10 gl) {
        try {
            // If not tracking, don't draw image.
            if (mArCamera.getTrackingState() == ARTrackable.TrackingState.PAUSED) {
                Log.i(TAG, "draw background PAUSED!");
                return;
            }

            if (mIsImageTrackOnly) {
                Matrix.setIdentityM(mViewMatrix, 0);
            }

            // Visualize augmented images.
            drawAugmentedImages(mArFrame, mProjectionMatrix, mViewMatrix);
        } catch (ArDemoRuntimeException e) {
            LogUtil.error(TAG, "Exception on the ArDemoRuntimeException!");
        } catch (ARFatalException | IllegalArgumentException | ARDeadlineExceededException |
            ARUnavailableServiceApkTooOldException t) {
            // This prevents the app from crashing due to unhandled exceptions.
            LogUtil.error(TAG, "Exception on the OpenGL thread.");
        }
    }

    @Override
    public void drawImage(ARAugmentedImage augmentedImage, float[] viewMatrix, float[] projectionMatrix) {
        mImageBoxRenderService.drawImageBox(augmentedImage, viewMatrix, projectionMatrix);
    }
}
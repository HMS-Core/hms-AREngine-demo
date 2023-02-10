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

package com.huawei.arengine.demos.java.augmentedimage.rendering;

import android.app.Activity;
import android.opengl.Matrix;

import com.huawei.arengine.demos.common.BaseRendererManager;
import com.huawei.arengine.demos.common.ImageRendererManager;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.hiar.ARAugmentedImage;
import com.huawei.hiar.ARTrackable;

import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Augmented image rendering manager, configured to render the image.
 *
 * @author HW
 * @since 2021-02-04
 */
public class AugmentedImageRendererManager extends ImageRendererManager
    implements BaseRendererManager.BaseRenderer, ImageRendererManager.OnDrawImageListener {
    private static final String TAG = AugmentedImageRendererManager.class.getSimpleName();

    private ArrayList<AugmentedImageComponentDisplay> mImageRelatedDisplays = new ArrayList<>();

    /**
     * Pass the activity.
     *
     * @param activity activity.
     */
    public AugmentedImageRendererManager(Activity activity) {
        AugmentedImageComponentDisplay imageKeyPointDisplay = new ImageKeyPointDisplay();
        AugmentedImageComponentDisplay imageKeyLineDisplay = new ImageKeyLineDisplay();
        AugmentedImageComponentDisplay imageLabelDisplay = new ImageLabelDisplay(activity);
        mImageRelatedDisplays.add(imageKeyPointDisplay);
        mImageRelatedDisplays.add(imageKeyLineDisplay);
        mImageRelatedDisplays.add(imageLabelDisplay);
        setDrawImageListener(this);
        setRenderer(this);
    }

    @Override
    public void surfaceCreated(GL10 gl, EGLConfig config) {
        for (AugmentedImageComponentDisplay imageRelatedDisplay : mImageRelatedDisplays) {
            imageRelatedDisplay.init();
        }
    }

    @Override
    public void surfaceChanged(GL10 gl, int width, int height) {
    }

    @Override
    public void drawFrame(GL10 gl) {
        // If tracking is not set, the augmented image is not drawn.
        if (mArCamera.getTrackingState() == ARTrackable.TrackingState.PAUSED) {
            LogUtil.info(TAG, "Draw background paused!");
            return;
        }

        if (mIsImageTrackOnly) {
            Matrix.setIdentityM(mViewMatrix, 0);
        }

        // Draw the augmented image.
        drawAugmentedImages(mArFrame, mProjectionMatrix, mViewMatrix);
    }

    @Override
    public void drawImage(ARAugmentedImage augmentedImage, float[] viewMatrix, float[] projectionMatrix) {
        for (AugmentedImageComponentDisplay imageRelatedDisplay : mImageRelatedDisplays) {
            imageRelatedDisplay.onDrawFrame(augmentedImage, viewMatrix, projectionMatrix);
        }
    }
}
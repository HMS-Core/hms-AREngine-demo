/**
 * Copyright 2021. Huawei Technologies Co., Ltd. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.arengine.demos.java.augmentedimage.rendering;

import android.app.Activity;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Pair;

import com.huawei.arengine.demos.common.DisplayRotationManager;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.arengine.demos.common.TextureDisplay;
import com.huawei.hiar.ARAnchor;
import com.huawei.hiar.ARAugmentedImage;
import com.huawei.hiar.ARCamera;
import com.huawei.hiar.ARFrame;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.ARTrackable;
import com.huawei.hiar.exceptions.ARSessionPausedException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Augmented image rendering manager, configured to render the image.
 *
 * @author HW
 * @since 2021-02-04
 */
public class AugmentedImageRenderManager implements GLSurfaceView.Renderer {
    private static final String TAG = AugmentedImageRenderManager.class.getSimpleName();

    private static final int PROJ_MATRIX_SIZE = 16;

    private static final int PROJ_MATRIX_OFFSET = 0;

    private static final float PROJ_MATRIX_NEAR = 0.1f;

    private static final float PROJ_MATRIX_FAR = 100.0f;

    private boolean isImageTrackOnly = false;

    private ArrayList<AugmentedImageComponentDisplay> mImageRelatedDisplays = new ArrayList<>();

    private TextureDisplay backgroundDisplay = new TextureDisplay();

    private DisplayRotationManager mDisplayRotationManager;

    private ARSession mSession;

    private Activity mActivity;

    /**
     * Anchors of the augmented image and its related center,
     * which is controlled by the index key of the augmented image in the database.
     */
    private Map<Integer, Pair<ARAugmentedImage, ARAnchor>> augmentedImageMap = new HashMap<>();

    /**
     * Pass the activity.
     *
     * @param activity activity.
     */
    public AugmentedImageRenderManager(Activity activity) {
        mActivity = activity;
        AugmentedImageComponentDisplay imageKeyPointDisplay = new ImageKeyPointDisplay();
        AugmentedImageComponentDisplay imageKeyLineDisplay = new ImageKeyLineDisplay();
        AugmentedImageComponentDisplay imageLabelDisplay = new ImageLabelDisplay(mActivity);
        mImageRelatedDisplays.add(imageKeyPointDisplay);
        mImageRelatedDisplays.add(imageKeyLineDisplay);
        mImageRelatedDisplays.add(imageLabelDisplay);
    }

    /**
     * Set the DisplayRotationManager object, which is used in onSurfaceChanged and onDrawFrame.
     *
     * @param displayRotationManager Customized object.
     */
    public void setDisplayRotationManager(DisplayRotationManager displayRotationManager) {
        mDisplayRotationManager = displayRotationManager;
    }

    /**
     * Set the ARSession, which updates and obtains the latest data from OnDrawFrame.
     *
     * @param arSession ARSession.
     */
    public void setArSession(ARSession arSession) {
        mSession = arSession;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Set the window color.
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        backgroundDisplay.init();
        for (AugmentedImageComponentDisplay imageRelatedDisplay : mImageRelatedDisplays) {
            imageRelatedDisplay.init();
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        backgroundDisplay.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
        mDisplayRotationManager.updateViewportRotation(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        if (mSession == null) {
            return;
        }

        if (mDisplayRotationManager.getDeviceRotation()) {
            mDisplayRotationManager.updateArSessionDisplayGeometry(mSession);
        }
        mSession.setCameraTextureName(backgroundDisplay.getExternalTextureId());
        ARFrame arFrame;
        try {
            arFrame = mSession.update();
        } catch (ARSessionPausedException e) {
            LogUtil.error(TAG, "Invoke session.resume before invoking Session.update.");
            return;
        }
        ARCamera arCamera = arFrame.getCamera();
        backgroundDisplay.onDrawFrame(arFrame);

        // If tracking is not set, the augmented image is not drawn.
        if (arCamera.getTrackingState() == ARTrackable.TrackingState.PAUSED) {
            LogUtil.info(TAG, "Draw background paused!");
            return;
        }

        // Obtain the projection matrix.
        float[] projectionMatrix = new float[PROJ_MATRIX_SIZE];
        arCamera.getProjectionMatrix(projectionMatrix, PROJ_MATRIX_OFFSET, PROJ_MATRIX_NEAR, PROJ_MATRIX_FAR);

        // Obtain the view matrix.
        float[] viewMatrix = new float[PROJ_MATRIX_SIZE];
        if (isImageTrackOnly) {
            Matrix.setIdentityM(viewMatrix, 0);
        } else {
            arCamera.getViewMatrix(viewMatrix, 0);
        }

        // Draw the augmented image.
        drawAugmentedImages(arFrame, projectionMatrix, viewMatrix);
    }

    private void drawAugmentedImages(ARFrame frame, float[] projmtx, float[] viewmtx) {
        Collection<ARAugmentedImage> updatedAugmentedImages = frame.getUpdatedTrackables(ARAugmentedImage.class);
        LogUtil.debug(TAG, "drawAugmentedImages: Updated augment image is " + updatedAugmentedImages.size());

        // Iteratively update the augmented image mapping and remove the elements that cannot be drawn.
        for (ARAugmentedImage augmentedImage : updatedAugmentedImages) {
            switch (augmentedImage.getTrackingState()) {
                case PAUSED:
                    // When an image is in paused state but the camera is not paused,
                    // the image is detected but not tracked.
                    break;
                case TRACKING:
                    initTrackingImages(augmentedImage);
                    break;
                case STOPPED:
                    augmentedImageMap.remove(augmentedImage.getIndex());
                    break;
                default:
                    break;
            }
        }

        // Map the anchor to the AugmentedImage object and draw all augmentation effects.
        for (Pair<ARAugmentedImage, ARAnchor> pair : augmentedImageMap.values()) {
            ARAugmentedImage augmentedImage = pair.first;
            if (augmentedImage.getTrackingState() != ARTrackable.TrackingState.TRACKING) {
                continue;
            }
            for (AugmentedImageComponentDisplay imageRelatedDisplay : mImageRelatedDisplays) {
                imageRelatedDisplay.onDrawFrame(augmentedImage, viewmtx, projmtx);
            }
        }
    }

    private void initTrackingImages(ARAugmentedImage augmentedImage) {
        // Create an anchor for the newly found image and bind it to the image object.
        if (!augmentedImageMap.containsKey(augmentedImage.getIndex())) {
            ARAnchor centerPoseAnchor = null;
            if (!isImageTrackOnly) {
                centerPoseAnchor = augmentedImage.createAnchor(augmentedImage.getCenterPose());
            }
            augmentedImageMap.put(augmentedImage.getIndex(), Pair.create(augmentedImage, centerPoseAnchor));
        }
    }

    public void setImageTrackOnly(boolean isOnlyImageTrack) {
        this.isImageTrackOnly = isOnlyImageTrack;
    }
}
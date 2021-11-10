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

package com.huawei.arengine.demos.java.cloudimage.controller;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.huawei.arengine.demos.common.ArDemoRuntimeException;
import com.huawei.arengine.demos.common.DisplayRotationManager;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.arengine.demos.common.TextureDisplay;
import com.huawei.arengine.demos.java.cloudimage.service.ImageBoxRenderService;
import com.huawei.hiar.ARAnchor;
import com.huawei.hiar.ARAugmentedImage;
import com.huawei.hiar.ARCamera;
import com.huawei.hiar.ARFrame;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.ARTrackable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * cloud-enhanced image rendering controller, configured to render an augmented image.
 *
 * @author HW
 * @since 2021-08-24
 */
public class AugmentedImageRenderController implements GLSurfaceView.Renderer {
    private static final String TAG = AugmentedImageRenderController.class.getSimpleName();

    private static final int PROJ_MATRIX_SIZE = 16;

    private static final int PROJ_MATRIX_OFFSET = 0;

    private static final float PROJ_MATRIX_NEAR = 0.1f;

    private static final float PROJ_MATRIX_FAR = 100.0f;

    private static final int LIST_MAX_SIZE = 10;

    private Activity mActivity;

    private Context mContext;

    private ImageBoxRenderService mImageBoxRenderService = new ImageBoxRenderService();

    private DisplayRotationManager mDisplayRotationManager;

    private TextureDisplay mBackgroundDisplay = new TextureDisplay();

    private ARSession mSession;

    private String mCurrentImageId = "";

    private boolean mIsImageTrackOnly = false;

    /**
     * Augmented image and its associated center pose anchor, keyed by index of the augmented image in the database.
     */
    private Map<Integer, Pair<ARAugmentedImage, ARAnchor>> augmentedImageMap = new HashMap<>(LIST_MAX_SIZE);

    /**
     * The constructor passes context and activity. This method will be called when {@link Activity#onCreate}.
     *
     * @param activity Activity
     * @param context Context
     */
    public AugmentedImageRenderController(Activity activity, Context context) {
        mActivity = activity;
        mContext = context;
    }

    /**
     * Set the DisplayRotationManager object, which will be used in onSurfaceChanged and onDrawFrame.
     *
     * @param displayRotationManager DisplayRotationManage is customized object.
     */
    public void setDisplayRotationManager(DisplayRotationManager displayRotationManager) {
        mDisplayRotationManager = displayRotationManager;
    }

    /**
     * Set ARSession, which will update and obtain the latest data in OnDrawFrame.
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
        mBackgroundDisplay.init();
        mImageBoxRenderService.init();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mBackgroundDisplay.onSurfaceChanged(width, height);
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
        try {
            mSession.setCameraTextureName(mBackgroundDisplay.getExternalTextureId());
            ARFrame arFrame = mSession.update();
            ARCamera arCamera = arFrame.getCamera();
            mBackgroundDisplay.onDrawFrame(arFrame);

            // If not tracking, don't draw image.
            if (arCamera.getTrackingState() == ARTrackable.TrackingState.PAUSED) {
                Log.i(TAG, "draw background PAUSED!");
                return;
            }

            // Get projection matrix.
            float[] projectionMatrix = new float[PROJ_MATRIX_SIZE];
            arCamera.getProjectionMatrix(projectionMatrix, PROJ_MATRIX_OFFSET, PROJ_MATRIX_NEAR, PROJ_MATRIX_FAR);

            // Get view matrix and draw.
            float[] viewMatrix = new float[PROJ_MATRIX_SIZE];
            if (mIsImageTrackOnly) {
                Matrix.setIdentityM(viewMatrix, 0);
            } else {
                arCamera.getViewMatrix(viewMatrix, 0);
            }

            // Visualize augmented images.
            drawAugmentedImages(arFrame, projectionMatrix, viewMatrix);
        } catch (ArDemoRuntimeException e) {
            LogUtil.error(TAG, "Exception on the ArDemoRuntimeException!");
        } catch (Throwable t) {
            // This prevents the app from crashing due to unhandled exceptions.
            LogUtil.error(TAG, "Exception on the OpenGL thread.");
        }
    }

    private void drawAugmentedImages(ARFrame frame, float[] projmtx, float[] viewmtx) {
        Collection<ARAugmentedImage> updatedAugmentedImages = frame.getUpdatedTrackables(ARAugmentedImage.class);
        Log.d(TAG, "drawAugmentedImages: updated Augment image is " + updatedAugmentedImages.size());

        // Iterate to update augmentedImageMap, remove elements we cannot draw.
        for (ARAugmentedImage augmentedImage : updatedAugmentedImages) {
            switch (augmentedImage.getTrackingState()) {
                case PAUSED:
                    // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
                    // but not yet tracked.
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

        // Draw all images in augmentedImageMap
        for (Pair<ARAugmentedImage, ARAnchor> pair : augmentedImageMap.values()) {
            ARAugmentedImage augmentedImage = pair.first;
            if (augmentedImage.getTrackingState() == ARTrackable.TrackingState.TRACKING) {
                mImageBoxRenderService.drawImageBox(augmentedImage, viewmtx, projmtx);
            }
        }
    }

    private void initTrackingImages(ARAugmentedImage augmentedImage) {
        // Create new anchor for newly found images.
        if (!augmentedImageMap.containsKey(augmentedImage.getIndex())) {
            ARAnchor centerPoseAnchor = null;
            if (!mIsImageTrackOnly) {
                centerPoseAnchor = augmentedImage.createAnchor(augmentedImage.getCenterPose());
            }
            augmentedImageMap.put(augmentedImage.getIndex(), Pair.create(augmentedImage, centerPoseAnchor));
        }
        if (!mCurrentImageId.equals(augmentedImage.getCloudImageId())) {
            mCurrentImageId = augmentedImage.getCloudImageId();
            final String tipsMsg = augmentedImage.getCloudImageMetadata();
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, tipsMsg, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Set the type of the tracked object. If the value is true, only the image object is tracked.
     *
     * @param isOnlyImageTrack Boolean variable. If the value is true, only the image object is tracked.
     */
    public void setImageTrackOnly(boolean isOnlyImageTrack) {
        this.mIsImageTrackOnly = isOnlyImageTrack;
    }
}
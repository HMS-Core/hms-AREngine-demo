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

package com.huawei.arengine.demos.common;

import android.text.TextUtils;
import android.util.Pair;
import android.widget.Toast;

import com.huawei.hiar.ARAnchor;
import com.huawei.hiar.ARAugmentedImage;
import com.huawei.hiar.ARFrame;
import com.huawei.hiar.ARTrackable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 2D image recognition and rendering base class.
 *
 * @author HW
 * @since 2022-09-02
 */
public class ImageRendererManager extends BaseRendererManager {
    private static final String TAG = "ImageRendererManager";

    private static final int LIST_MAX_SIZE = 10;

    /**
     * Enable or disable the image tracking capability.
     */
    protected boolean mIsImageTrackOnly = false;

    /**
     * Anchors of the augmented image and its related center,
     * which is controlled by the index key of the augmented image in the database.
     */
    protected Map<Integer, Pair<ARAugmentedImage, ARAnchor>> augmentedImageMap = new HashMap<>(LIST_MAX_SIZE);

    private OnDrawImageListener mDrawImageListener = null;

    private String mCurrentImageId = "";

    /**
     * Obtain the image drawing listener of the augmented image rendering class.
     *
     * @param listener Image drawing listener.
     */
    protected void setDrawImageListener(OnDrawImageListener listener) {
        if (listener == null) {
            LogUtil.error(TAG, "listener was not initialized.");
            return;
        }
        mDrawImageListener = listener;
    }

    /**
     * Augmented image drawing method.
     *
     * @param frame A snapshot of the AR Engine system.
     * @param projmtx Model projection matrix under the orthographic camera.
     * @param viewmtx View transformation matrix under the orthographic camera.
     */
    protected void drawAugmentedImages(ARFrame frame, float[] projmtx, float[] viewmtx) {
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
                    showTrackingImageInfo(augmentedImage);
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
            if (mDrawImageListener != null) {
                mDrawImageListener.drawImage(augmentedImage, viewmtx, projmtx);
            }
        }
    }

    private void initTrackingImages(ARAugmentedImage augmentedImage) {
        // Create an anchor for the newly found image and bind it to the image object.
        if (!augmentedImageMap.containsKey(augmentedImage.getIndex())) {
            ARAnchor centerPoseAnchor = null;
            if (!mIsImageTrackOnly) {
                centerPoseAnchor = augmentedImage.createAnchor(augmentedImage.getCenterPose());
            }
            augmentedImageMap.put(augmentedImage.getIndex(), Pair.create(augmentedImage, centerPoseAnchor));
        }
    }

    private void showTrackingImageInfo(ARAugmentedImage augmentedImage) {
        if (!mCurrentImageId.equals(augmentedImage.getCloudImageId())) {
            mCurrentImageId = augmentedImage.getCloudImageId();
            final String tipsMsg = augmentedImage.getCloudImageMetadata();
            if (TextUtils.isEmpty(tipsMsg)) {
                return;
            }
            mActivity.runOnUiThread(() -> Toast.makeText(mActivity, tipsMsg, Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * Enable or disable the image tracking capability.
     *
     * @param isImageTrackOnly true: enable; false: disable
     */
    public void isImageTrackOnly(boolean isImageTrackOnly) {
        mIsImageTrackOnly = isImageTrackOnly;
    }

    /**
     * 2D image rendering API, which is implemented by the 2D image rendering instance class.
     */
    public interface OnDrawImageListener {
        /**
         * 2D image rendering method.
         *
         * @param augmentedImage Image detection and tracking results in the environment.
         * @param viewMatrix View transformation matrix under the orthographic camera.
         * @param projectionMatrix Model projection matrix under the orthographic camera.
         */
        void drawImage(ARAugmentedImage augmentedImage, float[] viewMatrix, float[] projectionMatrix);
    }
}

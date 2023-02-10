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

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.huawei.hiar.ARSession;

/**
 * Device rotation manager, which is used by the demo to adapt to device rotations
 *
 * @author HW
 * @since 2020-03-20
 */
public class DisplayRotationManager implements DisplayListener {
    private static final String TAG = "DisplayRotationManager";

    private boolean mIsDeviceRotation;

    private final Context mContext;

    private final Display mDefaultDisplay;

    private int mViewWidth;

    private int mViewHeight;

    /**
     * Construct DisplayRotationManage with the context.
     *
     * @param context Context.
     */
    public DisplayRotationManager(@NonNull Context context) {
        Display defaultDisplay = null;
        mContext = context;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            defaultDisplay = mContext.getDisplay();
        } else {
            WindowManager systemService = mContext.getSystemService(WindowManager.class);
            if (systemService != null) {
                defaultDisplay = systemService.getDefaultDisplay();
            }
        }
        mDefaultDisplay = defaultDisplay;
    }

    /**
     * Deregister a listener on display changes. This method can be called when onPause is called for an activity.
     */
    public void unregisterDisplayListener() {
        DisplayManager systemService = mContext.getSystemService(DisplayManager.class);
        if (systemService != null) {
            systemService.unregisterDisplayListener(this);
        }
    }

    /**
     * Register a listener on display changes. This method can be called when onResume is called for an activity.
     */
    public void registerDisplayListener() {
        DisplayManager systemService = mContext.getSystemService(DisplayManager.class);
        if (systemService != null) {
            systemService.registerDisplayListener(this, null);
        }
    }

    /**
     * When a device is rotated, the viewfinder size and whether the device is rotated
     * should be updated to correctly display the geometric information returned by the
     * AR Engine. This method should be called when onSurfaceChanged.
     *
     * @param width Width of the surface updated by the device.
     * @param height Height of the surface updated by the device.
     */
    public void updateViewportRotation(int width, int height) {
        mViewWidth = width;
        mViewHeight = height;
        mIsDeviceRotation = true;
    }

    /**
     * Check whether the current device is rotated.
     *
     * @return The device rotation result.
     */
    public boolean getDeviceRotation() {
        return mIsDeviceRotation;
    }

    /**
     * If the device is rotated, update the device window of the current ARSession.
     * This method can be called when onDrawFrame is called.
     *
     * @param session {@link ARSession} object.
     */
    public void updateArSessionDisplayGeometry(ARSession session) {
        int displayRotation = 0;
        if (mDefaultDisplay != null) {
            displayRotation = mDefaultDisplay.getRotation();
        } else {
            Log.e(TAG, "updateArSessionDisplayGeometry mDisplay null!");
        }
        session.setDisplayGeometry(displayRotation, mViewWidth, mViewHeight);
        mIsDeviceRotation = false;
    }

    @Override
    public void onDisplayChanged(int displayId) {
        mIsDeviceRotation = true;
    }

    @Override
    public void onDisplayAdded(int displayId) {
    }

    @Override
    public void onDisplayRemoved(int displayId) {
    }
}

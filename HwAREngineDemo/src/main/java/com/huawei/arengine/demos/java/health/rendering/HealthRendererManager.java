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

package com.huawei.arengine.demos.java.health.rendering;

import android.app.Activity;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.huawei.arengine.demos.common.ArDemoRuntimeException;
import com.huawei.arengine.demos.common.BaseRendererManager;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.hiar.ARFace;
import com.huawei.hiar.ARTrackable;
import com.huawei.hiar.exceptions.ARDeadlineExceededException;
import com.huawei.hiar.exceptions.ARFatalException;
import com.huawei.hiar.exceptions.ARSessionPausedException;
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException;

import java.util.Collection;
import java.util.HashMap;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Contains logics related to background rendering and health data updating.
 *
 * @author HW
 * @since 2020-03-18
 */
public class HealthRendererManager extends BaseRendererManager implements BaseRendererManager.BaseRenderer {
    private static final String TAG = HealthRendererManager.class.getSimpleName();

    private static final int MAX_PROGRESS = 100;

    private static final int PADDING_VALUE = 30;

    private TableLayout mHealthParamTable;

    private int mProgress;

    /**
     * The constructor initializes context and activity.
     * This method will be called when {@link Activity#onCreate}.
     *
     * @param activity Activity
     */
    public HealthRendererManager(Activity activity) {
        mActivity = activity;
        setRenderer(this);
    }

    @Override
    public void surfaceCreated(GL10 gl10, EGLConfig eglConfig) {
    }

    @Override
    public void surfaceChanged(GL10 gl10, int width, int height) {
    }

    @Override
    public void drawFrame(GL10 gl10) {
        try {
            Collection<ARFace> faces = mSession.getAllTrackables(ARFace.class);
            if (faces.size() == 0) {
                return;
            }
            for (ARFace face : faces) {
                if (face.getTrackingState() != ARTrackable.TrackingState.TRACKING) {
                    continue;
                }
                HashMap<ARFace.HealthParameter, Float> healthParams = face.getHealthParameters();
                if (mProgress < MAX_PROGRESS) {
                    updateHealthParamTable(healthParams);
                }
            }
        } catch (ARSessionPausedException e) {
            LogUtil.error(TAG, "Exception on the ARSessionPausedException!");
        } catch (ARFatalException e) {
            LogUtil.error(TAG, "Exception on the ARFatalException!");
        } catch (IllegalArgumentException | ARDeadlineExceededException |
            ARUnavailableServiceApkTooOldException | ArDemoRuntimeException t) {
            // This prevents the app from crashing due to unhandled exceptions.
            LogUtil.error(TAG, "Exception on the OpenGL thread.");
        }
    }

    /**
     * Setting the health check progress.
     *
     * @param progress Progress Information.
     */
    public void setHealthCheckProgress(int progress) {
        mProgress = progress;
    }

    /**
     * Setting the TableLayout Used for Health Display.
     *
     * @param tableLayout TableLayout.
     */
    public void setHealthParamTable(TableLayout tableLayout) {
        if (tableLayout == null) {
            LogUtil.error(TAG, "Set health parameter table failed, tableLayout is null");
            return;
        }
        mHealthParamTable = tableLayout;
    }

    private void updateHealthParamTable(final HashMap<ARFace.HealthParameter, Float> healthParams) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mHealthParamTable.removeAllViews();
                TableRow heatRateTableRow = initTableRow(ARFace.HealthParameter.PARAMETER_HEART_RATE.toString(),
                    healthParams.getOrDefault(ARFace.HealthParameter.PARAMETER_HEART_RATE, 0.0f).toString());
                mHealthParamTable.addView(heatRateTableRow);
                TableRow breathRateTableRow = initTableRow(ARFace.HealthParameter.PARAMETER_BREATH_RATE.toString(),
                    healthParams.getOrDefault(ARFace.HealthParameter.PARAMETER_BREATH_RATE, 0.0f).toString());
                mHealthParamTable.addView(breathRateTableRow);
            }
        });
    }

    private TableRow initTableRow(String keyStr, String valueStr) {
        TextView textViewKey = new TextView(mActivity);
        TextView textViewValue = new TextView(mActivity);
        textViewKey.setText(keyStr);
        textViewValue.setText(valueStr);
        textViewValue.setPadding(PADDING_VALUE, 0, 0, 0);
        TableRow tableRow = new TableRow(mActivity);
        tableRow.addView(textViewKey);
        tableRow.addView(textViewValue);
        return tableRow;
    }
}

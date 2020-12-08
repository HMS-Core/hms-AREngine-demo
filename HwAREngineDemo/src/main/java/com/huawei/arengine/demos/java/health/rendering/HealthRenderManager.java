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

package com.huawei.arengine.demos.java.health.rendering;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.huawei.arengine.demos.common.ArDemoRuntimeException;
import com.huawei.arengine.demos.common.DisplayRotationManager;
import com.huawei.arengine.demos.common.TextureDisplay;
import com.huawei.hiar.ARFace;
import com.huawei.hiar.ARFrame;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.ARTrackable;

import java.util.Collection;
import java.util.HashMap;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * This class shows how to render health data obtained from HUAWEI AR Engine.
 *
 * @author HW
 * @since 2020-03-18
 */
public class HealthRenderManager implements GLSurfaceView.Renderer {
    private static final String TAG = HealthRenderManager.class.getSimpleName();

    private static final int MAX_PROGRESS = 100;

    private static final int PADDING_VALUE = 30;

    private ARSession mArSession;

    private TextureDisplay mTextureDisplay = new TextureDisplay();

    private Activity mActivity;

    private Context mContext;

    private DisplayRotationManager mDisplayRotationManager;

    private TableLayout mHealthParamTable;

    private int mProgress;

    /**
     * The constructor initializes context and activity.
     * This method will be called when {@link Activity#onCreate}.
     *
     * @param activity Activity
     * @param context Context
     */
    public HealthRenderManager(Activity activity, Context context) {
        mActivity = activity;
        mContext = context;
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        mTextureDisplay.init();
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        mTextureDisplay.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
        mDisplayRotationManager.updateViewportRotation(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        if (mArSession == null) {
            return;
        }
        if (mDisplayRotationManager.getDeviceRotation()) {
            mDisplayRotationManager.updateArSessionDisplayGeometry(mArSession);
        }
        try {
            mArSession.setCameraTextureName(mTextureDisplay.getExternalTextureId());
            ARFrame frame = mArSession.update();
            mTextureDisplay.onDrawFrame(frame);
            Collection<ARFace> faces = mArSession.getAllTrackables(ARFace.class);
            if (faces.size() == 0) {
                return;
            }
            for (ARFace face : faces) {
                if (face.getTrackingState() == ARTrackable.TrackingState.TRACKING) {
                    HashMap<ARFace.HealthParameter, Float> healthParams = face.getHealthParameters();
                    if (mProgress < MAX_PROGRESS) {
                        updateHealthParamTable(healthParams);
                    }
                }
            }
        } catch (ArDemoRuntimeException e) {
            Log.e(TAG, "Exception on the ArDemoRuntimeException!");
        }
    }

    /**
     *  Sets the health monitoring progress.
     *
     * @param progress Progress.
     */
    public void setHealthCheckProgress(int progress) {
        mProgress = progress;
    }

    /**
     * Set an ARSession. The input ARSession will be called in onDrawFrame
     * to obtain the latest data. This method is called when {@link Activity#onResume}.
     *
     * @param arSession ARSession.
     */
    public void setArSession(ARSession arSession) {
        if (arSession == null) {
            Log.e(TAG, "Set session error, arSession is null!");
            return;
        }
        mArSession = arSession;
    }

    /**
     * Set the displayRotationManage object, which will be used in onSurfaceChanged
     * and onDrawFrame. This method is called when {@link Activity#onResume}.
     *
     * @param displayRotationManager DisplayRotationManage.
     */
    public void setDisplayRotationManage(DisplayRotationManager displayRotationManager) {
        if (displayRotationManager == null) {
            Log.e(TAG, "Set display rotation manage error, displayRotationManage is null!");
            return;
        }
        mDisplayRotationManager = displayRotationManager;
    }

    /**
     *  Sets the TableLayout used for health data display.
     *
     * @param tableLayout TableLayout.
     */
    public void setHealthParamTable(TableLayout tableLayout) {
        if (tableLayout == null) {
            Log.e(TAG, "Set health parameter table failed, tableLayout is null");
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
                TableRow faceAgeTableRow = initTableRow(ARFace.HealthParameter.PARAMETER_FACE_AGE.toString(),
                    healthParams.getOrDefault(ARFace.HealthParameter.PARAMETER_FACE_AGE, 0.0f).toString());
                mHealthParamTable.addView(faceAgeTableRow);
            }
        });
    }

    private TableRow initTableRow(String keyStr, String valueStr) {
        TextView textViewKey = new TextView(mContext);
        TextView textViewValue = new TextView(mContext);
        textViewKey.setText(keyStr);
        textViewValue.setText(valueStr);
        textViewValue.setPadding(PADDING_VALUE, 0, 0, 0);
        TableRow tableRow = new TableRow(mContext);
        tableRow.addView(textViewKey);
        tableRow.addView(textViewValue);
        return tableRow;
    }
}

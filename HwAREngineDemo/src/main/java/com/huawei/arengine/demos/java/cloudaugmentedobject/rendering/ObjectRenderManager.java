/**
 * Copyright 2021. Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huawei.arengine.demos.java.cloudaugmentedobject.rendering;

import android.app.Activity;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.widget.TextView;

import com.huawei.arengine.demos.R;
import com.huawei.arengine.demos.common.ArDemoRuntimeException;
import com.huawei.arengine.demos.common.DisplayRotationManager;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.arengine.demos.common.TextDisplay;
import com.huawei.arengine.demos.common.TextureDisplay;
import com.huawei.hiar.ARCamera;
import com.huawei.hiar.ARFrame;
import com.huawei.hiar.ARObject;
import com.huawei.hiar.ARPose;
import com.huawei.hiar.ARSession;

import java.util.ArrayList;
import java.util.Collection;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * This class renders object data obtained by the AR Engine.
 *
 * @author HW
 * @since 2021-04-02
 */
public class ObjectRenderManager implements GLSurfaceView.Renderer  {
    private static final String TAG = ObjectRenderManager.class.getSimpleName();

    private static final int PROJ_MATRIX_OFFSET = 0;

    private static final float PROJ_MATRIX_NEAR = 0.1f;

    private static final float PROJ_MATRIX_FAR = 100.0f;

    private ARSession mSession;

    private float fps;

    private int frames = 0;

    private long lastInterval;

    private Activity mActivity;

    private TextView mTextView;

    private String mAuthJson = null;

    private boolean isAuthed = false;

    private TextureDisplay mTextureDisplay = new TextureDisplay();

    private TextDisplay mTextDisplay = new TextDisplay();

    private ArrayList<ObjectRelatedDisplay> mObjectRelatedDisplays = new ArrayList<>();

    private DisplayRotationManager mDisplayRotationManager;

    public ObjectRenderManager(Activity activity) {
        mActivity = activity;
        mTextView = mActivity.findViewById(R.id.cloudAugmentObjectTextView);
        ObjectRelatedDisplay objectLabelDisplay = new ObjectLabelDisplay(mActivity);
        mObjectRelatedDisplays.add(objectLabelDisplay);
    }

    /**
     * Set the AR session to be updated in onDrawFrame to obtain the latest data.
     *
     * @param arSession ARSession.
     */
    public void setArSession(ARSession arSession) {
        if (arSession == null) {
            LogUtil.error(TAG, "Set session error, arSession is null!");
            return;
        }
        mSession = arSession;
    }

    /**
     * 设置云锚点鉴权json
     *
     * @param authJson 鉴权json
     */
    public void setAuthJson(String authJson) {
        if (authJson == null) {
            LogUtil.error(TAG, "setAuthJson error, authJson is null!");
            return;
        }
        mAuthJson = authJson;
    }

    /**
     * Set displayRotationManage, which is used in onSurfaceChanged and onDrawFrame.
     *
     * @param displayRotationManager DisplayRotationManage.
     */
    public void setDisplayRotationManager(DisplayRotationManager displayRotationManager) {
        if (displayRotationManager == null) {
            LogUtil.error(TAG, "Set display rotation manage error, display rotation manage is null!");
            return;
        }
        mDisplayRotationManager = displayRotationManager;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Set the window color.
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        for (ObjectRelatedDisplay objectRelatedDisplay: mObjectRelatedDisplays) {
            objectRelatedDisplay.init();
        }
        mTextureDisplay.init();
        mTextDisplay.setListener(new TextDisplay.OnTextInfoChangeListener() {
            @Override
            public void textInfoChanged(String text, float positionX, float positionY) {
                showWorldTypeTextView(text, positionX, positionY);
            }
        });
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mTextureDisplay.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
        mDisplayRotationManager.updateViewportRotation(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        if (mSession == null) {
            return;
        }
        if (!isAuthed && mAuthJson != null) {
            LogUtil.debug(TAG, "Cloud Anchor onDrawFrame set Auth info: " + mAuthJson);
            LogUtil.debug(TAG, "Cloud Anchor onDrawFrame is authed: " + isAuthed);
            mSession.setCloudServiceAuthInfo(mAuthJson);
            isAuthed = true;
        }
        if (mDisplayRotationManager.getDeviceRotation()) {
            mDisplayRotationManager.updateArSessionDisplayGeometry(mSession);
        }

        try {
            mSession.setCameraTextureName(mTextureDisplay.getExternalTextureId());
            ARFrame arFrame = mSession.update();
            ARCamera arCamera = arFrame.getCamera();

            // The size of the projection matrix is 4 * 4.
            float[] projectionMatrix = new float[16];
            // The size of the view matrix is 4 * 4.
            float[] viewMatrix = new float[16];

            arCamera.getProjectionMatrix(projectionMatrix, PROJ_MATRIX_OFFSET, PROJ_MATRIX_NEAR, PROJ_MATRIX_FAR);
            arCamera.getViewMatrix(viewMatrix, 0);
            mTextureDisplay.onDrawFrame(arFrame);
            Collection<ARObject> updatedObjects = mSession.getAllTrackables(ARObject.class);
            for (ObjectRelatedDisplay objectRelatedDisplay: mObjectRelatedDisplays) {
                objectRelatedDisplay.onDrawFrame(updatedObjects, viewMatrix, projectionMatrix);
            }
            LogUtil.debug(TAG, "onDrawFrame: Updated ARObject is " + updatedObjects.size());
            StringBuilder sb = new StringBuilder();
            updateMessageData(sb, updatedObjects);
            mTextDisplay.onDrawFrame(sb);
        } catch (ArDemoRuntimeException e) {
            LogUtil.error(TAG, "Exception on the ArDemoRuntimeException!");
        } catch (Throwable t) {
            // This prevents the app from crashing due to unhandled exceptions.
            LogUtil.error(TAG, "Exception on the OpenGL thread.");
        }
    }

    /**
     * Create a thread for text display in the UI thread. This thread will be called back in TextureDisplay.
     *
     * @param text Gesture information displayed on the screen
     * @param positionX The left padding in pixels.
     * @param positionY The right padding in pixels.
     */
    private void showWorldTypeTextView(final String text, final float positionX, final float positionY) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextView.setTextColor(Color.WHITE);

                // Set the font size to be displayed on the screen.
                mTextView.setTextSize(10f);
                if (text != null) {
                    mTextView.setText(text);
                    mTextView.setPadding((int) positionX, (int) positionY, 0, 0);
                } else {
                    mTextView.setText("");
                }
            }
        });
    }

    /**
     * Update the information to be displayed on the screen.
     *
     * @param sb String buffer.
     * @param updatedObjects updated ar objects
     */
    private void updateMessageData(StringBuilder sb, Collection<ARObject> updatedObjects) {
        float fpsResult = doFpsCalculate();
        sb.append("FPS=").append(fpsResult).append(System.lineSeparator());
        sb.append("object size: ").append(updatedObjects.size()).append(System.lineSeparator());
        for (ARObject arObject : updatedObjects) {
            sb.append("object state: ").append(arObject.getTrackingState()).append(System.lineSeparator());
            sb.append("object name: ").append(arObject.getName()).append(System.lineSeparator());
            sb.append("object ID: ").append(arObject.getObjectID()).append(System.lineSeparator());
            ARPose arpose = arObject.getCenterPose();
            sb.append("arPose ").append("x: ").append(arpose.tx()).append(" y: ")
                .append(arpose.ty()).append(" z: ").append(arpose.tz()).append(System.lineSeparator());
            sb.append("arPose ").append("qx: ").append(arpose.qw()).append(" qy: ").append(arpose.qy()).append(" qz: ")
                .append(arpose.qz()).append(" qw: ").append(arpose.qw()).append(System.lineSeparator());
            sb.append("object anchor id: ").append(arObject.getObjectAnchorId()).append(System.lineSeparator());
        }
    }

    private float doFpsCalculate() {
        ++frames;
        long timeNow = System.currentTimeMillis();

        // Convert millisecond to second.
        if (((timeNow - lastInterval) / 1000.0f) > 0.5f) {
            fps = frames / ((timeNow - lastInterval) / 1000.0f);
            frames = 0;
            lastInterval = timeNow;
        }
        return fps;
    }
}

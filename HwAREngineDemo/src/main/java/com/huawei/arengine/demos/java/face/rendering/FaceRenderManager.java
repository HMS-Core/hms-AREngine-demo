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

package com.huawei.arengine.demos.java.face.rendering;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.widget.TextView;

import com.huawei.arengine.demos.common.ArDemoRuntimeException;
import com.huawei.arengine.demos.common.DisplayRotationManager;
import com.huawei.arengine.demos.common.TextDisplay;
import com.huawei.arengine.demos.common.TextureDisplay;
import com.huawei.hiar.ARCamera;
import com.huawei.hiar.ARFace;
import com.huawei.hiar.ARFrame;
import com.huawei.hiar.ARPose;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.ARTrackable.TrackingState;

import java.util.Collection;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * This class manages rendering related to facial data.
 *
 * @author HW
 * @since 2020-03-21
 */
public class FaceRenderManager implements GLSurfaceView.Renderer {
    private static final String TAG = FaceRenderManager.class.getSimpleName();

    private static final float UPDATE_INTERVAL = 0.5f;

    private int frames = 0;

    private long lastInterval;

    private ARSession mArSession;

    private float fps;

    private Context mContext;

    private Activity mActivity;

    private TextView mTextView;

    private boolean isOpenCameraOutside = true;

    private int mTextureId = -1; // Initialize the texture ID.

    private TextureDisplay mTextureDisplay = new TextureDisplay();

    private FaceGeometryDisplay mFaceGeometryDisplay = new FaceGeometryDisplay();

    private TextDisplay mTextDisplay = new TextDisplay();

    private DisplayRotationManager mDisplayRotationManager;

    /**
     * The constructor initializes context and activity.
     * This method will be called when {@link Activity#onCreate}.
     *
     * @param context Context
     * @param activity Activity
     */
    public FaceRenderManager(Context context, Activity activity) {
        mContext = context;
        mActivity = activity;
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
     * Set TextView. This object will be used in the UI thread. This method is called when {@link Activity#onCreate}.
     *
     * @param textView TextView.
     */
    public void setTextView(TextView textView) {
        if (textView == null) {
            Log.e(TAG, "Set text view error, textView is null!");
            return;
        }
        mTextView = textView;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Set the window color.
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        if (isOpenCameraOutside) {
            mTextureDisplay.init(mTextureId);
        } else {
            mTextureDisplay.init();
        }
        Log.i(TAG, "On surface created textureId= " + mTextureId);

        mFaceGeometryDisplay.init(mContext);

        mTextDisplay.setListener(new TextDisplay.OnTextInfoChangeListener() {
            @Override
            public void textInfoChanged(String text, float positionX, float positionY) {
                showTextViewOnUiThread(text, positionX, positionY);
            }
        });
    }

    /**
     * Create a thread for text display on the UI. The method for displaying texts is called back in TextureDisplay.
     *
     * @param text Information displayed on the screen.
     * @param positionX X coordinate of a point.
     * @param positionY Y coordinate of a point.
     */
    private void showTextViewOnUiThread(final String text, final float positionX, final float positionY) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextView.setTextColor(Color.WHITE);

                // Set the size of the text displayed on the screen.
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

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        mTextureDisplay.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
        mDisplayRotationManager.updateViewportRotation(width, height);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
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
            float fpsResult = doFpsCalculate();
            Collection<ARFace> faces = mArSession.getAllTrackables(ARFace.class);
            if (faces.size() == 0) {
                mTextDisplay.onDrawFrame(null);
                return;
            }
            Log.d(TAG, "Face number: " + faces.size());
            ARCamera camera = frame.getCamera();
            for (ARFace face : faces) {
                if (face.getTrackingState() == TrackingState.TRACKING) {
                    mFaceGeometryDisplay.onDrawFrame(camera, face);
                    StringBuilder sb = new StringBuilder();
                    updateMessageData(sb, fpsResult, face);
                    mTextDisplay.onDrawFrame(sb);
                }
            }
        } catch (ArDemoRuntimeException e) {
            Log.e(TAG, "Exception on the ArDemoRuntimeException!");
        } catch (Throwable t) {
            // This prevents the app from crashing due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }

    private void updateMessageData(StringBuilder sb, float fpsResult, ARFace face) {
        sb.append("FPS= ").append(fpsResult).append(System.lineSeparator());
        ARPose pose = face.getPose();
        if (pose != null) {
            sb.append("face pose information:");
            sb.append("face pose tx:[").append(pose.tx()).append("]").append(System.lineSeparator());
            sb.append("face pose ty:[").append(pose.ty()).append("]").append(System.lineSeparator());
            sb.append("face pose tz:[").append(pose.tz()).append("]").append(System.lineSeparator());
            sb.append("face pose qx:[").append(pose.qx()).append("]").append(System.lineSeparator());
            sb.append("face pose qy:[").append(pose.qy()).append("]").append(System.lineSeparator());
            sb.append("face pose qz:[").append(pose.qz()).append("]").append(System.lineSeparator());
            sb.append("face pose qw:[").append(pose.qw()).append("]").append(System.lineSeparator());
        }
        sb.append(System.lineSeparator());

        float[] textureCoordinates = face.getFaceGeometry().getTextureCoordinates().array();
        sb.append("textureCoordinates length:[ ").append(textureCoordinates.length).append(" ]");
    }

    private float doFpsCalculate() {
        ++frames;
        long timeNow = System.currentTimeMillis();

        // Convert millisecond to second.
        if (((timeNow - lastInterval) / 1000.0f) > UPDATE_INTERVAL) {
            fps = frames / ((timeNow - lastInterval) / 1000.0f);
            frames = 0;
            lastInterval = timeNow;
        }
        return fps;
    }
}
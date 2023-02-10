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

import android.app.Activity;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.widget.TextView;

import com.huawei.arengine.demos.java.utils.CommonUtil;
import com.huawei.arengine.demos.java.utils.UiUtils;
import com.huawei.hiar.ARCamera;
import com.huawei.hiar.ARFrame;
import com.huawei.hiar.ARHitResult;
import com.huawei.hiar.ARPlane;
import com.huawei.hiar.ARPoint;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.ARTrackable;
import com.huawei.hiar.exceptions.ARDeadlineExceededException;
import com.huawei.hiar.exceptions.ARFatalException;
import com.huawei.hiar.exceptions.ARSessionPausedException;
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException;

import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * AR feature rendering base class.
 *
 * @author HW
 * @since 2022-08-26
 */
public abstract class BaseRendererManager implements GLSurfaceView.Renderer {
    private static final String TAG = "BaseRenderManager";

    private static final float SECOND_TO_MILLISECOND = 1000.0f;

    private static final float MIN_INTERVAL = 0.5f;

    private static final int PROJ_MATRIX_OFFSET = 0;

    private static final float PROJ_MATRIX_NEAR = 0.1f;

    private static final float PROJ_MATRIX_FAR = 100.0f;

    /**
     * The size of the projection matrix is 4 * 4.
     */
    protected float[] mProjectionMatrix = new float[16];

    /**
     * The size of ViewMatrix is 4 * 4.
     */
    protected float[] mViewMatrix = new float[16];

    /**
     * Camera preview display.
     */
    protected BaseBackgroundDisplay mBackgroundDisplay = new TextureDisplay();

    /**
     * Display of text prompts on the screen.
     */
    protected TextDisplay mTextDisplay = new TextDisplay();

    /**
     * Session instance.
     */
    protected ARSession mSession;

    /**
     * Camera instance.
     */
    protected ARCamera mArCamera;

    /**
     * Frame instance.
     */
    protected ARFrame mArFrame;

    /**
     * Used for the display of recognition data.
     */
    protected TextView mTextView;

    /**
     * Activity instance.
     */
    protected Activity mActivity;

    private DisplayRotationManager mDisplayRotationManager;

    private BaseRenderer mRenderer;

    private int frames = 0;

    private long lastTime;

    private float fps = 0.0f;

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
        if (arSession == null) {
            LogUtil.error(TAG, "Set session error, arSession is null!");
            return;
        }
        mSession = arSession;
    }

    /**
     * Set TextView, which is called in the UI thread to display data correctly.
     *
     * @param textView TextView.
     */
    public void setTextView(TextView textView) {
        if (textView == null) {
            LogUtil.error(TAG, "Set textView error, text view is null!");
            return;
        }
        mTextView = textView;
    }

    /**
     * Called after the AR feature rendering class is instantiated, and is returned to BaseRenderManager.
     *
     * @param baseRenderer AR feature rendering class.
     */
    protected void setRenderer(BaseRenderer baseRenderer) {
        mRenderer = baseRenderer;
    }

    /**
     * Use a custom background renderer.
     *
     * @param display Custom background renderer.
     * @param <T> Class for previewing the custom background, which requires the BaseBackgroundDisplay API to be
     *        implemented.
     */
    protected <T extends BaseBackgroundDisplay> void useDefaultBackGround(T display) {
        if (display == null) {
            LogUtil.error(TAG, "display init failed,");
            return;
        }
        mBackgroundDisplay = display;
    }

    /**
     * Calculate and return the current frame rate.
     *
     * @return fps Current frame rate of the screen.
     */
    protected float doFpsCalculate() {
        ++frames;
        long timeNow = System.currentTimeMillis();

        // Convert millisecond to second.
        float intervalTime = (timeNow - lastTime) / SECOND_TO_MILLISECOND;
        if (intervalTime > MIN_INTERVAL) {
            fps = (float) frames / intervalTime;
            frames = 0;
            lastTime = timeNow;
        }
        return fps;
    }

    /**
     * Determine whether the tap event occurs on the AR plane or in the AR dot matrix.
     *
     * @param frame A snapshot of the AR Engine system.
     * @param camera AR camera instance.
     * @param event Finger tap and move event.
     * @return Return the tap result (ARHitResult type) if the tap event occurs on the AR plane or in the AR dot matrix;
     *         return null otherwise.
     */
    protected ARHitResult hitTest4Result(ARFrame frame, ARCamera camera, MotionEvent event) {
        ARHitResult hitResult = null;
        List<ARHitResult> hitTestResults = CommonUtil.hitTest(frame, event);

        for (int i = 0; i < hitTestResults.size(); i++) {
            // Determine whether the hit point is within the plane polygon.
            ARHitResult hitResultTemp = hitTestResults.get(i);
            if (hitResultTemp == null) {
                continue;
            }
            ARTrackable trackable = hitResultTemp.getTrackable();

            boolean isPlanHitJudge =
                trackable instanceof ARPlane && ((ARPlane) trackable).isPoseInPolygon(hitResultTemp.getHitPose())
                    && (CommonUtil.calculateDistanceToPlane(hitResultTemp.getHitPose(), camera.getPose()) > 0);

            // Determine whether the point cloud is clicked and whether the point faces the camera.
            boolean isPointHitJudge = trackable instanceof ARPoint
                && ((ARPoint) trackable).getOrientationMode() == ARPoint.OrientationMode.ESTIMATED_SURFACE_NORMAL;

            // Select points on the plane preferentially.
            if (isPlanHitJudge || isPointHitJudge) {
                hitResult = hitResultTemp;
                if (trackable instanceof ARPlane) {
                    break;
                }
            }
        }
        return hitResult;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Set the window color.
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        if (mBackgroundDisplay == null || mRenderer == null) {
            LogUtil.error(TAG, "surface create error.");
            return;
        }
        mBackgroundDisplay.init();
        if (mActivity != null && mTextView != null) {
            mTextDisplay.setListener((text, positionX, positionY) -> UiUtils.showTypeTextView(mActivity, mTextView,
                text, positionX, positionY));
        }
        mRenderer.surfaceCreated(gl, config);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        if (mBackgroundDisplay == null || mDisplayRotationManager == null || mRenderer == null) {
            LogUtil.error(TAG, "surface change error.");
            return;
        }
        mBackgroundDisplay.onSurfaceChanged(width, height);
        mDisplayRotationManager.updateViewportRotation(width, height);
        mRenderer.surfaceChanged(gl, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        if (mSession == null) {
            return;
        }
        if (mDisplayRotationManager == null || mBackgroundDisplay == null || mRenderer == null) {
            return;
        }
        if (mDisplayRotationManager.getDeviceRotation()) {
            mDisplayRotationManager.updateArSessionDisplayGeometry(mSession);
        }
        mSession.setCameraTextureName(mBackgroundDisplay.getExternalTextureId());
        try {
            mArFrame = mSession.update();
            mBackgroundDisplay.onDrawFrame(mArFrame);
            mArCamera = mArFrame.getCamera();
            mArCamera.getProjectionMatrix(mProjectionMatrix, PROJ_MATRIX_OFFSET, PROJ_MATRIX_NEAR, PROJ_MATRIX_FAR);
            mArCamera.getViewMatrix(mViewMatrix, 0);
        } catch (ARSessionPausedException e) {
            LogUtil.error(TAG, "Invoke session.resume before invoking Session.update.");
            return;
        } catch (ARFatalException | IllegalArgumentException | ARDeadlineExceededException
            | ARUnavailableServiceApkTooOldException | ArDemoRuntimeException exception) {
            // This prevents the app from crashing due to unhandled exceptions.
            LogUtil.warn(TAG, "Exception on the OpenGL thread, " + exception.getClass());
            return;
        }
        mRenderer.drawFrame(gl);
    }

    /**
     * Rendering base class API, which is implemented by a specific AR feature rendering class.
     */
    public interface BaseRenderer {
        /**
         * API for creating surfaces.
         *
         * @param gl GL API.
         * @param config EGL configuration for creating a surface.
         */
        void surfaceCreated(GL10 gl, EGLConfig config);

        /**
         * API for changing the surface window size.
         *
         * @param gl GL API.
         * @param width Surface window width.
         * @param height Surface window height.
         */
        void surfaceChanged(GL10 gl, int width, int height);

        /**
         * API for surface drawing.
         *
         * @param gl GL API.
         */
        void drawFrame(GL10 gl);
    }
}

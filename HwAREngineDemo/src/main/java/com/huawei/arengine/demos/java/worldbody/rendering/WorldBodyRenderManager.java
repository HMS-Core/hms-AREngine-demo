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

package com.huawei.arengine.demos.java.worldbody.rendering;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.widget.TextView;

import com.huawei.arengine.demos.R;
import com.huawei.arengine.demos.common.ArDemoRuntimeException;
import com.huawei.arengine.demos.common.DisplayRotationManager;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.arengine.demos.common.TextDisplay;
import com.huawei.arengine.demos.common.TextureDisplay;
import com.huawei.arengine.demos.java.body3d.rendering.BodyRelatedDisplay;
import com.huawei.arengine.demos.java.body3d.rendering.BodySkeletonDisplay;
import com.huawei.arengine.demos.java.body3d.rendering.BodySkeletonLineDisplay;
import com.huawei.arengine.demos.java.world.GestureEvent;
import com.huawei.arengine.demos.java.world.VirtualObject;
import com.huawei.arengine.demos.java.world.rendering.ObjectDisplay;
import com.huawei.arengine.demos.java.world.rendering.PointCloudRenderer;
import com.huawei.hiar.ARBody;
import com.huawei.hiar.ARCamera;
import com.huawei.hiar.ARFrame;
import com.huawei.hiar.ARHitResult;
import com.huawei.hiar.ARPlane;
import com.huawei.hiar.ARPoint;
import com.huawei.hiar.ARPointCloud;
import com.huawei.hiar.ARPose;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.ARTrackable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * World body AR type use presentation.
 *
 * @author HW
 * @since 2021-03-22
 */
public class WorldBodyRenderManager implements GLSurfaceView.Renderer {
    private static final String TAG = WorldBodyRenderManager.class.getSimpleName();

    private static final int PROJ_MATRIX_OFFSET = 0;

    private static final float PROJ_MATRIX_NEAR = 0.1f;

    private static final float PROJ_MATRIX_FAR = 100.0f;

    private static final float[] BLUE_COLORS = new float[] {66.0f, 133.0f, 244.0f, 255.0f};

    private ARSession mSession;

    private Activity mActivity;

    private Context mContext;

    private TextView mTextView;

    private int frames = 0;

    private long lastInterval;

    private float fps;

    // Drawing of Camera Background Textures
    private TextureDisplay mTextureDisplay = new TextureDisplay();

    // Display of text prompts on the screen
    private TextDisplay mTextDisplay = new TextDisplay();

    private ObjectDisplay mObjectDisplay = new ObjectDisplay();

    private PointCloudRenderer mPointCloud = new PointCloudRenderer();

    private DisplayRotationManager mDisplayRotationManager;

    private ArrayBlockingQueue<GestureEvent> mQueuedSingleTaps;

    private ArrayList<VirtualObject> mVirtualObjects = new ArrayList<>();

    private ArrayList<BodyRelatedDisplay> mBodyRelatedDisplays = new ArrayList<>();

    /**
     * The constructor passes context and activity. This method will be called when {@link Activity#onCreate}.
     *
     * @param activity Activity
     * @param context Context
     */
    public WorldBodyRenderManager(Activity activity, Context context) {
        mActivity = activity;
        mContext = context;
        mTextView = activity.findViewById(R.id.wordBodyTextView);
        BodyRelatedDisplay bodySkeletonDisplay = new BodySkeletonDisplay();
        BodyRelatedDisplay bodySkeletonLineDisplay = new BodySkeletonLineDisplay();
        mBodyRelatedDisplays.add(bodySkeletonDisplay);
        mBodyRelatedDisplays.add(bodySkeletonLineDisplay);
    }

    /**
     * Set ARSession, which will update and obtain the latest data in OnDrawFrame.
     *
     * @param arSession ARSession.
     */
    public void setArSession(ARSession arSession) {
        if (arSession == null) {
            LogUtil.error(TAG, "setSession error, arSession is null!");
            return;
        }
        mSession = arSession;
    }

    /**
     * Set a gesture type queue.
     *
     * @param queuedSingleTaps Gesture type queue.
     */
    public void setQueuedSingleTaps(ArrayBlockingQueue<GestureEvent> queuedSingleTaps) {
        if (queuedSingleTaps == null) {
            LogUtil.error(TAG, "setSession error, arSession is null!");
            return;
        }
        mQueuedSingleTaps = queuedSingleTaps;
    }

    /**
     * Set the DisplayRotationManage object, which will be used in onSurfaceChanged and onDrawFrame.
     *
     * @param displayRotationManager DisplayRotationManage is a customized object.
     */
    public void setDisplayRotationManage(DisplayRotationManager displayRotationManager) {
        if (displayRotationManager == null) {
            LogUtil.error(TAG, "SetDisplayRotationManage error, displayRotationManage is null!");
            return;
        }
        mDisplayRotationManager = displayRotationManager;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Set the window color.
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        for (BodyRelatedDisplay bodyRelatedDisplay : mBodyRelatedDisplays) {
            bodyRelatedDisplay.init();
        }

        mTextureDisplay.init();
        mTextDisplay.setListener(new TextDisplay.OnTextInfoChangeListener() {
            @Override
            public void textInfoChanged(String text, float positionX, float positionY) {
                showWorldBodyTypeTextView(text, positionX, positionY);
            }
        });

        mObjectDisplay.init(mContext);
        mPointCloud.init(mContext);
    }

    /**
     * Create a thread for text display in the UI thread. This thread will be called back in TextureDisplay.
     *
     * @param text Gesture information displayed on the screen
     * @param positionX The left padding in pixels.
     * @param positionY The right padding in pixels.
     */
    private void showWorldBodyTypeTextView(final String text, final float positionX, final float positionY) {
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

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        mTextureDisplay.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
        mDisplayRotationManager.updateViewportRotation(width, height);
        mObjectDisplay.setSize(width, height);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        if (mSession == null) {
            return;
        }
        if (mDisplayRotationManager.getDeviceRotation()) {
            mDisplayRotationManager.updateArSessionDisplayGeometry(mSession);
        }
        try {
            mSession.setCameraTextureName(mTextureDisplay.getExternalTextureId());
            ARFrame arFrame = mSession.update();
            LogUtil.debug(TAG, "mTextureDisplay.onDrawFrame(arFrame);");
            mTextureDisplay.onDrawFrame(arFrame);
            ARCamera arCamera = arFrame.getCamera();

            // The size of the projection matrix is 4 * 4.
            float[] projectionMatrix = new float[16];
            arCamera.getProjectionMatrix(projectionMatrix, PROJ_MATRIX_OFFSET, PROJ_MATRIX_NEAR, PROJ_MATRIX_FAR);

            // The size of ViewMatrix is 4 * 4.
            float[] viewMatrix = new float[16];
            arCamera.getViewMatrix(viewMatrix, 0);

            handleGestureEvent(arFrame, arCamera);
            drawAllObjects(projectionMatrix, viewMatrix);
            ARPointCloud arPointCloud = arFrame.acquirePointCloud();
            mPointCloud.onDrawFrame(arPointCloud, viewMatrix, projectionMatrix);
            Collection<ARBody> bodies = mSession.getAllTrackables(ARBody.class);

            StringBuilder sb = new StringBuilder();
            updateMessageData(sb, bodies);
            mTextDisplay.onDrawFrame(sb);

            for (BodyRelatedDisplay bodyRelatedDisplay : mBodyRelatedDisplays) {
                bodyRelatedDisplay.onDrawFrame(bodies, projectionMatrix);
            }
            LogUtil.debug(TAG, "after worldBody display.");
        } catch (ArDemoRuntimeException e) {
            LogUtil.error(TAG, "Exception on the ArDemoRuntimeException!");
        } catch (Throwable t) {
            // This prevents the app from crashing due to unhandled exceptions.
            LogUtil.error(TAG, "Exception on the OpenGL thread");
        }
    }

    private void drawAllObjects(float[] projectionMatrix, float[] viewMatrix) {
        Iterator<VirtualObject> ite = mVirtualObjects.iterator();
        while (ite.hasNext()) {
            VirtualObject obj = ite.next();
            if (obj.getAnchor().getTrackingState() == ARTrackable.TrackingState.STOPPED) {
                ite.remove();
            }
            if (obj.getAnchor().getTrackingState() == ARTrackable.TrackingState.TRACKING) {
                // Light intensity 1.
                mObjectDisplay.onDrawFrame(viewMatrix, projectionMatrix, 1.0f, obj);
            }
        }
    }

    /**
     * Update the information to be displayed on the screen.
     *
     * @param sb String buffer.
     * @param bodies identified ARBody.
     */
    private void updateMessageData(StringBuilder sb, Collection<ARBody> bodies) {
        float fpsResult = doFpsCalculate();
        sb.append("FPS=").append(fpsResult).append(System.lineSeparator());
        int trackingBodySum = 0;
        for (ARBody body : bodies) {
            if (body.getTrackingState() != ARTrackable.TrackingState.TRACKING) {
                continue;
            }
            trackingBodySum ++;
            sb.append("body action: ").append(body.getBodyAction()).append(System.lineSeparator());
        }
        sb.append("tracking body sum: ").append(trackingBodySum).append(System.lineSeparator());
        sb.append("Virtual Object number: ").append(mVirtualObjects.size()).append(System.lineSeparator());
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

    private void handleGestureEvent(ARFrame arFrame, ARCamera arCamera) {
        GestureEvent event = mQueuedSingleTaps.poll();
        if (event == null) {
            return;
        }

        // Do not perform anything when the object is not tracked.
        if (arCamera.getTrackingState() != ARTrackable.TrackingState.TRACKING) {
            return;
        }

        int eventType = event.getType();
        if (eventType == GestureEvent.GESTURE_EVENT_TYPE_SINGLETAPCONFIRMED) {
            MotionEvent tap = event.getEventFirst();
            ARHitResult hitResult = hitTest4Result(arFrame, arCamera, tap);
            if (hitResult == null) {
                return;
            }
            doWhenEventTypeSingleTap(hitResult);
        }
    }

    private void doWhenEventTypeSingleTap(ARHitResult hitResult) {
        // The hit results are sorted by distance. Only the nearest hit point is valid.
        // Set the number of stored objects to 10 to avoid the overload of rendering and AR Engine.
        if (mVirtualObjects.size() >= 16) {
            mVirtualObjects.get(0).getAnchor().detach();
            mVirtualObjects.remove(0);
        }

        ARTrackable currentTrackable = hitResult.getTrackable();
        if (currentTrackable instanceof ARPoint || currentTrackable instanceof ARPlane) {
            mVirtualObjects.add(new VirtualObject(hitResult.createAnchor(), BLUE_COLORS));
        }else {
            LogUtil.info(TAG, "Hit result is not plane or point.");
        }
    }

    private ARHitResult hitTest4Result(ARFrame frame, ARCamera camera, MotionEvent event) {
        ARHitResult hitResult = null;
        List<ARHitResult> hitTestResults = frame.hitTest(event);

        for (int i = 0; i < hitTestResults.size(); i++) {
            // Determine whether the hit point is within the plane polygon.
            ARHitResult hitResultTemp = hitTestResults.get(i);
            if (hitResultTemp == null) {
                continue;
            }
            ARTrackable trackable = hitResultTemp.getTrackable();

            boolean isPlanHitJudge =
                trackable instanceof ARPlane && ((ARPlane) trackable).isPoseInPolygon(hitResultTemp.getHitPose())
                    && (calculateDistanceToPlane(hitResultTemp.getHitPose(), camera.getPose()) > 0);

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

    /**
     * Calculate the distance between a point in a space and a plane. This method is used
     * to calculate the distance between a camera in a space and a specified plane.
     *
     * @param planePose ARPose of a plane.
     * @param cameraPose ARPose of a camera.
     * @return Calculation results.
     */
    private static float calculateDistanceToPlane(ARPose planePose, ARPose cameraPose) {
        // The dimension of the direction vector is 3.
        float[] normals = new float[3];

        // Obtain the unit coordinate vector of a normal vector of a plane.
        planePose.getTransformedAxis(1, 1.0f, normals, 0);

        // Calculate the distance based on projection.
        return (cameraPose.tx() - planePose.tx()) * normals[0] // 0:x
            + (cameraPose.ty() - planePose.ty()) * normals[1] // 1:y
            + (cameraPose.tz() - planePose.tz()) * normals[2]; // 2:z
    }
}
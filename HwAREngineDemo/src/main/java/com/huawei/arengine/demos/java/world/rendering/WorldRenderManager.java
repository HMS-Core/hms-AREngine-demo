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

package com.huawei.arengine.demos.java.world.rendering;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.huawei.arengine.demos.R;
import com.huawei.arengine.demos.common.ArDemoRuntimeException;
import com.huawei.arengine.demos.common.DisplayRotationManager;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.arengine.demos.common.TextDisplay;
import com.huawei.arengine.demos.common.TextureDisplay;
import com.huawei.arengine.demos.java.utils.CommonUtil;
import com.huawei.arengine.demos.java.world.GestureEvent;
import com.huawei.arengine.demos.java.world.VirtualObject;
import com.huawei.hiar.ARCamera;
import com.huawei.hiar.ARConfigBase;
import com.huawei.hiar.ARFrame;
import com.huawei.hiar.ARHitResult;
import com.huawei.hiar.ARLightEstimate;
import com.huawei.hiar.ARPlane;
import com.huawei.hiar.ARPoint;
import com.huawei.hiar.ARPointCloud;
import com.huawei.hiar.ARPose;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.ARTarget;
import com.huawei.hiar.ARTrackable;
import com.huawei.hiar.ARWorldTrackingConfig;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * This class provides rendering management related to the world scene, including
 * label rendering and virtual object rendering management.
 *
 * @author HW
 * @since 2020-03-21
 */
public class WorldRenderManager implements GLSurfaceView.Renderer {
    private static final String TAG = WorldRenderManager.class.getSimpleName();

    private static final int PROJ_MATRIX_OFFSET = 0;

    private static final float PROJ_MATRIX_NEAR = 0.1f;

    private static final float PROJ_MATRIX_FAR = 100.0f;

    private static final float MATRIX_SCALE_SX = -1.0f;

    private static final float MATRIX_SCALE_SY = -1.0f;

    private static final float[] BLUE_COLORS = new float[] {66.0f, 133.0f, 244.0f, 255.0f};

    private static final float[] GREEN_COLORS = new float[] {66.0f, 244.0f, 133.0f, 255.0f};

    private static final int SIDE_LENGTH = 128;

    private static final int LIGHTING_CUBE_MAP_SINGLE_FACE_SIZE = SIDE_LENGTH * SIDE_LENGTH * 3;

    private static final int LIGHTING_CUBE_MAP_SIZE = LIGHTING_CUBE_MAP_SINGLE_FACE_SIZE * 6;

    private ARSession mSession;

    private ARWorldTrackingConfig mArWorldTrackingConfig;

    private Activity mActivity;

    private Context mContext;

    private TextView mTextView;

    private TextView mSearchingTextView;

    private ImageView mTextureImageTop;

    private ImageView mTextureImageBottom;

    private ImageView mTextureImageLeft;

    private ImageView mTextureImageRight;

    private ImageView mTextureImageFront;

    private ImageView mTextureImageBack;

    private int frames = 0;

    private long lastInterval;

    private float fps;

    private TextureDisplay mTextureDisplay = new TextureDisplay();

    private TextDisplay mTextDisplay = new TextDisplay();

    private LabelDisplay mLabelDisplay = new LabelDisplay();

    private ObjectDisplay mObjectDisplay = new ObjectDisplay();

    private PointCloudRenderer mPointCloud = new PointCloudRenderer();

    private DisplayRotationManager mDisplayRotationManager;

    private ArrayBlockingQueue<GestureEvent> mQueuedSingleTaps;

    private VirtualObject mSelectedObj = null;

    private ArrayList<VirtualObject> mVirtualObjects = new ArrayList<>();

    private TargetRenderManager mTargetRenderManager = new TargetRenderManager();

    private boolean mHaveSetEnvTextureData = false;

    private int mUpdateIndex = 0;

    /**
     * The constructor passes context and activity. This method will be called when {@link Activity#onCreate}.
     *
     * @param activity Activity
     * @param context Context
     */
    public WorldRenderManager(Activity activity, Context context) {
        mActivity = activity;
        mContext = context;
        mTextView = activity.findViewById(R.id.wordTextView);
        mSearchingTextView = activity.findViewById(R.id.searchingTextView);
        mTextureImageTop = activity.findViewById(R.id.img_env_texture_top);
        mTextureImageFront = activity.findViewById(R.id.img_env_texture_front);
        mTextureImageBottom = activity.findViewById(R.id.img_env_texture_bottom);
        mTextureImageRight = activity.findViewById(R.id.img_env_texture_right);
        mTextureImageLeft = activity.findViewById(R.id.img_env_texture_left);
        mTextureImageBack = activity.findViewById(R.id.img_env_Texture_back);
        LogUtil.info(TAG, "mSearchingTextView init.");
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
     * Set ARWorldTrackingConfig to obtain the configuration mode.
     *
     * @param arConfig ARWorldTrackingConfig.
     */
    public void setArWorldTrackingConfig(ARWorldTrackingConfig arConfig) {
        if (arConfig == null) {
            LogUtil.error(TAG, "setArWorldTrackingConfig error, arConfig is null!");
            return;
        }
        mArWorldTrackingConfig = arConfig;
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

        mTextureDisplay.init();
        mTextDisplay.setListener(new TextDisplay.OnTextInfoChangeListener() {
            @Override
            public void textInfoChanged(String text, float positionX, float positionY) {
                showWorldTypeTextView(text, positionX, positionY);
            }
        });

        mLabelDisplay.init(getPlaneBitmaps());

        mObjectDisplay.init(mContext);

        mPointCloud.init(mContext);

        mTargetRenderManager.init();

        mTargetRenderManager.initTargetLabelDisplay(getTargetLabelBitmaps(""));
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

            // Set the environment texture probe and mode after the camera is initialized.
            setEnvTextureData();
            ARCamera arCamera = arFrame.getCamera();

            // The size of the projection matrix is 4 * 4.
            float[] projectionMatrix = new float[16];
            arCamera.getProjectionMatrix(projectionMatrix, PROJ_MATRIX_OFFSET, PROJ_MATRIX_NEAR, PROJ_MATRIX_FAR);
            mTextureDisplay.onDrawFrame(arFrame);
            StringBuilder sb = new StringBuilder();
            updateMessageData(arFrame, sb);
            mTextDisplay.onDrawFrame(sb);

            // The size of ViewMatrix is 4 * 4.
            float[] viewMatrix = new float[16];
            arCamera.getViewMatrix(viewMatrix, 0);
            for (ARPlane plane : mSession.getAllTrackables(ARPlane.class)) {
                if (plane.getType() != ARPlane.PlaneType.UNKNOWN_FACING
                    && plane.getTrackingState() == ARTrackable.TrackingState.TRACKING) {
                    hideLoadingMessage();
                    break;
                }
            }
            drawTarget(mSession.getAllTrackables(ARTarget.class), arCamera, viewMatrix, projectionMatrix);
            mLabelDisplay.onDrawFrame(mSession.getAllTrackables(ARPlane.class), arCamera.getDisplayOrientedPose(),
                projectionMatrix);
            handleGestureEvent(arFrame, arCamera, projectionMatrix, viewMatrix);
            ARLightEstimate lightEstimate = arFrame.getLightEstimate();
            ARPointCloud arPointCloud = arFrame.acquirePointCloud();
            getEnvironmentTexture(lightEstimate);
            drawAllObjects(projectionMatrix, viewMatrix,  getPixelIntensity(lightEstimate));
            mPointCloud.onDrawFrame(arPointCloud, viewMatrix, projectionMatrix);
        } catch (ArDemoRuntimeException e) {
            LogUtil.error(TAG, "Exception on the ArDemoRuntimeException!");
        } catch (Throwable t) {
            // This prevents the app from crashing due to unhandled exceptions.
            LogUtil.error(TAG, "Exception on the OpenGL thread.");
        }
    }

    private void setEnvTextureData() {
        if (!mHaveSetEnvTextureData) {
            float[] boundBox = mObjectDisplay.getBoundingBox();
            mSession.setEnvironmentTextureProbe(boundBox);
            LogUtil.info(TAG, "setEnvironmentTextureProbe = " + Arrays.toString(boundBox));
            mSession.setEnvironmentTextureUpdateMode(ARSession.EnvironmentTextureUpdateMode.AUTO);
            mHaveSetEnvTextureData = true;
        }
    }

    private float getPixelIntensity(ARLightEstimate lightEstimate) {
        float lightPixelIntensity = 1;

        // Obtain the pixel light intensity when the light intensity mode is enabled.
        if ((mArWorldTrackingConfig.getLightingMode() & ARConfigBase.LIGHT_MODE_AMBIENT_INTENSITY) != 0) {
            lightPixelIntensity = lightEstimate.getPixelIntensity();
            LogUtil.info(TAG, "onDrawFrame: lightEstimate getPixelIntensity =" + lightPixelIntensity);
        }
        return lightPixelIntensity;
    }

    private void getEnvironmentTexture(ARLightEstimate lightEstimate) {
        if ((mSearchingTextView.getVisibility() != View.GONE)
            || (lightEstimate.getState() != ARLightEstimate.State.VALID)) {
            return;
        }

        // Obtain the environment texture data when the environment texture mode is enabled.
        if ((mArWorldTrackingConfig.getLightingMode() & ARConfigBase.LIGHT_MODE_ENVIRONMENT_TEXTURE) != 0) {
            ByteBuffer byteBuffer = lightEstimate.acquireEnvironmentTexture();
            if (byteBuffer != null) {
                // Update the environment texture every 10 frames.
                if ((mUpdateIndex % 10) == 0) {
                    byte[] bytes = new byte[LIGHTING_CUBE_MAP_SIZE];
                    byteBuffer.get(bytes);
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateTextureDisplay(bytes);
                            mUpdateIndex = 0;
                        }
                    });
                }
                mUpdateIndex++;
            }
        }
    }

    private void updateTextureDisplay(byte[] bytes) {
        ImageView[] cubeMapImgView = new ImageView[]{mTextureImageRight, mTextureImageLeft, mTextureImageTop,
            mTextureImageBottom, mTextureImageFront, mTextureImageBack};

        // The environment texture is a cube mapping diagram, including six surfaces.
        // The side 0 indicates the right side, and the side 1 indicates the left side.
        // The side 2 indicates the top side, and the side 3 indicates the bottom side.
        // The side 4 indicates the front side, and the side 5 indicates the rear side.
        for (int i = 0; i < cubeMapImgView.length; i++) {
            Optional<Bitmap> cubeMapFace = CommonUtil.createBitmapImage(Arrays.copyOfRange(bytes,
                LIGHTING_CUBE_MAP_SINGLE_FACE_SIZE * i,
                LIGHTING_CUBE_MAP_SINGLE_FACE_SIZE * (i + 1)), SIDE_LENGTH, SIDE_LENGTH);
            cubeMapFace.ifPresent(cubeMapImgView[i]::setImageBitmap);
        }
    }

    private void drawTarget(Collection<ARTarget> allEntities, ARCamera camera, float[] cameraView,
        float[] cameraPerspective) {
        if (camera.getTrackingState() != ARTrackable.TrackingState.TRACKING) {
            LogUtil.debug(TAG, "ARCamera isn't TRACKING.");
            return;
        }
        TargetRenderer targetRenderer = null;
        for (ARTarget target : allEntities) {
            if (target.getTrackingState() != ARTrackable.TrackingState.TRACKING
                || target.getShapeType() == ARTarget.TargetShapeType.TARGET_SHAPE_INVALID) {
                continue;
            }
            targetRenderer = mTargetRenderManager.getTargetRenderByType(target.getShapeType());
            if (targetRenderer == null) {
                continue;
            }
            targetRenderer.updateParameters(target);
            targetRenderer.draw(cameraView, cameraPerspective);
            LabelDisplay targetLabelDisplay = mTargetRenderManager.getTargetLabelDisplay();
            targetLabelDisplay.onDrawFrame(target, getTargetLabelBitmaps(targetRenderer.getTargetInfo()).get(0), camera,
                cameraPerspective);
        }
    }

    private void drawAllObjects(float[] projectionMatrix, float[] viewMatrix, float lightPixelIntensity) {
        Iterator<VirtualObject> ite = mVirtualObjects.iterator();
        while (ite.hasNext()) {
            VirtualObject obj = ite.next();
            if (obj.getAnchor().getTrackingState() == ARTrackable.TrackingState.STOPPED) {
                ite.remove();
            }
            if (obj.getAnchor().getTrackingState() == ARTrackable.TrackingState.TRACKING) {
                mObjectDisplay.onDrawFrame(viewMatrix, projectionMatrix, lightPixelIntensity, obj);
            }
        }
    }

    private ArrayList<Bitmap> getPlaneBitmaps() {
        ArrayList<Bitmap> bitmaps = new ArrayList<>();
        bitmaps.add(getPlaneBitmap(R.id.plane_other));
        bitmaps.add(getPlaneBitmap(R.id.plane_wall));
        bitmaps.add(getPlaneBitmap(R.id.plane_floor));
        bitmaps.add(getPlaneBitmap(R.id.plane_seat));
        bitmaps.add(getPlaneBitmap(R.id.plane_table));
        bitmaps.add(getPlaneBitmap(R.id.plane_ceiling));
        bitmaps.add(getPlaneBitmap(R.id.plane_door));
        bitmaps.add(getPlaneBitmap(R.id.plane_window));
        bitmaps.add(getPlaneBitmap(R.id.plane_bed));
        return bitmaps;
    }

    private ArrayList<Bitmap> getTargetLabelBitmaps(String textStr) {
        ArrayList<Bitmap> bitmaps = new ArrayList<>(1);
        TextView view = mActivity.findViewById(R.id.target_measure);
        if (view == null) {
            LogUtil.error(TAG, "getTargetLabelBitmaps id invalid.");
            return bitmaps;
        }
        if (!textStr.isEmpty()) {
            view.setText(textStr);
        }
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        view.setDrawingCacheEnabled(true);
        view.destroyDrawingCache();
        view.buildDrawingCache();
        Bitmap bitmap = view.getDrawingCache();
        LogUtil.debug(TAG, "Image bitmap create start!");
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.setScale(MATRIX_SCALE_SX, MATRIX_SCALE_SY);
        if (bitmap != null) {
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
        LogUtil.debug(TAG, "Image bitmap create end!");
        bitmaps.add(bitmap);
        return bitmaps;
    }

    private Bitmap getPlaneBitmap(int id) {
        TextView view = mActivity.findViewById(id);
        view.setDrawingCacheEnabled(true);
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        Bitmap bitmap = view.getDrawingCache();
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.setScale(MATRIX_SCALE_SX, MATRIX_SCALE_SY);
        if (bitmap != null) {
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
        return bitmap;
    }

    private void updateMessageData(ARFrame arFrame, StringBuilder sb) {
        float fpsResult = doFpsCalculate();
        sb.append("FPS=").append(fpsResult).append(System.lineSeparator());

        ARLightEstimate lightEstimate = arFrame.getLightEstimate();

        if ((mSearchingTextView.getVisibility() != View.GONE)
            || (lightEstimate.getState() != ARLightEstimate.State.VALID)) {
            return;
        }

        // Obtain the estimated light data when the light intensity mode is enabled.
        if ((mArWorldTrackingConfig.getLightingMode() & ARConfigBase.LIGHT_MODE_AMBIENT_INTENSITY) != 0) {
            sb.append("PixelIntensity=").append(lightEstimate.getPixelIntensity()).append(System.lineSeparator());
        }

        // Obtain the texture data when the environment texture mode is enabled.
        if ((mArWorldTrackingConfig.getLightingMode() & ARConfigBase.LIGHT_MODE_ENVIRONMENT_LIGHTING) != 0) {
            sb.append("PrimaryLightIntensity=").append(lightEstimate.getPrimaryLightIntensity())
                .append(System.lineSeparator());
            sb.append("PrimaryLightDirection=").append(Arrays.toString(lightEstimate.getPrimaryLightDirection()))
                .append(System.lineSeparator());
            sb.append("PrimaryLightColor=").append(Arrays.toString(lightEstimate.getPrimaryLightColor()))
                .append(System.lineSeparator());
            sb.append("LightShadowType=").append(lightEstimate.getLightShadowType()).append(System.lineSeparator());
            sb.append("LightShadowStrength=").append(lightEstimate.getShadowStrength()).append(System.lineSeparator());
            sb.append("LightSphericalHarmonicCoefficients=")
                .append(Arrays.toString(lightEstimate.getSphericalHarmonicCoefficients()))
                .append(System.lineSeparator());
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

    private void hideLoadingMessage() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mSearchingTextView != null) {
                    mSearchingTextView.setVisibility(View.GONE);
                }
            }
        });
    }

    private void handleGestureEvent(ARFrame arFrame, ARCamera arCamera, float[] projectionMatrix, float[] viewMatrix) {
        GestureEvent event = mQueuedSingleTaps.poll();
        if (event == null) {
            return;
        }

        // Do not perform anything when the object is not tracked.
        if (arCamera.getTrackingState() != ARTrackable.TrackingState.TRACKING) {
            return;
        }

        int eventType = event.getType();
        switch (eventType) {
            case GestureEvent.GESTURE_EVENT_TYPE_DOUBLETAP: {
                doWhenEventTypeDoubleTap(viewMatrix, projectionMatrix, event);
                break;
            }
            case GestureEvent.GESTURE_EVENT_TYPE_SCROLL: {
                if (mSelectedObj == null) {
                    break;
                }
                ARHitResult hitResult = hitTest4Result(arFrame, arCamera, event.getEventSecond());
                if (hitResult != null) {
                    mSelectedObj.setAnchor(hitResult.createAnchor());
                }
                break;
            }
            case GestureEvent.GESTURE_EVENT_TYPE_SINGLETAPCONFIRMED: {
                // Do not perform anything when an object is selected.
                if (mSelectedObj != null) {
                    mSelectedObj.setIsSelected(false);
                    mSelectedObj = null;
                }

                MotionEvent tap = event.getEventFirst();
                ARHitResult hitResult = null;

                hitResult = hitTest4Result(arFrame, arCamera, tap);

                if (hitResult == null) {
                    break;
                }
                doWhenEventTypeSingleTap(hitResult);
                break;
            }
            default: {
                LogUtil.error(TAG, "Unknown motion event type, and do nothing.");
            }
        }
    }

    private void doWhenEventTypeDoubleTap(float[] viewMatrix, float[] projectionMatrix, GestureEvent event) {
        if (mSelectedObj != null) {
            mSelectedObj.setIsSelected(false);
            mSelectedObj = null;
        }
        for (VirtualObject obj : mVirtualObjects) {
            if (mObjectDisplay.hitTest(viewMatrix, projectionMatrix, obj, event.getEventFirst())) {
                obj.setIsSelected(true);
                mSelectedObj = obj;
                break;
            }
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
        if (currentTrackable instanceof ARPoint) {
            mVirtualObjects.add(new VirtualObject(hitResult.createAnchor(), BLUE_COLORS));
        } else if (currentTrackable instanceof ARPlane) {
            mVirtualObjects.add(new VirtualObject(hitResult.createAnchor(), GREEN_COLORS));
        } else {
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

    /**
     * Release the anchor when the activity is destroyed.
     */
    public void releaseARAnchor() {
        if (mVirtualObjects == null) {
            return;
        }
        for (VirtualObject object : mVirtualObjects) {
            object.detachAnchor();
        }
    }
}
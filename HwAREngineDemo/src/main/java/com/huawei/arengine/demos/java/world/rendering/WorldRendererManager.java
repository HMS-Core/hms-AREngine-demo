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

package com.huawei.arengine.demos.java.world.rendering;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.huawei.arengine.demos.R;
import com.huawei.arengine.demos.common.ArDemoRuntimeException;
import com.huawei.arengine.demos.common.BaseRendererManager;
import com.huawei.arengine.demos.common.GestureEvent;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.arengine.demos.common.ObjectDisplay;
import com.huawei.arengine.demos.common.VirtualObject;
import com.huawei.arengine.demos.java.utils.CommonUtil;
import com.huawei.hiar.ARCamera;
import com.huawei.hiar.ARConfigBase;
import com.huawei.hiar.ARFrame;
import com.huawei.hiar.ARHitResult;
import com.huawei.hiar.ARLightEstimate;
import com.huawei.hiar.ARPlane;
import com.huawei.hiar.ARPoint;
import com.huawei.hiar.ARPointCloud;
import com.huawei.hiar.ARSession;
import com.huawei.hiar.ARTarget;
import com.huawei.hiar.ARTrackable;
import com.huawei.hiar.ARWorldTrackingConfig;
import com.huawei.hiar.exceptions.ARDeadlineExceededException;
import com.huawei.hiar.exceptions.ARFatalException;
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * This class provides rendering management related to the world scene, including
 * label rendering and virtual object rendering management.
 *
 * @author HW
 * @since 2020-03-21
 */
public class WorldRendererManager extends BaseRendererManager implements BaseRendererManager.BaseRenderer {
    private static final String TAG = WorldRendererManager.class.getSimpleName();

    private static final float MATRIX_SCALE_SX = -1.0f;

    private static final float MATRIX_SCALE_SY = -1.0f;

    private static final float[] BLUE_COLORS = new float[] {66.0f, 133.0f, 244.0f, 255.0f};

    private static final float[] GREEN_COLORS = new float[] {66.0f, 244.0f, 133.0f, 255.0f};

    private static final int SIDE_LENGTH = 128;

    private static final int LIGHTING_CUBE_MAP_SINGLE_FACE_SIZE = SIDE_LENGTH * SIDE_LENGTH * 3;

    private static final int LIGHTING_CUBE_MAP_SIZE = LIGHTING_CUBE_MAP_SINGLE_FACE_SIZE * 6;

    private ARWorldTrackingConfig mArWorldTrackingConfig;

    private TextView mSearchingTextView;

    private ImageView mTextureImageTop;

    private ImageView mTextureImageBottom;

    private ImageView mTextureImageLeft;

    private ImageView mTextureImageRight;

    private ImageView mTextureImageFront;

    private ImageView mTextureImageBack;

    private String mTargetInfo;

    private AtomicReference<Bitmap> mBitmap = new AtomicReference<>();

    private LabelDisplay mLabelDisplay = new LabelDisplay();

    private ObjectDisplay mObjectDisplay = new ObjectDisplay();

    private PointCloudRenderer mPointCloud = new PointCloudRenderer();

    private ArrayBlockingQueue<GestureEvent> mQueuedSingleTaps;

    private VirtualObject mSelectedObj = null;

    private ArrayList<VirtualObject> mVirtualObjects = new ArrayList<>();

    private TargetRenderManager mTargetRenderManager = new TargetRenderManager();

    private boolean hasSetEnvTextureData = false;

    private int mUpdateIndex = 0;

    /**
     * The constructor passes context and activity. This method will be called when {@link Activity#onCreate}.
     *
     * @param activity Activity
     */
    public WorldRendererManager(Activity activity) {
        mActivity = activity;
        mSearchingTextView = activity.findViewById(R.id.searchingTextView);
        mTextureImageTop = activity.findViewById(R.id.img_env_texture_top);
        mTextureImageFront = activity.findViewById(R.id.img_env_texture_front);
        mTextureImageBottom = activity.findViewById(R.id.img_env_texture_bottom);
        mTextureImageRight = activity.findViewById(R.id.img_env_texture_right);
        mTextureImageLeft = activity.findViewById(R.id.img_env_texture_left);
        mTextureImageBack = activity.findViewById(R.id.img_env_Texture_back);
        setRenderer(this);
        LogUtil.info(TAG, "mSearchingTextView init.");
    }

    /**
     * Set ARWorldTrackingConfig to obtain the configuration mode.
     *
     * @param arConfig ARWorldTrackingConfig.
     */
    public void setArWorldTrackingConfig(ARConfigBase arConfig) {
        if (arConfig == null) {
            LogUtil.error(TAG, "setArWorldTrackingConfig error, arConfig is null!");
            return;
        }
        if (arConfig instanceof ARWorldTrackingConfig) {
            mArWorldTrackingConfig = (ARWorldTrackingConfig) arConfig;
        }
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

    @Override
    public void surfaceCreated(GL10 gl, EGLConfig config) {
        mLabelDisplay.init(getPlaneBitmaps());

        mObjectDisplay.init(mActivity);

        mPointCloud.init(mActivity);

        mTargetRenderManager.init();

        mTargetRenderManager.initTargetLabelDisplay(getTargetLabelBitmaps(""));
    }

    @Override
    public void surfaceChanged(GL10 unused, int width, int height) {
        mObjectDisplay.setSize(width, height);
    }

    @Override
    public void drawFrame(GL10 unused) {
        try {
            // Set the environment texture probe and mode after the camera is initialized.
            setEnvTextureData();

            StringBuilder sb = new StringBuilder();
            updateMessageData(mArFrame, sb);
            mTextDisplay.onDrawFrame(sb.toString());

            for (ARPlane plane : mSession.getAllTrackables(ARPlane.class)) {
                if (plane.getType() != ARPlane.PlaneType.UNKNOWN_FACING
                    && plane.getTrackingState() == ARTrackable.TrackingState.TRACKING) {
                    hideLoadingMessage();
                    break;
                }
            }
            drawTarget(mSession.getAllTrackables(ARTarget.class), mArCamera, mViewMatrix, mProjectionMatrix);
            mLabelDisplay.onDrawFrame(mSession.getAllTrackables(ARPlane.class), mArCamera.getDisplayOrientedPose(),
                mProjectionMatrix);
            handleGestureEvent(mArFrame, mArCamera, mProjectionMatrix, mViewMatrix);
            ARLightEstimate lightEstimate = mArFrame.getLightEstimate();
            ARPointCloud arPointCloud = mArFrame.acquirePointCloud();
            getEnvironmentTexture(lightEstimate);
            drawAllObjects(mProjectionMatrix, mViewMatrix, getPixelIntensity(lightEstimate));
            mPointCloud.onDrawFrame(arPointCloud, mViewMatrix, mProjectionMatrix);
        } catch (ArDemoRuntimeException e) {
            LogUtil.error(TAG, "Exception on the ArDemoRuntimeException!");
        } catch (ARFatalException | IllegalArgumentException | ARDeadlineExceededException |
            ARUnavailableServiceApkTooOldException t) {
            // This prevents the app from crashing due to unhandled exceptions.
            LogUtil.error(TAG, "Exception on the OpenGL thread. Name:" + t.getClass());
        }
    }

    private void setEnvTextureData() {
        if (!hasSetEnvTextureData) {
            float[] boundBox = mObjectDisplay.getBoundingBox();
            mSession.setEnvironmentTextureProbe(boundBox);
            LogUtil.info(TAG, "setEnvironmentTextureProbe = " + Arrays.toString(boundBox));
            mSession.setEnvironmentTextureUpdateMode(ARSession.EnvironmentTextureUpdateMode.AUTO);
            hasSetEnvTextureData = true;
        }
    }

    private float getPixelIntensity(ARLightEstimate lightEstimate) {
        float lightPixelIntensity = 1f;

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
        if ((mArWorldTrackingConfig.getLightingMode() & ARConfigBase.LIGHT_MODE_ENVIRONMENT_TEXTURE) == 0) {
            return;
        }
        ByteBuffer byteBuffer = lightEstimate.acquireEnvironmentTexture();
        if (byteBuffer == null) {
            return;
        }

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

    private void updateTextureDisplay(byte[] bytes) {
        ImageView[] cubeMapImgView = new ImageView[] {mTextureImageRight, mTextureImageLeft, mTextureImageTop,
            mTextureImageBottom, mTextureImageFront, mTextureImageBack};

        // The environment texture is a cube mapping diagram, including six surfaces.
        // The side 0 indicates the right side, and the side 1 indicates the left side.
        // The side 2 indicates the top side, and the side 3 indicates the bottom side.
        // The side 4 indicates the front side, and the side 5 indicates the rear side.
        for (int i = 0; i < cubeMapImgView.length; i++) {
            Optional<Bitmap> cubeMapFace =
                CommonUtil.createBitmapImage(Arrays.copyOfRange(bytes, LIGHTING_CUBE_MAP_SINGLE_FACE_SIZE * i,
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
            String finalTargetInfo = targetRenderer.getTargetInfo();
            if (!finalTargetInfo.equals(mTargetInfo)) {
                mActivity.runOnUiThread(() -> {
                    mBitmap.set(getTargetLabelBitmaps(finalTargetInfo).get(0));
                });
                mTargetInfo = finalTargetInfo;
            }
            Bitmap currentBitmap = mBitmap.get();
            if (currentBitmap != null) {
                targetLabelDisplay.onDrawFrame(target, currentBitmap, camera, cameraPerspective);
            }
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
        AtomicReference<Bitmap> bitmap = new AtomicReference<>();
        Optional<Bitmap> op = CommonUtil.getBitmapFromView(mActivity.findViewById(R.id.target_measure),
            MATRIX_SCALE_SX, MATRIX_SCALE_SY);
        op.ifPresent(object -> {
            bitmap.set(object);
        });
        if (bitmap != null) {
            bitmaps.add(bitmap.get());
        }
        return bitmaps;
    }

    private Bitmap getPlaneBitmap(int id) {
        AtomicReference<Bitmap> bitmap = new AtomicReference<>();
        Optional<Bitmap> op = CommonUtil.getBitmapFromView(mActivity.findViewById(id),
            MATRIX_SCALE_SX, MATRIX_SCALE_SY);
        op.ifPresent(object -> {
            bitmap.set(object);
        });
        return bitmap.get();
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
            sb.append("PrimaryLightIntensity=")
                .append(lightEstimate.getPrimaryLightIntensity())
                .append(System.lineSeparator());
            sb.append("PrimaryLightDirection=")
                .append(Arrays.toString(lightEstimate.getPrimaryLightDirection()))
                .append(System.lineSeparator());
            sb.append("PrimaryLightColor=")
                .append(Arrays.toString(lightEstimate.getPrimaryLightColor()))
                .append(System.lineSeparator());
            sb.append("LightShadowType=").append(lightEstimate.getLightShadowType()).append(System.lineSeparator());
            sb.append("LightShadowStrength=").append(lightEstimate.getShadowStrength()).append(System.lineSeparator());
            sb.append("LightSphericalHarmonicCoefficients=")
                .append(Arrays.toString(lightEstimate.getSphericalHarmonicCoefficients()))
                .append(System.lineSeparator());
        }
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

    /**
     * Release the anchor when destroying Activity.
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
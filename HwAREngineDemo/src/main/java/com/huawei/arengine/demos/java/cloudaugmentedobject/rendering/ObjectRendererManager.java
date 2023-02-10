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

package com.huawei.arengine.demos.java.cloudaugmentedobject.rendering;

import android.app.Activity;

import com.huawei.arengine.demos.common.ArDemoRuntimeException;
import com.huawei.arengine.demos.common.BaseRendererManager;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.hiar.ARObject;
import com.huawei.hiar.ARPose;
import com.huawei.hiar.exceptions.ARDeadlineExceededException;
import com.huawei.hiar.exceptions.ARFatalException;
import com.huawei.hiar.exceptions.ARUnavailableServiceApkTooOldException;

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
public class ObjectRendererManager extends BaseRendererManager implements BaseRendererManager.BaseRenderer {
    private static final String TAG = ObjectRendererManager.class.getSimpleName();

    private ArrayList<ObjectRelatedDisplay> mObjectRelatedDisplays = new ArrayList<>();

    /**
     * The ObjectRenderManager constructor.
     *
     * @param activity Activity.
     */
    public ObjectRendererManager(Activity activity) {
        mActivity = activity;
        ObjectRelatedDisplay objectLabelDisplay = new ObjectLabelDisplay(mActivity);
        mObjectRelatedDisplays.add(objectLabelDisplay);
        setRenderer(this);
    }

    @Override
    public void surfaceCreated(GL10 gl, EGLConfig config) {
        for (ObjectRelatedDisplay objectRelatedDisplay : mObjectRelatedDisplays) {
            objectRelatedDisplay.init();
        }
    }

    @Override
    public void surfaceChanged(GL10 gl, int width, int height) {
    }

    @Override
    public void drawFrame(GL10 gl) {
        try {
            Collection<ARObject> updatedObjects = mSession.getAllTrackables(ARObject.class);
            for (ObjectRelatedDisplay objectRelatedDisplay : mObjectRelatedDisplays) {
                objectRelatedDisplay.onDrawFrame(updatedObjects, mViewMatrix, mProjectionMatrix,
                    mArCamera.getDisplayOrientedPose());
            }
            LogUtil.debug(TAG, "onDrawFrame: Updated ARObject is " + updatedObjects.size());
            StringBuilder sb = new StringBuilder();
            updateMessageData(sb, updatedObjects);
            mTextDisplay.onDrawFrame(sb.toString());
        } catch (ArDemoRuntimeException e) {
            LogUtil.error(TAG, "Exception on the ArDemoRuntimeException!");
        } catch (ARFatalException | IllegalArgumentException | ARDeadlineExceededException |
            ARUnavailableServiceApkTooOldException t) {
            // This prevents the app from crashing due to unhandled exceptions.
            LogUtil.error(TAG, "Exception on the OpenGL thread. " + t.getClass());
        }
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
}

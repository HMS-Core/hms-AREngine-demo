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

import android.graphics.Bitmap;

import com.huawei.hiar.ARTarget;

import java.util.ArrayList;

/**
 * Manager class of the target semantic renderer.
 *
 * @author HW
 * @since 2021-07-21
 */
public class TargetRenderManager {
    private AabbRenderer mAabbRenderer = new AabbRenderer();

    private CircleRenderer mCircleRenderer = new CircleRenderer();

    private RectRenderer mRectRenderer = new RectRenderer();

    private LabelDisplay mTargetLabelDisplay = new LabelDisplay();

    /**
     * Obtain the corresponding renderer based on the target shape.
     *
     * @param targetShapeType Target shape.
     * @return Renderer.
     */
    public TargetRenderer getTargetRenderByType(ARTarget.TargetShapeType targetShapeType) {
        switch (targetShapeType) {
            case TARGET_SHAPE_RECT:
                return mRectRenderer;
            case TARGET_SHAPE_CIRCLE:
                return mCircleRenderer;
            default:
                return mAabbRenderer;
        }
    }

    /**
     * Initialize the shape renderer.
     */
    public void init() {
        mAabbRenderer.createOnGlThread();
        mCircleRenderer.createOnGlThread();
        mRectRenderer.createOnGlThread();
    }

    /**
     * Initialize the label renderer.
     *
     * @param bitmaps Image data.
     */
    public void initTargetLabelDisplay(ArrayList<Bitmap> bitmaps) {
        mTargetLabelDisplay.init(bitmaps);
    }

    /**
     * Obtain the label renderer.
     *
     * @return Label renderer object.
     */
    public LabelDisplay getTargetLabelDisplay() {
        return mTargetLabelDisplay;
    }
}

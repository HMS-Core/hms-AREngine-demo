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

package com.huawei.arengine.demos.world.controller

import android.graphics.Bitmap

import com.huawei.arengine.demos.world.service.AabbRendererService
import com.huawei.arengine.demos.world.service.CircleRendererService
import com.huawei.arengine.demos.world.service.LabelService
import com.huawei.arengine.demos.world.service.RectRendererService
import com.huawei.hiar.ARTarget

/**
 * Manager class of the target semantic renderer.
 *
 * @author HW
 * @since 2022-06-17
 */
class TargetRenderManager {
    private var mAabbRenderer = AabbRendererService()

    private var mCircleRenderer = CircleRendererService()

    private var mRectRenderer = RectRendererService()

    private var mTargetLabelDisplay = LabelService()

    /**
     * Obtain the corresponding renderer based on the target shape.
     *
     * @param targetShapeType Target shape.
     * @return Renderer.
     */
    fun getTargetRenderByType(targetShapeType: ARTarget.TargetShapeType?): TargetRenderController {
        return when (targetShapeType) {
            ARTarget.TargetShapeType.TARGET_SHAPE_RECT -> mRectRenderer
            ARTarget.TargetShapeType.TARGET_SHAPE_CIRCLE -> mCircleRenderer
            else -> mAabbRenderer
        }
    }

    /**
     * Initialize the shape renderer.
     */
    fun init() {
        mAabbRenderer.createOnGlThread()
        mCircleRenderer.createOnGlThread()
        mRectRenderer.createOnGlThread()
    }

    /**
     * Initialize the label renderer.
     *
     * @param bitmaps Image data.
     */
    fun initTargetLabelDisplay(bitmaps: ArrayList<Bitmap>) {
        mTargetLabelDisplay.init(bitmaps)
    }

    /**
     * Obtain the label renderer.
     *
     * @return Label renderer object.
     */
    fun getTargetLabelDisplay(): LabelService {
        return mTargetLabelDisplay
    }
}
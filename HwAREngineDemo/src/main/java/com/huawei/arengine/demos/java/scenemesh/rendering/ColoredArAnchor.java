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

package com.huawei.arengine.demos.java.scenemesh.rendering;

import com.huawei.hiar.ARAnchor;

/**
 * Colored AR anchor.
 *
 * @author hw
 * @since 2021-01-27
 */
public class ColoredArAnchor {
    /**
     * OBJ color flag. AR_TRACKABLE_POINT is blue.
     */
    public static final String AR_TRACK_POINT_COLOR = "track_point_color";

    /**
     * OBJ color flag. AR_TRACKABLE_PLANE is green.
     */
    public static final String AR_TRACK_PLANE_COLOR = "track_plane_color";

    /**
     * OBJ color flag. The default color is white.
     */
    public static final String AR_DEFAULT_COLOR = "default_color";

    private final ARAnchor anchor;

    private final String colorFlag;

    ColoredArAnchor(ARAnchor arAnchor, String colorType) {
        this.anchor = arAnchor;
        this.colorFlag = colorType;
    }

    /**
     * Obtains the anchor color flag. The data selects the customized color based on the color flag.
     *
     * @return OBJ Model color type. AR_TRACKABLE_POINT is blue, and AR_TRACKABLE_PLANE is green.
     */
    public String getColor() {
        return colorFlag;
    }

    /**
     * Anchor color.
     *
     * @return AR anchor.
     */
    public ARAnchor getAnchor() {
        return anchor;
    }
}

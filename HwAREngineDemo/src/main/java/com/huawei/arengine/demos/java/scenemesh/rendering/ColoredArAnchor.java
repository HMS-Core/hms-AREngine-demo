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

package com.huawei.arengine.demos.java.scenemesh.rendering;

import com.huawei.hiar.ARAnchor;

/**
 * 着色的AR锚点。
 *
 * @author hw
 * @since 2021-01-27
 */
public class ColoredArAnchor {
    /**
     * OBJ颜色标志位，AR_TRACKABLE_POINT为蓝色。
     */
    public static final String AR_TRACK_POINT_COLOR = "track_point_color";

    /**
     * OBJ颜色标志位，AR_TRACKABLE_PLANE为绿色。
     */
    public static final String AR_TRACK_PLANE_COLOR = "track_plane_color";

    /**
     * OBJ颜色标志位，默认白色。
     */
    public static final String AR_DEFAULT_COLOR = "default_color";

    private final ARAnchor anchor;

    private final String colorFlag;

    ColoredArAnchor(ARAnchor arAnchor, String colorType) {
        this.anchor = arAnchor;
        this.colorFlag = colorType;
    }

    /**
     * 获取锚点颜色标志，Data根据颜色标志选取自定义的颜色。
     *
     * @return OBJ模型颜色类型，AR_TRACKABLE_POINT为蓝色，AR_TRACKABLE_PLANE为绿色。
     */
    public String getColor() {
        return colorFlag;
    }

    /**
     * 锚的颜色。
     *
     * @return AR锚点。
     */
    public ARAnchor getAnchor() {
        return anchor;
    }
}

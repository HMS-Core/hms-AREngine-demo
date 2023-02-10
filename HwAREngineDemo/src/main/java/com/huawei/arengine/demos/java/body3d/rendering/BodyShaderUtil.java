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

package com.huawei.arengine.demos.java.body3d.rendering;

import com.huawei.arengine.demos.common.ShaderUtil;

/**
 * This class provides code and programs related to body rendering shader.
 *
 * @author hw
 * @since 2020-03-31
 */
class BodyShaderUtil {
    private static final String TAG = BodyShaderUtil.class.getSimpleName();

    /**
     * Newline character.
     */
    public static final String LS = System.lineSeparator();

    /**
     * Code for the vertex shader.
     */
    public static final String SHADOW_VERTEX =
        "attribute vec4 a_Position;" + LS
        + "attribute vec2 a_TexCoord;" + LS
        + "uniform mat4 vMatrix;" + LS
        + "uniform mat4 vCoordMatrix;" + LS
        + "varying vec2 v_TexCoord;" + LS
        + "void main(){" + LS
        + "    gl_Position = vMatrix*a_Position;" + LS
        + "    v_TexCoord = (vCoordMatrix*vec4(a_TexCoord,0,1)).xy;" + LS
        + "}";

    /**
     * Code for the segment shader.
     */
    public static final String SHADOW_FRAGMENT =
        "#extension GL_OES_EGL_image_external : require" + LS
        + "precision mediump float;" + LS
        + "varying vec2 v_TexCoord;" + LS
        + "uniform samplerExternalOES vTexture;" + LS
        + "uniform sampler2D u_Mask;" + LS
        + "uniform int u_UseMask;" + LS
        + "void main() {" + LS
        + "    if (u_UseMask == 1) {" + LS
        + "        vec4 mColor = texture2D(u_Mask, v_TexCoord);" + LS
        + "        gl_FragColor = texture2D(vTexture, v_TexCoord) * (1.0 - mColor.r);" + LS
        + "    } else {" + LS
        + "        gl_FragColor = texture2D(vTexture, v_TexCoord);" + LS
        + "    }" + LS
        + "}";

    /**
     * Code for the vertex shader.
     */
    public static final String BODY_VERTEX =
        "uniform vec4 inColor;" + LS
        + "attribute vec4 inPosition;" + LS
        + "uniform float inPointSize;" + LS
        + "varying vec4 varColor;" + LS
        + "uniform mat4 inProjectionMatrix;" + LS
        + "uniform float inCoordinateSystem;" + LS
        + "void main() {" + LS
        + "    vec4 position = vec4(inPosition.xyz, 1.0);" + LS
        + "    if (inCoordinateSystem == 2.0) {" + LS
        + "        position = inProjectionMatrix * position;" + LS
        + "    }" + LS
        + "    gl_Position = position;" + LS
        + "    varColor = inColor;" + LS
        + "    gl_PointSize = inPointSize;" + LS
        + "}";

    /**
     * Code for the segment shader.
     */
    public static final String BODY_FRAGMENT =
        "precision mediump float;" + LS
        + "varying vec4 varColor;" + LS
        + "void main() {" + LS
        + "    gl_FragColor = varColor;" + LS
        + "}";

    private BodyShaderUtil() {
    }

    /**
     * Create a shader.
     *
     * @return Shader program.
     */
    static int createSkeletonGlProgram() {
        return ShaderUtil.createGlProgram(BODY_VERTEX, BODY_FRAGMENT);
    }

    /**
     * Create a shader.
     *
     * @return Shader program.
     */
    static int createShadowGlProgram() {
        return ShaderUtil.createGlProgram(SHADOW_VERTEX, SHADOW_FRAGMENT);
    }
}
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

/**
 * This class provides code and program for the rendering shader related to the world scene.
 *
 * @author hw
 * @since 2020-04-11
 */
public class WorldShaderUtil {
    private static final String LS = System.lineSeparator();

    private static final String OBJECT_VERTEX =
        "uniform mat4 inMVPMatrix;" + LS
        + "uniform mat4 inViewMatrix;" + LS
        + "attribute vec3 inObjectNormalVector;" + LS
        + "attribute vec4 inObjectPosition;" + LS
        + "attribute vec2 inTexCoordinate;" + LS
        + "varying vec3 varCameraNormalVector;" + LS
        + "varying vec2 varTexCoordinate;" + LS
        + "varying vec3 varCameraPos;" + LS
        + "void main() {" + LS
        + "    gl_Position = inMVPMatrix * inObjectPosition;" + LS
        + "    varCameraNormalVector = (inViewMatrix * vec4(inObjectNormalVector, 0.0)).xyz;" + LS
        + "    varTexCoordinate = inTexCoordinate;" + LS
        + "    varCameraPos = (inViewMatrix * inObjectPosition).xyz;" + LS
        + "}";

    private static final String OBJECT_FRAGMENT =
        "precision mediump float;" + LS
        + " uniform vec4 inLight;" + LS
        + "uniform vec4 inObjectColor;" + LS
        + "uniform sampler2D inObjectTexture;" + LS
        + "varying vec3 varCameraPos;" + LS
        + "varying vec3 varCameraNormalVector;" + LS
        + "varying vec2 varTexCoordinate;" + LS
        + "void main() {" + LS
        + "    vec4 objectColor = texture2D(inObjectTexture, vec2(varTexCoordinate.x, 1.0 - varTexCoordinate.y));" + LS
        + "    objectColor.rgb = inObjectColor.rgb / 255.0;" + LS
        + "    vec3 viewNormal = normalize(varCameraNormalVector);" + LS
        + "    vec3 reflectedLightDirection = reflect(inLight.xyz, viewNormal);" + LS
        + "    vec3 normalCameraPos = normalize(varCameraPos);" + LS
        + "    float specularStrength = max(0.0, dot(normalCameraPos, reflectedLightDirection));" + LS
        + "    gl_FragColor.a = objectColor.a;" + LS
        + "    float diffuse = inLight.w * 3.5 *" + LS
        + "        0.5 * (dot(viewNormal, inLight.xyz) + 1.0);" + LS
        + "    float specular = inLight.w *" + LS
        + "        pow(specularStrength, 6.0);" + LS
        + "    gl_FragColor.rgb = objectColor.rgb * + diffuse + specular;" + LS
        + "}";

    private static final String POINTCLOUD_VERTEX =
        "uniform mat4 u_ModelViewProjection;" + LS
            + "uniform vec4 u_Color;" + LS
            + "uniform float u_PointSize;" + LS
            + "attribute vec4 a_Position;" + LS
            + "varying vec4 v_Color;" + LS
            + "void main() {" + LS
            + "   v_Color = u_Color;" + LS
            + "   gl_Position = u_ModelViewProjection * vec4(a_Position.xyz, 1.0);" + LS
            + "   gl_PointSize = u_PointSize;" + LS
            + "}";

    private static final String POINTCLOUD_FRAGMENT =
        "precision mediump float;" + LS
            + "varying vec4 v_Color;" + LS
            + "void main() {" + LS
            + "    gl_FragColor = v_Color;" + LS
            + "}";

    private WorldShaderUtil() {
    }

    /**
     * Shader label program generator.
     *
     * @return int Program handle.
     */
    public static int getLabelProgram() {
        return ShaderUtil.createGlProgram(ShaderUtil.LABEL_VERTEX, ShaderUtil.LABEL_FRAGMENT);
    }

    /**
     * Shader point cloud program generator.
     *
     * @return int Program handle.
     */
    public static int getPointCloudProgram() {
        return ShaderUtil.createGlProgram(POINTCLOUD_VERTEX, POINTCLOUD_FRAGMENT);
    }

    /**
     * Shader object program generator.
     *
     * @return int Program handle.
     */
    protected static int getObjectProgram() {
        return ShaderUtil.createGlProgram(OBJECT_VERTEX, OBJECT_FRAGMENT);
    }
}
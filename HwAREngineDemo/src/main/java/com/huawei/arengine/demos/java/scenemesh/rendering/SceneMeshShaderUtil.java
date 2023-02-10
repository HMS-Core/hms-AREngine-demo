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

import com.huawei.arengine.demos.common.ShaderUtil;

/**
 * This class provides code and program of the shader related to the ambient environment.
 *
 * @author hw
 * @since 2021-01-25
 */
class SceneMeshShaderUtil {
    private static final String TAG = SceneMeshShaderUtil.class.getSimpleName();

    private static final String LS = System.lineSeparator();

    /**
     * Vertex shader used for label rendering.
     */
    private static final String SCENE_MESH_VERTEX =
        "uniform mat4 u_ModelViewProjection;" + LS
            + "attribute vec4 a_Position;" + LS
            + "varying vec4 v_Position;" + LS
            + "void main() {" + LS
            + "    v_Position = a_Position;" + LS
            + "    gl_Position = u_ModelViewProjection * vec4(a_Position.xyz, 1.0);" + LS
            + "}";

    private static final String SCENE_MESH_FRAGMENT =
        "precision mediump float;" + LS
            + "uniform sampler2D u_Texture;" + LS
            + "varying vec4 v_Position;" + LS
            + "void main() {" + LS
            + "    vec4 control = texture2D(u_Texture, v_Position.xz);" + LS
            + "    gl_FragColor = vec4(control.rgb, 0.6);" + LS
            + "}";

    private static final String VIRTUAL_OBJECT_FRAGMENT =
        "precision mediump float;" + LS
            + "uniform sampler2D u_Texture;" + LS
            + "uniform vec4 u_LightingParameters;" + LS
            + "uniform vec4 u_MaterialParameters;" + LS
            + "varying vec3 v_ViewPosition;" + LS
            + "varying vec3 v_ViewNormal;" + LS
            + "varying vec2 v_TexCoord;" + LS
            + "uniform vec4 u_ObjColor;" + LS
            + "void main() {" + LS
            + "    const float kGamma = 0.4545454;" + LS
            + "    const float kInverseGamma = 2.2;" + LS
            + "    vec3 viewLightDirection = u_LightingParameters.xyz;" + LS
            + "    float lightIntensity = u_LightingParameters.w;" + LS
            + "    float materialAmbient = u_MaterialParameters.x;" + LS
            + "    float materialDiffuse = u_MaterialParameters.y;" + LS
            + "    float materialSpecular = u_MaterialParameters.z;" + LS
            + "    float materialSpecularPower = u_MaterialParameters.w;" + LS
            + "    vec3 viewFragmentDirection = normalize(v_ViewPosition);" + LS
            + "    vec3 viewNormal = normalize(v_ViewNormal);" + LS
            + "    vec4 objectColor = texture2D(u_Texture, vec2(v_TexCoord.x, 1.0 - v_TexCoord.y));" + LS
            + "    if (u_ObjColor.a >= 255.0) {" + LS
            + "      float intensity = objectColor.r;" + LS
            + "      objectColor.rgb = u_ObjColor.rgb * intensity / 255.0;" + LS
            + "    }" + LS
            + "    objectColor.rgb = pow(objectColor.rgb, vec3(kInverseGamma));" + LS
            + "    float ambient = materialAmbient;" + LS
            + "    float diffuse = lightIntensity * materialDiffuse *" + LS
            + "            0.5 * (dot(viewNormal, viewLightDirection) + 1.0);" + LS
            + "    vec3 reflectedLightDirection = reflect(viewLightDirection, viewNormal);" + LS
            + "    float specularStrength = max(0.0, dot(viewFragmentDirection, reflectedLightDirection));" + LS
            + "    float specular = lightIntensity * materialSpecular *" + LS
            + "            pow(specularStrength, materialSpecularPower);" + LS
            + "    gl_FragColor.a = objectColor.a;" + LS
            + "    gl_FragColor.rgb = pow(objectColor.rgb * (ambient + diffuse) + specular, vec3(kGamma));" + LS
            + "}";

    private static final String VIRTUAL_OBJECT_VERTEX =
        "uniform mat4 u_ModelView;" + LS
            + "uniform mat4 u_ModelViewProjection;" + LS
            + "attribute vec4 a_Position;" + LS
            + "attribute vec3 a_Normal;" + LS
            + "attribute vec2 a_TexCoord;" + LS
            + "varying vec3 v_ViewPosition;" + LS
            + "varying vec3 v_ViewNormal;" + LS
            + "varying vec2 v_TexCoord;" + LS
            + "void main() {" + LS
            + "    v_ViewPosition = (u_ModelView * a_Position).xyz;" + LS
            + "    v_ViewNormal = (u_ModelView * vec4(a_Normal, 0.0)).xyz;" + LS
            + "    v_TexCoord = a_TexCoord;" + LS
            + "    gl_Position = u_ModelViewProjection * a_Position;" + LS
            + "}";

    private SceneMeshShaderUtil() {
    }

    static int getMeshDisplayProgram() {
        return ShaderUtil.createGlProgram(SCENE_MESH_VERTEX, SCENE_MESH_FRAGMENT);
    }

    static int getVirtualObjectProgram() {
        return ShaderUtil.createGlProgram(VIRTUAL_OBJECT_VERTEX, VIRTUAL_OBJECT_FRAGMENT);
    }
}
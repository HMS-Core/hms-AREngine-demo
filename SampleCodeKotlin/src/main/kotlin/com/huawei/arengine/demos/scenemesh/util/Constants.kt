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

package com.huawei.arengine.demos.scenemesh.util

import java.lang.Float.SIZE

/**
 * constant class object.
 *
 * @author HW
 * @since 2021-04-21
 */
object Constants {
    /**
     * Vertex shader used for label rendering.
     */
    const val SCENE_MESH_VERTEX = ("uniform mat4 u_ModelViewProjection;"
        + "attribute vec4 a_Position;"
        + "varying vec4 v_Position;"
        + "void main() {"
        + "    v_Position = a_Position;"
        + "    gl_Position = u_ModelViewProjection * vec4(a_Position.xyz, 1.0);"
        + "}")

    const val SCENE_MESH_FRAGMENT = ("precision mediump float;"
        + "uniform sampler2D u_Texture;"
        + "varying vec4 v_Position;"
        + "void main() {"
        + "    vec4 control = texture2D(u_Texture, v_Position.xz);"
        + "    gl_FragColor = vec4(control.rgb, 0.6);"
        + "}")

    const val VIRTUAL_OBJECT_FRAGMENT = ("precision mediump float;"
        + "uniform sampler2D u_Texture;"
        + "uniform vec4 u_LightingParameters;"
        + "uniform vec4 u_MaterialParameters;"
        + "varying vec3 v_ViewPosition;"
        + "varying vec3 v_ViewNormal;"
        + "varying vec2 v_TexCoord;"
        + "uniform vec4 u_ObjColor;"
        + "void main() {"
        + "    const float kGamma = 0.4545454;"
        + "    const float kInverseGamma = 2.2;"
        + "    vec3 viewLightDirection = u_LightingParameters.xyz;"
        + "    float lightIntensity = u_LightingParameters.w;"
        + "    float materialAmbient = u_MaterialParameters.x;"
        + "    float materialDiffuse = u_MaterialParameters.y;"
        + "    float materialSpecular = u_MaterialParameters.z;"
        + "    float materialSpecularPower = u_MaterialParameters.w;"
        + "    vec3 viewFragmentDirection = normalize(v_ViewPosition);"
        + "    vec3 viewNormal = normalize(v_ViewNormal);"
        + "    vec4 objectColor = texture2D(u_Texture, vec2(v_TexCoord.x, 1.0 - v_TexCoord.y));"
        + "    if (u_ObjColor.a >= 255.0) {"
        + "      float intensity = objectColor.r;"
        + "      objectColor.rgb = u_ObjColor.rgb * intensity / 255.0;"
        + "    }"
        + "    objectColor.rgb = pow(objectColor.rgb, vec3(kInverseGamma));"
        + "    float ambient = materialAmbient;"
        + "    float diffuse = lightIntensity * materialDiffuse *"
        + "            0.5 * (dot(viewNormal, viewLightDirection) + 1.0);"
        + "    vec3 reflectedLightDirection = reflect(viewLightDirection, viewNormal);"
        + "    float specularStrength = max(0.0, dot(viewFragmentDirection, reflectedLightDirection));"
        + "    float specular = lightIntensity * materialSpecular *"
        + "            pow(specularStrength, materialSpecularPower);"
        + "    gl_FragColor.a = objectColor.a;"
        + "    gl_FragColor.rgb = pow(objectColor.rgb * (ambient + diffuse) + specular, vec3(kGamma));"
        + "}")

    const val VIRTUAL_OBJECT_VERTEX = ("uniform mat4 u_ModelView;"
        + "uniform mat4 u_ModelViewProjection;"
        + "attribute vec4 a_Position;"
        + "attribute vec3 a_Normal;"
        + "attribute vec2 a_TexCoord;"
        + "varying vec3 v_ViewPosition;"
        + "varying vec3 v_ViewNormal;"
        + "varying vec2 v_TexCoord;"
        + "void main() {"
        + "    v_ViewPosition = (u_ModelView * a_Position).xyz;"
        + "    v_ViewNormal = (u_ModelView * vec4(a_Normal, 0.0)).xyz;"
        + "    v_TexCoord = a_TexCoord;"
        + "    gl_Position = u_ModelViewProjection * a_Position;"
        + "}")

    /**
     * OBJ color flag. AR_TRACKABLE_POINT is blue.
     */
    const val AR_TRACK_POINT_COLOR = "track_point_color"

    /**
     * OBJ color flag. AR_TRACKABLE_PLANE is green.
     */
    const val AR_TRACK_PLANE_COLOR = "track_plane_color"

    /**
     * OBJ color flag. The default color is white.
     */
    const val AR_DEFAULT_COLOR = "default_color"

    const val BYTES_PER_FLOAT = SIZE / 8

    /**
     * X, Y, Z, and confidence.
     */
    const val FLOATS_PER_POINT = 3

    const val BYTES_PER_POINT = BYTES_PER_FLOAT * FLOATS_PER_POINT

    const val BUFFER_OBJECT_NUMBER = 2

    const val INT_PER_TRIANGE = 3

    const val MODLE_VIEW_PROJ_SIZE = 16

    const val POSITION_COMPONENTS_NUMBER = 4

    const val COORDS_PER_VERTEX = 3

    const val LIGHT_DIRECTION_SIZE = 4

    val LIGHT_DIRECTIONS = floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f)

    val TRACK_POINT_COLOR = floatArrayOf(66.0f, 133.0f, 244.0f, 255.0f)

    val TRACK_PLANE_COLOR = floatArrayOf(139.0f, 195.0f, 74.0f, 255.0f)

    val DEFAULT_COLORS = floatArrayOf(0f, 0f, 0f, 0f)

    const val OBJECT_AMBIENT = 0.5f

    const val OBJECT_DIFFUSE = 1.0f

    const val OBJECT_SPECULAR = 1.0f

    const val OBJECT_SPECULARPOWER = 4.0f

    const val FLOAT_BYTE_SIZE = 4

    const val A_NORMAL_SIZE = 3

    const val A_TEXCOORD_SIZE = 2

    const val OBJASSETNAME = "AR_logo.obj"

    const val DIFFUSETEXTUREASSETNAME = "AR_logo.png"

    const val MATERIAL_AMBIENT = 0.0f

    const val MATERIAL_DIFFUSE = 3.5f

    const val MATERIAL_SPECULAR = 1.0f

    const val MATERIAL_SPECULAI_POWER = 6.0f

    const val BLOCK_QUEUE_CAPACITY = 2

    const val FPS_TEXT_SIZE = 10f

    const val PROJ_MATRIX_OFFSET = 0

    const val PROJ_MATRIX_NEAR = 0.1f

    const val PROJ_MATRIX_FAR = 100.0f

    const val CONFIG_CHOOSER_RED_SIZE = 8

    const val CONFIG_CHOOSER_GREEN_SIZE = 8

    const val CONFIG_CHOOSER_BLUE_SIZE = 8

    const val CONFIG_CHOOSER_ALPHA_SIZE = 8

    const val CONFIG_CHOOSER_DEPTH_SIZE = 16

    const val CONFIG_CHOOSER_STENCIL_SIZE = 0

    const val OPENGLES_VERSION = 2
}
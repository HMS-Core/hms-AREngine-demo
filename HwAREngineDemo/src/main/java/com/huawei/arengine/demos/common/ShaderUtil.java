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

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.huawei.arengine.demos.java.utils.CommonUtil;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class is used to read shader code and compile links.
 *
 * @author HW
 * @since 2020-04-05
 */
public class ShaderUtil {
    /**
     * Newline character.
     */
    public static final String LS = System.lineSeparator();

    /**
     * Vertex shader used for label rendering.
     */
    public static final String LABEL_VERTEX =
        "uniform mat2 inPlanUVMatrix;" + LS
            + "uniform mat4 inMVPMatrix;" + LS
            + "attribute vec3 inPosXZAlpha;" + LS
            + "varying vec3 varTexCoordAlpha;" + LS
            + "void main() {" + LS
            + "    vec4 tempPosition = vec4(inPosXZAlpha.x, 0.0, inPosXZAlpha.y, 1.0);" + LS
            + "    vec2 tempUV = inPlanUVMatrix * inPosXZAlpha.xy;" + LS
            + "    varTexCoordAlpha = vec3(tempUV.x + 0.5, tempUV.y + 0.5, inPosXZAlpha.z);" + LS
            + "    gl_Position = inMVPMatrix * tempPosition;" + LS
            + "}";

    /**
     * Segment shader used for label rendering.
     */
    public static final String LABEL_FRAGMENT =
        "precision highp float;" + LS
            + "uniform sampler2D inTexture;" + LS
            + "varying vec3 varTexCoordAlpha;" + LS
            + "void main() {" + LS
            + "    vec4 control = texture2D(inTexture, varTexCoordAlpha.xy);" + LS
            + "    gl_FragColor = vec4(control.rgb, 1.0);" + LS
            + "}";

    /**
     * Code of the vertex shader.
     */
    private static final String LINE_VERTEX =
        "uniform vec4 inColor;" + LS
            + "attribute vec4 inPosition;" + LS
            + "uniform float inPointSize;" + LS
            + "varying vec4 varColor;" + LS
            + "uniform mat4 inMVPMatrix;" + LS
            + "void main() {" + LS
            + "    gl_PointSize = inPointSize;" + LS
            + "    gl_Position = inMVPMatrix * vec4(inPosition.xyz, 1.0);" + LS
            + "    varColor = inColor;" + LS
            + "}";

    /**
     * Code of the fragment shader.
     */
    private static final String LINE_FRAGMENT =
        "precision mediump float;" + LS
            + "varying vec4 varColor;" + LS
            + "void main() {" + LS
            + "    gl_FragColor = varColor;" + LS
            + "}";

    private static final float MATRIX_SCALE_SX = 1.0f;

    private static final float MATRIX_SCALE_SY = 1.0f;

    private ShaderUtil() {
    }

    /**
     * Check OpenGL ES running exceptions and throw them when necessary.
     *
     * @param tag Exception information.
     * @param label Program label.
     */
    public static void checkGlError(@NonNull String tag, @NonNull String label) {
        int lastError = GLES20.GL_NO_ERROR;
        int error = GLES20.glGetError();
        while (error != GLES20.GL_NO_ERROR) {
            Log.e(tag, label + ": glError " + error);
            lastError = error;
            error = GLES20.glGetError();
        }
        if (lastError != GLES20.GL_NO_ERROR) {
            throw new ArDemoRuntimeException(label + ": glError " + lastError);
        }
    }

    /**
     * Shader program generator.
     *
     * @param vertexName Code of the vertex shader.
     * @param fragmentName Code of the image fragment shader.
     * @return int Program handle.
     */
    public static int createGlProgram(String vertexName, String fragmentName) {
        int vertex = loadShader(GLES20.GL_VERTEX_SHADER, vertexName);
        if (vertex == 0) {
            return 0;
        }
        int fragment = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentName);
        if (fragment == 0) {
            return 0;
        }
        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertex);
            GLES20.glAttachShader(program, fragment);
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e("createGlProgram", "Could not link program:" + GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    private static int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e("createGlProgram", "Could not compile shader:" + shaderType);
                Log.e("createGlProgram", "GLES20 Error:" + GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    /**
     * Shader label program generator.
     *
     * @return int Program handle.
     */
    public static int getLabelProgram() {
        return createGlProgram(LABEL_VERTEX, LABEL_FRAGMENT);
    }

    /**
     * Shader program generator.
     *
     * @return int Program handle.
     */
    public static int getGlProgram() {
        return createGlProgram(LINE_VERTEX, LINE_FRAGMENT);
    }

    /**
     * Initialize the tag plane.
     *
     * @param tag Log printing tag.
     * @param textures Texture array.
     * @param labelTextView Display text control.
     * @return textures Texture data.
     */
    public static int[] initLabel(String tag, int[] textures, TextView labelTextView) {
        ShaderUtil.checkGlError(tag, "Update start.");
        GLES20.glGenTextures(textures.length, textures, 0);

        // Label plane.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        Bitmap labelBitmap = getImageBitmap(labelTextView);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, labelBitmap, 0);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        ShaderUtil.checkGlError(tag, "Update end.");
        return textures;
    }

    private static Bitmap getImageBitmap(TextView view) {
        AtomicReference<Bitmap> bitmap = new AtomicReference<>();
        Optional<Bitmap> op = CommonUtil.getBitmapFromView(view, MATRIX_SCALE_SX, MATRIX_SCALE_SY);
        op.ifPresent(object -> {
            bitmap.set(object);
        });
        return bitmap.get();
    }
}
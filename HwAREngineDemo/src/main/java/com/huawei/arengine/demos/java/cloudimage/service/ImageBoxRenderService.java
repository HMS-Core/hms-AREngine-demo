/**
 * Copyright 2021. Huawei Technologies Co., Ltd. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.arengine.demos.java.cloudimage.service;

import android.opengl.GLES20;
import android.opengl.Matrix;

import com.huawei.arengine.demos.common.ShaderUtil;
import com.huawei.arengine.demos.java.cloudimage.model.ImageBox;
import com.huawei.arengine.demos.java.cloudimage.model.ShaderPojo;
import com.huawei.arengine.demos.java.cloudimage.util.ImageBoxShaderUtil;
import com.huawei.hiar.ARAugmentedImage;

/**
 * Draw cloud image box based on the coordinates of the cloud image.
 *
 * @author HW
 * @since 2021-08-24
 */
public class ImageBoxRenderService {
    private static final String TAG = ImageBoxRenderService.class.getSimpleName();

    // Number of bytes occupied by each 3D coordinate. float data occupies 4 bytes.
    private static final int BYTES_PER_POINT = 4 * 4;

    private static final int INITIAL_POINTS_SIZE = 20;

    private ShaderPojo shaderPojo = new ShaderPojo();

    /**
     * Create and build the shader for the image box on the OpenGL thread.
     */
    public void init() {
        ShaderUtil.checkGlError(TAG, "Init start.");
        int[] buffers = new int[1];
        GLES20.glGenBuffers(1, buffers, 0);
        shaderPojo.setVbo(buffers[0]);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, shaderPojo.getVbo());
        shaderPojo.setVboSize(INITIAL_POINTS_SIZE * BYTES_PER_POINT);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, shaderPojo.getVboSize(), null, GLES20.GL_DYNAMIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        createProgram();
        ShaderUtil.checkGlError(TAG, "Init end.");
    }

    private void createProgram() {
        ShaderUtil.checkGlError(TAG, "Create program start.");
        shaderPojo.setProgram(ImageBoxShaderUtil.createGlProgram());
        int program = shaderPojo.getProgram();
        ShaderUtil.checkGlError(TAG, "program");
        shaderPojo.setPosition(GLES20.glGetAttribLocation(program, "inPosition"));
        shaderPojo.setColor(GLES20.glGetUniformLocation(program, "inColor"));
        shaderPojo.setPointSize(GLES20.glGetUniformLocation(program, "inPointSize"));
        shaderPojo.setMvpMatrix(GLES20.glGetUniformLocation(program, "inMVPMatrix"));
        ShaderUtil.checkGlError(TAG, "Create program end.");
    }

    /**
     * Draw image box to augmented image.
     *
     * @param augmentedImage Identified image to be augmented.
     * @param viewMatrix view matrix
     * @param projectionMatrix Projection matrix(4 * 4).
     */
    public void drawImageBox(ARAugmentedImage augmentedImage, float[] viewMatrix, float[] projectionMatrix) {
        float[] vpMatrix = new float[BYTES_PER_POINT];
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        ImageBox imageBox = new ImageBox(augmentedImage, shaderPojo);
        imageBox.draw(vpMatrix);
    }
}
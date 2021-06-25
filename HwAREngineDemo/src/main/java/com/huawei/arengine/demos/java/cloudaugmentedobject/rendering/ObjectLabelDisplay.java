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

package com.huawei.arengine.demos.java.cloudaugmentedobject.rendering;

import android.app.Activity;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.view.View;
import android.widget.TextView;

import com.huawei.arengine.demos.R;
import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.arengine.demos.common.ShaderUtil;
import com.huawei.hiar.ARObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Collection;

/**
 * 根据所识别的3D物体ARPose绘制label，label跟随3D物体运动.
 *
 * @author HW
 * @since 2021-04-02
 */
public class ObjectLabelDisplay implements ObjectRelatedDisplay {
    private static final String TAG = ObjectLabelDisplay.class.getSimpleName();

    private static final int IMAGE_ANGLE_MATRIX_SIZE = 4;

    private static final float MATRIX_SCALE_SX = 1.0f;

    private static final float MATRIX_SCALE_SY = 1.0f;

    private static final int COORDS_PER_VERTEX = 3;

    private static final float LABEL_WIDTH = 1.0f;

    private static final float LABEL_HEIGHT = 0.5f;

    private static final int TEXTURES_SIZE = 1;

    private static final int MATRIX_SIZE = 16;

    /**
     * 平面角度uv矩阵，调整label的旋转角度及纵向横向的缩放比例。
     */
    private final float[] imageAngleUvMatrix = new float[IMAGE_ANGLE_MATRIX_SIZE];

    private final float[] modelViewProjectionMatrix = new float[MATRIX_SIZE];

    private final float[] modelViewMatrix = new float[MATRIX_SIZE];

    /**
     * 分配一个临时矩阵，以减少每帧的分配次数。
     */
    private final float[] modelMatrix = new float[MATRIX_SIZE];

    private final int[] textures = new int[TEXTURES_SIZE];

    private int mProgram;

    private int glPositionParameter;

    private int glModelViewProjectionMatrix;

    private int glTexture;

    private int glPlaneUvMatrix;

    private final Activity mActivity;

    private TextView labelTextView;

    /**
     * 构造函数传递activity。
     *
     * @param activity Activity
     */
    public ObjectLabelDisplay(Activity activity) {
        mActivity = activity;
    }

    /**
     * 在OpenGL线程上创建并构建增强后的图像着色器。
     */
    @Override
    public void init() {
        labelTextView = mActivity.findViewById(R.id.image_ar_object);
        createProgram();
        initLabel();
    }

    private void initLabel() {
        ShaderUtil.checkGlError(TAG, "Update start.");
        GLES20.glGenTextures(textures.length, textures, 0);

        // label平面。
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        Bitmap labelBitmap = getImageBitmap(labelTextView);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, labelBitmap, 0);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        ShaderUtil.checkGlError(TAG, "Update end.");
    }

    private Bitmap getImageBitmap(TextView view) {
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, labelTextView.getMeasuredWidth(), labelTextView.getMeasuredHeight());
        view.setDrawingCacheEnabled(true);
        view.destroyDrawingCache();
        view.buildDrawingCache();
        Bitmap bitmap = view.getDrawingCache();
        LogUtil.debug(TAG, "object bitmap create start!");
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.setScale(MATRIX_SCALE_SX, MATRIX_SCALE_SY);
        if (bitmap != null) {
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
        LogUtil.debug(TAG, "object bitmap create end!");
        return bitmap;
    }

    private void createProgram() {
        ShaderUtil.checkGlError(TAG, "program start.");
        mProgram = ObjectShaderUtil.getLabelProgram();
        glTexture = GLES20.glGetUniformLocation(mProgram, "inTexture");
        glPositionParameter = GLES20.glGetAttribLocation(mProgram, "inPosXZAlpha");
        glPlaneUvMatrix = GLES20.glGetUniformLocation(mProgram, "inPlanUVMatrix");
        glModelViewProjectionMatrix = GLES20.glGetUniformLocation(mProgram, "inMVPMatrix");
        ShaderUtil.checkGlError(TAG, "program end.");
    }

    /**
     * 绘制图片label，来标识识别到的3D物体。
     * 此方法将在以下情况下调用 {@link ObjectRenderManager#onDrawFrame}.
     *
     * @param arObjects 3D物体。
     * @param viewMatrix 视图矩阵。
     * @param projectionMatrix ARCamera投影矩阵。
     */
    @Override
    public void onDrawFrame(Collection<ARObject> arObjects, float[] viewMatrix, float[] projectionMatrix) {
        prepareForGl();
        for (ARObject arObject: arObjects) {
            updateImageLabelData(arObject);
            drawLabel(viewMatrix, projectionMatrix);
        }
        recycleGl();
    }

    private void prepareForGl() {
        GLES20.glDepthMask(false);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFuncSeparate(GLES20.GL_DST_ALPHA, GLES20.GL_ONE, GLES20.GL_ZERO, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glUseProgram(mProgram);
        GLES20.glEnableVertexAttribArray(glPositionParameter);
    }

    /**
     * 更新3D物体的label信息。
     *
     * @param arObject arObject。
     */
    private void updateImageLabelData(ARObject arObject) {
        float[] imageMatrix = new float[MATRIX_SIZE];
        arObject.getCenterPose().toMatrix(imageMatrix, 0);
        System.arraycopy(imageMatrix, 0, modelMatrix, 0, MATRIX_SIZE);

        float scaleU = 1.0f / LABEL_WIDTH;

        // 设置平面角度uv矩阵的值。
        imageAngleUvMatrix[0] = scaleU;
        imageAngleUvMatrix[1] = 0.0f;
        imageAngleUvMatrix[2] = 0.0f;
        float scaleV = 1.0f / LABEL_HEIGHT;
        imageAngleUvMatrix[3] = scaleV;

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glUniform1i(glTexture, 0);
        GLES20.glUniformMatrix2fv(glPlaneUvMatrix, 1, false, imageAngleUvMatrix, 0);
    }

    /**
     * 绘制label。
     *
     * @param cameraViews 视图矩阵。
     * @param cameraProjection ARCamera投影矩阵。
     */
    private void drawLabel(float[] cameraViews, float[] cameraProjection) {
        ShaderUtil.checkGlError(TAG, "Draw object label start.");
        Matrix.multiplyMM(modelViewMatrix, 0, cameraViews, 0, modelMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraProjection, 0, modelViewMatrix, 0);

        // 获取宽、高的一半作坐标点数据用。
        float halfWidth = LABEL_WIDTH / 2.0f;
        float halfHeight = LABEL_HEIGHT / 2.0f;
        float[] vertices = {-halfWidth, -halfHeight, 1, -halfWidth, halfHeight, 1, halfWidth, halfHeight, 1, halfWidth,
            -halfHeight, 1};

        // 每个浮点数大小为4 byte。
        FloatBuffer vetBuffer =
            ByteBuffer.allocateDirect(4 * vertices.length).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vetBuffer.rewind();
        for (float vertex : vertices) {
            vetBuffer.put(vertex);
        }
        vetBuffer.rewind();
        GLES20.glVertexAttribPointer(glPositionParameter, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false,
            4 * COORDS_PER_VERTEX, vetBuffer);

        // 设置OpenGL绘制点的顺序，生成两个三角形，形成一个平面。
        short[] indices = {0, 1, 2, 0, 2, 3};

        // 分配的缓冲区大小，每个短整型数大小为2 byte。
        ShortBuffer idxBuffer =
            ByteBuffer.allocateDirect(2 * indices.length).order(ByteOrder.nativeOrder()).asShortBuffer();
        idxBuffer.rewind();
        for (short index : indices) {
            idxBuffer.put(index);
        }
        idxBuffer.rewind();

        GLES20.glUniformMatrix4fv(glModelViewProjectionMatrix, 1, false, modelViewProjectionMatrix, 0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, idxBuffer.limit(), GLES20.GL_UNSIGNED_SHORT, idxBuffer);
        ShaderUtil.checkGlError(TAG, "Draw object label end.");
    }

    private void recycleGl() {
        GLES20.glDisableVertexAttribArray(glPositionParameter);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glDepthMask(true);
    }
}
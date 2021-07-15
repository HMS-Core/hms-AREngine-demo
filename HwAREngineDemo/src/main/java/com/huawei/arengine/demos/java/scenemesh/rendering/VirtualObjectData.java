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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.arengine.demos.common.ShaderUtil;

import de.javagl.obj.Obj;
import de.javagl.obj.ObjData;
import de.javagl.obj.ObjReader;
import de.javagl.obj.ObjUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Optional;

/**
 * Displays objects that are loaded from the OBJ file in Open GL.
 *
 * @author HW
 * @since 2021-01-25
 */
public class VirtualObjectData {
    private static final String TAG = VirtualObjectData.class.getSimpleName();

    private static final int COORDS_PER_VERTEX = 3;

    private static final int LIGHT_DIRECTION_SIZE = 4;

    private static final float[] LIGHT_DIRECTIONS = new float[] {0.0f, 1.0f, 0.0f, 0.0f};

    private static final float[] TRACK_POINT_COLOR = new float[] {66.0f, 133.0f, 244.0f, 255.0f};

    private static final float[] TRACK_PLANE_COLOR = new float[] {139.0f, 195.0f, 74.0f, 255.0f};

    private static final float[] DEFAULT_COLORS = new float[] {0f, 0f, 0f, 0f};

    private static final float OBJECT_AMBIENT = 0.5f;

    private static final float OBJECT_DIFFUSE = 1.0f;

    private static final float OBJECT_SPECULAR = 1.0f;

    private static final float OBJECT_SPECULARPOWER = 4.0f;

    private static final int BUFFER_OBJECT_NUMBER = 2;

    private static final int FLOAT_BYTE_SIZE = 4;

    private static final int A_NORMAL_SIZE = 3;

    private static final int A_TEXCOORD_SIZE = 2;

    private static final String OBJASSETNAME = "AR_logo.obj";

    private static final String DIFFUSETEXTUREASSETNAME = "AR_logo.png";

    private float[] mViewLightDirections = new float[LIGHT_DIRECTION_SIZE];

    private int mVertexBufferId;

    private int mVerticesBaseAddress;

    private int mTexCoordsBaseAddress;

    private int mNormalsBaseAddress;

    private int mIndexBufferId;

    private int mIndexCount;

    private int mProgram;

    private int[] mTextures = new int[1];

    private int mModelViewUniform;

    private int mModelViewProjectionUniform;

    private int mPositionAttribute;

    private int mNormalAttribute;

    private int mTexCoordAttribute;

    private int mTextureUniform;

    private int mLightingParametersUniform;

    private int mMaterialParametersUniform;

    /**
     * Shader position: object color attribute (primary color of the object).
     */
    private int mColorUniform;

    private float[] mModelMatrixs = new float[16];

    private float[] mModelViewMatrixs = new float[16];

    private float[] mModelViewProjectionMatrixs = new float[16];

    private float mAmbient = OBJECT_AMBIENT;

    private float mDiffuse = OBJECT_DIFFUSE;

    private float mSpecular = OBJECT_SPECULAR;

    private float mSpecularPower = OBJECT_SPECULARPOWER;

    /**
     * Constructor of the virtual object data class.
     */
    public VirtualObjectData() {
    }

    /**
     * Virtual object data class.
     *
     * @author HW
     * @since 2021-02-8
     */
    private static class ObjectData {
        IntBuffer objectIndices;
        FloatBuffer objectVertices;
        ShortBuffer indices;
        FloatBuffer texCoords;
        FloatBuffer normals;

        ObjectData(IntBuffer objectIndices,
            FloatBuffer objectVertices,
            ShortBuffer indices,
            FloatBuffer texCoords,
            FloatBuffer normals) {
            this.objectIndices = objectIndices;
            this.objectVertices = objectVertices;
            this.indices = indices;
            this.texCoords = texCoords;
            this.normals = normals;
        }
    }

    /**
     * Initialize the cache and compile the link coloring program On GlThread.
     *
     * @param context Load the shader and the context of the following model and texture assets.
     */
    public void init(Context context) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glGenTextures(mTextures.length, mTextures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        initGlImageData(context);

        ObjectData objectData = null;
        Optional<ObjectData> objectDataOptional = readObject(context);
        if (objectDataOptional.isPresent()) {
            objectData = objectDataOptional.get();
        } else {
            LogUtil.error(TAG, "Read object error.");
            return;
        }

        mTexCoordsBaseAddress = mVerticesBaseAddress + FLOAT_BYTE_SIZE * objectData.objectIndices.limit();
        mNormalsBaseAddress = mTexCoordsBaseAddress + FLOAT_BYTE_SIZE * objectData.texCoords.limit();
        final int totalBytes = mNormalsBaseAddress + FLOAT_BYTE_SIZE * objectData.normals.limit();
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferId);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, totalBytes, null, GLES20.GL_STATIC_DRAW);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, mVerticesBaseAddress,
            FLOAT_BYTE_SIZE * objectData.objectVertices.limit(), objectData.objectVertices);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, mTexCoordsBaseAddress,
            FLOAT_BYTE_SIZE * objectData.texCoords.limit(), objectData.texCoords);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, mNormalsBaseAddress,
            FLOAT_BYTE_SIZE * objectData.normals.limit(), objectData.normals);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndexBufferId);
        mIndexCount = objectData.indices.limit();

        // Prevent the memory from being insufficient and multiply the memory.
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, 2 * mIndexCount, objectData.indices, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

        loadShaderAndGetOpenGLESVariable();
    }

    private Optional<ObjectData> readObject(Context context) {
        Obj obj;
        try (InputStream objInputStream = context.getAssets().open(OBJASSETNAME)) {
            obj = ObjReader.read(objInputStream);
            obj = ObjUtils.convertToRenderable(obj);
        } catch (IllegalArgumentException | IOException e) {
            LogUtil.error(TAG, "Get data failed!");
            return Optional.empty();
        }

        // Each surface of the object has three vertices.
        IntBuffer objectIndices = ObjData.getFaceVertexIndices(obj, 3);

        // Prevent the memory from being insufficient and multiply the memory.
        ShortBuffer indices = ByteBuffer.allocateDirect(2 * objectIndices.limit())
            .order(ByteOrder.nativeOrder())
            .asShortBuffer();
        while (objectIndices.hasRemaining()) {
            indices.put((short) objectIndices.get());
        }
        indices.rewind();
        int[] buffers = new int[BUFFER_OBJECT_NUMBER];
        GLES20.glGenBuffers(BUFFER_OBJECT_NUMBER, buffers, 0);
        mVertexBufferId = buffers[0];
        mIndexBufferId = buffers[1];
        mVerticesBaseAddress = 0;
        FloatBuffer objectVertices = ObjData.getVertices(obj);
        FloatBuffer texCoords = ObjData.getTexCoords(obj, 2); // Set the coordinate dimension to 2.
        FloatBuffer normals = ObjData.getNormals(obj);
        return Optional.of(
            new ObjectData(objectIndices, objectVertices, indices, texCoords, normals));
    }

    private void initGlImageData(Context context) {
        ShaderUtil.checkGlError(TAG, "Init gl texture data start.");
        Bitmap textureBitmap;
        try (InputStream inputStream = context.getAssets().open(DIFFUSETEXTUREASSETNAME)) {
            textureBitmap = BitmapFactory.decodeStream(inputStream);
        } catch (IllegalArgumentException | IOException e) {
            LogUtil.error(TAG, "Get data error!");
            return;
        }
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, textureBitmap, 0);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        textureBitmap.recycle();
        ShaderUtil.checkGlError(TAG, "Init gl texture data end.");
    }

    /**
     * Load the shader and obtain the ES variables of the OpenGL.
     */
    private void loadShaderAndGetOpenGLESVariable() {
        mProgram = SceneMeshShaderUtil.getVirtualObjectProgram();
        GLES20.glUseProgram(mProgram);

        mModelViewUniform = GLES20.glGetUniformLocation(mProgram, "u_ModelView");
        mModelViewProjectionUniform = GLES20.glGetUniformLocation(mProgram, "u_ModelViewProjection");

        mPositionAttribute = GLES20.glGetAttribLocation(mProgram, "a_Position");
        mNormalAttribute = GLES20.glGetAttribLocation(mProgram, "a_Normal");
        mTexCoordAttribute = GLES20.glGetAttribLocation(mProgram, "a_TexCoord");

        mTextureUniform = GLES20.glGetUniformLocation(mProgram, "u_Texture");

        mLightingParametersUniform = GLES20.glGetUniformLocation(mProgram, "u_LightingParameters");
        mMaterialParametersUniform = GLES20.glGetUniformLocation(mProgram, "u_MaterialParameters");
        mColorUniform = GLES20.glGetUniformLocation(mProgram, "u_ObjColor");

        Matrix.setIdentityM(mModelMatrixs, 0);
    }

    /**
     * Update the model matrix data.
     *
     * @param modelMatrix Model matrix data.
     * @param scaleFactor Scaling factor.
     */
    public void updateModelMatrix(float[] modelMatrix, float scaleFactor) {
        float[] scaleMatrixs = new float[16];
        Matrix.setIdentityM(scaleMatrixs, 0);

        // Set the first column of the matrix on the right to the scaling factor.
        // Scaling factor of the diagonal line of the matrix.
        scaleMatrixs[0] = scaleFactor;
        scaleMatrixs[5] = scaleFactor;
        scaleMatrixs[10] = scaleFactor;
        Matrix.multiplyMM(mModelMatrixs, 0, modelMatrix, 0, scaleMatrixs, 0);
        Matrix.rotateM(mModelMatrixs, 0, 315.0f, 0f, 1f, 0f); // Rotation 315 degrees.
    }

    /**
     * Set the material attributes.
     *
     * @param ambient Material property: environment parameter.
     * @param diffuse Material property: diffusion parameter.
     * @param specular Material property: specular parameter.
     * @param specularPower Material property: specular power parameter.
     */
    public void setMaterialProperties(float ambient, float diffuse, float specular, float specularPower) {
        mAmbient = ambient;
        mDiffuse = diffuse;
        mSpecular = specular;
        mSpecularPower = specularPower;
    }

    /**
     * Draw a virtual object at a specific location on a specified plane.
     *
     * @param cameraView Camera view data.
     * @param cameraPerspective Perspective data of the camera.
     * @param lightIntensity Light intensity data.
     * @param objColor Object color.
     */
    public void draw(float[] cameraView, float[] cameraPerspective, float lightIntensity, String objColor) {
        LogUtil.debug(TAG, "Before draw Virtual Object : ");

        Matrix.multiplyMM(mModelViewMatrixs, 0, cameraView, 0, mModelMatrixs, 0);
        Matrix.multiplyMM(mModelViewProjectionMatrixs, 0, cameraPerspective, 0, mModelViewMatrixs, 0);

        GLES20.glUseProgram(mProgram);

        Matrix.multiplyMV(mViewLightDirections, 0, mModelViewMatrixs, 0, LIGHT_DIRECTIONS, 0);
        normalizeVec3(mViewLightDirections);

        // The lighting direction data has three dimensions (0, 1, and 2).
        GLES20.glUniform4f(mLightingParametersUniform, mViewLightDirections[0], mViewLightDirections[1],
            mViewLightDirections[2], lightIntensity);

        // Set the object color.
        switch (objColor) {
            case ColoredArAnchor.AR_TRACK_POINT_COLOR:
                GLES20.glUniform4fv(mColorUniform, 1, TRACK_POINT_COLOR, 0);
                break;
            case ColoredArAnchor.AR_TRACK_PLANE_COLOR:
                GLES20.glUniform4fv(mColorUniform, 1, TRACK_PLANE_COLOR, 0);
                break;
            case ColoredArAnchor.AR_DEFAULT_COLOR:
                GLES20.glUniform4fv(mColorUniform, 1, DEFAULT_COLORS, 0);
                break;
            default:
                LogUtil.error(TAG, "draw, obj color error");
                break;
        }

        GLES20.glUniform4f(mMaterialParametersUniform, mAmbient, mDiffuse, mSpecular, mSpecularPower);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);
        GLES20.glUniform1i(mTextureUniform, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferId);

        GLES20.glVertexAttribPointer(mPositionAttribute, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0,
            mVerticesBaseAddress);
        GLES20.glVertexAttribPointer(mNormalAttribute, A_NORMAL_SIZE, GLES20.GL_FLOAT, false, 0, mNormalsBaseAddress);
        GLES20.glVertexAttribPointer(mTexCoordAttribute, A_TEXCOORD_SIZE, GLES20.GL_FLOAT, false, 0,
            mTexCoordsBaseAddress);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        GLES20.glUniformMatrix4fv(mModelViewUniform, 1, false, mModelViewMatrixs, 0);
        GLES20.glUniformMatrix4fv(mModelViewProjectionUniform, 1, false, mModelViewProjectionMatrixs, 0);
        GLES20.glEnableVertexAttribArray(mPositionAttribute);
        GLES20.glEnableVertexAttribArray(mNormalAttribute);
        GLES20.glEnableVertexAttribArray(mTexCoordAttribute);

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndexBufferId);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mIndexCount, GLES20.GL_UNSIGNED_SHORT, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

        GLES20.glDisableVertexAttribArray(mPositionAttribute);
        GLES20.glDisableVertexAttribArray(mNormalAttribute);
        GLES20.glDisableVertexAttribArray(mTexCoordAttribute);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        LogUtil.debug(TAG, "After draw Virtual Object : ");
    }

    /**
     * Three-dimensional data standardization method, which divides each number by the root of the
     * sum of squares of all numbers.
     *
     * @param vector: 3D vector.
     */
    public static void normalizeVec3(float[] vector) {
        // The data has three dimensions (0, 1, and 2).
        float length = 1 / (float) Math.sqrt(vector[0] * vector[0] + vector[1] * vector[1] + vector[2] * vector[2]);
        vector[0] *= length;
        vector[1] *= length;
        vector[2] *= length;
    }
}

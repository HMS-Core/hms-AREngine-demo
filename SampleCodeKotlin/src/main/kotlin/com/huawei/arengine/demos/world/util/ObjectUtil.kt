/**
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.huawei.arengine.demos.world.util

import android.util.Log
import com.huawei.arengine.demos.MainApplication
import com.huawei.arengine.demos.world.pojo.ObjectPojo
import de.javagl.obj.Obj
import de.javagl.obj.ObjData
import de.javagl.obj.ObjReader
import de.javagl.obj.ObjUtils
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Object Read Util Class.
 *
 * @author HW
 * @since 2020-11-09
 */
object ObjectUtil {
    private const val TAG = "ObjectUtil"

    /**
     * The largest bounding box of a virtual object, represented by two diagonals of a cube.
     */
    val boundingBox = FloatArray(6)

    fun readObject(): ObjectPojo? {
        var obj: Obj? = null
        try {
            MainApplication.context.assets.open("AR_logo.obj").use { objInputStream ->
                obj = ObjReader.read(objInputStream)
                obj = ObjUtils.convertToRenderable(obj)
            }
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Get data failed!")
            return null
        } catch (e: IOException) {
            Log.e(TAG, "Get data failed!")
            return null
        }

        // Every surface of an object has three vertices.
        val objectIndices = ObjData.getFaceVertexIndices(obj, 3)
        val objectVertices = ObjData.getVertices(obj)
        calculateBoundingBox(objectVertices)

        // Size of the allocated buffer.
        val indices = ByteBuffer.allocateDirect(2 * objectIndices.limit())
            .order(ByteOrder.nativeOrder()).asShortBuffer()
        while (objectIndices.hasRemaining()) {
            indices.put(objectIndices.get().toShort())
        }
        indices.rewind()

        // The dimension of the texture coordinate is 2.
        val texCoordinates = ObjData.getTexCoords(obj, 2)
        val normals = ObjData.getNormals(obj)
        return ObjectPojo(objectIndices, objectVertices, indices, texCoordinates, normals)
    }

    private fun calculateBoundingBox(vertices: FloatBuffer) {
        // Bounding box [minX, minY, minZ, maxX, maxY, maxZ].
        if (vertices.limit() < 3) {
            boundingBox.fill(0.0f, 0, boundingBox.size)
            return
        }
        for (i in 0..2) {
            boundingBox[i] = vertices[i]
            boundingBox[i + 3] = vertices[i]
        }

        /**
         * Use the first three pairs as the initial variables and
         * get the three maximum values and three minimum values.
         */
        for (index in 3..(vertices.limit() - 2) step 3) {
            for (i in 0..2) {
                if (vertices[index] < boundingBox[i]) {
                    boundingBox[i] = vertices[index]
                }
                if (vertices[index] > boundingBox[i + 3]) {
                    boundingBox[i + 3] = vertices[index]
                }
            }
        }
    }
}
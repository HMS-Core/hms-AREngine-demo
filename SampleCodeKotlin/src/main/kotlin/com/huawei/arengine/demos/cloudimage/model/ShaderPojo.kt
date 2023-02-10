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

package com.huawei.arengine.demos.cloudimage.model

/**
 * Shader parameters used to draw OpenGL elements.
 *
 * @author HW
 * @since 2022-03-14
 */
class ShaderPojo {
    private var vbo = 0

    private var vboSize = 0

    private var program = 0

    private var position = 0

    private var mvpMatrix = 0

    private var color = 0

    private var pointSize = 0

    private var numPoints = 0

    fun getVbo(): Int {
        return vbo
    }

    fun setVbo(vbo: Int) {
        this.vbo = vbo
    }

    fun getVboSize(): Int {
        return vboSize
    }

    fun setVboSize(vboSize: Int) {
        this.vboSize = vboSize
    }

    fun getProgram(): Int {
        return program
    }

    fun setProgram(program: Int) {
        this.program = program
    }

    fun getPosition(): Int {
        return position
    }

    fun setPosition(position: Int) {
        this.position = position
    }

    fun getMvpMatrix(): Int {
        return mvpMatrix
    }

    fun setMvpMatrix(mvpMatrix: Int) {
        this.mvpMatrix = mvpMatrix
    }

    fun getColor(): Int {
        return color
    }

    fun setColor(color: Int) {
        this.color = color
    }

    fun getPointSize(): Int {
        return pointSize
    }

    fun setPointSize(pointSize: Int) {
        this.pointSize = pointSize
    }

    fun getNumPoints(): Int {
        return numPoints
    }

    fun setNumPoints(numPoints: Int) {
        this.numPoints = numPoints
    }
}
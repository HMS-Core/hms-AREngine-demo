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

package com.huawei.arengine.demos.java.cloudimage.model;

/**
 * Shader parameters used to draw OpenGL elements.
 *
 * @author HW
 * @since 2021-08-24
 */
public class ShaderPojo {
    private int vbo;

    private int vboSize;

    private int program;

    private int position;

    private int mvpMatrix;

    private int color;

    private int pointSize;

    private int numPoints;

    public int getVbo() {
        return vbo;
    }

    public void setVbo(int vbo) {
        this.vbo = vbo;
    }

    public int getVboSize() {
        return vboSize;
    }

    public void setVboSize(int vboSize) {
        this.vboSize = vboSize;
    }

    public int getProgram() {
        return program;
    }

    public void setProgram(int program) {
        this.program = program;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getMvpMatrix() {
        return mvpMatrix;
    }

    public void setMvpMatrix(int mvpMatrix) {
        this.mvpMatrix = mvpMatrix;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getPointSize() {
        return pointSize;
    }

    public void setPointSize(int pointSize) {
        this.pointSize = pointSize;
    }

    public int getNumPoints() {
        return numPoints;
    }

    public void setNumPoints(int numPoints) {
        this.numPoints = numPoints;
    }
}

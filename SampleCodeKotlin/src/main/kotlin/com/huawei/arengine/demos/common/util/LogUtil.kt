/**
 * Copyright 2021. Huawei Technologies Co., Ltd. All rights reserved.
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
package com.huawei.arengine.demos.common

import android.util.Log

/**
 * Log utility class, which provides a more convenient method of printing logs.
 *
 * @author HW
 * @since 2021-03-18
 */
object LogUtil {
    private const val PROJECT_NAME = "ArEngine_Kt"

    /**
     * Method name.
     */
    private var methodName: String? = null

    /**
     * Line number.
     */
    private var lineNumber = 0

    /**
     * Create log information.
     *
     * @param tag Tag.
     * @param logMsg Log messages.
     * @return Packaged log messages.
     */
    private fun createLog(tag: String, logMsg: String): String {
        val buffer = StringBuffer()
        buffer.append("[").append(tag)
        buffer.append(": ").append(methodName)
        buffer.append(": ").append(lineNumber).append("] ")
        buffer.append(logMsg)
        return buffer.toString()
    }

    /**
     * Obtain the method name and line number in the printed log.
     *
     * @param stackElements Stack elements.
     */
    private fun getMethodNames(stackElements: Array<StackTraceElement>) {
        methodName = stackElements[1].methodName
        lineNumber = stackElements[1].lineNumber
    }

    /**
     * Print verbose logs.
     *
     * @param tag Tag.
     * @param message Log messages.
     */
    fun verbose(tag: String, message: String) {
        getMethodNames(Throwable().stackTrace)
        Log.v(PROJECT_NAME, createLog(tag, message))
    }

    /**
     * Print debug logs.
     *
     * @param tag Tag.
     * @param message Log messages.
     */
    fun debug(tag: String, message: String) {
        getMethodNames(Throwable().stackTrace)
        Log.d(PROJECT_NAME, createLog(tag, message))
    }

    /**
     * Print info logs.
     *
     * @param tag Tag.
     * @param message Log messages.
     */
    fun info(tag: String, message: String) {
        getMethodNames(Throwable().stackTrace)
        Log.i(PROJECT_NAME, createLog(tag, message))
    }

    /**
     * Print warn logs.
     *
     * @param tag Tag.
     * @param message Log messages.
     */
    fun warn(tag: String, message: String) {
        getMethodNames(Throwable().stackTrace)
        Log.w(PROJECT_NAME, createLog(tag, message))
    }

    /**
     * Print error logs.
     *
     * @param tag Tag.
     * @param message Log messages.
     */
    fun error(tag: String, message: String) {
        getMethodNames(Throwable().stackTrace)
        Log.e(PROJECT_NAME, createLog(tag, message))
    }
}
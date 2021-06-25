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
 * log的工具类，提供更便捷的打印日志信息的方式。
 *
 * @author HW
 * @since 2021-03-18
 */
object LogUtil {
    private const val PROJECT_NAME = "ArEngine_Kt"

    /**
     * 所在的方法名。
     */
    private var methodName: String? = null

    /**
     * 所在行号。
     */
    private var lineNumber = 0

    /**
     * 创建日志信息。
     *
     * @param tag TAG标记。
     * @param logMsg 日志信息。
     * @return 包装后的日志信息。
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
     * 获取打印日志部分的方法名、行号。
     *
     * @param stackElements 堆栈信息。
     */
    private fun getMethodNames(stackElements: Array<StackTraceElement>) {
        methodName = stackElements[1].methodName
        lineNumber = stackElements[1].lineNumber
    }

    /**
     * 打印verbose级别的日志。
     *
     * @param tag TAG标记。
     * @param message 日志信息。
     */
    fun verbose(tag: String, message: String) {
        getMethodNames(Throwable().stackTrace)
        Log.v(PROJECT_NAME, createLog(tag, message))
    }

    /**
     * 打印debug级别的日志。
     *
     * @param tag TAG标记。
     * @param message 日志信息。
     */
    fun debug(tag: String, message: String) {
        getMethodNames(Throwable().stackTrace)
        Log.d(PROJECT_NAME, createLog(tag, message))
    }

    /**
     * 打印info级别的日志。
     *
     * @param tag TAG标记。
     * @param message 日志信息。
     */
    fun info(tag: String, message: String) {
        getMethodNames(Throwable().stackTrace)
        Log.i(PROJECT_NAME, createLog(tag, message))
    }

    /**
     * 打印warn级别的日志。
     *
     * @param tag TAG标记。
     * @param message 日志信息。
     */
    fun warn(tag: String, message: String) {
        getMethodNames(Throwable().stackTrace)
        Log.w(PROJECT_NAME, createLog(tag, message))
    }

    /**
     * 打印error级别的日志。
     *
     * @param tag TAG标记。
     * @param message 日志信息。
     */
    fun error(tag: String, message: String) {
        getMethodNames(Throwable().stackTrace)
        Log.e(PROJECT_NAME, createLog(tag, message))
    }
}
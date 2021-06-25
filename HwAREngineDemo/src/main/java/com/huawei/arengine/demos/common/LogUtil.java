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

package com.huawei.arengine.demos.common;

import android.util.Log;

/**
 * log的工具类，提供更便捷的打印日志信息的方式。
 *
 * @author HW
 * @since 2021-03-08
 */
public class LogUtil {
    private static final String PROJECT_NAME = "ArEngine_demo";

    /**
     * 所在的方法名。
     */
    private static String methodName;

    /**
     * 所在行号。
     */
    private static int lineNumber;

    /**
     * 创建日志信息。
     *
     * @param tag TAG标记。
     * @param logMsg 日志信息。
     * @return 包装后的日志信息。
     */
    private static String createLog(String tag, String logMsg) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("[").append(tag);
        buffer.append(": ").append(methodName);
        buffer.append(": ").append(lineNumber).append("] ");
        buffer.append(logMsg);
        return buffer.toString();
    }

    /**
     * 获取打印日志部分的方法名、行号。
     *
     * @param stackElements 堆栈信息。
     */
    private static void getMethodNames(StackTraceElement[] stackElements) {
        methodName = stackElements[1].getMethodName();
        lineNumber = stackElements[1].getLineNumber();
    }

    /**
     * 打印verbose级别的日志。
     *
     * @param tag TAG标记。
     * @param message 日志信息。
     */
    public static void verbose(String tag, String message) {
        getMethodNames(new Throwable().getStackTrace());
        Log.v(PROJECT_NAME, createLog(tag, message));
    }

    /**
     * 打印debug级别的日志。
     *
     * @param tag TAG标记。
     * @param message 日志信息。
     */
    public static void debug(String tag, String message) {
        getMethodNames(new Throwable().getStackTrace());
        Log.d(PROJECT_NAME, createLog(tag, message));
    }

    /**
     * 打印info级别的日志。
     *
     * @param tag TAG标记。
     * @param message 日志信息。
     */
    public static void info(String tag, String message) {
        getMethodNames(new Throwable().getStackTrace());
        Log.i(PROJECT_NAME, createLog(tag, message));
    }

    /**
     * 打印warn级别的日志。
     *
     * @param tag TAG标记。
     * @param message 日志信息。
     */
    public static void warn(String tag, String message) {
        getMethodNames(new Throwable().getStackTrace());
        Log.w(PROJECT_NAME, createLog(tag, message));
    }

    /**
     * 打印error级别的日志。
     *
     * @param tag TAG标记。
     * @param message 日志信息。
     */
    public static void error(String tag, String message) {
        getMethodNames(new Throwable().getStackTrace());
        Log.e(PROJECT_NAME, createLog(tag, message));
    }
}

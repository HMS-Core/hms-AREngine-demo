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

import android.util.Log;

/**
 * Log utility class, which provides a more convenient method of printing logs.
 *
 * @author HW
 * @since 2021-03-08
 */
public class LogUtil {
    private static final String PROJECT_NAME = "ArEngine_demo";

    /**
     * Method name.
     */
    private static String methodName;

    /**
     * Line number.
     */
    private static int lineNumber;

    /**
     * Create log information.
     *
     * @param tag Tag.
     * @param logMsg Log messages.
     * @return Packaged log messages.
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
     * Obtain the method name and line number in the printed log.
     *
     * @param stackElements Stack elements.
     */
    private static void getMethodNames(StackTraceElement[] stackElements) {
        methodName = stackElements[1].getMethodName();
        lineNumber = stackElements[1].getLineNumber();
    }

    /**
     * Print verbose logs.
     *
     * @param tag Tag.
     * @param message Log messages.
     */
    public static void verbose(String tag, String message) {
        getMethodNames(new Throwable().getStackTrace());
        Log.v(PROJECT_NAME, createLog(tag, message));
    }

    /**
     * Print debug logs.
     *
     * @param tag Tag.
     * @param message Log messages.
     */
    public static void debug(String tag, String message) {
        getMethodNames(new Throwable().getStackTrace());
        Log.d(PROJECT_NAME, createLog(tag, message));
    }

    /**
     * Print info logs.
     *
     * @param tag Tag.
     * @param message Log messages.
     */
    public static void info(String tag, String message) {
        getMethodNames(new Throwable().getStackTrace());
        Log.i(PROJECT_NAME, createLog(tag, message));
    }

    /**
     * Print warn logs.
     *
     * @param tag Tag.
     * @param message Log messages.
     */
    public static void warn(String tag, String message) {
        getMethodNames(new Throwable().getStackTrace());
        Log.w(PROJECT_NAME, createLog(tag, message));
    }

    /**
     * Print error logs.
     *
     * @param tag Tag.
     * @param message Log messages.
     */
    public static void error(String tag, String message) {
        getMethodNames(new Throwable().getStackTrace());
        Log.e(PROJECT_NAME, createLog(tag, message));
    }
}

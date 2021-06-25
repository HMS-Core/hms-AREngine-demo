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

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Json file read util
 *
 * @author HW
 * @since 2021-04-20
 */
public class JsonUtil {
    private static final String TAG = JsonUtil.class.getSimpleName();

    private static final int EXPECTED_BUFFER_DATA = 1024;

    /**
     * Read json file.
     *
     * @param fileName file name.
     * @param context context.
     * @return json file content.
     */
    public static String getJson(String fileName, Context context) {
        // change json file to string
        StringBuilder stringBuilder = new StringBuilder(EXPECTED_BUFFER_DATA);
        InputStreamReader inputStreamReader = null;
        BufferedReader reader = null;
        try {
            AssetManager assetManager = context.getAssets();

            // open file and read file input stream
            inputStreamReader = new InputStreamReader(assetManager.open(fileName), "UTF-8");
            reader = new BufferedReader(inputStreamReader);
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (IOException e) {
            Log.w(TAG, "open json file error");
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.w(TAG, "close BufferedReader error");
                }
            }
            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException e) {
                    Log.w(TAG, "close inputStream error");
                }
            }
        }
        return stringBuilder.toString();
    }
}

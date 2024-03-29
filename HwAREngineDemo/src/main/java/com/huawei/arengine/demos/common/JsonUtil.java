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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;

import com.huawei.arengine.demos.java.cloudaugmentedobject.ModeInformation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Json file read util
 *
 * @author HW
 * @since 2021-04-20
 */
public class JsonUtil {
    private static final String TAG = JsonUtil.class.getSimpleName();

    private static final int EXPECTED_BUFFER_DATA = 1024;

    private static final int LIST_MAX_SIZE = 1024;

    private static final String DEFAULT_FILE = "default_id";

    private static final String DEFAULT_KEY = "mode_id";

    private JsonUtil() {
    }

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
                if (line.contains("/") || line.contains("*")) {
                    continue;
                }
                stringBuilder.append(line);
            }
            LogUtil.debug(TAG, "stringBuilder = " + stringBuilder.toString());
        } catch (IOException e) {
            LogUtil.warn(TAG, "open json file error");
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LogUtil.warn(TAG, "close BufferedReader error");
                }
            }
            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException e) {
                    LogUtil.warn(TAG, "close inputStream error");
                }
            }
        }
        return stringBuilder.toString();
    }

    /**
     * Read continents, modelName, appId, and appSecret in a specified JSON file.
     *
     * @param jsonFile Name of the JSON file that stores mode name data.
     * @return Return the modeID array, which contains continents, modelName, appId, and appSecret.
     */
    public static List<ModeInformation> json2List(String jsonFile) {
        List<ModeInformation> modeIdList = new ArrayList<>(LIST_MAX_SIZE);
        if (jsonFile == null) {
            LogUtil.error(TAG, "jsonFile is null");
            return modeIdList;
        }
        try {
            JSONObject jsonObject = new JSONObject(jsonFile);
            JSONArray jsonArray = jsonObject.getJSONArray("data");
            if (jsonArray == null) {
                LogUtil.error(TAG, "jsonFile is null");
                return modeIdList;
            }
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject object = jsonArray.getJSONObject(i);
                if (object == null) {
                    continue;
                }
                ModeInformation id =
                    new ModeInformation(object.getString("modeInformation"), object.getString("continents"));
                if (id.getContinents().isEmpty() || id.getModeInformation().isEmpty()) {
                    continue;
                }
                modeIdList.add(id);
            }
        } catch (JSONException e) {
            LogUtil.error(TAG, "json object read error" + e.getClass());
        }
        return modeIdList;
    }

    /**
     * Read the default authentication data.
     *
     * @param context context.
     * @return Return the default authentication data.
     */
    public static String readApplicationMessage(Context context) {
        String json = "";
        if (context == null) {
            LogUtil.error(TAG, "context is null");
            return json;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(DEFAULT_FILE, Context.MODE_PRIVATE);
        return sharedPreferences.getString(DEFAULT_KEY, "");
    }

    /**
     * Store the default authentication data, including modelName, appId, and appSecret.
     *
     * @param context context.
     * @param json Default authentication data, including modelName, appId, and appSecret.
     */
    public static void writeApplicationMessage(Context context, String json) {
        if (context == null || json == null) {
            LogUtil.error(TAG, "context is null or json is null");
            return;
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(DEFAULT_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(DEFAULT_KEY, json);
        editor.commit();
    }
}

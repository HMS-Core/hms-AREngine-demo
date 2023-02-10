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

package com.huawei.arengine.demos.common.util

import android.content.Context

import com.huawei.arengine.demos.cloudaugmentobject.model.ModeInformation
import com.huawei.arengine.demos.common.LogUtil

import org.json.JSONException
import org.json.JSONObject

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * Json file read util
 *
 * @author HW
 * @since 2022-04-20
 */
object JsonUtil {
    private val TAG = "JsonUtil"

    private const val EXPECTED_BUFFER_DATA = 1024

    private const val LIST_MAX_SIZE = 1024

    private const val DEFAULT_FILE = "default_id"

    private const val DEFAULT_KEY = "mode_id"

    /**
     * Read json file.
     *
     * @param fileName file name.
     * @param context context.
     * @return json file content.
     */
    fun getJson(fileName: String?, context: Context): String {
        // change json file to string
        val stringBuilder = StringBuilder(EXPECTED_BUFFER_DATA)
        var inputStreamReader: InputStreamReader? = null
        var reader: BufferedReader? = null
        var lines: MutableList<String>? = null
        try {
            val assetManager = context.assets

            // open file and read file input stream
            inputStreamReader = InputStreamReader(assetManager.open(fileName!!), "UTF-8")
            reader = BufferedReader(inputStreamReader)
            lines= reader.readLines().toMutableList()
            for (string in lines) {
                if (string == null) {
                    break
                }
                if (string.contains("/") || string.contains("*")) {
                    continue
                }
                stringBuilder.append(string)
            }
        } catch (e: IOException) {
            LogUtil.warn(TAG, "open json file error")
        } finally {
            if (reader != null) {
                try {
                    reader.close()
                } catch (e: IOException) {
                    LogUtil.warn(TAG, "close BufferedReader error")
                }
            }
            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close()
                } catch (e: IOException) {
                    LogUtil.warn(TAG, "close inputStream error")
                }
            }
        }
        return stringBuilder.toString()
    }

    /**
     * Read the default authentication data.
     *
     * @param context context.
     * @return Return the default authentication data.
     */
    fun readApplicationMessage(context: Context?): String {
        val json = ""
        if (context == null) {
            LogUtil.error(TAG, "context is null")
            return json
        }
        val sharedPreferences = context.getSharedPreferences(DEFAULT_FILE, Context.MODE_PRIVATE)
        return sharedPreferences.getString(DEFAULT_KEY, "") ?: ""
    }

    /**
     * Store the default authentication data, including modelName, appId, and appSecret.
     *
     * @param context context.
     * @param json Default authentication data, including modelName, appId, and appSecret.
     */
    fun writeApplicationMessage(context: Context?, json: String?) {
        if (context == null || json == null) {
            LogUtil.error(TAG, "context is null or json is null")
            return
        }
        val sharedPreferences = context.getSharedPreferences(DEFAULT_FILE, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(DEFAULT_KEY, json)
        editor.commit()
    }

    fun json2List(jsonFile: String?): List<ModeInformation> {
        val modeIdList = ArrayList<ModeInformation>(LIST_MAX_SIZE)
        if (jsonFile == null) {
            LogUtil.error(TAG, "jsonFile is null")
            return modeIdList
        }
        try {
            val jsonObject = JSONObject(jsonFile)
            val jsonArray = jsonObject.getJSONArray("data")
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i) ?: continue
                val id = ModeInformation(jsonObject.getString("modeInformation"), jsonObject.getString("continents"))
                if (id.getContinent().isEmpty() || id.getModelInformation().isEmpty()) {
                    continue
                }
                modeIdList.add(id)
            }
        } catch (e: JSONException) {
            LogUtil.error(TAG, "json object read error" + e.javaClass)
        }
        return modeIdList
    }

    fun defaultAppId(context: Context, fileName: String): String {
        var jsonString = getJson(fileName, context)
        var authJson = ""
        var modeList = json2List(jsonString)
        if (modeList.size < 0) {
            LogUtil.error(TAG, "sign error, get application message error")
            return authJson
        }
        authJson = modeList.get(0).getModelInformation()
        return authJson
    }
}
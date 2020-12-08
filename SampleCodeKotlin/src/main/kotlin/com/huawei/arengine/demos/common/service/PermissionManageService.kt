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
package com.huawei.arengine.demos.common.service

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.huawei.arengine.demos.MainApplication

/**
 * Permission manager, which provides methods for determining and applying for camera permissions.
 *
 * @author HW
 * @since 2020-10-10
 */
object PermissionManageService {
    private const val REQUEST_CODE_ASK_PERMISSIONS = 1

    private val PERMISSIONS_ARRAYS = arrayOf(Manifest.permission.CAMERA)

    /**
     * List of permissions to be applied for.
     */
    private val permissionsList by lazy { mutableListOf<String>() }

    /**
     * Check whether the current app has the necessary permissions (by default, the camera permission is required).
     * If not, apply for the permission. This method should be called in the onResume method of the main activity.
     *
     * @param activity Activity
     */
    fun checkPermission(activity: Activity) {
        PERMISSIONS_ARRAYS.forEach {
            if (ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED) {
                permissionsList.add(it)
            }
        }
        if (permissionsList.isEmpty()) {
            return
        }
        ActivityCompat.requestPermissions(activity,
            permissionsList.toTypedArray(), REQUEST_CODE_ASK_PERMISSIONS)
    }

    /**
     * Check whether the current app has the required permissions.
     *
     * @return Has permission or not.
     */
    fun hasPermission(): Boolean {
        for (permission in PERMISSIONS_ARRAYS) {
            if (ContextCompat.checkSelfPermission(MainApplication.context, permission)
                != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }
}
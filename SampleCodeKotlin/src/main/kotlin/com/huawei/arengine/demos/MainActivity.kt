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

package com.huawei.arengine.demos

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast

import com.huawei.arengine.demos.R.id.btn_augmented_image
import com.huawei.arengine.demos.R.id.btn_body_ar_3d
import com.huawei.arengine.demos.R.id.btn_cloud_2DImage
import com.huawei.arengine.demos.R.id.btn_cloud_3DObject
import com.huawei.arengine.demos.R.id.btn_face_ar
import com.huawei.arengine.demos.R.id.btn_hand_ar
import com.huawei.arengine.demos.R.id.btn_health_ar
import com.huawei.arengine.demos.R.id.btn_scence_mesh
import com.huawei.arengine.demos.R.id.btn_world_ar
import com.huawei.arengine.demos.R.id.btn_worldbody_ar
import com.huawei.arengine.demos.augmentedimage.AugmentedImageActivity
import com.huawei.arengine.demos.scenemesh.SceneMeshActivity
import com.huawei.arengine.demos.body3d.BodyActivity
import com.huawei.arengine.demos.cloudaugmentobject.CloudAugmentObjectActivity
import com.huawei.arengine.demos.cloudimage.CloudAugmentedImageActivity
import com.huawei.arengine.demos.common.LogUtil
import com.huawei.arengine.demos.common.service.PermissionManageService
import com.huawei.arengine.demos.common.util.startActivityByType
import com.huawei.arengine.demos.face.FaceActivity
import com.huawei.arengine.demos.hand.HandActivity
import com.huawei.arengine.demos.health.HealthActivity
import com.huawei.arengine.demos.world.WorldActivity
import com.huawei.arengine.demos.worldbody.WorldBodyActivity

/**
 * This class provides the permission verification and sub-AR example redirection functions.
 *
 * @author HW
 * @since 2020-10-10
 */
class MainActivity : Activity() {
    companion object {
        const val TAG = "ChooseActivity"
    }

    private var isFirstClick = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_choose)
        // AR Engine requires the camera permission.
        PermissionManageService.checkPermission(this)
    }

    override fun onResume() {
        super.onResume()
        isFirstClick = true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, results: IntArray) {
        if (!PermissionManageService.hasPermission()) {
            Toast.makeText(MainApplication.context, "This application needs camera permission.",
                Toast.LENGTH_LONG).show()
            finish()
        }
    }

    fun onClick(view: View) {
        if (!isFirstClick) return
        isFirstClick = false
        when (view.id) {
            btn_world_ar -> startActivityByType<WorldActivity>()
            btn_face_ar -> startActivityByType<FaceActivity>()
            btn_body_ar_3d -> startActivityByType<BodyActivity>()
            btn_hand_ar -> startActivityByType<HandActivity>()
            btn_augmented_image -> startActivityByType<AugmentedImageActivity>()
            btn_worldbody_ar -> startActivityByType<WorldBodyActivity>()
            btn_scence_mesh -> startActivityByType<SceneMeshActivity>()
            btn_health_ar -> startActivityByType<HealthActivity>()
            btn_cloud_2DImage -> startActivityByType<CloudAugmentedImageActivity>()
            btn_cloud_3DObject -> startActivityByType<CloudAugmentObjectActivity>()
            else -> LogUtil.error(TAG, "onClick error!")
        }
    }
}
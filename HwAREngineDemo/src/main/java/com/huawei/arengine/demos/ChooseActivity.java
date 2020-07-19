/**
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
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

package com.huawei.arengine.demos;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.huawei.arengine.demos.common.PermissionManager;

/**
 * This class provides the permission verification and sub-AR example redirection functions.
 *
 * @author HW
 * @since 2020-03-31
 */
public class ChooseActivity extends Activity {
    private static final String TAG = ChooseActivity.class.getSimpleName();

    private boolean isFirstClick = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_choose);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        isFirstClick = true;

        // AR Engine requires the camera permission.
        PermissionManager.onResume(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!PermissionManager.hasPermission(this)) {
            Toast.makeText(this, "This application needs camera permission.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy start.");
        super.onDestroy();
        Log.i(TAG, "onDestroy end.");
    }

    /**
     * Choose activity.
     *
     * @param view View
     */
    public void onClick(View view) {
        if (!isFirstClick) {
            return;
        } else {
            isFirstClick = false;
        }
        switch (view.getId()) {
            case R.id.btn_WorldAR_Java:
                startActivity(new Intent(this,
                    com.huawei.arengine.demos.java.world.WorldActivity.class));
                break;
            case R.id.btn_FaceAR:
                startActivity(new Intent(this,
                    com.huawei.arengine.demos.java.face.FaceActivity.class));
                break;
            case R.id.btn_body3d:
                startActivity(new Intent(this,
                    com.huawei.arengine.demos.java.body3d.BodyActivity.class));
                break;
            case R.id.btn_hand:
                startActivity(new Intent(this,
                    com.huawei.arengine.demos.java.hand.HandActivity.class));
                break;
            default:
                Log.e(TAG, "onClick error!");
        }
    }
}
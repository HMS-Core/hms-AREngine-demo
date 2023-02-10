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

package com.huawei.arengine.demos.java.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.huawei.arengine.demos.common.LogUtil;
import com.huawei.hiar.ARFrame;
import com.huawei.hiar.ARHitResult;
import com.huawei.hiar.ARPose;
import com.huawei.hiar.common.CloudServiceState;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Public utility class.
 *
 * @author HW
 * @since 2021-09-04
 */
public class CommonUtil {
    private static final String TAG = CommonUtil.class.getSimpleName();

    /**
     * Privatization construction method.
     */
    private CommonUtil() {
    }

    /**
     * Create a bitmap based on the RGB array.
     *
     * @param rgbData Byte array in RGB_888 format.
     * @param width Number of horizontal pixels.
     * @param height Number of vertical pixels.
     * @return Cube mapping bitmap.
     */
    public static Optional<Bitmap> createBitmapImage(byte[] rgbData, int width, int height) {
        // The data passed from the AREngineServer is in the RGB_888 format.
        // The bitmap can be output only after the data is converted into the ARGB_8888 format.
        int[] colors = convertRgbToArgb(rgbData);
        if (colors.length == 0) {
            LogUtil.warn(TAG, "colors length is 0.");
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888));
        } catch (IllegalArgumentException e) {
            LogUtil.error(TAG, "Exception on the createBitmap.");
            return Optional.empty();
        }
    }

    /**
     * Load images to bitmaps.
     *
     * @param context Context.
     * @param imagePath Image path.
     * @return Bitmap.
     */
    public static Optional<Bitmap> loadAugmentedImageBitmap(Context context, String imagePath) {
        if (context == null || imagePath == null) {
            return Optional.empty();
        }
        try (InputStream is = context.getAssets().open(imagePath)) {
            return Optional.of(BitmapFactory.decodeStream(is));
        } catch (IOException e) {
            LogUtil.error(TAG, "IO exception loading augmented image bitmap.");
        }
        return Optional.empty();
    }

    /**
     * Converts the pixel color in RGB_888 format to ARGB_8888 format.
     *
     * @param rgbArr Byte array of the pixel point color in RGB_888 format.
     * @return Integer array of the pixel point color in ARGB_8888 format.
     */
    private static int[] convertRgbToArgb(byte[] rgbArr) {
        if (rgbArr == null) {
            LogUtil.warn(TAG, "rgbData is null.");
            return new int[0];
        }

        // Add an 8-bit transparency channel to convert RGB_888 pixels into ARGB_8888 pixels.
        // In the RGB_888 format, one pixel is stored every 3 bytes.
        // The storage sequence is R-G-B (high on the left and low on the right).
        // In the ARGB_888 format, one pixel is stored for each int.
        // The storage sequence is A-B-G-R (high on the left and low on the right).
        // The 8-bit transparency data of each pixel is stored in bits 24 to 31 of argbArr.
        // The 8-bit B data of each pixel is stored in bits 16 to 23 of argbArr.
        // The 8-bit G data of each pixel is stored in bits 8–15 of argbArr.
        // The 8-bit R data of each pixel is stored in bits 0-7 of argbArr.
        int[] argbArr = new int[rgbArr.length / 3];
        for (int i = 0; i < argbArr.length; ++i) {
            argbArr[i] = 0xFF000000 | (rgbArr[i * 3] << 16 & 0x00FF0000) | (rgbArr[i * 3 + 1] << 8 & 0x0000FF00)
                | (rgbArr[i * 3 + 2] & 0x000000FF);
        }
        return argbArr;
    }

    /**
     * Calculate the distance between a point in a space and a plane. This method is used
     * to calculate the distance between a camera in a space and a specified plane.
     *
     * @param planePose ARPose of a plane.
     * @param cameraPose ARPose of a camera.
     * @return Calculation results.
     */
    public static float calculateDistanceToPlane(ARPose planePose, ARPose cameraPose) {
        if (planePose == null || cameraPose == null) {
            LogUtil.error(TAG, "calculate failed, planePose or cameraPose was null");
            return 0.0f;
        }
        // The dimension of the direction vector is 3.
        float[] normals = new float[3];

        // Obtain the unit coordinate vector of a normal vector of a plane.
        planePose.getTransformedAxis(1, 1.0f, normals, 0);

        // Calculate the distance based on projection,0:x,1:y,2:z.
        return (cameraPose.tx() - planePose.tx()) * normals[0]
            + (cameraPose.ty() - planePose.ty()) * normals[1]
            + (cameraPose.tz() - planePose.tz()) * normals[2];
    }

    /**
     * Return the recognition result form the 2D and 3D cloud recognition server.
     *
     * @param state 2D and 3D cloud recognition status codes.
     * @return Cloud recognition result returned by the server.
     */
    public static String cloudServiceErrorMessage(CloudServiceState state) {
        String message = "";
        if (state == null) {
            return message;
        }
        Log.d(TAG, "handleEvent: CloudImage :" + state);
        switch (state) {
            case CLOUD_SERVICE_ERROR_NETWORK_UNAVAILABLE:
                message = "network unavailable";
                break;
            case CLOUD_SERVICE_ERROR_CLOUD_SERVICE_UNAVAILABLE:
                message = "cloud service unavailable";
                break;
            case CLOUD_SERVICE_ERROR_NOT_AUTHORIZED:
                message = "cloud service not authorized";
                break;
            case CLOUD_SERVICE_ERROR_SERVER_VERSION_TOO_OLD:
                message = "cloud server version too old";
                break;
            case CLOUD_SERVICE_ERROR_TIME_EXHAUSTED:
                message = "time exhausted";
                break;
            case CLOUD_SERVICE_ERROR_INTERNAL:
                message = "cloud service gallery invalid";
                break;
            case CLOUD_IMAGE_ERROR_IMAGE_GALLERY_INVALID:
                message = "cloud image error, cloud service gallery invalid";
                break;
            case CLOUD_IMAGE_ERROR_IMAGE_RECOGNIZE_FAILE:
                message = "cloud image recognize fail";
                break;
            case CLOUD_OBJECT_ERROR_OBJECT_MODEL_INVALID:
                message = "cloud object error, object invalid";
                break;
            case CLOUD_OBJECT_ERROR_OBJECT_RECOGNIZE_FAILE:
                message = "cloud object recognize fail";
                break;
            default:
                break;
        }
        return message;
    }

    /**
     * Create a bitmap using the view.
     *
     * @param view View.
     * @param sX Scale of the X axis.
     * @param sY Scale of the Y axis.
     * @return Bitmap created using the view.
     */
    public static Optional<Bitmap> getBitmapFromView(View view, float sX, float sY) {
        if (view == null) {
            return Optional.empty();
        }
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(),
            Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        Matrix matrix = new Matrix();
        matrix.preScale(sX, sY);
        if (bitmap != null) {
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
        return Optional.of(bitmap);
    }

    /**
     * Get hit result of the position in the screen
     *
     * @param frame ARFrame
     * @param event input event of screen
     * @return List of ARHitResult
     */
    public static List<ARHitResult> hitTest(ARFrame frame, MotionEvent event) {
        if (event == null || event.getX() < 0 || event.getY() < 0) {
            LogUtil.error(TAG, "hitTest, event is null, or coordinate is below zero.");
            return Collections.emptyList();
        }
        return frame.hitTest(event);
    }
}

/**
 * Copyright 2021. Huawei Technologies Co., Ltd. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.arengine.demos.java.cloudaugmentedobject;

/**
 * 3D cloud recognition model class.
 *
 * @author HW
 * @since 2021-08-19
 */
public class ModeInformation {
    private String modeInformation;

    private String continents;

    /**
     * Create a ModeInformation object.
     *
     * @param information Model information, including modelName, appId, and appSecret.
     * @param continents Country/Region.
     */
    public ModeInformation(String information, String continents) {
        this.modeInformation = (information == null) ? "" : information;
        this.continents = (continents == null) ? "" : continents;
    }

    /**
     * Obtains the modelName.
     *
     * @return modelName.
     */
    public String getContinents() {
        return continents;
    }

    /**
     * Obtain the model information, including modelName, appId, and appSecret.
     *
     * @return Return the model information.
     */
    public String getModeInformation() {
        return modeInformation;
    }
}

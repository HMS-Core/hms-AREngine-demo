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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.huawei.arengine.demos.R;

import java.util.List;

/**
 * Control for displaying the list dialog.
 *
 * @author HW
 * @since 2022-09-01
 */
public class ListDialog {
    private Dialog dialog = null;

    private DialogOnItemClickListener dialogOnItemClickListener = null;

    /**
     * Display the list dialog.
     *
     * @param context instanceof Activity.
     * @param keyList Data displayed in the list.
     */
    public void showDialogList(Context context, List<String> keyList) {
        if (!(context instanceof Activity) || keyList == null || keyList.size() == 0) {
            return;
        }
        if (dialogOnItemClickListener == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_list, null);
        ListView dialogList = view.findViewById(R.id.select_dialog_listview);
        builder.setView(view).setCancelable(false);
        dialogList.setAdapter(new ArrayAdapter<>(context, R.layout.dialog_list_item, R.id.list_item, keyList));
        dialogList.setOnItemClickListener((parent, view1, position, id) -> {
            dialog.dismiss();
            dialogOnItemClickListener.onItemClick(position);
        });
        dialog = builder.show();
    }

    /**
     * Callback API for list tapping.
     *
     * @author HW
     * @since 2022-09-01
     */
    public interface DialogOnItemClickListener {
        /**
         * Called when an item in the list is selected.
         *
         * @param position Subscript of the selected item in the list.
         */
        void onItemClick(int position);
    }

    /**
     * Set the callback listener for selecting an item in the list.
     *
     * @param dialogOnItemClickListener DialogOnItemClickListener.
     */
    public void setDialogOnItemClickListener(DialogOnItemClickListener dialogOnItemClickListener) {
        if (dialogOnItemClickListener == null) {
            return;
        }
        this.dialogOnItemClickListener = dialogOnItemClickListener;
    }
}

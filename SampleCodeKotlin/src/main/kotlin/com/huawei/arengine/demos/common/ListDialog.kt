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

package com.huawei.arengine.demos.common

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.ListView

import com.huawei.arengine.demos.R

/**
 * Control for displaying the list dialog.
 *
 * @author HW
 * @since 2022-09-27
 */
class ListDialog {
    private var mDialog: Dialog? = null

    private var mDialogOnItemClickListener: DialogOnItemClickListener? = null

    /**
     * Display the list dialog.
     *
     * @param context instanceof Activity.
     * @param keyList Data displayed in the list.
     */
    fun showDialogList(mContext: Context, keyList: ArrayList<String>) {
        val view = LayoutInflater.from(mContext).inflate(R.layout.dialog_list, null);
        AlertDialog.Builder(mContext)?.apply {
            setView(view).setCancelable(true)
            mDialog = create()
        }
        var dialogList: ListView = view.findViewById(R.id.select_dialog_listview)
        dialogList?.apply {
            adapter = ArrayAdapter(context, R.layout.dialog_list_item, R.id.list_item, keyList)
            setOnItemClickListener { _, _, position, _ ->
                mDialog?.dismiss()
                mDialogOnItemClickListener?.onItemClick(position)
            }
        }
        mDialog?.show()
    }

    /**
     * Callback API for list tapping.
     *
     * @author HW
     * @since 2022-09-27
     */
    interface DialogOnItemClickListener {
        /**
         * Called when an item in the list is selected.
         *
         * @param position Subscript of the selected item in the list.
         */
        fun onItemClick(position: Int)
    }

    /**
     * Set the callback listener for selecting an item in the list.
     *
     * @param dialogOnItemClickListener DialogOnItemClickListener.
     */
    fun setDialogOnItemClickListener(dialogOnItemClickListener: DialogOnItemClickListener?) {
        if (dialogOnItemClickListener == null) {
            return
        }
        mDialogOnItemClickListener = dialogOnItemClickListener
    }
}
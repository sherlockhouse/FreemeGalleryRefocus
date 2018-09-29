/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.filtershow.presets;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import com.android.gallery3d.R;
import com.freeme.community.utils.ToastUtil;
import com.freeme.gallery.filtershow.FilterShowActivity;

public class PresetManagementDialog extends DialogFragment implements View.OnClickListener {
    private static final int MAX_INPUT_LENTH = 9;
    private UserPresetsAdapter mAdapter;
    private EditText mEditText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.filtershow_presets_management_dialog, container);

        FilterShowActivity activity = (FilterShowActivity) getActivity();
        /// M: [BUG.MARK] @{
        /* some times, getUserPresetsAdapter return null, so move it to onClick
           mAdapter = activity.getUserPresetsAdapter();*/
        /// @}
        mEditText = (EditText) view.findViewById(R.id.editView);
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().contains(" ")) {
                    String[] strOriginal = s.toString().split(" ");
                    String strResult = "";
                    for (int i = 0; i < strOriginal.length; i++) {
                        strResult += strOriginal[i];
                    }
                    mEditText.setText(strResult);
                    mEditText.setSelection(start);
                }
                if (s.length() > MAX_INPUT_LENTH){
                    mEditText.setText(s.toString().substring(0,MAX_INPUT_LENTH));
                    mEditText.setSelection(MAX_INPUT_LENTH);
                    ToastUtil.showToast(getActivity(),getActivity().getResources().getString(R.string.max_input_length));
                    return;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        view.findViewById(R.id.cancel).setOnClickListener(this);
        view.findViewById(R.id.ok).setOnClickListener(this);
        getDialog().setTitle(getString(R.string.filtershow_save_preset));
        return view;
    }

    @Override
    public void onClick(View v) {
        FilterShowActivity activity = (FilterShowActivity) getActivity();
        /// M: [BUG.ADD] @{
        mAdapter = activity.getUserPresetsAdapter();
        /// @}
        switch (v.getId()) {
            case R.id.cancel:
                mAdapter.clearChangedRepresentations();
                mAdapter.clearDeletedRepresentations();
                activity.updateUserPresetsFromAdapter(mAdapter);
                dismiss();
                break;
            case R.id.ok:
                String text = String.valueOf(mEditText.getText());
                activity.saveCurrentImagePreset(text);
                mAdapter.updateCurrent();
                activity.updateUserPresetsFromAdapter(mAdapter);
                dismiss();
                break;
        }
    }
}

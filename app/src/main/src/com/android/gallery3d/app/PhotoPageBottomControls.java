/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.gallery3d.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.freeme.gallery.R;
import com.freeme.utils.FreemeUtils;

import java.util.HashMap;
import java.util.Map;

public class PhotoPageBottomControls implements OnClickListener {
    public static final int       CONTAINER_ANIM_DURATION_MS = 200;
    private static final int CONTROL_ANIM_DURATION_MS = 150;
    private ViewGroup mPhotopageToolbar;
    private View mPhotoDetails;
    private RadioGroup mBottomRg;
    private RadioButton mRbEdit;
    private RadioButton mRbSetas;
    private RadioButton mRbTag;
    private RadioButton mRbFilm;

    public RadioButton getmRbEdit() {
        return mRbEdit;
    }

    public RadioButton getmRbSetas() {
        return mRbSetas;
    }

    public RadioButton getmRbFilm() {
        return mRbFilm;
    }

    public RadioButton getmRbTag() {
        return mRbTag;
    }


    public  SharedPreferences   mSharedPref;
    private boolean isEditable = true;

    public interface Delegate {
        public boolean canDisplayBottomControls();
        public boolean canDisplayBottomControl(int control, View view);
        public void onBottomControlClicked(int control);
        public void refreshBottomControlsWhenReady();
    }
    public  SharedPreferences.Editor mEditor;
    private Delegate  mDelegate;
    private ViewGroup mParentLayout;
    private ViewGroup mContainer;
    // */
    private ViewGroup mControls;

    private Context                  mContext;
    private boolean mContainerVisible = false;
    private Map<View, Boolean> mControlsVisible = new HashMap<View, Boolean>();

    private Animation mContainerAnimIn = new AlphaAnimation(0f, 1f);
    private Animation mContainerAnimOut = new AlphaAnimation(1f, 0f);
    //*/ Added by droi Linguanrong for freeme gallery, 16-1-30
    private View mNavigationBar;

    private static Animation getControlAnimForVisibility(boolean visible) {
        Animation anim = visible ? new AlphaAnimation(0f, 1f)
                : new AlphaAnimation(1f, 0f);
        anim.setDuration(CONTROL_ANIM_DURATION_MS);
        return anim;
    }
    public PhotoPageBottomControls(Delegate delegate, Context context, ViewGroup layout) {
        mDelegate = delegate;
        mParentLayout = layout;

        //*/ Added by Linguanrong for guide, 2015-08-10
        mContext = context;
        mSharedPref = context.getSharedPreferences(FreemeUtils.STORY_SHAREPREFERENCE_KEY, Context.MODE_PRIVATE);
        mEditor = mSharedPref.edit();
        //*/

        initViews();

        mContainerAnimIn.setDuration(CONTAINER_ANIM_DURATION_MS);
        mContainerAnimOut.setDuration(CONTAINER_ANIM_DURATION_MS);

        mDelegate.refreshBottomControlsWhenReady();
    }

    private void initViews() {
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContainer = (ViewGroup) inflater
                .inflate(R.layout.freeme_photopage_controls, mParentLayout, false);
        mParentLayout.addView(mContainer);
        mControls = (ViewGroup) mContainer.findViewById(R.id.freeme_photopage_controls);
        mPhotopageToolbar = mContainer.findViewById(R.id.photopage_toolbar);
        mNavigationBar = mContainer.findViewById(R.id.navigation_bar);
        for (int i = mContainer.getChildCount() - 1; i >= 0; i--) {
            View child = mContainer.getChildAt(i);
            child.setOnClickListener(this);
            mControlsVisible.put(child, false);
        }

        mPhotoDetails  = mContainer.findViewById(R.id.photopage_details);
        mControlsVisible.put(mPhotoDetails, false);
        mPhotoDetails.setOnClickListener(this);
        for (int i = mPhotopageToolbar.getChildCount() - 1; i >= 0; i--) {
            Log.d("bottomnav", mPhotopageToolbar.getChildCount() + "");
            View child = mPhotopageToolbar.getChildAt(i);
            child.setOnClickListener(this);
            mControlsVisible.put(child, false);
        }
        mBottomRg = (RadioGroup) mContainer.findViewById(R.id.rg_photopage_bottom_navigation_bar);
        for (int i = mBottomRg.getChildCount() - 1; i >= 0; i--) {
            Log.d("bottomnav", mBottomRg.getChildCount() + "");
            View child = mBottomRg.getChildAt(i);
            child.setOnClickListener(this);
            mControlsVisible.put(child, false);
        }
        initRadioButtons(mBottomRg);
    }

    public void initViewsConfigureChanged() {
        mParentLayout.removeView(mContainer);

        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContainer = (ViewGroup) inflater
                .inflate(R.layout.freeme_photopage_controls, mParentLayout, false);
        mParentLayout.addView(mContainer);
        mControls = (ViewGroup) mContainer.findViewById(R.id.freeme_photopage_controls);

        mPhotopageToolbar = mContainer.findViewById(R.id.photopage_toolbar);
        mNavigationBar = mContainer.findViewById(R.id.navigation_bar);
        for (int i = mContainer.getChildCount() - 1; i >= 0; i--) {
            View child = mContainer.getChildAt(i);
            child.setOnClickListener(this);
            mControlsVisible.put(child, false);
        }

        mPhotoDetails  = mContainer.findViewById(R.id.photopage_details);
        mControlsVisible.put(mPhotoDetails, false);
        mPhotoDetails.setOnClickListener(this);
        for (int i = mPhotopageToolbar.getChildCount() - 1; i >= 0; i--) {
            Log.d("bottomnav", mPhotopageToolbar.getChildCount() + "");
            View child = mPhotopageToolbar.getChildAt(i);
            child.setOnClickListener(this);
            mControlsVisible.put(child, false);
        }
        mBottomRg = (RadioGroup) mContainer.findViewById(R.id.rg_photopage_bottom_navigation_bar);
        for (int i = mBottomRg.getChildCount() - 1; i >= 0; i--) {
            Log.d("bottomnav", mBottomRg.getChildCount() + "");
            View child = mBottomRg.getChildAt(i);
            child.setOnClickListener(this);
            mControlsVisible.put(child, false);
        }
        initRadioButtons(mBottomRg);
        setIsEditable(isEditable, true);
    }

    private void initRadioButtons(RadioGroup mBottomRg) {
        mRbEdit = mBottomRg.findViewById(R.id.photopage_bottom_control_edit);
        mRbSetas = mBottomRg.findViewById(R.id.photopage_bottom_control_setas);
        mRbFilm = mBottomRg.findViewById(R.id.photopage_bottom_control_blockbuster);
        mRbTag = mBottomRg.findViewById(R.id.photopage_bottom_control_tag);
    }

    private void hide() {

        mContainer.clearAnimation();
        mContainerAnimOut.reset();
        mContainer.startAnimation(mContainerAnimOut);
        mContainer.setVisibility(View.INVISIBLE);
    }

    private void show() {
        mContainer.clearAnimation();
        mContainerAnimIn.reset();
        mContainer.startAnimation(mContainerAnimIn);
        mContainer.setVisibility(View.VISIBLE);
    }

    public void refresh() {
        boolean visible = mDelegate.canDisplayBottomControls();
        boolean containerVisibilityChanged = (visible != mContainerVisible);
        if (containerVisibilityChanged) {
            if (visible) {
                show();
            } else {
                hide();
            }
            mContainerVisible = visible;
        }
        if (!mContainerVisible) {
            return;
        }
        for (View control : mControlsVisible.keySet()) {
            Boolean prevVisibility = mControlsVisible.get(control);
            boolean curVisibility = mDelegate.canDisplayBottomControl(control.getId(), control);
            if (prevVisibility.booleanValue() != curVisibility) {
                if (!containerVisibilityChanged) {
                    control.clearAnimation();
                    control.startAnimation(getControlAnimForVisibility(curVisibility));
                }
                control.setVisibility(curVisibility ? View.VISIBLE : View.INVISIBLE);
                mControlsVisible.put(control, curVisibility);
            }
        }
        // Force a layout change
        mContainer.requestLayout(); // Kick framework to draw the control.
    }

    public void cleanup() {
        mParentLayout.removeView(mContainer);
        mControlsVisible.clear();
    }

    @Override
    public void onClick(View view) {
        Boolean controlVisible = mControlsVisible.get(view);
        if (mContainerVisible && controlVisible != null && controlVisible.booleanValue()) {
            mDelegate.onBottomControlClicked(view.getId());
        }
    }

    public boolean getIsEditable() {
        return isEditable;
    }

    private boolean sixTabs = false;
    private boolean twoTabs = false;

    public void setIsEditable(boolean yes, boolean orientationChanged) {
    }

}

package com.freeme.community.base;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.droi.sdk.analytics.DroiAnalytics;
import com.freeme.gallery.BuildConfig;
import com.freeme.utils.FreemeUtils;

/**
 * ClassName: BaseFragmentActivity
 * Description:
 * Author: connorlin
 * Date: Created on 2016-6-17.
 */
public abstract class BaseFragmentActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initVariables();
        initViews(savedInstanceState);
        loadData();
    }

    protected void initVariables() {

    }

    protected void initViews(Bundle savedInstanceState) {

    }

    protected void loadData() {

    }

    protected void onDestroy() {
        super.onDestroy();
    }

    protected void onPause() {
        super.onPause();

        DroiAnalytics.onPause(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        DroiAnalytics.onResume(this);
    }
}
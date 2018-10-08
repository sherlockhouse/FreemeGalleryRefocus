package com.freeme.droiad;

import android.content.Context;
import android.view.View;
import android.widget.RelativeLayout;

import com.adroi.polysdk.listener.AdViewListener;
import com.adroi.polysdk.view.AdConfig;
import com.adroi.polysdk.view.AdView;

import okio.AsyncTimeout;

/**
 * 作者：gulincheng
 * 日期:2018/09/07 15:04
 * 文件描述:卓易广告Adroi控制类
 */
public class DroiAdViewController extends AsyncTimeout {
    public static final String DROI_BANNER_ID = "s4ba91445";
    public static final String TAG = "DroiAdViewController";
    private final AdView mBannerView;

    @Override
    protected void timedOut() {
        mBannerView.post(new Runnable() {
            @Override
            public void run() {
                mBannerView.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void addToParent(RelativeLayout parent) {
        if (parent != null && mBannerView != null) {
            parent.removeView(mBannerView);
            parent.addView(mBannerView);
        }
    }

    public DroiAdViewController(Context mContext) {
        mBannerView = new AdView(mContext, AdConfig.AD_TYPE_BANNER, DROI_BANNER_ID, "");
        mBannerView.setListener(new AdViewListener() {

            @Override
            public void onAdClick(String s) {
                android.util.Log.d(TAG, "banner onAdClick");
            }

            @Override
            public void onAdDismissed() {
                android.util.Log.d(TAG, "banner onAdDismissed");
            }

            @Override
            public void onAdFailed(String arg0) {
                android.util.Log.d(TAG, "banner onAdFailed  " + arg0);

            }

            @Override
            public void onAdReady() {
                android.util.Log.d(TAG, "banner onAdReady");

            }

            @Override
            public void onAdShow() {
                android.util.Log.d(TAG, "banner onAdShow");

            }

            @Override
            public void onAdShowFailed() {
                android.util.Log.d(TAG, "banner onAdShowFailed");

            }

            @Override
            public void onAdSwitch() {
                android.util.Log.d(TAG, "banner onAdSwitch");

            }
        });
    }

    public void destoryAdview() {
        if (mBannerView != null) {
            mBannerView.onDestroyAd();
        }
    }

    public void enterSafe() {
        exit();
        enter();
    }
}

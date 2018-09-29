/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.freeme.gallery.app;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.aiwinn.wrapper.FaceSimManager;
import com.aiwinn.wrapper.able.OnInitFaceCallbackListener;
import com.android.gallery3d.app.GalleryApp;
import com.droi.sdk.analytics.DroiAnalytics;
import com.facebook.stetho.Stetho;
import com.facebook.stetho.okhttp.StethoInterceptor;
import com.freeme.community.utils.AccountUtil;
import com.freeme.gallery.R;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.DownloadCache;
import com.android.gallery3d.data.ImageCacheService;
import com.android.gallery3d.gadget.WidgetUtils;
import com.android.gallery3d.picasasource.PicasaSource;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.UsageStatistics;
import com.android.gallery3d.util.ThreadPool;
import com.freeme.provider.GalleryDBManager;
//import com.freeme.updateself.update.UpdateMonitor;
import com.freeme.utils.CustomJsonParser;
import com.freeme.utils.SettingProperties;
import com.mediatek.gallery3d.adapter.FeatureManager;
import com.squareup.okhttp.OkHttpClient;

import java.io.File;
import java.io.IOException;

public class GalleryAppImpl extends MultiDexApplication implements GalleryApp {

    private static final String TAG = "GalleryAppImpl";
    private static final String DOWNLOAD_FOLDER   = "download";
    private static final long   DOWNLOAD_CAPACITY = 64 * 1024 * 1024; // 64M

    private ImageCacheService mImageCacheService;
    private Object mLock = new Object();
    private DataManager   mDataManager;
    private ThreadPool    mThreadPool;
    private DownloadCache mDownloadCache;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        System.getProperties();

        // Load configurations from setting files
        try {
            SettingProperties.build("/assets/freemegallery.properties",
                    "/system/vendor/etc/freemegallery_custom.properties",
                    "/system/etc/freemegallery_custom.properties");
        } catch (IOException e) {
            if (Build.TYPE.equals("eng") || Build.TYPE.equals("userdebug")) {
                throw new RuntimeException(e);
            } else {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Stetho.initializeWithDefaults(this);
        new OkHttpClient().networkInterceptors().add(new StethoInterceptor());

        initializeAsyncTask();
        GalleryUtils.initialize(this);
        WidgetUtils.initialize(this);
        PicasaSource.initialize(this);
        FeatureManager.setup(getAndroidContext());
        /*UsageStatistics.initialize(this);
        AccountUtil.getInstance(this);*/
        //*/ Added by droi Linguanrong for droi push, 16-3-7
        //DroiPushManager.getInstance(this).init();
        //*/
        CustomJsonParser.getInstance();

        String path = "/system/etc/";
//        String path = Environment.getExternalStorageDirectory().getPath()+"/data";
        Log.w("face", path);
        FaceSimManager.initFace(this, path, 12, new
                OnInitFaceCallbackListener() {
                    @Override
                    public void Succ() {
                        Log.w("face", "init done!");
                    }
                    @Override
                    public void Fail(int i, String s) {
                        Log.w("face", "init fail!");
                    }
                });
        boolean flag = FaceSimManager.checkPb();

        // for baas analytics
       /* DroiAnalytics.initialize(this);

        UpdateMonitor.Builder
                 /*//*//* init UpdateMonitor
                .getInstance(this)
                 /*//*//* register you Application to obsever
                .registerApplication(this)
                 /*//*//* register you Application is Service or hasEnrtyActivity
                .setApplicationIsServices(true)
                 /*//*//* default notify small icon, ifnot set use updateself_ic_notify_small
                .setDefaultNotifyIcon(R.drawable.updateself_ic_notify_small)
                .complete();*/
    }

    private void initializeAsyncTask() {
        // AsyncTask class needs to be loaded in UI thread.
        // So we load it here to comply the rule.
        try {
            Class.forName(AsyncTask.class.getName());
        } catch (ClassNotFoundException e) {
        }
    }

    @Override
    public synchronized DataManager getDataManager() {
        if (mDataManager == null) {
            mDataManager = new DataManager(this);
            mDataManager.initializeSourceMap();
        }
        return mDataManager;
    }


    @Override
    public ImageCacheService getImageCacheService() {
        // This method may block on file I/O so a dedicated lock is needed here.
        synchronized (mLock) {
            if (mImageCacheService == null) {
                mImageCacheService = new ImageCacheService(getAndroidContext());
            }
            return mImageCacheService;
        }
    }

    @Override
    public synchronized DownloadCache getDownloadCache() {
        if (mDownloadCache == null) {
            File cacheDir = new File(getExternalCacheDir(), DOWNLOAD_FOLDER);

            if (!cacheDir.isDirectory()) cacheDir.mkdirs();

            if (!cacheDir.isDirectory()) {
                throw new RuntimeException(
                        "fail to create: " + cacheDir.getAbsolutePath());
            }
            mDownloadCache = new DownloadCache(this, cacheDir, DOWNLOAD_CAPACITY);
        }
        return mDownloadCache;
    }

    @Override
    public synchronized ThreadPool getThreadPool() {
        if (mThreadPool == null) {
            mThreadPool = new ThreadPool();
        }
        return mThreadPool;
    }

    @Override
    public Context getAndroidContext() {
        return this;
    }
}

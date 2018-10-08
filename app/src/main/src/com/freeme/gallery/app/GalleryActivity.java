/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.Manifest;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v4.view.ViewPager;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.aiwinn.wrapper.FaceSimManager;
import com.android.gallery3d.app.ActivityState;
import com.android.gallery3d.app.AlbumPage;
import com.android.gallery3d.app.AlbumSetPage;
import com.android.gallery3d.app.FilterUtils;
import com.android.gallery3d.app.Log;
import com.android.gallery3d.app.PhotoPage;
import com.android.gallery3d.app.SinglePhotoPage;
import com.android.gallery3d.app.SlideshowPage;
import com.android.gallery3d.app.StateManager;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.gadget.WidgetUtils;
import com.android.gallery3d.picasasource.PicasaSource;
import com.android.gallery3d.util.GalleryUtils;
import com.freeme.data.StoryAlbumSet;
import com.freeme.data.VisitorAlbum;
import com.freeme.data.VisitorAlbumVideo;
import com.freeme.gallery.BuildConfig;
import com.freeme.gallery.R;
import com.freeme.page.AlbumCameraPage;
import com.freeme.page.AlbumStorySetPage;
import com.freeme.page.AlbumTimeShaftPage;
import com.freeme.page.AlbumVisitorPage;
import com.freeme.provider.GalleryDBManager;
import com.freeme.provider.MediaStoreImporter;
import com.freeme.scott.galleryui.design.adapter.GalleryPageAdapter;
import com.freeme.scott.galleryui.design.widget.FreemeBottomSelectedController;
import com.freeme.scott.galleryui.design.widget.GalleryViewPager;
import com.freeme.utils.FreemeUtils;
import com.freeme.utils.LogcatHelper;
import com.mediatek.gallery3d.adapter.FeatureHelper;
import com.mediatek.gallery3d.util.PermissionHelper;
import com.mediatek.gallery3d.util.TraceHelper;
import com.mediatek.galleryframework.base.MediaFilter;
import com.mediatek.galleryframework.base.MediaFilterSetting;

import java.util.ArrayList;

public final class GalleryActivity extends AbstractGalleryActivity
        implements OnCancelListener,ViewPager.OnPageChangeListener {
    public static final String EXTRA_SLIDESHOW = "slideshow";
    public static final String EXTRA_DREAM = "dream";
    public static final String EXTRA_CROP = "crop";
    /// M: [BUG.ADD] @{
    public static final String EXTRA_FROM_WIDGET = "fromWidget";
    /// @}

    public static final String ACTION_REVIEW = "com.android.camera.action.REVIEW";
    public static final String KEY_GET_CONTENT = "get-content";
    public static final String KEY_GET_ALBUM = "get-album";
    public static final String KEY_TYPE_BITS = "type-bits";
    public static final String KEY_MEDIA_TYPES = "mediaTypes";
    public static final String KEY_DISMISS_KEYGUARD = "dismiss-keyguard";
    public static final String IS_IKO_START = "isIkoStart";

    private static final String TAG = "GalleryActivity";
    //*/ Added by tyd Linguanrong for freeme gallery, 16-1-13
    private static final String SHOW_TAB_GUIDE = "showTabGuide";
    private final static int REQUEST_COMMUNITY = 1100;
    public final static int INDEX_CAMERA = 0;
    public final static int INDEX_STORY = 2;
    public final static int INDEX_ALBUM = 1;
    public final static int INDEX_COMMUNITY = 3;
    public final static int COUNT_DEFAULT = 3;

    //*/ Added by Tyd Lpublicinguanrong for secret photos, 2014-3-10
    private static final String VISITOR_MODE_ON = "com.freeme.ACTION_VISITOR_MODE_ON";
    public static final int CHOOSER_REQUEST_CODE = 2018;
    private Dialog mVersionCheckDialog;
//    private RadioGroup radiogroup;
    private Animation bottomdown;
    private Animation bottomup;
    private VisitorModeChangedReceiver mReceiver;

    private Context mContext;
    //*/ Added by droi Linguanrong for visitor mode, 16-3-31
    private SettingsObserver mSettingsObserver;

    //*/ Added by droi Linguanrong for statistic, 16-7-19
    private boolean mStartOutside = false;
    //*/

    private GalleryViewPager mViewPager;

    private GalleryPageAdapter mAdapter;
    private final ArrayList<ViewPager.OnPageChangeListener> mOnPageChangeListeners = new ArrayList<>();
    private FreemeBottomSelectedController controller;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /// M: [DEBUG.ADD] @{
        TraceHelper.traceBegin(">>>>Gallery-onCreate");
        /// @}
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_ACTION_MODE_OVERLAY);

        getWindow().setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);


        if (getIntent().getBooleanExtra(KEY_DISMISS_KEYGUARD, false)) {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }

        setContentView(R.layout.main);
        setViewPager();
        initHintLayout();
        mContext = this;

        if (BuildConfig.DEBUG) {
            LogcatHelper.getInstance(this).start();
        }

        //*/ Added by tyd Linguanrong for freeme gallery, 16-1-13
        initAnimation();
        initBottomTab();
        //*/
        /// M: [FEATURE.MODIFY] [Runtime permission] @{
        /*
        if (savedInstanceState != null) {
            getStateManager().restoreFromState(savedInstanceState);
        } else {
            initializeByIntent();
        }
         */
        if (mGranted) {

            MediaStoreImporter.getInstance().setmResolver(this.getContentResolver());
            HandlerThread handlerThread = new HandlerThread("background-handler");
            handlerThread.start();
            Looper looper = handlerThread.getLooper();
            new Handler(looper).post(new Runnable() {
                @Override
                public void run() {
                    MediaStoreImporter.getInstance().deleteFiles();
                }
            });
            mViewPagerTabs.setVisibility(View.VISIBLE);
            /// M: [BUG.ADD] set gl_root_cover visible if open from widget or launch by @{
            // launcher, or else it will flash
            Intent intent = getIntent();
            if (intent != null
                    && (intent.getBooleanExtra(EXTRA_FROM_WIDGET, false) || (intent
                    .getAction() != null && intent.getAction().equals(
                    intent.ACTION_MAIN)))) {
                View view = findViewById(R.id.gl_root_cover);
                if (view != null) {
                    view.setVisibility(View.VISIBLE);
                    Log.i(TAG, "<onCreate> from widget or launcher, set gl_root_cover VISIBLE");
                }
            }

            /// @}
            if (savedInstanceState != null) {
                getStateManager().restoreFromState(savedInstanceState);
            } else {
                initializeByIntent();
            }

            //*/ Added by tyd Linguanrong for freeme gallery, 16-1-13
            if (!getStateManager().isStackEmpty()) {
                if (!(getStateManager().getTopState() instanceof AlbumSetPage
                        || getStateManager().getTopState() instanceof AlbumTimeShaftPage
                        || getStateManager().getTopState() instanceof AlbumCameraPage
                        || getStateManager().getTopState() instanceof AlbumStorySetPage)) {
                    setBottomTabVisibility(false);
                }
            }
            //*/
        } else {
            mSaveInstanceState = savedInstanceState;
        }
        /// @}
        /// M: [PERF.ADD] Modify CPU boost policy for first launch performance @{
        FeatureHelper.modifyBoostPolicy(this);
        /// @}
        /// M: [DEBUG.ADD] @{
        TraceHelper.traceEnd();
        /// @}
        //*/ Added by Tyd Linguanrong for secret photos, 2014-3-10
        IntentFilter visitorModeChangedFilter = new IntentFilter();
        visitorModeChangedFilter.addAction(VISITOR_MODE_ON);
        mReceiver = new VisitorModeChangedReceiver();
        registerReceiver(mReceiver, visitorModeChangedFilter);

        mSettingsObserver = new SettingsObserver();
        //*/
        getDataManager().forceRefreshAll();
    }
    private FreemeBottomSelectedController mBottomSelectedController;


    private void setViewPager() {
        String[] tabTitles = new String[COUNT_DEFAULT];
        tabTitles[INDEX_CAMERA] = getResources().getString(R.string.tab_by_camera);
        tabTitles[INDEX_ALBUM] = getResources().getString(R.string.tab_albums);
        tabTitles[INDEX_STORY] = getResources().getString(R.string.tab_by_story);
        mViewPager = findViewById(R.id.lists_pager);
        mAdapter = new GalleryPageAdapter(tabTitles);
        mViewPager.setAdapter(mAdapter);
        mViewPager.addOnPageChangeListener(this);
        mViewPagerTabs = findViewById(R.id.lists_pager_header);
        /*
        mViewPagerTabs.configureTabIcons(tabIcons);
        */
        mViewPagerTabs.setViewPager(mViewPager);
        mFreemeActionBarContainer = findViewById(R.id.actionbar_top_bar_freeme);
        addOnPageChangeListener(mViewPagerTabs);
        mFreemeBottomSelectedView = findViewById(R.id.bottom_container_freeme);
        mBottomSelectedController = new FreemeBottomSelectedController(mFreemeBottomSelectedView);
        controller = new FreemeBottomSelectedController(mFreemeBottomSelectedView);

    }

    private void  initHintLayout(){
        mHintLayout = (LinearLayout) findViewById(R.id.aiwin_hint);

    }



    @Override
    protected void onStop() {
        super.onStop();
        PowerManager manager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        if (!manager.isInteractive()) {
            finishActivity(CHOOSER_REQUEST_CODE);
        }

        //*/ Added by Tyd Linguanrong for secret photos, 2014-3-10
        if (mVisitorMode) {
            Settings.System.putInt(getContentResolver(), FreemeUtils.INNER_VISTOR_MODE, 0);
        }
        //*/
    }

    protected void onDestroy() {
        super.onDestroy();
        //*/ Added by Tyd Linguanrong for secret photos, 2014-3-10
        if (mVisitorMode) {
            Settings.System.putInt(getContentResolver(), FreemeUtils.INNER_VISTOR_MODE, 0);
        }

        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
        //*/

        mSettingsObserver.unregister();

        //*/ Added by tyd Linguanrong for statistic, 15-12-18
//        if (!mStartOutside) {
//            StatisticUtil.generateExitStatisticInfo(mContext, StatisticData.OPTION_EXIT);
//            StatisticUtil.saveStatisticInfoToFileFromDB(mContext);
//
//            // for baas analytics
//            DroiAnalytics.onEvent(mContext, StatisticData.OPTION_EXIT);
//        }
        //*/

        if (BuildConfig.DEBUG) {
            LogcatHelper.getInstance(this).stop();
        }
        GalleryDBManager.getInstance().unbindServer();
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
    }

    //private int resultIndex = -1;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }

        if (REQUEST_COMMUNITY == requestCode) {
            int resultIndex = data.getIntExtra("GalleryIndex", 0);
            if (INDEX_COMMUNITY == resultIndex) {
                finish();
            } else {
                switchPage(resultIndex);
            }
        }
    }

    private void switchPage(int index) {
        String basePath, newPath;
        Bundle data = new Bundle();
        data.putBoolean(FreemeUtils.KEY_FROM_COMMUNITY, true);
        StateManager stateManager = getStateManager();
        ActivityState topState = stateManager.getTopState();

        switch (index) {
            case INDEX_CAMERA:
//                radiogroup.check(R.id.bottom_tab_camera);
                newPath = getDataManager().makeCameraSetPath();
                boolean mTimeShaftPage = mSharedPreferences.getBoolean("default_page", true);
                if (mTimeShaftPage) {
                    data.putString(AlbumTimeShaftPage.KEY_MEDIA_PATH, newPath);
                    stateManager.switchState(topState, AlbumTimeShaftPage.class, data);
                } else {
                    data.putString(AlbumCameraPage.KEY_MEDIA_PATH, newPath);
                    stateManager.switchState(topState, AlbumCameraPage.class, data);
                }
                break;

            case INDEX_STORY:
//                radiogroup.check(R.id.bottom_tab_story);
                data.putBoolean(GalleryActivity.KEY_GET_CONTENT, false);
                data.putString(AlbumStorySetPage.KEY_MEDIA_PATH, StoryAlbumSet.PATH.toString());
                data.putInt(AlbumStorySetPage.KEY_SELECTED_CLUSTER_TYPE,
                        FreemeUtils.CLUSTER_BY_STORY);
                stateManager.switchState(topState, AlbumStorySetPage.class, data);
                break;

            case INDEX_ALBUM:
//                radiogroup.check(R.id.bottom_tab_album);
                basePath = getDataManager().getTopSetPath(DataManager.INCLUDE_ALL);
                newPath = FilterUtils.switchClusterPath(basePath, FreemeUtils.CLUSTER_BY_ALBUM);
                data.putString(AlbumSetPage.KEY_MEDIA_PATH, newPath);
                data.putInt(AlbumSetPage.KEY_SELECTED_CLUSTER_TYPE, FreemeUtils.CLUSTER_BY_ALBUM);
                getStateManager().switchState(topState, AlbumSetPage.class, data);
                break;
        }
    }

    public void setBottomTabVisibility(boolean visible) {
//        if (radiogroup != null) {
//            //radiogroup.startAnimation(visible ? bottomup : bottomdown);
//            radiogroup.setVisibility(visible ? View.VISIBLE : View.GONE);
//        }
    }

    private void initAnimation() {
        bottomdown = AnimationUtils.loadAnimation(this, R.anim.bottom_down);
        bottomup = AnimationUtils.loadAnimation(this, R.anim.bottom_up);
    }

    private void initBottomTab() {
//        radiogroup = (RadioGroup) findViewById(R.id.bottom_tab);
        final SharedPreferences sharedPref = getSharedPreferences(
                FreemeUtils.STORY_SHAREPREFERENCE_KEY, MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPref.edit();
        boolean showStoryGuide = sharedPref.getBoolean(SHOW_TAB_GUIDE, true);
        updateStoryGuide(showStoryGuide);
//        radiogroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(RadioGroup group, int checkedId) {
//                switch (checkedId) {
//                    case R.id.bottom_tab_camera:
//                        getGalleryActionBar().onBottomTabSelected(INDEX_CAMERA);
//                        break;
//
//                    case R.id.bottom_tab_story:
//                        getGalleryActionBar().onBottomTabSelected(INDEX_STORY);
//                        if (sharedPref.getBoolean(SHOW_TAB_GUIDE, true)) {
//                            updateStoryGuide(false);
//                            editor.putBoolean(SHOW_TAB_GUIDE, false);
//                            editor.apply();
//                        }
//                        break;
//
//                    case R.id.bottom_tab_album:
//                        getGalleryActionBar().onBottomTabSelected(INDEX_ALBUM);
//                        break;
//
//                    case R.id.bottom_tab_community:
//                        getGalleryActionBar().onBottomTabSelected(INDEX_COMMUNITY);
//                        break;
//                }
//            }
//        });
    }

    private void initializeByIntent() {
        Intent intent = getIntent();
        String action = intent.getAction();

        //*/ Added by droi Linguanrong for statistic, 16-7-19
        mStartOutside = true;
        //*/
        if (Intent.ACTION_GET_CONTENT.equalsIgnoreCase(action)) {
            startGetContent(intent);
        } else if (Intent.ACTION_PICK.equalsIgnoreCase(action)) {
            // We do NOT really support the PICK intent. Handle it as
            // the GET_CONTENT. However, we need to translate the type
            // in the intent here.
            Log.w(TAG, "action PICK is not supported");
            String type = Utils.ensureNotNull(intent.getType());
            if (type.startsWith("vnd.android.cursor.dir/")) {
                if (type.endsWith("/image")) intent.setType("image/*");
                if (type.endsWith("/video")) intent.setType("video/*");
            }
            startGetContent(intent);
        } else if (Intent.ACTION_VIEW.equalsIgnoreCase(action)
                || ACTION_REVIEW.equalsIgnoreCase(action)) {
            startViewAction(intent);
        }
        //*/ Added by Tyd Linguanrong for secret photos, 2014-2-17
        else if ("com.freeme.gallery3d.visitor".equalsIgnoreCase(action)) {
            startVisitor(intent.getBooleanExtra("secret_media", true));
        }
        //*/
        else {
            startDefaultPage();
        }
    }

    public void startDefaultPage() {
        PicasaSource.showSignInReminder(this);
        Bundle data = new Bundle();
        /*/Modified by Tyd Linguanrong for Gallery new style, 2013-12-23
        data.putString(AlbumSetPage.KEY_MEDIA_PATH,
                getDataManager().getTopSetPath(DataManager.INCLUDE_ALL));
        getStateManager().startState(AlbumSetPage.class, data);
        /*/
        String newPath;
        newPath = getDataManager().makeCameraSetPath();
        boolean mTimeShaftPage = mSharedPreferences.getBoolean("default_page", true);
        if (mTimeShaftPage) {
            data.putString(AlbumTimeShaftPage.KEY_MEDIA_PATH, newPath);
            getStateManager().startState(AlbumTimeShaftPage.class, data);
        } else {
            data.putString(AlbumCameraPage.KEY_MEDIA_PATH, newPath);
            getStateManager().startState(AlbumCameraPage.class, data);
        }
        //*/
        mVersionCheckDialog = PicasaSource.getVersionCheckDialog(this);
        if (mVersionCheckDialog != null) {
            mVersionCheckDialog.setOnCancelListener(this);
        }

        //*/ Added by tyd Linguanrong for statistic, 15-12-18
//        mStartOutside = false;
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                StatisticUtil.saveStatisticInfoToFileFromDB(mContext);
//                StatisticUtil.generateStatisticInfo(mContext, StatisticData.OPTION_ENTER);
//                // for baas analytics
//                DroiAnalytics.onEvent(mContext, StatisticData.OPTION_ENTER);
//            }
//        });
        //*/
    }
    private void updateStoryGuide(boolean showStoryGuide) {
//        RadioButton story = (RadioButton) findViewById(R.id.bottom_tab_story);
//        story.setCompoundDrawablesWithIntrinsicBounds(0,
//                showStoryGuide ? R.drawable.guide_tab_story : R.drawable.tab_story,
//                0, 0);
    }

    private void startGetContent(Intent intent) {
        Bundle data = intent.getExtras() != null
                ? new Bundle(intent.getExtras())
                : new Bundle();
        data.putBoolean(KEY_GET_CONTENT, true);
        int typeBits = GalleryUtils.determineTypeBits(this, intent);
        data.putInt(KEY_TYPE_BITS, typeBits);
        data.putString(AlbumSetPage.KEY_MEDIA_PATH,
                getDataManager().getTopSetPath(typeBits));
        getStateManager().startState(AlbumSetPage.class, data);
    }

    private String getContentType(Intent intent) {
        String type = intent.getType();
        if (type != null) {
            return GalleryUtils.MIME_TYPE_PANORAMA360.equals(type)
                ? MediaItem.MIME_TYPE_JPEG : type;
        }

        Uri uri = intent.getData();
        try {
            return getContentResolver().getType(uri);
        } catch (Throwable t) {
            Log.w(TAG, "get type fail", t);
            return null;
        }
    }

    private void startViewAction(Intent intent) {
        Boolean slideshow = intent.getBooleanExtra(EXTRA_SLIDESHOW, false);
        if (slideshow) {
            getActionBar().hide();
            DataManager manager = getDataManager();
            Path path = manager.findPathByUri(FreemeUtils.convertGalleryUri(intent.getData()),
                    intent.getType());
            if (path == null || manager.getMediaObject(path)
                    instanceof MediaItem) {
                path = Path.fromString(
                        manager.getTopSetPath(DataManager.INCLUDE_IMAGE));
            }
            Bundle data = new Bundle();
            data.putString(SlideshowPage.KEY_SET_PATH, path.toString());
            data.putBoolean(SlideshowPage.KEY_RANDOM_ORDER, true);
            data.putBoolean(SlideshowPage.KEY_REPEAT, true);
            if (intent.getBooleanExtra(EXTRA_DREAM, false)) {
                data.putBoolean(SlideshowPage.KEY_DREAM, true);
            }
            getStateManager().startState(SlideshowPage.class, data);
        } else {
            Bundle data = new Bundle();
            DataManager dm = getDataManager();
            Uri uri = intent.getData();
            String contentType = getContentType(intent);
            if (contentType == null) {
                Toast.makeText(this,
                        R.string.no_such_item, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            if (uri == null) {
                int typeBits = GalleryUtils.determineTypeBits(this, intent);
                data.putInt(KEY_TYPE_BITS, typeBits);
                data.putString(AlbumSetPage.KEY_MEDIA_PATH,
                        getDataManager().getTopSetPath(typeBits));
                getStateManager().startState(AlbumSetPage.class, data);
            } else if (contentType.startsWith(
                    ContentResolver.CURSOR_DIR_BASE_TYPE)) {
                int mediaType = intent.getIntExtra(KEY_MEDIA_TYPES, 0);
                if (mediaType != 0) {
                    uri = uri.buildUpon().appendQueryParameter(
                            KEY_MEDIA_TYPES, String.valueOf(mediaType))
                            .build();
                }
                Path setPath = dm.findPathByUri(uri, null);
                MediaSet mediaSet = null;
                if (setPath != null) {
                    mediaSet = (MediaSet) dm.getMediaObject(setPath);
                }
                if (mediaSet != null) {
                    if (mediaSet.isLeafAlbum()) {
                        data.putString(AlbumPage.KEY_MEDIA_PATH, setPath.toString());
                        data.putString(AlbumPage.KEY_PARENT_MEDIA_PATH,
                                dm.getTopSetPath(DataManager.INCLUDE_ALL));
                        getStateManager().startState(AlbumPage.class, data);
                    } else {
                        data.putString(AlbumSetPage.KEY_MEDIA_PATH, setPath.toString());
                        getStateManager().startState(AlbumSetPage.class, data);
                    }
                } else {
                    startDefaultPage();
                }
            } else {
                MediaFilter mf = MediaFilterSetting.getCurrentFilter();
                uri = FeatureHelper.tryContentMediaUri(this, uri);
                Log.d(TAG, "<startViewAction> uri:" + uri);

                if (uri == null) {
                    Toast.makeText(this, R.string.VideoView_error_text_type_not_supported,
                            Toast.LENGTH_LONG).show();
                    startDefaultPage();
                    return;
                }
                Path itemPath = dm.findPathByUri(uri, contentType);

                /// M: [BUG.ADD] modify for item not exit,show toast and finish.@{
                if (itemPath == null) {
                    Toast.makeText(this, R.string.no_such_item,
                            Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                /// @}

                /// M: [BUG.MODIFY] @{
                /*Path albumPath = dm.getDefaultSetOf(itemPath);*/
                Path albumPath = null;
                //clear old mediaObject, query database again
                itemPath.clearObject();
                albumPath = dm.getDefaultSetOf(itemPath);
                int count  = 0;
                while (albumPath == null && count < 8) {
                    try {
                        Thread.sleep(400);
                        count ++;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    albumPath = dm.getDefaultSetOf(itemPath);
                }
                /// @}

                data.putString(PhotoPage.KEY_MEDIA_ITEM_PATH, itemPath.toString());
                data.putBoolean(PhotoPage.KEY_READONLY, true);

                // TODO: Make the parameter "SingleItemOnly" public so other
                //       activities can reference it.
                boolean singleItemOnly = (albumPath == null)
                        || intent.getBooleanExtra("SingleItemOnly", false);
                if (!singleItemOnly) {
                    data.putString(PhotoPage.KEY_MEDIA_SET_PATH, albumPath.toString());
                    // when FLAG_ACTIVITY_NEW_TASK is set, (e.g. when intent is fired
                    // from notification), back button should behave the same as up button
                    // rather than taking users back to the home screen
                    if (intent.getBooleanExtra(PhotoPage.KEY_TREAT_BACK_AS_UP, false)
                            || ((intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0)) {
                        data.putBoolean(PhotoPage.KEY_TREAT_BACK_AS_UP, true);
                    }
                }
                getStateManager().startState(SinglePhotoPage.class, data);
            }
        }
    }

    private void startVisitor(boolean isImages) {
        Settings.System.putInt(getContentResolver(), FreemeUtils.INNER_VISTOR_MODE, 1);
        Bundle data = new Bundle();
        if (isImages) {
            data.putString(AlbumVisitorPage.KEY_MEDIA_PATH, VisitorAlbum.PATH.toString());
        } else {
            data.putString(AlbumVisitorPage.KEY_MEDIA_PATH, VisitorAlbumVideo.PATH.toString());
        }
        data.putBoolean(AlbumVisitorPage.KEY_VISITOR_TYPE, isImages);
        getStateManager().startState(AlbumVisitorPage.class, data);
    }
    //*/
    @Override
    protected void onResume() {
        /// M: [DEBUG.ADD] @{
        TraceHelper.traceBegin(">>>>Gallery-onResume");
        /// @}
        /// M: [FEATURE.MARK] [Runtime permission] @{
        /*Utils.assertTrue(getStateManager().getStateCount() > 0);*/
        /// @}
        super.onResume();
        //*/gulincheng 20180614 add statistic info for aiwinn
        FaceSimManager.uLink(getApplicationContext());
        //*/

        FreemeUtils.setScreenBrightness(this,getWindow());
        if (mVersionCheckDialog != null) {
            mVersionCheckDialog.show();
        }
        /// M: [DEBUG.ADD] @{
        TraceHelper.traceEnd();


        //*/ Added by Linguanrong for secret photos, 2014-9-17
        int state = Settings.System.getInt(getContentResolver(), "tydtech_vistor_mode_state", 0);
        if (state == 1 && mVisitorMode) {
            finish();
        }

        if (mVisitorMode) {
            Settings.System.putInt(getContentResolver(), FreemeUtils.INNER_VISTOR_MODE, 1);
        }
        //*/

        // for baas analytic
//        DroiAnalytics.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        FreemeUtils.restoreScreenBrightness(this);

        if (mVersionCheckDialog != null) {
            mVersionCheckDialog.dismiss();
        }

        // for baas analytic
//        DroiAnalytics.onPause(this);
    }


    @Override
    public void onCancel(DialogInterface dialog) {
        if (dialog == mVersionCheckDialog) {
            mVersionCheckDialog = null;
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        final boolean isTouchPad = (event.getSource()
                & InputDevice.SOURCE_CLASS_POSITION) != 0;
        if (isTouchPad) {
            float maxX = event.getDevice().getMotionRange(MotionEvent.AXIS_X).getMax();
            float maxY = event.getDevice().getMotionRange(MotionEvent.AXIS_Y).getMax();
            View decor = getWindow().getDecorView();
            float scaleX = decor.getWidth() / maxX;
            float scaleY = decor.getHeight() / maxY;
            float x = event.getX() * scaleX;
            //x = decor.getWidth() - x; // invert x
            float y = event.getY() * scaleY;
            //y = decor.getHeight() - y; // invert y
            MotionEvent touchEvent = MotionEvent.obtain(event.getDownTime(),
                    event.getEventTime(), event.getAction(), x, y, event.getMetaState());
            return dispatchTouchEvent(touchEvent);
        }
        return super.onGenericMotionEvent(event);
    }
    //*/

    public void startCommunity() {
        //Intent intent = new Intent("action.intent.freeme.Community");
        //ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(mActivity, 0,
        // 0);
        //ActivityCompat.startActivityForResult(mActivity, intent, 1001, options.toBundle());

        Intent intent = new Intent("action.intent.freeme.Community");
        startActivityForResult(intent, REQUEST_COMMUNITY);
        //overridePendingTransition(0, 0);
    }

    public FreemeBottomSelectedController getController() {
        return controller;
    }

    public void setController(FreemeBottomSelectedController controller) {
        this.controller = controller;
    }


    public class VisitorModeChangedReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            if (VISITOR_MODE_ON.equals(intent.getAction())) {
                finish();
            }
        }
    }
    //*/

    private final class SettingsObserver extends ContentObserver {
        private final Uri mVisitorUri = Settings.System.getUriFor(FreemeUtils.VISTOR_MODE_STATE);

        ContentResolver resolver = GalleryActivity.this.getContentResolver();

        public SettingsObserver() {
            super(new Handler());
//            ContentResolver resolver = GalleryActivity.this.getContentResolver();
            resolver.registerContentObserver(mVisitorUri, false, this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (mVisitorUri.equals(uri)) {
                GalleryActivity.this.finish();
            }
        }

        public void unregister() {
            resolver.unregisterContentObserver(this);
        }
    }
    //*/
    //********************************************************************
    //*                              MTK                                 *
    //********************************************************************

    // <CTA Data Protection> @{
    // Start with CTA DataProtection action,
    // View the Image as FL drm file format.
    private static final String ACTION_VIEW_LOCKED_FILE =
            "com.mediatek.dataprotection.ACTION_VIEW_LOCKED_FILE";
    private String mToken = null;
    private String mTokenKey = null;

    private void startViewLockedFileAction() {
        Intent intent = getIntent();
        mToken = intent.getStringExtra("TOKEN");
        mTokenKey = intent.getStringExtra("TOKEN_KEY");
        if (intent.getData() == null) {
            Log.i(TAG, "<startViewLockedFileAction> intent.getData() is null, finish activity");
            finish();
        }

//        if (null == mToken
//                || !FeatureHelper.isTokenValid(this, mTokenKey, mToken)) {
//            Log.i(TAG, "<startViewLockedFileAction> token invalid, finish activity");
//            finish();
//        }
        Bundle data = new Bundle();
        DataManager dm = getDataManager();
        Path itemPath = dm.findPathByUri(intent.getData(), intent.getType());
        if (itemPath == null) {
            Toast.makeText(this, R.string.no_such_item, Toast.LENGTH_LONG)
                    .show();
            Log.i(TAG, "<startViewLockedFileAction> find path is null, finish activity");
            finish();
            return;
        }
        /// M: [BUG.MODIFY] Clear old mediaObject, query database again @{
        itemPath.clearObject();
        /// @}
        data.putBoolean(PhotoPage.KEY_READONLY, true);
        data.putString(SinglePhotoPage.KEY_MEDIA_ITEM_PATH, itemPath.toString());
        Log.i(TAG, "<startViewLockedFileAction> startState SinglePhotoPage, path = "
                        + itemPath);
        getStateManager().startState(SinglePhotoPage.class, data);
    }

    private void pauseViewLockedFileAction() {
        if (mToken == null) {
            return;
        }
        Log.i(TAG, "<pauseViewLockedFileAction> Finish activity when pause");
//        FeatureHelper.clearToken(this, mTokenKey, mToken);
        finish();
    }
    // @}

    // add for log trace @{
    protected void onStart() {
        TraceHelper.traceBegin(">>>>Gallery-onStart");
        super.onStart();
        TraceHelper.traceEnd();
    }
    // @}

    // [Runtime permission] @{
    private Bundle mSaveInstanceState;

    @Override
    public void onRequestPermissionsResult(int requestCode,
            String[] permissions, int[] grantResults) {
        // when gallery permssion changed, notify all widgets to update
        WidgetUtils.notifyAllWidgetViewChanged();
        if (getStateManager().getStateCount() != 0) {
            Log.i(TAG, "<onRequestPermissionsResult> dispatch to ActivityState");
            getStateManager().getTopState().onRequestPermissionsResult(requestCode, permissions,
                    grantResults);
        } else if (PermissionHelper.isAllPermissionsGranted(permissions, grantResults)) {
            Log.i(TAG, "<onRequestPermissionsResult> all permission granted");
            GalleryDBManager.getInstance().initDB(this, FREEMEGALLERY_DB);
            if (mSaveInstanceState != null) {
                getStateManager().restoreFromState(mSaveInstanceState);
            } else {
                initializeByIntent();
            }
        } else {
            for (int i = 0; i < permissions.length; i++) {
                if (Manifest.permission.READ_EXTERNAL_STORAGE.equals(permissions[i])
                        && grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    PermissionHelper.showDeniedPrompt(this);
                    break;
                }

                if (Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permissions[i])
                        && grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    PermissionHelper.showDeniedPrompt(this);
                    break;
                }

                if (Manifest.permission.READ_PHONE_STATE.equals(permissions[i])
                        && grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    PermissionHelper.showDeniedPrompt(this);
                    break;
                }
            }
            Log.i(TAG, "<onRequestPermissionsResult> permission denied, finish");
            finish();
        }
    }
    // @}


    public void addOnPageChangeListener(ViewPager.OnPageChangeListener onPageChangeListener) {
        if (!mOnPageChangeListeners.contains(onPageChangeListener)) {
            mOnPageChangeListeners.add(onPageChangeListener);
        }
    }


    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        final int count = mOnPageChangeListeners.size();
        for (int i = 0; i < count; i++) {
            mOnPageChangeListeners.get(i).onPageScrolled(position, positionOffset, positionOffsetPixels);
        }
    }

    @Override
    public void onPageSelected(int position) {

        final int count = mOnPageChangeListeners.size();
        for (int i = 0; i < count; i++) {
            mOnPageChangeListeners.get(i).onPageSelected(position);
        }
        getGalleryActionBar().onBottomTabSelected(position);

    }

    @Override
    public void onPageScrollStateChanged(int state) {
        final int count = mOnPageChangeListeners.size();
        for (int i = 0; i < count; i++) {
            mOnPageChangeListeners.get(i).onPageScrollStateChanged(state);
        }
    }

}

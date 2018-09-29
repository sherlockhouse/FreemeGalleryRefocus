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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.print.PrintHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.aiwinn.wrapper.FaceSimManager;
import com.android.gallery3d.app.GalleryActionBar;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.app.GalleryContext;
import com.android.gallery3d.app.Log;
import com.android.gallery3d.app.OrientationManager;
import com.android.gallery3d.app.StateManager;
import com.android.gallery3d.app.TransitionStore;
import com.android.gallery3d.util.MediaSetUtils;
import com.freeme.gallery.BuildConfig;
import com.freeme.gallery.R;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.filtershow.cache.ImageLoader;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.GLRootView;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.PanoramaViewHelper;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.util.ThreadPool;
import com.freeme.gesturesensor.GestureSensorManger;
import com.android.photos.data.GalleryBitmapPool;
import com.freeme.provider.GalleryDBManager;
import com.freeme.scott.galleryui.design.widget.FreemeActionBarUpContainerLayout;
import com.freeme.scott.galleryui.design.widget.FreemeBottomSelectedView;
import com.freeme.scott.galleryui.design.widget.ViewPagerTabs;
import com.freeme.ui.manager.NaviController;
import com.freeme.ui.manager.NavigationWidgetManager;
import com.freeme.ui.manager.State;
import com.freeme.utils.SystemPropertiesProxy;
import com.mediatek.gallery3d.util.PermissionHelper;

import java.io.FileNotFoundException;

public class AbstractGalleryActivity extends Activity implements GalleryContext,NaviController {
    private static final String TAG = "AbstractGalleryActivity";
    //*/ Added by droi Linguanrong for adjust glroot view layout, 2014-6-12
    public int mStatusBarHeight;
    //*/Added by Tyd Linguanrong for Gallery new style, 2013-12-12
    public SharedPreferences        mSharedPreferences;
    public SharedPreferences.Editor mEditor;
    //*/
    //*/ Added by Linguanrong for story album, 2015-7-2
    public boolean mIsSelectionMode = false;
    //*/Added by Tyd Linguanrong for Gallery secret photos, 2014-2-20
    public boolean mVisitorMode = false;
    private GLRootView         mGLRootView;
    private StateManager mStateManager;
    private GalleryActionBar mActionBar;
    //*/
    private OrientationManager mOrientationManager;
    private TransitionStore mTransitionStore = new TransitionStore();
    private boolean            mDisableToggleStatusBar;
    //*/
    private PanoramaViewHelper mPanoramaViewHelper;
    //*/
    private AlertDialog       mAlertDialog   = null;
    private BroadcastReceiver mMountReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (getExternalCacheDir() != null) onStorageReady();
        }
    };
    private IntentFilter      mMountFilter   = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
    private BatchService mBatchService;
    private boolean           mBatchServiceIsBound    = false;

    //*/ Added by Tyd Linguanrong for [tyd00519666] set slide show background, 2014-4-30
    private float[] mSetBackgroundColor = null;
    public boolean mIsSlideShow = false;
    //*/

    private final Object mGestureSyncObj = new Object();
    private boolean mSlideByGestureEnable;
    private boolean mHasGestureSensor;
    private SensorManager mSensorManager;
    private Sensor mGestureSensor;
    private onGestureSensorListener mGestureChangedListener;
    private boolean mHasRegister;
    private TakingGestrueListener mGestureListener = new TakingGestrueListener();

    private ServiceConnection mBatchServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBatchService = ((BatchService.LocalBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mBatchService = null;
        }
    };


    public static int colorPrimary;
    public static int colorPrimaryDarkValue;
    protected boolean mGranted;


    public ViewPagerTabs mViewPagerTabs;
    public FreemeBottomSelectedView mFreemeBottomSelectedView;
    public FreemeActionBarUpContainerLayout mFreemeActionBarContainer;

    public static final String FREEMEGALLERY_DB = "freemegalleryai.db";
    private int mCurrentState;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGranted = PermissionHelper.checkAndRequestForGallery(this);
        if (mGranted) {
            //*/ Added by droi Linguanrong for freeme gallery db, 16-1-19
            GalleryDBManager.getInstance().initDB(this, FREEMEGALLERY_DB);
            //*/
        }
        TypedArray array = getTheme().obtainStyledAttributes(new int[] {
                android.R.attr.colorPrimary, android.R.attr.colorPrimaryDark
        });
        colorPrimary = array.getResourceId(0, 0);
        colorPrimaryDarkValue = getResources().getColor(array.getResourceId(1, 0));
        array.recycle();
        //*/Added by Tyd Linguanrong for Gallery secret photos, 2014-2-20
        String action = getIntent().getAction();
        if (action != null && "com.freeme.gallery3d.visitor".equals(action)) {
            mVisitorMode = true;
        }
        //*/
        mOrientationManager = new OrientationManager(this);
        toggleStatusBarByOrientation();
        getWindow().setBackgroundDrawable(null);
        mPanoramaViewHelper = new PanoramaViewHelper(this);
        mPanoramaViewHelper.onCreate();
        doBindBatchService();

        //*/ Added by droi Linguanrong for adjust glroot view layout, 2014-6-12
        mStatusBarHeight = (int) this.getResources().getDimension(R.dimen.status_bar_height);
        //*/

        //*/ Added by Tyd Linguanrong for Gallery new style, 2014-2-15
        mSharedPreferences = getSharedPreferences("Gallery", Context.MODE_PRIVATE);
        mEditor = mSharedPreferences.edit();
        //*/

        //*/ Added by Tyd Linguanrong for [tyd00519666] set slide show background, 2014-4-30
        mSetBackgroundColor = GalleryUtils.intColorToFloatARGBArray(
                this.getResources().getColor(R.color.slideshow_background));
        //*/

        initGestureSensor();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (getExternalCacheDir() == null) {
            OnCancelListener onCancel = new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    finish();
                }
            };
            OnClickListener onClick = new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            };
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle(R.string.no_external_storage_title)
                    .setMessage(R.string.no_external_storage)
                    .setNegativeButton(android.R.string.cancel, onClick)
                    .setOnCancelListener(onCancel);
            if (ApiHelper.HAS_SET_ICON_ATTRIBUTE) {
                setAlertDialogIconAttribute(builder);
            } else {
                builder.setIcon(android.R.drawable.ic_dialog_alert);
            }
            mAlertDialog = builder.show();
            registerReceiver(mMountReceiver, mMountFilter);
        }
        mPanoramaViewHelper.onStart();
    }

    @TargetApi(ApiHelper.VERSION_CODES.HONEYCOMB)
    private static void setAlertDialogIconAttribute(
            AlertDialog.Builder builder) {
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
    }

    @Override
    protected void onStop() {
        /// M: [DEBUG.ADD] @{
        Log.d(TAG, "<onStop>");
        /// @}
        super.onStop();
        if (mAlertDialog != null) {
            unregisterReceiver(mMountReceiver);
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }
        mPanoramaViewHelper.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //*/ Added by Tyd Linguanrong for [tyd00519666] set slide show background, 2014-4-30
        String action = getIntent().getAction();
        if(Intent.ACTION_VIEW.equals(action)
                && getIntent().getBooleanExtra("slideshow", false)) {
            mIsSlideShow = true;
        }

        if(mIsSlideShow) {
            setDefaultBackgroundColor(mSetBackgroundColor);
        }
        //*/

        mGLRootView.lockRenderThread();
        /// M: [BUG.ADD] @{
        // when default storage has been changed, we should refresh bucked id,
        // or else the icon showing on the album set slot can not update
//        MediaSetUtils.refreshBucketId();
        /// @}
        try {
            getStateManager().resume();
            getDataManager().resume();
        } finally {
            mGLRootView.unlockRenderThread();
        }
        mGLRootView.onResume();
        mOrientationManager.resume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mGLRootView.lockRenderThread();
        try {
            super.onSaveInstanceState(outState);
            getStateManager().saveState(outState);
        } finally {
            mGLRootView.unlockRenderThread();
        }
    }

    public synchronized StateManager getStateManager() {
        if (mStateManager == null) {
            mStateManager = new StateManager(this);
        }
        return mStateManager;
    }

    @Override
    protected void onPause() {
        /// M: [DEBUG.ADD] @{
        Log.d(TAG, "<onPause>");
        /// @}
        super.onPause();
        mOrientationManager.pause();
        mGLRootView.onPause();
        mGLRootView.lockRenderThread();
        try {
            getStateManager().pause();
            getDataManager().pause();
        } finally {
            mGLRootView.unlockRenderThread();
        }
        GalleryBitmapPool.getInstance().clear();
        MediaItem.getBytesBufferPool().clear();
    }



    @Override
    protected void onDestroy() {
        /// M: [DEBUG.ADD] @{
        Log.d(TAG, "<onDestroy>");
        /// @}
        super.onDestroy();
        mGLRootView.lockRenderThread();
        try {
            getStateManager().destroy();
        } finally {
            mGLRootView.unlockRenderThread();
        }
        doUnbindBatchService();
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        //*/ Added by droi Linguanrong for freeme gallery, 16-1-14
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mStatusBarHeight = 0;
        } else {
            mStatusBarHeight = (int) this.getResources().getDimension(R.dimen.status_bar_height);
        }
        //*/
        mStateManager.onConfigurationChange(config);
        getGalleryActionBar().onConfigurationChanged();
        invalidateOptionsMenu();
        toggleStatusBarByOrientation();
    }

    @Override
    public void setContentView(int resId) {
        super.setContentView(resId);
        mGLRootView = (GLRootView) findViewById(R.id.gl_root_view);
    }

    @Override
    public void onBackPressed() {
        // send the back event to the top sub-state
        GLRoot root = getGLRoot();
        root.lockRenderThread();
        try {
            getStateManager().onBackPressed();
        } finally {
            root.unlockRenderThread();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return getStateManager().createOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        GLRoot root = getGLRoot();
        root.lockRenderThread();
        try {
            return getStateManager().itemSelected(item);
        } finally {
            root.unlockRenderThread();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mGLRootView.lockRenderThread();
        try {
            getStateManager().notifyActivityResult(
                    requestCode, resultCode, data);
        } finally {
            mGLRootView.unlockRenderThread();
        }
    }

    public GLRoot getGLRoot() {
        return mGLRootView;
    }

    public GalleryActionBar getGalleryActionBar() {
        if (mActionBar == null) {
            mActionBar = new GalleryActionBar(this);
        }
        return mActionBar;
    }
    public GalleryActionBar getGalleryActionBarWithoutTap() {
        if (mActionBar == null) {
            mActionBar = new GalleryActionBar(this, true);
        }
        return mActionBar;
    }

    private void doUnbindBatchService() {
        if (mBatchServiceIsBound) {
            // Detach our existing connection.
            unbindService(mBatchServiceConnection);
            mBatchServiceIsBound = false;
        }
    }

    @Override
    public DataManager getDataManager() {
        return ((GalleryApp) getApplication()).getDataManager();
    }

    @Override
    public Context getAndroidContext() {
        return this;
    }

    @Override
    public ThreadPool getThreadPool() {
        return ((GalleryApp) getApplication()).getThreadPool();
    }

    // Shows status bar in portrait view, hide in landscape view
    private void toggleStatusBarByOrientation() {
        if (mDisableToggleStatusBar) return;

        Window win = getWindow();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            win.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            win.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    private void doBindBatchService() {
        bindService(new Intent(this, BatchService.class), mBatchServiceConnection, Context.BIND_AUTO_CREATE);
        mBatchServiceIsBound = true;
    }

    public OrientationManager getOrientationManager() {
        return mOrientationManager;
    }

    protected void onStorageReady() {
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
            mAlertDialog = null;
            unregisterReceiver(mMountReceiver);
        }
    }

    protected void disableToggleStatusBar() {
        mDisableToggleStatusBar = true;
    }

    public TransitionStore getTransitionStore() {
        return mTransitionStore;
    }

    public PanoramaViewHelper getPanoramaViewHelper() {
        return mPanoramaViewHelper;
    }

    public boolean isFullscreen() {
        return (getWindow().getAttributes().flags
                & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0;
    }

    public ThreadPool getBatchServiceThreadPoolIfAvailable() {
        if (mBatchServiceIsBound && mBatchService != null) {
            return mBatchService.getThreadPool();
        } else {
            throw new RuntimeException("Batch service unavailable");
        }
    }

    public void printSelectedImage(Uri uri) {
        if (uri == null) {
            return;
        }
        String path = ImageLoader.getLocalPathFromUri(this, uri);
        if (path != null) {
            Uri localUri = Uri.parse(path);
            path = localUri.getLastPathSegment();
        } else {
            path = uri.getLastPathSegment();
        }
        PrintHelper printer = new PrintHelper(this);
        try {
            printer.printBitmap(path, uri);
        } catch (FileNotFoundException fnfe) {
            Log.e(TAG, "Error printing an image", fnfe);
        }
    }

    //*/ Added by droi Linguanrong for freem gallery, 16-4-1
    public void setDefaultBackgroundColor(float[] color) {
        mGLRootView.setDefaultBackgroundColor(color);
    }
    //*/

    //*/ Added by droi Linguanrong for disable light out mode, 16-4-14
    public void setLigthOutMOdeDisable(boolean disable) {
        mGLRootView.setLigthOutMOdeDisable(disable);
    }
    //*/

    public void setGestureChangedListener(onGestureSensorListener listener) {
        synchronized (mGestureSyncObj) {
            registerGestureSensorListener();
            mGestureChangedListener = listener;
        }
    }

    public static final int IN_PHOTOPAGE = 0;
    public static final int IN_ALBUMSETPAGE = 1;
    public static final int IN_ALBUMPAGE = 2;
    public static final int IN_SELECTMODE= 3;
    public static final int IN_STORYPAGE= 4;
    public static final int IN_ADD_STORYPAGE= 5;


    public int getmCurrentState() {
        return mCurrentState;
    }

    @Override
    public void showNavi(int state) {
        mCurrentState = state;
        switch (state) {
            case IN_PHOTOPAGE:// in photopage
                setViewPagerVisible(View.INVISIBLE);
                setFreemeActionbarContainerVisible(View.INVISIBLE);
                setmFreemeBottomSelectedViewVisible(View.INVISIBLE);
                break;
            case IN_ALBUMSETPAGE:// in albumsetpage
                setViewPagerVisible(View.VISIBLE);
                setFreemeActionbarContainerVisible(View.INVISIBLE);
                setmFreemeBottomSelectedViewVisible(View.INVISIBLE);
                break;
            case IN_ALBUMPAGE:
                setViewPagerVisible(View.INVISIBLE);
                setFreemeActionbarContainerVisible(View.VISIBLE);
                setmFreemeBottomSelectedViewVisible(View.INVISIBLE);
                break;

            case IN_SELECTMODE:
                setViewPagerVisible(View.INVISIBLE);
                setFreemeActionbarContainerVisible(View.INVISIBLE);
                setmFreemeBottomSelectedViewVisible(View.VISIBLE);
                break;
            case IN_STORYPAGE:
                setViewPagerVisible(View.INVISIBLE);
                setFreemeActionbarContainerVisible(View.VISIBLE);
                setmFreemeBottomSelectedViewVisible(View.INVISIBLE);
                mFreemeActionBarContainer.setBackgroundColor(Color.TRANSPARENT);
                break;
            case IN_ADD_STORYPAGE:
                setViewPagerVisible(View.INVISIBLE);
                setFreemeActionbarContainerVisible(View.VISIBLE);
                setmFreemeBottomSelectedViewVisible(View.INVISIBLE);
                break;
            default:
                break;
        }

    }

    private void setmFreemeBottomSelectedViewVisible(int visibility) {
        if (mFreemeBottomSelectedView != null) {
            mFreemeBottomSelectedView.setVisibility(visibility);
        }
    }


    private void setFreemeActionbarContainerVisible(int visibility) {
        if (mFreemeActionBarContainer != null) {
            mFreemeActionBarContainer.setVisibility(visibility);
        }
    }

    private void setViewPagerVisible(int visibility) {
        if (mViewPagerTabs != null) {
            mViewPagerTabs.setVisibility(visibility);
        }
    }


    public interface onGestureSensorListener {
        void onGestureSensorChanged(SensorEvent event);
    }
    private void initGestureSensor() {
        if (mSensorManager == null) {
            mSensorManager = (SensorManager)getAndroidContext()
                    .getSystemService(Context.SENSOR_SERVICE);
        }

        //frameworks/base/core/java/android/hardware/Sensor.TYPE_GESTURE = 50
        mGestureSensor =mSensorManager.getDefaultSensor(50);
        mHasGestureSensor = (mGestureSensor != null);
        mHasRegister = false;
    }

    private void registerGestureSensorListener() {
        try {
            mSlideByGestureEnable = GestureSensorManger.isGestureSensorEnable(this, GestureSensorManger.FREEME_GESTURE_GALLERY_SLIDE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(BuildConfig.SUPPORT_LAGACY_FREEMEOS_PLATFORM){
            mSlideByGestureEnable = SystemPropertiesProxy.getBooleanbit(getContentResolver(),
                    "tyd_gesture_sets",
                    1 << 0,
                    false);
        }
        if (mSlideByGestureEnable && mHasGestureSensor
                && !mHasRegister) {
            mSensorManager.registerListener(mGestureListener, mGestureSensor,
                    SensorManager.SENSOR_DELAY_FASTEST);
            mHasRegister = true;
        }
    }

    private void unregisterGestureSensorListener() {
        if (mSlideByGestureEnable && mHasGestureSensor
                && mHasRegister) {
            mSensorManager.unregisterListener(mGestureListener);
            mHasRegister = false;
        }
    }

    public void removeGestrueChangedListener(onGestureSensorListener listener) {
        synchronized (mGestureSyncObj) {
            if (listener == null
                    || (mGestureChangedListener != null && mGestureChangedListener == listener)) {
                unregisterGestureSensorListener();
                mGestureChangedListener = null;
            }
        }
    }

    public class TakingGestrueListener implements SensorEventListener {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            synchronized (mGestureSyncObj) {
                if (mGestureChangedListener != null) {
                    if(!isFastMultipleClick())
                    mGestureChangedListener.onGestureSensorChanged(event);
                }
            }
        }
    }

    private  long lastClickTime = -1;

    public  boolean isFastMultipleClick() {
        long time = System.currentTimeMillis();
        long timeD = time - lastClickTime;
        if (timeD > 0 && timeD <= 50) {
            return true;
        }
        lastClickTime = time;
        return false;
    }

    private NavigationWidgetManager mNavigationWidgetManager;

    public synchronized NavigationWidgetManager getNavigationWidgetManager() {
        if (mNavigationWidgetManager == null) {
            mNavigationWidgetManager = new NavigationWidgetManager(new State() {
                @Override
                public void onEnterState() {

                }

                @Override
                public void observe() {

                }
            });
        }
        return mNavigationWidgetManager;
    }

    public void setTopbarBackgroundColor(int id) {
        if (mFreemeActionBarContainer != null) {
            mFreemeActionBarContainer.setBackgroundColor(getResources().getColor(id));
        }
    }

}

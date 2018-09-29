/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
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

package com.android.gallery3d.app;

import android.annotation.TargetApi;
import android.app.ActionBar.OnMenuVisibilityListener;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateBeamUrisCallback;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.ShareActionProvider;
import android.widget.Toast;

import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.LocalImage;
import com.freeme.extern.PhotopageComments;
import com.freeme.gallery.R;
import com.android.gallery3d.data.CameraShortcutImage;
import com.android.gallery3d.data.ComboAlbum;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.EmptyAlbumImage;
import com.android.gallery3d.data.FilterDeleteSet;
import com.android.gallery3d.data.FilterSource;
import com.android.gallery3d.data.MediaDetails;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaObject.PanoramaSupportCallback;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.data.SecureAlbum;
import com.android.gallery3d.data.SecureSource;
import com.android.gallery3d.data.SnailAlbum;
import com.android.gallery3d.data.SnailItem;
import com.android.gallery3d.data.SnailSource;
import com.freeme.gallery.app.AbstractGalleryActivity;
import com.freeme.gallery.app.GalleryActivity;
import com.freeme.gallery.app.TrimVideo;
import com.freeme.gallery.filtershow.FilterShowActivity;
import com.freeme.gallery.filtershow.crop.CropActivity;
import com.android.gallery3d.picasasource.PicasaSource;
import com.android.gallery3d.ui.DetailsHelper;
import com.android.gallery3d.ui.DetailsHelper.CloseListener;
import com.android.gallery3d.ui.DetailsHelper.DetailsSource;
import com.android.gallery3d.ui.GLRootView;
import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.ui.MenuExecutor;
import com.android.gallery3d.ui.PhotoView;
import com.android.gallery3d.ui.SelectionManager;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.UsageStatistics;
import com.android.gallery3d.common.ApiHelper;
import com.freeme.ui.manager.State;
import com.freeme.utils.FrameworkSupportUtils;
import com.freeme.utils.FreemeCustomUtils;
import com.freeme.utils.FreemeUtils;
import java.io.File;
import java.util.ArrayList;
import com.android.gallery3d.ui.DetailsHelper.OpenListener;
import com.sprd.gallery3d.refocus.RefocusPhotoEditActivity;

public abstract class PhotoPage extends ActivityState implements
        PhotoView.Listener, AppBridge.Server, ShareActionProvider.OnShareTargetSelectedListener,
        PhotoPageBottomControls.Delegate, GalleryActionBar.OnAlbumModeSelectedListener ,
        PhotoVoiceProgress.TimeListener, State{
    public static final  int                      REQUEST_PLAY_VIDEO                = 5;
    public static final  int                      REQUEST_TRIM                      = 6;
    public static final  String                   KEY_MEDIA_SET_PATH                = "media-set-path";
    public static final  String                   KEY_MEDIA_ITEM_PATH               = "media-item-path";
    public static final  String                   KEY_INDEX_HINT                    = "index-hint";
    public static final  String                   KEY_OPEN_ANIMATION_RECT           = "open-animation-rect";
    public static final  String                   KEY_APP_BRIDGE                    = "app-bridge";
    public static final  String                   KEY_TREAT_BACK_AS_UP              = "treat-back-as-up";
    public static final  String                   KEY_START_IN_FILMSTRIP            = "start-in-filmstrip";
    public static final  String                   KEY_RETURN_INDEX_HINT             = "return-index-hint";
    public static final  String                   KEY_SHOW_WHEN_LOCKED              = "show_when_locked";
    public static final  String                   KEY_IN_CAMERA_ROLL                = "in_camera_roll";
    public static final  String                   KEY_READONLY                      = "read-only";
    public static final  String                   KEY_ALBUMPAGE_TRANSITION          = "albumpage-transition";
    public static final  int                      MSG_ALBUMPAGE_NONE                = 0;
    public static final  int                      MSG_ALBUMPAGE_STARTED             = 1;
    public static final  int                      MSG_ALBUMPAGE_RESUMED             = 2;
    public static final  int                      MSG_ALBUMPAGE_PICKED              = 4;
    public static final  String                   ACTION_NEXTGEN_EDIT               = "action_nextgen_edit";
    public static final  String                   ACTION_SIMPLE_EDIT                = "action_simple_edit";
    public static final int DELAY_MILLIS = 1000;
    private static final String                   TAG                               = "PhotoPage";
    private static final int                      MSG_HIDE_BARS                     = 1;
    private static final int                      MSG_ON_FULL_SCREEN_CHANGED        = 4;
    //*/
    private static final int                      MSG_UPDATE_ACTION_BAR             = 5;
    private static final int                      MSG_UNFREEZE_GLROOT               = 6;
    private static final int                      MSG_WANT_BARS                     = 7;
    private static final int                      MSG_REFRESH_BOTTOM_CONTROLS       = 8;
    private static final int                      MSG_ON_CAMERA_CENTER              = 9;
    private static final int                      MSG_ON_PICTURE_CENTER             = 10;
    private static final int                      MSG_REFRESH_IMAGE                 = 11;
    private static final int                      MSG_UPDATE_PHOTO_UI               = 12;
    private static final int                      MSG_UPDATE_DEFERRED               = 14;
    private static final int                      MSG_UPDATE_SHARE_URI              = 15;
    private static final int                      MSG_UPDATE_PANORAMA_UI            = 16;
    private static final int                      HIDE_BARS_TIMEOUT                 = 3500;
    private static final int                      UNFREEZE_GLROOT_TIMEOUT           = 250;
    private static final int                      REQUEST_SLIDESHOW                 = 1;
    private static final int                      REQUEST_CROP                      = 2;
    private static final int                      REQUEST_CROP_PICASA               = 3;
    private static final int                      REQUEST_EDIT                      = 4;
    //*/ Added by droi Linguanrong for bigmodel, 16-2-22
    private static final int                      REQUEST_BIGMODE                   = 7;
    private static final long                     CAMERA_SWITCH_CUTOFF_THRESHOLD_MS = 300;
    private static final long                     DEFERRED_UPDATE_MS                = 250;
    private final        MyMenuVisibilityListener mMenuVisibilityListener           =
            new MyMenuVisibilityListener();
    boolean           isCommentvisible = false;
    PhotopageComments mBottomText      = null;
    private GalleryApp       mApplication;
    private SelectionManager mSelectionManager;
    private PhotoView        mPhotoView;
    private PhotoPage.Model  mModel;
    private DetailsHelper    mDetailsHelper;
    private boolean          mShowDetails;
    // mMediaSet could be null if there is no KEY_MEDIA_SET_PATH supplied.
    // E.g., viewing a photo in gmail attachment
    private FilterDeleteSet  mMediaSet;
    // The mediaset used by camera launched from secure lock screen.
    private SecureAlbum      mSecureAlbum;
    private int mCurrentIndex = 0;
    private Handler mHandler;
    private          boolean mShowBars         = true;
    private boolean mDialogOpen = false;

    private volatile boolean mActionBarAllowed = true;
    private GalleryActionBar mActionBar;
    private final GLView mRootPane = new GLView() {
        @Override
        protected void onLayout(
                boolean changed, int left, int top, int right, int bottom) {
            mPhotoView.layout(0, 0, right - left, bottom - top);
            if (mShowDetails) {
                mDetailsHelper.layout(left, mActionBar.getHeight(), right, bottom);
            }
        }
    };
    private boolean                 mIsMenuVisible;
    private boolean                 mHaveImageEditor;
    private PhotoPageBottomControls mBottomControls;
    private MediaItem mCurrentPhoto = null;
    private MenuExecutor mMenuExecutor;
    private boolean      mIsActive;
    private boolean      mShowSpinner;
    private String       mSetPathString;
    // This is the original mSetPathString before adding the camera preview item.
    private boolean mReadOnlyView = false;
    private String             mOriginalSetPathString;
    private AppBridge          mAppBridge;
    private SnailItem          mScreenNailItem;
    private SnailAlbum         mScreenNailSet;
    private OrientationManager mOrientationManager;
    private boolean            mTreatBackAsUp;
    private boolean            mStartInFilmstrip;
    private boolean mHasCameraScreennailOrPlaceholder = false;
    private boolean mRecenterCameraOnResume           = false;
    private long    mCameraSwitchCutoff               = 0;
    private boolean mSkipUpdateCurrentPhoto           = false;
    private boolean mDeferredUpdateWaiting            = false;
    private long    mDeferUpdateUntil                 = Long.MAX_VALUE;
    // The item that is deleted (but it can still be undeleted before commiting)
    private Path    mDeletePath;
    private boolean mDeleteIsFocus;  // whether the deleted item was in focus
    private Uri[] mNfcPushUris     = new Uri[1];
    private int   mLastSystemUiVis = 0;
    private SharedPreferences        mSharedPref;
    //*/ 
    private SharedPreferences.Editor mEditor;
    private boolean isEnbled = false;

    private final PanoramaSupportCallback mUpdatePanoramaMenuItemsCallback = new PanoramaSupportCallback() {
        @Override
        public void panoramaInfoAvailable(MediaObject mediaObject, boolean isPanorama,
                boolean isPanorama360) {
            if (mediaObject == mCurrentPhoto) {
                mHandler.obtainMessage(MSG_UPDATE_PANORAMA_UI, isPanorama360 ? 1 : 0, 0,
                        mediaObject).sendToTarget();
            }
        }
    };

    private final PanoramaSupportCallback mRefreshBottomControlsCallback = new PanoramaSupportCallback() {
        @Override
        public void panoramaInfoAvailable(MediaObject mediaObject, boolean isPanorama,
                boolean isPanorama360) {
            if (mediaObject == mCurrentPhoto) {
                mHandler.obtainMessage(MSG_REFRESH_BOTTOM_CONTROLS, isPanorama ? 1 : 0, isPanorama360 ? 1 : 0,
                        mediaObject).sendToTarget();
            }
        }
    };

    private final PanoramaSupportCallback mUpdateShareURICallback = new PanoramaSupportCallback() {
        @Override
        public void panoramaInfoAvailable(MediaObject mediaObject, boolean isPanorama,
                boolean isPanorama360) {
            if (mediaObject == mCurrentPhoto) {
                mHandler.obtainMessage(MSG_UPDATE_SHARE_URI, isPanorama360 ? 1 : 0, 0, mediaObject)
                        .sendToTarget();
            }
        }
    };


    private boolean                       isBlockGuideShow       = false;
    //*/ Added by droi Linguanrong for camera shortcut, 16-2-29
    private boolean                       showCameraShortCut     = true;
    private boolean                       canShowCameraShortCut  = false;
    private MenuExecutor.ProgressListener mConfirmDialogListener =
            new MenuExecutor.ProgressListener() {
                @Override
                public void onConfirmDialogShown() {
                    mHandler.removeMessages(MSG_HIDE_BARS);
                }

                @Override
                public void onConfirmDialogDismissed(boolean confirmed) {
                    refreshHidingMessage();
                }

                @Override
                public void onProgressStart() {
                }

                @Override
                public void onProgressUpdate(int index) {
                }

                @Override
                public void onProgressComplete(int result) {
                }
            };
    //*/ Added by Linguanrong for photopage bottom controls, 2014-9-17
    private MyDetailsSource mMyDetailsSource;
    private Path itemPath;

    private static Intent createShareIntent(MediaObject mediaObject) {
        int type = mediaObject.getMediaType();
        return new Intent(Intent.ACTION_SEND)
                .setType(MenuExecutor.getMimeType(type))
                .putExtra(Intent.EXTRA_STREAM, FreemeUtils.convertUri(mediaObject.getContentUri()))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    private static Intent createSharePanoramaIntent(Uri contentUri) {
        return new Intent(Intent.ACTION_SEND)
                .setType(GalleryUtils.MIME_TYPE_PANORAMA360)
                .putExtra(Intent.EXTRA_STREAM, contentUri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    @Override
    public boolean canDisplayBottomControls() {
        //*/ Added by Linguanrong for invisible actionbar & menus when no images, 2014-11-20
        if (mCurrentPhoto == null
                || mCurrentPhoto instanceof EmptyAlbumImage
                || mCurrentPhoto instanceof CameraShortcutImage) {
            return false;
        }
        //*/


        //*/ Added by Linguanrong for photopage bottom controls, 2014-9-17

        return mShowBars && canShowBars() && mIsActive && !mPhotoView.getFilmMode()
                && (mOriginalSetPathString != null);
        //*/
    }

    @Override
    public boolean canDisplayBottomControl(int control, View view) {
        if (mCurrentPhoto == null) {
            return false;
        }
        if (isCommentvisible && mBottomText != null && !mPhotoView.getFilmMode()) {
//            mBottomText.setvisible(false);
        }
        switch (control) {
            case R.id.photopage_back_text:
                if(mIsStartFromTimeshaft) {
                    mBottomControls.getMenuBack().setText(R.string.tab_photos);
                } else {
                    mBottomControls.getMenuBack().setText(mMediaSet.getName());
                }
                return true;
            case R.id.photopage_bottom_navigation_bar:
                if ((mCurrentPhoto.getSupportedOperations() & MediaObject.SUPPORT_EDIT) != 0) {
                    mBottomControls.setIsEditable(true, false);
                } else {
                    mBottomControls.setIsEditable(false, false);
                }
                return true;
            case R.id.photopage_bottom_control_edit:
                mBottomControls.getMenuEdit().setEnabled(
                    (mCurrentPhoto.getSupportedOperations() & MediaObject.SUPPORT_EDIT) != 0);
                return true;
            case R.id.photopage_bottom_control_blockbuster:
                mBottomControls.getMenuBlock().setEnabled(
                    (mCurrentPhoto.getSupportedOperations() & MediaObject.SUPPORT_EDIT) != 0);
                return true;
            case R.id.photo_voice_icon:
//                if (mCurrentPhoto instanceof LocalImage && FrameworkSupportUtils.isSupportVoiceImage()) {
//                    LocalImage localImage = (LocalImage) mCurrentPhoto;
//                    String photoVoice = localImage.getPhotoVoice();
//                    Log.d(TAG, "updateCurrentPhoto   photoVoice = " + photoVoice);
//                    return photoVoice != null ? true : false;
//                }
                return false;

            case R.id.photo_voice_progress:
//                if (mCurrentPhoto instanceof LocalImage) {
//                    LocalImage localImage = (LocalImage) mCurrentPhoto;
//                    String photoVoice = localImage.getPhotoVoice();
//                    if (photoVoice != null) {
//                        mPhotoVoiceProgress = (PhotoVoiceProgress) view;
//                    }
//                }
                return false;
            case R.id.photo_refocus_icon:
                if (mCurrentPhoto instanceof LocalImage && FrameworkSupportUtils.isSupportRefocusImage()) {
                    LocalImage localImage = (LocalImage) mCurrentPhoto;
                    boolean isRefocus = localImage.getMimeType().startsWith("refocusImage/");
                    Log.d(TAG, "updateCurrentPhoto   isRefocusImage = " + isRefocus);
                    return isRefocus;
                }
                if (mCurrentPhoto instanceof LocalImage ) {
                    LocalImage localImage = (LocalImage) mCurrentPhoto;
                    boolean isRefocus = (localImage.getMediaType() == LocalImage.MEDIA_TYPE_IMAGE_REFOCUS);
                    return isRefocus;
                }
                return false;
            default:
                return true;
        }
    }

    public static final int TAB0= 0;
    public static final int TAB1 = 1;
    public static final int TAB2 = 2;
    public static final int TAB3 = 3;
    public static final int TAB4 = 4;
    public static final int TAB5 = 5;

    @Override
    public void onBottomControlClicked(int control) {
        refreshHidingMessage();
        MediaItem current = mModel.getMediaItem(0);


        int currentIndex = mModel.getCurrentIndex();
        Path path = current.getPath();

        DataManager manager = mActivity.getDataManager();
        String confirmMsg = null;
        int freemeControl = 0;
        switch (control) {
            case TAB0:
                freemeControl = R.id.photopage_bottom_control_edit;
                if (!mBottomControls.getIsEditable()) {
                    setOrientaionBeforeShare();
                    return;
                }
                break;
            case TAB1:
                if (!mBottomControls.getIsEditable()) {
                    freemeControl = R.id.photopage_bottom_control_delete;
                } else {
                    freemeControl = R.id.photopage_bottom_control_share;
                }
                break;
            case TAB2:
                freemeControl = R.id.photopage_bottom_control_delete;
                break;
            case TAB3:
                mSelectionManager.deSelectAll();
                mSelectionManager.toggle(path);
                mOrientationManager.lockOrientation(true);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        actionSetas();
                    }
                }, mOrientationManager.getDisplayRotation() == Surface.ROTATION_0 ? 0 : DELAY_MILLIS);

                return;
            case TAB4:
                freemeControl = R.id.photopage_bottom_control_blockbuster;
                break;
            case TAB5:
                if (!isCommentvisible) {
                    isCommentvisible = true;
                    if (isCommentvisible && mBottomText != null && !isEnbled) {
                        hideBars();
                    }

                    saveOfflineNewStatus("visible", isCommentvisible);
                } else {
                    isCommentvisible = false;
                    saveOfflineNewStatus("visible", isCommentvisible);
                }
                return;
            default:
                freemeControl = control;
                break;
        }
        switch (freemeControl) {
            case R.id.photopage_back_image:
            case R.id.photopage_back_text:
                onBackPressed();
                return;
            case R.id.photopage_details:
                if (mShowDetails) {
                    hideDetails();
                } else {
                    showDetails();
                }
                return;

            case R.id.photopage_bottom_control_edit:
                launchPhotoEditor();
                return;


            case R.id.photopage_bottom_control_blockbuster:

                jumptolarge();
                return;
            case R.id.photopage_bottom_control_share:
                setOrientaionBeforeShare();

                //*/ Added by tyd Linguanrong for statistic, 15-12-18
//                StatisticUtil.generateStatisticInfo(mActivity, StatisticData.OPTION_SHARE);
                //*/

                // for baas analytics
//                DroiAnalytics.onEvent(mActivity, StatisticData.OPTION_SHARE);
                return;

            case R.id.photopage_bottom_control_delete:
                confirmMsg = mActivity.getResources().getQuantityString(
                        R.plurals.delete_selection, 1);
                mSelectionManager.deSelectAll();
                mSelectionManager.toggle(path);
                mMenuExecutor.onMenuClicked(R.id.action_delete, confirmMsg, mConfirmDialogListener);
                return;
            case R.id.photopage_bottom_control_slideshow:
//                onItemSelected(menu.findItem(R.id.action_slideshow));
                return;
      /* SPRD: Add for bug535110 new feature,  support play audio picture @{ */
            case R.id.photo_voice_icon:
//                if (mCurrentPhoto instanceof LocalImage) {
//                    LocalImage localImage = (LocalImage) mCurrentPhoto;
//                    String photoVoice = localImage.getPhotoVoice();
//                    Log.d(TAG, "updateCurrentPhoto   photoVoice = " + photoVoice);
//                    playPhotoVoice(photoVoice);
//                }
                return;
            case R.id.photo_refocus_icon:
                if (mCurrentPhoto instanceof LocalImage) {
                    LocalImage localImage = (LocalImage) mCurrentPhoto;
                    boolean isRefocus = localImage.getMimeType().startsWith("refocusImage/");
                    Log.d(TAG, "updateCurrentPhoto   isRefocusImage = " + isRefocus);
                    if (isRefocus) {
                        startRefocusActivity(localImage.getPlayUri(), localImage);
                        return;
                    }
                    boolean isbstRefocus = (localImage.getMediaType() == LocalImage.MEDIA_TYPE_IMAGE_REFOCUS);
                    if (isbstRefocus) {
                        startBstRefocusActivity();
                        return;
                    }
                }
                return;
            default:
                return;
        }
    }

    private void setOrientaionBeforeShare() {
        mOrientationManager.lockOrientation(true);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                actionShare();
            }
        }, mOrientationManager.getDisplayRotation() == Surface.ROTATION_0 ? 0 : DELAY_MILLIS);
    }

    @Override
    public void refreshBottomControlsWhenReady() {
        if (mBottomControls == null) {
            return;
        }

        //*/ Modified by Linguanrong for photopage bottom controls, 2014-9-17
        mHandler.obtainMessage(MSG_REFRESH_BOTTOM_CONTROLS, 0, 0, mCurrentPhoto).sendToTarget();
        /*/
        MediaObject currentPhoto = mCurrentPhoto;
        if (currentPhoto == null) {
            mHandler.obtainMessage(MSG_REFRESH_BOTTOM_CONTROLS, 0, 0, currentPhoto).sendToTarget();
        } else {
            currentPhoto.getPanoramaSupport(mRefreshBottomControlsCallback);
        }
        //*/
    }

    private boolean canShowBars() {
        //*/ Added by Linguanrong for invisible actionbar & menus when no images, 2014-11-20
        if (mCurrentPhoto == null
                || mCurrentPhoto instanceof EmptyAlbumImage
                || mCurrentPhoto instanceof CameraShortcutImage) {
            return false;
        }
        //*/
        // No bars if we are showing camera preview.
        if (mAppBridge != null && mCurrentIndex == 0
                && !mPhotoView.getFilmMode()) return false;

        // No bars if it's not allowed.
        if (!mActionBarAllowed) return false;

        Configuration config = mActivity.getResources().getConfiguration();
        return config.touchscreen != Configuration.TOUCHSCREEN_NOTOUCH;

    }
    //*/

    private void actionSetas() {
        Intent intent = getIntentBySingleSelectedPath(Intent.ACTION_ATTACH_DATA)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra("mimeType", intent.getType());
        Activity activity = mActivity;
        activity.startActivity(Intent.createChooser(
                intent, activity.getString(R.string.set_as)));
    }

    private void actionShare() {
        Intent shareIntent = createShareIntent(mCurrentPhoto);
        mActivity.startActivityForResult(FreemeCustomUtils.createCustomChooser(mActivity, shareIntent,
                mActivity.getResources().getString(R.string.share)), GalleryActivity.CHOOSER_REQUEST_CODE);
    }

    @TargetApi(ApiHelper.VERSION_CODES.JELLY_BEAN)
    private void setupNfcBeamPush() {
        if (!ApiHelper.HAS_SET_BEAM_PUSH_URIS) return;

        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(mActivity);
        if (adapter != null) {
            adapter.setBeamPushUris(null, mActivity);
            adapter.setBeamPushUrisCallback(new CreateBeamUrisCallback() {
                @Override
                public Uri[] createBeamUris(NfcEvent event) {
                    return mNfcPushUris;
                }
            }, mActivity);
        }
    }

    private void setNfcBeamPushUri(Uri uri) {
        mNfcPushUris[0] = uri;
    }

    private void launchTinyPlanet() {
        // Deep link into tiny planet
        MediaItem current = mModel.getMediaItem(0);
        Intent intent = new Intent(FilterShowActivity.TINY_PLANET_ACTION);
        intent.setClass(mActivity, FilterShowActivity.class);
        intent.setDataAndType(current.getContentUri(), current.getMimeType())
                .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(FilterShowActivity.LAUNCH_FULLSCREEN,
                mActivity.isFullscreen());
        mActivity.startActivityForResult(intent, REQUEST_EDIT);
        overrideTransitionToEditor();
    }

    private void overrideTransitionToEditor() {
        mActivity.overridePendingTransition(android.R.anim.fade_in,
                android.R.anim.fade_out);
    }

    private void launchCamera() {
        mRecenterCameraOnResume = true;
        GalleryUtils.startCameraActivity(mActivity);
    }

    private void launchPhotoEditor() {
        MediaItem current = mModel.getMediaItem(0);
        if (current == null || (current.getSupportedOperations()
                & MediaObject.SUPPORT_EDIT) == 0) {
            return;
        }

        Intent intent = new Intent(ACTION_NEXTGEN_EDIT);
        //intent.setClassName("com.freeme.gallery", "FilterShowActivity");
        //intent.setComponent(new ComponentName("com.freeme.gallery", "FilterShowActivity"));
        intent.setDataAndType(current.getContentUri(), current.getMimeType())
                .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (mActivity.getPackageManager()
                .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).size() == 0) {
            intent.setAction(Intent.ACTION_EDIT);
        }
        intent.putExtra(FilterShowActivity.LAUNCH_FULLSCREEN,
                mActivity.isFullscreen());
        mActivity.startActivityForResult(Intent.createChooser(intent, null),
                REQUEST_EDIT);
        overrideTransitionToEditor();
    }

    private void launchSimpleEditor() {
        MediaItem current = mModel.getMediaItem(0);
        if (current == null || (current.getSupportedOperations()
                & MediaObject.SUPPORT_EDIT) == 0) {
            return;
        }

        Intent intent = new Intent(ACTION_SIMPLE_EDIT);

        intent.setDataAndType(current.getContentUri(), current.getMimeType())
                .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (mActivity.getPackageManager()
                .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).size() == 0) {
            intent.setAction(Intent.ACTION_EDIT);
        }
        intent.putExtra(FilterShowActivity.LAUNCH_FULLSCREEN,
                mActivity.isFullscreen());
        mActivity.startActivityForResult(Intent.createChooser(intent, null),
                REQUEST_EDIT);
        overrideTransitionToEditor();
    }

    private void requestDeferredUpdate() {
        mDeferUpdateUntil = SystemClock.uptimeMillis() + DEFERRED_UPDATE_MS;
        if (!mDeferredUpdateWaiting) {
            mDeferredUpdateWaiting = true;
            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_DEFERRED, DEFERRED_UPDATE_MS);
        }
    }

    private void updateUIForCurrentPhoto() {
        if (mCurrentPhoto == null) return;

        // If by swiping or deletion the user ends up on an action item
        // and zoomed in, zoom out so that the context of the action is
        // more clear
        if ((mCurrentPhoto.getSupportedOperations() & MediaObject.SUPPORT_ACTION) != 0
                && !mPhotoView.getFilmMode()) {
            mPhotoView.setWantPictureCenterCallbacks(true);
        }

//        updateMenuOperations();
        refreshBottomControlsWhenReady();
        if (mShowDetails) {
            mDetailsHelper.reloadDetails();
        }
        if ((mSecureAlbum == null)
                && (mCurrentPhoto.getSupportedOperations() & MediaItem.SUPPORT_SHARE) != 0) {
            mCurrentPhoto.getPanoramaSupport(mUpdateShareURICallback);
        } else {
            if (mBottomText != null) {
//                mBottomText.setvisible(false);
            }
        }
    }

    private void updateCurrentPhoto(MediaItem photo) {
        if (mCurrentPhoto == photo) return;
        // Add for bug535110 new feature,  support play audio picture
        playPhotoVoice(null);
        mCurrentPhoto = photo;
        if (mPhotoView.getFilmMode()) {
            requestDeferredUpdate();
        } else {
            updateUIForCurrentPhoto();
        }

        //*/ Added by droi Linguanrong for comments, 16-2-1
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mBottomText != null && mCurrentPhoto != null) {
                    mBottomText.updateText(mCurrentPhoto.getPath(), mCurrentPhoto.getMediaType());
                } else {
                    RelativeLayout galleryRoot = (RelativeLayout) mActivity.findViewById(R.id.gallery_root);
                    mBottomText = new PhotopageComments(mActivity, galleryRoot,
                            mCurrentPhoto.getPath(), mCurrentPhoto.getMediaType());
                    mBottomText.setvisible(false);
                }

                if (readOfflineNewStatus("visible", false) && mBottomText != null && !mPhotoView.getFilmMode()) {
                    mBottomText.setvisible(!mShowBars);
                }
            }
        });
        //end
    }

    private void showBars() {
        if (mShowBars) return;
        mShowBars = true;
        mOrientationManager.unlockOrientation();
//        mActionBar.show();
        //*/ Added by droi Linguanrong for comments, 16-2-1
        if ( mBottomText != null) {
            mBottomText.setvisible(false);
        }
        //end
        mActivity.getGLRoot().setLightsOutMode(false);
        refreshHidingMessage();
        refreshBottomControlsWhenReady();
    }

    private void hideBars() {
        //*/ Added by droi Linguanrong for comments, 16-2-1
        if (isCommentvisible && mBottomText != null
                && mCurrentPhoto != null
                && !mPhotoView.getFilmMode()) {
            mBottomText.setvisible(true);
        }
        //end
        if (!mShowBars) return;
        if(mDialogOpen) return;
        mShowBars = false;
//        mActionBar.hide();
        mActivity.getGLRoot().setLightsOutMode(true);
        mHandler.removeMessages(MSG_HIDE_BARS);
        refreshBottomControlsWhenReady();
    }

    private void wantBars() {
//        if (canShowBars()) showBars();
    }

    //////////////////////////////////////////////////////////////////////////
    //  Action Bar show/hide management
    //////////////////////////////////////////////////////////////////////////

    private void toggleBars() {
        if (mShowBars) {
            hideBars();
        } else {
            if (canShowBars()) showBars();
        }
    }

    private void updateBars() {
        if (!canShowBars()) {
            hideBars();
        }
        //*/ Added by droi Linguanrong for freeme gallery, 16-1-14
//        updateActionBarTitle();
        //*/
    }

    @Override
    protected void onBackPressed() {
        showBars();
        //*/ Added by droi Linguanrong for comments, 16-2-1
        if (mCurrentPhoto != null && mBottomText != null) {
            mBottomText.setvisible(false);
        }
        //end
        if (mShowDetails) {
            hideDetails();
        } else if (mAppBridge == null || !switchWithCaptureAnimation(-1)) {
            // We are leaving this page. Set the result now.
            setResult();
            if (mStartInFilmstrip && !mPhotoView.getFilmMode()) {
//                mPhotoView.setFilmMode(true);
                super.onBackPressed();
            } else if (mTreatBackAsUp) {
//                onUpPressed()
                mActivity.finish();
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    protected void onStateResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED) {
            // This is a reset, not a canceled
            return;
        }
        mRecenterCameraOnResume = false;
        switch (requestCode) {
            case REQUEST_EDIT:
                mActivity.getDataManager().broadcastUpdatePicture();
                setCurrentPhotoByIntent(data);
                break;

            case REQUEST_CROP:
                if (resultCode == Activity.RESULT_OK) {
                    setCurrentPhotoByIntent(data);
                }
                break;

            case REQUEST_CROP_PICASA: {
//                if (resultCode == Activity.RESULT_OK) {
//                    Context context = mActivity.getAndroidContext();
//                    String message = context.getString(R.string.crop_saved,
//                            context.getString(R.string.folder_edited_online_photos));
//                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
//                }
                break;
            }

            case REQUEST_SLIDESHOW: {
                if (data == null) break;
                String path = data.getStringExtra(SlideshowPage.KEY_ITEM_PATH);
                int index = data.getIntExtra(SlideshowPage.KEY_PHOTO_INDEX, 0);
                if (path != null) {
                    mModel.setCurrentPhoto(Path.fromString(path), index);
                }
                break;
            }

            case REQUEST_BIGMODE:
                if (resultCode == Activity.RESULT_OK) {
                    setCurrentPhotoByIntentEx(data);
                }
                break;
        }
    }

    @Override
    protected int getBackgroundColorId() {
        return R.color.photo_background;
    }

    @Override
    public void onCreate(Bundle data, Bundle restoreState) {
        mActionBar = mActivity.getGalleryActionBar();
        mActionBar.hide();
        super.onCreate(data, restoreState);
        mActivity.getNavigationWidgetManager().changeStateTo(this);
        mSelectionManager = new SelectionManager(mActivity, false);
        mMenuExecutor = new MenuExecutor(mActivity, mSelectionManager);
        //*/ Added by Tyd Linguanrong for Gallery new style, 2014-04-14
        mMyDetailsSource = new MyDetailsSource();
        //*/
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(mActivity);
        mEditor = mSharedPref.edit();

        mPhotoView = new PhotoView(mActivity);
        mPhotoView.setListener(this);
        mRootPane.addComponent(mPhotoView);
        mApplication = (GalleryApp) mActivity.getApplication();
        mOrientationManager = mActivity.getOrientationManager();
        mActivity.getGLRoot().setOrientationSource(mOrientationManager);
        // Add for bug535110 new feature,  support play audio picture
        mAudioManager = (AudioManager) mActivity.getSystemService(Context.AUDIO_SERVICE);
        mHandler = new SynchronizedHandler(mActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_HIDE_BARS: {
                        if (!isBlockGuideShow) {
                            hideBars();
                        }
                        break;
                    }
                    case MSG_REFRESH_BOTTOM_CONTROLS: {
                        if (mCurrentPhoto == message.obj && mBottomControls != null) {
                            /*/ Disabled by Linguanrong for photopage bottom controls, 2014-9-17
                            mIsPanorama = message.arg1 == 1;
                            mIsPanorama360 = message.arg2 == 1;
                            //*/
                            mBottomControls.refresh();
                        }
                        break;
                    }
                    case MSG_ON_FULL_SCREEN_CHANGED: {
                        if (mAppBridge != null) {
                            mAppBridge.onFullScreenChanged(message.arg1 == 1);
                        }
                        break;
                    }
                    case MSG_UPDATE_ACTION_BAR: {
                        updateBars();
                        break;
                    }
                    case MSG_WANT_BARS: {
                        wantBars();
                        break;
                    }
                    case MSG_UNFREEZE_GLROOT: {
                        mActivity.getGLRoot().unfreeze();
                        break;
                    }
                    case MSG_UPDATE_DEFERRED: {
                        long nextUpdate = mDeferUpdateUntil - SystemClock.uptimeMillis();
                        if (nextUpdate <= 0) {
                            mDeferredUpdateWaiting = false;
                            updateUIForCurrentPhoto();
                        } else {
                            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_DEFERRED, nextUpdate);
                        }
                        break;
                    }
                    case MSG_ON_CAMERA_CENTER: {
                        mSkipUpdateCurrentPhoto = false;
                        boolean stayedOnCamera = false;
                        if (!mPhotoView.getFilmMode()) {
                            stayedOnCamera = true;
                        } else if (SystemClock.uptimeMillis() < mCameraSwitchCutoff &&
                                mMediaSet.getMediaItemCount() > 1) {
                            mPhotoView.switchToImage(1);
                        } else {
                            if (mAppBridge != null) mPhotoView.setFilmMode(false);
                            stayedOnCamera = true;
                        }

                        if (stayedOnCamera) {
                            //*/ Modified by droi Linguanrong for freeme gallery, 16-2-24
                            updateBars();
                             /// M: [BUG.MODIFY] getMediaItem(0) may be null, fix JE @{
                                /*updateCurrentPhoto(mModel.getMediaItem(0));*/
                                MediaItem photo = mModel.getMediaItem(0);
                                if (photo != null) {
                                    updateCurrentPhoto(photo);
                                }
                             /// @}
                            /*/
                            if (mAppBridge == null && mMediaSet.getTotalMediaItemCount() > 1) {
                                launchCamera();
                                /* We got here by swiping from photo 1 to the
                                   placeholder, so make it be the thing that
                                   is in focus when the user presses back from
                                   the camera app *
                                mPhotoView.switchToImage(1);
                            } else {
                                updateBars();
                                updateCurrentPhoto(mModel.getMediaItem(0));
                            }
                            //*/
                        }
                        break;
                    }
                    case MSG_ON_PICTURE_CENTER: {
                        /// M: [BUG.MARK] @{
                        // Design change : get into page mode directly when from camera to gallery.
                        /*if (!mPhotoView.getFilmMode() && mCurrentPhoto != null
                             && (mCurrentPhoto.getSupportedOperations()
                                 & MediaObject.SUPPORT_ACTION) != 0) {
                         mPhotoView.setFilmMode(true);
                         }*/
                        /// @}
                        break;
                    }
                    case MSG_REFRESH_IMAGE: {
                        final MediaItem photo = mCurrentPhoto;
                        mCurrentPhoto = null;
                        updateCurrentPhoto(photo);
                        break;
                    }
                    case MSG_UPDATE_PHOTO_UI: {
                        updateUIForCurrentPhoto();
                        break;
                    }
                    case MSG_UPDATE_SHARE_URI: {
                        /// M: [BUG.ADD] @{
                        // never update share uri when PhotoPage is not active
                        if (!mIsActive) {
                            break;
                        }
                        /// @}
                        /// M: [BUG.MARK] @{
                        // No matter what message.obj is, we update share intent for current photo
                        /* if (mCurrentPhoto == message.obj) {*/
                        /// @}
                            boolean isPanorama360 = message.arg1 != 0;
                            Uri contentUri = mCurrentPhoto.getContentUri();
                            Intent panoramaIntent = null;
                            if (isPanorama360) {
                                panoramaIntent = createSharePanoramaIntent(contentUri);
                            }
                            Intent shareIntent = createShareIntent(mCurrentPhoto);

                            mActionBar.setShareIntents(panoramaIntent, shareIntent, PhotoPage.this);
                            setNfcBeamPushUri(contentUri);
                        /// M: [BUG.MARK] @{
                        // }
                        /// @}
                        break;
                    }
                    case MSG_UPDATE_PANORAMA_UI: {
                        if (mCurrentPhoto == message.obj) {
                            boolean isPanorama360 = message.arg1 != 0;
                            updatePanoramaUI(isPanorama360);
                        }
                        break;
                    }
                    default: throw new AssertionError(message.what);
                }
            }
        };

        mSetPathString = data.getString(KEY_MEDIA_SET_PATH);
        mReadOnlyView = data.getBoolean(KEY_READONLY);
        mIsStartFromTimeshaft = data.getBoolean(KEY_STRAT_FROM_TIMESHAFT);
        mOriginalSetPathString = mSetPathString;
        setupNfcBeamPush();
        String itemPathString = data.getString(KEY_MEDIA_ITEM_PATH);
        itemPath = itemPathString != null ?
                Path.fromString(data.getString(KEY_MEDIA_ITEM_PATH)) :
                null;
        mTreatBackAsUp = data.getBoolean(KEY_TREAT_BACK_AS_UP, false);
        mStartInFilmstrip = data.getBoolean(KEY_START_IN_FILMSTRIP, false);
        boolean inCameraRoll = data.getBoolean(KEY_IN_CAMERA_ROLL, false);
        mCurrentIndex = data.getInt(KEY_INDEX_HINT, 0);
        if (mSetPathString != null) {
            //*/ Modified by Linguanrong for gallery horizontal support, 2014-9-4
            mShowSpinner = false;//true;
            //*/
            mAppBridge = data.getParcelable(KEY_APP_BRIDGE);

            //*/ Added by droi Linguanrong for camera shortcut, 16-2-29
            canShowCameraShortCut = showCameraShortCut && inCameraRoll
                    && GalleryUtils.isCameraAvailable(mActivity);
            //*/

            if (mAppBridge != null) {
                mShowBars = false;
                mHasCameraScreennailOrPlaceholder = true;
                mAppBridge.setServer(this);

                // Get the ScreenNail from AppBridge and register it.
                int id = SnailSource.newId();
                Path screenNailSetPath = SnailSource.getSetPath(id);
                Path screenNailItemPath = SnailSource.getItemPath(id);
                mScreenNailSet = (SnailAlbum) mActivity.getDataManager()
                        .getMediaObject(screenNailSetPath);
                mScreenNailItem = (SnailItem) mActivity.getDataManager()
                        .getMediaObject(screenNailItemPath);
                mScreenNailItem.setScreenNail(mAppBridge.attachScreenNail());

                if (data.getBoolean(KEY_SHOW_WHEN_LOCKED, false)) {
                    // Set the flag to be on top of the lock screen.
                    mFlags |= FLAG_SHOW_WHEN_LOCKED;
                }
                // Don't display "empty album" action item for capture intents.
                if (!mSetPathString.equals("/local/all/0")) {
                    // Check if the path is a secure album.
                    if (SecureSource.isSecurePath(mSetPathString)) {
                        mSecureAlbum = (SecureAlbum) mActivity.getDataManager()
                                .getMediaSet(mSetPathString);
                        mShowSpinner = false;
                    }
                    mSetPathString = "/filter/empty/{"+mSetPathString+"}";
                }

                // Combine the original MediaSet with the one for ScreenNail
                // from AppBridge.
                mSetPathString = "/combo/item/{" + screenNailSetPath +
                        "," + mSetPathString + "}";

                // Start from the screen nail.
                itemPath = screenNailItemPath;
            } else if (canShowCameraShortCut) {
                mSetPathString = "/combo/item/{" + FilterSource.FILTER_CAMERA_SHORTCUT +
                        "," + mSetPathString + "}";
                mCurrentIndex++;
                mHasCameraScreennailOrPlaceholder = true;
            }

            MediaSet originalSet = mActivity.getDataManager()
                    .getMediaSet(mSetPathString);
            if (mHasCameraScreennailOrPlaceholder && originalSet instanceof ComboAlbum) {
                // Use the name of the camera album rather than the default
                // ComboAlbum behavior
                ((ComboAlbum) originalSet).useNameOfChild(1);
            }
            mSelectionManager.setSourceMediaSet(originalSet);
            mSetPathString = "/filter/delete/{" + mSetPathString + "}";
            mMediaSet = (FilterDeleteSet) mActivity.getDataManager()
                    .getMediaSet(mSetPathString);
            if (mMediaSet == null) {
                Log.w(TAG, "failed to restore " + mSetPathString);
            }
            if (itemPath == null) {
                int mediaItemCount = mMediaSet.getMediaItemCount();
                if (mediaItemCount > 0) {
                    if (mCurrentIndex >= mediaItemCount) mCurrentIndex = 0;
                    itemPath = mMediaSet.getMediaItem(mCurrentIndex, 1)
                        .get(0).getPath();
                } else {
                    // Bail out, PhotoPage can't load on an empty album
                    return;
                }
            }
            PhotoDataAdapter pda = new PhotoDataAdapter(
                    mActivity, mPhotoView, mMediaSet, itemPath, mCurrentIndex,
                    mAppBridge == null ? -1 : 0,
                    mAppBridge != null && mAppBridge.isPanorama(),
                    mAppBridge != null && mAppBridge.isStaticCamera());
            mModel = pda;
            mPhotoView.setModel(mModel);

            pda.setDataListener(new PhotoDataAdapter.DataListener() {

                @Override
                public void onPhotoChanged(int index, Path item) {
                    int oldIndex = mCurrentIndex;
                    mCurrentIndex = index;

                    if (mHasCameraScreennailOrPlaceholder) {
                        if (mCurrentIndex > 0) {
                            mSkipUpdateCurrentPhoto = false;
                        }

                        if (oldIndex == 0 && mCurrentIndex > 0
                                && !mPhotoView.getFilmMode()) {
                            //mPhotoView.setFilmMode(true);
                            if (mAppBridge != null) {
                                UsageStatistics.onEvent("CameraToFilmstrip",
                                        UsageStatistics.TRANSITION_SWIPE, null);
                            }
                        } else if (oldIndex == 2 && mCurrentIndex == 1) {
                            mCameraSwitchCutoff = SystemClock.uptimeMillis() +
                                    CAMERA_SWITCH_CUTOFF_THRESHOLD_MS;
                            mPhotoView.stopScrolling();
                        } else if (oldIndex >= 1 && mCurrentIndex == 0) {
                            mPhotoView.setWantPictureCenterCallbacks(true);
                            mSkipUpdateCurrentPhoto = true;
                        }
                    }
                    /*
                     * Added by TYD Theobald_Wu on 20130409 [begin] for
                     * invisible the unavailable options menu items if this is a
                     * camera shortcut image.
                     */
                    MediaItem mediaItem = mModel.getMediaItem(0);
                    if (mediaItem != null) {
                        int supported = mediaItem.getSupportedOperations();

                        if ((supported & MediaItem.SUPPORT_CAMERA_SHORTCUT) != 0) {
                            //*/ Modified by Tyd Linguanrong for update option menu, 2014-5-13
                            Menu menu = mActionBar.getMenu();
                            //add by tyd mingjun for comments
                            if (mBottomText != null) {
//                                mBottomText.setvisible(false);
                            }
                            //end
                            MenuExecutor.updateMenuOperation(menu, 0);
                            //*/

                            //*/ Added by Linguanrong for photopage bottom controls, 2014-9-17
                            hideBars();
                            //*/
                        } else if (mCurrentPhoto == mediaItem) {
//                            updateMenuOperations();
                        }
                    }
                    // Added by TYD Theobald_Wu on 20130409 [end]

                    if (!mSkipUpdateCurrentPhoto) {
                        if (item != null) {
                            MediaItem photo = mModel.getMediaItem(0);
                            if (photo != null) updateCurrentPhoto(photo);
                        }
                        //*/ Modified by Linguanrong for show bar default, 2015-08-07
//                        updateActionBarTitle();
                        /*/
                        updateBars();
                        //*/
                    }
                    // Reset the timeout for the bars after a swipe
                    refreshHidingMessage();
                }

                @Override
                public void onLoadingStarted() {
                }

                @Override
                public void onLoadingFinished(boolean loadingFailed) {
                    refreshBottomControlsWhenReady();
                    if (!mModel.isEmpty()) {
                        MediaItem photo = mModel.getMediaItem(0);
                        if (photo != null) updateCurrentPhoto(photo);
                    } else if (mIsActive) {
                        // We only want to finish the PhotoPage if there is no
                        // deletion that the user can undo.
                        if (mMediaSet.getNumberOfDeletions() == 0) {
                            mPhotoView.pause();
                            mActivity.getStateManager().finishState(
                                    PhotoPage.this);
                        }
                    }
                }
            });
        }

        RelativeLayout galleryRoot = (RelativeLayout) mActivity.findViewById(R.id.gallery_root);
        if (galleryRoot != null) {
            if (mSecureAlbum == null) {
                mBottomControls = new PhotoPageBottomControls(this, mActivity, galleryRoot);
            }
        }

        ((GLRootView) mActivity.getGLRoot()).setOnSystemUiVisibilityChangeListener(
                new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        int diff = mLastSystemUiVis ^ visibility;
                        mLastSystemUiVis = visibility;
                        if ((diff & View.SYSTEM_UI_FLAG_FULLSCREEN) != 0
                                && (visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                            showBars();
                        }
                    }
                });

//        new Handler().post(new Runnable() {
//            @Override
//            public void run() {
//                if (mSharedPref.getBoolean("showBlockGuide", true) && mSetPathString != null
//                        && !mPhotoView.getFilmMode() && mShowBars) {
//                    showGuideDialog();
//                }
//            }
//        });
    }
    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        for (int i = 0; i < menu.size(); i++){
            menu.getItem(i).setVisible(false);
            menu.getItem(i).setEnabled(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }


    @Override
    public void onPause() {
        super.onPause();
        mIsActive = false;

        mOrientationManager.getmRotationObserver().stopObserver();
        mActivity.getGLRoot().unfreeze();
        mHandler.removeMessages(MSG_UNFREEZE_GLROOT);

        DetailsHelper.pause();
        // Hide the detail dialog on exit
        if (mShowDetails) hideDetails();
        if (mModel != null) {
            mModel.pause();
        }
        mPhotoView.pause();
        mHandler.removeMessages(MSG_HIDE_BARS);
        mHandler.removeMessages(MSG_REFRESH_BOTTOM_CONTROLS);
        refreshBottomControlsWhenReady();
        /*mActionBar.removeOnMenuVisibilityListener(mMenuVisibilityListener);
        if (mShowSpinner) {
            mActionBar.disableAlbumModeMenu(true);
        }*/
        onCommitDeleteImage();
        mMenuExecutor.pause();
        if (mMediaSet != null) mMediaSet.clearDeletion();

        // SPRD: Add for bug535110 new feature,  support play audio picture
        releasePlayer();
    }

    private void hideDetails() {
        mShowDetails = false;
        mDetailsHelper.hide();
    }

 
   @Override
    protected void onResume() {
        super.onResume();
        mActivity.getNavigationWidgetManager().observe();
        //*/ freeme.gulincheng. 20170920. #0018664 move from onCreate to onResume
        if (mSetPathString == null) {
            // Get default media set by the URI
            MediaItem mediaItem = (MediaItem)
                    mActivity.getDataManager().getMediaObject(itemPath);
            /// M: [BUG.ADD] fix JE when mediaItem is deleted@{
            if (mediaItem == null) {
                Toast.makeText(((Activity) mActivity), R.string.no_such_item,
                        Toast.LENGTH_LONG).show();
                mPhotoView.pause();
                mActivity.getStateManager().finishState(this);
                return;
            }
            /// @}
            /// M: [BUG.ADD] @{
            // no PhotoDataAdapter style loading in SinglePhotoDataAdapter
            //mLoadingFinished = true;
            /// @}
            mModel = new SinglePhotoDataAdapter(mActivity, mPhotoView, mediaItem);
            mPhotoView.setModel(mModel);
            updateCurrentPhoto(mediaItem);
            mShowSpinner = false;
        }

        mPhotoView.setFilmMode(mStartInFilmstrip && mMediaSet.getMediaItemCount() > 1);
        //*/
        if (mModel == null) {
            /// M: [BUG.ADD] pause PhotoView before finish PhotoPage @{
            mPhotoView.pause();
            /// @}
            mActivity.getStateManager().finishState(this);
            return;
        }
        transitionFromAlbumPageIfNeeded();

        //*/ Added by droi Linguanrong for lock orientation, 16-3-1
        mOrientationManager.unlockOrientation();
        //*/
        mOrientationManager.getmRotationObserver().startObserver();

        /// M: [BUG.MARK] @{
        // In order to avoid black screen when PhotoPage just starts, google freeze the GLRoot when
        // resume, and unfreeze it when image updated or unfreeze time out. But this solution is not
        // suitable for N, it will cause ANR when lock/unlock screen.
        /* mActivity.getGLRoot().freeze();*/
        /// @}
        mIsActive = true;
        setContentPane(mRootPane);
        mModel.resume();
        mPhotoView.resume();
        //*/ Modified by droi Linguanrong for freeme gallery, 16-1-14
//        mActionBar.addOnMenuVisibilityListener(mMenuVisibilityListener);
//        updateActionBarTitle();
        /*/
        mActionBar.setDisplayOptions(
                ((mSecureAlbum == null) && (mSetPathString != null)), false);
        mActionBar.addOnMenuVisibilityListener(mMenuVisibilityListener);
        updateActionBarTitle();
        refreshBottomControlsWhenReady();
         */
        if (mSecureAlbum == null) { // should add this condition, Theobald_Wu, 2014-11-05
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    /**
                     * [TYD_M001]
                     * Modified by TYD Theobald_Wu on 20150617 for improve the speed of entering
                     * application, solution 4, delay initialize bottom control.
                     */
                    RelativeLayout galleryRoot = (RelativeLayout) mActivity.findViewById(R.id.gallery_root);
                    if (galleryRoot != null && mBottomControls == null) {
                        mBottomControls = new PhotoPageBottomControls(PhotoPage.this, mActivity, galleryRoot);
                    }
                    /***/
                    refreshBottomControlsWhenReady();
                }
            }, PhotoPageBottomControls.CONTAINER_ANIM_DURATION_MS);
        }
        //*/
        /*if (mShowSpinner && mPhotoView.getFilmMode()) {
            mActionBar.enableAlbumModeMenu(
                    GalleryActionBar.ALBUM_FILMSTRIP_MODE_SELECTED, this);
        }*/
        if (!mShowBars) {
//            mActionBar.hide();
            mActivity.getGLRoot().setLightsOutMode(true);
        }
        boolean haveImageEditor = true;//GalleryUtils.isEditorAvailable(mActivity, "image/*");
        if (haveImageEditor != mHaveImageEditor) {
            mHaveImageEditor = haveImageEditor;
//            updateMenuOperations();
        }

        if (mRecenterCameraOnResume) {
            mPhotoView.switchToImage(1);
            mRecenterCameraOnResume = false;
        }
        mHandler.sendEmptyMessageDelayed(MSG_UNFREEZE_GLROOT, UNFREEZE_GLROOT_TIMEOUT);
    }

    @Override
    protected boolean onCreateActionBar(Menu menu) {
        //*/ Added by Linguanrong for photopage bottom controls, 2014-9-17
        setActionBarBackground(true);
        //*/
//        mActionBar.createActionBarMenu(R.menu.photo, menu);
//        mHaveImageEditor = GalleryUtils.isEditorAvailable(mActivity, "image/*");
//        updateMenuOperations();
        //*/ Modified by droi Linguanrong for freeme gallery, 16-1-14
//        updateActionBarTitle();
        /*/
        mActionBar.setTitle(mMediaSet != null ? mMediaSet.getName() : "");
        //*/

        //add by tyd mingjun for comment
//        MenuItem item = menu.findItem(R.id.action_comment);
//        if (readOfflineNewStatus("visible", false)) {
//            item.setTitle(mActivity.getResources().getString(
//                    R.string.freeme_comment_gone));
//            isCommentvisible = true;
//        } else {
//            item.setTitle(mActivity.getResources().getString(
//                    R.string.freeme_comment_visible));
//            isCommentvisible = false;
//        }
        //end

        return true;
    }

    //////////////////////////////////////////////////////////////////////////
    //  AppBridge.Server interface
    //////////////////////////////////////////////////////////////////////////

    @Override
    protected boolean onItemSelected(MenuItem item) {
        if (mModel == null) return true;
        refreshHidingMessage();
        MediaItem current = mModel.getMediaItem(0);

        // This is a shield for monkey when it clicks the action bar
        // menu when transitioning from filmstrip to camera
        if (current instanceof SnailItem) return true;
        // TODO: We should check the current photo against the MediaItem
        // that the menu was initially created for. We need to fix this
        // after PhotoPage being refactored.
        if (current == null) {
            // item is not ready, ignore
            return true;
        }
        int currentIndex = mModel.getCurrentIndex();
        Path path = current.getPath();

        DataManager manager = mActivity.getDataManager();
        int action = item.getItemId();
        String confirmMsg = null;
        switch (action) {
            case android.R.id.home: {
                onUpPressed();
                return true;
            }
            case R.id.action_slideshow: {
                //*/ Added by tyd Linguanrong for statistic, 15-12-18
//                StatisticUtil.generateStatisticInfo(mActivity, StatisticData.OPTION_SLIDESHOW);
                //*/
                // for baas analytics
//                DroiAnalytics.onEvent(mActivity, StatisticData.OPTION_SLIDESHOW);

                Bundle data = new Bundle();
                data.putString(SlideshowPage.KEY_SET_PATH, mMediaSet.getPath().toString());
                data.putString(SlideshowPage.KEY_ITEM_PATH, path.toString());
                data.putInt(SlideshowPage.KEY_PHOTO_INDEX, currentIndex);
                data.putBoolean(SlideshowPage.KEY_REPEAT, true);
                mActivity.getStateManager().startStateForResult(
                        SlideshowPage.class, REQUEST_SLIDESHOW, data);
                return true;
            }
            case R.id.action_crop: {
                Activity activity = mActivity;
                Intent intent = new Intent(CropActivity.CROP_ACTION);
                intent.setClass(activity, CropActivity.class);
                intent.setDataAndType(manager.getContentUri(path), current.getMimeType())
                        .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                activity.startActivityForResult(intent, PicasaSource.isPicasaImage(current)
                        ? REQUEST_CROP_PICASA
                        : REQUEST_CROP);
                return true;
            }
            case R.id.action_trim: {
                Intent intent = new Intent(mActivity, TrimVideo.class);
                intent.setData(FreemeUtils.convertUri(manager.getContentUri(path)));
                // We need the file path to wrap this into a RandomAccessFile.
                intent.putExtra(KEY_MEDIA_ITEM_PATH, current.getFilePath());
                mActivity.startActivityForResult(intent, REQUEST_TRIM);
                return true;
            }
            case R.id.action_mute: {
                MuteVideo muteVideo = new MuteVideo(current.getFilePath(),
                        FreemeUtils.convertUri(manager.getContentUri(path)), mActivity);
                muteVideo.muteInBackground();
                return true;
            }
            case R.id.action_edit: {
                launchPhotoEditor();
                //*/ Added by tyd Linguanrong for statistic, 15-12-18
//                StatisticUtil.generateStatisticInfo(mActivity, StatisticData.OPTION_EDIT);
                //*/
                // for baas analytics
//                DroiAnalytics.onEvent(mActivity, StatisticData.OPTION_EDIT);

                return true;
            }
            case R.id.action_simple_edit: {
                launchSimpleEditor();
                return true;
            }
            case R.id.action_details: {
                if (mShowDetails) {
                    hideDetails();
                } else {
                    showDetails();
                }
                return true;
            }
            case R.id.print: {
                mActivity.printSelectedImage(manager.getContentUri(path));
                return true;
            }
            case R.id.action_delete:
                confirmMsg = mActivity.getResources().getQuantityString(
                        R.plurals.delete_selection, 1);
            case R.id.action_setas:
            case R.id.action_rotate_ccw:
            case R.id.action_rotate_cw:
            case R.id.action_show_on_map:
                mSelectionManager.deSelectAll();
                mSelectionManager.toggle(path);
                mMenuExecutor.onMenuClicked(item, confirmMsg, mConfirmDialogListener);
                return true;
            //*/ Added by Linguanrong for photopage bottom controls, 2014-9-17
            case R.id.action_camera: {
                GalleryUtils.startCameraActivity(mActivity);
                return true;
            }
            //*/

            //*/ Added by droi Linguanrong for comments, 16-2-1
            case R.id.action_comment: {
                if (item.getTitle().equals(mActivity.getResources().getString(
                        R.string.freeme_comment_visible))) {
                    item.setTitle(mActivity.getResources().getString(
                            R.string.freeme_comment_gone));
//            		 mBottomText.setvisible(true);
                    isCommentvisible = true;
                    if (isCommentvisible && mBottomText != null && !isEnbled) {
                        mBottomText.setvisible(true);
                    }

                    saveOfflineNewStatus("visible", isCommentvisible);
                } else {
                    item.setTitle(mActivity.getResources().getString(
                            R.string.freeme_comment_visible));
//                    mBottomText.setvisible(false);
                    isCommentvisible = false;
                    saveOfflineNewStatus("visible", isCommentvisible);
                }

                return true;
            }
            //end

            //*/ Added by tyd linguanrong for enable action share, 2015-12-18
            case R.id.action_share:
                actionShare();
                //*/ Added by tyd Linguanrong for statistic, 15-12-18
//                StatisticUtil.generateStatisticInfo(mActivity, StatisticData.OPTION_SHARE);
                //*/

                // for baas analytics
//                DroiAnalytics.onEvent(mActivity, StatisticData.OPTION_SHARE);
                return true;
            //*/
            default:
                return false;
        }
    }

    @Override
    protected void onDestroy() {
        if (mAppBridge != null) {
            mAppBridge.setServer(null);
            mScreenNailItem.setScreenNail(null);
            mAppBridge.detachScreenNail();
            mAppBridge = null;
            mScreenNailSet = null;
            mScreenNailItem = null;
        }
        mActivity.getGLRoot().setOrientationSource(null);
        if (mBottomControls != null) mBottomControls.cleanup();
        //*/ Added by droi Linguanrong for comments, 16-2-1
        if (mBottomText != null) mBottomText.cleanup();
        //*/

        // Remove all pending messages.
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void setActionBarBackground(boolean translucent) {
        if (translucent) {
//            mActionBar.setBackgroundDrawable(mActivity.getResources()
//                    .getDrawable(GalleryActivity.colorPrimary, null));
        }
    }

    private void updateMenuOperations() {
        Menu menu = mActionBar.getMenu();

        // it could be null if onCreateActionBar has not been called yet
        if (menu == null) return;

        MenuItem item = menu.findItem(R.id.action_slideshow);
        if (item != null) {
            item.setVisible((mSecureAlbum == null) && canDoSlideShow());
        }
        if (mCurrentPhoto == null) return;

        int supportedOperations = mCurrentPhoto.getSupportedOperations();
        if (mReadOnlyView) {
            supportedOperations ^= MediaObject.SUPPORT_EDIT;
        }
        if (mSecureAlbum != null) {
            supportedOperations &= MediaObject.SUPPORT_DELETE;
        } else {
            mCurrentPhoto.getPanoramaSupport(mUpdatePanoramaMenuItemsCallback);
            if (!mHaveImageEditor) {
                supportedOperations &= ~MediaObject.SUPPORT_EDIT;
            }
        }

        /*/ Added by Linguanrong for panorama unsupport rotate, 2015-4-22
        if (mCurrentPhoto.getMediaData() != null
                && mCurrentPhoto.getMediaData().mediaType == MediaData.MediaType.PANORAMA) {
            supportedOperations &= ~MediaObject.SUPPORT_ROTATE;
        }
        //*/

        //*/ Added by droi Linguanrong for comments, 16-2-1
        if (mSetPathString == null) {
            supportedOperations &= ~MediaObject.SUPPORT_COMMENT;
        } else {
            supportedOperations |= MediaObject.SUPPORT_COMMENT;
        }
        //end

        if (mCurrentPhoto.getMediaType() == MediaObject.MEDIA_TYPE_IMAGE_REFOCUS
                || mCurrentPhoto.getMimeType().startsWith("refocusImage/")) {
            supportedOperations &= ~MediaObject.SUPPORT_SHARE;
            supportedOperations &= ~MediaObject.SUPPORT_ROTATE;
            supportedOperations &= ~MediaObject.SUPPORT_CROP;
            /*if (GalleryUtils.isBlendingEnable()) {
                supportedOperations |= MediaObject.SUPPORT_BLENDING;
            }*/
        }
        MenuExecutor.updateMenuOperation(menu, supportedOperations);

        //*/ Added by Linguanrong for photopage bottom controls, 2014-9-17
        boolean filmmode = mPhotoView.getFilmMode() && (mOriginalSetPathString != null);
        MenuItem action_item = menu.findItem(R.id.action_share);
        if (action_item != null) {
            action_item.setVisible(filmmode);
        }

        action_item = menu.findItem(R.id.action_delete);
        if (action_item != null) {
            action_item.setVisible(filmmode);
        }

        action_item = menu.findItem(R.id.action_slideshow);
        if (action_item != null) {
            action_item.setVisible(filmmode);
        }

        action_item = menu.findItem(R.id.action_edit);
        if (action_item != null) {
            action_item.setVisible(filmmode);
        }

        if (mOriginalSetPathString == null) {
            action_item = menu.findItem(R.id.action_camera);
            if (action_item != null) {
                action_item.setVisible(false);
            }
        }
        //*/

    }

    private void updateActionBarTitle() {
        if (mPhotoView == null || mActionBar == null) {
            return;
        }

        try {
            if (mActivity.getStateManager().getTopState() != this) {
                return;
            }
        } catch (AssertionError e) {
            Log.v(TAG, "no state in State Manager when updates actionbar title");
            return;
        }

        mActionBar.setDisplayOptions(((mSecureAlbum == null) && (mSetPathString != null)), true);
        //*/ Modified by droi Linguanrong for camera shortcut, 16-2-29
        if (canShowCameraShortCut) {
            mActionBar.setTitle(mCurrentPhoto != null ?
                    mMyDetailsSource.setIndex() + "/" + mMyDetailsSource.size() : "");
        } else {
            mActionBar.setTitle(mCurrentPhoto != null ?
                    (mMyDetailsSource.setIndex() + 1) + "/" + (mMyDetailsSource.size() + 1) : "");
        }
        //*/
    }

    // add by mingjun for comment
    private boolean readOfflineNewStatus(String key, boolean defaultVal) {
        return mSharedPref.getBoolean(key, defaultVal);
    }

    private boolean canDoSlideShow() {
        if (mMediaSet == null || mCurrentPhoto == null) {
            return false;
        }
        return mCurrentPhoto.getMediaType() == MediaObject.MEDIA_TYPE_IMAGE;
    }

    private void transitionFromAlbumPageIfNeeded() {
        TransitionStore transitions = mActivity.getTransitionStore();

        int albumPageTransition = transitions.get(
                KEY_ALBUMPAGE_TRANSITION, MSG_ALBUMPAGE_NONE);

        if (albumPageTransition == MSG_ALBUMPAGE_NONE && mAppBridge != null
                && mRecenterCameraOnResume) {
            // Generally, resuming the PhotoPage when in Camera should
            // reset to the capture mode to allow quick photo taking
            mCurrentIndex = 0;
            mPhotoView.resetToFirstPicture();
        } else {
            int resumeIndex = transitions.get(KEY_INDEX_HINT, -1);
            if (resumeIndex >= 0) {
                if (mHasCameraScreennailOrPlaceholder) {
                    // Account for preview/placeholder being the first item
                    resumeIndex++;
                }
                if (resumeIndex < mMediaSet.getMediaItemCount()) {
                    mCurrentIndex = resumeIndex;
                    mModel.moveTo(mCurrentIndex);
                }
            }
        }

        if (albumPageTransition == MSG_ALBUMPAGE_RESUMED) {
            mPhotoView.setFilmMode(mStartInFilmstrip || mAppBridge != null);
        } else if (albumPageTransition == MSG_ALBUMPAGE_PICKED) {
            mPhotoView.setFilmMode(false);
        }
    }

    private void setCurrentPhotoByIntent(Intent intent) {
        if (intent == null) return;
        Path path = mApplication.getDataManager()
                .findPathByUri(intent.getData(), intent.getType());
        if (path != null) {
            Path albumPath = mApplication.getDataManager().getDefaultSetOf(path);
            if (albumPath == null) {
                return;
            }
//            if (!albumPath.equalsIgnoreCase(mOriginalSetPathString)) {
//                // If the edited image is stored in a different album, we need
//                // to start a new activity state to show the new image
//                Bundle data = new Bundle(getData());
//                data.putString(KEY_MEDIA_SET_PATH, albumPath.toString());
//                data.putString(PhotoPage.KEY_MEDIA_ITEM_PATH, path.toString());
//                mActivity.getStateManager().startState(SinglePhotoPage.class, data);
//                return;
//            }
            mModel.setCurrentPhoto(path, mCurrentIndex);
            mActivity.getDataManager().broadcastUpdatePicture();
        }
    }

    private void setCurrentPhotoByIntentEx(Intent intent) {
        if (intent == null) return;
        Path photoEditPath = mApplication.getDataManager()
                .findPathByUri(intent.getData(), intent.getType());
        if (photoEditPath != null) {
            String string = photoEditPath.toString();
            if (string != null) {
                mModel.setCurrentPhoto(Path.fromString(string), mCurrentIndex);
                mActivity.getDataManager().broadcastUpdatePicture();
            }
        }
    }

    @Override
    public void setCameraRelativeFrame(Rect frame) {
        mPhotoView.setCameraRelativeFrame(frame);
    }

    @Override
    public boolean switchWithCaptureAnimation(int offset) {
        return mPhotoView.switchWithCaptureAnimation(offset);
    }

    @Override
    public void setSwipingEnabled(boolean enabled) {
        mPhotoView.setSwipingEnabled(enabled);
    }

    @Override
    public void notifyScreenNailChanged() {
        mScreenNailItem.setScreenNail(mAppBridge.attachScreenNail());
        mScreenNailSet.notifyChange();
    }

    @Override
    public void addSecureAlbumItem(boolean isVideo, int id) {
        mSecureAlbum.addMediaItem(isVideo, id);
    }

    private void showDetails() {
        mShowDetails = true;
        if (mDetailsHelper == null) {
            mDetailsHelper = new DetailsHelper(mActivity, mRootPane, new MyDetailsSource());
            mDetailsHelper.setCloseListener(new CloseListener() {
                @Override
                public void onClose() {
                    hideDetails();
                    mDialogOpen = false;
                    hideBars();
                }
            });
            mDetailsHelper.setOpenListener(new OpenListener() {
                @Override
                public void onOpen() {
                    mDialogOpen = true;
                }
            });
        }
        mDetailsHelper.show();
    }

    ////////////////////////////////////////////////////////////////////////////
    //  Callbacks from PhotoView
    ////////////////////////////////////////////////////////////////////////////
    @Override
    public void onSingleTapUp(int x, int y) {
        if (mAppBridge != null) {
            if (mAppBridge.onSingleTapUp(x, y)) return;
        }

        MediaItem item = mModel.getMediaItem(0);
        if (item == null || item == mScreenNailItem) {
            // item is not ready or it is camera preview, ignore
            return;
        }
        int w = mPhotoView.getWidth();
        int h = mPhotoView.getHeight();
        int supported = item.getSupportedOperations();
        boolean playVideo = ((supported & MediaItem.SUPPORT_PLAY) != 0);
        boolean unlock = ((supported & MediaItem.SUPPORT_UNLOCK) != 0);
        boolean goBack = ((supported & MediaItem.SUPPORT_BACK) != 0);
        boolean launchCamera = ((supported & MediaItem.SUPPORT_CAMERA_SHORTCUT) != 0);
        boolean clickCenter = false;

        clickCenter = (Math.abs(x - w / 2) * 12 <= w) && (Math.abs(y - h / 2) * 12 <= h);

        if (playVideo && clickCenter) {
            if (mSecureAlbum == null) {
                FreemeUtils.playVideo(mActivity, item.getPlayUri(), item.getName());
            } else {
                mActivity.getStateManager().finishState(this);
            }
        } else if (goBack) {
            onBackPressed();
        } else if (unlock) {
            Intent intent = new Intent(mActivity, GalleryActivity.class);
            intent.putExtra(GalleryActivity.KEY_DISMISS_KEYGUARD, true);
            mActivity.startActivity(intent);
        } else if (launchCamera) {
            launchCamera();
        } else if (item.getMimeType().startsWith("refocusImage/") && clickCenter) {
            if (FrameworkSupportUtils.isSupportRefocusImage()) {
                startRefocusActivity(item.getPlayUri(), item);
            }
        } else {
            toggleBars();
        }
    }

    @Override
    public void onFullScreenChanged(boolean full) {
        Message m = mHandler.obtainMessage(
                MSG_ON_FULL_SCREEN_CHANGED, full ? 1 : 0, 0);
        m.sendToTarget();
    }

    @Override
    public void onActionBarAllowed(boolean allowed) {
        mActionBarAllowed = allowed;
        mHandler.sendEmptyMessage(MSG_UPDATE_ACTION_BAR);
    }

    @Override
    public void onActionBarWanted() {
        mHandler.sendEmptyMessage(MSG_WANT_BARS);
    }

    @Override
    public void onCurrentImageUpdated() {
        mActivity.getGLRoot().unfreeze();
    }

    // How we do delete/undo:
    //
    // When the user choose to delete a media item, we just tell the
    // FilterDeleteSet to hide that item. If the user choose to undo it, we
    // again tell FilterDeleteSet not to hide it. If the user choose to commit
    // the deletion, we then actually delete the media item.
    @Override
    public void onDeleteImage(Path path, int offset) {
        onCommitDeleteImage();  // commit the previous deletion
        mDeletePath = path;
        mDeleteIsFocus = (offset == 0);
        mMediaSet.addDeletion(path, mCurrentIndex + offset);
    }

    @Override
    public void onUndoDeleteImage() {
        if (mDeletePath == null) return;
        // If the deletion was done on the focused item, we want the model to
        // focus on it when it is undeleted.
        if (mDeleteIsFocus) mModel.setFocusHintPath(mDeletePath);
        mMediaSet.removeDeletion(mDeletePath);
        mDeletePath = null;
    }

    @Override
    public void onCommitDeleteImage() {
        if (mDeletePath == null) return;
        mMenuExecutor.startSingleItemAction(R.id.action_delete, mDeletePath);
        mDeletePath = null;
    }

    @Override
    public void onFilmModeChanged(boolean enabled) {
        refreshBottomControlsWhenReady();
        //add by TYD mingjun for for comments
        isEnbled = enabled;
        //end
        /*if (mShowSpinner) {
            if (enabled) {
                mActionBar.enableAlbumModeMenu(
                        GalleryActionBar.ALBUM_FILMSTRIP_MODE_SELECTED, this);
            } else {
                mActionBar.disableAlbumModeMenu(true);
            }
        }*/
        if (enabled) {
            mHandler.removeMessages(MSG_HIDE_BARS);
            UsageStatistics.onContentViewChanged(
                    UsageStatistics.COMPONENT_GALLERY, "FilmstripPage");
        } else {
            refreshHidingMessage();
            if (mAppBridge == null || mCurrentIndex > 0) {
                UsageStatistics.onContentViewChanged(
                        UsageStatistics.COMPONENT_GALLERY, "SinglePhotoPage");
            } else {
                UsageStatistics.onContentViewChanged(
                        UsageStatistics.COMPONENT_CAMERA, "Unknown"); // TODO
            }
        }

        //*/ Added by droi Linguanrong for freeme gallery, 16-1-14
//        updateActionBarTitle();
//        updateMenuOperations();
        //*/
    }

    @Override
    public void onPictureCenter(boolean isCamera) {
        isCamera = isCamera || (mHasCameraScreennailOrPlaceholder && mAppBridge == null);
        mPhotoView.setWantPictureCenterCallbacks(false);
        mHandler.removeMessages(MSG_ON_CAMERA_CENTER);
        mHandler.removeMessages(MSG_ON_PICTURE_CENTER);
        mHandler.sendEmptyMessage(isCamera ? MSG_ON_CAMERA_CENTER : MSG_ON_PICTURE_CENTER);
    }

    @Override
    public void onUndoBarVisibilityChanged(boolean visible) {
        refreshBottomControlsWhenReady();
    }

    private void refreshHidingMessage() {
        mHandler.removeMessages(MSG_HIDE_BARS);
        if (!mIsMenuVisible && !mPhotoView.getFilmMode()) {
            mHandler.sendEmptyMessageDelayed(MSG_HIDE_BARS, HIDE_BARS_TIMEOUT);
        }
    }

    @Override
    public void onAlbumModeSelected(int mode) {
        if (mode == GalleryActionBar.ALBUM_GRID_MODE_SELECTED) {
            switchToGrid();
        }
    }

    private void switchToGrid() {
        if (mActivity.getStateManager().hasStateClass(AlbumPage.class)) {
            onUpPressed();
        } else {
            if (mOriginalSetPathString == null) return;
            Bundle data = new Bundle(getData());
            data.putString(AlbumPage.KEY_MEDIA_PATH, mOriginalSetPathString);
            data.putString(AlbumPage.KEY_PARENT_MEDIA_PATH,
                    mActivity.getDataManager().getTopSetPath(
                            DataManager.INCLUDE_ALL));

            data.putBoolean(PhotoPage.KEY_APP_BRIDGE, mAppBridge != null);

            // Account for live preview being first item
            mActivity.getTransitionStore().put(KEY_RETURN_INDEX_HINT,
                    mAppBridge != null ? mCurrentIndex - 1 : mCurrentIndex);

            if (mHasCameraScreennailOrPlaceholder && mAppBridge != null) {
                mActivity.getStateManager().startState(AlbumPage.class, data);
            } else {
                mActivity.getStateManager().switchState(this, AlbumPage.class, data);
            }
        }
    }

    private void onUpPressed() {
        if ((mStartInFilmstrip || mAppBridge != null)
                && !mPhotoView.getFilmMode()) {
            mPhotoView.setFilmMode(true);
            return;
        }

        if (mActivity.getStateManager().getStateCount() > 1) {
            setResult();
            super.onBackPressed();
            return;
        }

        if (mOriginalSetPathString == null) return;

        if (mAppBridge == null) {
            // We're in view mode so set up the stacks on our own.
            Bundle data = new Bundle(getData());
            data.putString(AlbumPage.KEY_MEDIA_PATH, mOriginalSetPathString);
            data.putString(AlbumPage.KEY_PARENT_MEDIA_PATH,
                    mActivity.getDataManager().getTopSetPath(
                            DataManager.INCLUDE_ALL));
            mActivity.getStateManager().switchState(this, AlbumPage.class, data);
        } else {
            GalleryUtils.startGalleryActivity(mActivity);
        }
    }

    private void setResult() {
        Intent result = null;
        result = new Intent();
        result.putExtra(KEY_RETURN_INDEX_HINT, mCurrentIndex);
        setStateResult(Activity.RESULT_OK, result);
    }

    private void updatePanoramaUI(boolean isPanorama360) {
        Menu menu = mActionBar.getMenu();

        // it could be null if onCreateActionBar has not been called yet
        if (menu == null) {
            return;
        }

        MenuExecutor.updateMenuForPanorama(menu, isPanorama360, isPanorama360);
    }

    @Override
    public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
        final long timestampMillis = mCurrentPhoto.getDateInMs();
        final String mediaType = getMediaTypeString(mCurrentPhoto);
        UsageStatistics.onEvent(UsageStatistics.COMPONENT_GALLERY,
                UsageStatistics.ACTION_SHARE,
                mediaType,
                timestampMillis > 0
                        ? System.currentTimeMillis() - timestampMillis
                        : -1);
        return false;
    }

    private static String getMediaTypeString(MediaItem item) {
        if (item.getMediaType() == MediaObject.MEDIA_TYPE_VIDEO) {
            return "Video";
        } else if (item.getMediaType() == MediaObject.MEDIA_TYPE_IMAGE) {
            return "Photo";
        } else {
            return "Unknown:" + item.getMediaType();
        }
    }

    //*/ Added by droi Linguanrong for bigmodel, 16-2-22
    public void jumptolarge() {
        MediaItem current = mModel.getMediaItem(0);
        Path path = current.getPath();
        DataManager manager = mActivity.getDataManager();
        Activity activity = mActivity;
        Intent intent = new Intent("intent.action.freemegallery.largermode");
        //intent.setClass(activity, BlockbustercropActivity.class);
        intent.setDataAndType(manager.getContentUri(path), current.getMimeType())
                .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivityForResult(intent, REQUEST_BIGMODE);
    }
    //*/

    private void saveOfflineNewStatus(String key, boolean value) {
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public interface Model extends PhotoView.Model {
        void resume();

        void pause();

        boolean isEmpty();

        void setCurrentPhoto(Path path, int indexHint);
    }
    //*/

    private class MyMenuVisibilityListener implements OnMenuVisibilityListener {
        @Override
        public void onMenuVisibilityChanged(boolean isVisible) {
            mIsMenuVisible = isVisible;
            refreshHidingMessage();
        }
    }

    private class MyDetailsSource implements DetailsSource {

        @Override
        public boolean isCamera() {
            return canShowCameraShortCut;
        }

        @Override
        public int size() {
            return mMediaSet != null ? mMediaSet.getMediaItemCount() - 1 : 0;
        }

        @Override
        public int setIndex() {
            return mModel.getCurrentIndex();
        }

        @Override
        public MediaDetails getDetails() {
            return mModel.getMediaItem(0).getDetails();
        }
    }
    //end

    private void showGuideDialog() {
        isBlockGuideShow = true;
        mHandler.removeMessages(MSG_HIDE_BARS);
        mOrientationManager.lockOrientation(true);

        WindowManager windowManager = mActivity.getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getMetrics(displayMetrics);

        int left = displayMetrics.widthPixels / 5;
        int top = displayMetrics.heightPixels - 60;
        int right = left * 2;
        int bottom = displayMetrics.heightPixels;
        final Rect rect = new Rect(left, top, right, bottom);
        final Dialog guide = new Dialog(mActivity, R.style.GuideNotFullScreen);
        View view = new View(mActivity.getAndroidContext());
        view.setBackgroundResource(FreemeUtils.isInternational(mActivity)
                ? R.drawable.guide_bg_en : R.drawable.guide_bg);
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        int getX = (int) event.getX();
                        int getY = (int) event.getY();
                        if (rect.contains(getX, getY)) {
                            jumptolarge();
                        }
                        guide.dismiss();
                        mOrientationManager.unlockOrientation();
                        refreshHidingMessage();
                        isBlockGuideShow = false;
                        break;
                }
                return true;
            }
        });
        mEditor.putBoolean("showBlockGuide", false);
        mEditor.commit();
        guide.setContentView(view);
        guide.show();
    }
    //********************************************************************
    //*                              freeme                              *
    //********************************************************************

    public static final String KEY_STRAT_FROM_TIMESHAFT = "strat_from_timeshaft";
    private boolean mIsStartFromTimeshaft;
    //********************************************************************
    //*                              sprd                                *
    //********************************************************************

    private static final String ACTION_REFOCUS_EDIT = "com.android.sprd.gallery3d.refocusedit";
    private static final String ACTION_REFOCUS_EDIT_BST = "cn.com.bst.librefocus.refocusedit";

    public void startRefocusActivity(Uri uri, MediaItem item) {
        if (uri == null || item == null)
            return;
        int refocusPhotoWidth = item.getWidth();
        int refocusPhotoHeight = item.getHeight();
        Intent intent = new Intent();
        intent.setAction(ACTION_REFOCUS_EDIT);
        intent.setDataAndType(uri, "refocusImage/jpeg");
        intent.putExtra(RefocusPhotoEditActivity.REFOCUS_WIDTH, refocusPhotoWidth);
        intent.putExtra(RefocusPhotoEditActivity.REFOCUS_HEIGHT, refocusPhotoHeight);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            Log.d(TAG, "startRefocusActivity");
            mActivity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Refocus activity previously detected but cannot be found", e);
        }
    }
    
    public void startBstRefocusActivity() {
        if (mCurrentPhoto == null || mCurrentPhoto.getFilePath() == null)
            return;

        Intent intent = new Intent();
        intent.setClassName(mActivity, "cn.com.bst.librefocus.RefocusActivity");
        intent.setAction(ACTION_REFOCUS_EDIT_BST);
        intent.setData(Uri.parse(mCurrentPhoto.getFilePath()));
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION  | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        try {
            Log.d(TAG, "startRefocusActivity");
            mActivity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Refocus activity previously detected but cannot be found", e);
        }
    }
    /* SPRD: Add for bug535110 new feature,  support play audio picture @{ */
    private PhotoVoiceProgress mPhotoVoiceProgress;
    private AudioManager mAudioManager;
    private MediaPlayer mMediaPlayer;

    private void releasePlayer() {
        if (mMediaPlayer != null) {
            Log.e(TAG, "releasePlayer");
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            //SPRD : fix bug 604671 show voice photo is different from camera.
            if (mPhotoVoiceProgress != null) {
                mPhotoVoiceProgress.stopShowTime();
            }
            abandonAudioFocus();
        }
    }

    @Override
    public int getTime() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()){
            return mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    private void playPhotoVoice(String path) {
        if (null == mMediaPlayer) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer arg0) {
                    //SPRD : fix bug 604671 show voice photo is different from camera.
                    if (mPhotoVoiceProgress != null) {
                        mPhotoVoiceProgress.stopShowTime();
                        mPhotoVoiceProgress.setTimeListener(null);

                    }
                    abandonAudioFocus();
                }
            });
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
                    //SPRD : fix bug 604671 show voice photo is different from camera.
                    if (mPhotoVoiceProgress != null) {
                        mPhotoVoiceProgress.stopShowTime();
                    }
                    abandonAudioFocus();
                    return false;
                }
            });
        }

        if (path != null && mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.reset();
                //SPRD : fix bug 604671 show voice photo is different from camera.
                if (mPhotoVoiceProgress != null) {
                    mPhotoVoiceProgress.stopShowTime();
                }
                abandonAudioFocus();
                Log.e(TAG, "playPhotoVoice isPlaying , reset stop play");
                return;
            }
            File voiceFile = new File(path);
            if (!voiceFile.exists()) {
                Log.e(TAG, "playPhotoVoice path = " + path + " does not exist!");
                return;
            }
            try {
                if (mAudioManager.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT) == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
                    Toast.makeText(mActivity, R.string.play_audio_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                mPhotoVoiceProgress.setTimeListener(this);
                mMediaPlayer.reset();
                mMediaPlayer.setDataSource(path);
                mMediaPlayer.prepare();
                //SPRD : fix bug 604671 show voice photo is different from camera.
                if (mPhotoVoiceProgress != null) {
                    mPhotoVoiceProgress.setTotalTime(mMediaPlayer.getDuration());
                    mPhotoVoiceProgress.setFocusable(false);
                    mPhotoVoiceProgress.setClickable(false);
                    mPhotoVoiceProgress.startShowTime();
                }
                mMediaPlayer.start();
            } catch (Exception e) {
                Log.e(TAG, "playPhotoVoice Exception e = " + e.toString());
            }
            Log.e(TAG, "playPhotoVoice play path = " + path);
        } else {
            if (mMediaPlayer != null) {
                mMediaPlayer.reset();
                //SPRD : fix bug 604671 show voice photo is different from camera.
                if (mPhotoVoiceProgress != null) {
                    mPhotoVoiceProgress.stopShowTime();
                }
                abandonAudioFocus();
            }
        }
    }

    AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS:
                    if (mMediaPlayer != null) {
                        mMediaPlayer.reset();
                        //SPRD : fix bug 604671 show voice photo is different from camera.
                        if (mPhotoVoiceProgress != null) {
                            mPhotoVoiceProgress.stopShowTime();
                        }
                    }
            }
        }

        ;
    };

    private void abandonAudioFocus() {
        mAudioManager.abandonAudioFocus(afChangeListener);
    }
    /* @} */
    private Path getSingleSelectedPath() {
        ArrayList<Path> ids = mSelectionManager.getSelected(true);
        Utils.assertTrue(ids.size() == 1);
        return ids.get(0);
    }
    private Intent getIntentBySingleSelectedPath(String action) {
        DataManager manager = mActivity.getDataManager();
        Path path = getSingleSelectedPath();
        String mimeType = getMimeType(manager.getMediaType(path));
        return new Intent(action).setDataAndType(manager.getContentUri(path), mimeType);
    }

    public static String getMimeType(int type) {
        switch (type) {
            case MediaObject.MEDIA_TYPE_IMAGE :
                return GalleryUtils.MIME_TYPE_IMAGE;
            case MediaObject.MEDIA_TYPE_VIDEO :
                return GalleryUtils.MIME_TYPE_VIDEO;
            default: return GalleryUtils.MIME_TYPE_ALL;
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        mBottomControls.initViewsConfigureChanged();
    }

    public void onEnterState(){
        mActivity.showNavi(AbstractGalleryActivity.IN_PHOTOPAGE);

    }

    public void observe(){

    }
}

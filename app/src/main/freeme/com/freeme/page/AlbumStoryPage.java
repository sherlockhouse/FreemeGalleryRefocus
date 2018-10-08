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

package com.freeme.page;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.view.Display;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.gallery3d.app.ActivityState;
import com.android.gallery3d.app.AlbumDataLoader;
import com.android.gallery3d.app.AlbumSetPage;
import com.android.gallery3d.app.FilmstripPage;
import com.android.gallery3d.app.FilterUtils;
import com.android.gallery3d.app.GalleryActionBar;
import com.android.gallery3d.app.LoadingListener;
import com.android.gallery3d.app.OrientationManager;
import com.android.gallery3d.app.PhotoPage;
import com.android.gallery3d.app.SinglePhotoPage;
import com.android.gallery3d.app.SlideshowPage;
import com.android.gallery3d.app.TransitionStore;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaDetails;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.glrenderer.FadeTexture;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.ui.ActionModeHandler;
import com.android.gallery3d.ui.DetailsHelper;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.ui.MenuExecutor;
import com.android.gallery3d.ui.RelativePosition;
import com.android.gallery3d.ui.SelectionManager;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.GalleryUtils;
import com.freeme.data.StoryAlbum;
import com.freeme.data.StoryAlbumSet;
import com.freeme.data.StoryMergeAlbum;
import com.freeme.gallery.R;
import com.freeme.gallery.app.AbstractGalleryActivity;
import com.freeme.gallery.app.GalleryActivity;
import com.freeme.scott.galleryui.design.widget.FreemeBottomSelectedView;
import com.freeme.ui.AlbumTimeSlotRenderer;
import com.freeme.ui.DateSlotView;
import com.freeme.ui.manager.State;
import com.freeme.utils.FreemeUtils;
import com.freeme.utils.LogUtil;


public class AlbumStoryPage extends ActivityState implements GalleryActionBar.ClusterRunner,
        SelectionManager.SelectionListener, MediaSet.SyncListener,State, View.OnClickListener {
    public static final String KEY_MEDIA_PATH         = "media-path";
    public static final String KEY_AUTO_SELECT_ALL    = "auto-select-all";
    public static final String KEY_SELECT_INDEX       = "select-index";
    public static final String KEY_STORY_SELECT_INDEX = "story-select-index";
    public static final String KEY_COVER_INDEX        = "story-cover-index";
    public static final String KEY_NEW_ALBUM          = "story_new_album";
    public static final  int REQUEST_PHOTO        = 2;
    @SuppressWarnings("unused")
    private static final String TAG = "Gallery2/AlbumStoryPage";
    private static final int REQUEST_SLIDESHOW    = 1;
    private static final int REQUEST_DO_ANIMATION = 3;
    private static final int REQUEST_SET_COVER    = 4;

    // M: added for get content feature change
    private static final int REQUEST_CROP           = 100;
    private static final int REQUEST_CROP_WALLPAPER = 101;
    private static final int BIT_LOADING_RELOAD     = 1;
    private static final int BIT_LOADING_SYNC       = 2;

    private static final float USER_DISTANCE_METER = 0.3f;
    private static final int MSG_PICK_PHOTO = 0;
    protected SelectionManager mSelectionManager;
    private Future<?> mConvertUriTask;
    private boolean mIsActive = false;
    private AlbumTimeSlotRenderer mAlbumView;
    private Path                  mMediaSetPath;
    private DateSlotView          mSlotView;
    private AlbumDataLoader mAlbumDataAdapter;
    private   Vibrator         mVibrator;
    private boolean mGetContent;
    private ActionModeHandler mActionModeHandler;
    private int mFocusIndex = 0;
    private DetailsHelper   mDetailsHelper;
    private MyDetailsSource mDetailsSource;
    private MediaSet        mMediaSet;
    private boolean         mShowDetails;
    private float           mUserDistance; // in pixel
    private Future<Integer> mSyncTask = null;
    private boolean mLaunchedFromPhotoPage;
    private int     mLoadingBits   = 0;
    private boolean mInitialSynced = false;
    private int     mSyncResult;
    private boolean mLoadingFailed;
    private RelativePosition mOpenCenter = new RelativePosition();
    private Handler mHandler;
    //private PhotoFallbackEffect mResumeEffect;
    // save selection for onPause/onResume
    private boolean mNeedUpdateSelection = false;
    /// M: flag to specify whether mSelectionManager.restoreSelection task has done
    private boolean mRestoreSelectionDone;

    private GalleryActionBar mActionBar;

    private TextView mEmptyImg;
    private boolean mShowedEmpty     = false;
    private boolean mResumeSelection = false;

    private int mSlotViewPadding = 0;
    private final GLView mRootPane = new GLView() {
        private final float mMatrix[] = new float[16];

        @Override
        protected void render(GLCanvas canvas) {
            canvas.save(GLCanvas.SAVE_FLAG_MATRIX);
            canvas.multiplyMatrix(mMatrix, 0);
            super.render(canvas);
            canvas.restore();
        }

        @Override
        protected void onLayout(
                boolean changed, int left, int top, int right, int bottom) {

            int slotViewTop = top;//mActionBar.getHeight() + mActivity.mStatusBarHeight;
            int slotViewBottom = (int) (bottom - top
                    - mActivity.getResources().getDimension(R.dimen.navigation_bar_height)
                    - mActivity.getResources().getDimension(R.dimen.freeme_bottom_menu_container_height));
            int slotViewRight = right - left;

            if (mShowDetails) {
                mDetailsHelper.layout(left, slotViewTop, right, bottom);
            } else {
                mAlbumView.setHighlightItemPath(null);
            }

            // Set the mSlotView as a reference point to the open animation
            mOpenCenter.setReferencePosition(0, slotViewTop);
            mSlotView.layout(0, slotViewTop, slotViewRight, slotViewBottom - mSlotViewPadding );
            GalleryUtils.setViewPointMatrix(mMatrix,
                    (right - left) / 2, (bottom - top) / 2, -mUserDistance);
        }
    };
    private int mStoryBucketId = -1;
    //private int mStoryIndex = -1;
    private MediaItem mCoverItem;
    private boolean mNewAlbum = false;
    private SharedPreferences mSharedPref;
    //*/
    //*/ Added by droi Linguanrong for lock orientation, 16-3-1
    private OrientationManager mOrientationManager;
    // M: this holds the item picked in onGetContent
    private MediaItem mPickedItem;

    // This are the transitions we want:
    //
    // +--------+           +------------+    +-----------+
    // | Camera |---------->| Fullscreen |--->| AlbumTime |
    // |  View  | thumbnail |   Photo    | up | ShagtPage |
    // +--------+           +------------+    +-----------+
    //     ^                      |               |        
    //     |                      |               |               close
    //     +----------back--------+               +-------back->  app
    //
    @Override
    protected void onBackPressed() {
        if (mShowDetails) {
            hideDetails();
        } else if (mSelectionManager.inSelectionMode()) {
            mSelectionManager.leaveSelectionMode();
        } else {
            if (mLaunchedFromPhotoPage) {
                mActivity.getTransitionStore().putIfNotPresent(
                        PhotoPage.KEY_ALBUMPAGE_TRANSITION,
                        PhotoPage.MSG_ALBUMPAGE_RESUMED);
            }

            AlbumStorySetPage.mAddNewAlbum = false;

            super.onBackPressed();
        }
    }

    @Override
    protected void onStateResult(int request, int result, Intent data) {
        switch (request) {
            case REQUEST_SLIDESHOW: {
                // data could be null, if there is no images in the album
                if (data == null) return;
                mFocusIndex = data.getIntExtra(SlideshowPage.KEY_PHOTO_INDEX, 0);
                mSlotView.setCenterIndex(mFocusIndex);
                break;
            }
            case REQUEST_PHOTO: {
                if (data == null) return;
                mFocusIndex = data.getIntExtra(PhotoPage.KEY_RETURN_INDEX_HINT, 0);
                mSlotView.makeSlotVisible(mFocusIndex);
                break;
            }
            case REQUEST_DO_ANIMATION: {
                mSlotView.startRisingAnimation();
                break;
            }

            case REQUEST_SET_COVER:
                if (data != null) {
                    int select = data.getIntExtra(KEY_COVER_INDEX, 0);
                    if (FreemeUtils.STORY_DEBUG) {
                        LogUtil.i("connor", "REQUEST_SET_COVER select = " + select);
                    }
                    mCoverItem = mAlbumDataAdapter.get(select);
                    if (mMediaSet instanceof StoryMergeAlbum) {
                        ((StoryMergeAlbum) mMediaSet).setCover(select, mCoverItem);
                    } else {
                        ((StoryAlbum) mMediaSet).setCover(select, mCoverItem);
                    }
                    mSlotView.setCoverItem(mCoverItem);
                }
                break;

            // M: default case is added for MTK-specific pick-and-crop flow
            default:
                handleMtkCropResult(request, result, data);
        }
    }

    @Override
    protected int getBackgroundColorId() {
        return R.color.albumset_background;
    }

    private LinearLayout mFreemeHomeView;
    private TextView mFreemeActionBarBackTitle;
    private LinearLayout mFreemeTitleLayout;


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case com.freeme.gallery.R.id.up:
            case com.freeme.gallery.R.id.freeme_actionbar_back_title:
                onBackPressed();
                break;
            default:
                break;
        }
    }

    private static final int ACTION_CODE_ADD= 0x300;
    private static final int[] mActionNames = new int[]{R.string.add_images};
    private static final int[] mActionCodes = new int[]{ACTION_CODE_ADD};

    @Override
    protected void onCreate(Bundle data, Bundle restoreState) {
        super.onCreate(data, restoreState);
        mActivity.setTopbarBackgroundColor(R.color.transparent);
        mUserDistance = GalleryUtils.meterToPixel(USER_DISTANCE_METER);
        mActionBar = mActivity.getGalleryActionBar();
        //mStoryIndex = data.getInt(KEY_SELECT_INDEX, -1);
        mStoryBucketId = data.getInt(KEY_STORY_SELECT_INDEX, -1);
        mNewAlbum = data.getBoolean(KEY_NEW_ALBUM, false);
        initializeViews();
        initializeData(data);
        mSharedPref = mActivity.getSharedPreferences(
                FreemeUtils.STORY_SHAREPREFERENCE_KEY, Context.MODE_PRIVATE);


        mGetContent = data.getBoolean(GalleryActivity.KEY_GET_CONTENT, false);
        mDetailsSource = new MyDetailsSource();
        Context context = mActivity.getAndroidContext();
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        // Enable auto-select-all for mtp album
        if (data.getBoolean(KEY_AUTO_SELECT_ALL)) {
            mSelectionManager.selectAll();
        }

        // Modified by ZY Theobald_Wu on 20150330 [begin] that photopage change to abstract.
        mLaunchedFromPhotoPage =
                mActivity.getStateManager().hasStateClass(/*PhotoPage.class*/SinglePhotoPage.class);
        // Modified by ZY Theobald_Wu on 20150330 [end]

        mHandler = new SynchronizedHandler(mActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_PICK_PHOTO: {
                        pickPhoto(message.arg1);
                        break;
                    }
                    default:
                        throw new AssertionError(message.what);
                }
            }
        };


    }

    private FreemeBottomSelectedView.IFreemeBottomActionCallBack mCallBack
            = new FreemeBottomSelectedView.IFreemeBottomActionCallBack() {
        @Override
        public void onAction(int actionCode) {
            switch (actionCode) {
                case ACTION_CODE_ADD:
                    onMenuItemSelected(R.id.action_add);
                    break;
                default:
                    break;
            }
        }
    };
    @Override
    protected void onPause() {
        super.onPause();
        mIsActive = false;


        mActivity.mIsSelectionMode = mSelectionManager != null && mSelectionManager.inSelectionMode();
        if (mActivity.mIsSelectionMode) {
            mSelectionManager.leaveSelectionMode();
        }
        mAlbumView.setSlotFilter(null);

        mAlbumDataAdapter.pause();
        mAlbumView.pause();
        DetailsHelper.pause();
        mActionBar.disableClusterMenu(false);

        if (mSyncTask != null) {
            mSyncTask.cancel();
            mSyncTask = null;
            clearLoadingBit(BIT_LOADING_SYNC);
        }
        mActionModeHandler.pause();
        if (mSelectionManager != null && mSelectionManager.inSelectionMode()) {
            //mSelectionManager.saveSelection();
            mNeedUpdateSelection = false;
        }

        hideEmptyImg();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsActive = true;
        if (mActivity.mFreemeActionBarContainer != null) {
            mFreemeHomeView = mActivity.mFreemeActionBarContainer.findViewById(com.freeme.gallery.R.id.freeme_home_view);
            mFreemeHomeView.findViewById(com.freeme.gallery.R.id.up).setOnClickListener(this);
            mFreemeActionBarBackTitle = mFreemeHomeView.findViewById(com.freeme.gallery.R.id.freeme_actionbar_back_title);
            mFreemeActionBarBackTitle.setText(com.android.gallery3d.R.string.tab_by_story);
            mFreemeActionBarBackTitle.setOnClickListener(this);
            mFreemeTitleLayout = mActivity.mFreemeActionBarContainer.findViewById(com.freeme.gallery.R.id.freeme_title_layout);
            TextView mActionbarTitle = mFreemeTitleLayout.findViewById(R.id.action_bar_title);
            if (mNewAlbum) {
                mActionbarTitle.setText(mSharedPref.getString(StoryAlbumSet.ALBUM_KEY + mStoryBucketId, ""));
            } else {
                mActionbarTitle.setText(mMediaSet.getName());
            }
            mActivity.setTopbarBackgroundColor(com.android.gallery3d.R.color.primary_freeme_light);

        }
        mActivity.getNavigationWidgetManager().changeStateTo(this);
        //*/ Added by droi Linguanrong for freeme gallery, 16-1-13
        ((GalleryActivity) mActivity).setBottomTabVisibility(false);
        //*/

        //*/ Added by droi Linguanrong for lock orientation, 16-3-1
        mOrientationManager.lockOrientation(true);
        //*/

        setContentPane(mRootPane);
        // Set the reload bit here to prevent it exit this page in clearLoadingBit().
        setLoadingBit(BIT_LOADING_RELOAD);
        mLoadingFailed = false;
        mResumeSelection = false;
        if (mSelectionManager != null && mSelectionManager.inSelectionMode()) {
            mNeedUpdateSelection = true;
            /// M: set mRestoreSelectionDone as false if we need to retore selection
            mRestoreSelectionDone = false;

            mResumeSelection = true;
            //mSelectionManager.leaveSelectionMode();
            //mSelectionManager.enterSelectionMode();
        } else {
            /// M: set mRestoreSelectionDone as true there is no need to retore selection
            mRestoreSelectionDone = true;
        }
        mAlbumDataAdapter.resume();

        mAlbumView.resume();
        mAlbumView.setPressedIndex(-1);
        mActionModeHandler.resume();
        if (!mInitialSynced) {
            setLoadingBit(BIT_LOADING_SYNC);
            mSyncTask = mMediaSet.requestSync(this);
        }

        if (mMediaSet != null && mMediaSet.getMediaItemCount() != 0) {
            hideEmptyImg();
        }
        ((GalleryActivity)mActivity).getController().showActions(mActionNames, mActionCodes, mCallBack);
        ((GalleryActivity)mActivity).getController().updateActionEnabled(true, ACTION_CODE_ADD);
    }

    @Override
    protected boolean onCreateActionBar(Menu menu) {
//        MenuInflater inflator = getSupportMenuInflater();

//        mActionBar.setDisplayOptions(true, true);
        if (mGetContent) {
            mActionBar.createActionBarMenu(R.menu.pickup, menu);
        } else {
            mActionBar.createActionBarMenu(R.menu.album_story, menu);

            menu.findItem(R.id.action_slideshow).setVisible(mMediaSet.getMediaItemCount() != 0);

//            MenuItem shareFreemeOS = menu.findItem(R.id.action_share_freeme);
//            if(shareFreemeOS != null) {
//                String title = mActivity.getResources().getString(R.string.share)
//                        + " " + BuildConfig.SUPPORT_OS_TAG;
//                shareFreemeOS.setTitle(title);
//            }

            FilterUtils.setupMenuItems(mActionBar, mMediaSetPath, true);
        }

        if (mNewAlbum) {
            mActionBar.setTitle(mSharedPref.getString(StoryAlbumSet.ALBUM_KEY + mStoryBucketId, ""));
        } else {
            mActionBar.setTitle(mMediaSet.getName());
        }
        mActionBar.setSubtitle(null);

        //*/ Added by Linguanrong for set menu item visible, 2015-4-24
        MenuItem item = menu.findItem(R.id.action_select);
        if (item != null) {
            item.setVisible(mMediaSet != null && mMediaSet.getMediaItemCount() > 0);
        }
        //*/

        return true;
    }

    protected boolean onItemSelected(MenuItem item, int menuItemid) {
        if (item != null) {
            return onItemSelected(item);
        } else {
            return onMenuItemSelected(menuItemid);
        }
    }

    @Override
    protected boolean onItemSelected(MenuItem item) {
        return onMenuItemSelected(item.getItemId());
    }

    private boolean onMenuItemSelected(int menuItemid) {

        switch (menuItemid) {
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.action_cancel:
                mActivity.getStateManager().finishState(this);
                return true;

            case R.id.action_select:
                mSelectionManager.setAutoLeaveSelectionMode(false);
                mSelectionManager.enterSelectionMode();
                return true;

            case R.id.action_slideshow: {
                //*/ Added by tyd Linguanrong for statistic, 15-12-18
//                StatisticUtil.generateStatisticInfo(mActivity, StatisticData.OPTION_SLIDESHOW);
                //*/
                // for baas analytics
//                DroiAnalytics.onEvent(mActivity, StatisticData.OPTION_SLIDESHOW);

                Bundle data = new Bundle();
                data.putString(SlideshowPage.KEY_SET_PATH,
                        mMediaSetPath.toString());
                data.putBoolean(SlideshowPage.KEY_REPEAT, true);
                mActivity.getStateManager().startStateForResult(
                        SlideshowPage.class, REQUEST_SLIDESHOW, data);
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

            case R.id.action_add:
                startStoryAddImagePage();
                return true;
//
//            case R.id.action_setcover:
//                int select = Math.max(0, mDetailsSource.setIndex());
//                if (FreemeUtils.STORY_DEBUG) {
//                    LogUtil.i("connor", "action_setcover select = " + select);
//                }
//                mCoverItem = mAlbumDataAdapter.get(select);
//                if (mMediaSet instanceof StoryMergeAlbum) {
//                    ((StoryMergeAlbum) mMediaSet).setCover(select, mCoverItem);
//                } else {
//                    ((StoryAlbum) mMediaSet).setCover(select, mCoverItem);
//                }
//                mSlotView.setCoverItem(mCoverItem);
//                return true;
//            case R.id.action_share_freeme:
//                ShareFreemeUtil.shareFreemeOS(mActivity);
//                return true;
            default:
                return false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        /*/ Added by droi Linguanrong for lock orientation, 16-3-1
        mOrientationManager.unlockOrientation();
        //*/

        if (mAlbumDataAdapter != null) {
            mAlbumDataAdapter.setLoadingListener(null);
        }

        //*/ Added by Tyd Linguanrong for Gallery new style, 2014-4-24
        cleanupEmptyImg();
        //*/
    }

    private void cleanupEmptyImg() {
        if (mEmptyImg == null) return;
        RelativeLayout galleryRoot = (RelativeLayout) mActivity
                .findViewById(R.id.gallery_root);
        if (galleryRoot == null) return;
        galleryRoot.removeView(mEmptyImg);
        mEmptyImg = null;
    }

    private void setLoadingBit(int loadTaskBit) {
        mLoadingBits |= loadTaskBit;
    }

    private void hideEmptyImg() {
        if (mEmptyImg == null) return;
        mEmptyImg.setVisibility(View.GONE);
    }

    private void clearLoadingBit(int loadTaskBit) {
        mLoadingBits &= ~loadTaskBit;

        if (mShowedEmpty) {
            mShowedEmpty = false;
            hideEmptyImg();
        }

        if (mLoadingBits == 0 && mIsActive) {
            if (mAlbumDataAdapter.size() == 0) {
                mShowedEmpty = true;
                mSlotView.invalidate();
                showEmptyImg();
            }
        }
    }

    private void showEmptyImg() {
        if (mEmptyImg == null && !setupEmptyImg()) return;
        mEmptyImg.setVisibility(View.VISIBLE);
    }

    private boolean setupEmptyImg() {
        RelativeLayout galleryRoot = (RelativeLayout) mActivity
                .findViewById(R.id.gallery_root);
        if (galleryRoot == null) return false;

        if (mEmptyImg == null) {
            mEmptyImg = new TextView(mActivity);
            boolean international = FreemeUtils.isInternational(mActivity);
            switch (mStoryBucketId) {
                case StoryAlbumSet.ALBUM_BABY_ID:
                    mEmptyImg.setBackgroundResource(international ?
                            R.drawable.empty_img_baby_en : R.drawable.empty_img_baby);
                    break;

                case StoryAlbumSet.ALBUM_LOVE_ID:
                    mEmptyImg.setBackgroundResource(international ?
                            R.drawable.empty_img_love_en : R.drawable.empty_img_love);
                    break;

                default:
                    mEmptyImg.setBackgroundResource(international ?
                            R.drawable.empty_img_normal_en : R.drawable.empty_img_normal);
                    break;
            }
            //mEmptyImg.setTextColor(csl);
            //mEmptyImg.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_camera, 0, 0);
        }

        Display display = mActivity.getWindowManager().getDefaultDisplay();
        int screenHeight = display.getHeight();

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        lp.topMargin = screenHeight / 3;
        galleryRoot.addView(mEmptyImg, lp);

        return true;
    }

    private void handleMtkCropResult(int request, int result, Intent data) {
        LogUtil.d(TAG, "handleMtkCropFlow: request=" + request + ", result=" + result +
                ", dataString=" + (data != null ? data.getDataString() : "null"));
        switch (request) {
            case REQUEST_CROP:
            /* Fall through */
            case REQUEST_CROP_WALLPAPER:
                if (result == Activity.RESULT_OK) {
                    // M: as long as the result is OK, we just setResult and finish
                    Activity activity = mActivity;
                    // M: if data does not contain uri, we add the one we pick;
                    // otherwise don't modify data
                    if (data != null && mPickedItem != null) {
                        data.setDataAndType(mPickedItem.getContentUri(), data.getType());
                    }
                    activity.setResult(Activity.RESULT_OK, data);
                    activity.finish();
                }
                break;
            default:
                LogUtil.i(TAG, "unknown MTK crop request!!");
        }
    }

    private void onDown(int index) {
        mAlbumView.setPressedIndex(index);
    }

    private void onUp(boolean followedByLongPress) {
        if (followedByLongPress) {
            // Avoid showing press-up animations for long-press.
            mAlbumView.setPressedIndex(-1);
        } else {
            mAlbumView.setPressedUp();
        }
    }

    private void onSingleTapUp(int slotIndex) {
        if (!mIsActive) return;

        if (mSelectionManager.inSelectionMode()) {
            MediaItem item = mAlbumDataAdapter.get(slotIndex);
            if (item == null) return; // Item not ready yet, ignore the click
            mSelectionManager.toggle(item.getPath());
            updateMenuSetCover();
            mSlotView.invalidate();
            mSlotView.adjustSelection(slotIndex);
        } else {
            // Render transition in pressed state
            mAlbumView.setPressedIndex(slotIndex);
            mAlbumView.setPressedUp();
            mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_PICK_PHOTO, slotIndex, 0),
                    FadeTexture.DURATION);
        }
    }

    private void pickPhoto(int slotIndex) {
        pickPhoto(slotIndex, false);
    }

    private void pickPhoto(int slotIndex, boolean startInFilmstrip) {
        if (!mIsActive) return;

        if (!startInFilmstrip) {
            // Launch photos in lights out mode
            mActivity.getGLRoot().setLightsOutMode(true);
        }

        MediaItem item = mAlbumDataAdapter.get(slotIndex);
        if (item == null) return; // Item not ready yet, ignore the click
        if (mGetContent) {
            onGetContent(item);
        } else if (mLaunchedFromPhotoPage) {
            TransitionStore transitions = mActivity.getTransitionStore();
            transitions.put(
                    PhotoPage.KEY_ALBUMPAGE_TRANSITION,
                    PhotoPage.MSG_ALBUMPAGE_PICKED);
            transitions.put(PhotoPage.KEY_INDEX_HINT, slotIndex);
            onBackPressed();
        } else {
            //*/ Added by Linguanrong for play video directly, 2015-6-19
            if (!startInFilmstrip && canBePlayed(item)) {
                FreemeUtils.playVideo(mActivity, item.getPlayUri(), item.getMimeType(), item.getName());
                return;
            }
            //*/
            // Get into the PhotoPage.
            // mAlbumView.savePositions(PositionRepository.getInstance(mActivity));
            Bundle data = new Bundle();
            data.putInt(PhotoPage.KEY_INDEX_HINT, slotIndex);
            data.putParcelable(PhotoPage.KEY_OPEN_ANIMATION_RECT,
                    mSlotView.getSlotRect(slotIndex, mRootPane));
            data.putString(PhotoPage.KEY_MEDIA_SET_PATH,
                    mMediaSetPath.toString());
            data.putString(PhotoPage.KEY_MEDIA_ITEM_PATH,
                    item.getPath().toString());
            data.putInt(PhotoPage.KEY_ALBUMPAGE_TRANSITION,
                    PhotoPage.MSG_ALBUMPAGE_STARTED);
            data.putBoolean(PhotoPage.KEY_START_IN_FILMSTRIP,
                    startInFilmstrip);
            data.putBoolean(PhotoPage.KEY_IN_CAMERA_ROLL, mMediaSet.isCameraRoll());
            // Modified by ZY Theobald_Wu on 20150330 [begin] that photopage change to abstract.
            if (startInFilmstrip) {
                mActivity.getStateManager().switchState(this, FilmstripPage.class, data);
            } else {
                mActivity.getStateManager().startStateForResult(
                        SinglePhotoPage.class, REQUEST_PHOTO, data);
            }
            // Modified by ZY Theobald_Wu on 20150330 [end]
        }
    }

    private boolean canBePlayed(MediaItem item) {
        int supported = item.getSupportedOperations();
        return ((supported & MediaItem.SUPPORT_PLAY) != 0);
    }

    private void onGetContent(final MediaItem item) {
        DataManager dm = mActivity.getDataManager();
        Activity activity = mActivity;

        Intent intent = new Intent(null, item.getContentUri())
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.setResult(Activity.RESULT_OK, intent);
        activity.finish();
    }

    public void onLongTap(int slotIndex) {
        if (mGetContent) return;
        MediaItem item = mAlbumDataAdapter.get(slotIndex);
        if (item == null) return;
        mSelectionManager.setAutoLeaveSelectionMode(true);
        mSelectionManager.toggle(item.getPath());
        updateMenuSetCover();
        mSlotView.invalidate();
        mSlotView.adjustSelection(slotIndex);
    }

    @Override
    public void doCluster(int clusterType) {
        if (FreemeUtils.CLUSTER_BY_CAMERE == clusterType) {
            return;
        } else if (FreemeUtils.CLUSTER_BY_STORY == clusterType) {
            if (mSelectionManager != null && mSelectionManager.inSelectionMode()) {
                mSelectionManager.leaveSelectionMode();
            }
            Bundle data = new Bundle();
            data.putString(AlbumStorySetPage.KEY_MEDIA_PATH, StoryAlbumSet.PATH.toString());
            data.putInt(AlbumStorySetPage.KEY_SELECTED_CLUSTER_TYPE, clusterType);
            mActivity.getStateManager().switchState(this, AlbumStorySetPage.class, data);
        } else {
            if (mSelectionManager != null && mSelectionManager.inSelectionMode()) {
                mSelectionManager.leaveSelectionMode();
            }
            String basePath = mActivity.getDataManager()
                    .getTopSetPath(DataManager.INCLUDE_ALL);
            String newPath = FilterUtils.switchClusterPath(basePath, clusterType);
            Bundle data = new Bundle(getData());
            data.putString(AlbumSetPage.KEY_MEDIA_PATH, newPath);
            data.putInt(AlbumSetPage.KEY_SELECTED_CLUSTER_TYPE, clusterType);
            mActivity.getStateManager().switchState(this, AlbumSetPage.class, data);
        }
    }

    private void initializeViews() {
        mSelectionManager = new SelectionManager(mActivity, false);
        mSelectionManager.setSelectionListener(this);
        PageConfig.AlbumStoryPage config = PageConfig.AlbumStoryPage.get(mActivity);
        mSlotView = new DateSlotView(mActivity, mSelectionManager,
                config.slotViewSpec, config.labelSpec);
        mSlotView.initStoryRender(this, mActivity.getAndroidContext(), mStoryBucketId);
        mSlotViewPadding = config.slotViewSpec.slotPaddingV + mSlotView.mMore.getHeight()
                + config.slotViewSpec.bottomPadding * 2;
        mAlbumView = new AlbumTimeSlotRenderer(mActivity, mSlotView,
                mSelectionManager, config.placeholderColor);
        mSlotView.setSlotRenderer(mAlbumView);
        mRootPane.addComponent(mSlotView);
        mSlotView.setListener(new DateSlotView.SimpleListener() {
            @Override
            public void onDown(int index) {
                AlbumStoryPage.this.onDown(index);
            }

            @Override
            public void onUp(boolean followedByLongPress) {
                AlbumStoryPage.this.onUp(followedByLongPress);
            }

            @Override
            public void onSingleTapUp(int slotIndex) {
                AlbumStoryPage.this.onSingleTapUp(slotIndex);
            }

            @Override
            public void onLongTap(int slotIndex) {
                AlbumStoryPage.this.onLongTap(slotIndex);
            }
        });
        mActionModeHandler = new ActionModeHandler(mActivity, mSelectionManager);
        mActionModeHandler.setActionModeListener(new ActionModeHandler.ActionModeListener() {
            @Override
            public boolean onActionItemClicked(MenuItem item,int menuItemid) {
                return onItemSelected(item, menuItemid);
            }
        });

        //*/ Added by droi Linguanrong for lock orientation, 16-3-1
        mOrientationManager = mActivity.getOrientationManager();
        mActivity.getGLRoot().setOrientationSource(mOrientationManager);
        //*/
    }

    private void initializeData(Bundle data) {
        mMediaSetPath = Path.fromString(data.getString(KEY_MEDIA_PATH));
        mMediaSet = mActivity.getDataManager().getMediaSet(mMediaSetPath);
        if (mMediaSet == null) {
            //Utils.fail("MediaSet is null. Path = %s", mMediaSetPath);
            doCluster(FreemeUtils.CLUSTER_BY_STORY);
        }
        mSlotView.setData(mActivity, mMediaSet);
        mSelectionManager.setSourceMediaSet(mMediaSet);
        mAlbumDataAdapter = new AlbumDataLoader(mActivity, mMediaSet);
        mAlbumDataAdapter.setLoadingListener(new MyLoadingListener());
        mAlbumView.setModel(mAlbumDataAdapter);
    }

    private void showDetails() {
        mShowDetails = true;
        if (mDetailsHelper == null) {
            mDetailsHelper = new DetailsHelper(mActivity, mRootPane, mDetailsSource);
            mDetailsHelper.setCloseListener(new DetailsHelper.CloseListener() {
                @Override
                public void onClose() {
                    hideDetails();
                }
            });
        }
        mDetailsHelper.show();
    }

    private void hideDetails() {
        mShowDetails = false;
        mDetailsHelper.hide();
        mAlbumView.setHighlightItemPath(null);
        mSlotView.invalidate();
    }

    private void updateMenuSetCover() {
        MenuExecutor.updateMenuSetCover(mActionBar.getMenu(),
                mSelectionManager.getSelectedCount() == 1);
    }

    public void goToSelectCover() {
        ((GalleryActivity)mActivity).getController().hideActions();
        if (mAlbumDataAdapter.size() > 0) {
            Bundle data = new Bundle();
            data.putString(AlbumStoryCoverPage.KEY_MEDIA_PATH, mMediaSetPath.toString());
            data.putBoolean(GalleryActivity.KEY_GET_CONTENT, true);
            data.putInt(AlbumStoryCoverPage.KEY_SELECT_INDEX,
                    mCoverItem != null ? mAlbumDataAdapter.findItem(mCoverItem.getPath()) : 0);
            data.putInt(AlbumStoryCoverPage.KEY_STORY_SELECT_INDEX, mStoryBucketId);
            mActivity.getStateManager().startStateForResult(
                    AlbumStoryCoverPage.class, REQUEST_SET_COVER, data);
        }
    }

    private void startStoryAddImagePage() {
        Bundle data = new Bundle();
        data.putBoolean(GalleryActivity.KEY_GET_CONTENT, true);
        data.putBoolean(AlbumSetPage.KEY_STORY_SELECT_MODE, true);
        data.putBoolean(AlbumSetPage.KEY_STORY_FROM_CHILD, true);
        data.putInt(AlbumSetPage.KEY_STORY_SELECT_INDEX, mStoryBucketId);
        data.putString(AlbumSetPage.KEY_MEDIA_PATH,
                mActivity.getDataManager().getTopSetPath(DataManager.INCLUDE_ALL));
        mActivity.getStateManager().startState(AlbumSetPage.class, data);
    }

    @Override
    public void onSelectionModeChange(int mode) {
        switch (mode) {
            case SelectionManager.ENTER_SELECTION_MODE: {
                mSlotView.setInSelectionMode(true);
                mActionModeHandler.startActionMode();
                if (!mResumeSelection) {
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                }
                mResumeSelection = false;
                break;
            }
            case SelectionManager.LEAVE_SELECTION_MODE: {
                mSlotView.setInSelectionMode(false);
                mSlotView.updateSelection(false);
                mActionModeHandler.finishActionMode();
                mRootPane.invalidate();
                break;
            }

            case SelectionManager.DESELECT_ALL_MODE:
            case SelectionManager.SELECT_ALL_MODE: {
                mSlotView.updateSelection(mode == SelectionManager.SELECT_ALL_MODE);
                mActionModeHandler.updateSupportedOperation();
                mRootPane.invalidate();
                break;
            }
        }
    }

    @Override
    public void onSelectionChange(Path path, boolean selected) {
        int count = mSelectionManager.getSelectedCount();
        String format = mActivity.getResources().getQuantityString(
                R.plurals.number_of_items_selected, count);
        mActionModeHandler.setTitle(String.format(format, count));
        mActionModeHandler.updateSupportedOperation(path, selected);
    }

    @Override
    public void onSelectionRestoreDone() {

    }

    @Override
    public void onSyncDone(final MediaSet mediaSet, final int resultCode) {
        LogUtil.d(TAG, "onSyncDone: " + Utils.maskDebugInfo(mediaSet.getName()) + " result="
                + resultCode);
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                GLRoot root = mActivity.getGLRoot();
                root.lockRenderThread();
                mSyncResult = resultCode;
                try {
                    if (resultCode == MediaSet.SYNC_RESULT_SUCCESS) {
                        mInitialSynced = true;
                    }
                    clearLoadingBit(BIT_LOADING_SYNC);
                    showSyncErrorIfNecessary(mLoadingFailed);
                } finally {
                    root.unlockRenderThread();
                }
            }
        });
    }

    // Show sync error toast when all the following conditions are met:
    // (1) both loading and sync are done,
    // (2) sync result is error,
    // (3) the page is still active, and
    // (4) no photo is shown or loading fails.
    private void showSyncErrorIfNecessary(boolean loadingFailed) {
        if ((mLoadingBits == 0) && (mSyncResult == MediaSet.SYNC_RESULT_ERROR) && mIsActive
                && (loadingFailed || (mAlbumDataAdapter.size() == 0))) {
            Toast.makeText(mActivity, R.string.sync_album_error,
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onEnterState() {
        mActivity.showNavi(AbstractGalleryActivity.IN_STORYPAGE);
    }

    @Override
    public void observe() {

    }

    private class MyLoadingListener implements LoadingListener {
        @Override
        public void onLoadingStarted() {
            setLoadingBit(BIT_LOADING_RELOAD);
            mLoadingFailed = false;
        }

        @Override
        public void onLoadingFinished(boolean loadingFailed) {
            clearLoadingBit(BIT_LOADING_RELOAD);
            mLoadingFailed = loadingFailed;
            showSyncErrorIfNecessary(loadingFailed);

            // M: we have to notify SelectionManager about data change,
            // and this is the most proper place we could find till now
            boolean inSelectionMode = (mSelectionManager != null && mSelectionManager.inSelectionMode());
            int itemCount = mMediaSet != null ? mMediaSet.getMediaItemCount() : 0;
            LogUtil.d(TAG, "onLoadingFinished: item count=" + itemCount);
            //mSelectionManager.onSourceContentChanged();
            boolean restore = false;
            if (itemCount > 0 && inSelectionMode) {
                if (mNeedUpdateSelection) {
                    mNeedUpdateSelection = false;
                    restore = true;
                    //mSelectionManager.restoreSelection();
                }
                mActionModeHandler.updateSupportedOperation();
                //mActionModeHandler.updateSelectionMenu();
            }

            if (!inSelectionMode) {
                mActivity.invalidateOptionsMenu();
            }

            if (!restore) {
                mRestoreSelectionDone = true;
            }

            if (mMediaSet instanceof StoryMergeAlbum) {
                if (mCoverItem != null) {
                    int index = mAlbumDataAdapter.findItem(mCoverItem.getPath());
                    if (FreemeUtils.STORY_DEBUG) {
                        LogUtil.i("connor", "StoryMergeAlbum mCoverItem index = " + index);
                    }
                    if (index == -1) {
                        index = 0;
                        mCoverItem = null;
                    }
                    ((StoryMergeAlbum) mMediaSet).setCover(index, mCoverItem);

                    if (mCoverItem == null) {
                        mCoverItem = ((StoryMergeAlbum) mMediaSet).getCover();
                    }
                } else {
                    mCoverItem = ((StoryMergeAlbum) mMediaSet).getCover();
                    if (FreemeUtils.STORY_DEBUG) {
                        LogUtil.i("connor", "StoryMergeAlbum mCoverItem = " + mCoverItem);
                    }
                    ((StoryMergeAlbum) mMediaSet).setCover(mCoverItem);
                }
            } else {
                if (mCoverItem != null) {
                    int index = Math.max(0, mAlbumDataAdapter.findItem(mCoverItem.getPath()));
                    if (FreemeUtils.STORY_DEBUG) {
                        LogUtil.i("connor", "StoryAlbum mCoverItem index = " + index);
                    }
                    if (index == -1) {
                        index = 0;
                        mCoverItem = null;
                    }
                    ((StoryAlbum) mMediaSet).setCover(index, mCoverItem);

                    if (mCoverItem == null) {
                        mCoverItem = ((StoryAlbum) mMediaSet).getCover();
                    }
                } else {
                    mCoverItem = ((StoryAlbum) mMediaSet).getCover();
                    if (FreemeUtils.STORY_DEBUG) {
                        LogUtil.i("connor", "StoryAlbum mCoverItem = " + mCoverItem);
                    }
                    ((StoryAlbum) mMediaSet).setCover(mCoverItem);
                }
            }

            mSlotView.setCoverItem(mCoverItem);
            mSlotView.invalidate();
        }
    }

    private class MyDetailsSource implements DetailsHelper.DetailsSource {
        private int mIndex;

        @Override
        public boolean isCamera() {
            return false;
        }

        @Override
        public int size() {
            return mAlbumDataAdapter.size();
        }

        @Override
        public int setIndex() {
            Path id = mSelectionManager.getSelected(false).get(0);
            mIndex = mAlbumDataAdapter.findItem(id);
            return mIndex;
        }

        @Override
        public MediaDetails getDetails() {
            // this relies on setIndex() being called beforehand
            MediaObject item = mAlbumDataAdapter.get(mIndex);
            if (item != null) {
                mAlbumView.setHighlightItemPath(item.getPath());
                return item.getDetails();
            } else {
                return null;
            }
        }
    }
}

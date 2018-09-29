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

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.gallery3d.R;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.ClusterAlbum;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.LocalImage;
import com.android.gallery3d.data.MediaDetails;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
//import com.android.gallery3d.filtershow.crop.CropActivity;
import com.android.gallery3d.filtershow.crop.CropExtras;
import com.android.gallery3d.glrenderer.FadeTexture;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.ui.ActionModeHandler;
import com.android.gallery3d.ui.ActionModeHandler.ActionModeListener;
import com.android.gallery3d.ui.AlbumSlotRenderer;
import com.android.gallery3d.ui.DetailsHelper;
import com.android.gallery3d.ui.DetailsHelper.CloseListener;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.ui.PhotoFallbackEffect;
import com.android.gallery3d.ui.RelativePosition;
import com.android.gallery3d.ui.SelectionManager;
import com.android.gallery3d.ui.SlotView;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.MediaSetUtils;
import com.droi.sdk.analytics.DroiAnalytics;
import com.freeme.data.StoryAlbum;
import com.freeme.data.VisitorAlbum;
import com.freeme.data.VisitorAlbumVideo;
import com.freeme.extern.SecretMenuHandler;
import com.freeme.gallery.GalleryClassifierService;
import com.freeme.gallery.app.AbstractGalleryActivity;
import com.freeme.gallery.app.GalleryActivity;
import com.freeme.gallery.app.MovieActivity;
import com.freeme.gallery.filtershow.crop.CropActivity;
import com.freeme.jigsaw.app.JigsawEntry;
import com.freeme.page.AlbumStoryPage;
import com.freeme.ui.manager.State;
import com.mediatek.gallery3d.util.PermissionHelper;
import java.util.ArrayList;

import com.freeme.provider.GalleryStore;
import com.mediatek.galleryframework.util.DebugUtils;

public class AlbumClassifierPage extends ActivityState implements GalleryActionBar.ClusterRunner,
        SelectionManager.SelectionListener, MediaSet.SyncListener, GalleryActionBar.OnAlbumModeSelectedListener
        , SecretMenuHandler.MenuListener,State , View.OnClickListener{
    public static final String KEY_MEDIA_PATH = "media-path";
    public static final String KEY_PARENT_MEDIA_PATH = "parent-media-path";
    public static final String KEY_SET_CENTER = "set-center";
    public static final String KEY_AUTO_SELECT_ALL = "auto-select-all";
    public static final String KEY_SHOW_CLUSTER_MENU = "cluster-menu";
    public static final String KEY_EMPTY_ALBUM = "empty-album";
    public static final String KEY_RESUME_ANIMATION = "resume_animation";
    //*/ Added by Tyd Linguanrong for secret photos, 2014-5-29
    public static final String KEY_VISITOR_MODE       = "visitor-mode";
    public static final String KEY_VISITOR_TYPE       = "visitor-type";
    //*/
    //*/ Added by Linguanrong for story album, 2015-4-9
    public static final String KEY_STORY_SELECT_MODE  = "story-select-mode";
    public static final String KEY_STORY_SELECT_INDEX = "story-select-index";
    public static final String KEY_STORY_FROM_CHILD   = "story-from-child";
    public static final  int REQUEST_PHOTO        = 2;
    //*/
    @SuppressWarnings("unused")
    private static final String TAG = "AlbumClassifierPage";
    private static final int REQUEST_SLIDESHOW    = 1;
    private static final int REQUEST_DO_ANIMATION = 3;

    private static final int BIT_LOADING_RELOAD = 1;
    private static final int BIT_LOADING_SYNC = 2;

    private static final float USER_DISTANCE_METER = 0.3f;
    private static final int MSG_PICK_PHOTO = 0;
    protected SelectionManager mSelectionManager;
    private boolean mIsActive = false;
    private AlbumSlotRenderer mAlbumView;
    private Path              mMediaSetPath;
    private String            mParentMediaSetString;
    private SlotView          mSlotView;
    private AlbumDataLoader mAlbumDataAdapter;
    private boolean mGetContent;
    private boolean mShowClusterMenu;

    private ActionModeHandler mActionModeHandler;
    private int mFocusIndex = 0;
    private DetailsHelper mDetailsHelper;
    private MyDetailsSource mDetailsSource;
    private MediaSet mMediaSet;
    private boolean mShowDetails;
    private float mUserDistance; // in pixel
    private Future<Integer> mSyncTask = null;
    private boolean mLaunchedFromPhotoPage;
    private boolean mInCameraApp;
    private boolean mInCameraAndWantQuitOnPause;

    private int mLoadingBits = 0;
    private boolean mInitialSynced = false;
    private int mSyncResult;
    private boolean mLoadingFailed;
    private RelativePosition mOpenCenter = new RelativePosition();
    private Handler mHandler;
    //*/Added by droi Linguanrong for Gallery new style, 2013-12-19
    private int mSlotViewPadding = 0;
    //*/

    private PhotoFallbackEffect mResumeEffect;

    //*/ Added by Tyd Linguanrong for secret photos, 2014-2-17
    private boolean           mAutoSelectAll;
    private boolean           mVisitorMode;
    private boolean           mIsSecretImages;
    private SecretMenuHandler mSecretMenu;
    //*/
    //*/ Added by Linguanrong for story album, 2015-4-9
    private boolean           mStorySelectMode;
    private boolean           mStoryFromChild;
    private int mStoryBucketId   = -1;
    private int mStoryActionType = -1;
    private Button mConfirm;
    //*/

    // Added by TYD Theobald_Wu on 2014/01 [begin] for jigsaw feature
    private boolean mJigsawPicker;
    // Added by TYD Theobald_Wu on 2014/01 [end]
 
    private PhotoFallbackEffect.PositionProvider mPositionProvider =
            new PhotoFallbackEffect.PositionProvider() {
        @Override
        public Rect getPosition(int index) {
            Rect rect = mSlotView.getSlotRect(index);
            Rect bounds = mSlotView.bounds();
            rect.offset(bounds.left - mSlotView.getScrollX(),
                    bounds.top - mSlotView.getScrollY());
            return rect;
        }

        @Override
        public int getItemIndex(Path path) {
            int start = mSlotView.getVisibleStart();
            int end = mSlotView.getVisibleEnd();
            for (int i = start; i < end; ++i) {
                MediaItem item = mAlbumDataAdapter.get(i);
                if (item != null && item.getPath() == path) return i;
            }
            return -1;
        }
    };
    private LinearLayout mFreemeHomeView;
    private TextView mFreemeActionBarBackTitle;
    private LinearLayout mFreemeTitleLayout;

    @Override
    protected int getBackgroundColorId() {
        return R.color.album_background;
    }

    private final GLView mRootPane = new GLView() {
        private final float mMatrix[] = new float[16];

        @Override
        protected void onLayout(
                boolean changed, int left, int top, int right, int bottom) {

            //*/ Modified by droi Linguanrong for adjust glroot view layout, 2014-6-12
            int slotViewTop = mActivity.getGalleryActionBar().getHeight() + mActivity.mStatusBarHeight + 6;
            //*/
            int slotViewBottom = (int) (bottom - top
                                - mActivity.getResources().getDimension(com.freeme.gallery.R.dimen.navigation_bar_height));
            int slotViewRight = right - left;

            //*/ Added by TYD Theobald_Wu on 2014/01 [begin] for jigsaw feature
            if (mJigsawPicker && mActivity instanceof JigsawEntry) {
                JigsawEntry jigsaw = (JigsawEntry) mActivity;
                slotViewTop = mActivity.getGalleryActionBar().getHeight();
                slotViewBottom = slotViewBottom - jigsaw.getBottomCtrlHeight();
            }
            // Added by TYD Theobald_Wu on 2014/01 [end] */

            if (mShowDetails) {
                mDetailsHelper.layout(left, slotViewTop, right, bottom);
            } else {
                mAlbumView.setHighlightItemPath(null);
            }

            // Set the mSlotView as a reference point to the open animation
            mOpenCenter.setReferencePosition(0, slotViewTop);
            //*/Modified by droi Linguanrong for Gallery new style, 2013-12-19
            int layout_bottom = slotViewBottom - mSlotViewPadding;
            if (mVisitorMode || mStorySelectMode) {
                layout_bottom -= mActivity.getGalleryActionBar().getHeight();
            }
            mSlotView.layout(0, slotViewTop, slotViewRight, layout_bottom);
            //*/
            GalleryUtils.setViewPointMatrix(mMatrix,
                    (right - left) / 2, (bottom - top) / 2, -mUserDistance);
        }

        @Override
        protected void render(GLCanvas canvas) {
            canvas.save(GLCanvas.SAVE_FLAG_MATRIX);
            canvas.multiplyMatrix(mMatrix, 0);
            super.render(canvas);

            if (mResumeEffect != null) {
                boolean more = mResumeEffect.draw(canvas);
                if (!more) {
                    mResumeEffect = null;
                    mAlbumView.setSlotFilter(null);
                }
                // We want to render one more time even when no more effect
                // required. So that the animated thumbnails could be draw
                // with declarations in super.render().
                invalidate();
            }
            canvas.restore();
        }
    };

    //*/ Added by Tyd Linguanrong for secret photos, 2014-2-17
    @Override
    public boolean canShowButton(int id) {
        boolean mEditMode = mSelectionManager.inSelectionMode();
        //*/ Modified by Linguanrong for story album, 2015-5-28
        return (mVisitorMode || mStorySelectMode) && mEditMode;
        //*/
    }    // This are the transitions we want:
    //
    // +--------+           +------------+    +-------+    +----------+
    // | Camera |---------->| Fullscreen |--->| Album |--->| AlbumSet |
    // |  View  | thumbnail |   Photo    | up | Page  | up |   Page   |
    // +--------+           +------------+    +-------+    +----------+
    //     ^                      |               |            ^  |
    //     |                      |               |            |  |         close
    //     +----------back--------+               +----back----+  +--back->  app
    //
    @Override
    protected void onBackPressed() {
        if (mShowDetails) {
            hideDetails();
        } else if (mSelectionManager.inSelectionMode()) {
            mSelectionManager.leaveSelectionMode();
            //*/ Added by Tyd Linguanrong for secret photos, 2014-2-17
            if (mVisitorMode) {
                super.onBackPressed();
            }
            //*/
        }
        //*/ Added by Tyd Linguanrong for secret photos, 2014-2-17
        else if (mVisitorMode) {
            super.onBackPressed();
        }
        //*/
        else {
            if (mLaunchedFromPhotoPage) {
                mActivity.getTransitionStore().putIfNotPresent(
                        PhotoPage.KEY_ALBUMPAGE_TRANSITION,
                        PhotoPage.MSG_ALBUMPAGE_RESUMED);
            }
            // TODO: fix this regression
            // mAlbumView.savePositions(PositionRepository.getInstance(mActivity));
            if (mInCameraApp) {
                super.onBackPressed();
            } else {
                onUpPressed();
            }
        }
    }



    private void onUpPressed() {

        //*/ Added by Tyd Linguanrong for secret photos, 2014-2-21
        if (mStorySelectMode) {
            Bundle data = new Bundle(getData());
            switch (mStoryActionType) {
                case -1:
                    data.putBoolean(GalleryActivity.KEY_GET_CONTENT, true);
                    data.putBoolean(AlbumSetPage.KEY_STORY_SELECT_MODE, true);
                    data.putInt(AlbumSetPage.KEY_STORY_SELECT_INDEX, mStoryBucketId);
                    data.putString(AlbumSetPage.KEY_MEDIA_PATH,
                            mActivity.getDataManager().getTopSetPath(DataManager.INCLUDE_ALL));
                    mActivity.getStateManager().switchState(this, AlbumSetPage.class, data);
                    break;

                case 0:
                    super.onBackPressed();
                    break;

                case 1:
                    if (!mStoryFromChild) {
                        data.putBoolean(GalleryActivity.KEY_GET_CONTENT, false);
                        data.putString(AlbumStoryPage.KEY_MEDIA_PATH, "/local/story/" + mStoryBucketId);
                        data.putInt(AlbumStoryPage.KEY_STORY_SELECT_INDEX, mStoryBucketId);
                        data.putBoolean(AlbumStoryPage.KEY_NEW_ALBUM, true);
                        mActivity.getStateManager().switchState(this, AlbumStoryPage.class, data);
                    } else {
                        super.onBackPressed();
                    }
                    break;
            }
        } else if (mVisitorMode) {
            super.onBackPressed();
        } else
            //*/
            if (mInCameraApp) {
                GalleryUtils.startGalleryActivity(mActivity);
            } else if (mActivity.getStateManager().getStateCount() > 1) {
                super.onBackPressed();
            } else if (mParentMediaSetString != null) {
                Bundle data = new Bundle(getData());
                data.putString(AlbumSetPage.KEY_MEDIA_PATH, mParentMediaSetString);
                mActivity.getStateManager().switchState(
                        this, AlbumSetPage.class, data);
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

    private void setPrepareConShots(MediaSet mediaset) {
        ArrayList<Path> conshot = new ArrayList<Path>();
        int total = mediaset.getMediaItemCount();

        ArrayList<MediaItem> list = mediaset.getMediaItem(0, total);
        for (MediaItem item : list) {
            Path id = item.getPath();
            conshot.add(id);
        }

        //mSelectionManager.setPrepared(conshot);
    }

    ArrayList<Path> mSubPaths;

    public void getSubMediaPath(MediaSet mediaset) {
        mSubPaths = new ArrayList<Path>();
        int total = mediaset.getMediaItemCount();

        ArrayList<MediaItem> list = mediaset.getMediaItem(0, total);
        for (MediaItem item : list) {
            Path id = item.getPath();
            /*if (StoryAlbum.isPathAdded(mActivity.getAndroidContext().getContentResolver(), id,
                    mStoryBucketId, true)) {
                mSelectionManager.toggle(id);
                mSubPaths.add(id);
            }*/
        }
        mSelectionManager.setSelectedPaths(mSubPaths);
    }

    private void onSingleTapUp(int slotIndex) {
        if (!mIsActive) return;

        if (mSelectionManager.inSelectionMode()) {
            MediaItem item = mAlbumDataAdapter.get(slotIndex);
            if (item == null) return; // Item not ready yet, ignore the click
            if (!mSubPaths.contains(item.getPath())) {
                mSelectionManager.toggle(item.getPath());
            }
            mSlotView.invalidate();
        } else {
            // Render transition in pressed state
            mAlbumView.setPressedIndex(slotIndex);
            mAlbumView.setPressedUp();
            mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_PICK_PHOTO, slotIndex, 0),
                    FadeTexture.DURATION);
        }
    }




    private void pickPhoto(int slotIndex, boolean startInFilmstrip) {
        if (!mIsActive) return;

        // Modified by TYD Theobald_Wu on 2014/01 [begin] for jigsaw feature
        //if (!startInFilmstrip) {
        if (!startInFilmstrip && !mJigsawPicker && !mGetContent) {
            // Modified by TYD Theobald_Wu on 2014/01 [end]
            // Launch photos in lights out mode
            mActivity.getGLRoot().setLightsOutMode(true);
        }

        MediaItem item = mAlbumDataAdapter.get(slotIndex);
        if (item == null) return; // Item not ready yet, ignore the click
        if (mGetContent) {
            // Added by TYD Theobald_Wu on 2014/01 [begin] for jigsaw feature
            if (mJigsawPicker && mActivity instanceof JigsawEntry
                    && item instanceof LocalImage) {
                JigsawEntry activity = (JigsawEntry) mActivity;
                activity.pickPhoto(item);
                return;
            }
            // Added by TYD Theobald_Wu on 2014/01 [end]
            onGetContent(item);
        } else if (mLaunchedFromPhotoPage) {
            TransitionStore transitions = mActivity.getTransitionStore();
            transitions.put(
                    PhotoPage.KEY_ALBUMPAGE_TRANSITION,
                    PhotoPage.MSG_ALBUMPAGE_PICKED);
            transitions.put(PhotoPage.KEY_INDEX_HINT, slotIndex);
            onBackPressed();
        } else {
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
            if (startInFilmstrip) {
                mActivity.getStateManager().switchState(this, FilmstripPage.class, data);
            } else {
                mActivity.getStateManager().startStateForResult(
                            SinglePhotoPage.class, REQUEST_PHOTO, data);
            }
        }
    }

    private void onGetContent(final MediaItem item) {
        DataManager dm = mActivity.getDataManager();
        Activity activity = mActivity;
        if (mData.getString(GalleryActivity.EXTRA_CROP) != null) {
            Uri uri = dm.getContentUri(item.getPath());
            Intent intent = new Intent(CropActivity.CROP_ACTION, uri)
                    .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                    .putExtras(getData());
            if (mData.getParcelable(GalleryStore.EXTRA_OUTPUT) == null) {
                intent.putExtra(CropExtras.KEY_RETURN_DATA, true);
            }
            /// M: [DEBUG.ADD] @{
            Log.d(TAG, "<onGetContent> start CropActivity for extra crop, uri: " + uri);
            /// @}
            activity.startActivity(intent);
            activity.finish();
        } else {
            Intent intent = new Intent(null, item.getContentUri())
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.setResult(Activity.RESULT_OK, intent);
            /// M: [DEBUG.ADD] @{
            Log.d(TAG, "<onGetContent> return uri: " + item.getContentUri());
            /// @}
            activity.finish();
        }
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
                    Toast.LENGTH_LONG).show();
        }
    }    public void onLongTap(int slotIndex) {
        if (mGetContent) return;
        MediaItem item = mAlbumDataAdapter.get(slotIndex);
        if (item == null) return;
        mSelectionManager.setAutoLeaveSelectionMode(true);
        mSelectionManager.toggle(item.getPath());
        mSlotView.invalidate();
    }
    @Override
    public void doCluster(int clusterType) {
        /// M: [FEATURE.ADD] [Runtime permission] @{
        if (clusterType == FilterUtils.CLUSTER_BY_LOCATION
                && !PermissionHelper.checkAndRequestForLocationCluster(mActivity)) {
            Log.i(TAG, "<doCluster> permission not granted");
            mNeedDoClusterType = clusterType;
            return;
        }
        /// @}
        String basePath = mMediaSet.getPath().toString();
        String newPath = FilterUtils.newClusterPath(basePath, clusterType);
        Bundle data = new Bundle(getData());
        data.putString(AlbumSetPage.KEY_MEDIA_PATH, newPath);

        // mAlbumView.savePositions(PositionRepository.getInstance(mActivity));
        mActivity.getStateManager().startStateForResult(
                AlbumSetPage.class, REQUEST_DO_ANIMATION, data);
    }
    @Override
    public void onAlbumModeSelected(int mode) {
        if (mode == GalleryActionBar.ALBUM_FILMSTRIP_MODE_SELECTED) {
            switchToFilmstrip();
        }
    }

    private void switchToFilmstrip() {
        if (mAlbumDataAdapter.size() < 1) return;
        int targetPhoto = mSlotView.getVisibleStart();
        prepareAnimationBackToFilmstrip(targetPhoto);
        if (mLaunchedFromPhotoPage) {
            onBackPressed();
        } else {
            pickPhoto(targetPhoto, true);
        }
    }

    @Override
    protected void onCreate(Bundle data, Bundle restoreState) {
        super.onCreate(data, restoreState);
        mUserDistance = GalleryUtils.meterToPixel(USER_DISTANCE_METER);
        initializeViews();
        initializeData(data);
        if (mActivity.mFreemeActionBarContainer != null) {
            mFreemeHomeView = mActivity.mFreemeActionBarContainer.findViewById(com.freeme.gallery.R.id.freeme_home_view);
            mFreemeHomeView.findViewById(com.freeme.gallery.R.id.up).setOnClickListener(this);
            mFreemeActionBarBackTitle = mFreemeHomeView.findViewById(com.freeme.gallery.R.id.freeme_actionbar_back_title);
            mFreemeActionBarBackTitle.setText(R.string.tab_by_story);
            mFreemeTitleLayout = mActivity.mFreemeActionBarContainer.findViewById(com.freeme.gallery.R.id.freeme_title_layout);
            ((TextView)(mFreemeTitleLayout.findViewById(com.freeme.gallery.R.id.action_bar_title))).setText(mMediaSet.getName());
            mFreemeActionBarBackTitle.setOnClickListener(this);
            mActivity.setTopbarBackgroundColor(R.color.primary_freeme_light);

        }
        mGetContent = data.getBoolean(GalleryActivity.KEY_GET_CONTENT, false);
        mShowClusterMenu = data.getBoolean(KEY_SHOW_CLUSTER_MENU, false);
        mDetailsSource = new MyDetailsSource();
        Context context = mActivity.getAndroidContext();

        if (data.getBoolean(KEY_AUTO_SELECT_ALL)) {
            mSelectionManager.selectAll();
        }

        mLaunchedFromPhotoPage =
                mActivity.getStateManager().hasStateClass(FilmstripPage.class);
        mInCameraApp = data.getBoolean(PhotoPage.KEY_APP_BRIDGE, false);

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

        // Added by TYD Theobald_Wu on 2014/01 [begin] for jigsaw feature
        mJigsawPicker = data.getBoolean(JigsawEntry.KEY_JIGSAW_PICKER, false);
        // Added by TYD Theobald_Wu on 2014/01 [end]

        //*/ Added by Tyd Linguanrong for secret photos, 2014-2-17
        mVisitorMode = data.getBoolean(KEY_VISITOR_MODE, false);
        mIsSecretImages = data.getBoolean(KEY_VISITOR_TYPE, true);
        mAutoSelectAll = data.getBoolean(KEY_AUTO_SELECT_ALL);
        mStorySelectMode = data.getBoolean(KEY_STORY_SELECT_MODE, false);
        mStoryFromChild = data.getBoolean(KEY_STORY_FROM_CHILD, false);
        mStoryBucketId = data.getInt(KEY_STORY_SELECT_INDEX, -1);

        if (mVisitorMode || mStorySelectMode) {
            ViewGroup galleryRoot = (ViewGroup) mActivity.findViewById(R.id.gallery_root);
            mSecretMenu = new SecretMenuHandler(mActivity, galleryRoot,
                    this, R.layout.album_secret_menu);

            mConfirm = (Button) mActivity.findViewById(R.id.btn_confirm);
            mConfirm.setEnabled(false);

            mActionModeHandler.shoulHideMenu(true);
            mSelectionManager.setAutoLeaveSelectionMode(false);
            mSelectionManager.enterSelectionMode();
        }
        //*/
    }



    @Override
    protected void onResume() {
        super.onResume();
        mIsActive = true;
        mActivity.getNavigationWidgetManager().changeStateTo(this);
        mResumeEffect = mActivity.getTransitionStore().get(KEY_RESUME_ANIMATION);
        if (mResumeEffect != null) {
            mAlbumView.setSlotFilter(mResumeEffect);
            mResumeEffect.setPositionProvider(mPositionProvider);
            mResumeEffect.start();
        }

        setContentPane(mRootPane);

        boolean enableHomeButton = (mActivity.getStateManager().getStateCount() > 1) |
                mParentMediaSetString != null;
        GalleryActionBar actionBar = null;
        if (mGetContent) {
            actionBar = mActivity.getGalleryActionBarWithoutTap();
        } else {
            actionBar = mActivity.getGalleryActionBar();
        }
        actionBar.setDisplayOptions(enableHomeButton, false);
        if (!mGetContent) {
            actionBar.enableAlbumModeMenu(GalleryActionBar.ALBUM_GRID_MODE_SELECTED, this);
        }

        // Set the reload bit here to prevent it exit this page in clearLoadingBit().
        setLoadingBit(BIT_LOADING_RELOAD);
        mLoadingFailed = false;
        mAlbumDataAdapter.resume();

        mAlbumView.resume();
        mAlbumView.setPressedIndex(-1);
        mActionModeHandler.resume();
        if (!mInitialSynced) {
            setLoadingBit(BIT_LOADING_SYNC);
            mSyncTask = mMediaSet.requestSync(this);
        }
        mInCameraAndWantQuitOnPause = mInCameraApp;

        //*/ Added by Tyd Linguanrong for secret photos, 2014-2-21
        if (mVisitorMode || mStorySelectMode) {
            mSecretMenu.refreshMenu();
        }
        //*/
        getSubMediaPath(mMediaSet);

    }



    @Override
    protected void onPause() {
        super.onPause();
        mIsActive = false;

        if (mSelectionManager.inSelectionMode()) {
            mSelectionManager.leaveSelectionMode();
        }

        //*/ Added by Linguanrong for story album, 2015-7-2
        mActivity.mIsSelectionMode = mSelectionManager != null && mSelectionManager.inSelectionMode();
        //*/
        mAlbumView.setSlotFilter(null);
        mActionModeHandler.pause();
        mAlbumDataAdapter.pause();
        mAlbumView.pause();
        DetailsHelper.pause();
        GalleryActionBar actionBar = null;
        if (mGetContent) {
            actionBar = mActivity.getGalleryActionBarWithoutTap();
        } else {
            actionBar = mActivity.getGalleryActionBar();
        }
        if (!mGetContent) {
            actionBar.disableAlbumModeMenu(true);
        }

        if (mSyncTask != null) {
            mSyncTask.cancel();
            mSyncTask = null;
            clearLoadingBit(BIT_LOADING_SYNC);
        }

        //*/ Added by Tyd Linguanrong for secret photos, 2014-2-21
        if (mVisitorMode || mStorySelectMode) {
            mSecretMenu.refreshMenu();
        }
        //*/
    }

   @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAlbumDataAdapter != null) {
            mAlbumDataAdapter.setLoadingListener(null);
        }
        mActionModeHandler.destroy();

        //*/ Added by Tyd Linguanrong for secret photos, 2014-2-17
        if (mVisitorMode || mStorySelectMode) {
            mSecretMenu.removeMenu();
        }
        //*/
    }

    private void initializeViews() {
        mSelectionManager = new SelectionManager(mActivity, false);
        mSelectionManager.setSelectionListener(this);
        Config.AlbumPage config = Config.AlbumPage.get(mActivity);
        mSlotView = new SlotView(mActivity, config.slotViewSpec);
        //*/Added by droi Linguanrong for Gallery new style, 2013-12-19
        mSlotViewPadding = config.slotViewSpec.slotPadding;
        //*/
        mAlbumView = new AlbumSlotRenderer(mActivity, mSlotView,
                mSelectionManager, config.placeholderColor);
        mSlotView.setSlotRenderer(mAlbumView);
        mRootPane.addComponent(mSlotView);
        mSlotView.setListener(new SlotView.SimpleListener() {
            @Override
            public void onDown(int index) {
                AlbumClassifierPage.this.onDown(index);
            }

            @Override
            public void onUp(boolean followedByLongPress) {
                AlbumClassifierPage.this.onUp(followedByLongPress);
            }

            @Override
            public void onSingleTapUp(int slotIndex) {
                AlbumClassifierPage.this.onSingleTapUp(slotIndex);
            }

            @Override
            public void onLongTap(int slotIndex) {
                AlbumClassifierPage.this.onLongTap(slotIndex);
            }
        });
        mActionModeHandler = new ActionModeHandler(mActivity, mSelectionManager);
        mActionModeHandler.setActionModeListener(new ActionModeHandler.ActionModeListener() {
            @Override
            public boolean onActionItemClicked(MenuItem item,int menuItemid) {
                return onItemSelected(item, menuItemid);
            }
        });
    }

    private void initializeData(Bundle data) {
        mMediaSetPath = Path.fromString(data.getString(KEY_MEDIA_PATH));
        mParentMediaSetString = data.getString(KEY_PARENT_MEDIA_PATH);
        mMediaSet = mActivity.getDataManager().getMediaSet(mMediaSetPath);
        if (mMediaSet == null) {
            Utils.fail("MediaSet is null. Path = %s", mMediaSetPath);
        }
        mSelectionManager.setSourceMediaSet(mMediaSet);
        mAlbumDataAdapter = new AlbumDataLoader(mActivity, mMediaSet);
        mAlbumDataAdapter.setLoadingListener(new MyLoadingListener());
        mAlbumView.setModel(mAlbumDataAdapter);
    }

    private void showDetails() {
        mShowDetails = true;
        if (mDetailsHelper == null) {
            mDetailsHelper = new DetailsHelper(mActivity, mRootPane, mDetailsSource);
            mDetailsHelper.setCloseListener(new CloseListener() {
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

    @Override
    protected boolean onCreateActionBar(Menu menu) {
        GalleryActionBar actionBar = null;

        if (mGetContent) {
            actionBar = mActivity.getGalleryActionBarWithoutTap();
        } else {
            actionBar = mActivity.getGalleryActionBar();
        }


//        MenuInflater inflator = getSupportMenuInflater();

        //*/ Added by Tyd Linguanrong for secret photos, 2014-2-21
        if (mVisitorMode || mStorySelectMode) {
            actionBar.setTitle(mMediaSet.getName());
            return true;
        }
        //*/

        //*/ Added by droi Linguanrong for freeme gallery, 16-1-14
        actionBar.setDisplayOptions(true, true);
        actionBar.setTitle(mMediaSet.getName());
        //*/

        if (mGetContent) {
            //*/ Modified by droi Linguanrong for freeme gallery, 16-1-14
            actionBar.createActionBarMenu(R.menu.pickup, menu);
            menu.findItem(R.id.action_cancel).setVisible(false);
            /*/
            inflator.inflate(com.freeme.gallery.R.menu.pickup, menu);
            //*/
            int typeBits = mData.getInt(GalleryActivity.KEY_TYPE_BITS,
                    DataManager.INCLUDE_IMAGE);
            actionBar.setTitle(GalleryUtils.getSelectionModePrompt(typeBits));
//        } else {
//            //*/ Modified by droi Linguanrong for freeme gallery, 16-1-14
//            actionBar.createActionBarMenu(R.menu.album, menu);
//            /*/
//            inflator.inflate(com.freeme.gallery.R.menu.album, menu);
//            //*/
//            actionBar.setTitle(mMediaSet.getName());
//
//            FilterUtils.setupMenuItems(actionBar, mMediaSetPath, true);
//
//            menu.findItem(R.id.action_camera).setVisible( GalleryUtils.isCameraAvailable(mActivity));

        }
        actionBar.setSubtitle(null);
        return true;
    }

    private void prepareAnimationBackToFilmstrip(int slotIndex) {
        if (mAlbumDataAdapter == null || !mAlbumDataAdapter.isActive(slotIndex)) return;
        MediaItem item = mAlbumDataAdapter.get(slotIndex);
        if (item == null) return;
        TransitionStore transitions = mActivity.getTransitionStore();
        transitions.put(PhotoPage.KEY_INDEX_HINT, slotIndex);
        transitions.put(PhotoPage.KEY_OPEN_ANIMATION_RECT,
                mSlotView.getSlotRect(slotIndex, mRootPane));
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

    private boolean onMenuItemSelected(int itemId) {
        switch (itemId) {
            case android.R.id.home: {
                onUpPressed();
                return true;
            }
            case R.id.action_cancel:
                mActivity.getStateManager().finishState(this);
                return true;
            case R.id.action_select:
                mSelectionManager.setAutoLeaveSelectionMode(false);
                mSelectionManager.enterSelectionMode();
                return true;
            case R.id.action_slideshow: {
                mInCameraAndWantQuitOnPause = false;
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
            case R.id.action_camera: {
                GalleryUtils.startCameraActivity(mActivity);
                return true;
            }

            // Added by TYD Theobald_Wu on 2014/01 [begin] for jigsaw feature
            case R.id.action_jigsaw: {
                //*/ Added by tyd Linguanrong for statistic, 15-12-18
//                StatisticUtil.generateStatisticInfo(mActivity, StatisticData.OPTION_JIGSAW);
                //*/

                // for baas analytics
//                DroiAnalytics.onEvent(mActivity, StatisticData.OPTION_JIGSAW);

                Intent intent = new Intent(mActivity, JigsawEntry.class);
                mActivity.startActivity(intent);
                return true;
            }
            // Added by TYD Theobald_Wu on 2014/01 [end]
            default:
                return false;
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
        }
    }

    @Override
    public void onSelectionModeChange(int mode) {
        switch (mode) {
            case SelectionManager.ENTER_SELECTION_MODE: {
                mActionModeHandler.startActionMode();
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                break;
            }
            case SelectionManager.LEAVE_SELECTION_MODE: {
                mActionModeHandler.finishActionMode();
                mRootPane.invalidate();
                //*/ Added by Tyd Linguanrong for secret photos, 2014-2-17
                if (mVisitorMode || mStorySelectMode) {
                    onUpPressed();
                }
                //*/
                break;
            }
            /// M: [BEHAVIOR.ADD] @{
            // when click deselect all in menu, not leave selection mode
            case SelectionManager.DESELECT_ALL_MODE:
            /// @}
            case SelectionManager.SELECT_ALL_MODE: {
                //*/ Added by Linguanrong for story album, 2015-08-13
                if (mConfirm != null) {
                    mConfirm.setEnabled(mode == SelectionManager.SELECT_ALL_MODE);
                }
                //*/
                mActionModeHandler.updateSupportedOperation();
                mRootPane.invalidate();
                break;
            }
        }
    }    private void pickPhoto(int slotIndex) {
        pickPhoto(slotIndex, false);
    }

    @Override
    public void onSelectionChange(Path path, boolean selected) {
        int count = mSelectionManager.getSelectedCount();
        //*/ Added by Linguanrong for story album, 2015-08-13
        if (mConfirm != null) {
            mConfirm.setEnabled(count > 0);
        }
        //*/
        String format = mActivity.getResources().getQuantityString(
                R.plurals.number_of_items_selected, count);
        mActionModeHandler.setTitle(String.format(format, count));
        mActionModeHandler.updateSupportedOperation(path, selected);
    }

    @Override
    public void onSyncDone(final MediaSet mediaSet, final int resultCode) {
        Log.d(TAG, "onSyncDone: " + Utils.maskDebugInfo(mediaSet.getName()) + " result="
                + resultCode);
        ((Activity) mActivity).runOnUiThread(new Runnable() {
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
    private void setLoadingBit(int loadTaskBit) {
        mLoadingBits |= loadTaskBit;
    }

    private void clearLoadingBit(int loadTaskBit) {
        mLoadingBits &= ~loadTaskBit;
        //*/ Modified by Tyd Linguanrong for secret photos, 2014-2-17
        if (mLoadingBits == 0 && mIsActive && !mVisitorMode && !mStorySelectMode) {
            //*/
            if (mAlbumDataAdapter.size() == 0) {
                Intent result = new Intent();
                result.putExtra(KEY_EMPTY_ALBUM, true);
                setStateResult(Activity.RESULT_OK, result);
                mActivity.getStateManager().finishState(this);
            }
        }
    }

    @Override
    public void onEnterState() {
        mActivity.showNavi(AbstractGalleryActivity.IN_ALBUMPAGE);
    }

    @Override
    public void observe() {

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case com.freeme.gallery.R.id.up:
            case com.freeme.gallery.R.id.freeme_actionbar_back_title:
                onUpPressed();
                break;
            default:
                break;
        }

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
    //********************************************************************
    //*                              MTK                                 *
    //********************************************************************

    // Flag to specify whether mSelectionManager.restoreSelection task has done
    private boolean mRestoreSelectionDone;
    // Save selection for onPause/onResume
    private boolean mNeedUpdateSelection = false;
    // If restore selection not done in selection mode,after click one slot, show 'wait' toast
    private Toast mWaitToast = null;

    public void onSelectionRestoreDone() {
        if (!mIsActive) {
            return;
        }
        mRestoreSelectionDone = true;
        // Update selection menu after restore done @{
        mActionModeHandler.updateSupportedOperation();
        mActionModeHandler.updateSelectionMenu();
    }

    /// M: [BUG.ADD] leave selection mode when plug out sdcard @{
//    @Override
//    public void onEjectSdcard() {
//        if (mSelectionManager.inSelectionMode()) {
//            Log.i(TAG, "<onEjectSdcard> leaveSelectionMode");
//            mSelectionManager.leaveSelectionMode();
//        }
//    }
    /// @}

    /// M:[FEATURE.ADD] play video directly. @{
    public void playVideo(Activity activity, Uri uri, String title) {
        Log.i(TAG, "<playVideo> enter playVideo");
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "video/*")
                    .putExtra(Intent.EXTRA_TITLE, title)
                    .putExtra(MovieActivity.KEY_TREAT_UP_AS_BACK, true);
            activity.startActivityForResult(intent, PhotoPage.REQUEST_PLAY_VIDEO);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, activity.getString(R.string.video_err),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private boolean canBePlayed(MediaItem item) {
        int supported = item.getSupportedOperations();
        return ((supported & MediaItem.SUPPORT_PLAY) != 0
                && MediaObject.MEDIA_TYPE_VIDEO == item.getMediaType());
    }
    /// @}

    /// M: [PERF.ADD] add for delete many files performance improve @{
    @Override
    public void setProviderSensive(boolean isProviderSensive) {
        mAlbumDataAdapter.setSourceSensive(isProviderSensive);
    }
    @Override
    public void fakeProviderChange() {
        mAlbumDataAdapter.fakeSourceChange();
    }
    /// @}

    /// M: [FEATURE.ADD] [Runtime permission] @{
    private int mNeedDoClusterType = 0;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        if (PermissionHelper.isAllPermissionsGranted(permissions, grantResults)) {
            doCluster(mNeedDoClusterType);
        } else {
            PermissionHelper.showDeniedPromptIfNeeded(mActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }
    /// @}
    
        @Override
    public void onButtonClicked(int id) {
        switch (id) {
            case R.id.btn_confirm:
                GLRoot root = mActivity.getGLRoot();
                root.lockRenderThread();
                //*/ Modified by Linguanrong for story album, 2015-5-28
                if (mVisitorMode) {
                    handleVisitor();
                } else if (mStorySelectMode) {
                    handleStoryImages();
                }
                //*/
                root.unlockRenderThread();
                break;

           /* case R.id.btn_cancel:
                root = mActivity.getGLRoot();
                root.lockRenderThread();
                if (mSelectionManager.inSelectionMode()) {
                    mStoryActionType = 0;
                    mSelectionManager.leaveSelectionMode();
                }
                root.unlockRenderThread();
                break;*/
        }
    }
    
    private void handleVisitor() {
        ArrayList<Path> path = mSelectionManager.getSelected(false);
        if (mIsSecretImages) {
            /*/ Disabled by Linguanrong for story album, 2015-6-4
            MediaItem item;
            for(int i = 0; i < mSelectionManager.getSelectedCount(); i++) {
                item = mAlbumDataAdapter.getItem(mAlbumDataAdapter.findItem(path.get(i)));
                if(item != null && item.isContainer() && item.isConShot()) {
                    setPrepareConShots(item.getRelatedMediaSet());
                    VisitorAlbum.addVisitorImage(mActivity.getAndroidContext().getContentResolver(),
                        mSelectionManager.getPrepared());
                }
            }
            //*/

            VisitorAlbum.addVisitorImage(mActivity.getAndroidContext().getContentResolver(), path);
        } else {
            VisitorAlbumVideo.addVisitorVideo(mActivity.getAndroidContext().getContentResolver(), path);
        }

        mSelectionManager.leaveSelectionMode();
    }
    
    //*/ Added by Linguanrong for story album, 2015-4-9
    private void handleStoryImages() {
        ArrayList<Path> path = mSelectionManager.getSelected(false);
        ArrayList<Path> videoPath = new ArrayList<Path>();
        MediaItem item;
        int id;

        for (int i = 0; i < mSelectionManager.getSelectedCount(); i++) {
            id = Math.max(0, mAlbumDataAdapter.findItem(path.get(i)));
            item = mAlbumDataAdapter.getItem(id);
            if (item != null && item.getMediaType() == MediaObject.MEDIA_TYPE_VIDEO) {
                videoPath.add(path.get(i));
            }
        }

        if (videoPath.size() > 0) {
            for (int i = 0; i < videoPath.size(); i++) {
                path.remove(videoPath.get(i));
            }
            StoryAlbum.addStoryImage(mActivity.getAndroidContext().getContentResolver(),
                    videoPath, mStoryBucketId, false);
        }

        StoryAlbum.addStoryImage(mActivity.getAndroidContext().getContentResolver(),
                path, mStoryBucketId, true);

        mStoryActionType = 1;

        mSelectionManager.leaveSelectionMode();


    }
}

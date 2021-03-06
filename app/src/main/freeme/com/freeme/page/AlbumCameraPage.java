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
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.droi.sdk.analytics.DroiAnalytics;
import com.freeme.data.StoryAlbumSet;
import com.freeme.gallery.R;
import com.android.gallery3d.anim.StateTransitionAnimation;
import com.android.gallery3d.app.ActivityState;
import com.android.gallery3d.app.AlbumDataLoader;
import com.android.gallery3d.app.AlbumSetPage;
import com.android.gallery3d.app.FilmstripPage;
import com.android.gallery3d.app.FilterUtils;
import com.android.gallery3d.app.GalleryActionBar;
import com.freeme.gallery.app.GalleryActivity;
import com.android.gallery3d.app.LoadingListener;
import com.android.gallery3d.app.OrientationManager;
import com.android.gallery3d.app.PhotoPage;
import com.android.gallery3d.app.SinglePhotoPage;
import com.android.gallery3d.app.SlideshowPage;
import com.android.gallery3d.app.TransitionStore;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaDetails;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.glrenderer.FadeTexture;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.ui.ActionModeHandler;
import com.android.gallery3d.ui.AlbumSlotRenderer;
import com.android.gallery3d.ui.DetailsHelper;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.ui.PhotoFallbackEffect;
import com.android.gallery3d.ui.RelativePosition;
import com.android.gallery3d.ui.SelectionManager;
import com.android.gallery3d.ui.SlotView;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.util.Future;
import com.freeme.jigsaw.app.JigsawEntry;
import com.freeme.settings.GallerySettings;
import com.freeme.statistic.StatisticData;
import com.freeme.statistic.StatisticUtil;
import com.freeme.utils.FreemeUtils;

import java.lang.ref.WeakReference;


public class AlbumCameraPage extends ActivityState implements GalleryActionBar.ClusterRunner,
        SelectionManager.SelectionListener, MediaSet.SyncListener {
    public static final String KEY_MEDIA_PATH        = "media-path";
    public static final String KEY_PARENT_MEDIA_PATH = "parent-media-path";
    public static final String KEY_SET_CENTER        = "set-center";
    public static final String KEY_AUTO_SELECT_ALL   = "auto-select-all";
    public static final String KEY_EMPTY_ALBUM       = "empty-album";
    public static final String KEY_RESUME_ANIMATION  = "resume_animation";
    public static final  int REQUEST_PHOTO        = 2;
    @SuppressWarnings("unused")
    private static final String TAG = "AlbumCameraPage";
    private static final int REQUEST_SLIDESHOW    = 1;
    private static final int REQUEST_DO_ANIMATION = 3;

    private static final int BIT_LOADING_RELOAD = 1;
    private static final int BIT_LOADING_SYNC   = 2;

    private static final float USER_DISTANCE_METER = 0.3f;
    private static final int MSG_PICK_PHOTO = 0;
    protected SelectionManager mSelectionManager;
    WeakReference<Toast> mEmptyAlbumToast = null;
    private boolean mIsActive = false;
    private AlbumSlotRenderer mAlbumView;
    private Path              mMediaSetPath;
    private String            mParentMediaSetString;
    private SlotView          mSlotView;
    private AlbumDataLoader mAlbumDataAdapter;
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
    private boolean mInCameraApp;
    private boolean mInCameraAndWantQuitOnPause;
    private int     mLoadingBits   = 0;
    private boolean mInitialSynced = false;
    private int     mSyncResult;
    private boolean mLoadingFailed;
    private RelativePosition mOpenCenter = new RelativePosition();
    private Handler mHandler;
    //*/Added by Tyd Linguanrong for Gallery new style, 2013-12-19
    private GalleryActionBar mActionBar;
    private int     mSlotViewPadding    = 0;
    private int     mBottomPadding      = 0;
    //*/
    private boolean resumeFromCommunity = false;
    private View mEmptyView;
    private boolean mShowedEmptyToastForSelf = false;
    //*/
    //*/ Added by droi Linguanrong for lock orientation, 16-3-1
    private OrientationManager mOrientationManager;
    private PhotoFallbackEffect mResumeEffect;
    private final GLView mRootPane = new GLView() {
        private final float mMatrix[] = new float[16];

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

        @Override
        protected void onLayout(
                boolean changed, int left, int top, int right, int bottom) {

            int slotViewTop = mActionBar.getHeight() + mActivity.mStatusBarHeight;
            int slotViewBottom = bottom - top;
            int slotViewRight = right - left;

            if (mShowDetails) {
                mDetailsHelper.layout(left, slotViewTop, right, bottom);
            } else {
                mAlbumView.setHighlightItemPath(null);
            }

            // Set the mSlotView as a reference point to the open animation
            mOpenCenter.setReferencePosition(0, slotViewTop);
            mSlotView.layout(0, slotViewTop, slotViewRight,
                    slotViewBottom - mBottomPadding - mSlotViewPadding);
            GalleryUtils.setViewPointMatrix(mMatrix,
                    (right - left) / 2, (bottom - top) / 2, -mUserDistance);
        }
    };
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

    // This are the transitions we want:
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
        } else {
            if (mLaunchedFromPhotoPage) {
                mActivity.getTransitionStore().putIfNotPresent(
                        PhotoPage.KEY_ALBUMPAGE_TRANSITION,
                        PhotoPage.MSG_ALBUMPAGE_RESUMED);
            }
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
        }
    }

    @Override
    protected int getBackgroundColorId() {
        return R.color.album_background;
    }

    @Override
    protected void onCreate(Bundle data, Bundle restoreState) {
        super.onCreate(data, restoreState);
        mUserDistance = GalleryUtils.meterToPixel(USER_DISTANCE_METER);
        mActionBar = mActivity.getGalleryActionBar();
        initializeViews();
        initializeData(data);
        mGetContent = data.getBoolean(GalleryActivity.KEY_GET_CONTENT, false);
        mDetailsSource = new MyDetailsSource();

        //*/ Added by droi Linguanrong for freeme gallery, 16-1-14
        resumeFromCommunity = data.getBoolean(FreemeUtils.KEY_FROM_COMMUNITY);
        //*/

        if (data.getBoolean(KEY_AUTO_SELECT_ALL)) {
            mSelectionManager.selectAll();
        }

        mLaunchedFromPhotoPage =
                mActivity.getStateManager().hasStateClass(FilmstripPage.class);

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

    @Override
    protected void onPause() {
        super.onPause();
        mIsActive = false;

        //*/ Added by Linguanrong for story album, 2015-7-2
        mActivity.mIsSelectionMode = mSelectionManager != null ?
                mSelectionManager.inSelectionMode() : false;
        //*/

        mAlbumView.setSlotFilter(null);
        mActionModeHandler.pause();
        mAlbumDataAdapter.pause();
        mAlbumView.pause();
        DetailsHelper.pause();
        mActionBar.disableClusterMenu(false);

        if (mSyncTask != null) {
            mSyncTask.cancel();
            mSyncTask = null;
            clearLoadingBit(BIT_LOADING_SYNC);
        }

        hideCameraButton();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsActive = true;

        //*/ Added by droi Linguanrong for lock orientation, 16-3-1
        mOrientationManager.lockOrientation(true);
        //*/

        //*/ Added by tyd Linguanrong for freeme gallery, 16-1-13
        ((GalleryActivity) mActivity).setBottomTabVisibility(true);
        mActionBar.enableClusterMenu(FreemeUtils.CLUSTER_BY_CAMERE, this);
        if (resumeFromCommunity) {
            resumeFromCommunity = false;
            startIntroAnimation();
        }
        //*/

        mResumeEffect = mActivity.getTransitionStore().get(KEY_RESUME_ANIMATION);
        if (mResumeEffect != null) {
            mAlbumView.setSlotFilter(mResumeEffect);
            mResumeEffect.setPositionProvider(mPositionProvider);
            mResumeEffect.start();
        }

        setContentPane(mRootPane);

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

        //*/ Added by Tyd Linguanrong for Gallery new style, 2014-4-24
        if (mMediaSet != null && mMediaSet.getMediaItemCount() != 0) {
            hideCameraButton();
        }
        //*/
    }

    @Override
    protected boolean onCreateActionBar(Menu menu) {
        mActionBar.setDisplayOptions(false, GalleryActionBar.SHOWTITLE);

        if (mGetContent) {
            mActionBar.createActionBarMenu(R.menu.pickup, menu);
        } else {
            mActionBar.createActionBarMenu(R.menu.album_camera, menu);
            //*/ Modified by Linguanrong for story album, 2015-5-28
            mActionBar.enableClusterMenu(FreemeUtils.CLUSTER_BY_CAMERE, this);
            //*/
            //menu.findItem(R.id.action_slideshow).setVisible(!(mMediaSet instanceof MtpDevice));
            menu.findItem(R.id.action_slideshow).setVisible(mMediaSet.getMediaItemCount() != 0);

            FilterUtils.setupMenuItems(mActionBar, mMediaSetPath, true);
            menu.findItem(R.id.action_camera).setVisible(GalleryUtils.isCameraAvailable(mActivity));

        }
        mActionBar.setTitle(R.string.tab_by_camera);
        mActionBar.setSubtitle(null);

        //*/ Added by Linguanrong for set menu item visible, 2015-4-24
        menu.findItem(R.id.action_select).setVisible(
                mMediaSet != null && mMediaSet.getMediaItemCount() > 0);
        //*/

        return true;
    }

    @Override
    protected boolean onItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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
                data.putString(SlideshowPage.KEY_SET_PATH, mMediaSetPath.toString());
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

            case R.id.action_switch_type: {
                StateTransitionAnimation.setOffset(0, 0);
                String newPath;
                Bundle data = new Bundle();
                newPath = mActivity.getDataManager().makeCameraSetPath();
                data.putString(AlbumTimeShaftPage.KEY_MEDIA_PATH, newPath);
                mActivity.getStateManager().switchState(this, AlbumTimeShaftPage.class, data);
                mActivity.mEditor.putBoolean("default_page", true);
                mActivity.mEditor.commit();
                return true;
            }

            case R.id.action_settings: {
                mActivity.startActivity(new Intent(mActivity, GallerySettings.class));
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
    protected void onDestroy() {
        super.onDestroy();

        /*/ Added by droi Linguanrong for lock orientation, 16-3-1
        mOrientationManager.unlockOrientation();
        //*/

        if (mAlbumDataAdapter != null) {
            mAlbumDataAdapter.setLoadingListener(null);
        }

        //*/ Added by Tyd Linguanrong for Gallery new style, 2014-4-24
        cleanupCameraButton();
        //*/

        mActionModeHandler.destroy();
    }

    private void cleanupCameraButton() {
        if (mEmptyView == null) return;
        RelativeLayout galleryRoot = (RelativeLayout) mActivity
                .findViewById(R.id.gallery_root);
        if (galleryRoot == null) return;
        galleryRoot.removeView(mEmptyView);
        mEmptyView = null;
    }

    private void setLoadingBit(int loadTaskBit) {
        mLoadingBits |= loadTaskBit;
    }

    private void hideCameraButton() {
        if (mEmptyView == null) return;
        mEmptyView.setVisibility(View.GONE);
    }

    private void clearLoadingBit(int loadTaskBit) {
        mLoadingBits &= ~loadTaskBit;
        if (mLoadingBits == 0 && mIsActive) {
            if (mAlbumDataAdapter.size() == 0) {
                if (mActivity.getStateManager().getStateCount() > 1) {
                    Intent result = new Intent();
                    result.putExtra(KEY_EMPTY_ALBUM, true);
                    setStateResult(Activity.RESULT_OK, result);
                    mActivity.getStateManager().finishState(this);
                } else {
                    mShowedEmptyToastForSelf = true;
                    //showEmptyAlbumToast(Toast.LENGTH_LONG);
                    mSlotView.invalidate();
                    //showCameraButton();
                }
                return;
            }
        }

        // Hide the empty album toast if we are in the root instance of
        // AlbumSetPage and the album is no longer empty (for instance,
        // after a sync is completed and web albums have been synced)
        if (mShowedEmptyToastForSelf) {
            mShowedEmptyToastForSelf = false;
            hideEmptyAlbumToast();
            hideCameraButton();
        }
    }

    private void showCameraButton() {
        if (mEmptyView == null && !setupCameraButton()) return;
        mEmptyView.setVisibility(View.VISIBLE);

        mActivity.invalidateOptionsMenu();
    }

    private void hideEmptyAlbumToast() {
        if (mEmptyAlbumToast != null) {
            Toast toast = mEmptyAlbumToast.get();
            if (toast != null) toast.cancel();
        }
    }

    private boolean setupCameraButton() {
        if (!GalleryUtils.isCameraAvailable(mActivity)) return false;
        RelativeLayout galleryRoot = (RelativeLayout) mActivity
                .findViewById(R.id.gallery_root);
        if (galleryRoot == null) return false;

        mEmptyView = LayoutInflater.from(mActivity).inflate(R.layout.empty_tips, null);
        TextView btnCamera = (TextView) mEmptyView.findViewById(R.id.btn_camera);
        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                GalleryUtils.startCameraActivity(mActivity);
            }
        });

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        lp.addRule(RelativeLayout.CENTER_IN_PARENT);
        galleryRoot.addView(mEmptyView, lp);

        return true;
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
            mSlotView.invalidate();
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

        //*/ Added by tyd Linguanrong for freeme gallery, 16-1-13
        ((GalleryActivity) mActivity).setBottomTabVisibility(false);
        //*/

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
                FreemeUtils.playVideo(mActivity, item.getPlayUri(), item.getName());
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
            if (startInFilmstrip) {
                mActivity.getStateManager().switchState(this, FilmstripPage.class, data);
            } else {
                mActivity.getStateManager().startStateForResult(
                        SinglePhotoPage.class, REQUEST_PHOTO, data);
            }
        }
    }

    private boolean canBePlayed(MediaItem item) {
        int supported = item.getSupportedOperations();
        return ((supported & MediaItem.SUPPORT_PLAY) != 0);
    }

    private void onGetContent(final MediaItem item) {
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
        mSlotView.invalidate();
    }

    @Override
    public void doCluster(int clusterType) {
        //*/ Added by Linguanrong for story album, 2015-5-28
        if (FreemeUtils.CLUSTER_BY_CAMERE == clusterType) {
        } else if (FreemeUtils.CLUSTER_BY_STORY == clusterType) {
            if (mSelectionManager != null && mSelectionManager.inSelectionMode()) {
                mSelectionManager.leaveSelectionMode();
            }
            Bundle data = new Bundle();
            data.putString(AlbumStorySetPage.KEY_MEDIA_PATH, StoryAlbumSet.PATH.toString());
            data.putInt(AlbumStorySetPage.KEY_SELECTED_CLUSTER_TYPE, clusterType);
            mActivity.getStateManager().switchState(this, AlbumStorySetPage.class, data);
        } else if (FreemeUtils.CLUSTER_BY_ALBUM == clusterType) {
            if (mSelectionManager != null && mSelectionManager.inSelectionMode()) {
                mSelectionManager.leaveSelectionMode();
            }
            String basePath = mActivity.getDataManager().getTopSetPath(DataManager.INCLUDE_ALL);
            String newPath = FilterUtils.switchClusterPath(basePath, clusterType);
            Bundle data = new Bundle(getData());
            data.putString(AlbumSetPage.KEY_MEDIA_PATH, newPath);
            data.putInt(AlbumSetPage.KEY_SELECTED_CLUSTER_TYPE, clusterType);
            mActivity.getStateManager().switchState(this, AlbumSetPage.class, data);
        } else if (clusterType == FreemeUtils.CLUSTER_BY_COMMUNITY) {
            if (mSelectionManager != null && mSelectionManager.inSelectionMode()) {
                mSelectionManager.leaveSelectionMode();
            }
            ((GalleryActivity) mActivity).startCommunity();
        }
    }

    private void initializeViews() {
        mSelectionManager = new SelectionManager(mActivity, false);
        mSelectionManager.setSelectionListener(this);
        PageConfig.AlbumCameraPage config = PageConfig.AlbumCameraPage.get(mActivity);
        mSlotView = new SlotView(mActivity, config.slotViewSpec);
        mSlotViewPadding = config.slotViewSpec.slotPadding;
        mBottomPadding = config.slotViewSpec.bottomPadding;
        mAlbumView = new AlbumSlotRenderer(mActivity, mSlotView,
                mSelectionManager, config.placeholderColor);
        mSlotView.setSlotRenderer(mAlbumView);
        mRootPane.addComponent(mSlotView);
        mSlotView.setListener(new SlotView.SimpleListener() {
            @Override
            public void onDown(int index) {
                AlbumCameraPage.this.onDown(index);
            }

            @Override
            public void onUp(boolean followedByLongPress) {
                AlbumCameraPage.this.onUp(followedByLongPress);
            }

            @Override
            public void onSingleTapUp(int slotIndex) {
                AlbumCameraPage.this.onSingleTapUp(slotIndex);
            }

            @Override
            public void onLongTap(int slotIndex) {
                AlbumCameraPage.this.onLongTap(slotIndex);
            }
        });
        mActionModeHandler = new ActionModeHandler(mActivity, mSelectionManager);
        mActionModeHandler.setActionModeListener(new ActionModeHandler.ActionModeListener() {
            @Override
            public boolean onActionItemClicked(MenuItem item) {
                return onItemSelected(item);
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
                break;
            }

            case SelectionManager.DESELECT_ALL_MODE:
            case SelectionManager.SELECT_ALL_MODE: {
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

    private void showEmptyAlbumToast(int toastLength) {
        Toast toast;
        if (mEmptyAlbumToast != null) {
            toast = mEmptyAlbumToast.get();
            if (toast != null) {
                toast.show();
                return;
            }
        }
        toast = Toast.makeText(mActivity, R.string.empty_album, toastLength);
        mEmptyAlbumToast = new WeakReference<>(toast);
        toast.show();
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

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    int itemCount = mMediaSet != null ? mMediaSet.getMediaItemCount() : 0;
                    if(itemCount == 0) {
                        showCameraButton();
                    } else {
                        hideCameraButton();
                    }
                }
            }, 50);
        }
    }

    private class MyDetailsSource implements DetailsHelper.DetailsSource {
        private int mIndex;

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

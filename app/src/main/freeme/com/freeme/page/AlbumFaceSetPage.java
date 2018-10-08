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

package com.freeme.page;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.gallery3d.app.ActivityState;
import com.android.gallery3d.app.AlbumClassifierPage;
import com.android.gallery3d.app.AlbumSetDataLoader;
import com.android.gallery3d.app.AlbumSetPage;
import com.android.gallery3d.app.EyePosition;
import com.android.gallery3d.app.FilterUtils;
import com.android.gallery3d.app.GalleryActionBar;
import com.android.gallery3d.app.LoadingListener;
import com.android.gallery3d.app.OrientationManager;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaDetails;
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
import com.android.gallery3d.ui.SelectionManager;
import com.android.gallery3d.ui.SlotView;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.GalleryUtils;
import com.freeme.data.FaceAlbum;
import com.freeme.data.FaceAlbumSet;
import com.freeme.data.FaceMergeAlbum;
import com.freeme.gallery.GalleryClassifierService;
import com.freeme.gallery.R;
import com.freeme.gallery.app.AbstractGalleryActivity;
import com.freeme.gallery.app.AlbumPicker;
import com.freeme.gallery.app.GalleryActivity;
import com.freeme.settings.GallerySettings;
import com.freeme.ui.FaceAlbumSetSlotRender;
import com.freeme.ui.FaceSlotView;
import com.freeme.ui.manager.State;
import com.freeme.utils.FreemeUtils;
import com.freeme.utils.LogUtil;

import java.util.ArrayList;
import java.util.Calendar;

public class AlbumFaceSetPage extends ActivityState implements
        SelectionManager.SelectionListener, GalleryActionBar.ClusterRunner,
        EyePosition.EyePositionListener, MediaSet.SyncListener,State,View.OnClickListener {
    public static final  String KEY_MEDIA_PATH            = "media-path";
    public static final  String KEY_SET_TITLE             = "set-title";
    public static final  String KEY_SET_SUBTITLE          = "set-subtitle";
    public static final  String KEY_SELECTED_CLUSTER_TYPE = "selected-cluster";
    @SuppressWarnings("unused")
    private static final String TAG                       = "Gallery2/AlbumFaceSetPage";
    private static final String SHOW_STORYSET_GUIDE       = "showStorySetGuide";
    private static final String SHOW_STORY_GUIDE          = "showStoryGuide";
    private static final int    MSG_PICK_ALBUM            = 1;
    private static final int    DATA_CACHE_SIZE           = 256;
    private static final int    REQUEST_DO_ANIMATION      = 1;

    private static final int     BIT_LOADING_RELOAD = 1;
    private static final int     BIT_LOADING_SYNC   = 2;
    public static        boolean mAddNewAlbum       = false;
    protected SelectionManager mSelectionManager;
    private boolean mIsActive = false;
    private FaceSlotView mSlotView;
    private FaceAlbumSetSlotRender         mAlbumSetView;
    private PageConfig.AlbumFaceSetPage  mConfig;
    private MediaSet                     mMediaSet;
    private String                       mTitle;
    private GalleryActionBar             mActionBar;
    private int                          mSelectedAction;
    private AlbumSetDataLoader           mAlbumSetDataAdapter;
    private boolean                      mGetContent;
    private boolean                      mGetAlbum;
    private ActionModeHandler            mActionModeHandler;
    private DetailsHelper                mDetailsHelper;
    private MyDetailsSource              mDetailsSource;
    private boolean                      mShowDetails;
    private EyePosition                  mEyePosition;
    private Handler                      mHandler;
    // The eyes' position of the user, the origin is at the center of the
    // device and the unit is in pixels.
    private float                        mX;
    private float                        mY;
    private float                        mZ;
    private       Future<Integer> mSyncTask           = null;
    private       int             mLoadingBits        = 0;
    private       boolean         mInitialSynced      = false;
    //*/Added by Tyd Linguanrong for Gallery new style, 2013-12-19
    private       int             mSlotViewPadding    = 0;
    private       int             mBottomPadding      = 0;
    private final GLView          mRootPane           = new GLView() {
        private final float mMatrix[] = new float[16];

        @Override
        protected void render(GLCanvas canvas) {
            canvas.save(GLCanvas.SAVE_FLAG_MATRIX);
            GalleryUtils.setViewPointMatrix(mMatrix,
                    getWidth() / 2 + mX, getHeight() / 2 + mY, mZ);
            canvas.multiplyMatrix(mMatrix, 0);
            super.render(canvas);
            canvas.restore();
        }

        @Override
        protected void onLayout(
                boolean changed, int left, int top, int right, int bottom) {
            mEyePosition.resetPosition();

            //*/ Modified by Tyd Linguanrong for adjust glroot view layout, 2014-6-12
            int slotViewTop = mActionBar.getHeight() + mConfig.paddingLeftRight + mActivity.mStatusBarHeight;
            //*/
            int slotViewBottom = bottom - top - mConfig.paddingBottom;
            int slotViewLeft = 0;
            int slotViewRight = 0;
//            if (left > 10) {
                slotViewLeft = left + mConfig.paddingLeftRight;
                slotViewRight = right - left - mConfig.paddingLeftRight;
//            } else {
//                slotViewLeft = left;
//                slotViewRight = right - left - mConfig.paddingLeftRight;
//            }

            if (mShowDetails) {
                mDetailsHelper.layout(left, slotViewTop, right, bottom);
            } else {
                mAlbumSetView.setHighlightItemPath(null);
            }

            mSlotView.layout(slotViewLeft, slotViewTop, slotViewRight,
                    slotViewBottom - mBottomPadding - mSlotViewPadding);
        }
    };
    //*/
    private       boolean         mResumeSelection    = false;
    private       boolean         resumeFromCommunity = false;
    private SharedPreferences mSharedPref;
    private Editor            mEditor;
    private EditText          mEditText;
    private AlertDialog       mDialog;
    private Button            mPositiveBtn;
    private int     mStoryBucketId = -1;
    private int     mRenameItemId  = -1;
//    private boolean mBabyAlbum     = false;
    private boolean toSetDate      = false;
    private DatePickerDialog mDatePickerDialog;
    private int mPickAlbumIndex = -1;
    private Dialog             mGuideDialog;
    //*/
    //*/ Added by droi Linguanrong for lock orientation, 16-3-1
    private OrientationManager mOrientationManager;

    @Override
    protected int getBackgroundColorId() {
        return R.color.albumset_background;
    }

    @Override
    public void onEyePositionChanged(float x, float y, float z) {
        mRootPane.lockRendering();
        mX = x;
        mY = y;
        mZ = z;
        mRootPane.unlockRendering();
        mRootPane.invalidate();
    }

    @Override
    public void doCluster(int clusterType) {
        if (mGuideDialog != null) {
            mGuideDialog.dismiss();
            mGuideDialog = null;
        }

        if (mSelectionManager != null && mSelectionManager.inSelectionMode()) {
            mSelectionManager.leaveSelectionMode();
        }

        String basePath, newPath;
        Bundle data = new Bundle(getData());

        if (clusterType == FreemeUtils.CLUSTER_BY_CAMERE) {
            newPath = mActivity.getDataManager().makeCameraSetPath();
            boolean mTimeShaftPage = mActivity.mSharedPreferences.getBoolean("default_page", true);
            if (mTimeShaftPage) {
                data.putString(AlbumTimeShaftPage.KEY_MEDIA_PATH, newPath);
                mActivity.getStateManager().switchState(this, AlbumTimeShaftPage.class, data);
            } else {
                data.putString(AlbumCameraPage.KEY_MEDIA_PATH, newPath);
                mActivity.getStateManager().switchState(this, AlbumCameraPage.class, data);
            }
        } else if (FreemeUtils.CLUSTER_BY_ALBUM == clusterType) {
            basePath = mActivity.getDataManager().getTopSetPath(DataManager.INCLUDE_ALL);
            newPath = FilterUtils.switchClusterPath(basePath, clusterType);
            data.getBoolean(AlbumSetPage.KEY_STORY_SELECT_MODE, false);
            data.putString(AlbumSetPage.KEY_MEDIA_PATH, newPath);
            data.putInt(AlbumSetPage.KEY_SELECTED_CLUSTER_TYPE, clusterType);
            mActivity.getStateManager().switchState(this, AlbumSetPage.class, data);
        } else if (clusterType == FreemeUtils.CLUSTER_BY_COMMUNITY) {
            ((GalleryActivity) mActivity).startCommunity();
        }
    }

    @Override
    public void onSelectionModeChange(int mode) {
        switch (mode) {
            case SelectionManager.ENTER_SELECTION_MODE: {
                mActionModeHandler.startActionMode();
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                mResumeSelection = false;
                //mAlbumSetView.setInSelectionMode(true);
                mSlotView.invalidate();
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
    public void onBackPressed() {
        if (mShowDetails) {
            hideDetails();
        } else if (mSelectionManager.inSelectionMode()) {
            mSelectionManager.leaveSelectionMode();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onSelectionChange(Path path, boolean selected) {
        updateSelectionAll();
        mActionModeHandler.setTitle(getSelectedString());
        mActionModeHandler.updateSupportedOperation(path, selected);
        updateMenuRename();
    }

    @Override
    public void onSelectionRestoreDone() {

    }

    private void getSlotCenter(int slotIndex, int center[]) {
        Rect offset = new Rect();
        mRootPane.getBoundsOf(mSlotView, offset);
        Rect r = mSlotView.getSlotRect(slotIndex);
        int scrollX = mSlotView.getScrollX();
        int scrollY = mSlotView.getScrollY();
        center[0] = offset.left + (r.left + r.right) / 2 - scrollX;
        center[1] = offset.top + (r.top + r.bottom) / 2 - scrollY;
    }

    public void updateSelectionAll() {
        if (FaceAlbumSet.isNotMaxAlbum) {
            return;
        }

        MediaSet setAdd = mAlbumSetDataAdapter.getMediaSet(mAlbumSetDataAdapter.size() - 1);
        Path path = setAdd.getPath();
        ArrayList<Path> list = mSelectionManager.getSelected(false);
        if (list.contains(path)) {
            mSelectionManager.removeContainsPath(path);
        } else if (!list.contains(path)
                && list.size() == mAlbumSetDataAdapter.size() - 1) {
            mSelectionManager.selectAll();
        }
    }

    public void onSingleTapUp(int slotIndex) {
        if (!mIsActive) return;

        if (mSelectionManager.inSelectionMode()) {
            MediaSet targetSet = mAlbumSetDataAdapter.getMediaSet(slotIndex);
            if (targetSet == null) return; // Content is dirty, we shall reload soon
            mSelectionManager.toggle(targetSet.getPath());
            mSlotView.invalidate();
        } else {
            // Show pressed-up animation for the single-tap.
            mAlbumSetView.setPressedIndex(slotIndex);
            mAlbumSetView.setPressedUp();
            mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_PICK_ALBUM, slotIndex, 0),
                    FadeTexture.DURATION);
        }
    }

    public String getSelectedString() {
        if (mAlbumSetDataAdapter.size() == 0) {
            return "";
        }
        MediaSet set = mAlbumSetDataAdapter.getMediaSet(mAlbumSetDataAdapter.size() - 1);
        ArrayList<Path> list = mSelectionManager.getSelected(false);

        int count = mSelectionManager.getSelectedCount();
        if (set != null && list.contains(set.getPath()) && !FaceAlbumSet.isNotMaxAlbum) {
            count--;
        }
        int string = R.plurals.number_of_items_selected;
        String format = mActivity.getResources().getQuantityString(string, count);
        return String.format(format, count);
    }

    private void CreateDialog() {
        final Resources res = mActivity.getResources();
        View view = LayoutInflater.from(mActivity).inflate(R.layout.new_story_album_layout, null);
        mEditText = (EditText) view.findViewById(R.id.custom_label);
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                boolean enable = s.toString().trim().length() > 0;
                mPositiveBtn.setEnabled(enable);
                mPositiveBtn.setTextColor(enable ? res.getColor(R.color.dialog_button_text_color_enable)
                        : res.getColor(R.color.dialog_button_text_color_disable));
            }
        });

        mDialog = new AlertDialog.Builder(mActivity)
                .setTitle(R.string.add_story_album)
                .setView(view)
                .setPositiveButton(R.string.add_images,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int i) {
                                final String text = mEditText.getText().toString().trim();
                                if (!"".equals(text)) {
                                    if (((FaceAlbumSet) mMediaSet).checkIsContainName(mRenameItemId, text)) {
                                        Toast.makeText(mActivity, R.string.contain_name, Toast.LENGTH_SHORT).show();
                                        mHandler.postDelayed(new Runnable() {
                                            public void run() {
                                                showDialog(mRenameItemId == -1 ? "" :
                                                        (mMediaSet.getSubMediaSet(mRenameItemId)).getName());
                                            }
                                        }, 100);
                                    } else {
                                        if (mRenameItemId != -1) {
                                            ((FaceAlbumSet) mMediaSet).startRenameAction(mRenameItemId, text);
                                            mAlbumSetView.resume();
                                        } else {
                                            mAddNewAlbum = true;
                                            mStoryBucketId = ((FaceAlbumSet) mMediaSet).addAlbum(text);
                                            startStoryAddImagePage(mStoryBucketId);
                                            //*/ Added by tyd Linguanrong for statistic, 15-12-18
//                                            StatisticUtil.generateStatisticInfo(mActivity, StatisticData.OPTION_ALBUM_ADD);
                                            //*/

                                            // for baas analytics
//                                            DroiAnalytics.onEvent(mActivity, StatisticData.OPTION_ALBUM_ADD);
                                        }
                                        mRenameItemId = -1;
                                    }
                                }
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int i) {
                            }
                        })
                .create();
    }

    public void updateMenuRename() {
        MenuExecutor.updateMenuRename(mActionBar.getMenu(), canShowRename());
    }

//    private void CreateDatePickerDialog() {
//        Time t = new Time();
//        t.setToNow();
//
//        final OnDateSetListener dateSetListener = new OnDateSetListener() {
//            @Override
//            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
//                if (toSetDate) {
//                    toSetDate = false;
//                    Calendar calendar = Calendar.getInstance();
//                    calendar.set(year, monthOfYear, dayOfMonth);
//                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
//                    String str = dateFormat.format(calendar.getTime());
//
//                    mEditor.remove(mBabyAlbum ? FreemeUtils.BABY_DESCRIPTION : FreemeUtils.LOVE_DESCRIPTION);
//                    mEditor.putString(mBabyAlbum ? FreemeUtils.BABY_BIRTHDAY : FreemeUtils.LOVE_DATE, str);
//                    mEditor.commit();
//                }
//            }
//        };
//
//        mDatePickerDialog = new DatePickerDialog(mActivity, dateSetListener, t.year, t.month, t.monthDay);
//        final DatePicker datePicker = mDatePickerDialog.getDatePicker();
//        configureDatePicker(datePicker);
//        mDatePickerDialog.setButton(DialogInterface.BUTTON_POSITIVE,
//                mActivity.getResources().getString(R.string.add_images),
//                new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int i) {
//                        toSetDate = true;
//                        dateSetListener.onDateSet(datePicker, datePicker.getYear(),
//                                datePicker.getMonth(), datePicker.getDayOfMonth());
//                        startStoryAddImagePage(mStoryBucketId);
//                    }
//                });
//
//        mDatePickerDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
//            @Override
//            public void onDismiss(DialogInterface dialog) {
//                Time t = new Time();
//                t.setToNow();
//                mDatePickerDialog.updateDate(t.year, t.month, t.monthDay);
//            }
//        });
//    }

    public boolean canShowRename() {
        MediaSet set = mAlbumSetDataAdapter.getMediaSet(mAlbumSetDataAdapter.size() - 1);
        ArrayList<Path> list = mSelectionManager.getSelected(false);

        int count = mSelectionManager.getSelectedCount();
        if (list.contains(set.getPath())) {
            count--;
        }

        return count == 1;
    }

    private void configureDatePicker(DatePicker datePicker) {
        // The system clock can't represent dates outside this range.
        Calendar t = Calendar.getInstance();
        t.clear();
        t.set(1970, Calendar.JANUARY, 1);
        datePicker.setMinDate(t.getTimeInMillis());
        t.clear();
        t.set(2037, Calendar.DECEMBER, 31);
        datePicker.setMaxDate(t.getTimeInMillis());
    }


//
//    private void createGuideDialog(final boolean babyAlbum) {
//        final Dialog dialog = new Dialog(mActivity, R.style.GuideFullScreen);
//        View view = LayoutInflater.from(mActivity).inflate(R.layout.story_guide, null);
//        boolean international = FreemeUtils.isInternational(mActivity);
//        if (international) {
//            view.findViewById(R.id.main_bg).setBackgroundResource(
//                    babyAlbum ? R.drawable.guide_baby_en : R.drawable.guide_love_en);
//        } else {
//            view.findViewById(R.id.main_bg).setBackgroundResource(
//                    babyAlbum ? R.drawable.guide_baby : R.drawable.guide_love);
//        }
//
//        ImageView cancel = (ImageView) view.findViewById(R.id.cancel);
//        cancel.setBackgroundResource(international ? R.drawable.guide_cancel_en : R.drawable.guide_cancel);
//        cancel.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                dialog.dismiss();
//            }
//        });
//
//        ImageView confirm = (ImageView) view.findViewById(R.id.confirm);
//        confirm.setBackgroundResource(international ? R.drawable.guide_confirm_en : R.drawable.guide_confirm);
//        confirm.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mStoryBucketId = babyAlbum ? FaceAlbumSet.ALBUM_BABY_ID : FaceAlbumSet.ALBUM_LOVE_ID;
//                mDatePickerDialog.setTitle(babyAlbum ?
//                        mActivity.getResources().getString(R.string.baby_birthday) :
//                        mActivity.getResources().getString(R.string.love_date));
//                mDatePickerDialog.show();
//                dialog.dismiss();
//            }
//        });
//        dialog.setContentView(view);
//        dialog.show();
//    }

    @Override
    public void onSyncDone(final MediaSet mediaSet, final int resultCode) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                GLRoot root = mActivity.getGLRoot();
                root.lockRenderThread();
                try {
                    if (resultCode == MediaSet.SYNC_RESULT_SUCCESS) {
                        mInitialSynced = true;
                    }
                    clearLoadingBit(BIT_LOADING_SYNC);
                } finally {
                    root.unlockRenderThread();
                }
            }
        });
    }

    private void showGuideDialog() {
        WindowManager windowManager = mActivity.getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getMetrics(displayMetrics);

        int left = displayMetrics.widthPixels / 3;
        int top = mActionBar.getHeight() + mConfig.paddingTop + mActivity.mStatusBarHeight;
        int right = left * 2;
        int bottom = top + left;
        final Rect rect = new Rect(left, top, right, bottom);
        mGuideDialog = new Dialog(mActivity, R.style.GuideNotFullScreen);
        View view = new View(mActivity.getAndroidContext());
        view.setBackgroundResource(FreemeUtils.isInternational(mActivity)
                ? R.drawable.guide_story_en : R.drawable.guide_story);
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        int getX = (int) event.getX();
                        int getY = (int) event.getY();
                        if (rect.contains(getX, getY)) {
//                            createGuideDialog(false);
                            mEditor.putBoolean(SHOW_STORY_GUIDE, false);
                            mEditor.commit();
                        }
                        mGuideDialog.dismiss();
                        break;
                }
                return true;
            }
        });
        mGuideDialog.setContentView(view);
        mGuideDialog.show();
    }

    @Override
    public void onEnterState() {
        mActivity.showNavi(AbstractGalleryActivity.IN_ALBUMPAGE);
    }

    @Override
    public void observe() {
        if (mActivity.mFreemeActionBarContainer != null) {
            mFreemeHomeView = mActivity.mFreemeActionBarContainer.findViewById(com.freeme.gallery.R.id.freeme_home_view);
            mFreemeHomeView.findViewById(com.freeme.gallery.R.id.up).setOnClickListener(this);
            mFreemeActionBarBackTitle = mFreemeHomeView.findViewById(com.freeme.gallery.R.id.freeme_actionbar_back_title);
            mFreemeActionBarBackTitle.setText(com.android.gallery3d.R.string.tab_by_story);
            mFreemeTitleLayout = mActivity.mFreemeActionBarContainer.findViewById(com.freeme.gallery.R.id.freeme_title_layout);
            ((TextView)(mFreemeTitleLayout.findViewById(com.freeme.gallery.R.id.action_bar_title))).setText(R.string.face_album);
            mFreemeActionBarBackTitle.setOnClickListener(this);
            mActivity.setTopbarBackgroundColor(com.android.gallery3d.R.color.primary_freeme_light);

        }
    }
    //*/

    private class MyLoadingListener implements LoadingListener {
        @Override
        public void onLoadingStarted() {
            setLoadingBit(BIT_LOADING_RELOAD);
        }

        @Override
        public void onLoadingFinished(boolean loadingFailed) {
            clearLoadingBit(BIT_LOADING_RELOAD);
            if (mAddNewAlbum) {
                mAddNewAlbum = false;
                ((FaceAlbumSet) mMediaSet).removeInvalidNewAlbum();
            }
        }
    }

    private void showDialog(String text) {
        mDialog.show();
        mDialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        mPositiveBtn = mDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        boolean enable = !"".equals(text);
        mPositiveBtn.setEnabled(enable);
        mPositiveBtn.setText(mRenameItemId != -1 ? R.string.ok : R.string.add_images);
        Resources res = mActivity.getResources();
        mPositiveBtn.setTextColor(enable ? res.getColor(R.color.dialog_button_text_color_enable)
                : res.getColor(R.color.dialog_button_text_color_disable));
        mEditText.setText(text);
        if (text != null) {
            mEditText.setSelection(text.length());
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
            return mAlbumSetDataAdapter.size();
        }

        @Override
        public int setIndex() {
            Path id = mSelectionManager.getSelected(false).get(0);
            mIndex = mAlbumSetDataAdapter.findSet(id);
            return mIndex;
        }

        @Override
        public MediaDetails getDetails() {
            MediaObject item = mAlbumSetDataAdapter.getMediaSet(mIndex);
            if (item != null) {
                mAlbumSetView.setHighlightItemPath(item.getPath());
                return item.getDetails();
            } else {
                return null;
            }
        }
    }

    private void startStoryAddImagePage(int storyBucketId) {
        Bundle data = new Bundle();
        data.putBoolean(GalleryActivity.KEY_GET_CONTENT, true);
        data.putBoolean(AlbumSetPage.KEY_STORY_SELECT_MODE, true);
        data.putBoolean(AlbumSetPage.KEY_STORY_FROM_CHILD, false);
        data.putInt(AlbumSetPage.KEY_STORY_SELECT_INDEX, storyBucketId);
        data.putString(AlbumSetPage.KEY_MEDIA_PATH,
                mActivity.getDataManager().getTopSetPath(DataManager.INCLUDE_ALL));
        mActivity.getStateManager().startState(AlbumSetPage.class, data);
    }

    private void pickAlbum(int slotIndex) {
        mPickAlbumIndex = slotIndex;
        if (!mIsActive) return;

        MediaSet targetSet = mAlbumSetDataAdapter.getMediaSet(slotIndex);
        if (targetSet == null) return; // Content is dirty, we shall reload soon
        if (slotIndex == mAlbumSetDataAdapter.size() - 1
                && !FaceAlbumSet.isNotMaxAlbum
                && targetSet.getTotalMediaItemCount() == 0) {
            mRenameItemId = -1;
            showDialog("");
            return;
        }

        //*/ Added by droi Linguanrong for freeme gallery, 16-1-13
        ((GalleryActivity) mActivity).setBottomTabVisibility(false);
        //*/

        String mediaPath = targetSet.getPath().toString();

        Bundle data = new Bundle(getData());
        int[] center = new int[2];
        getSlotCenter(slotIndex, center);
/*        if (slotIndex == FaceAlbumSet.ALBUM_BABY_ID ) {
            mBabyAlbum = slotIndex == FaceAlbumSet.ALBUM_BABY_ID;
            String basePath = mActivity.getDataManager().getTopSetPath(DataManager.INCLUDE_ALL);
            String newPath = FilterUtils.switchClusterPath(basePath, FreemeUtils.CLUSTER_BY_ALBUM);
            data.getBoolean(AlbumSetPage.KEY_STORY_SELECT_MODE, false);
            data.putString(AlbumSetPage.KEY_MEDIA_PATH, newPath);
            data.putInt(AlbumSetPage.KEY_SELECTED_CLUSTER_TYPE, FreemeUtils.CLUSTER_BY_ALBUM);
            mActivity.getStateManager().startStateForResult(
                    AlbumSetPage.class, REQUEST_DO_ANIMATION, data);
            return;
        }*/
        if (mGetAlbum && targetSet.isLeafAlbum()) {
            Activity activity = mActivity;
            Intent result = new Intent()
                    .putExtra(AlbumPicker.KEY_ALBUM_PATH, targetSet.getPath().toString());
            activity.setResult(Activity.RESULT_OK, result);
            activity.finish();
        } else if (targetSet.getSubMediaSetCount() > 0 ) {
            data.putString(AlbumFaceSetPage.KEY_MEDIA_PATH, mediaPath);
            mActivity.getStateManager().startStateForResult(
                    AlbumFaceSetPage.class, REQUEST_DO_ANIMATION, data);
        } else {
            data.putString(AlbumStoryPage.KEY_MEDIA_PATH, mediaPath);
            //*/ Modified by Tyd Linguanrong for secret photos, 2014-5-29
            if (targetSet instanceof FaceMergeAlbum) {
                mStoryBucketId = ((FaceMergeAlbum) targetSet).getStoryBucketId();
            } else {
                mStoryBucketId = ((FaceAlbum) targetSet).getStoryBucketId();
            }
            if (targetSet.getMediaItemCount() < 1) return;
            //data.putInt(AlbumStoryPage.KEY_SELECT_INDEX, slotIndex);
            data.putInt(AlbumStoryPage.KEY_STORY_SELECT_INDEX, mStoryBucketId);

            mActivity.getStateManager().startStateForResult(
                    AlbumClassifierPage.class, REQUEST_DO_ANIMATION, data);
//                      AlbumPage.class, REQUEST_DO_ANIMATION, data);

                    //*/
        }
    }

    private void onDown(int index) {
        mAlbumSetView.setPressedIndex(index);
    }

    private void onUp(boolean followedByLongPress) {
        if (followedByLongPress) {
            // Avoid showing press-up animations for long-press.
            mAlbumSetView.setPressedIndex(-1);
        } else {
            mAlbumSetView.setPressedUp();
        }
    }

    public void onLongTap(int slotIndex) {
        if (mGetContent || mGetAlbum) return;
        MediaSet set = mAlbumSetDataAdapter.getMediaSet(slotIndex);
        if (set == null) {
            return;
        }

        if (slotIndex == mAlbumSetDataAdapter.size() - 1
                && !FaceAlbumSet.isNotMaxAlbum
                && set.getTotalMediaItemCount() == 0) {
            return;
        }
        mSelectionManager.setAutoLeaveSelectionMode(true);
        mSelectionManager.toggle(set.getPath());
        mSlotView.invalidate();
    }
    private LinearLayout mFreemeHomeView;
    private TextView mFreemeActionBarBackTitle;
    private LinearLayout mFreemeTitleLayout;

    @Override
    public void onCreate(Bundle data, Bundle restoreState) {
        super.onCreate(data, restoreState);

        mSharedPref = mActivity.getSharedPreferences(
                FreemeUtils.FACE_SHAREPREFERENCE_KEY, Context.MODE_PRIVATE);
        mEditor = mSharedPref.edit();

        initializeViews();
        initializeData(data);
//        observe();
        Context context = mActivity.getAndroidContext();
        mGetContent = data.getBoolean(GalleryActivity.KEY_GET_CONTENT, false);
        mGetAlbum = data.getBoolean(GalleryActivity.KEY_GET_ALBUM, false);
        mTitle = data.getString(AlbumFaceSetPage.KEY_SET_TITLE);
        mEyePosition = new EyePosition(context, this);
        mDetailsSource = new MyDetailsSource();
        mActionBar = mActivity.getGalleryActionBar();
        mSelectedAction = data.getInt(AlbumFaceSetPage.KEY_SELECTED_CLUSTER_TYPE,
                FreemeUtils.CLUSTER_BY_STORY);
        //*/ Added by droi Linguanrong for freeme gallery, 16-1-14
        resumeFromCommunity = data.getBoolean(FreemeUtils.KEY_FROM_COMMUNITY);
        //*/

        mHandler = new SynchronizedHandler(mActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_PICK_ALBUM: {
                        pickAlbum(message.arg1);
                        break;
                    }
                    default:
                        throw new AssertionError(message.what);
                }
            }
        };

        CreateDialog();

//        CreateDatePickerDialog();

        //*/ Added by Linguanrong for guide, 2015-08-10
//        if (mSharedPref.getBoolean(SHOW_STORYSET_GUIDE, true)) {
//            mEditor.putBoolean(SHOW_STORYSET_GUIDE, false);
//            mEditor.apply();
//            showGuideDialog();
//        }
        //*/

    }

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

    @Override
    public void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(mActivity)
                .unregisterReceiver(onEvent);

        mActionModeHandler.destroy();
    }

    private void clearLoadingBit(int loadingBit) {
        mLoadingBits &= ~loadingBit;
        if (mLoadingBits == 0 && mIsActive) {
            if (mAlbumSetDataAdapter.size() == 0) {
                mSlotView.invalidate();
            }
        }
    }

    private void setLoadingBit(int loadingBit) {
        mLoadingBits |= loadingBit;
    }

    @Override
    public void onPause() {
        super.onPause();
        //*/
        LocalBroadcastManager.getInstance(mActivity)
                .unregisterReceiver(onEvent);
        /*/
       mActivity.unregisterReceiver(onEvent);
       //*/
        mIsActive = false;
        mActivity.mIsSelectionMode = mSelectionManager != null && mSelectionManager.inSelectionMode();

        // avoid mediaSet is null
        if (mMediaSet == null) {
            return;
        }

        if (mActivity.mIsSelectionMode) {
            mSelectionManager.leaveSelectionMode();
        }

        mAlbumSetDataAdapter.pause();
        mAlbumSetView.pause();
        mActionModeHandler.pause();
        mEyePosition.pause();
        DetailsHelper.pause();
        // Call disableClusterMenu to avoid receiving callback after paused.
        // Don't hide menu here otherwise the list menu will disappear earlier than
        // the action bar, which is janky and unwanted behavior.
        mActionBar.disableClusterMenu(false);
        if (mSyncTask != null) {
            mSyncTask.cancel();
            mSyncTask = null;
            clearLoadingBit(BIT_LOADING_SYNC);
        }
    }
    private BroadcastReceiver onEvent=new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent i) {
            switch (i.getAction()) {
                case GalleryClassifierService.ACTION_FACE_COMPLETE:
                    if (!isDestroyed() || mIsActive) {
                        // freeme.gulincheng 2018.0601 don't modify showToast here ,or it will get crushed.
                        showToast(mActivity, mActivity.getResources().getString(R.string.classifying_faces)
                                + i.getStringExtra("storycount"));
                    }
                    break;
                case GalleryClassifierService.ACTION_ADDFACEALBUM:
                    if (mMediaSet != null) {
                        mAddNewAlbum = true;
                        LogUtil.i("testface :" + "in addalbum");
                        ((FaceAlbumSet) mMediaSet).addAlbum(i.getStringExtra("addalbum"));
                    }
                    break;

                case GalleryClassifierService.ACTION_FACEDONE:
                    ((FaceAlbumSet) mMediaSet).updateAlbumMap();
                    ((FaceAlbumSet) mMediaSet).fakeChange();
                    mSlotView.invalidate();
                    break;
            }

        }
    };

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter f=new IntentFilter(GalleryClassifierService.ACTION_ADDFACEALBUM);
        f.addAction(GalleryClassifierService.ACTION_FACE_COMPLETE);
        f.addAction(GalleryClassifierService.ACTION_FACEDONE);
        //*/
        LocalBroadcastManager.getInstance(mActivity)
                .registerReceiver(onEvent, f);
        if (mMediaSet != null) {
            ((FaceAlbumSet) mMediaSet).updateAlbumMap();
        }

        mActivity.getNavigationWidgetManager().changeStateTo(this);
        mActivity.getNavigationWidgetManager().observe();

        mActionBar.initActionBar();
        mActivity.getNavigationWidgetManager().changeStateTo(this);
        //*/ Added by droi Linguanrong for lock orientation, 16-3-1
        mOrientationManager.lockOrientation(true);
        //*/

        //*/ Added by droi Linguanrong for freeme gallery, 16-1-13
        ((GalleryActivity) mActivity).setBottomTabVisibility(true);
        mActionBar.enableClusterMenu(mSelectedAction, this);
        if (resumeFromCommunity) {
            resumeFromCommunity = false;
            startIntroAnimation();
        }
        //*/

        mIsActive = true;
        setContentPane(mRootPane);

        // Set the reload bit here to prevent it exit this page in clearLoadingBit().
        setLoadingBit(BIT_LOADING_RELOAD);
        //*/ Added by Tyd Linguanrong for Gallery new style, 2014-2-13
        mResumeSelection = false;
        //*/

        // avoid mediaSet is null
        if (mMediaSet == null) {
            return;
        }

        mAlbumSetDataAdapter.resume();

        mAlbumSetView.resume();
        mEyePosition.resume();
        mActionModeHandler.resume();
        if (!mInitialSynced) {
            setLoadingBit(BIT_LOADING_SYNC);
            mSyncTask = mMediaSet.requestSync(AlbumFaceSetPage.this);
        }

        Intent i=new Intent(mActivity, GalleryClassifierService.class);
        i.setAction(GalleryClassifierService.ACTION_FACE);

        GalleryClassifierService.enqueueWork(mActivity, i);

    }

    private void initializeData(Bundle data) {
        String mediaPath = data.getString(AlbumFaceSetPage.KEY_MEDIA_PATH);
        mMediaSet = mActivity.getDataManager().getMediaSet(mediaPath);
        if (mMediaSet == null) {
            Utils.fail("MediaSet is null. Path = %s", mediaPath);
            return;
        }
        mSelectionManager.setSourceMediaSet(mMediaSet);
        mAlbumSetDataAdapter = new AlbumSetDataLoader(
                mActivity, mMediaSet, DATA_CACHE_SIZE);
        mAlbumSetDataAdapter.setLoadingListener(new MyLoadingListener());
        mAlbumSetView.setModel(mAlbumSetDataAdapter);
    }

    private void initializeViews() {
        mSelectionManager = new SelectionManager(mActivity, true);
        mSelectionManager.setSelectionListener(this);

        mConfig = PageConfig.AlbumFaceSetPage.get(mActivity);
        mSlotView = new FaceSlotView(mActivity, mConfig.slotViewSpec,mConfig.labelSpec);
        mSlotViewPadding = mConfig.slotViewSpec.slotPadding;
        mBottomPadding = mConfig.slotViewSpec.bottomPadding;
        mAlbumSetView = new FaceAlbumSetSlotRender(
                mActivity, mSelectionManager, mSlotView, mConfig.labelSpec,
                mConfig.placeholderColor);
        mSlotView.setSlotRenderer(mAlbumSetView);
        mSlotView.setListener(new SlotView.SimpleListener() {
            @Override
            public void onDown(int index) {
                AlbumFaceSetPage.this.onDown(index);
            }

            @Override
            public void onUp(boolean followedByLongPress) {
                AlbumFaceSetPage.this.onUp(followedByLongPress);
            }

            @Override
            public void onSingleTapUp(int slotIndex) {
                AlbumFaceSetPage.this.onSingleTapUp(slotIndex);
            }

            @Override
            public void onLongTap(int slotIndex) {
                AlbumFaceSetPage.this.onLongTap(slotIndex);
            }
        });

        mActionModeHandler = new ActionModeHandler(mActivity, mSelectionManager);
        mActionModeHandler.setActionModeListener(new ActionModeHandler.ActionModeListener() {
            @Override
            public boolean onActionItemClicked(MenuItem item,int menuItemid) {
                return onItemSelected(item, menuItemid);
            }
        });

        mRootPane.addComponent(mSlotView);

        //*/ Added by droi Linguanrong for lock orientation, 16-3-1
        mOrientationManager = mActivity.getOrientationManager();
        mActivity.getGLRoot().setOrientationSource(mOrientationManager);
        //*/
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
        Activity activity = mActivity;
        switch (itemId) {
            //*/ Added by Tyd Linguanrong for Gallery new style, 2014-3-5
            case android.R.id.home:
                onBackPressed();
                return true;
            //*/

            case R.id.action_cancel:
                activity.setResult(Activity.RESULT_CANCELED);
                activity.finish();
                return true;
            case R.id.action_select:
                mSelectionManager.setAutoLeaveSelectionMode(false);
                mSelectionManager.enterSelectionMode();
                return true;
            case R.id.action_details:
                if (mAlbumSetDataAdapter.size() != 0) {
                    if (mShowDetails) {
                        hideDetails();
                    } else {
                        showDetails();
                    }
                } else {
                    Toast.makeText(activity,
                            activity.getText(R.string.no_albums_alert),
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.action_camera: {
                GalleryUtils.startCameraActivity(activity);
                return true;
            }

            case R.id.action_settings: {
                activity.startActivity(new Intent(activity, GallerySettings.class));
                return true;
            }

//            case R.id.action_rename:
//                mRenameItemId = Math.max(0, mDetailsSource.setIndex());
//                String text = (mMediaSet.getSubMediaSet(mRenameItemId)).getName();
//                showDialog(text);
//                return true;

//            case R.id.action_share_freeme: {
//                ShareFreemeUtil.shareFreemeOS(mActivity);
//                return true;
//            }

            default:
                return false;
        }
    }

    @Override
    protected void onStateResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_DO_ANIMATION: {
                if (mPickAlbumIndex < 0 ) {
                    return;
                }
//                MediaSet set = mAlbumSetDataAdapter.getMediaSet(mPickAlbumIndex);
                mAlbumSetDataAdapter.setItemCover(mPickAlbumIndex,
                        ((FaceAlbumSet) mMediaSet).getItemCover(mPickAlbumIndex));
                mSlotView.startRisingAnimation();
            }
        }
    }


    private void hideDetails() {
        mShowDetails = false;
        mDetailsHelper.hide();
        mAlbumSetView.setHighlightItemPath(null);
        mSlotView.invalidate();
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

    private Toast mToast;

    private void showToast(Activity activity, String storycount) {
        if (mToast != null) {
            mToast.cancel();
        }

        mToast = Toast.makeText(activity, storycount, Toast.LENGTH_SHORT);
        mToast.show();
    }
}

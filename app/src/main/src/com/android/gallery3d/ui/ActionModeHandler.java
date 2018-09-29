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

package com.android.gallery3d.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Handler;
import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.ShareActionProvider;
import android.widget.ShareActionProvider.OnShareTargetSelectedListener;
import android.widget.TextView;
import android.widget.Toast;

import com.droi.sdk.analytics.DroiAnalytics;
import com.freeme.community.utils.ToastUtil;
import com.freeme.gallery.R;
import com.freeme.gallery.app.AbstractGalleryActivity;
import com.android.gallery3d.app.ActivityState;
import com.android.gallery3d.app.AlbumSetPage;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.ui.MenuExecutor.ProgressListener;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.ThreadPool;
import com.freeme.gallery.app.GalleryActivity;
import com.freeme.page.AlbumStoryCoverPage;
import com.freeme.page.AlbumStoryPage;
import com.freeme.page.AlbumStorySetPage;
import com.freeme.scott.galleryui.design.widget.FreemeBottomSelectedController;
import com.freeme.scott.galleryui.design.widget.FreemeBottomSelectedView;
import com.freeme.statistic.StatisticData;
import com.freeme.statistic.StatisticUtil;
import com.freeme.ui.manager.State;
import com.freeme.utils.FreemeCustomUtils;

import java.util.ArrayList;

public class ActionModeHandler implements Callback, PopupList.OnPopupItemClickListener ,State{

    @SuppressWarnings("unused")
    private static final String TAG = "ActionModeHandler";

    private static final int MAX_SELECTED_ITEMS_FOR_SHARE_INTENT          = 300;
    private static final int MAX_SELECTED_ITEMS_FOR_PANORAMA_SHARE_INTENT = 10;

    private static final int SUPPORT_MULTIPLE_MASK = MediaObject.SUPPORT_DELETE
            | MediaObject.SUPPORT_ROTATE | MediaObject.SUPPORT_SHARE
            | MediaObject.SUPPORT_CACHE;
    private static final int MAX_SHARE_COUNT = 100;
    private final AbstractGalleryActivity mActivity;
    private final MenuExecutor            mMenuExecutor;
    private final SelectionManager        mSelectionManager;
    private final NfcAdapter              mNfcAdapter;
    private final Handler                 mMainHandler;

    private Menu               mMenu;
    private MenuItem           mShareMenuItem;
    private SelectionMenu      mSelectionMenu;
    private MenuItem           mSelectMenuItem;
    private ActionModeListener mListener;
    private Future<?>          mMenuTask;
    private ActionMode         mActionMode;

    //*/ Added by Tyd Linguanrong for Gallery new style, 2014-2-13
    private CheckBox mCheckBox;
    private Intent mShareIntent = null;
    //*/
    private WakeLockHoldingProgressListener mDeleteProgressListener;
    //*/ Added by Linguanrong for story album, 2015-5-29
    private boolean mIsStoryCoverPage = false;
    private boolean mHideMenu         = false;

    private int mSelectedItemCount = 0;
    private MenuItem mConfirmMenu;
    private int mLastState;


    @Override
    public boolean onPopupItemClick(int itemId) {
        GLRoot root = mActivity.getGLRoot();
        root.lockRenderThread();
        try {
            if (itemId == R.id.action_select_all) {
                updateSupportedOperation();
                mMenuExecutor.onMenuClicked(itemId, null, false, true);
            }
            return true;
        } finally {
            root.unlockRenderThread();
        }
    }

    private void updateStoryMenu() {
        if (mSelectionManager.isInverseSelection()
                && mSelectionManager.getSelectedCount() == 1
                && mActivity.getStateManager().getStateCount() != 0) {

            ActivityState topState = mActivity.getStateManager().getTopState();
            if (topState != null && topState instanceof AlbumStorySetPage) {
                MenuExecutor.updateMenuDelete(mMenu, false);
                MenuExecutor.updateMenuShare(mMenu, false);
            }
        }
    }

    public void setTitle(String title) {
        if (mActionMode == null) return;
        mActionMode.setTitle(title);
    }

    @TargetApi(ApiHelper.VERSION_CODES.JELLY_BEAN)
    private void setNfcBeamPushUris(Uri[] uris) {
        if (mNfcAdapter != null && ApiHelper.HAS_SET_BEAM_PUSH_URIS) {
            mNfcAdapter.setBeamPushUrisCallback(null, mActivity);
            mNfcAdapter.setBeamPushUris(uris, mActivity);
        }
    }







    public void updateSupportedOperation(Path path, boolean selected) {
        updateSupportedOperation();
    }



    private void updateMenuCrop() {
        if (mActivity.getStateManager().getStateCount() != 0) {
            ActivityState topState = mActivity.getStateManager().getTopState();
            if (topState != null && topState instanceof AlbumStoryPage) {
                MenuExecutor.updateMenuCrop(mMenu, false);
            }
        }
    }

    public void shoulHideMenu(boolean hide) {
        mHideMenu = hide;
    }

    @Override
    public void onEnterState() {
        mLastState = mActivity.getmCurrentState();
        mActivity.showNavi(AbstractGalleryActivity.IN_SELECTMODE);
    }

    @Override
    public void observe() {

    }

    public interface ActionModeListener {
        boolean onActionItemClicked(MenuItem item, int action);
    }

    private static class GetAllPanoramaSupports implements MediaObject.PanoramaSupportCallback {
        public boolean mAllPanoramas   = true;
        public boolean mAllPanorama360 = true;
        public boolean mHasPanorama360 = false;
        private int                   mNumInfoRequired;
        private ThreadPool.JobContext mJobContext;
        private Object mLock = new Object();

        public GetAllPanoramaSupports(ArrayList<MediaObject> mediaObjects, ThreadPool.JobContext jc) {
            mJobContext = jc;
            mNumInfoRequired = mediaObjects.size();
            for (MediaObject mediaObject : mediaObjects) {
                mediaObject.getPanoramaSupport(this);
            }
        }

        @Override
        public void panoramaInfoAvailable(MediaObject mediaObject, boolean isPanorama,
                                          boolean isPanorama360) {
            synchronized (mLock) {
                mNumInfoRequired--;
                mAllPanoramas = isPanorama && mAllPanoramas;
                mAllPanorama360 = isPanorama360 && mAllPanorama360;
                mHasPanorama360 = mHasPanorama360 || isPanorama360;
                if (mNumInfoRequired == 0 || mJobContext.isCancelled()) {
                    mLock.notifyAll();
                }
            }
        }

        public void waitForPanoramaSupport() {
            synchronized (mLock) {
                while (mNumInfoRequired != 0 && !mJobContext.isCancelled()) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException e) {
                        // May be a cancelled job context
                    }
                }
            }
        }
    }

    public ActionModeHandler(
            AbstractGalleryActivity activity, SelectionManager selectionManager) {
        mActivity = Utils.checkNotNull(activity);
        mSelectionManager = Utils.checkNotNull(selectionManager);
        mMenuExecutor = new MenuExecutor(activity, selectionManager);
        mMainHandler = new Handler(activity.getMainLooper());
        mNfcAdapter = NfcAdapter.getDefaultAdapter(mActivity.getAndroidContext());

        //*/ Added by Linguanrong for story album, 2015-6-27
        mIsStoryCoverPage = isStoryCoverPage();
        //*/
    }

    private boolean isStoryCoverPage() {
        if (mActivity.getStateManager().getStateCount() != 0) {
            ActivityState topState = mActivity.getStateManager().getTopState();
            if (topState != null && topState instanceof AlbumStoryCoverPage) {
                return true;
            }
        }
        return false;
    }

    public void startActionMode() {
        Activity a = mActivity;
        mActionMode = a.startActionMode(this);
        mActivity.getNavigationWidgetManager().changeStateTo(this);

//        View customView = LayoutInflater.from(a).inflate(
//                R.layout.action_mode, null);
//        mActionMode.setCustomView(customView);
        setStatusView(true);
//        mSelectionMenu = new SelectionMenu(a,
//                (TextView) customView.findViewById(R.id.selection_menu), this);

        //*/ Added by Tyd Linguanrong for Gallery new style, 2014-2-13
//        mCheckBox = (CheckBox) customView.findViewById(R.id.selection_all);
//        mCheckBox.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                GLRoot root = mActivity.getGLRoot();
//                root.lockRenderThread();
//                if (mSelectionManager.inSelectAllMode()) {
//                    mSelectionManager.deSelectAll();
//                } else {
//                    mSelectionManager.selectAll();
//                }
//                updateSupportedOperation();
//                updateSelectionMenu();
//                root.unlockRenderThread();
//            }
//        });
        //*/
//        TextView cancel = (TextView) customView.findViewById(R.id.cancel);
//        cancel.setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View v) {
//                if (mSelectionManager != null) {
//                    mSelectionManager.leaveSelectionMode();
//                }
//            }
//        });
//        //*/
//
//        //*/ Added by Linguanrong for story album, 2015-6-27


        if (mIsStoryCoverPage) {
//            mCheckBox.setVisibility(View.GONE);
//            confirm.setVisibility(View.VISIBLE);
        }
//        */

//        updateSelectionMenu();
    }

    private void setStatusView(boolean inAction) {
        ActivityState topState = mActivity.getStateManager().getTopState();
        if (topState instanceof AlbumStoryPage) {
            final Window win = mActivity.getWindow();

            if (inAction) {
                win.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                win.setStatusBarColor(mActivity.getResources().getColor(R.color.theme_title_color));
            } else {
                win.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
                win.setStatusBarColor(Color.TRANSPARENT);
            }
        }
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    private int getActionItemId(int action) {
        switch (action) {
            case ACTION_CODE_DELETE:
                return R.id.action_delete;
            case ACTION_CODE_SHARE:
                return R.id.action_share;
            default:
                return -1;
        }
    }
    public boolean onActionItemClicked(int actioncode) {
        GLRoot root = mActivity.getGLRoot();
        root.lockRenderThread();

        try {
            int itemid = getActionItemId(actioncode);

            if (itemid == R.id.action_share) {
                if(mSelectionManager.getSelectedCount() > MAX_SHARE_COUNT){
                    mSelectionManager.leaveSelectionMode();
                    ToastUtil.showToast(mActivity,mActivity.getResources().getString(R.string.max_share_count));
                    return true;
                }else {
                    if (mSelectionManager.getSelected(true).size() > MAX_SHARE_COUNT) {
                        mSelectionManager.leaveSelectionMode();
                        ToastUtil.showToast(mActivity,mActivity.getResources().getString(R.string.max_share_count));
                    } else {
                        mSelectionManager.leaveSelectionMode();
                        mActivity.startActivity(FreemeCustomUtils.createCustomChooser(mActivity, mShareIntent,
                                mActivity.getResources().getString(R.string.share)));
                    }
                    return true;
                }
            }

            boolean result;
            // Give listener a chance to process this command before it's routed to
            // ActionModeHandler, which handles command only based on the action id.
            // Sometimes the listener may have more background information to handle
            // an action command.
            if (mListener != null) {
                result = mListener.onActionItemClicked(null, getActionItemId(actioncode));
                if (result) {
                    mSelectionManager.leaveSelectionMode();
                    return result;
                }
            }

            //*/ Added by Linguanrong for story album, 2015-6-29
            if (actioncode  == ACTION_CODE_DELETE
                    && mActivity.getStateManager().getStateCount() != 0) {
                ActivityState topState = mActivity.getStateManager().getTopState();
                if (topState != null && topState instanceof AlbumStorySetPage) {
                    int type = ((AlbumStorySetPage)topState).containsDefaultAlbum();
                    if(type != -1) {
                        Toast.makeText(mActivity, mActivity.getText(R.string.cannot_delete_tip),
                                Toast.LENGTH_LONG).show();
                    }

                    if(type == 1) {
                        mSelectionManager.leaveSelectionMode();
                        return true;
                    }
                }
            }
            //*/

            ProgressListener listener = null;
            String confirmMsg = null;
            if (actioncode == ACTION_CODE_DELETE) {
                confirmMsg = mActivity.getResources().getQuantityString(
                        R.plurals.delete_selection, mSelectionManager.getSelectedCount());
                if (mDeleteProgressListener == null) {
                    mDeleteProgressListener = new WakeLockHoldingProgressListener(mActivity,
                            "Gallery Delete Progress Listener");
                }
                listener = mDeleteProgressListener;
            }
            mMenuExecutor.onMenuClicked(R.id.action_delete, confirmMsg, listener);
        } finally {
            root.unlockRenderThread();
        }
        return true;
    }
    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        GLRoot root = mActivity.getGLRoot();
        root.lockRenderThread();
        try {
            boolean result;
            // Give listener a chance to process this command before it's routed to
            // ActionModeHandler, which handles command only based on the action id.
            // Sometimes the listener may have more background information to handle
            // an action command.
            if (mListener != null) {
                result = mListener.onActionItemClicked(item, 0);
                if (result) {
                    mSelectionManager.leaveSelectionMode();
                    return result;
                }
            }

            //*/ Added by Linguanrong for story album, 2015-6-29
            if (item.getItemId() == R.id.action_delete
                    && mActivity.getStateManager().getStateCount() != 0) {
                ActivityState topState = mActivity.getStateManager().getTopState();
                if (topState != null && topState instanceof AlbumStorySetPage) {
                    int type = ((AlbumStorySetPage)topState).containsDefaultAlbum();
                    if(type != -1) {
                        Toast.makeText(mActivity, mActivity.getText(R.string.cannot_delete_tip),
                                Toast.LENGTH_LONG).show();
                    }

                    if(type == 1) {
                        mSelectionManager.leaveSelectionMode();
                        return true;
                    }
                }
            }
            //*/

            ProgressListener listener = null;
            String confirmMsg = null;
            int action = item.getItemId();
            if (action == R.id.action_delete) {
                confirmMsg = mActivity.getResources().getQuantityString(
                        R.plurals.delete_selection, mSelectionManager.getSelectedCount());
                if (mDeleteProgressListener == null) {
                    mDeleteProgressListener = new WakeLockHoldingProgressListener(mActivity,
                            "Gallery Delete Progress Listener");
                }
                listener = mDeleteProgressListener;
            }
            mMenuExecutor.onMenuClicked(item, confirmMsg, listener);
        } finally {
            root.unlockRenderThread();
        }
        return true;
    }
    public void finishActionMode() {
        //*/ Added by Linguanrong for story album , 2015-08-08
        mHideMenu = false;
        //*/
        if (mActionMode == null) return;
        mActionMode.finish();
        //*/ Added by Tyd Linguanrong for Gallery new style, 2014-4-23
        mActivity.invalidateOptionsMenu();
        //*/
    }
    private static final int ACTION_CODE_SHARE = 0x100;
    private static final int ACTION_CODE_DELETE = 0x200;
    private static final int[] mActionNames = new int[]{R.string.delete,R.string.share};
    private static final int[] mActionCodes = new int[]{ACTION_CODE_DELETE, ACTION_CODE_SHARE};
    private final OnShareTargetSelectedListener mShareTargetSelectedListener =
            new OnShareTargetSelectedListener() {
                @Override
                public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
                    mSelectionManager.leaveSelectionMode();
                    return false;
                }
            };

    private FreemeBottomSelectedView.IFreemeBottomActionCallBack mCallBack
            = new FreemeBottomSelectedView.IFreemeBottomActionCallBack() {
        @Override
        public void onAction(int actionCode) {
            switch (actionCode) {
                case ACTION_CODE_DELETE:
                    onActionItemClicked(ACTION_CODE_DELETE);
                    break;
                case ACTION_CODE_SHARE:
                    onActionItemClicked(ACTION_CODE_SHARE);
                default:
                    break;
            }
        }
    };


    @Override
    public boolean onCreateActionMode(ActionMode mode, final Menu menu) {
        //*/ Added by Linguanrong for story album, 2015-6-27
        if ( mHideMenu) {
            return true;
        }
        //*/


        //*/ Added by Tyd Linguanrong for secret photos, 2014-3-5
        if (mActivity.mVisitorMode) return true;
        //*/

        if (mIsStoryCoverPage) {
            mActivity.getGalleryActionBar().createActionBarMenu(R.menu.actionmode_story, menu);
            mConfirmMenu = menu.findItem(R.id.action_story_confirm);
            mConfirmMenu.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem arg0) {
                    if (mActivity.getStateManager().getStateCount() != 0) {
                        ActivityState topState = mActivity.getStateManager().getTopState();
                        if (topState != null && topState instanceof AlbumStoryCoverPage) {
                            ((AlbumStoryCoverPage) topState).setCover();
                        }
                    }
                    return true;
                }
            });
        } else {
            ((GalleryActivity)mActivity).getController().showActions(mActionNames, mActionCodes, mCallBack);

            mActivity.getGalleryActionBar().createActionBarMenu(R.menu.operation, menu);
        }
        //*/

        mMenu = menu;
        mShareMenuItem = menu.findItem(R.id.action_share);
        if (mShareMenuItem != null) {
            mShareMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem arg0) {
                    mSelectionManager.leaveSelectionMode();
                    mActivity.startActivity(FreemeCustomUtils.createCustomChooser(mActivity, mShareIntent,
                            mActivity.getResources().getString(R.string.share)));
                    return true;
                }
            });
        }




        mSelectMenuItem = menu.findItem(R.id.action_selectall);
        if (mSelectMenuItem != null) {
            mSelectMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    GLRoot root = mActivity.getGLRoot();
                    root.lockRenderThread();
                    if (mSelectionManager.inSelectAllMode()) {
                        mSelectionManager.deSelectAll();
                        mSelectMenuItem.setTitle(R.string.select_all);

                    } else {
                        mSelectionManager.selectAll();
                        mSelectMenuItem.setTitle(R.string.deselect_all);
                    }
                    updateSupportedOperation();
                    updateSelectionMenu();
                    root.unlockRenderThread();
                    return true;
                }
            });
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mActivity.showNavi(mLastState);
        setStatusView(false);
        ((GalleryActivity)mActivity).getController().hideActions();
        mSelectionManager.leaveSelectionMode();
    }

    private ArrayList<MediaObject> getSelectedMediaObjects(ThreadPool.JobContext jc) {
        ArrayList<Path> unexpandedPaths = mSelectionManager.getSelected(false);
        if (unexpandedPaths.isEmpty()) {
            // This happens when starting selection mode from overflow menu
            // (instead of long press a media object)
            return null;
        }
        ArrayList<MediaObject> selected = new ArrayList<MediaObject>();
        DataManager manager = mActivity.getDataManager();
        for (Path path : unexpandedPaths) {
            if (jc.isCancelled()) {
                return null;
            }
            selected.add(manager.getMediaObject(path));
        }

        return selected;
    }
    // Menu options are determined by selection set itself.
    // We cannot expand it because MenuExecuter executes it based on
    // the selection set instead of the expanded result.
    // e.g. LocalImage can be rotated but collections of them (LocalAlbum) can't.
    private int computeMenuOptions(ArrayList<MediaObject> selected) {
        int operation = MediaObject.SUPPORT_ALL;
        int type = 0;
        for (MediaObject mediaObject : selected) {
            int support = mediaObject.getSupportedOperations();
            type |= mediaObject.getMediaType();
            operation &= support;
        }

        switch (selected.size()) {
            case 1:
                final String mimeType = MenuExecutor.getMimeType(type);
                /*if (!GalleryUtils.isEditorAvailable(mActivity, mimeType)) {
                    operation &= ~MediaObject.SUPPORT_EDIT;
                }*/
                break;
            default:
                operation &= SUPPORT_MULTIPLE_MASK;
        }

        return operation;
    }
    // Share intent needs to expand the selection set so we can get URI of
    // each media item
    private Intent computePanoramaSharingIntent(ThreadPool.JobContext jc, int maxItems) {
        ArrayList<Path> expandedPaths = mSelectionManager.getSelected(true, maxItems);
        if (expandedPaths == null || expandedPaths.size() == 0) {
            return new Intent();
        }
        final ArrayList<Uri> uris = new ArrayList<Uri>();
        DataManager manager = mActivity.getDataManager();
        final Intent intent = new Intent();
        for (Path path : expandedPaths) {
            if (jc.isCancelled()) return null;
            uris.add(manager.getContentUri(path));
        }

        final int size = uris.size();
        if (size > 0) {
            if (size > 1) {
                intent.setAction(Intent.ACTION_SEND_MULTIPLE);
                intent.setType(GalleryUtils.MIME_TYPE_PANORAMA360);
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            } else {
                intent.setAction(Intent.ACTION_SEND);
                intent.setType(GalleryUtils.MIME_TYPE_PANORAMA360);
                intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        return intent;
    }
    private Intent computeSharingIntent(ThreadPool.JobContext jc, int maxItems) {
        ArrayList<Path> expandedPaths = mSelectionManager.getSelected(true, maxItems);
        if (expandedPaths == null || expandedPaths.size() == 0) {
            setNfcBeamPushUris(null);
            return new Intent();
        }
        final ArrayList<Uri> uris = new ArrayList<Uri>();
        DataManager manager = mActivity.getDataManager();
        int type = 0;
        final Intent intent = new Intent();
        for (Path path : expandedPaths) {
            if (jc.isCancelled()) return null;
            int support = manager.getSupportedOperations(path);
            type |= manager.getMediaType(path);

            if ((support & MediaObject.SUPPORT_SHARE) != 0) {
                uris.add(manager.getContentUri(path));
            }
        }

        final int size = uris.size();
        if (size > 0) {
            final String mimeType = MenuExecutor.getMimeType(type);
            if (size > 1) {
                intent.setAction(Intent.ACTION_SEND_MULTIPLE).setType(mimeType);
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            } else {
                intent.setAction(Intent.ACTION_SEND).setType(mimeType);
                intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            setNfcBeamPushUris(uris.toArray(new Uri[uris.size()]));
        } else {
            setNfcBeamPushUris(null);
        }

        return intent;
    }
 public void updateSupportedOperation() {
        // Interrupt previous unfinished task, mMenuTask is only accessed in main thread
        if (mMenuTask != null) mMenuTask.cancel();
        if (mActionMode == null) return;

        updateSelectionMenu();

        //*/ Added by Tyd Linguanrong for secret photos, 2014-3-5
        if (mActivity.mVisitorMode || mHideMenu) return;
        //*/

        //*/ Added by Tyd Linguanrong for secret photos, 2014-3-5
        if (mHideMenu) return;
        //*/

        // Disable share actions until share intent is in good shape
        if (mShareMenuItem != null) mShareMenuItem.setEnabled(false);

        // Generate sharing intent and update supported operations in the background
        // The task can take a long time and be canceled in the mean time.
        mMenuTask = mActivity.getThreadPool().submit(new ThreadPool.Job<Void>() {
            @Override
            public Void run(final ThreadPool.JobContext jc) {
                // Pass1: Deal with unexpanded media object list for menu operation.
                ArrayList<MediaObject> selected = getSelectedMediaObjects(jc);
                if (selected == null) {
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mMenuTask = null;
                            if (jc.isCancelled()) return;
                            // Disable all the operations when no item is selected
                            MenuExecutor.updateMenuOperation(mMenu, 0);
                            MenuExecutor.updateMenuSelect(mMenu, true);
                        }
                    });
                    return null;
                }
                final int operation = computeMenuOptions(selected);
                if (jc.isCancelled()) {
                    return null;
                }
                int numSelected = selected.size();
                final boolean canSharePanoramas =
                        numSelected < MAX_SELECTED_ITEMS_FOR_PANORAMA_SHARE_INTENT;
                final boolean canShare =
                        numSelected < MAX_SELECTED_ITEMS_FOR_SHARE_INTENT;

                final GetAllPanoramaSupports supportCallback = canSharePanoramas ?
                        new GetAllPanoramaSupports(selected, jc)
                        : null;

                // Pass2: Deal with expanded media object list for sharing operation.
                final Intent share_panorama_intent = canSharePanoramas ?
                        computePanoramaSharingIntent(jc, MAX_SELECTED_ITEMS_FOR_PANORAMA_SHARE_INTENT)
                        : new Intent();
                mShareIntent = canShare ?
                        computeSharingIntent(jc, MAX_SELECTED_ITEMS_FOR_SHARE_INTENT)
                        : new Intent();

                if (canSharePanoramas) {
                    supportCallback.waitForPanoramaSupport();
                }
                if (jc.isCancelled()) {
                    return null;
                }

                mSelectedItemCount = 0;
                if(selected != null) {
                    for(MediaObject set : selected) {
                        if(set instanceof MediaSet) {
                            mSelectedItemCount += ((MediaSet) set).getMediaItemCount();
                        } else {
                            mSelectedItemCount++;
                        }
                    }

                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mMenuTask = null;
                            if (jc.isCancelled()) return;
                            MenuExecutor.updateMenuOperation(mMenu, operation);
                            if (mShareMenuItem != null) {
                                boolean selected = mSelectedItemCount > 0;
                                mShareMenuItem.setEnabled(selected);
                                MenuExecutor.updateMenuShare(mMenu, selected);

                                updateStoryMenu();
                            }
                        }
                    });
                }
                return null;
            }
        });
    }
    public void pause() {
        if (mMenuTask != null) {
            mMenuTask.cancel();
            mMenuTask = null;
        }
        if (mActionMode == null) {
            return;
        }
        mMenuExecutor.pause();
    }

    public void destroy() {
        mMenuExecutor.destroy();
    }

    public void resume() {
        if (mActionMode == null) return;
        if (mSelectionManager.inSelectionMode()) updateSupportedOperation();
        mMenuExecutor.resume();
    }
    public void updateSelectionMenu() {
        if (mActionMode == null) return;
        // update title
        int count = mSelectionManager.getSelectedCount();
        String title;
        ActivityState topState = null;
        // add empty state check to avoid JE
        if (mActivity.getStateManager().getStateCount() != 0) {
            topState = mActivity.getStateManager().getTopState();
        }

        //*/ Modified by Linguanrong for story album, 2015-6-27
        if (topState != null && topState instanceof AlbumSetPage) {
            title = ((AlbumSetPage) topState).getSelectedString();
        } else if (topState != null && topState instanceof AlbumStorySetPage) {
            title = ((AlbumStorySetPage) topState).getSelectedString();
        } else if (topState != null && topState instanceof AlbumStoryCoverPage) {
            title = ((AlbumStoryCoverPage) topState).getTitle();
        } else {
            String format = mActivity.getResources().getQuantityString(
                    R.plurals.number_of_items_selected, count);
            title = String.format(format, count);
        }
        //*/
        setTitle(title);
        ((GalleryActivity)mActivity).getController().updateActionEnabled(count > 0, ACTION_CODE_DELETE);
        ((GalleryActivity)mActivity).getController().updateActionEnabled(count > 0, ACTION_CODE_SHARE);

        //*/ Added by Tyd Linguanrong for Gallery new style, 2014-2-13
        if (mSelectMenuItem != null) {
            if (mSelectionManager.inSelectAllMode()) {
//            mCheckBox.setChecked(true);
                mSelectMenuItem.setTitle(R.string.deselect_all);
            } else {
//            mCheckBox.setChecked(false);
                mSelectMenuItem.setTitle(R.string.select_all);
            }
        }
        //*/

        //*/ Added by Linguanrong for story album, 2015-5-20
        if (topState != null && topState instanceof AlbumStorySetPage) {
            MenuExecutor.updateMenuRename(mMenu, count == 1);
        }

        if (topState != null && topState instanceof AlbumStoryPage) {
            MenuExecutor.updateMenuSetCover(mMenu, count == 1);
            MenuExecutor.updateMenuCrop(mMenu, false);
        }
        //*/
    }
    public void setActionModeListener(ActionModeListener listener) {
        mListener = listener;
    }
    //*/
}

package com.freeme.extern;

import android.app.ActionBar;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.freeme.gallery.BuildConfig;
import com.freeme.gallery.R;
import com.android.gallery3d.filtershow.crop.CropExtras;
import com.freeme.utils.FreemeUtils;
import com.freeme.utils.LogUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;

public class SaveWallpaper {
    private static final String  TAG      = "SaveWallPaper";
    private static final boolean _PROFILE = false;
    private final int mDisplayHeight;
    private final int mDisplayWidth;
    private final int mFullWallHeight;
    private final int mFullWallWidth;
    ImageButton mPortraitModeImageButton;
    ImageButton mLandscapModeImageButton;
    private Activity              mActivity;
    private boolean               mIsSaveWallPaper;
    private int                   mSaveType;
    private SaveWallPaperListener mSaveWallPaperListener;
    private ColorStateList mNormalTextColor;
    private ColorStateList mHighlightTextColor;
    private TextView       mLockTextBtn;
    private TextView       mHomeTextBtn;
    private TextView       mAllTextBtn;
    private ImageView      mLockUnderLine;
    private ImageView      mHomeUnderLine;
    private ImageView      mAllUnderLine;
    private View           mHomeWallpaperMode;
    private CropExtras mCropExtras;
    private float      mSpotlightX;
    private float      mSpotlightY;

    public SaveWallpaper(Activity activity, SaveWallPaperListener l, int type, CropExtras cropExtras) {
        mIsSaveWallPaper = false;

        mActivity = activity;
        mSaveWallPaperListener = l;

        final DisplayMetrics dm = getLockScreenDisplayMetrics(mActivity);
        mDisplayWidth = dm.widthPixels;
        mDisplayHeight = dm.heightPixels;
        mFullWallWidth = mDisplayWidth * CropExtras.WALLPAPER_SCREEN_SPAN;
        mFullWallHeight = mDisplayHeight;

        mCropExtras = cropExtras;

        ActionBar actionBar = activity.getActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.wallpaper_title_bar);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setHomeButtonEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);

        // initialize views
        initView();

        setSaveType(type);

        // initial states
        LinearLayout tab_lock = (LinearLayout) activity.findViewById(R.id.tab_lockscreen);
        LinearLayout tab_home = (LinearLayout) activity.findViewById(R.id.tab_wallpaper);
        LinearLayout tab_all = (LinearLayout) activity.findViewById(R.id.tab_all);

        if (!FreemeUtils.isFreemeOS(activity) && !BuildConfig.SUPPORT_LOCKSCREEN_WALLPAPER) {
            tab_lock.setVisibility(View.GONE);
            tab_all.setVisibility(View.GONE);
            mHomeUnderLine.setVisibility(View.INVISIBLE);
        }

        View.OnClickListener onclicklistener = new View.OnClickListener() {
            public void onClick(View v) {
                int type = 0;
                switch (v.getId()) {
                    case R.id.tab_lockscreen: {
                        type = SaveTypeGener.getLockscreenType();
                        break;
                    }
                    case R.id.tab_wallpaper: {
                        type = SaveTypeGener.getHomescreenType(true);
                        break;
                    }
                    case R.id.tab_all: {
                        type = SaveTypeGener.getAllscreenType(true);
                        break;
                    }
                }
                setSaveType(type);
                mSaveWallPaperListener.onTabClicked();
            }
        };
        tab_lock.setOnClickListener(onclicklistener);
        tab_home.setOnClickListener(onclicklistener);
        tab_all.setOnClickListener(onclicklistener);
    }

    private static DisplayMetrics getLockScreenDisplayMetrics(Context context) {
        DisplayMetrics dm = Resources.getSystem().getDisplayMetrics();
        if (dm.widthPixels == 0 || dm.heightPixels == 0) {
            dm = new DisplayMetrics();
            try {
                ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(dm);
            } catch (Exception e) {
                // ignore
            }
        }
        return dm;
    }

    private void initView() {
        final Resources r = mActivity.getResources();
        mNormalTextColor = r.getColorStateList(R.color.drag_text_normal);
        mHighlightTextColor = r.getColorStateList(R.color.drag_text_highlight);

        mLockTextBtn = (TextView) mActivity.findViewById(R.id.tab_lockscreen_text);
        mHomeTextBtn = (TextView) mActivity.findViewById(R.id.tab_wallpaper_text);
        mAllTextBtn = (TextView) mActivity.findViewById(R.id.tab_all_text);
        mLockUnderLine = (ImageView) mActivity.findViewById(R.id.tab_lockscreen_bottom_line);
        mHomeUnderLine = (ImageView) mActivity.findViewById(R.id.tab_wallpaper_bottom_line);
        mAllUnderLine = (ImageView) mActivity.findViewById(R.id.tab_all_bottom_line);

        mHomeWallpaperMode = mActivity.findViewById(R.id.control);
        mPortraitModeImageButton = (ImageButton) mActivity.findViewById(R.id.portrait_mode);
        mLandscapModeImageButton = (ImageButton) mActivity.findViewById(R.id.landscap_mode);
        View.OnClickListener changeBetPortAndLand = new View.OnClickListener() {
            public void onClick(View v) {
                boolean isPortrait = false;
                switch (v.getId()) {
                    case R.id.portrait_mode: {
                        isPortrait = true;
                        break;
                    }
                    case R.id.landscap_mode: {
                        isPortrait = false;
                        break;
                    }
                }

                // fresh ui
                if (SaveTypeGener.isPortraitMode(mSaveType) != isPortrait) {
                    int newType = SaveTypeGener.genThisType(
                            SaveTypeGener.getMajorType(mSaveType), isPortrait);
                    setSaveType(newType);
                    mSaveWallPaperListener.onTabClicked();
                }
            }
        };
        mPortraitModeImageButton.setOnClickListener(changeBetPortAndLand);
        mLandscapModeImageButton.setOnClickListener(changeBetPortAndLand);
    }

    private void setSaveType(final int type) {
        mSaveType = type;

        if (mCropExtras == null) throw new RuntimeException("mCropExtras is null !");

        int width = 0;
        int height = 0;
        int desiredWidth = mFullWallWidth;
        int desiredHeight = mFullWallHeight;

        float spotX = 0;
        float spotY = 0;

        switch (SaveTypeGener.getMajorType(type)) {
            case SaveTypeGener.SAVE_TYPE_LOCKSCREEN: {
                mLockTextBtn.setTextColor(mHighlightTextColor);
                mHomeTextBtn.setTextColor(mNormalTextColor);
                mAllTextBtn.setTextColor(mNormalTextColor);
                mLockUnderLine.setImageResource(R.drawable.wallpaper_drag_bottom_line_normal);
                mHomeUnderLine.setImageResource(R.drawable.wallpaper_drag_bottom_line_normal);
                mAllUnderLine.setImageResource(R.drawable.wallpaper_drag_bottom_line_normal);

                // +++
                mHomeWallpaperMode.setVisibility(View.GONE);
                // ---

                width = mDisplayWidth;
                height = mDisplayHeight;

                spotX = 0;
                spotY = 0;
                break;
            }
            case SaveTypeGener.SAVE_TYPE_WALLPAPER: {
                mLockTextBtn.setTextColor(mNormalTextColor);
                mHomeTextBtn.setTextColor(mHighlightTextColor);
                mAllTextBtn.setTextColor(mNormalTextColor);
                mLockUnderLine.setImageResource(R.drawable.wallpaper_drag_bottom_line_normal);
                mHomeUnderLine.setImageResource(R.drawable.wallpaper_drag_bottom_line_normal);
                mAllUnderLine.setImageResource(R.drawable.wallpaper_drag_bottom_line_normal);

                // +++
                mHomeWallpaperMode.setVisibility(View.VISIBLE);
                final boolean isPortrait = SaveTypeGener.isPortraitMode(type);
                mPortraitModeImageButton.setBackgroundResource(
                        isPortrait ? R.drawable.khob_button_bg : R.drawable.khob_transparent_bg);
                mLandscapModeImageButton.setBackgroundResource(
                        isPortrait ? R.drawable.khob_transparent_bg : R.drawable.khob_button_bg);

                desiredWidth = isPortrait ? mDisplayWidth : mFullWallWidth;
                desiredHeight = isPortrait ? mDisplayHeight : mFullWallHeight;
                // ---

                width = desiredWidth;
                height = desiredHeight;

                spotX = 0;
                spotY = 0;
                break;
            }
            case SaveTypeGener.SAVE_TYPE_ALL: {
                mLockTextBtn.setTextColor(mNormalTextColor);
                mHomeTextBtn.setTextColor(mNormalTextColor);
                mAllTextBtn.setTextColor(mHighlightTextColor);
                mLockUnderLine.setImageResource(R.drawable.wallpaper_drag_bottom_line_normal);
                mHomeUnderLine.setImageResource(R.drawable.wallpaper_drag_bottom_line_normal);
                mAllUnderLine.setImageResource(R.drawable.wallpaper_drag_bottom_line_normal);

                // +++
                mHomeWallpaperMode.setVisibility(View.VISIBLE);
                final boolean isPortrait = SaveTypeGener.isPortraitMode(type);
                mPortraitModeImageButton.setBackgroundResource(
                        isPortrait ? R.drawable.khob_button_bg : R.drawable.khob_transparent_bg);
                mLandscapModeImageButton.setBackgroundResource(
                        isPortrait ? R.drawable.khob_transparent_bg : R.drawable.khob_button_bg);

                desiredWidth = isPortrait ? mDisplayWidth : mFullWallWidth;
                desiredHeight = isPortrait ? mDisplayHeight : mFullWallHeight;
                // ---

                width = desiredWidth;
                height = desiredHeight;
                mSpotlightX = (float) mDisplayWidth / desiredWidth;
                mSpotlightY = (float) mDisplayHeight / desiredHeight;

                spotX = mSpotlightX;
                spotY = mSpotlightY;
                break;
            }
        }

        mCropExtras.applyWallpaperParameters(width, height, true, true,
                width, height, spotX, spotY);
    }

    public int getSavingMessageId() {
        int resid = R.string.set_lock_or_wallpaper;//R.string.saving_image;
        switch (SaveTypeGener.getMajorType(mSaveType)) {
            case SaveTypeGener.SAVE_TYPE_LOCKSCREEN: {
                resid = R.string.set_lockscreen;
                break;
            }
            case SaveTypeGener.SAVE_TYPE_WALLPAPER: {
                resid = R.string.wallpaper;
                break;
            }
            case SaveTypeGener.SAVE_TYPE_ALL: {
                resid = R.string.set_lock_or_wallpaper;
                break;
            }
        }
        return resid;
    }

    public int getFailMessageId() {
        int id = -1;
        switch (SaveTypeGener.getMajorType(mSaveType)) {
            case SaveTypeGener.SAVE_TYPE_LOCKSCREEN: {
                id = R.string.set_lockscreen_failed;
                break;
            }
        }
        return id;
    }

    public boolean setAsLockOrMainWallpaper(Bitmap cropped, byte[] croppedBytes,
                                            CompressFormat cf, int quality) {
        boolean result = true;
        boolean newerThanM = true;
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N) {
            newerThanM = false;
        }
        switch (SaveTypeGener.getMajorType(mSaveType)) {
            case SaveTypeGener.SAVE_TYPE_LOCKSCREEN: {
                if (newerThanM) {
                    result = setAsLockScreenSinceN(croppedBytes);
                } else {
                    result = setAsLockScreen(croppedBytes);
                }
                break;
            }
            case SaveTypeGener.SAVE_TYPE_WALLPAPER: {
                if (newerThanM) {
                    result = setAsWallpaperSinceN(croppedBytes);
                } else {
                    result = setAsWallpaper(croppedBytes);
                }
                break;
            }
            case SaveTypeGener.SAVE_TYPE_ALL: {
                if (newerThanM) {
                    result = setAsAllPaperSinceN(croppedBytes);
                } else {
                    result = setAsAllPaper(cropped, croppedBytes, cf, quality);
                }
                break;
            }
        }
        return result;
    }

    private boolean setAsLockScreen(byte[] croppedBytes) {
        try {
            WallpaperManager wm = WallpaperManager.getInstance(mActivity);
            Class<?> demo = Class.forName("android.app.WallpaperManager");
            Method method = demo.getMethod("setLockscreenStream", InputStream.class);
            method.invoke(wm, new ByteArrayInputStream(croppedBytes));
//            WallpaperManager.getInstance(mActivity).setLockscreenStream(
//                    new ByteArrayInputStream(croppedBytes));
            return true;
        } catch (Exception e) {
//            ToastUtil.showToast(mActivity, R.string.set_lockscreen_failed);
            LogUtil.i(TAG, "fail to set lockscreen wall paper, inner :" + e);
            return false;
        }
    }

    private boolean setAsWallpaper(byte[] croppedBytes) {
        WallpaperManager ws = WallpaperManager.getInstance(mActivity);

        boolean isSpan = true;
        // +++
        isSpan = !SaveTypeGener.isPortraitMode(mSaveType);
        // ---
        setWallpaperDimensions(ws, isSpan);
        try {
            ws.setStream(new ByteArrayInputStream(croppedBytes));
            return true;
        } catch (Exception e) {
            LogUtil.i(TAG, "fail to set wall paper, inner :" + e);
            return false;
        }
    }

    private boolean setAsAllPaper(Bitmap wallpaper, byte[] croppedBytes,
                                  CompressFormat cf, int quality) {
        if (setAsWallpaper(croppedBytes)) {
            byte[] lockCroppedBytes = null;

            if (mSpotlightX == 1 && mSpotlightY == 1) {
                lockCroppedBytes = croppedBytes;
            } else if (mSpotlightX != 0 && mSpotlightY != 0) {
                final int width = wallpaper.getWidth() >>> 1;
                final int height = wallpaper.getHeight();

                Bitmap lock = Bitmap.createBitmap(wallpaper, width >>> 1, 0, width, height);
                if (lock != null) {
                    ByteArrayOutputStream tmpOut = new ByteArrayOutputStream(2048);
                    if (lock.compress(cf, quality, tmpOut)) {
                        lockCroppedBytes = tmpOut.toByteArray();
                    }
                    lock.recycle();
                    lock = null;
                }
            }

            if (lockCroppedBytes != null) {
                return setAsLockScreen(lockCroppedBytes);
            }
        }
        return false;
    }

    private boolean setAsLockScreenSinceN(byte[] croppedBytes) {
        WallpaperManager wm = WallpaperManager.getInstance(mActivity);
        try {
            wm.setStream(new ByteArrayInputStream(croppedBytes),
                    null, true, WallpaperManager.FLAG_LOCK);
            return true;
        } catch (Exception e) {
            LogUtil.i(TAG, "fail to set lockscreen wall paper, inner :" + e);
            return false;
        }
    }

    private boolean setAsWallpaperSinceN(byte[] croppedBytes) {
        WallpaperManager ws = WallpaperManager.getInstance(mActivity);

        boolean isSpan = true;
        // +++
        isSpan = !SaveTypeGener.isPortraitMode(mSaveType);
        // ---
        setWallpaperDimensions(ws, isSpan);
        try {
            ws.setStream(new ByteArrayInputStream(croppedBytes),
                    null, true, WallpaperManager.FLAG_SYSTEM);
            return true;
        } catch (Exception e) {
            LogUtil.i(TAG, "fail to set wall paper, inner :" + e);
            return false;
        }
    }

    private boolean setAsAllPaperSinceN(byte[] croppedBytes) {
        WallpaperManager ws = WallpaperManager.getInstance(mActivity);

        try {
            ws.setStream(new ByteArrayInputStream(croppedBytes),
                    null, true, WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK);
            return true;
        } catch (Exception e) {
            LogUtil.i(TAG, "fail to set wall paper, inner :" + e);
            return false;
        }
    }


    private final void setWallpaperDimensions(WallpaperManager wm, boolean span) {
        int width = span ? mFullWallWidth : mDisplayWidth;
        int height = mDisplayHeight;
        wm.suggestDesiredDimensions(width, height);
    }

    public interface SaveWallPaperListener {
        void onTabClicked();

        Bitmap onGetCropBitmap(Rect rect, int outputX, int outputY);
    }

    public static final class SaveTypeGener {
        private static final int SAVE_TYPE_LOCKSCREEN = 0x0010;
        private static final int SAVE_TYPE_WALLPAPER  = 0x0020;
        private static final int SAVE_TYPE_ALL        = 0x0030;

        private static final int SAVE_TYPE_MINOR_LAND = 0;
        private static final int SAVE_TYPE_MINOR_PORT = 1;

        public static int getLockscreenType() {
            return SAVE_TYPE_LOCKSCREEN;
        }

        public static int getHomescreenType(boolean portrait) {
            return SAVE_TYPE_WALLPAPER |
                    (portrait ? SAVE_TYPE_MINOR_PORT : SAVE_TYPE_MINOR_LAND);
        }

        public static int getAllscreenType(boolean portrait) {
            return SAVE_TYPE_LOCKSCREEN |
                    SAVE_TYPE_WALLPAPER |
                    (portrait ? SAVE_TYPE_MINOR_PORT : SAVE_TYPE_MINOR_LAND);
        }

        public static int genThisType(int major, boolean portrait) {
            return major |
                    (portrait ? SAVE_TYPE_MINOR_PORT : SAVE_TYPE_MINOR_LAND);
        }

        public static boolean isPortraitMode(int type) {
            return getMinorType(type) == SAVE_TYPE_MINOR_PORT;
        }

        public static int getMinorType(int type) {
            return type & SAVE_TYPE_MINOR_PORT;
        }

        public static int getMajorType(int type) {
            return type & ~SAVE_TYPE_MINOR_PORT;
        }
    }
}

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

package com.freeme.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.freeme.gallery.BuildConfig;
import com.freeme.gallery.R;
import com.freeme.gallery.app.AbstractGalleryActivity;
import com.freeme.gallery.app.MovieActivity;
import com.freeme.provider.GalleryStore;

import java.io.File;

public final class FreemeUtils {

    public static final int CAMERA_CLUSTER_ID = 0;
    public static final int STORY_CLUSTER_ID = 1;
    public static final int ALBUM_CLUSTER_ID = 2;
    public static final int COMMUNITY_CLUSTER_ID = 3;
    public static final int CLUSTER_BY_CAMERE = 64;
    public static final int CLUSTER_BY_STORY = 65;
    public static final int CLUSTER_BY_ALBUM = 66;
    public static final int CLUSTER_BY_COMMUNITY = 67;
    public static final String KEY_FROM_COMMUNITY = "from-community";
    //*/ Added by Linguanrong for story album, 2015-5-22
    public static final boolean STORY_DEBUG = true;
    public static final String STORY_SHAREPREFERENCE_KEY = "StoryAlbumSet";
    public static final String FACE_SHAREPREFERENCE_KEY = "FaceAlbumSet";
    public static final String BABY_BIRTHDAY = "BabyBirthday";
    public static final String LOVE_DATE = "LoveDate";
    public static final String BABY_DESCRIPTION = "BabyDescription";
    public static final String LOVE_DESCRIPTION = "LoveDescription";
    public static final String DATE_SPLIT = "-";
    //*/ Added by tyd Linguanrong for udpateself, 2015-12-16
    public final static int UPDATE_SELF_DURATION = 1; // 1 day
    //*/
    //*/ Added by Linguanrong for secret photos, 2014-9-19
    public static final String VISTOR_MODE_STATE = "tydtech_vistor_mode_state";
    //*/
    public static final String INNER_VISTOR_MODE = "inner_vistor_mode";
    private static final String TAG = "FreemeUtils";
    private static int settingsScreenlight;
    private static float screenBrightness;

    public static boolean isVisitorModeInner(ContentResolver resolver) {
        return Settings.System.getInt(resolver, INNER_VISTOR_MODE, 0) != 0;
    }

    public static boolean isVisitorMode(ContentResolver resolver) {
        return Settings.System.getInt(resolver, VISTOR_MODE_STATE, 0) != 0
                || Settings.System.getInt(resolver, INNER_VISTOR_MODE, 0) != 0;
    }
    //*/

    @SuppressLint("NewApi")
    public static int getRealHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(dm);
        return dm.heightPixels;
    }

    public static void playVideo(Activity activity, Uri uri, String title) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(convertUri(uri), "video/*")
                    .putExtra(Intent.EXTRA_TITLE, title)
                    .putExtra(MovieActivity.KEY_TREAT_UP_AS_BACK, true);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (BuildConfig.SUPPORT_INNER_VIDEO) {
                intent.setPackage("com.freeme.onlinevideo");
            }
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, activity.getString(R.string.video_err),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public static Uri convertUri(Uri uri) {
        if (uri == null) {
            return null;
        }
        return Uri.parse(uri.toString().replace(GalleryStore.AUTHORITY, MediaStore.AUTHORITY));
    }

    public static boolean isFreemeOS(Context context) {
        return "FreemeOS".equals(SystemPropertiesProxy.get(context, "ro.build.freemeos_label"));
    }

    public static boolean isInternational(Context context) {
        String country = context.getResources().getConfiguration().locale.getCountry();
        return !"CN".equals(country) && !"TW".equals(country);
    }

    public static Uri tryContentMediaUri(Context context, Uri uri, String contentType) {
        if (null == uri) {
            return null;
        }

        String scheme = uri.getScheme();
        if (!ContentResolver.SCHEME_FILE.equals(scheme)) {
            return uri;
        } else {
            String path = uri.getPath();
            LogUtil.i(TAG, "<tryContentMediaUri> for " + path);
            if (!new File(path).exists()) {
                return null;
            }
        }

        Cursor cursor = null;
        Uri queryUri = null;
        try {
            if (contentType.startsWith("image")) {
                queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            } else if (contentType.startsWith("video")) {
                queryUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            }
            // for file kinds of uri, query media database
            cursor = context.getContentResolver().query(queryUri,
                    new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.BUCKET_ID},
                    "_data=(?)", new String[]{uri.getPath()},
                    null);
            if (null != cursor && cursor.moveToNext()) {
                long id = cursor.getLong(0);
                uri = Uri.parse(queryUri + "/" + id);
                LogUtil.i(TAG, "<tryContentMediaUri> got " + uri);
            } else {
                LogUtil.i(TAG, "<tryContentMediaUri> fail to convert " + uri);
            }
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }
        return convertGalleryUri(uri);
    }

    public static Uri convertGalleryUri(Uri uri) {
        if (uri == null) {
            return null;
        }
        return uri;
    }

    public static void setScreenBrightness(AbstractGalleryActivity mActivity, Window window) {
        WindowManager.LayoutParams winParams = window.getAttributes();
        screenBrightness = FrameworkSupportUtils.getScreenBritness();
        if (screenBrightness > 0) {
            ContentResolver resolver = mActivity.getContentResolver();
            settingsScreenlight = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 0);
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, (int) (255 * screenBrightness));
        }
    }

    public static void restoreScreenBrightness(AbstractGalleryActivity mActivity) {
        ContentResolver resolver = mActivity.getContentResolver();
        if (screenBrightness > 0) {
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, settingsScreenlight);
        }
    }
}


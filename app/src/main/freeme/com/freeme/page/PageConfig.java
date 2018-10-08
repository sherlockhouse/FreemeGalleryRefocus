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

import android.content.Context;
import android.content.res.Resources;

import com.freeme.gallery.R;
import com.android.gallery3d.ui.AlbumSetSlotRenderer;
import com.android.gallery3d.ui.SlotView;
import com.freeme.ui.DateSlotView;
import com.freeme.ui.FaceAlbumSetSlotRender;
import com.freeme.ui.FaceSlotView;
import com.freeme.ui.StoryAlbumSetSlotRender;
import com.freeme.ui.StorySlotView;

public final class PageConfig {
    public static class AlbumCameraPage {
        private static AlbumCameraPage sInstance;

        public SlotView.Spec slotViewSpec;
        public int           placeholderColor;

        private AlbumCameraPage(Context context) {
            Resources r = context.getResources();

            placeholderColor = r.getColor(R.color.album_placeholder);

            slotViewSpec = new SlotView.Spec();
            slotViewSpec.rowsLand = r.getInteger(R.integer.album_rows_land);
            slotViewSpec.rowsPort = r.getInteger(R.integer.album_rows_port);
            slotViewSpec.slotGap = r.getDimensionPixelSize(R.dimen.album_slot_gap);
            slotViewSpec.slotPadding = r.getDimensionPixelSize(R.dimen.album_slot_padding);
            slotViewSpec.bottomPadding = r.getDimensionPixelSize(R.dimen.tab_bar_default_height);
        }

        public static synchronized AlbumCameraPage get(Context context) {
            if (sInstance == null) {
                sInstance = new AlbumCameraPage(context);
            }
            return sInstance;
        }
    }

    public static class AlbumTimeShaftPage {
        private static AlbumTimeShaftPage sInstance;

        public DateSlotView.Spec      slotViewSpec;
        public DateSlotView.LabelSpec labelSpec;
        public int                    placeholderColor;

        private AlbumTimeShaftPage(Context context) {
            Resources r = context.getResources();

            placeholderColor = r.getColor(R.color.album_placeholder);

            slotViewSpec = new DateSlotView.Spec();
            slotViewSpec.rowsLand = r.getInteger(R.integer.album_timeshaft_rows_land);
            slotViewSpec.rowsPort = r.getInteger(R.integer.album_timeshaft_rows_port);
            slotViewSpec.slotGap = r.getDimensionPixelSize(R.dimen.album_slot_gap);
            slotViewSpec.slotPadding = r.getDimensionPixelSize(R.dimen.album_timeshaft_slot_padding);
            slotViewSpec.bottomPadding = r.getDimensionPixelSize(R.dimen.tab_bar_default_height);
            slotViewSpec.topPadding = r.getDimensionPixelSize(R.dimen.timeshaft_remark_height);

            labelSpec = new DateSlotView.LabelSpec();
            labelSpec.labelBackgroundHeight = r.getDimensionPixelSize(
                    R.dimen.album_date_label_background_height);
            labelSpec.titleOffset = r.getDimensionPixelSize(
                    R.dimen.album_date_title_offset);
            labelSpec.countOffset = r.getDimensionPixelSize(
                    R.dimen.album_date_count_offset);
            labelSpec.titleFontSize = r.getDimensionPixelSize(
                    R.dimen.album_date_title_font_size);
            labelSpec.countFontSize = r.getDimensionPixelSize(
                    R.dimen.album_date_count_font_size);
            labelSpec.leftMargin = r.getDimensionPixelSize(
                    R.dimen.album_date_left_margin);
            labelSpec.titleRightMargin = r.getDimensionPixelSize(
                    R.dimen.album_date_title_right_margin);
            labelSpec.iconSize = r.getDimensionPixelSize(
                    R.dimen.album_date_icon_size);
            labelSpec.backgroundColor = r.getColor(
                    R.color.album_date_label_background);
            labelSpec.titleColor = r.getColor(R.color.album_date_label_title);
            labelSpec.countColor = r.getColor(R.color.album_date_label_count);

            /*/ Disabled by Tyd Linguanrong for secret photos, 2014-2-24
            slotViewSpec.commentSpec = new CommentView.Spec();
            slotViewSpec.commentSpec.fontSize = r.getDimensionPixelSize(R.dimen.album_date_title_font_size);
            slotViewSpec.commentSpec.fontColor = r.getColor(R.color.album_date_label_title);
            slotViewSpec.commentSpec.contentColor = r.getColor(R.color.album_date_label_title);
            slotViewSpec.commentSpec.disableColor = r.getColor(R.color.album_date_label_title);
            slotViewSpec.commentSpec.commentTopMargin = r.getDimensionPixelSize(R.dimen.album_date_left_margin);
            slotViewSpec.commentSpec.commentLeftMargin = r.getDimensionPixelSize(R.dimen.album_date_left_margin);
            //*/
        }

        public static synchronized AlbumTimeShaftPage get(Context context) {
            if (sInstance == null) {
                sInstance = new AlbumTimeShaftPage(context);
            }
            return sInstance;
        }
    }

    public static class AlbumStorySetPage {
        private static AlbumStorySetPage sInstance;

        public StorySlotView.Spec                  slotViewSpec;
        public StoryAlbumSetSlotRender.LabelSpec labelSpec;
        public int                            paddingTop;
        public int                            paddingBottom;
        public int                            paddingLeftRight;
        public int                            placeholderColor;

        private AlbumStorySetPage(Context context) {
            Resources r = context.getResources();

            placeholderColor = r.getColor(R.color.transparent);

            slotViewSpec = new StorySlotView.Spec();
            slotViewSpec.rowsLand = r.getInteger(R.integer.albumstoryset_rows_land);
            slotViewSpec.rowsPort = r.getInteger(R.integer.albumstory_rows_port);
            slotViewSpec.slotGap = r.getDimensionPixelSize(R.dimen.albumstory_slot_gap) ;
            slotViewSpec.slotGapV = r.getDimensionPixelSize(R.dimen.albumstory_slot_gap_vertical);
            slotViewSpec.slotHeightAdditional = 0;
            slotViewSpec.slotPadding = r.getDimensionPixelSize(R.dimen.album_slot_padding);
            slotViewSpec.bottomPadding = r.getDimensionPixelSize(R.dimen.storyset_bottompadding);

            paddingTop = r.getDimensionPixelSize(R.dimen.albumstory_padding_top);
            paddingBottom = r.getDimensionPixelSize(R.dimen.albumstory_padding_bottom);
            paddingLeftRight = r.getDimensionPixelSize(R.dimen.albumstory_padding_left_right);

            labelSpec = new StoryAlbumSetSlotRender.LabelSpec();
            labelSpec.labelBackgroundHeight = r.getDimensionPixelSize(R.dimen.albumstory_label_background_height);
            labelSpec.titleOffset = r.getDimensionPixelSize(R.dimen.albumstory_title_offset);
            labelSpec.countOffset = r.getDimensionPixelSize(R.dimen.albumstory_count_offset);
            labelSpec.titleFontSize = r.getDimensionPixelSize(R.dimen.albumstory_title_font_size);
            labelSpec.countFontSize = r.getDimensionPixelSize(R.dimen.albumstory_count_font_size);
            labelSpec.leftMargin = r.getDimensionPixelSize(R.dimen.albumstory_left_margin);
            labelSpec.titleRightMargin = r.getDimensionPixelSize(R.dimen.albumstory_title_right_margin);
            labelSpec.iconSize = r.getDimensionPixelSize(R.dimen.albumstory_icon_size);

            labelSpec.backgroundColor = r.getColor(R.color.albumset_label_background);
            labelSpec.titleColor = r.getColor(R.color.albumstory_label_title);
            labelSpec.countColor = r.getColor(R.color.albumstory_label_count);

            labelSpec.isStoryAlbum = true;
        }

        public static synchronized AlbumStorySetPage get(Context context) {
            if (sInstance == null) {
                sInstance = new AlbumStorySetPage(context);
            }
            return sInstance;
        }
    }

    public static class AlbumStoryPage {
        private static AlbumStoryPage sInstance;

        public DateSlotView.Spec      slotViewSpec;
        public DateSlotView.LabelSpec labelSpec;
        public int                    placeholderColor;

        private AlbumStoryPage(Context context) {
            Resources r = context.getResources();

            placeholderColor = r.getColor(R.color.album_placeholder);

            slotViewSpec = new DateSlotView.Spec();
            slotViewSpec.rowsLand = r.getInteger(R.integer.albumstory_rows_land);
            slotViewSpec.rowsPort = r.getInteger(R.integer.albumstory_rows_port);
            slotViewSpec.slotGap = r.getDimensionPixelSize(R.dimen.album_story_slot_gap);
            slotViewSpec.slotPadding = r.getDimensionPixelSize(R.dimen.album_story_padding);
            slotViewSpec.slotPaddingV = r.getDimensionPixelSize(R.dimen.album_story_padding_vertical);
            slotViewSpec.isStory = true;
            slotViewSpec.leftPadding = r.getDimensionPixelSize(R.dimen.album_story_left_padding);
            slotViewSpec.topPadding = r.getDimensionPixelSize(R.dimen.album_story_top_padding);
            slotViewSpec.bottomPadding = r.getDimensionPixelSize(R.dimen.album_story_bottom_padding);
            slotViewSpec.dotLeftPadding = r.getDimensionPixelSize(R.dimen.album_story_dot_left_padding);
            slotViewSpec.descripGap = r.getDimensionPixelSize(R.dimen.story_header_descrip_gap);
            slotViewSpec.dateGap = r.getDimensionPixelSize(R.dimen.story_header_date_gap);

            labelSpec = new DateSlotView.LabelSpec();
            labelSpec.labelBackgroundHeight = r.getDimensionPixelSize(R.dimen.album_date_label_background_height);
            labelSpec.titleOffset = r.getDimensionPixelSize(R.dimen.album_date_title_offset);
            labelSpec.countOffset = r.getDimensionPixelSize(R.dimen.album_date_count_offset);
            labelSpec.titleFontSize = r.getDimensionPixelSize(R.dimen.story_item_text_size);
            labelSpec.countFontSize = r.getDimensionPixelSize(R.dimen.story_item_text_size);
            labelSpec.leftMargin = r.getDimensionPixelSize(R.dimen.album_date_left_margin);
            labelSpec.titleRightMargin = r.getDimensionPixelSize(R.dimen.album_date_title_right_margin);
            labelSpec.iconSize = r.getDimensionPixelSize(R.dimen.album_date_icon_size);
            labelSpec.backgroundColor = r.getColor(R.color.album_date_label_background);
            labelSpec.titleColor = r.getColor(R.color.album_date_label_title);
            labelSpec.countColor = r.getColor(R.color.story_item_text_color);
        }

        public static synchronized AlbumStoryPage get(Context context) {
            if (sInstance == null) {
                sInstance = new AlbumStoryPage(context);
            }
            return sInstance;
        }
    }

    public static class AlbumStoryCoverPage {
        private static AlbumStoryCoverPage sInstance;

        public SlotView.Spec slotViewSpec;
        public int           placeholderColor;

        private AlbumStoryCoverPage(Context context) {
            Resources r = context.getResources();

            placeholderColor = r.getColor(R.color.album_placeholder);

            slotViewSpec = new SlotView.Spec();
            slotViewSpec.rowsLand = r.getInteger(R.integer.story_cover_rows_land);
            slotViewSpec.rowsPort = r.getInteger(R.integer.story_cover_rows_port);
            slotViewSpec.slotGap = r.getDimensionPixelSize(R.dimen.album_slot_gap);
            slotViewSpec.slotPadding = r.getDimensionPixelSize(R.dimen.album_slot_padding);
        }

        public static synchronized AlbumStoryCoverPage get(Context context) {
            if (sInstance == null) {
                sInstance = new AlbumStoryCoverPage(context);
            }
            return sInstance;
        }
    }

    public static class AlbumVisitorPage {
        private static AlbumVisitorPage sInstance;

        public SlotView.Spec slotViewSpec;
        public int           placeholderColor;

        private AlbumVisitorPage(Context context) {
            Resources r = context.getResources();

            placeholderColor = r.getColor(R.color.album_placeholder);

            slotViewSpec = new SlotView.Spec();
            slotViewSpec.rowsLand = r.getInteger(R.integer.album_rows_land);
            slotViewSpec.rowsPort = r.getInteger(R.integer.album_rows_port);
            slotViewSpec.slotGap = r.getDimensionPixelSize(R.dimen.album_slot_gap);
            slotViewSpec.slotPadding = r.getDimensionPixelSize(R.dimen.album_slot_padding);
        }

        public static synchronized AlbumVisitorPage get(Context context) {
            if (sInstance == null) {
                sInstance = new AlbumVisitorPage(context);
            }
            return sInstance;
        }
    }

    public static class AlbumFaceSetPage {
        private static AlbumFaceSetPage sInstance;

        public FaceSlotView.Spec                  slotViewSpec;
        public FaceAlbumSetSlotRender.LabelSpec labelSpec;
        public int                            paddingTop;
        public int                            paddingBottom;
        public int                            paddingLeftRight;
        public int                            placeholderColor;

        private AlbumFaceSetPage(Context context) {
            Resources r = context.getResources();

            placeholderColor = r.getColor(R.color.transparent);

            slotViewSpec = new FaceSlotView.Spec();
            slotViewSpec.rowsLand = r.getInteger(R.integer.albumset_rows_land);
            slotViewSpec.rowsPort = r.getInteger(R.integer.albumset_rows_port);
            slotViewSpec.slotGap = r.getDimensionPixelSize(R.dimen.albumset_slot_gap) ;
            slotViewSpec.slotGapV = r.getDimensionPixelSize(R.dimen.albumset_slot_gap_vertical);
            slotViewSpec.slotHeightAdditional = 0;
            slotViewSpec.slotPadding = r.getDimensionPixelSize(R.dimen.album_slot_padding);
            slotViewSpec.bottomPadding = r.getDimensionPixelSize(R.dimen.tab_bar_default_height);

            paddingTop = r.getDimensionPixelSize(R.dimen.albumset_padding_top);
            paddingBottom = r.getDimensionPixelSize(R.dimen.albumset_padding_bottom);
            paddingLeftRight = r.getDimensionPixelSize(R.dimen.albumstory_padding_left_right);

            labelSpec = new FaceAlbumSetSlotRender.LabelSpec();
            labelSpec.labelBackgroundHeight = r.getDimensionPixelSize(R.dimen.albumset_label_background_height);
            labelSpec.titleOffset = r.getDimensionPixelSize(R.dimen.albumset_title_offset);
            labelSpec.countOffset = r.getDimensionPixelSize(R.dimen.albumset_count_offset);
            labelSpec.titleFontSize = r.getDimensionPixelSize(R.dimen.albumstory_title_font_size);
            labelSpec.countFontSize = r.getDimensionPixelSize(R.dimen.albumstory_count_font_size);
            labelSpec.leftMargin = r.getDimensionPixelSize(R.dimen.albumstory_left_margin);
            labelSpec.titleRightMargin = r.getDimensionPixelSize(R.dimen.albumset_title_right_margin);
            labelSpec.iconSize = r.getDimensionPixelSize(R.dimen.albumstory_icon_size);

            labelSpec.backgroundColor = r.getColor(R.color.albumset_label_background);
            labelSpec.titleColor = r.getColor(R.color.albumstory_label_title);
            labelSpec.countColor = r.getColor(R.color.albumstory_label_count);
        }

        public static synchronized AlbumFaceSetPage get(Context context) {
            if (sInstance == null) {
                sInstance = new AlbumFaceSetPage(context);
            }
            return sInstance;
        }
    }

}


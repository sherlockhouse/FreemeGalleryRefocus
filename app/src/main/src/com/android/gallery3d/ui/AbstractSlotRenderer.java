/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.content.Context;
import android.graphics.Rect;

import com.freeme.gallery.R;
import com.android.gallery3d.glrenderer.FadeOutTexture;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.NinePatchTexture;
import com.android.gallery3d.glrenderer.ResourceTexture;
import com.android.gallery3d.glrenderer.Texture;

public abstract class AbstractSlotRenderer implements SlotView.SlotRenderer {

    private final ResourceTexture mVideoOverlay;
    private final ResourceTexture  mVideoPlayIcon;
    private final ResourceTexture  mPanoramaIcon;
    private final NinePatchTexture mFramePressed;
    private final NinePatchTexture mFrameSelected;
    private final NinePatchTexture mFrameUnSelected;
    //*/ Added by Linguanrong for Gallery new styel, 2014-2-27
    private final NinePatchTexture mFrameOutSide;
    private       FadeOutTexture   mFramePressedUp;
    //*/
    private final ResourceTexture mRefocusTexture;
    private final ResourceTexture mVoiceTexture;


    protected AbstractSlotRenderer(Context context) {
        mVideoOverlay = new ResourceTexture(context, R.drawable.ic_video_thumb_freeme);
        mVideoPlayIcon = new ResourceTexture(context, R.drawable.ic_gallery_play);
        mPanoramaIcon = new ResourceTexture(context, R.drawable.ic_360pano_holo_light);
        mFramePressed = new NinePatchTexture(context, R.drawable.grid_pressed);
        mFrameSelected = new NinePatchTexture(context, R.drawable.freeme_grid_selected);
        mFrameUnSelected = new NinePatchTexture(context, R.drawable.freeme_grid_unselected);
        //*/ Added by Linguanrong for Gallery new styel, 2014-2-27
//        mFrameOutSide = new NinePatchTexture(context, R.drawable.albumset_outside_frame);
        mFrameOutSide = new NinePatchTexture(context, R.drawable.albumset_outside_frame);
        //*/
        mRefocusTexture = new ResourceTexture(context, R.drawable.ic_newui_indicator_refocus);
        mVoiceTexture = new ResourceTexture(context, R.drawable.ic_newui_indicator_voice);

    }

    protected void drawContent(GLCanvas canvas,
                               Texture content, int width, int height, int rotation) {
        canvas.save(GLCanvas.SAVE_FLAG_MATRIX);

        // The content is always rendered in to the largest square that fits
        // inside the slot, aligned to the top of the slot.
        width = height = Math.min(width, height);
        if (rotation != 0) {
            canvas.translate(width / 2, height / 2);
            canvas.rotate(rotation, 0, 0, 1);
            canvas.translate(-width / 2, -height / 2);
        }

        // Fit the content into the box
        float scale = Math.min(
                (float) width / content.getWidth(),
                (float) height / content.getHeight());
        canvas.scale(scale, scale, 1);
        content.draw(canvas, 0, 0);

        canvas.restore();
    }

    protected void drawStoryContent(GLCanvas canvas,
                               Texture content, int width, int height, int rotation) {
        canvas.save(GLCanvas.SAVE_FLAG_MATRIX);
        Log.i("sldfjl drawContent",  " mSlotHeight: " + height
                + " mSlotWidth: " + width);
        // Fit the content into the box

        // The content is always rendered in to the largest square that fits
        // inside the slot, aligned to t he top of the slot.
        if (rotation == 270) {
            canvas.rotate(rotation, 0, 0, 1);
            canvas.translate( - height , 0 );
            float scale = Math.max(
                    (float) width / content.getWidth() ,
                    (float) height / content.getHeight());
            canvas.scale(scale, scale, 1);
        } else if (rotation == 90){
            float scale = Math.max(
                    (float) width / content.getWidth() ,
                    (float) height / content.getHeight());
            canvas.scale(scale, scale, 1);
            canvas.rotate(rotation, 0, 0, 1);
            canvas.translate(0, - content.getWidth());
        } else {
            float scale = Math.max(
                    (float) width / content.getWidth() ,
                    (float) height / content.getHeight());
            canvas.scale(scale, scale, 1);
        }

        content.draw(canvas, 0, 0);
        canvas.restore();
    }

    protected void drawAlbumSetContent(GLCanvas canvas,
                                    Texture content, int width, int height, int rotation) {
        canvas.save(GLCanvas.SAVE_FLAG_MATRIX);
        Log.i("sldfjl drawContent",  " mSlotHeight: " + height
                + " mSlotWidth: " + width);
        // Fit the content into the box

        // The content is always rendered in to the largest square that fits
        // inside the slot, aligned to t he top of the slot.
        if (rotation == 270) {
            canvas.rotate(rotation, 0, 0, 1);
            canvas.translate( - height , 0 );
            float scale = Math.max(
                    (float) width / content.getWidth() ,
                    (float) height / content.getHeight());
            canvas.scale(scale, scale, 1);
        } else if (rotation == 90){
            float scale = Math.max(
                    (float) width / content.getWidth() ,
                    (float) height / content.getHeight());
            canvas.scale(scale, scale, 1);
            canvas.rotate(rotation, 0, 0, 1);
            canvas.translate(0, - content.getWidth());
        } else {
            float scale = Math.max(
                    (float) width / content.getWidth() ,
                    (float) height / content.getHeight());
            canvas.scale(scale, scale, 1);
        }

        content.draw(canvas, 0, 0);
        canvas.restore();
    }

    protected void drawRefocusIndicator(GLCanvas canvas, int width, int height) {
        int iconSize = Math.min(width, height) / 3;
        mRefocusTexture.draw(canvas, (width - iconSize) / 2, (height - iconSize) / 2,
                iconSize, iconSize);
    }

    protected void drawVideoOverlay(GLCanvas canvas, int width, int height) {
        int iconSize = Math.min(width, height) / 3;
        mVideoOverlay.draw(canvas, (width - iconSize) / 2, (height - iconSize) / 2,
                iconSize, iconSize);

        /// M: [FEATURE.MODIFY] do not show play icon@{
        // int s = Math.min(width, height) / 6;
        // mVideoPlayIcon.draw(canvas, (width - s) / 2, (height - s) / 2, s, s);
        /// @}
    }
    protected void drawVoiceIndicator(GLCanvas canvas, int width, int height) {
        int iconSize = Math.min(width, height) / 3;
        mVoiceTexture.draw(canvas, (width - iconSize) / 2, (height - iconSize) / 2,
                iconSize, iconSize);
    }

    protected void drawPanoramaIcon(GLCanvas canvas, int width, int height) {
        int iconSize = Math.min(width, height) / 6;
        mPanoramaIcon.draw(canvas, (width - iconSize) / 2, (height - iconSize) / 2,
                iconSize, iconSize);
    }

    protected boolean isPressedUpFrameFinished() {
        if (mFramePressedUp != null) {
            if (mFramePressedUp.isAnimating()) {
                return false;
            } else {
                mFramePressedUp = null;
            }
        }
        return true;
    }

    protected void drawPressedUpFrame(GLCanvas canvas, int width, int height) {
        if (mFramePressedUp == null) {
            mFramePressedUp = new FadeOutTexture(mFramePressed);
        }
        drawFrame(canvas, mFramePressed.getPaddings(), mFramePressedUp, 0, 0, width, height);
    }

    protected void drawPressedFrame(GLCanvas canvas, int width, int height) {
        drawFrame(canvas, mFramePressed.getPaddings(), mFramePressed, 0, 0, width, height);
    }

    protected void drawSelectedFrame(GLCanvas canvas, int width, int height) {
        drawFrame(canvas, mFrameSelected.getPaddings(), mFrameSelected, 0, 0, width, height);
    }

    protected void drawUnSelectedFrame(GLCanvas canvas, int width, int height) {
        drawFrame(canvas, mFrameUnSelected.getPaddings(), mFrameUnSelected, 0, 0, width, height);
    }

    protected static void drawFrame(GLCanvas canvas, Rect padding, Texture frame,
            int x, int y, int width, int height) {
        frame.draw(canvas, x - padding.left, y - padding.top, width + padding.left + padding.right,
                 height + padding.top + padding.bottom);
    }
	
    //*/ Added by Linguanrong for Gallery new styel, 2014-2-27
    protected void drawOutSideFrame(GLCanvas canvas, int x, int y, int width, int height) {
        drawFrame(canvas, mFrameOutSide.getPaddings(), mFrameOutSide, x, y, width, height);
    }
    //*/
}

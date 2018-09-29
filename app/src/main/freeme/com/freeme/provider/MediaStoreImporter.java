package com.freeme.provider;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.util.Log;

import com.freeme.gallery.GalleryClassifierService;
import com.freeme.utils.LogUtil;

import java.util.ArrayList;
import java.util.List;

public class MediaStoreImporter {
    private static final String TAG = "MediaStoreImporter";
    private static GalleryFilesDao galleryFilesDao = null;
    private Context mContext;

    public void setmResolver(ContentResolver mResolver) {
        this.mResolver = mResolver;
    }

    private ContentResolver mResolver;

    public static MediaStoreImporter getInstance() {
        return Singleton.instance;
    }

    public void doImport(Context context) {
        mContext = context;
        mResolver = context.getContentResolver();
        galleryFilesDao = GalleryDBManager.getInstance().getGalleryFilesDao();

        final HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        Handler mHandler = new Handler(handlerThread.getLooper());
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                importData();
                handlerThread.quit();
            }
        });
    }

    private void importData() {
        LogUtil.i("import Data");
        List<GalleryFiles> addFilesList = new ArrayList<>();
        addFilesList.clear();
        List<GalleryFiles> updateFilesList = new ArrayList<>();
        updateFilesList.clear();

        List<Long> containIds = getContainIds();

        String where = "media_type =" + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                + " OR " + "media_type =" + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

        Cursor cursor = mResolver.query(MediaStore.Files.getContentUri("external"),
                GalleryStore.PROJECTION, where, null, null);
        LogUtil.i("import Data cursor count" , "" + cursor.getCount());
        //todo store cursor count to compare story count aialbum; cursor count - story count
        if (cursor != null && cursor.moveToFirst()) {
            for (int i = 0; i < cursor.getCount(); i++) {
                long _id = cursor.getLong(0);
                GalleryFiles galleryFiles = getGalleryFiles(_id);
                setGalleryFile(galleryFiles, cursor, _id);
                if(galleryFiles.getMime_type() != null) {
                    if (containIds.contains(_id)) {
                        updateFilesList.add(galleryFiles);
                    } else {
                        addFilesList.add(galleryFiles);
                    }
                }else{
                    galleryFilesDao.delete(galleryFiles);
                }
                cursor.moveToNext();
            }
            cursor.close();
        }

        try {
            galleryFilesDao.insertInTx(addFilesList);
        } catch (SQLiteConstraintException e) {
            e.printStackTrace();
            LogUtil.i("SQLiteConstraintException : " + e);
        }
        galleryFilesDao.updateInTx(updateFilesList);


    }

    private List<Long> getContainIds() {
        List<Long> containIds = new ArrayList<>();
        Cursor cursor = mResolver.query(
                GalleryStore.Files.getContentUri("external"),
                new String[]{BaseColumns._ID}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            for (int index = 0; index < cursor.getCount(); index++) {
                containIds.add(cursor.getLong(0));
                cursor.moveToNext();
            }
            cursor.close();
        }

        return containIds;
    }

    private GalleryFiles getGalleryFiles(long id) {
        return GalleryFiles.getGalleryFiles(galleryFilesDao, id);
    }

    private void setGalleryFile(GalleryFiles galleryFiles, Cursor cursor, long _id) {
        String tmpString;
        int index = 0;

        // _id
        galleryFiles.setId(_id);

        // _data
        index++;
        tmpString = cursor.getString(index);
        if (tmpString != null) {
            galleryFiles.setData(tmpString);
        }

        // _size
        index++;
        galleryFiles.setSize(cursor.getInt(index));

        // media_type
        index++;
        galleryFiles.setMedia_type(cursor.getInt(index));

        // _display_name
        index++;
        tmpString = cursor.getString(index);
        if (tmpString != null) {
            galleryFiles.setDisplay_name(tmpString);
        }

        // mime_type
        index++;
        tmpString = cursor.getString(index);
        if (tmpString != null) {
            galleryFiles.setMime_type(tmpString);
        }

        // title
        index++;
        tmpString = cursor.getString(index);
        if (tmpString != null) {
            galleryFiles.setTitle(tmpString);
        }

        // date_added
        index++;
        galleryFiles.setDate_added(cursor.getInt(index));

        // date_modified
        index++;
        galleryFiles.setDate_modified(cursor.getInt(index));

        // description
        index++;
        tmpString = cursor.getString(index);
        if (tmpString != null) {
            galleryFiles.setDescription(tmpString);
        }

        // picasa_id
        index++;
        tmpString = cursor.getString(index);
        if (tmpString != null) {
            galleryFiles.setDescription(tmpString);
        }

        // duration
        index++;
        galleryFiles.setDuration(cursor.getInt(index));

        // artist
        index++;
        tmpString = cursor.getString(index);
        if (tmpString != null) {
            galleryFiles.setArtist(tmpString);
        }

        // album
        index++;
        tmpString = cursor.getString(index);
        if (tmpString != null) {
            galleryFiles.setAlbum(tmpString);
        }

        // resolution
        index++;
        tmpString = cursor.getString(index);
        if (tmpString != null) {
            galleryFiles.setResolution(tmpString);
        }

        // width
        index++;
        galleryFiles.setWidth(cursor.getInt(index));

        // height
        index++;
        galleryFiles.setHeight(cursor.getInt(index));

        // latitude
        index++;
        galleryFiles.setLatitude(cursor.getDouble(index));

        // longitude
        index++;
        galleryFiles.setLongitude(cursor.getDouble(index));

        // datetaken
        index++;
        //LogUtil.i("datetaken = " + cursor.getLong(index));
        galleryFiles.setDatetaken((int) (cursor.getLong(index) / 1000));

        // orientation
        index++;
        galleryFiles.setOrientation(cursor.getInt(index));

        // mini_thumb_magic
        index++;
        galleryFiles.setMini_thumb_magic(cursor.getInt(index));

        // bucket_id
        index++;
        tmpString = cursor.getString(index);
        if (tmpString != null) {
            galleryFiles.setBucket_id(tmpString);
        }

        // bucket_display_name
        index++;
        tmpString = cursor.getString(index);
        if (tmpString != null) {
            galleryFiles.setBucket_display_name(tmpString);
        }

        // photo_voice_id
//        index++;
//        galleryFiles.setPhoto_voice_id(cursor.getInt(index));
    }

    public void addFile(String type, long selectionId) {
        LogUtil.i("addfiles = " + type + " , " + selectionId);

        List<Long> containIds = getContainIds();

        String where = "(media_type =" + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                + " OR " + "media_type =" + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                + ") AND " + "_id = " + selectionId;

        Cursor cursor = mResolver.query(MediaStore.Files.getContentUri("external"),
                GalleryStore.PROJECTION, where, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            long _id = cursor.getLong(0);
            if (!containIds.contains(_id)) {
                GalleryFiles galleryFiles = getGalleryFiles(_id);
                if(galleryFiles.getMime_type() != null) {
                    setGalleryFile(galleryFiles, cursor, _id);
                    galleryFilesDao.insertInTx(galleryFiles);
                    notifyUriChange(type);
                }
            } else {
                updateFiles(type);
            }
            cursor.close();
        }
    }

    private void notifyUriChange(String type) {
        if ("images".equals(type)) {
            mResolver.notifyChange(GalleryStore.Images.Media.EXTERNAL_CONTENT_URI, null);
        } else {
            mResolver.notifyChange(GalleryStore.Video.Media.EXTERNAL_CONTENT_URI, null);
        }
    }

    public void updateFiles(String type) {
        importData();
        notifyUriChange(type);
    }

    public void deleteFiles() {
        List<Long> containIds = getContainIds();
        List<Long> mediastoreIds = new ArrayList<>();

        String where = "(media_type =" + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                + " OR " + "media_type =" + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                + ")";
        Cursor cursor = mResolver.query(
                MediaStore.Files.getContentUri("external"),
                new String[]{BaseColumns._ID}, where, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            for (int index = 0; index < cursor.getCount(); index++) {
                mediastoreIds.add(cursor.getLong(0));
                cursor.moveToNext();
            }
            cursor.close();
        }

        for (Long id : containIds) {
            if (!mediastoreIds.contains(id)) {
                galleryFilesDao.deleteByKey(id);
            }
        }

        for (Long id : mediastoreIds) {
            if (!containIds.contains(id)) {
                galleryFilesDao.deleteByKey(id);
            }
        }
        updateFiles("images");
        updateFiles("video");
    }

    private static class Singleton {
        private static MediaStoreImporter instance = new MediaStoreImporter();
    }
}

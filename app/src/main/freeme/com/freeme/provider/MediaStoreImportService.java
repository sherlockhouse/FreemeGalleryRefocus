package com.freeme.provider;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.Nullable;

import com.freeme.utils.LogUtil;

import java.util.List;

/**
 * Created by tyd ConnorLin on 15-12-23.
 */
public class MediaStoreImportService extends IntentService {
    private final static String TAG = "MediaStoreImportService";

    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private final String SEGMENT_IMAGE = "images";
    private final String SEGMENT_VIDEO = "video";

    private DelayedImport mImageDelayedImport = new DelayedImport(SEGMENT_IMAGE);
    private DelayedImport mVideoDelayedImport = new DelayedImport(SEGMENT_VIDEO);

    private Handler.Callback mCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            LogUtil.i("mContentObserver msg = " + msg);
            return false;
        }
    };
    private ContentObserver mContentObserver = new ContentObserver(mHandler) {
        @Override
        public boolean deliverSelfNotifications() {
            return super.deliverSelfNotifications();
        }

        @Override
        public void onChange(boolean selfChange) {
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            LogUtil.i("mContentObserver selfChange = " + selfChange);
            LogUtil.i("mContentObserver uri = " + uri);
            List<String> segments = uri.getPathSegments();
            for (String str : segments) {
                LogUtil.i("onChange str = " + str);
            }
            if (segments.size() > 3) {
                MediaStoreImporter.getInstance().addFile(segments.get(1), Long.valueOf(segments.get(3)));
                //todo update the number to be excuted producer-consumer aialbum
            } else if (segments.size() > 2) {
                String segment = segments.get(1);
                if (SEGMENT_IMAGE.equals(segment)) {
                    deleyImport(mImageDelayedImport);
                } else {
                    deleyImport(mVideoDelayedImport);
                }
            }
            MediaStoreImporter.getInstance().deleteFiles();
        }
    };

    public MediaStoreImportService() {
        super(MediaStoreImportService.class.getSimpleName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper(), mCallback);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandlerThread.quit();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, mContentObserver);
        getContentResolver().registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, mContentObserver);
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        getContentResolver().unregisterContentObserver(mContentObserver);
        return super.onUnbind(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }

    class DelayedImport implements Runnable {

        private String mSegment = "";
        private Long mLastImportTime = -1L;

        public DelayedImport(String segment) {

            this.mSegment = segment;
        }

        @Override
        public void run() {
            long l = System.currentTimeMillis() - mLastImportTime - 2000L;
            if (l >= 0L) {
                mLastImportTime = System.currentTimeMillis();
                MediaStoreImporter.getInstance().updateFiles(mSegment);
                return;
            }
            mHandler.postDelayed(this, Math.max(l, 200L));
        }
    }


    public void deleyImport(DelayedImport delayedImport) {
        mHandler.removeCallbacks(delayedImport);
        mHandler.postDelayed(delayedImport, 400L);
    }
}

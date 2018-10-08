package com.freeme.gallery;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;

import com.aiwinn.faceFramework.Recognition;
import com.aiwinn.faceFramework.faces.FaceModel;
import com.aiwinn.wrapper.FaceSimManager;
import com.android.gallery3d.data.Path;
import com.freeme.data.FaceAlbum;
import com.freeme.data.StoryAlbum;
import com.freeme.data.StoryHelper;
import com.freeme.utils.CloseUtils;
import com.freeme.utils.FreemeUtils;
import com.freeme.utils.LogUtil;
import com.mediatek.galleryframework.util.BitmapUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.freeme.data.FaceAlbumSet.ALBUM_FACE_FEATURE;
import static com.freeme.data.FaceAlbumSet.ALBUM_FACE_ID;
import static com.freeme.data.StoryAlbumSet.ALBUM_BABY_ID;
import static com.freeme.data.StoryAlbumSet.ALBUM_KEY;
import static com.freeme.data.StoryAlbumSet.ALBUM_LOVE_ID;
import static com.freeme.data.StoryAlbumSet.MAX_BUCKET_ID;


public class GalleryClassifierService extends JobIntentService {
    public static final String DEFAULT = "default";
    public static final String EMPTY = "empty";
    public static final String NOFEATURE = "nofeature";

//    private List<String> mCategoryNameList  = new ArrayList<>(Arrays.asList(
//            "us", "flower"));

    public static List<String> mAddedAlbums  = new ArrayList<>(Arrays.asList(
            "人物", "植物"
    ));

    public static List<String> mAddedFaceAlbums  = new ArrayList<>();
    public static List<byte[]> mAddedFaceAlbumsFeatures  = new ArrayList<>();
    public static List<byte[]> mNewFaceAlbumsFeatures  = new ArrayList<>();

    private ArrayList<Path> mFacePaths = new ArrayList<>();
    private HashMap<Integer, ArrayList<Path>> mPathsGroupbyFace = new HashMap<>();
    //,"landscape", "architecture", "car", "food" , "cat", "dog", "group"));
    /*public HashMap<String, Integer> defaultAlbums  = new HashMap<String, Integer>() {
        {
            defaultAlbums.put("us", R.string.people);
            defaultAlbums.put("flower", R.string.flower_album);
            defaultAlbums.put("group", R.string.group);
            defaultAlbums.put("food", R.string.food);
            defaultAlbums.put("car", R.string.car);
            defaultAlbums.put("architecture", R.string.architecture);
            defaultAlbums.put("landscape", R.string.landscape);
            defaultAlbums.put("baby", R.string.baby);
            defaultAlbums.put("dog", R.string.dog);
            defaultAlbums.put("cat", R.string.cat);

        }
    };*/

    public HashMap<String, String> defaultAlbumsChinese  = new HashMap<String, String>() {
        {
            put("us", "人物");
            put("flower", "植物");
            put("group", "合影");
            put("food", "美食");
            put("car", "车");
            put("architecture", "建筑");
            put("landscape", "风景");
            put("baby", "萌宝");
            put("dog", "狗");
            put("cat", "猫");

        }
    };
    private String UNRECOGNIZED_KEY = "unrecognized";
    private static int UNRECOGNIZED_INDEX = 1000;
//    private HashMap<String, ArrayList<Path>> mSelectedPathByCategory = new HashMap<>();

    public static final String ACTION_COMPLETE =
            "com.freeme.gallery.galleryclassifierservice.action.COMPLETE";

    public static final String ACTION_FACE_COMPLETE =
            "com.freeme.gallery.galleryclassifierservice.action.FACECOMPLETE";

    public static final String ACTION_ADDALBUM =
            "com.freeme.gallery.galleryclassifierservice.action.ADDALBUM";

    public static final String ACTION_ADDFACEALBUM =
            "com.freeme.gallery.galleryclassifierservice.action.ADDFACEALBUM";

    public static final String ACTION_FACEDONE =
            "com.freeme.gallery.galleryclassifierservice.action.FACEDONE";

    public static final String ACTION_DONE =
            "com.freeme.gallery.galleryclassifierservice.action.DONE";

    public static final String ACTION_FACE =
            "com.freeme.gallery.galleryclassifierservice.action.FACE";
    private static final int UNIQUE_JOB_ID = 7758;
    private static Context mContext;
    private int mStoryCount;
    private int mTotalCount;
    private static volatile boolean dealing;
    private SharedPreferences mSharedPref;
    private SharedPreferences.Editor mEditor;

    private SharedPreferences mFaceSharedPref;
    private SharedPreferences.Editor mFaceEditor;
    private float degree;
    private int mMaxStoryBucketId;
    private int max;
//    private SharedPreferences.Editor mEditor;

    public static void enqueueWork(Context ctxt, Intent i) {
        mContext = ctxt;

        if (!dealing) {
            dealing = true;
            enqueueWork(ctxt, GalleryClassifierService.class, UNIQUE_JOB_ID, i);
        }

    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(ACTION_FACE)) {
            dealFace();
            return;
        }

        LogUtil.i("onHandleWork again" );
        Cursor cursor = StoryHelper.getGalleryFileCursor(getContentResolver());
        try {
            mSharedPref = getApplicationContext()
                    .getSharedPreferences(FreemeUtils.STORY_SHAREPREFERENCE_KEY, Context.MODE_PRIVATE);
            mEditor = mSharedPref.edit();

            if (cursor == null || cursor.getCount() == 0) {
                return;
            }
            mSharedPref = getApplicationContext()
                    .getSharedPreferences(FreemeUtils.STORY_SHAREPREFERENCE_KEY, Context.MODE_PRIVATE);

            String addedAlumname = "default";
            for (int i = ALBUM_BABY_ID + 2; !addedAlumname.equals("empty"); i++) {
                addedAlumname = mSharedPref.getString(ALBUM_KEY + i, "empty");
                if (!addedAlumname.equals("empty") && !mAddedAlbums.contains(addedAlumname)) {
                    mAddedAlbums.add(addedAlumname);
                }
            }


            int index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            int idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            String path;
            int id;
            int loopCount = 1;
            while (cursor.moveToNext()) {
                path = cursor.getString(index);
                id = cursor.getInt(idIndex);
                Intent bd = new Intent(ACTION_COMPLETE);
                bd.putExtra("storycount",  loopCount++ + " / " + cursor.getCount());
                LocalBroadcastManager.getInstance(this)
                        .sendBroadcast(bd);
                String gallerypath = "/local/image/item/" + id;
                Path aiPath = Path.fromString(gallerypath);
                Bitmap mBitmap = BitmapUtils.resizeDownBySideLength(
                        BitmapFactory.decodeFile(path), 600, true);

                List<Recognition> recognitions =
                        FaceSimManager.Classifier(getApplicationContext(), mBitmap);
                if (recognitions != null) {
                    Recognition mRecognitionCategory = recognitions.get(0);
                    if (mRecognitionCategory.getTitle().equals("baby")) {
                        mRecognitionCategory = recognitions.get(1);
                    }
                    String mTitle = defaultAlbumsChinese.get(mRecognitionCategory.getTitle());
                    if (mRecognitionCategory.getConfidence() > 0.41) {
                        if (mTitle.equals(mAddedAlbums.get(0))) {
                            Bitmap mFaceBitmap = BitmapUtils.resizeDownBySideLength(
                                    BitmapFactory.decodeFile(path), 800, true);
                            if (handleImageClassfier(mFaceBitmap) != null) {
                                addStoryImage(aiPath, mAddedAlbums.indexOf(mTitle));
                            } else {
                                addStoryImageUncategoried(aiPath);
                            }
                        } else if (!mAddedAlbums.contains(mTitle)) {
                            sendLocalAddalbumBd(mTitle);
                            addStoryImage(aiPath, mAddedAlbums.indexOf(mTitle));

                            //todo add new album
                        } else {
                            addStoryImage(aiPath, mAddedAlbums.indexOf(mTitle));
                        }
                    } else {
                        addStoryImageUncategoried(aiPath);
                    }
                } else {
                    addStoryImageUncategoried(aiPath);
                }

            }
        } finally {
            sendDoneLocalBroadcast(ACTION_DONE);
            CloseUtils.closeQuietly(cursor);
            dealing = false;
        }
    }

    private void addStoryImage(Path path, int albumIndex) {
        StoryAlbum.addStoryImage(getContentResolver(), path
                , albumIndex, true);
    }

    private void addStoryImageUncategoried(Path aiPath) {
        StoryAlbum.addStoryImage(getContentResolver(), aiPath
                , UNRECOGNIZED_INDEX, true);
    }

    private void sendDoneLocalBroadcast(String actionDone) {
        Intent done = new Intent(actionDone);
        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(done);
    }

    private void dealFace() {
        Cursor cursor = StoryHelper.getFaceFileCursor(getContentResolver());
        try {

            mFaceSharedPref = getApplicationContext()
                    .getSharedPreferences(FreemeUtils.FACE_SHAREPREFERENCE_KEY, Context.MODE_PRIVATE);
            mFaceEditor = mFaceSharedPref.edit();
            String addedAlumname = DEFAULT;
            String addedAlbumFeature = DEFAULT;
            max = mFaceSharedPref.getInt(MAX_BUCKET_ID , ALBUM_FACE_ID);
            for (int i = ALBUM_BABY_ID ; i < max; i++) {
                addedAlumname = mFaceSharedPref.getString(ALBUM_KEY + i, EMPTY);
                addedAlbumFeature = mFaceSharedPref.getString(ALBUM_FACE_FEATURE + i, NOFEATURE);
                if ( !mAddedFaceAlbums.contains(addedAlumname)) {
                    mAddedFaceAlbums.add(addedAlumname);
                    if (addedAlbumFeature.equals(NOFEATURE)) {
                        mAddedFaceAlbumsFeatures.add(new byte[]{(byte) i});
                    } else {
                        byte[] decodedFeatures = Base64.decode(addedAlbumFeature, Base64.DEFAULT);
                        mAddedFaceAlbumsFeatures.add(decodedFeatures);
                    }
                }
            }
            if (cursor == null || cursor.getCount() == 0) return;

            int index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            int idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            String path;
            int id;

            int loopCount;
            int faceCount = 0;
            while (faceCount < cursor.getCount()) {
                loopCount = 0;
                while ( loopCount < 2 && cursor.moveToNext()) {
                    loopCount++;
                    faceCount++;
                    path = cursor.getString(index);
                    id = cursor.getInt(idIndex);

                    String gallerypath = "/local/image/item/" + id;
                    Path aiPath = Path.fromString(gallerypath);
                    Bitmap mBitmap = BitmapUtils.resizeDownBySideLength(
                            BitmapFactory.decodeFile(path), 800, true);
                    FaceModel[] faces = FaceSimManager.DetectFace(getApplicationContext(), mBitmap);
                    if (faces == null || faces.length == 0) continue;
                    mNewFaceAlbumsFeatures.add(faces[0].getFeatures());
                    mFacePaths.add(aiPath);
                }

                for (int i = 0; i < mFacePaths.size(); i++) {
                    if (mFacePaths.get(i) == null) {
                        continue;
                    }

                    //compare with added
                    for (int j = 0; j < mAddedFaceAlbumsFeatures.size(); j++) {
                        degree = FaceSimManager.CalcFace(getApplicationContext(),
                                mNewFaceAlbumsFeatures.get(i),
                                mAddedFaceAlbumsFeatures.get(j));
                        if (degree > 0.7) {
                            FaceAlbum.addFaceImage(getContentResolver(), mFacePaths.get(i)
                                    , j  , true);
                            mFacePaths.set(i, null);
                            mNewFaceAlbumsFeatures.set(i, null);
                        }
                    }

                    //compare with new pictures & not already added
                    for (int m = 0; m < mFacePaths.size() && mFacePaths.get(i) != null; m++) {
                        if (m == i || mFacePaths.get(m) == null) {
                            continue;
                            //same file or already classified
                        }
                        degree = FaceSimManager.CalcFace(getApplicationContext(),
                                mNewFaceAlbumsFeatures.get(i),
                                mNewFaceAlbumsFeatures.get(m));
                        if (degree > 0.7) {
                            if (!mAddedFaceAlbumsFeatures.contains(mNewFaceAlbumsFeatures.get(i))) {
                                sendLocalAddFacealbumBd("face " +
                                        (max + 1), mNewFaceAlbumsFeatures.get(i));
                                FaceAlbum.addFaceImage(getContentResolver(), mFacePaths.get(i)
                                        , max - 1, true);
                                mFacePaths.set(i, null);
                                mNewFaceAlbumsFeatures.set(i, null);
                            }
                            FaceAlbum.addFaceImage(getContentResolver(), mFacePaths.get(m)
                                    , max - 1, true);
                            mFacePaths.set(m, null);
                            mNewFaceAlbumsFeatures.set(m, null);
                        }
                    }

                    //lonely picture
                    if (mFacePaths.get(i) != null) {
                        sendLocalAddFacealbumBd("face " +
                                (max + 1), mNewFaceAlbumsFeatures.get(i));
                        FaceAlbum.addFaceImage(getContentResolver(), mFacePaths.get(i)
                                , max - 1, true);
                    }
                }
                mFacePaths.clear();
                mNewFaceAlbumsFeatures.clear();

                Intent bd = new Intent(ACTION_FACE_COMPLETE);
                bd.putExtra("storycount",  faceCount + " / " + cursor.getCount());
                LocalBroadcastManager.getInstance(this)
                        .sendBroadcast(bd);
            }
        } finally {
            sendDoneLocalBroadcast(ACTION_FACEDONE);
            mFacePaths.clear();
            mNewFaceAlbumsFeatures.clear();
            mAddedFaceAlbums.clear();
            mAddedFaceAlbumsFeatures.clear();
            max = 0;
            CloseUtils.closeQuietly(cursor);
            dealing = false;
        }
    }

    private void sendLocalAddalbumBd(String title) {
        LogUtil.i("onHandleWork addalbum " + title);
        mAddedAlbums.add(title);
        mMaxStoryBucketId = mSharedPref.getInt(MAX_BUCKET_ID, ALBUM_LOVE_ID);

        mMaxStoryBucketId += 1;

//
        mEditor.putInt(MAX_BUCKET_ID, mMaxStoryBucketId);
        mEditor.putString(ALBUM_KEY + mMaxStoryBucketId, title);
        mEditor.commit();
//        mEditor.putString(ALBUM_KEY + ALBUM_BABY_ID, mRecognitionCategory.getTitle());
//        mEditor.apply();
        Intent addalbumbd = new Intent(ACTION_ADDALBUM);
        addalbumbd.putExtra("addalbum", title);
        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(addalbumbd);

    }

    private void sendLocalAddFacealbumBd(String title, byte[] mFeatures) {
        LogUtil.i("testface :" + title);
        mAddedFaceAlbums.add(title);
        mAddedFaceAlbumsFeatures.add(mFeatures);

        mMaxStoryBucketId = mFaceSharedPref.getInt(MAX_BUCKET_ID, 0);

        mFaceEditor.putString(ALBUM_KEY + mMaxStoryBucketId, title);
        mFaceEditor.putString(ALBUM_FACE_FEATURE + mMaxStoryBucketId,
                Base64.encodeToString(mFeatures, Base64.DEFAULT));
        mMaxStoryBucketId++;

        mFaceEditor.putInt(MAX_BUCKET_ID, mMaxStoryBucketId);

        mFaceEditor.commit();
        max++;

        Intent addalbumbd = new Intent(ACTION_ADDFACEALBUM);
        addalbumbd.putExtra("addalbum", title);
        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(addalbumbd);
    }

    private FaceModel[] handleImageClassfier(Bitmap bp) {
        FaceModel[] faces = FaceSimManager.DetectFace(getApplicationContext(), bp);
        if (faces == null || faces.length == 0) {
            return null;
        } else {
            return faces;
        }
    }
}

package cn.com.bst.librefocus;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;

import com.freeme.camera.CameraActivity;
import com.freeme.gallery.R;

import java.io.ByteArrayOutputStream;

public class BvirtualView extends View{
    private static final String TAG = "BvirtualView";
    private CameraActivity mCamera;
    private int mOnSingleX;
    private int mOnSingleY;
    private final static int IN_SHARPNESS_RADIUS = 200;
    private final static int OUT_SHARPNESS_RADIUS = 320;
    private final static int REFERENCE_ASPECT_SIZE = 720;
    private final static int SUPPORT_MAX_ASPECT_SIZE = 540;
    private final static boolean SHOW_PREVIEW_DEBUG_LOG = false;
    private RectF mPreviewArea = new RectF();
    private Bitmap mPartBitmap;
    private Matrix mRotationMatrix;
    private Matrix mTranslateMatrix;
    private float mPartRadius;
    private Paint mPaint;
    private PaintFlagsDrawFilter mPFDF;
    private Path mPath;
    private float mSphereSize;
    private float mCenterX;
    private float mCenterY;
    private GestureDetector mGestureDetector;
    private boolean mIsInSeekbarArea;
    private float mTranslateYProgess;
    private Bitmap mSeekbarPoint;
    private Bitmap mCircleOutWhite;
    private Bitmap mCircleOutGreen;
    private float mCircleInRadius;
    private float mCircleOutRadius;
    private int mSlop;
    private boolean mIsFousing;
    private boolean mIsCancelDiaphragm = true;
    private final static int CANCEL_DELAY = 5000;
    private boolean mIsFouseMovingSucceed;
    private int mBlurDegress;
    private int mOrientation;
    private int PREFERENCE_FACTOR = 1000;
    private float mSphereRadiusScale;
    // The value for android.hardware.Camera.setDisplayOrientation.
    private int mCameraDisplayOrientation;
    private RectF mRectF = new RectF();

    public RectF getmPreviewArea() {
        return mPreviewArea;
    }

    private Runnable mCancelDiaphragmRunnable = new Runnable() {
        @Override
        public void run() {
            mIsCancelDiaphragm = true;
            mIsFouseMovingSucceed = false;
        }
    };

    public BvirtualView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mPartRadius = context.getResources().getDimension(R.dimen.freeme_diaphragm_radius);
        BitmapDrawable drawable = (BitmapDrawable) context.getResources().getDrawable(R.drawable.freeme_vb_diaphragm_part);
        BitmapDrawable pointDrawable = (BitmapDrawable) context.getResources().getDrawable(R.drawable.freeme_seekbar_scroll_point);
        BitmapDrawable outWhiteDrawable = (BitmapDrawable) context.getResources().getDrawable(R.drawable.freeme_circle_out_white);
        BitmapDrawable outGreenDrawable = (BitmapDrawable) context.getResources().getDrawable(R.drawable.freeme_circle_out_green);
        int color = context.getResources().getColor(R.color.theme_color);
        mCircleOutGreen = outGreenDrawable.getBitmap();
        mCircleOutWhite = outWhiteDrawable.getBitmap();
        mSeekbarPoint = pointDrawable.getBitmap();
        mPartBitmap = drawable.getBitmap();
        mSphereSize = mSeekbarPoint.getWidth();
        mRotationMatrix = new Matrix();
        mTranslateMatrix = new Matrix();
        mPaint = new Paint();
        mPaint.setColor(color);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaint.setAntiAlias(true);
        mPFDF = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        mPath = new Path();
        mCircleOutRadius = mPartBitmap.getWidth() / 2.0f;
        mCircleInRadius = mCircleOutRadius * 0.75f;
        mPath.addCircle(mCircleOutRadius, mCircleOutRadius, mCircleInRadius, Path.Direction.CCW);
        mGestureDetector = new GestureDetector(context, new GestureListener());
        mBlurDegress = 4;
        mTranslateYProgess = 0.5f;
        mSphereRadiusScale = mSphereSize / 2 / mPartRadius;
    }

    public void setTranslateYProgess(float progess) {
        mTranslateYProgess = progess;
    }

    private void computeCenterCoordinate(float x, float y) {
        int intX = (int) x;
        int intY = (int) y;
        int halfSize = (int) mPartRadius;
        if (intX >= halfSize && intX <= getMeasuredWidth() - halfSize) {
            mCenterX = x;
        } else if (intX < halfSize) {
            mCenterX = halfSize;
        } else {
            mCenterX = getMeasuredWidth() - halfSize;
        }
        if (intY >= halfSize && intY <= getMeasuredHeight() - halfSize) {
            mCenterY = y;
        } else if (intY < halfSize) {
            mCenterY = halfSize;
        } else {
            mCenterY = getMeasuredHeight() - halfSize;
        }
    }

    private boolean isInSeekbarSlideArea(float x, float y) {
        boolean isInArea = false;
        PointF point = compulateSeekbarCoordinate(isScreenPortrait());
        int intX = (int) x;
        int intY = (int) y;
        float size = 3.0f * mSphereSize;
        int leftX = (int) (point.x - size);
        int topY = (int) (point.y - size);
        int rightX = (int) (point.x + size);
        int bottomY = (int) (point.y + size);
        if (intX > leftX && intX < rightX && intY > topY && intY < bottomY) {
            isInArea = true;
        }
        return isInArea;
    }

    private PointF compulateSeekbarCoordinate(boolean isPortait) {
        PointF point = new PointF();
        float progress = mTranslateYProgess;
//        int factorProgress = (int) (mTranslateYProgess * PREFERENCE_FACTOR);
//        int sphere = (int) (mSphereRadiusScale * PREFERENCE_FACTOR);
//        if (factorProgress <= sphere) {
//            progress = mSphereRadiusScale;
//        } else if (factorProgress >= (PREFERENCE_FACTOR - sphere)) {
//            progress = 1 - mSphereRadiusScale;
//        }
        if (isPortait) {
            int span = (int) (getMeasuredWidth() - mCenterX - mPartRadius - mSphereSize);
            if (span < 0) {
                point.x = mCenterX - mPartRadius - mSphereSize / 2;
            } else {
                point.x = mCenterX + mPartRadius + mSphereSize / 2;
            }
            float delta = progress * 2 * mPartRadius;
            point.y = mCenterY - mPartRadius + delta;
        } else {
            int span = (int) (getMeasuredHeight() - (mCenterY + mPartRadius + mSphereSize));
            if (span < 0) {
                point.y = mCenterY - mPartRadius - mSphereSize / 2;
            } else {
                point.y = mCenterY + mPartRadius + mSphereSize / 2;
            }
            float delta = progress * 2 * mPartRadius;
            point.x = mCenterX + mPartRadius - delta;
        }

        return point;
    }


    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private MotionEvent mDown;


        @Override
        public boolean onDown(MotionEvent event) {
            mDown = MotionEvent.obtain(event);
                mIsInSeekbarArea = isInSeekbarSlideArea(event.getX(), event.getY());
            return true;
        }


        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent ev, float distanceX, float distanceY) {
//            if (FreemeSceneModeRoot.getCurrSceneMode() == FreemeSceneModeData.FREEME_SCENE_MODE_BV_ID && mIsInSeekbarArea) {
                getHandler().removeCallbacks(mCancelDiaphragmRunnable);
                float threshold = Math.min(getMeasuredHeight(), getMeasuredWidth());
                float step = distanceY / threshold;
                boolean isPortait = isScreenPortrait();
                if (isPortait) {
                    if (Math.abs(distanceY) > Math.abs(distanceX)) {
                        mTranslateYProgess -= step * 1.5f;
                        if (mTranslateYProgess < 0f) {
                            mTranslateYProgess = 0f;
                        } else if (mTranslateYProgess > 1f) {
                            mTranslateYProgess = 1f;
                        }
                        invalidate();
                    }
                } else {
                    step = distanceX / threshold;
                    if (Math.abs(distanceX) > Math.abs(distanceY)) {
                        mTranslateYProgess += step * 1.5f;
                        if (mTranslateYProgess < 0f) {
                            mTranslateYProgess = 0f;
                        } else if (mTranslateYProgess > 1f) {
                            mTranslateYProgess = 1f;
                        }
                        invalidate();
                        return true;
                    }
                }
//            }
            int deltaX = (int) (ev.getX() - mDown.getX());
            int deltaY = (int) (ev.getY() - mDown.getY());
            if (ev.getActionMasked() == MotionEvent.ACTION_MOVE) {
                if (Math.abs(deltaX) > mSlop || Math.abs(deltaY) > mSlop) {
                    // Calculate the direction of the swipe.
                    if (deltaX >= Math.abs(deltaY)) {
//                        // Swipe right.
//                        MainActivityLayout appRootView = mCamera.getCameraAppUI().getAppRootView();
//                        if (appRootView != null) {
//                            appRootView.redirectTouchEventsTo(mCamera.getSceneModeRoot());
//                        }
                    } else if (deltaX <= -Math.abs(deltaY)) {
                        // Swipe left.
                    }
                }
            }
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            computeCenterCoordinate(e.getX(), e.getY());
//            CameraModule cameraModule = mCamera.getCurrentModule();
//            if (cameraModule instanceof PhotoModule) {
//                PhotoModule photoModule = (PhotoModule) cameraModule;
//                photoModule.onSingleTapUp(null, (int) mCenterX, (int) mCenterY);
                mIsFousing = true;
                mIsCancelDiaphragm = false;
//            }
            mIsInSeekbarArea = false;
            return true;
        }
    }


    public void setCameraDisplayOrientation(int orientation) {
        mCameraDisplayOrientation = orientation;
    }


    public void onSingleTapUp(int x, int y) {
        mOnSingleX = x;
        mOnSingleY = y;
        invalidate();
    }

    public void init(CameraActivity cameraActivity) {
        mCamera = cameraActivity;
    }

//    public void setCameraModuleScreenShotProvider(
//            CameraAppUI.CameraModuleScreenShotProvider provider) {
//        mScreenShotProvider = provider;
//    }


    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility != View.VISIBLE) {
            mIsCancelDiaphragm = true;

        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = mGestureDetector.onTouchEvent(event);
        int action = event.getAction();
        if (action == MotionEvent.ACTION_UP) {
            getHandler().removeCallbacks(mCancelDiaphragmRunnable);
            getHandler().postDelayed(mCancelDiaphragmRunnable, CANCEL_DELAY);
        }
        return result;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mPreviewArea != null) {
            Log.i(TAG, "onMeasure,width:" + mPreviewArea.width() + ",height:" + mPreviewArea.height());
            this.setMeasuredDimension((int) mPreviewArea.width(), (int) mPreviewArea.height());
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mOnSingleX = (right - left) / 2;
        mOnSingleY = (bottom - top) / 2;
        mCenterX = mOnSingleX;
        mCenterY = mOnSingleY;
        Log.i(TAG, "onLayout mOnSingleX:" + mOnSingleX + ",mOnSingleY:" + mOnSingleY);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
//        drawTrueBgVirtualWithCanvas(canvas);
//        if (FreemeSceneModeRoot.getCurrSceneMode() == FreemeSceneModeData.FREEME_SCENE_MODE_BV_ID ||
//                FreemeSceneModeRoot.getCurrSceneMode() == FreemeSceneModeData.FREEME_SCENE_MODE_PHYSIOGNOMY_ID) {
            drawDiaphragm(canvas);
//        }
    }

    private boolean isScreenPortrait() {
        boolean isPortrait = true;
        if (mOrientation == 90 || mOrientation == 270) {
            isPortrait = false;
        }
        return isPortrait;
    }

    private void drawDiaphragm(Canvas canvas) {
        if (mIsCancelDiaphragm) {
            return;
        }
        mRotationMatrix.setScale(0.9f, 0.9f, mCircleOutRadius, mCircleOutRadius);
        mRotationMatrix.postTranslate(mCenterX - mCircleOutRadius, mCenterY - mCircleOutRadius);
        if (mIsFousing) {
            canvas.drawBitmap(mCircleOutWhite, mRotationMatrix, null);
        } else {
            canvas.drawBitmap(mCircleOutGreen, mRotationMatrix, null);
            boolean isPortrait = isScreenPortrait();
//            if (FreemeSceneModeRoot.getCurrSceneMode() == FreemeSceneModeData.FREEME_SCENE_MODE_BV_ID)
//            {
            PointF pointF = compulateSeekbarCoordinate(isPortrait);
            canvas.drawBitmap(mSeekbarPoint, pointF.x - mSphereSize / 2, pointF.y -
                    mSphereSize / 2, null);
            if (isPortrait) {
                int topStopY = (int) (pointF.y - mSphereSize / 2 - (mCenterY - mPartRadius));
                int bottomStopY = (int) (mCenterY + mPartRadius - (pointF.y + mSphereSize / 2));
                if (topStopY <= 0) {
                    canvas.drawLine(pointF.x, pointF.y + mSphereSize / 2, pointF.x, mCenterY
                            + mPartRadius, mPaint);
                } else if (bottomStopY <= 0) {
                    canvas.drawLine(pointF.x, mCenterY - mPartRadius, pointF.x, pointF.y -
                            mSphereSize / 2, mPaint);
                } else {
                    canvas.drawLine(pointF.x, mCenterY - mPartRadius, pointF.x, pointF.y -
                            mSphereSize / 2, mPaint);
                    canvas.drawLine(pointF.x, pointF.y + mSphereSize / 2, pointF.x, mCenterY
                            + mPartRadius, mPaint);
                }
            } else {
                int leftStopX = (int) (pointF.x - mSphereSize / 2 - (mCenterX - mPartRadius));
                int rightStopX = (int) (mCenterX + mPartRadius - (pointF.x + mSphereSize / 2));
                if (leftStopX <= 0) {
                    canvas.drawLine(pointF.x + mSphereSize / 2, pointF.y, mCenterX +
                            mPartRadius, pointF.y, mPaint);
                } else if (rightStopX <= 0) {
                    canvas.drawLine(mCenterX - mPartRadius, pointF.y, pointF.x - mSphereSize
                            / 2, pointF.y, mPaint);
                } else {
                    canvas.drawLine(mCenterX - mPartRadius, pointF.y, pointF.x - mSphereSize
                            / 2, pointF.y, mPaint);
                    canvas.drawLine(pointF.x + mSphereSize / 2, pointF.y, mCenterX +
                            mPartRadius, pointF.y, mPaint);
                }
            }

            if (mIsInSeekbarArea) {
                canvas.save();
                canvas.translate(mCenterX - mCircleOutRadius, mCenterY - mCircleOutRadius);
                canvas.setDrawFilter(mPFDF);
                canvas.clipPath(mPath, Region.Op.REPLACE);
                float[] point = new float[2];
                for (int i = 0; i < 360; i += 60) {
                    float progess = mTranslateYProgess;
                    if (progess < 0.1f) {
                        progess = 0.1f;
                    } else if (progess > 0.9f) {
                        progess = 0.9f;
                    }
                    point[0] = -mCircleInRadius * (1 - progess);
                    point[1] = 0;
                    mRotationMatrix.setRotate(i, mCircleOutRadius, mCircleOutRadius);
                    mTranslateMatrix.setRotate(i + 120);
                    mTranslateMatrix.mapPoints(point);
                    mRotationMatrix.postTranslate(point[0], point[1]);
                    canvas.drawBitmap(mPartBitmap, mRotationMatrix, mPaint);
                }
                canvas.restore();
            }
        }
//        }
    }


}

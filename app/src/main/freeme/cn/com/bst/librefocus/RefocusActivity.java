package cn.com.bst.librefocus;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.freeme.gallery.R;

import java.lang.ref.WeakReference;

public class RefocusActivity extends Activity {

    private long mImageHandle = 0;
    private static final String OTP_PATH = "/vendor/etc/freemegallery/bokeh_otp_default.dat";
    private static final String CFG_FILE_PATH = "/vendor/etc/freemegallery/bokeh_still_normal.cfg;/vendor/etc/freemegallery/bokeh_still_night.cfg;/vendor/etc/freemegallery/bokeh_still_macro.cfg";
    private Refocuser.Image mDisplayImage = null;
    private FocusCanvas mFocusCanvas;
    private TextView mFNumberText;
    private String filePath;
    private SeekBar mAdjustSeekBar;

    private static final int PROGRESS_PER_DOF = 20;
    private static final String[] DOFDATA = {"11", "10",
            "9.0", "8.0", "7.2", "6.3", "5.6", "4.5", "3.6", "2.8",
            "2.2", "1.8", "1.4", "1.3", "1.2", "1.0", "0.8"};

    private static final float[] DOFDATAFLOAT = {11f, 10f,
            9.0f, 8.0f, 7.2f, 6.3f, 5.6f, 4.5f, 3.6f, 2.8f,
            2.2f, 1.8f, 1.4f, 1.3f, 1.2f, 1.0f, 0.8f};
    private static final String LEVEL_DEFAULT = "7";
    private static final long PROGRESS_DELAY_TIME = 400;


    private String mLevel = LEVEL_DEFAULT;
    private Handler mMainHandler;
    private float[] imagePos;
    private float fNumber;
    private int mProgress;
    private ImageView mBackImage;
    private TextView mBackTextview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.refocus_layout);
        initData();
        initViews();
    }

    private void initData() {
        mMainHandler = new MainHandler(this, getMainLooper());
        filePath = getIntent().getData().toString();
    }

    private void initViews() {
        SurfaceView mSurface = (SurfaceView)findViewById(R.id.surface);
        ImageView mSaveBt = (ImageView)findViewById(R.id.saveButton);
        mFocusCanvas = (FocusCanvas)findViewById(R.id.focus_canvas);
        mFNumberText = (TextView)findViewById(R.id.aperture_indicator);
        mBackImage = (ImageView)findViewById(R.id.photopage_back_image);
        mBackTextview = (TextView)findViewById(R.id.photopage_back_text);
        mSurface.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                Refocuser._displayHelperInitDisplay(surfaceHolder.getSurface());
                long h = Refocuser._createRefocus(filePath, OTP_PATH, CFG_FILE_PATH);
                if (h != 0) {
                    mImageHandle = h;
                    refreshDisplay();
                } else {
                    // ERROR
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                long h = mImageHandle;
                mImageHandle = 0;
                if (h != 0) Refocuser._destroyRefocus(h);
                Refocuser._displayHelperDestroyDisplay();
            }
        });

        mSurface.setOnTouchListener(new View.OnTouchListener() {
            private boolean isDown = false;

            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    if (!isDown) {
                        if (mImageHandle == 0) return true;
                        float canvasX = motionEvent.getX();
                        float canvasY = motionEvent.getY();
                        imagePos = Refocuser._displayHelperGetPointRelativeToPicture(canvasX, canvasY);
                        mMainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                doRefocusDisplay();
                            }
                        });
                    }
                    isDown = true;
                } else if (action == MotionEvent.ACTION_UP) {
                    isDown = false;
                }
                return true;
            }
        });

        mSaveBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mImageHandle != 0) {
                    int res = Refocuser._saveImage(mImageHandle, filePath);
                    if (res != 0) {
                        // ERROR
                    } else {
                        Toast.makeText(RefocusActivity.this, R.string.refocus_save_success, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        mAdjustSeekBar = findViewById(R.id.sdof_adjust_bar);
        mAdjustSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                int depth = progress / PROGRESS_PER_DOF;
                fNumber = DOFDATAFLOAT[depth];
                mFNumberText.setText("F" + DOFDATA[depth]);
                if (!String.valueOf(depth).equals(mLevel)) {
                    mLevel = String.valueOf(depth);
                    Handler handler = getMainHander();
                    handler.removeCallbacks(mSetLevelRunnable);
                    handler.postDelayed(mSetLevelRunnable, PROGRESS_DELAY_TIME);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mLevel = LEVEL_DEFAULT;
        mProgress = Integer.parseInt(mLevel) * PROGRESS_PER_DOF;
        mAdjustSeekBar.setProgress(mProgress);
    }

    private void doRefocusDisplay() {
        int res = Refocuser._doRefocus(mImageHandle, imagePos[0], imagePos[1], fNumber);
        if (res == 0) {
            refreshDisplay();
        } else {
            //errror
        }
    }

    private Runnable mSetLevelRunnable = new Runnable() {
        @Override
        public void run() {
            if (imagePos == null) {
                mDisplayImage = Refocuser._getDisplayImage(mImageHandle);
                Refocuser._displayHelperDisplayRefocusImage(mImageHandle);
                // refresh ui
                int uniformedX = mDisplayImage.focusX * 1000 / mDisplayImage.width;
                int uniformedY = mDisplayImage.focusY * 1000 / mDisplayImage.height;
                float[] winPos = Refocuser._displayHelperGetPointRelativeToSurface(
                        uniformedX, uniformedY);
                imagePos = Refocuser._displayHelperGetPointRelativeToPicture(winPos[0], winPos[1]);
            }

            doRefocusDisplay();

        }
    };

    private Runnable mSetFocusCanvasRunnable = new Runnable() {
        @Override
        public void run() {
            mFocusCanvas.setVisibility(View.INVISIBLE);
            mFocusCanvas.setWhiteIcon();
        }
    };

    private void refreshDisplay() {
        // refresh image
        mDisplayImage = Refocuser._getDisplayImage(mImageHandle);
        Refocuser._displayHelperDisplayRefocusImage(mImageHandle);
        // refresh ui
        int uniformedX = mDisplayImage.focusX * 1000 / mDisplayImage.width;
        int uniformedY = mDisplayImage.focusY * 1000 / mDisplayImage.height;
        float[] windowPos = Refocuser._displayHelperGetPointRelativeToSurface(
                uniformedX, uniformedY);
        if (windowPos != null && windowPos.length == 2) {
            mFocusCanvas.focus((int) windowPos[0], (int) windowPos[1]);
            mFocusCanvas.postInvalidate();
        }
        String fString = String.valueOf(mDisplayImage.fNumber);
        mFNumberText.setText("F" + fString);


    }

    public Handler getMainHander() {
        return mMainHandler;
    }

    private static class MainHandler extends Handler {
        final WeakReference<Activity> mActivity;

        public MainHandler(Activity activity, Looper looper) {
            super(looper);
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            Activity activity = mActivity.get();
            if (activity == null) {
                return;
            }
            switch (msg.what) {
                case 1: {
                    break;
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public void backClicked(View view) {
        onBackPressed();
    }

}

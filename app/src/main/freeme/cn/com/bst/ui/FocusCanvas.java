package cn.com.bst.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.freeme.gallery.R;


public class FocusCanvas extends View {

    private static final String TAG = FocusCanvas.class.getSimpleName();
    private Bitmap mIcon;
    private int fx = -1;
    private int fy = -1;
    private int halfPicWidth = -1;
    private int halfPicHeight = -1;
    private Paint mP = new Paint();
    private Paint mPointP = new Paint();

    public FocusCanvas(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        Resources res = getResources();
        mIcon = BitmapFactory.decodeResource(res, R.drawable.ic_focus_point);
        halfPicWidth = mIcon.getWidth() / 2;
        halfPicHeight = mIcon.getHeight() / 2;
        mPointP.setStrokeWidth(20);
    }

    public void focus(int x, int y) {
        fx = x;
        fy = y;
        invalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        int width = getWidth();
        int height = getHeight();
        Log.d(TAG, "canvas " + width + "*" + height + ", focus " + fx + "*" + fy);
        if (fx > 0 && fx < width && fy > 0 && fy < height) {
            canvas.drawBitmap(mIcon, fx - halfPicWidth, fy - halfPicHeight, mP);

            canvas.drawPoint(fx, fy, mPointP);
        }
    }
}

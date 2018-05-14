package cn.com.bst.librefocus;


import android.view.Surface;

import java.nio.ByteBuffer;

public class Refocuser {

    public static class Image {
        public static final int TYPE_ERROR = -1;
        public static final int TYPE_NV12 = 1;
        public static final int TYPE_NV21 = 2;
        public static final int TYPE_RGB888 = 3;
        public static final int TYPE_RGBA8888 = 4;

        public final ByteBuffer buffer;
        public final int width;
        public final int height;
        public final int type;
        public final int focusX;
        public final int focusY;
        public final float fNumber;

        private Image(ByteBuffer b, int w, int h, int t, int fx, int fy, float fn) {
            buffer = b;
            width = w;
            height = h;
            type = t;

            focusX = fx;
            focusY = fy;
            fNumber = fn;
        }
    }

    public static native long _createRefocus(String imgPath, String otpPath, String cfgPath);
    public static native Image _getDisplayImage(long handle);
    public static native int _doRefocus(long handle, float x, float y, float fNumber); // upper left of image is (0, 0)
    public static native void _destroyRefocus(long handle);
    public static native int _saveImage(long handle, String path);
    /**
     * Return: 0 : is refocus image.
     *        -1 : not mpo image
     *        -2 : no bst mpo exif found
     *        other: not refocus image
     */
    public static native int _isRefocusImage(String path);

    public static native int _displayHelperInitDisplay(Surface surface);
    public static native int _displayHelperDisplayRefocusImage(long handle);
    public static native float[] _displayHelperGetPointRelativeToPicture(float x, float y);
    public static native float[] _displayHelperGetPointRelativeToSurface(float x, float y);
    public static native void _displayHelperDestroyDisplay();

    static {
        System.loadLibrary("DepthBokehEffectRefocus");
    }
}

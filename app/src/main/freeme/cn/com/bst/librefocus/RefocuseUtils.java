package cn.com.bst.librefocus;

/**
 * Created by gulincheng on 18-4-18.
 */

public class RefocuseUtils {

    public static boolean isRefocusImage(String path) {
        return Refocuser._isRefocusImage(path) == 0;
    }
}

package com.freeme.bigmodel;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.util.LinkedList;

public class BlockBaseActivity extends Activity {

    public static final LinkedList<Activity> activitys = new LinkedList<Activity>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activitys.addLast(this);
    }


    @Override
    public void finish() {
        super.finish();
        if(activitys != null) {
            activitys.removeLast();
        }
    }

    public void outOfBigModeWithReslut(Intent intent) {
        for (int i = activitys.size() - 1; i >= 0; i--) {
            Activity a = activitys.get(i);
            if (i == 0) {
                a.setResult(RESULT_OK, intent);
            }
            a.finish();
        }
    }

}

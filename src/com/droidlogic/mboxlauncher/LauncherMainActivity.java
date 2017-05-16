package com.droidlogic.mboxlauncher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.droidlogic.app.SystemControlManager;
import android.content.ComponentName;

public class LauncherMainActivity extends Activity {
    private static String TAG = "LauncherMainActivity";
    private static String COMPONENT_TV_APP = "com.droidlogic.tvsource/com.droidlogic.tvsource.DroidLogicTv";

    private SystemControlManager mSystemControlManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "------onCreate");

        mSystemControlManager = new SystemControlManager(this);
        if (TextUtils.equals(mSystemControlManager.getProperty("ro.platform.has.tvuimode"), "true") &&
            !TextUtils.equals(mSystemControlManager.getProperty("tv.launcher.firsttime.start"), "false") &&
            Settings.System.getInt(getContentResolver(), "tv_start_up_enter_app", 0) > 0) {
            Log.d(TAG, "starting tvapp...");
            Intent intent = new Intent();
            intent.setComponent(ComponentName.unflattenFromString(COMPONENT_TV_APP));
            startActivity(intent);
        } else {
            Log.d(TAG, "starting launcher...");
            Intent intent = new Intent();
            intent.setClass(this, Launcher.class);
            startActivity(intent);
        }
        mSystemControlManager.setProperty("tv.launcher.firsttime.start", "false");
        finish();
    }
}

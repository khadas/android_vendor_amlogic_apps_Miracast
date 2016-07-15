package com.droidlogic.mboxlauncher;

import android.app.SearchManager;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.pm.ActivityInfo;
import android.content.ComponentName;
import android.database.ContentObserver;
import android.database.IContentObserver;
import android.app.Activity;
import android.app.Instrumentation;
import android.graphics.drawable.Drawable;
import android.media.tv.TvContract;
import android.media.tv.TvContract.Channels;
import android.media.tv.TvView;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.KeyEvent;
import android.view.View.OnTouchListener;
import android.view.IWindowManager;

import android.widget.TextView;
import android.widget.ImageView;
import android.widget.GridView;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.graphics.Typeface;
import android.graphics.Rect;

import com.droidlogic.app.SystemControlManager;
import com.droidlogic.app.tv.ChannelInfo;
import com.droidlogic.app.tv.DroidLogicTvUtils;
import com.droidlogic.app.tv.TvControlManager;
import com.droidlogic.app.tv.TvDataBaseManager;

import java.util.List;
import java.util.ArrayList;

public class Launcher extends Activity{

    private final static String TAG="MediaBoxLauncher";

    private final String net_change_action = "android.net.conn.CONNECTIVITY_CHANGE";
    private final String wifi_signal_action = "android.net.wifi.RSSI_CHANGED";
    private final String outputmode_change_action = "android.amlogic.settings.CHANGE_OUTPUT_MODE";

    public static String COMPONENT_TV_APP = "com.droidlogic.tvsource/com.droidlogic.tvsource.DroidLogicTv";
    public static String COMPONENT_TV_SETTINGS = "com.android.tv.settings/com.android.tv.settings.MainSettings";
    public static String DEFAULT_INPUT_ID = "com.droidlogic.tvinput/.services.ATVInputService/HW0";
    public static final String PROP_TV_PREVIEW = "tv.is.preview.window";

    public static final int TYPE_VIDEO                           = 0;
    public static final int TYPE_RECOMMEND                       = 1;
    public static final int TYPE_MUSIC                           = 2;
    public static final int TYPE_APP                             = 3;
    public static final int TYPE_LOCAL                           = 4;
    public static final int TYPE_SETTINGS                        = 5;
    public static final int TYPE_HOME_SHORTCUT                   = 6;
    public static final int TYPE_APP_SHORTCUT                    = 7;

    public static final int MODE_HOME                            = 0;
    public static final int MODE_VIDEO                           = 1;
    public static final int MODE_RECOMMEND                       = 2;
    public static final int MODE_MUSIC                           = 3;
    public static final int MODE_APP                             = 4;
    public static final int MODE_LOCAL                           = 5;
    public static final int MODE_CUSTOM                          = 6;
    private int current_screen_mode = 0;
    private int saveModeBeforeCustom = 0;

    private static final int MSG_REFRESH_SHORTCUT                = 0;
    private static final int MSG_RECOVER_HOME                    = 1;
    private static final int animDuration                        = 70;
    private static final int animDelay                           = 0;

    private static final int[] childScreens = {
        MODE_VIDEO,
        MODE_RECOMMEND,
        MODE_APP,
        MODE_MUSIC,
        MODE_LOCAL
    };
    private static final int[] childScreensTv = {
        MODE_RECOMMEND,
        MODE_APP,
        MODE_MUSIC,
        MODE_LOCAL
    };
    private int[] mChildScreens = childScreens;

    private GridView lv_status;
    private HoverView mHoverView;
    private ViewGroup mHomeView = null;
    private AppLayout mSecondScreen = null;
    private View saveHomeFocusView = null;
    private MyGridLayout mHomeShortcutView = null;
    private MyRelativeLayout mVideoView;
    private MyRelativeLayout mRecommendView;
    private MyRelativeLayout mMusicView;
    private MyRelativeLayout mAppView;
    private MyRelativeLayout mLocalView;
    private MyRelativeLayout mSettingsView;
    private CustomView mCustomView = null;

    public static int HOME_SHORTCUT_COUNT                      = 10;

    private TvView tvView = null;
    private TextView tvPrompt = null;
    public static final int TV_MODE_NORMAL                     = 0;
    public static final int TV_MODE_TOP                        = 1;
    public static final int TV_MODE_BOTTOM                     = 2;
    private static final int TV_PROMPT_GOT_SIGNAL              = 0;
    private static final int TV_PROMPT_NO_SIGNAL               = 1;
    private static final int TV_PROMPT_IS_SCRAMBLED            = 2;
    private static final int TV_PROMPT_NO_DEVICE               = 3;
    private static final int TV_WINDOW_WIDTH                   = 310;
    private static final int TV_WINDOW_HEIGHT                  = 174;
    private static final int TV_WINDOW_NORMAL_LEFT             = 120;
    private static final int TV_WINDOW_NORMAL_TOP              = 197;
    private static final int TV_WINDOW_RIGHT_LEFT              = 1279 - TV_WINDOW_WIDTH;
    private static final int TV_WINDOW_TOP_TOP = 0;
    private static final int TV_WINDOW_BOTTOM_TOP              = 719 - TV_WINDOW_HEIGHT;
    private static final int TV_MSG_PLAY_TV                    = 0;
    public int tvViewMode = -1;
    private int mTvTop = -1;
    private boolean isRadioChannel = false;
    private ChannelObserver mChannelObserver;
    private TvInputManager mTvInputManager;
    private TvInputChangeCallback mTvInputChangeCallback;
    private TvDataBaseManager mTvDataBaseManager;
    private String mTvInputId;
    private Uri mChannelUri;

    public static float startX;
    public static float endX;
    private SystemControlManager mSystemControlManager;
    private IWindowManager mWindowManager;
    private AppDataLoader mAppDataLoader;
    private StatusLoader mStatusLoader;
    private static float scale_value;
    private Object mlock = new Object();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Log.d(TAG, "------onCreate");

        if (DesUtils.isAmlogicChip() == false) {
            finish();
        }
        mSystemControlManager = new SystemControlManager(this);
        mAppDataLoader = new AppDataLoader(this);
        mStatusLoader = new StatusLoader(this);
        mWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        scale_value = getAnimationScaleValue();

        mAppDataLoader.update();
        initChildViews();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addDataScheme("file");
        registerReceiver(mediaReceiver, filter);

        filter = new IntentFilter();
        filter.addAction(net_change_action);
        filter.addAction(wifi_signal_action);
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(outputmode_change_action);
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        registerReceiver(netReceiver, filter);

        filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        registerReceiver(appReceiver, filter);

        filter = new IntentFilter();
        filter.addAction("com.droidlogic.instaboot.RELOAD_APP_COMPLETED");
        registerReceiver(instabootReceiver, filter);
    }

    public boolean isMboxFeture () {
        return mSystemControlManager.getPropertyBoolean("ro.platform.has.mbxuimode", false);
    }

    public boolean isTvFeture () {
        return mSystemControlManager.getPropertyBoolean("ro.platform.has.tvuimode", false);
    }

    public boolean needPreviewFeture () {
        return isTvFeture() && mSystemControlManager.getPropertyBoolean("tv.need.preview_window", true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "------onResume");

        if (isMboxFeture()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        //getMainView().animate().translationY(0).start();
        setBigBackgroundDrawable();
        displayShortcuts();
        displayStatus();
        displayDate();

        if (needPreviewFeture()) {
            tvView.setVisibility(View.VISIBLE);
            mTvHandler.sendEmptyMessage(TV_MSG_PLAY_TV);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "------onPause");
    }

    @Override
    protected void onStop() {
        recycleBigBackgroundDrawable();
        if (needPreviewFeture())
            releaseTvView();
        super.onStop();
        Log.d(TAG, "------onStop");
    }

    @Override
    protected void onDestroy(){
        unregisterReceiver(mediaReceiver);
        unregisterReceiver(netReceiver);
        unregisterReceiver(appReceiver);
        unregisterReceiver(instabootReceiver);
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (Intent.ACTION_MAIN.equals(intent.getAction())) {
            setHomeViewVisible(true);
            current_screen_mode = MODE_HOME;
            MyRelativeLayout videoView = (MyRelativeLayout)findViewById(R.id.layout_video);
            videoView.requestFocus();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            startX = ev.getX();
        } else if (ev.getAction() == MotionEvent.ACTION_UP) {
            endX = ev.getX();
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent (MotionEvent event){
        if (event.getAction() == MotionEvent.ACTION_UP) {
        }
        return true;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
                switch (current_screen_mode) {
                    case MODE_VIDEO:
                    case MODE_RECOMMEND:
                    case MODE_APP:
                    case MODE_MUSIC:
                    case MODE_LOCAL:
                        setHomeViewVisible(true);
                        break;
                    case MODE_CUSTOM:
                        current_screen_mode = saveModeBeforeCustom;
                        mAppDataLoader.update();
                        break;
                }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
        }else if (keyCode == KeyEvent.KEYCODE_SEARCH) {
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            ComponentName globalSearchActivity = searchManager.getGlobalSearchActivity();
            if (globalSearchActivity == null) {
                return false;
            }
            Intent intent = new Intent(SearchManager.INTENT_ACTION_GLOBAL_SEARCH);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setComponent(globalSearchActivity);
            Bundle appSearchData = new Bundle();
            appSearchData.putString("source", "launcher-search");
            intent.putExtra(SearchManager.APP_DATA, appSearchData);
            startActivity(intent);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void displayStatus() {
        LocalAdapter ad = new LocalAdapter(this,
                mStatusLoader.getStatusData(),
                R.layout.homelist_item,
                new String[] {StatusLoader.ICON},
                new int[] {R.id.item_type});
        lv_status.setAdapter(ad);
    }

    private void displayDate() {
        TextView  time = (TextView)findViewById(R.id.tx_time);
        TextView  date = (TextView)findViewById(R.id.tx_date);
        time.setText(mStatusLoader.getTime());
        time.setTypeface(Typeface.DEFAULT_BOLD);
        date.setText(mStatusLoader.getDate());
    }

    private void initChildViews(){
        lv_status = (GridView)findViewById(R.id.list_status);
        lv_status.setOnTouchListener(new OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    return true;
                }
                return false;
            }
        });
        mHoverView = (HoverView)findViewById(R.id.hover_view);
        mHomeView = (ViewGroup)findViewById(R.id.layout_homepage);
        mSecondScreen  = (AppLayout)findViewById(R.id.second_screen);
        mHomeShortcutView = (MyGridLayout)findViewById(R.id.gv_shortcut);
        mVideoView = (MyRelativeLayout)findViewById(R.id.layout_video);
        mRecommendView = (MyRelativeLayout)findViewById(R.id.layout_recommend);
        mMusicView = (MyRelativeLayout)findViewById(R.id.layout_music);
        mAppView = (MyRelativeLayout)findViewById(R.id.layout_app);
        mLocalView = (MyRelativeLayout)findViewById(R.id.layout_local);
        mSettingsView = (MyRelativeLayout)findViewById(R.id.layout_setting);
        setHomeRectType();

        tvView = (TvView)findViewById(R.id.tv_view);
        tvPrompt = (TextView)findViewById(R.id.tx_tv_prompt);
        if (needPreviewFeture()) {
            mChildScreens = childScreensTv;
            setTvView();
        } else {
            mChildScreens = childScreens;
            tvView.setVisibility(View.GONE);
            tvPrompt.setVisibility(View.GONE);
        }
    }

    private void setBigBackgroundDrawable() {
        getMainView().setBackgroundDrawable(getResources().getDrawable(R.drawable.bg));
        ((ImageView)findViewById(R.id.img_video)).setImageDrawable(getResources().getDrawable(R.drawable.img_video));
        ((ImageView)findViewById(R.id.img_recommend)).setImageDrawable(getResources().getDrawable(R.drawable.img_recommend));
        ((ImageView)findViewById(R.id.img_music)).setImageDrawable(getResources().getDrawable(R.drawable.img_music));
        ((ImageView)findViewById(R.id.img_app)).setImageDrawable(getResources().getDrawable(R.drawable.img_app));
        ((ImageView)findViewById(R.id.img_local)).setImageDrawable(getResources().getDrawable(R.drawable.img_local));
        ((ImageView)findViewById(R.id.img_setting)).setImageDrawable(getResources().getDrawable(R.drawable.img_setting));
    }

    private void recycleBigBackgroundDrawable() {
        Drawable drawable = getMainView().getBackground();
        getMainView().setBackgroundResource(0);
        if (drawable != null)
            drawable.setCallback(null);

        drawable = ((ImageView)findViewById(R.id.img_video)).getDrawable();
        if (drawable != null)
            drawable.setCallback(null);

        drawable = ((ImageView)findViewById(R.id.img_video)).getDrawable();
        if (drawable != null)
            drawable.setCallback(null);

        drawable = ((ImageView)findViewById(R.id.img_video)).getDrawable();
        if (drawable != null)
            drawable.setCallback(null);

        drawable = ((ImageView)findViewById(R.id.img_video)).getDrawable();
        if (drawable != null)
            drawable.setCallback(null);

        drawable = ((ImageView)findViewById(R.id.img_video)).getDrawable();
        if (drawable != null)
            drawable.setCallback(null);

        drawable = ((ImageView)findViewById(R.id.img_video)).getDrawable();
        if (drawable != null)
            drawable.setCallback(null);
    }

    private void setHomeRectType(){
        mVideoView.setType(TYPE_VIDEO);
        mMusicView.setType(TYPE_MUSIC);
        mRecommendView.setType(TYPE_RECOMMEND);
        mAppView.setType(TYPE_APP);
        mLocalView.setType(TYPE_LOCAL);

        Intent intent = new Intent();
        intent.setComponent(ComponentName.unflattenFromString(COMPONENT_TV_SETTINGS));
        mSettingsView.setType(TYPE_SETTINGS);
        mSettingsView.setIntent(intent);
    }

    public void displayShortcuts() {
        mAppDataLoader.update();
        switch (current_screen_mode) {
            case MODE_HOME:
            case MODE_VIDEO:
            case MODE_RECOMMEND:
            case MODE_MUSIC:
            case MODE_APP:
            case MODE_LOCAL:
                setShortcutScreen(current_screen_mode);
                break;
            default:
                setShortcutScreen(saveModeBeforeCustom);
                break;
        }
    }

    private void updateStatus() {
        ((BaseAdapter) lv_status.getAdapter()).notifyDataSetChanged();
    }

    public int getCurrentScreenMode() {
        return current_screen_mode;
    }
    public void setShortcutScreen(int mode) {
        resetShortcutScreen(mode);
        current_screen_mode = mode;
    }

    public void resetShortcutScreen(int mode) {
        mHandler.removeMessages(MSG_REFRESH_SHORTCUT);
        Log.d(TAG, "resetShortcutScreen mode is " + mode);
        if (mAppDataLoader.isDataLoaded()) {
            if (mode == MODE_HOME) {
                mHomeShortcutView.setLayoutView(mode, mAppDataLoader.getShortcutList(mode));
            } else {
                mSecondScreen.setLayout(mode, mAppDataLoader.getShortcutList(mode));
            }
        } else {
            Message msg = new Message();
            msg.what = MSG_REFRESH_SHORTCUT;
            msg.arg1 = mode;
            mHandler.sendMessageDelayed(msg, 100);
        }
    }

    private int getChildModeIndex() {
        for (int i = 0; i < mChildScreens.length; i++) {
            if (current_screen_mode == mChildScreens[i]) {
                return i;
            }
        }
        return -1;
    }

    public AppDataLoader getAppDataLoader() {
        return mAppDataLoader;
    }

    public void switchSecondScren(int animType){
        int mode = -1;
        if (animType == AppLayout.ANIM_LEFT) {
            mode = mChildScreens[(getChildModeIndex() + mChildScreens.length - 1) % mChildScreens.length];
        } else {
            mode = mChildScreens[(getChildModeIndex() + 1) % mChildScreens.length];
        }
        mSecondScreen.setLayoutWithAnim(animType, mode, mAppDataLoader.getShortcutList(mode));
        current_screen_mode = mode;
    }

    public void setHomeViewVisible (boolean isShowHome) {
        if (isShowHome) {
            if (mCustomView != null && current_screen_mode == MODE_CUSTOM) {
                mCustomView.recoverMainView();
            }
            current_screen_mode = MODE_HOME;
            mSecondScreen.setVisibility(View.GONE);
            mHomeView.setVisibility(View.VISIBLE);
            if (needPreviewFeture())
                setTvViewPosition(TV_MODE_NORMAL);
        } else {
            mHomeView.setVisibility(View.GONE);
            mSecondScreen.setVisibility(View.VISIBLE);
            if (needPreviewFeture()) {
                setTvViewPosition(TV_MODE_BOTTOM);
            }
        }
    }

    public HoverView getHoverView(){
        return mHoverView;
    }

    public ViewGroup getHomeView(){
        return mHomeView;
    }

    public ViewGroup getMainView(){
        return (ViewGroup)findViewById(R.id.layout_main);
    }

    public ViewGroup getRootView(){
        return (ViewGroup)findViewById(R.id.layout_root);
    }

    public Object getLock() {
        return mlock;
    }

    public void saveHomeFocus(View view) {
        saveHomeFocusView = view;
    }

    private void sendKeyCode(final int keyCode){
        new Thread () {
            public void run() {
                try {
                    Instrumentation inst = new Instrumentation();
                    inst.sendKeyDownUpSync(keyCode);
                } catch (Exception e) {
                    Log.e("Exception when sendPointerSync", e.toString());
                }
            }
        }.start();
    }

    private void updateAppList(Intent intent){
        boolean isShortcutIndex = false;
        String packageName = null;

        if (intent.getData() != null) {
            packageName = intent.getData().getSchemeSpecificPart();
            if (packageName == null || packageName.length() == 0) {
                // they sent us a bad intent
                return;
            }
            if (packageName.equals("com.android.provision"))
                return;
        }
        displayShortcuts();
    }

    private static final int INDEX_TRANSITION_ANIMATION_SCALE = 1;
    private void setAnimationScale(boolean enable_animation) {
        try {
            if (enable_animation) {
                mWindowManager.setAnimationScale(INDEX_TRANSITION_ANIMATION_SCALE, scale_value);
            } else {
                mWindowManager.setAnimationScale(INDEX_TRANSITION_ANIMATION_SCALE, 0);
            }
        } catch (RemoteException e) {
        }
    }

    private float getAnimationScaleValue() {
        float scale = 0;
        try {
            scale = mWindowManager.getAnimationScale(INDEX_TRANSITION_ANIMATION_SCALE);
        } catch (RemoteException e) {
        }
        return scale;
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REFRESH_SHORTCUT:
                    resetShortcutScreen(msg.arg1);
                    break;
                case MSG_RECOVER_HOME:
                    resetShortcutScreen(current_screen_mode);
                    break;
                default:
                    break;
            }
        }
    };

    private BroadcastReceiver mediaReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            //Log.d(TAG, " mediaReceiver		  action = " + action);
            if (action == null)
                return;

            if (Intent.ACTION_MEDIA_EJECT.equals(action)
                    || Intent.ACTION_MEDIA_UNMOUNTED.equals(action) || Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
                displayStatus();
                updateStatus();
            }
        }
    };

    private BroadcastReceiver netReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action == null)
                return;

             //Log.d(TAG, "netReceiver         action = " + action);
            if (action.equals(Intent.ACTION_TIME_CHANGED)) {
                displayDate();
            }
            if (action.equals(Intent.ACTION_TIME_TICK)) {
                displayDate();
            } else if (Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE.equals(action)
                    || Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE.equals(action)) {
                updateAppList(intent);
            }else {
                displayStatus();
                updateStatus();
            }
        }
    };

    private BroadcastReceiver appReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub

            final String action = intent.getAction();
            if (Intent.ACTION_PACKAGE_CHANGED.equals(action)
                    || Intent.ACTION_PACKAGE_REMOVED.equals(action)
                    || Intent.ACTION_PACKAGE_ADDED.equals(action)) {

                updateAppList(intent);
            }
        }
    };

    private BroadcastReceiver instabootReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();
            if ("com.droidlogic.instaboot.RELOAD_APP_COMPLETED".equals(action)) {
                Log.e(TAG,"reloadappcompleted");
                displayShortcuts();
            }
        }
    };

    public void startTvSettings() {
        Intent intent = new Intent();
        intent.setComponent(ComponentName.unflattenFromString(COMPONENT_TV_SETTINGS));
        startActivity(intent);
    }

    public void startTvApp() {
        Intent intent = new Intent();
        intent.setComponent(ComponentName.unflattenFromString(COMPONENT_TV_APP));
        startActivity(intent);
    }

    public void startCustomScreen(View view) {
        if (current_screen_mode == MODE_CUSTOM) return;
        mHoverView.clear();
        if (needPreviewFeture()) {
            hideTvViewForCustom();
        }
        saveModeBeforeCustom = current_screen_mode;
        mCustomView = new CustomView(this, view, current_screen_mode);
        current_screen_mode = MODE_CUSTOM;

        Rect rect = new Rect();
        view.getGlobalVisibleRect(rect);
        if (rect.top > getResources().getDisplayMetrics().heightPixels / 2) {
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            getRootView().addView(mCustomView, lp);
        } else {
            getRootView().addView(mCustomView);
        }

        getMainView().bringToFront();
    }

    public void recoverFromCustom() {
        mHandler.sendEmptyMessage(MSG_RECOVER_HOME);
        if (needPreviewFeture()) {
            recoverTvViewForCustom();
        }
        getMainView().setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        getMainView().requestFocus();
    }

    public static int pxToDip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static int dipToPx(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    private void setTvView() {
        TextView title_video = (TextView)findViewById(R.id.tx_video);
        title_video.setText(R.string.str_tvapp);

        tvView.setVisibility(View.VISIBLE);
        tvView.setCallback(new TvViewInputCallback());
        tvView.setZOrderMediaOverlay(false);

        setTvViewPosition(TV_MODE_NORMAL);
    }

    private void hideTvViewForCustom () {
        tvPrompt.setBackgroundDrawable(getResources().getDrawable(R.drawable.black));
        tvView.setVisibility(View.INVISIBLE);
    }

    private void recoverTvViewForCustom () {
        tvView.setVisibility(View.VISIBLE);
        if (isRadioChannel) {
            tvPrompt.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_radio));
        } else {
            tvPrompt.setBackgroundDrawable(null);
        }
        mTvHandler.sendEmptyMessage(TV_MSG_PLAY_TV);
    }

    public void setTvViewElevation(float elevation) {
        tvView.setElevation(elevation);
        tvPrompt.setElevation(elevation);
        tvPrompt.bringToFront();
    }

    public void setTvViewPosition(int mode) {
        int left = -1;
        int top = -1;
        int right = -1;
        int bottom = -1;
        int transY = 0;
        int duration = 0;

        tvViewMode = mode;
        switch (mode) {
            case TV_MODE_TOP:
                transY = -dipToPx(this, TV_WINDOW_BOTTOM_TOP);
            case TV_MODE_BOTTOM:
                left = dipToPx(this, TV_WINDOW_RIGHT_LEFT);
                top = dipToPx(this, TV_WINDOW_BOTTOM_TOP);
                right = left + dipToPx(this, TV_WINDOW_WIDTH);
                bottom = top + dipToPx(this, TV_WINDOW_HEIGHT);
                duration = 500;
                break;
            case TV_MODE_NORMAL:
            default:
                left = dipToPx(this, TV_WINDOW_NORMAL_LEFT);
                top = dipToPx(this, TV_WINDOW_NORMAL_TOP);
                right = left + dipToPx(this, TV_WINDOW_WIDTH);
                bottom = top + dipToPx(this, TV_WINDOW_HEIGHT);
                duration = 0;
                break;
        }
        HoverView.setViewPosition(tvView, new Rect(left, top, right, bottom));
        HoverView.setViewPosition(tvPrompt, new Rect(left, top, right, bottom));

        tvView.animate()
            .translationY(transY)
            .setDuration(duration)
            .start();
        tvPrompt.animate()
            .translationY(transY)
            .setDuration(duration)
            .start();
    }

    private boolean isBootvideoStopped() {
        return !TextUtils.equals(mSystemControlManager.getProperty("service.bootvideo"), "1")
                || TextUtils.equals(mSystemControlManager.getProperty("service.bootvideo.exit"), "1");
    }

    private void tuneTvView() {
        stopMusicPlayer();

        //float window don't need load PQ
        mSystemControlManager.setProperty(PROP_TV_PREVIEW, "true");

        mTvInputId = null;
        mChannelUri = null;
        mTvInputManager = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
        mTvInputChangeCallback = new TvInputChangeCallback();
        mTvInputManager.registerCallback(mTvInputChangeCallback, new Handler());
        setTvPrompt(TV_PROMPT_GOT_SIGNAL);

        int device_id, index_atv, index_dtv;
        device_id = Settings.System.getInt(getContentResolver(), DroidLogicTvUtils.TV_CURRENT_DEVICE_ID, 0);
        index_atv = Settings.System.getInt(getContentResolver(), DroidLogicTvUtils.TV_ATV_CHANNEL_INDEX, -1);
        index_dtv = Settings.System.getInt(getContentResolver(), DroidLogicTvUtils.TV_DTV_CHANNEL_INDEX, -1);
        isRadioChannel = Settings.System.getInt(getContentResolver(), DroidLogicTvUtils.TV_CURRENT_CHANNEL_IS_RADIO, 0) == 1 ? true : false;
        Log.d(TAG, "TV get device_id=" + device_id + " atv=" + index_atv +" dtv=" + index_dtv + " is_radio="+isRadioChannel);

        List<TvInputInfo> input_list = mTvInputManager.getTvInputList();
        for (TvInputInfo info : input_list) {
            if (parseDeviceId(info.getId()) == device_id) {
                mTvInputId = info.getId();
            }
        }

        mTvDataBaseManager = new TvDataBaseManager(this);

        if (TextUtils.isEmpty(mTvInputId)) {
            Log.d(TAG, "device" + device_id + " is not exist");
            setTvPrompt(TV_PROMPT_NO_DEVICE);
            return;
            //mTvInputId = DEFAULT_INPUT_ID;
            //mChannelUri = TvContract.buildChannelUri(-1);
        } else {
            if (device_id == DroidLogicTvUtils.DEVICE_ID_ATV) {
                ArrayList<ChannelInfo> channelList = mTvDataBaseManager.getChannelList(mTvInputId, Channels.SERVICE_TYPE_AUDIO_VIDEO, true);
                setChannelUri(channelList, index_atv);
            } else if (device_id == DroidLogicTvUtils.DEVICE_ID_DTV) {
                ArrayList<ChannelInfo> channelList;
                if (!isRadioChannel) {
                    channelList = mTvDataBaseManager.getChannelList(mTvInputId, Channels.SERVICE_TYPE_AUDIO_VIDEO, true);
                } else {
                    channelList = mTvDataBaseManager.getChannelList(mTvInputId, Channels.SERVICE_TYPE_AUDIO, true);
                }
                setChannelUri(channelList, index_dtv);
            } else {
                mChannelUri = TvContract.buildChannelUriForPassthroughInput(mTvInputId);
            }
        }

        Log.d(TAG, "TV play tune inputId=" + mTvInputId + " uri=" + mChannelUri);
        tvView.tune(mTvInputId, mChannelUri);

        if (mChannelObserver == null)
            mChannelObserver = new ChannelObserver();
        getContentResolver().registerContentObserver(Channels.CONTENT_URI, true, mChannelObserver);
    }

    private void releaseTvView() {
        tvView.setVisibility(View.GONE);
        //tvView.reset();
        if (mTvInputChangeCallback != null) {
            mTvInputManager.unregisterCallback(mTvInputChangeCallback);
            mTvInputChangeCallback = null;
        }
        if (mChannelObserver != null) {
            getContentResolver().unregisterContentObserver(mChannelObserver);
            mChannelObserver = null;
        }
    }

    private void setChannelUri (ArrayList<ChannelInfo> channelList, int index) {
        if (channelList.size() > 0) {

            if (index > 0) {
                for (ChannelInfo channelInfo : channelList) {
                    if (index == channelInfo.getNumber()) {
                        mChannelUri = channelInfo.getUri();
                        setTvPrompt(TV_PROMPT_GOT_SIGNAL);
                        return;
                    }
                }
            }

            ChannelInfo channel = channelList.get(0);
            mChannelUri = channel.getUri();
            Settings.System.putInt(getContentResolver(), DroidLogicTvUtils.TV_ATV_CHANNEL_INDEX, 1);
            Settings.System.putInt(getContentResolver(), DroidLogicTvUtils.TV_CURRENT_CHANNEL_IS_RADIO,
                    ChannelInfo.isRadioChannel(channel) ? 1 : 0);
            setTvPrompt(TV_PROMPT_GOT_SIGNAL);
        } else {
            mChannelUri = TvContract.buildChannelUri(-1);
        }
    }

    private void setTvPrompt(int mode) {
        switch (mode) {
            case TV_PROMPT_GOT_SIGNAL:
                tvPrompt.setText(null);
                if (isRadioChannel) {
                    tvPrompt.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_radio));
                }  else {
                    tvPrompt.setBackground(null);
                }
                break;
            case TV_PROMPT_NO_SIGNAL:
                tvPrompt.setText(getResources().getString(R.string.str_no_signal));
                if (isRadioChannel) {
                    tvPrompt.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_radio));
                }  else {
                    tvPrompt.setBackground(null);
                }
                break;
            case TV_PROMPT_IS_SCRAMBLED:
                tvPrompt.setText(getResources().getString(R.string.str_scrambeled));
                if (isRadioChannel) {
                    tvPrompt.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_radio));
                }  else {
                    tvPrompt.setBackground(null);
                }
                break;
            case TV_PROMPT_NO_DEVICE:
                tvPrompt.setText(null);
                tvPrompt.setBackgroundDrawable(getResources().getDrawable(R.drawable.hotplug_out));
                break;
        }
    }

    //stop the background music player
    public void stopMusicPlayer() {
        Intent intent = new Intent();
        intent.setAction ("com.android.music.musicservicecommand.pause");
        intent.putExtra ("command", "stop");
        sendBroadcast (intent);
    }

    private Handler mTvHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TV_MSG_PLAY_TV:
                    if (isBootvideoStopped()) {
                        Log.d(TAG, "======== bootvideo is stopped, start tv play");
                        tuneTvView();
                    } else {
                        Log.d(TAG, "======== bootvideo is not stopped, wait it");
                        mTvHandler.sendEmptyMessageDelayed(TV_MSG_PLAY_TV, 200);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    public class TvViewInputCallback extends TvView.TvInputCallback {
        @Override
        public void onEvent(String inputId, String eventType, Bundle eventArgs) {
            Log.d(TAG, "====onEvent==inputId =" + inputId +", ===eventType ="+ eventType);
            if (eventType.equals(DroidLogicTvUtils.AV_SIG_SCRAMBLED)) {
                setTvPrompt(TV_PROMPT_IS_SCRAMBLED);
            }
        }

        @Override
        public void onVideoAvailable(String inputId) {
            //tvView.invalidate();
            setTvPrompt(TV_PROMPT_GOT_SIGNAL);

            Log.d(TAG, "====onVideoAvailable==inputId =" + inputId);
        }

        @Override
        public void onConnectionFailed(String inputId) {
            Log.d(TAG, "====onConnectionFailed==inputId =" + inputId);
            new Thread( new Runnable() {
                public void run() {
                    try{
                        Thread.sleep(200);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    tvView.tune(mTvInputId, mChannelUri);
                }
            }).start();
        }

        @Override
        public void onVideoUnavailable(String inputId, int reason) {
            Log.d(TAG, "====onVideoUnavailable==inputId =" + inputId +", ===reason ="+ reason);
            switch (reason) {
                case TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN:
                case TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING:
                case TvInputManager.VIDEO_UNAVAILABLE_REASON_BUFFERING:
                    break;
                default:
                    break;
            }
            setTvPrompt(TV_PROMPT_NO_SIGNAL);
        }
    }

    private final class TvInputChangeCallback extends TvInputManager.TvInputCallback {
        @Override
        public void onInputAdded(String inputId) {
            Log.d(TAG, "==== onInputAdded, inputId=" + inputId + " curent inputid=" + mTvInputId);
            int device_id = Settings.System.getInt(getContentResolver(), DroidLogicTvUtils.TV_CURRENT_DEVICE_ID, 0);
            if (device_id == parseDeviceId(inputId)) {
                switch (device_id) {
                    case DroidLogicTvUtils.DEVICE_ID_AV1:
                    case DroidLogicTvUtils.DEVICE_ID_AV2:
                    case DroidLogicTvUtils.DEVICE_ID_HDMI1:
                    case DroidLogicTvUtils.DEVICE_ID_HDMI2:
                    case DroidLogicTvUtils.DEVICE_ID_HDMI3:
                        tvView.reset();
                        setTvPrompt(TV_PROMPT_GOT_SIGNAL);
                        mTvInputId = inputId;
                        mChannelUri = TvContract.buildChannelUriForPassthroughInput(mTvInputId);
                        tvView.tune(mTvInputId, mChannelUri);
                        break;
                }
            }
        }

        @Override
        public void onInputRemoved(String inputId) {
            Log.d(TAG, "==== onInputRemoved, inputId=" + inputId + " curent inputid=" + mTvInputId);
            if (TextUtils.equals(inputId, mTvInputId)) {
                Log.d(TAG, "==== current input device removed");
                mTvInputId = null;
                setTvPrompt(TV_PROMPT_NO_DEVICE);
                /*mTvInputId = DEFAULT_INPUT_ID;
                Settings.System.putInt(getContentResolver(), DroidLogicTvUtils.TV_CURRENT_DEVICE_ID, DroidLogicTvUtils.DEVICE_ID_ATV);

                ArrayList<ChannelInfo> channelList = mTvDataBaseManager.getChannelList(mTvInputId, Channels.SERVICE_TYPE_AUDIO_VIDEO, true);
                int index_atv = Settings.System.getInt(getContentResolver(), DroidLogicTvUtils.TV_ATV_CHANNEL_INDEX, -1);
                setChannelUri(channelList, index_atv);
                tvView.tune(mTvInputId, mChannelUri);*/
            }
        }
    }

    private final class ChannelObserver extends ContentObserver {
        public ChannelObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            Log.d(TAG, "detect channel changed =" + uri);
            if (DroidLogicTvUtils.matchsWhich(mChannelUri) == DroidLogicTvUtils.NO_MATCH) {
                ChannelInfo changedChannel = mTvDataBaseManager.getChannelInfo(uri);
                if (TextUtils.equals(changedChannel.getInputId(), mTvInputId)) {
                    Log.d(TAG, "current channel is null, so tune to a new channel");
                    mChannelUri = uri;
                    tvView.tune(mTvInputId, mChannelUri);
                }
            }
        }

        @Override
        public IContentObserver releaseContentObserver() {
            // TODO Auto-generated method stub
            return super.releaseContentObserver();
        }
    }

    private int parseDeviceId(String inputId) {
        String[] temp = inputId.split("/");
        if (temp.length == 3) {
            /*  ignore for HDMI CEC device */
            if (temp[2].contains("HDMI"))
                return -1;
            return Integer.parseInt(temp[2].substring(2));
        } else {
            return -1;
        }
    }
}

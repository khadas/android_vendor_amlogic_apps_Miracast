package com.droidlogic.mboxlauncher;

import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.graphics.Rect;

public class MyOnTouchListener implements OnTouchListener{
    private final static String TAG = "MyOnTouchListener";
        
    private int NUM_VIDEO = 0;
    private int NUM_RECOMMEND = 1;
    private int NUM_APP = 2;
    private int NUM_MUSIC = 3;
    private int NUM_LOCAL = 4;
        
    private Context mContext;
    private Object appPath;
    
    public MyOnTouchListener(Context context, Object path){
         mContext = context;
         appPath = path;
    }
    public boolean onTouch (View view, MotionEvent event)  {
        // TODO Auto-generated method stub
		Launcher.layoutScaleShadow.setVisibility(View.INVISIBLE);
		Launcher.frameView.setVisibility(View.INVISIBLE);
        Launcher.isInTouchMode = true;
        if (event.getAction() == MotionEvent.ACTION_UP) {
            ImageView img = (ImageView)((ViewGroup)view).getChildAt(0);
            String path  = img.getResources().getResourceName(img.getId()); 
            String vName = path.substring(path.indexOf("/")+1);

           // Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ ACTION_UP");

            // Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@ viewname=" + vName);
            if (vName.equals("img_setting")){
                Intent intent = new Intent();
                intent .setComponent(new ComponentName("com.droidlogic.mboxsettings", "com.driodlogic.mboxsettings.SettingsMboxActivity"));     
				mContext.startActivity(intent);
            } else if (vName.equals("img_video")){
                showMenuView(NUM_VIDEO, view);
            }else if (vName.equals("img_recommend")){
                showMenuView(NUM_RECOMMEND, view);
            }else if (vName.equals("img_app")){
                showMenuView(NUM_APP, view);
            }else if (vName.equals("img_music")){
                showMenuView(NUM_MUSIC, view);
            }else if (vName.equals("img_local")){
                showMenuView(NUM_LOCAL, view);
            }else {
                if (appPath != null){                                      
                    mContext.startActivity((Intent)appPath);
                } 
            }                 
        }
        else if (event.getAction() == MotionEvent.ACTION_DOWN){
            ImageView img = (ImageView)((ViewGroup)view).getChildAt(0);
            String path  = img.getResources().getResourceName(img.getId()); 
            String vName = path.substring(path.indexOf("/")+1);   

            if (vName.equals("img_video") || vName.equals("img_recommend") || vName.equals("img_app") ||
                     vName.equals("img_music") ||  vName.equals("img_local")){
                return view.onTouchEvent(event);
            }
        }

        return false;
    }   

    private void showMenuView(int num, View view){
         Launcher.saveHomeFocusView = view;
         Launcher.isShowHomePage = false;
         Launcher.layoutScaleShadow.setVisibility(View.INVISIBLE);
         Launcher.frameView.setVisibility(View.INVISIBLE);
          
         Rect rect = new Rect();
         view.getGlobalVisibleRect(rect);
        // ScaleAnimation scaleAnimationIn = new ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f, getPiovtX(rect), getPiovtY(rect));
        // ScaleAnimation scaleAnimationOut = new ScaleAnimation(1.0f, 0.0f, 1.0f, 0.0f, getPiovtX(rect), getPiovtY(rect));
         //scaleAnimationIn.setDuration(400);
        // scaleAnimationOut.setDuration(400);
         Launcher.viewMenu.setInAnimation(null);
         Launcher.viewMenu.setOutAnimation(null);
         
         Launcher.viewHomePage.setVisibility(View.GONE);
         Launcher.viewMenu.setVisibility(View.VISIBLE);
         Launcher.viewMenu.setDisplayedChild(num);
         Launcher.viewMenu.setFocusableInTouchMode(true);
    }
    
}




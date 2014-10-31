package com.amlogic.mediaboxlauncher;

import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnKeyListener;
import android.view.KeyEvent;
import android.view.FocusFinder;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.util.Log;

import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.Animation.AnimationListener;
import android.graphics.Rect;

public class MyOnKeyListener implements OnKeyListener{
    private final static String TAG = "MyOnKeyListener";
        
    private int NUM_VIDEO = 0;
    private int NUM_RECOMMEND = 1;
    private int NUM_APP = 2;
    private int NUM_MUSIC = 3;
    private int NUM_LOCAL = 4;
        
    private Context mContext;
    private Object appPath;
    
    public MyOnKeyListener(Context context, Object path){
         mContext = context;
         appPath = path;
    }
    public boolean onKey(View view, int keyCode, KeyEvent event)  {
        if (Launcher.animIsRun)
            return true;
        
        if(keyCode!= KeyEvent.KEYCODE_BACK)
            Launcher.isInTouchMode = false;      
       // Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@ KeyEvent=" + keyCode);
        
        if(event.getAction() == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                                                           keyCode == KeyEvent.KEYCODE_ENTER )){
            //Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@ key view=" + view);

                ImageView img = (ImageView)((ViewGroup)view).getChildAt(0);
                String path  = img.getResources().getResourceName(img.getId()); 
                String vName = path.substring(path.indexOf("/")+1);

               // Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@ viewname=" + vName);
                if (vName.equals("img_setting")){
                    Launcher.saveHomeFocusView = view;
                    Intent intent = new Intent();
                    intent .setComponent(new ComponentName("com.mbx.settingsmbox", "com.mbx.settingsmbox.SettingsMboxActivity"));     
				    mContext.startActivity(intent);
                    Launcher.IntoApps = true;
                    return true;
                } else if (vName.equals("img_video")){
                    showMenuView(NUM_VIDEO, view);
                    return true;
                }else if (vName.equals("img_recommend")){
                    showMenuView(NUM_RECOMMEND, view);
                    return true;
                }else if (vName.equals("img_app")){
                    showMenuView(NUM_APP, view);
                    return true;
                }else if (vName.equals("img_music")){
                    showMenuView(NUM_MUSIC, view);
                    return true;
                }else if (vName.equals("img_local")){
                    showMenuView(NUM_LOCAL, view);
                    return true;
                }else {
                    if (appPath != null){  
                        if (Launcher.isShowHomePage){
                            Launcher.saveHomeFocusView = view;
                        }
                        mContext.startActivity((Intent)appPath);
                        Launcher.IntoApps = true;
                    }
                }                 
        }else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && !Launcher.isShowHomePage){; 

            if (checkNextFocusedIsNull(view, View.FOCUS_LEFT)){
                Launcher.accessBoundaryCount = 0;
                Animation animIn = AnimationUtils.loadAnimation(mContext, R.anim.push_left_in);
                Animation animOut = AnimationUtils.loadAnimation(mContext, R.anim.push_left_out);
                animIn.setAnimationListener(new MyAnimationListener(0));
                animOut.setAnimationListener(new MyAnimationListener(1));
                Launcher.viewMenu.setInAnimation(animIn);
                Launcher.viewMenu.setOutAnimation(animOut);
                Launcher.viewMenu.showPrevious();
                return true;
            }    
            
        }else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && !Launcher.isShowHomePage){

            if(checkNextFocusedIsNull(view,View.FOCUS_RIGHT)){
                Launcher.accessBoundaryCount = 0;  
                Animation animIn = AnimationUtils.loadAnimation(mContext, R.anim.push_right_in);
                Animation animOut = AnimationUtils.loadAnimation(mContext, R.anim.push_right_out);
                animIn.setAnimationListener(new MyAnimationListener(2));
                animOut.setAnimationListener(new MyAnimationListener(3));
                Launcher.viewMenu.setInAnimation(animIn);
                Launcher.viewMenu.setOutAnimation(animOut);
                Launcher.viewMenu.showNext();   
                return true;
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
         ScaleAnimation scaleAnimationIn = new ScaleAnimation(1.0f, 1.0f, 1.0f, 1.0f, getPiovtX(rect), getPiovtY(rect));
         //ScaleAnimation scaleAnimationOut = new ScaleAnimation(1.0f, 0.0f, 1.0f, 0.0f, getPiovtX(rect), getPiovtY(rect));
         scaleAnimationIn.setDuration(200);
         //scaleAnimationOut.setDuration(400);
         scaleAnimationIn.setAnimationListener(new MyAnimationListener(5));
         Launcher.viewMenu.setInAnimation(scaleAnimationIn);
         Launcher.viewMenu.setOutAnimation(null);

         Launcher.viewHomePage.setVisibility(View.GONE);
         Launcher.viewMenu.setVisibility(View.VISIBLE);
         Launcher.viewMenu.setDisplayedChild(num);
         Launcher.viewMenu.getChildAt(num).requestFocus();
    }

    private float getPiovtX(Rect rect){
        return (float)((rect.left + rect.width() / 2));
    }

    private float getPiovtY(Rect rect){
        return (float)((rect.top + rect.height() / 2));
    }
    
    private boolean checkNextFocusedIsNull(View view , int dec){
         ViewGroup gridLayout = (ViewGroup)view.getParent();
         if(FocusFinder.getInstance().findNextFocus(gridLayout, gridLayout.findFocus(),dec) == null){
             Launcher.accessBoundaryCount++;   
         } else {
            Launcher.accessBoundaryCount = 0;
         }

         if (Launcher.accessBoundaryCount <= 1)
            return false;
         else {
            Launcher.dontRunAnim = true;
            Launcher.animIsRun = true;
            Launcher.layoutScaleShadow.setVisibility(View.INVISIBLE);
            Launcher.frameView.setVisibility(View.INVISIBLE);
            return true;
         }
    }

    private class MyAnimationListener implements AnimationListener { 
        private int in_or_out;        
        public MyAnimationListener(int flag) { 
            in_or_out = flag; 
        } 
     
        @Override 
        public void onAnimationStart(Animation animation) { 
            Launcher.animIsRun = true;
            Launcher.layoutScaleShadow.setVisibility(View.INVISIBLE);
            Launcher.frameView.setVisibility(View.INVISIBLE);
        } 
     
        @Override 
        public void onAnimationEnd(Animation animation) { 
           // Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ flag="+ in_or_out);
            if (in_or_out == 1){    
                if (((ViewGroup)Launcher.viewMenu.getCurrentView()).getChildAt(4) instanceof MyScrollView){
                    ViewGroup findGridLayout = ((ViewGroup)((ViewGroup)((ViewGroup)Launcher.viewMenu.getCurrentView()).getChildAt(4)).getChildAt(0));
                    int count = findGridLayout.getChildCount()<6 ? findGridLayout.getChildCount()-1 : 5;
                    Launcher.dontRunAnim = true;
                    findGridLayout.getChildAt(count).requestFocus();   
                    Launcher.dontRunAnim = false;
                }
                //Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ count="+ ((ViewGroup)((ViewGroup)((ViewGroup)Launcher.viewMenu.getCurrentView()).getChildAt(4)).getChildAt(0)).getChildCount());    
            } else if(in_or_out == 3){
               // Launcher.dontDrawFocus = false;  
                Launcher.dontRunAnim = true;
                Launcher.viewMenu.clearFocus();
                Launcher.dontRunAnim = true;
                Launcher.viewMenu.requestFocus();
                Launcher.dontRunAnim = false;
            } else if(in_or_out == 5){
                Launcher.dontRunAnim = true;
                Launcher.viewMenu.clearFocus();
                Launcher.dontRunAnim = true;
                Launcher.viewMenu.requestFocus();   
                Launcher.dontRunAnim = false;
            }
            Launcher.animIsRun = false;
            Launcher.layoutScaleShadow.setVisibility(View.VISIBLE);
            Launcher.frameView.setVisibility(View.VISIBLE);
        } 
     
        @Override 
        public void onAnimationRepeat(Animation animation) {     
        } 
     
    } 

}




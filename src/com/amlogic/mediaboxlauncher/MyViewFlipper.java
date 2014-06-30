/*-------------------------------------------------------------------------
    
-------------------------------------------------------------------------*/
package com.amlogic.mediaboxlauncher;

import android.content.Context;
import android.content.ComponentName;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.ViewFlipper;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.graphics.drawable.Drawable;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;
import android.util.AttributeSet;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class MyViewFlipper extends ViewFlipper{
    private final static String TAG="MyViewFlipper";
    private Context mContext;

    private MyGridLayout menu_video = null;
    private MyGridLayout menu_recommend = null;
    private MyGridLayout menu_app = null;
    private MyGridLayout menu_music = null;
    private MyGridLayout menu_local = null;

    public MyViewFlipper(Context context){
        super(context); 
    }
    
    public MyViewFlipper(Context context, AttributeSet attrs){
        super(context, attrs); 
        mContext = context;
    }
    
    public void onDraw(Canvas canvas) {  
       // TODO Auto-generated method stub  
       super.onDraw(canvas);  
  
    }  

    @Override
    public boolean onTouchEvent (MotionEvent event){
        //Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ touch ="+ this);
        return false;       
    }
}

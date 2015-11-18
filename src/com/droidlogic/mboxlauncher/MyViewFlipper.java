/*-------------------------------------------------------------------------
    
-------------------------------------------------------------------------*/
package com.droidlogic.mboxlauncher;

import android.content.Context;
import android.widget.ViewFlipper;
import android.view.MotionEvent;
import android.graphics.Canvas;
import android.util.AttributeSet;

public class MyViewFlipper extends ViewFlipper{

    public MyViewFlipper(Context context){
        super(context); 
    }
    
    public MyViewFlipper(Context context, AttributeSet attrs){
        super(context, attrs); 
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

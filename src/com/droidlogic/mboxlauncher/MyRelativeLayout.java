/*-------------------------------------------------------------------------

  -------------------------------------------------------------------------*/
package com.droidlogic.mboxlauncher;

import android.content.Context;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.Animation.AnimationListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.util.Log;

import java.lang.Character;


public class MyRelativeLayout extends RelativeLayout{
    private final static String TAG="MyRelativeLayout";

    private Context mContext = null;
    private static Rect imgRect;
    private float scalePara = 1.1f;
    private float shortcutScalePara = 1.1f;
    private float framePara = 1.09f;
    private int animDuration;
    private int animDelay = 0;
    private final int MODE_HOME_RECT = 0;
    private final int MODE_HOME_SHORTCUT = 1;
    private final int MODE_CHILD_SHORTCUT = 2;

    public MyRelativeLayout(Context context){
        super(context);
    }

    public MyRelativeLayout(Context context, AttributeSet attrs){
        super(context, attrs);
        mContext = context;
        if (Launcher.isRealOutputMode)
            animDuration = 70;
        else
            animDuration = 90;
    }

    public MyRelativeLayout(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
    }

    @Override
        public void onDraw(Canvas canvas) {
            // TODO Auto-generated method stub
            super.onDraw(canvas);
        }

    @Override
        public boolean onTouchEvent (MotionEvent event){
            setAddShortcutHead();
            setNumberOfScreen();
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                //Launcher.startX = -1f;

                // setSurface();
                /*
                if (this.getChildAt(0) instanceof ImageView) {
                    ImageView img = (ImageView)this.getChildAt(0);
                    if (img != null && img.getDrawable() != null &&
                            img.getContentDescription() != null && img.getContentDescription().equals("img_add")) {
                        Launcher.isAddButtonBeTouched = true;
                        Launcher.pressedAddButton = this;
                    }
                }*/
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                return false;
            }
            return true;
        }

    @Override
        protected void onFocusChanged (boolean gainFocus, int direction, Rect previouslyFocusedRect){
            setAddShortcutHead();

            if (gainFocus == true && !Launcher.isInTouchMode && !Launcher.dontDrawFocus) {
                setNumberOfScreen();

                if (Launcher.prevFocusedView != null && (isParentSame(this,Launcher.prevFocusedView)
                            || Launcher.isShowHomePage)) {
                    if (!Launcher.dontRunAnim && !Launcher.IntoCustomActivity) {
                        Launcher.layoutScaleShadow.setVisibility(View.INVISIBLE);

                        Rect preRect = new Rect();
                        Launcher.prevFocusedView.getGlobalVisibleRect(preRect);

                        setShadowEffect();
                        startFrameAnim(preRect);
                    } else if (!(Launcher.IntoCustomActivity && Launcher.isShowHomePage && Launcher.ifChangedShortcut)) {
                        Launcher.dontRunAnim = false;
                        setSurface();
                    }
                } else{
                    if (Launcher.isShowHomePage || Launcher.dontRunAnim || Launcher.IntoCustomActivity) {
                        Launcher.IntoCustomActivity = false;
                        setSurface();
                    }
                }
            }
            else if (!Launcher.isInTouchMode) {
                Launcher.prevFocusedView = this;
                if (!Launcher.dontRunAnim) {
                    ScaleAnimation anim = new ScaleAnimation(1.07f, 1f, 1.07f, 1f,Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                    anim.setZAdjustment(Animation.ZORDER_TOP);
                    anim.setDuration(animDuration);
                    anim.setStartTime(animDelay);
                    if (!(this.getParent() instanceof MyGridLayout)) {
                        this.bringToFront();
                        ((View)this.getParent()).bringToFront();
                        Launcher.viewHomePage.bringToFront();
                        if (((Launcher)mContext).needPreviewFeture())
                            ((Launcher)mContext).setTvViewFront();
                    }
                    this.startAnimation(anim);
                }
            }
        }

    private void setAddShortcutHead(){
        View parent = (View)this.getParent();
        if (parent == (View)Launcher.videoShortcutView) {
            Launcher.current_shortcutHead = CustomAppsActivity.VIDEO_SHORTCUT_HEAD;
        } else if (parent == (View)Launcher.recommendShortcutView) {
            // Launcher.current_shortcutHead = CustomAppsActivity.RECOMMEND_SHORTCUT_HEAD;
        } else if (parent == (View)Launcher.musicShortcutView) {
            Launcher.current_shortcutHead = CustomAppsActivity.MUSIC_SHORTCUT_HEAD;
        } else if (parent == (View)Launcher.localShortcutView) {
            Launcher.current_shortcutHead = CustomAppsActivity.LOCAL_SHORTCUT_HEAD;
        } else {
            Launcher.current_shortcutHead = CustomAppsActivity.HOME_SHORTCUT_HEAD;
        }
    }

    private void setNumberOfScreen(){
        if (this.getParent() instanceof MyGridLayout) {
            MyGridLayout parent = (MyGridLayout)this.getParent();
            if (parent == Launcher.videoShortcutView) {
                Launcher.tx_video_count.setText(Integer.toString(Launcher.videoShortcutView.indexOfChild(this)+1));
            } else if (parent == Launcher.recommendShortcutView) {
                Launcher.tx_recommend_count.setText(Integer.toString(Launcher.recommendShortcutView.indexOfChild(this)+1));
            }else if (parent == Launcher.appShortcutView) {
                Launcher.tx_app_count.setText(Integer.toString(Launcher.appShortcutView.indexOfChild(this)+1));
            }else if (parent == Launcher.musicShortcutView) {
                Launcher.tx_music_count.setText(Integer.toString(Launcher.musicShortcutView.indexOfChild(this)+1));
            }else if (parent == Launcher.localShortcutView) {
                Launcher.tx_local_count.setText(Integer.toString(Launcher.localShortcutView.indexOfChild(this)+1));
            }
        }
    }

    public class TransAnimationListener implements AnimationListener {
        private Animation scaleAnim;
        private ViewGroup mView;

        public TransAnimationListener(Context context, ViewGroup view, Animation anim) {
            scaleAnim = anim;
            mView = view;
        }

        @Override
            public void onAnimationStart(Animation animation) {
            }

        @Override
            public void onAnimationEnd(Animation animation) {
                scaleAnim.reset();
                if (!Launcher.animIsRun) {
                    Launcher.layoutScaleShadow.setVisibility(View.VISIBLE);
                    //Launcher.frameView.setVisibility(View.VISIBLE);
                }
            }

        @Override
            public void onAnimationRepeat(Animation animation) {
            }
    }

    public class ScaleAnimationListener implements AnimationListener {
        @Override
            public void onAnimationStart(Animation animation) {
            }
        @Override
            public void onAnimationEnd(Animation animation) {
                if (!Launcher.animIsRun) {
                    Launcher.layoutScaleShadow.setVisibility(View.VISIBLE);
                    // Launcher.frameView.setVisibility(View.VISIBLE);
                }
            }
        @Override
            public void onAnimationRepeat(Animation animation) {
            }
    }

    private void startFrameAnim(Rect preRect){
        imgRect = new Rect();
        this.getGlobalVisibleRect(imgRect);
        //setTransFramePosition(preRect);

        /*AnimationSet animationSet = new AnimationSet(true);
          TranslateAnimation translateAnimation = new TranslateAnimation(0.0f, imgRect.left-preRect.left,0.0f, imgRect.top-preRect.top);
          translateAnimation.setDuration(animDuration);
          ScaleAnimation scaleAnimation = new ScaleAnimation(1f, (float)(imgRect.right-imgRect.left)/(float)(preRect.right-preRect.left),
          1f, (float)(imgRect.bottom-imgRect.top)/(float)(preRect.bottom-preRect.top));
        //   Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f );
        scaleAnimation.setDuration(animDuration);
        scaleAnimation.setStartTime(animDelay);
        animationSet.addAnimation(scaleAnimation);
        animationSet.addAnimation(translateAnimation);
        translateAnimation.setAnimationListener(new TransAnimationListener(mContext, this, scaleAnimation));
         */

        ScaleAnimation shadowAnim = new ScaleAnimation(0.95f, 1f, 0.95f, 1f,Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f );
        shadowAnim.setDuration(animDuration);
        shadowAnim.setStartTime(animDelay);
        shadowAnim.setAnimationListener(new ScaleAnimationListener());

        Launcher.layoutScaleShadow.startAnimation(shadowAnim);
        //Launcher.trans_frameView.startAnimation(animationSet);
    }

    public void setSurface(){
        setShadowEffect();
        if (!Launcher.animIsRun) {
            //Launcher.frameView.setVisibility(View.VISIBLE);
            Launcher.layoutScaleShadow.setVisibility(View.VISIBLE);
        }
    }

    public void setShadowEffect(){
        float bgScalePara;
        Rect layoutRect;
        Bitmap scaleBitmap;
        Bitmap shadowBitmap;
        ViewGroup mView = this;
        ImageView scaleImage;
        TextView scaleText;
        int screen_mode;
        String text = null;

        imgRect = new Rect();
        mView.getGlobalVisibleRect(imgRect);
        //setFramePosition(imgRect);

        //Launcher.trans_frameView.bringToFront();
        Launcher.layoutScaleShadow.bringToFront();
        // Launcher.frameView.bringToFront();

        if (((Launcher)mContext).needPreviewFeture()) {
            if (mView.getId() == R.id.layout_video)
                ((Launcher)mContext).setTvViewFront();

            if (!Launcher.isShowHomePage) {
                if (((Launcher)mContext).tvViewMode != Launcher.TV_MODE_TOP
                    && (imgRect.top + imgRect.bottom) / 2 >  ((Launcher)mContext).dipToPx(360))
                    ((Launcher)mContext).setTvViewPosition(Launcher.TV_MODE_TOP);
                else if (((Launcher)mContext).tvViewMode != Launcher.TV_MODE_BOTTOM
                    && (imgRect.top + imgRect.bottom) / 2 <= ((Launcher)mContext).dipToPx(360))
                    ((Launcher)mContext).setTvViewPosition(Launcher.TV_MODE_BOTTOM);
            }
        }

        screen_mode = getScreenMode(mView);

        scaleImage = (ImageView)Launcher.layoutScaleShadow.findViewById(R.id.img_focus_unit);
        scaleText = (TextView)Launcher.layoutScaleShadow.findViewById(R.id.tx_focus_unit);

        if (screen_mode == MODE_HOME_SHORTCUT) {
            bgScalePara = shortcutScalePara;
        } else {
            bgScalePara = scalePara;
        }

        ImageView img = (ImageView)(mView.getChildAt(0));
        img.buildDrawingCache();
        Bitmap bmp = img.getDrawingCache();
        if (bmp == null) {
            Launcher.cantGetDrawingCache = true;
            return;
        } else {
            Launcher.cantGetDrawingCache = false;
        }

        scaleBitmap = zoomBitmap(bmp, (int)(imgRect.width()*bgScalePara),(int)(imgRect.height()*bgScalePara));
        img.destroyDrawingCache();

        if (mView.getChildAt(1) instanceof TextView) {
            text = ((TextView)mView.getChildAt(1)).getText().toString();
        }

        shadowBitmap = BitmapFactory.decodeResource(mContext.getResources(), getShadow(mView.getChildAt(0), screen_mode));
        int layout_width = (shadowBitmap.getWidth() - imgRect.width()) / 2;
        int layout_height = (shadowBitmap.getHeight() - imgRect.height()) / 2;
        layoutRect = new Rect(imgRect.left-layout_width, imgRect.top-layout_height, imgRect.right+layout_width, imgRect.bottom+layout_height);

        Launcher.layoutScaleShadow.setBackgroundResource(0); //clear background
        if (screen_mode == MODE_HOME_RECT) {
            scaleImage.setImageResource(getFocusImage(mView.getChildAt(0)));
        } else {
            scaleImage.setImageBitmap(scaleBitmap);
        }

        if (text != null) {
            //setTextWidth(scaleText, scaleBitmap.getWidth());
            setTextMarginAndSize(scaleText, screen_mode);
            scaleText.setText(text);
        } else {
            scaleText.setText(null);
        }

        if (screen_mode != MODE_HOME_RECT) {
            Launcher.layoutScaleShadow.setBackgroundResource(getShadow(mView.getChildAt(0), screen_mode));
        }
        setViewPosition(Launcher.layoutScaleShadow, layoutRect);
    }

    private void setTextMarginAndSize(TextView text, int screen_mode){
        android.widget.RelativeLayout.LayoutParams para;
        para = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        text.setTextColor(mContext.getResources().getInteger(R.color.btn_text_color));
        if (screen_mode == MODE_HOME_RECT) {
            if (Launcher.REAL_OUTPUT_MODE.equals("4k2knative")) {
                para.setMargins(200, 170, 0, 0);
            } else if (Launcher.REAL_OUTPUT_MODE.equals("720p"))
                para.setMargins(66, 56, 0, 0);
            else
                para.setMargins(100, 85, 0, 0);
            text.setLayoutParams(para);
            text.setTextSize(28);
        } else {
            para.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            para.addRule(RelativeLayout.CENTER_HORIZONTAL);
            if (Launcher.REAL_OUTPUT_MODE.equals("4k2knative")) {
                para.setMargins(130, 0, 130, 100);
            }else if (Launcher.REAL_OUTPUT_MODE.equals("720p"))
                para.setMargins(44, 0, 44, 34);
            else
                para.setMargins(65, 0, 65, 50);
            text.setLayoutParams(para);
            text.setTextSize(26);
        }
    }

    private void setTransFramePosition(Rect rect){
        android.widget.AbsoluteLayout.LayoutParams lp = new android.widget.AbsoluteLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0, 0);
        int rectWidth = rect.right - rect.left;
        int rectHeight = rect.bottom - rect.top;

        lp.width = (int)(rectWidth* framePara);
        lp.height = (int)(rectHeight * framePara);
        lp.x = rect.left + (int)((rectWidth - lp.width)/2);
        lp.y = rect.top + (int)((rectHeight - lp.height)/2);
        Launcher.trans_frameView.setLayoutParams(lp);
    }

    private void setFramePosition(Rect rect){
        android.widget.AbsoluteLayout.LayoutParams lp = new android.widget.AbsoluteLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0, 0);
        int rectWidth = rect.right - rect.left;
        int rectHeight = rect.bottom - rect.top;

        lp.width = (int)(rectWidth* framePara);
        lp.height = (int)(rectHeight * framePara);
        lp.x = rect.left + (int)((rectWidth - lp.width)/2);
        lp.y = rect.top + (int)((rectHeight - lp.height)/2);
        //Launcher.frameView.setLayoutParams(lp);
    }

    public static void setViewPosition(View view, Rect rect){
        android.widget.AbsoluteLayout.LayoutParams lp = new android.widget.AbsoluteLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0, 0);

        lp.width = rect.width();
        lp.height = rect.height();
        lp.x = rect.left;
        lp.y = rect.top;
        view.setLayoutParams(lp);
    }

    private int getScreenMode(ViewGroup view){
        View img = view.getChildAt(0);
        View tx = view.getChildAt(1);
        String path  = img.getResources().getResourceName(img.getId());
        String vName = path.substring(path.indexOf("/")+1);

        if (vName.equals("img_recommend") || vName.equals("img_video") || vName.equals("img_setting")
                || vName.equals("img_app") || vName.equals("img_music") || vName.equals("img_local")) {
            return MODE_HOME_RECT;
        } else if (tx != null) {
            framePara = 1.06f;
            return MODE_CHILD_SHORTCUT;
        } else {
            return MODE_HOME_SHORTCUT;
        }
    }

    private boolean isParentSame(View view1, View view2){
        if (((ViewGroup)view1.getParent()).indexOfChild(view2) == -1) {
            return false;
        } else {
            return true;
        }
    }

    private int getFocusImage(View img){
        String path  = img.getResources().getResourceName(img.getId());
        String vName = path.substring(path.indexOf("/")+1);
        if (vName.equals("img_recommend")) {
            return R.drawable.focus_recommend;
        } else if(vName.equals("img_video")) {
            return R.drawable.focus_video;
        } else if(vName.equals("img_setting")) {
            return R.drawable.focus_setting;
        } else if(vName.equals("img_app")) {
            return R.drawable.focus_app;
        } else if(vName.equals("img_music")) {
            return R.drawable.focus_music;
        } else if(vName.equals("img_local")) {
            return R.drawable.focus_local;
        } else {
            return -1;
        }
    }
    private int getShadow(View img, int mode){
        String path  = img.getResources().getResourceName(img.getId());
        String vName = path.substring(path.indexOf("/")+1);
        // Log.d(" MyRelativeLayout", "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ child 1="+ vName);

        if (vName.equals("img_recommend")) {
            return R.drawable.focus_recommend;
        } else if(vName.equals("img_video")) {
            return R.drawable.focus_video;
        } else if(vName.equals("img_setting")) {
            return R.drawable.focus_setting;
        } else if(vName.equals("img_app")) {
            return R.drawable.focus_app;
        } else if(vName.equals("img_music")) {
            return R.drawable.focus_music;
        } else if(vName.equals("img_local")) {
            return R.drawable.focus_local;
        } else if (mode == MODE_CHILD_SHORTCUT) {
            return R.drawable.shadow_child_shortcut;
        } else if (mode == MODE_HOME_SHORTCUT) {
            return R.drawable.shadow_shortcut;
        }  else {
            return -1;
        }
    }
    public int getStringLength(String s)
    {
        int length = 0;
        for(int i = 0; i < s.length(); i++)
        {
            int ascii = Character.codePointAt(s, i);
            if (ascii >= 0 && ascii <=255)
                length++;
            else
                length += 2;

        }
        return length;

    }

    public Bitmap zoomBitmap(Bitmap bitmap, int w, int h) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Matrix matrix = new Matrix();
        float scaleWidht = ((float) w / width);
        float scaleHeight = ((float) h / height);
        matrix.postScale(scaleWidht, scaleHeight);
        Bitmap newbmp = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        return newbmp;
    }
}

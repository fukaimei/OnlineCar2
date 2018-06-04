package com.fukaimei.onlinecar.BicycleSharing.util;

import android.content.Context;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.fukaimei.onlinecar.R;

/**
 * Created by 傅开煤 on 2017/5/28.
 */

public class Utils {
    /**
     * 在屏幕中央显示一个Toast
     * @param text
     */
    public static void showToast(Context context, CharSequence text) {
        Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    //带有图片的Toast
    public static void showToast2(Context context, CharSequence text) {

        Toast toast=Toast.makeText(context, text, Toast.LENGTH_LONG);
        LinearLayout toast_layout=(LinearLayout) toast.getView();
        ImageView iv=new ImageView(context);
        iv.setImageResource(R.drawable.toast_loca);
        toast_layout.addView(iv,0);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();

    }

}
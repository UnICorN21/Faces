package com.unicorn.faces.app.views.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.unicorn.faces.app.R;
import com.unicorn.faces.app.Util;


/**
 * Created by Super on 2015/5/19.
 */
public class ImageShowActivity extends Activity {
    private String imgPath;
    private ImageView imageView;

    public static int faceDirection=0;

    private Handler handler;
    private Runnable runnable;

    private ProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_showimg);

        handler=new Handler();

        runnable=new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap= Util.getBitmapByPath(imgPath,faceDirection);
                if(bitmap!=null){
                    bitmap= Util.rotateBitmap(bitmap,-90,faceDirection);
                    imageView.setImageBitmap(bitmap);
                    progressBar.setVisibility(View.GONE);
                    handler.removeCallbacks(runnable);
                }
                handler.postDelayed(this,500);
            }
        };

        imageView= (ImageView) findViewById(R.id.view_showImg_image);

        Intent intent =getIntent();
        imgPath=intent.getStringExtra("imgPath");
        faceDirection=intent.getIntExtra("faceDirection",1);

        imageView.setBackgroundColor(Color.WHITE);

        progressBar= (ProgressBar) findViewById(R.id.view_progressBar);

        handler.postDelayed(runnable,500);
    }

    @Override
    public void onBackPressed() {
        handler.removeCallbacks(runnable);
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(runnable);
        super.onDestroy();

    }
}

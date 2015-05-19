package com.unicorn.faces.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

import java.io.File;

/**
 * Created by Super on 2015/5/19.
 * some common methods are difined as static and be implemented here
 */
public class Util {

    //get bitmap by the image path
    public static Bitmap getBitmapByPath(String path,int faceDirection){

        Bitmap bitmap=null;
        File file=new File(path);
        if(file.exists()){
            /*bitmap= rotateBitmap(BitmapFactory.decodeFile(path),-90,faceDirection) ;*/
            bitmap=BitmapFactory.decodeFile(path);
        }
        return bitmap;
    }

    //rotate the bitmap to a right direction
    public static Bitmap rotateBitmap(Bitmap bitmap,float rotateDegree,int faceDirection){
        if(faceDirection==0) rotateDegree=-rotateDegree;
        Matrix rotateMatrix=new Matrix();
        rotateMatrix.setRotate(rotateDegree,bitmap.getWidth()/2,bitmap.getHeight()/2);
        Bitmap goalBitmap=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),rotateMatrix,true);
        return goalBitmap;
    }

    public static Bitmap scaleBitmap(Bitmap bitmap,float goalWidth,float goalHeight){
        int oriWidth=bitmap.getWidth();
        int oriHeight=bitmap.getHeight();
        float scaleWidth=goalWidth/oriWidth;
        float scaleHeight=goalHeight/oriHeight;

        float  scaleDegree=(scaleWidth>=scaleHeight)?scaleWidth:scaleHeight;

        Matrix scaleMatrix=new Matrix();
        scaleMatrix.postScale(scaleDegree,scaleDegree);

        Bitmap goalBitmap=Bitmap.createBitmap(bitmap,0,0,oriWidth,oriHeight,scaleMatrix,true);

        goalBitmap=toRoundBitmap(goalBitmap);

        return goalBitmap;
    }

    public static Bitmap toRoundBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float roundPx;
        float left,right,bottom,dst_right,dst_bottom;
        if (width <= height) {
            roundPx = width / 2;
            bottom = width;
            left = 0;
            right = width;
            height = width;
            dst_right = width;
            dst_bottom = width;
        } else {
            roundPx = height / 2;
            float clip = (width - height) / 2;
            left = clip;
            right = width - clip;
            bottom = height;
            width = height;
            dst_right = height;
            dst_bottom = height;
        }


        Bitmap output = Bitmap.createBitmap(width,
                height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect src = new Rect((int)left, 0, (int)right, (int)bottom);
        final Rect dst = new Rect(0, 0, (int)dst_right, (int)dst_bottom);
        final RectF rectF = new RectF(dst);

        paint.setAntiAlias(true);

        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, src, dst, paint);
        return output;
    }
}

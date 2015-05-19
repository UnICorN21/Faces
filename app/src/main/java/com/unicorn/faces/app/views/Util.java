package com.unicorn.faces.app.views;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

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

        return goalBitmap;


    }
}

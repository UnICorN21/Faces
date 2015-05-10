//
//  FaceDetector.cpp
//  libfaces
//
//  Created by Huxley on 5/9/15.
//  Copyright (c) 2015 Huxley. All rights reserved.
//

#include <jni.h>
#include <stdio.h>
#include <iostream>
#include <string>
#include <vector>

#include <opencv2/objdetect.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/core/utility.hpp>

#include "com_unicorn_faces_app_natives_FaceDetector.h"

static cv::CascadeClassifier detector;


JNIEXPORT jboolean JNICALL Java_com_unicorn_faces_app_natives_FaceDetector_load
(JNIEnv *env, jobject obj, jstring cascadeFile) { // No release called. Memory leak might be caused here.
    const char* _cascadeFile = env->GetStringUTFChars(cascadeFile, 0);
    return detector.load(_cascadeFile);
}

JNIEXPORT jboolean JNICALL Java_com_unicorn_faces_app_natives_FaceDetector_empty
(JNIEnv *, jobject) {
    return detector.empty();
}

/**
 * Detect faces on given image.
 * @param yuv: The target image using YUV_NV21 format.
 * @return: A array of `Face`, `null` if no face is detected.
**/
JNIEXPORT jobjectArray JNICALL Java_com_unicorn_faces_app_natives_FaceDetector_findFaces
(JNIEnv *env, jobject obj, jbyteArray yuv, jint width, jint height) {
    if (detector.empty()) return NULL;
    
    jbyte* _yuv = env->GetByteArrayElements(yuv, 0);
    cv::Mat yuvMat(height + height / 2, width, CV_8UC1, (unsigned char*)_yuv);
    cv::Mat grayMat;
    cv::cvtColor(yuvMat, grayMat, CV_YUV2GRAY_NV21);
    
    std::vector<cv::Rect> faceVector;
    detector.detectMultiScale(grayMat, faceVector);
    
    env->ReleaseByteArrayElements(yuv, _yuv, JNI_ABORT);
    
    std::cout << "detect finished with " << faceVector.size() << " faces found." << std::endl;
    
    jclass faceClass = env->FindClass("com/unicorn/faces/app/natives/FaceDetector$Face");
    if (NULL == faceClass) return NULL;
    
    jobjectArray faces = env->NewObjectArray((int)faceVector.size(), faceClass, NULL);
    if (NULL == faces) return NULL;
    
    jmethodID faceInit = env->GetMethodID(faceClass, "<init>", "(Lcom/unicorn/faces/app/natives/FaceDetector;)V");
    jfieldID faceX = env->GetFieldID(faceClass, "x", "I");
    jfieldID faceY = env->GetFieldID(faceClass, "y", "I");
    jfieldID faceWidth = env->GetFieldID(faceClass, "width", "I");
    jfieldID faceHeight = env->GetFieldID(faceClass, "height", "I");
    
    for (int i = 0; i < faceVector.size(); ++i) {
        jobject faceObj = env->NewObject(faceClass, faceInit);
        
        if (NULL == faceObj) return NULL; // Error occur!!
        
        env->SetIntField(faceObj, faceX, faceVector[i].x);
        env->SetIntField(faceObj, faceY, faceVector[i].y);
        env->SetIntField(faceObj, faceWidth, faceVector[i].width);
        env->SetIntField(faceObj, faceHeight, faceVector[i].height);
        env->SetObjectArrayElement(faces, i, faceObj);
    }
    
    return faces;
}
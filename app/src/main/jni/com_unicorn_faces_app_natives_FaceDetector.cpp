//
//  FaceDetector.cpp
//  libfaces
//
//  Created by Huxley on 5/9/15.
//  Copyright (c) 2015 Huxley. All rights reserved.
//

#include <jni.h>
#include <stdio.h>
#include <string>
#include <vector>

#include <android/log.h>

#include <opencv2/objdetect.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/imgcodecs.hpp>
#include <opencv2/core/utility.hpp>

#include "com_unicorn_faces_app_natives_FaceDetector.h"

#define TAG "libfaces"
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,TAG,__VA_ARGS__)

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
(JNIEnv *env, jobject obj, jbyteArray data, jint length) {
    if (detector.empty()) return NULL;

    jbyte* _data = env->GetByteArrayElements(data, 0);
    std::vector<unsigned char> vec_obj;
    for ( unsigned int i = 0; i < length; i++ ) vec_obj.push_back(_data[i]);
    cv::Mat buf_obj(vec_obj, false);
    cv::Mat mat = cv::imdecode(buf_obj, 0);

    std::vector<cv::Rect> faceVector;
    detector.detectMultiScale(mat, faceVector);
    
    env->ReleaseByteArrayElements(data, _data, JNI_ABORT);

    LOGD("detect finished with %d faces found.", faceVector.size());
    
    jclass faceClass = env->FindClass("com/unicorn/faces/app/natives/FaceDetector$Face");
    if (NULL == faceClass) {
        LOGD("Can't found class `FaceDetector$Face`.");
        return NULL;
    }
    
    jobjectArray faces = env->NewObjectArray((int)faceVector.size(), faceClass, NULL);
    if (NULL == faces) {
        LOGD("`Can't allocate a `Face` array.");
        return NULL;
    }
    
    jmethodID faceInit = env->GetMethodID(faceClass, "<init>", "(Lcom/unicorn/faces/app/natives/FaceDetector;)V");
    jfieldID faceX = env->GetFieldID(faceClass, "x", "I");
    jfieldID faceY = env->GetFieldID(faceClass, "y", "I");
    jfieldID faceWidth = env->GetFieldID(faceClass, "width", "I");
    jfieldID faceHeight = env->GetFieldID(faceClass, "height", "I");
    
    for (int i = 0; i < faceVector.size(); ++i) {
        jobject faceObj = env->NewObject(faceClass, faceInit, NULL);
        
        if (NULL == faceObj) return NULL; // Error occur!!

        LOGD("Face[%d]{x=%d, y=%d, width=%d, height=%d}", i,
            faceVector[i].x, faceVector[i].y, faceVector[i].width, faceVector[i].height);
        
        env->SetIntField(faceObj, faceX, faceVector[i].x);
        env->SetIntField(faceObj, faceY, faceVector[i].y);
        env->SetIntField(faceObj, faceWidth, faceVector[i].width);
        env->SetIntField(faceObj, faceHeight, faceVector[i].height);
        env->SetObjectArrayElement(faces, i, faceObj);
    }
    
    return faces;
}
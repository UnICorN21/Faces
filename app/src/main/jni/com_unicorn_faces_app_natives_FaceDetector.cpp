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
    jboolean result = detector.load(_cascadeFile);
    env->ReleaseStringUTFChars(cascadeFile, _cascadeFile);
    return result;
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
(JNIEnv *env, jobject obj, jbyteArray data, jint length, jint degree, jboolean fixed) {
    if (detector.empty()) return NULL;

    jclass thisClass = env->FindClass("com/unicorn/faces/app/natives/FaceDetector");
    jfieldID orientation = env->GetFieldID(thisClass, "orientation", "I");
    env->SetIntField(obj, orientation, degree);

    jbyte* _data = env->GetByteArrayElements(data, 0);
    std::vector<unsigned char> vec_obj;
    for ( unsigned int i = 0; i < length; i++ ) vec_obj.push_back(_data[i]);
    cv::Mat buf_obj(vec_obj, false);
    cv::Mat mid = cv::imdecode(buf_obj, cv::IMREAD_GRAYSCALE), mat;

    // Rotate image by degree
    switch (degree) {
        case 0: mat = mid; break;
        case 90: cv::transpose(mid, mat); break;
        case 180: cv::flip(mid, mat, 0); break;
        case 270: cv::transpose(mid, mat); cv::flip(mat, mat, 0); break;
        default: return NULL;
    }

    std::vector<cv::Rect> faceVector;
    for (int beta = 0; beta <= 180; beta += 60) {
        detector.detectMultiScale(mat, faceVector, 1.2, 3, 0, cv::Size(36, 36));
        if (faceVector.size()) break;
        mat.convertTo(mat, 0, 1, beta);
    }
    
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

        if (fixed) {
            int oldX = faceVector[i].x, oldY = faceVector[i].y;
            switch (degree) {
                case 0: break;
                case 90:
                    faceVector[i].y = mid.rows - faceVector[i].x - faceVector[i].width;
                    faceVector[i].x = oldY;
                    std::swap(faceVector[i].width, faceVector[i].height);
                    break;
                case 180:
                    faceVector[i].x = mid.cols - faceVector[i].width - faceVector[i].x;
                    faceVector[i].y = mid.rows - faceVector[i].height - faceVector[i].y;
                    break;
                case 270:
                    faceVector[i].x = mid.cols - faceVector[i].y - faceVector[i].height;
                    faceVector[i].y = oldX;
                    std::swap(faceVector[i].width, faceVector[i].height);
                    break;
            }
        }

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
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

#include <opencv2/objdetect.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/highgui.hpp> // To remove in release version.
#include <opencv2/core/utility.hpp>

#include "FaceDetector.h"

cv::CascadeClassifier classifier;

JNIEXPORT void JNICALL Java_FaceDetector_load
(JNIEnv *env, jclass obj, jstring cascadeFile) {
    // TODO
}

JNIEXPORT jobjectArray JNICALL Java_FaceDetector_findFaces
(JNIEnv *env, jclass obj, jbyteArray data, jint width, jint height) {
    // TODO
    return nullptr;
}
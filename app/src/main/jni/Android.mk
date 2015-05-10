LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

#opencv
OPENCVROOT             := /Users/huxley/Downloads/OpenCV-android-sdk/
OPENCV_CAMERA_MODULES  := on
OPENCV_INSTALL_MODULES := on
OPENCV_LIB_TYPE        := SHARED
include ${OPENCVROOT}/sdk/native/jni/OpenCV.mk

LOCAL_SRC_FILES  := com_unicorn_faces_app_natives_FaceDetector.cpp
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_LDLIBS     += -llog

LOCAL_MODULE     := faces

include $(BUILD_SHARED_LIBRARY)
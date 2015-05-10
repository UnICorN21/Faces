LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

#OPENCV_CAMERA_MODULES:=off
#OPENCV_INSTALL_MODULES:=off
#OPENCV_LIB_TYPE:=SHARED
include /Users/huxley/Downloads/OpenCV-android-sdk/sdk/native/jni/OpenCV.mk

LOCAL_SRC_FILES  := com_unicorn_faces_app_natives_FaceDetector.cpp
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_LDLIBS     += -llog -ldl

LOCAL_MODULE     := libfaces

include $(BUILD_SHARED_LIBRARY)
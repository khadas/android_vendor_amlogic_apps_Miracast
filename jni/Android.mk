LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
    com_droidlogic_miracast_wfd.cpp

LOCAL_C_INCLUDES := \
    $(JNI_H_INCLUDE) \
    frameworks/av/media/libstagefright/wifi-display \
    $(TOP)/vendor/amlogic/frameworks/av/media/libstagefright/wifi-display

LOCAL_SHARED_LIBRARIES:= \
        libandroid_runtime \
        libbinder \
        libgui \
        libmedia \
        libstagefright \
        libstagefright_foundation \
        libstagefright_wfd \
        libstagefright_wfd_sink \
        libnativehelper \
        libutils \
        libcutils

LOCAL_MODULE := libwfd_jni
LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)

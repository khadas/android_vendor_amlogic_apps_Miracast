LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
    com_aml_miracast_wfd.cpp

LOCAL_C_INCLUDES := \
    $(JNI_H_INCLUDE) \
    frameworks/av/media/libstagefright/wifi-display

LOCAL_SHARED_LIBRARIES:= \
        libbinder \
        libgui \
        libmedia \
        libstagefright \
        libstagefright_foundation \
        libstagefright_wfd \
        libnativehelper \
        libutils \
        libcutils

LOCAL_MODULE := libwfd_jni
LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)

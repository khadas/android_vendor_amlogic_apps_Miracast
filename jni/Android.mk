LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
    com_droidlogic_miracast_wfd.cpp

LOCAL_C_INCLUDES := \
    $(JNI_H_INCLUDE) \
    libnativehelper/include_jni \
    system/core/libutils/include \
    system/core/liblog/include
#    frameworks/av/media/libstagefright/wifi-display \
#    $(TOP)/vendor/amlogic/frameworks/av/media/libstagefright/wifi-display

LOCAL_SHARED_LIBRARIES:= \
        libbinder \
        libgui \
        libmedia \
        libstagefright \
        libstagefright_foundation \
        libstagefright_wfd_sink \
        liblog \
        libutils \
        libcutils

ifeq ($(shell test $(PLATFORM_SDK_VERSION) -ge 28&& echo OK),OK)
LOCAL_C_INCLUDES+=$(TOP)/vendor/amlogic/common/frameworks/av/libstagefright/wifi-display
LOCAL_PRODUCT_MODULE := true
LOCAL_SHARED_LIBRARIES += \
        vendor.amlogic.hardware.miracast_hdcp2@1.0
else
LOCAL_C_INCLUDES+=$(TOP)/vendor/amlogic/frameworks/av/libstagefright/wifi-display
endif

LOCAL_MODULE := libwfd_jni
LOCAL_MODULE_TAGS := optional
include $(BUILD_SHARED_LIBRARY)

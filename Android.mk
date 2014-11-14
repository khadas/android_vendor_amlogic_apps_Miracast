LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-subdir-java-files)
LOCAL_JAVA_LIBRARIES := droidlogic
LOCAL_SDK_VERSION := current

LOCAL_PACKAGE_NAME := MboxLauncher

LOCAL_PROGUARD_ENABLED := full
LOCAL_PROGUARD_FLAG_FILES := proguard.flags
LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)

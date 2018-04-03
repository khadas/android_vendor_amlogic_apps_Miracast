# Copyright (c) 2014 Amlogic, Inc. All rights reserved.
#
# This source code is subject to the terms and conditions defined in the
# file 'LICENSE' which is part of this source code package.
#
# Description: makefile

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_JAVA_LIBRARIES := droidlogic droidlogic-tv

LOCAL_PACKAGE_NAME := MboxLauncher

ifeq ($(shell test $(PLATFORM_SDK_VERSION) -ge 26 && echo OK),OK)
LOCAL_PROPRIETARY_MODULE := true
else
LOCAL_PRIVILEGED_MODULE := true
endif

LOCAL_PRIVATE_PLATFORM_APIS := true

LOCAL_PROGUARD_ENABLED := full
LOCAL_PROGUARD_FLAG_FILES := proguard.flags
LOCAL_CERTIFICATE := platform

FILE := device/amlogic/$(TARGET_PRODUCT)/files/AndroidManifest-common.xml

ifeq ($(FILE), $(wildcard $(FILE)))
LOCAL_FULL_LIBS_MANIFEST_FILES := $(FILE)
LOCAL_JACK_COVERAGE_INCLUDE_FILTER := com.droidlogic.mboxlauncher*
endif

include $(BUILD_PACKAGE)

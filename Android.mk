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

ifndef PRODUCT_SHIPPING_API_LEVEL
LOCAL_PRIVATE_PLATFORM_APIS := true
endif

LOCAL_OVERRIDES_PACKAGES := Home

LOCAL_PROGUARD_ENABLED := full
LOCAL_PROGUARD_FLAG_FILES := proguard.flags
LOCAL_CERTIFICATE := platform

FILE := device/*/$(TARGET_PRODUCT)/files/MboxLauncher2/AndroidManifest-common.xml
FILES := $(foreach v,$(wildcard $(FILE)),$(v))

ifeq ($(FILES), $(wildcard $(FILE)))
LOCAL_FULL_LIBS_MANIFEST_FILES := $(FILES)
LOCAL_JACK_COVERAGE_INCLUDE_FILTER := com.droidlogic.mboxlauncher*
endif

include $(BUILD_PACKAGE)

/*
 * Copyright 2012, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//#define LOG_NDEBUG 0
#define LOG_TAG "amlMiracast-jni"

#include <utils/Log.h>
#include <nativehelper/jni.h>
#include <nativehelper/JNIHelp.h>
#include <android_runtime/AndroidRuntime.h>

#include "sink/WifiDisplaySink.h"
#include "source/WifiDisplaySource.h"

#include <media/IRemoteDisplay.h>
#include <media/IRemoteDisplayClient.h>

#include <binder/ProcessState.h>
#include <binder/IServiceManager.h>
#include <media/IMediaPlayerService.h>
#include <media/stagefright/DataSource.h>
#include <media/stagefright/foundation/ADebug.h>

using namespace android;

sp<ALooper> mSinkLooper = new ALooper;

sp<ANetworkSession> mSession = new ANetworkSession;
sp<WifiDisplaySink> mSink;
bool mStart = false;
bool mInit = false;

static int connect(const char *sourceHost, int32_t sourcePort) {
    /*
    ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    sp<ANetworkSession> session = new ANetworkSession;
    session->start();

    sp<WifiDisplaySink> sink = new WifiDisplaySink(session);
    mSinkLooper->registerHandler(sink);

    if (sourcePort >= 0) {
        sink->start(sourceHost, sourcePort);
    } else {
        sink->start(sourceHost);
    }

    mSinkLooper->start(true);
    */

    ProcessState::self()->startThreadPool();
    DataSource::RegisterDefaultSniffers();

    mSession->start();

    if(!mInit){
    mInit = true;
    mSink = new WifiDisplaySink(mSession);
    }

    mSinkLooper->registerHandler(mSink);

    if (sourcePort >= 0) {
        mSink->start(sourceHost, sourcePort);
    } else {
        mSink->start(sourceHost);
    }

    mStart = true;
    mSinkLooper->start(true /* runOnCallingThread */);

    //ALOGI("connected\n");
    return 0;
}

static void connect_to_wifi_source(JNIEnv* env, jclass clazz, jstring jip, jint jport) {
    const char *ip = env->GetStringUTFChars(jip, NULL);

    ALOGI("connect to wifi source %s:%d\n", ip, jport);

    connect(ip, jport);
    env->ReleaseStringUTFChars(jip, ip);
}

static void connect_to_rtsp_uri(JNIEnv* env, jclass clazz, jstring juri) {
    const char *ip = env->GetStringUTFChars(juri, NULL);

    ALOGI("connect to rtsp uri %s\n", ip);

    connect(ip, -1);
    env->ReleaseStringUTFChars(juri, ip);
}

static void disconnectSink(JNIEnv* env, jclass clazz) {
    ALOGI("disconnect sink mStart:%d\n", mStart);

    if(mStart){
        mSink->stop();
        mSession->stop();
        mSinkLooper->unregisterHandler(mSink->id());
        //mSinkLooper->stop();
        mStart = false;
    }
}

/*
static void source_start(const char *ip) {
  ProcessState::self()->startThreadPool();

    DataSource::RegisterDefaultSniffers();

  sp<ANetworkSession> session = new ANetworkSession;
    session->start();

  mSourceLooper = new ALooper();
  sp<IRemoteDisplayClient> client;
    sp<WifiDisplaySource> source = new WifiDisplaySource(session, client);
    mSourceLooper->registerHandler(source);

    source->start(ip);

    mSourceLooper->start(true);
}

static void source_stop(JNIEnv* env, jclass clazz) {
  ALOGI("source stop \n");
  mSourceLooper->stop();
}

static void run_as_source(JNIEnv* env, jclass clazz, jstring jip) {
  const char *ip = env->GetStringUTFChars(jip, NULL);

  ALOGI("run as source %s\n", ip);
  
    source_start(ip);
    env->ReleaseStringUTFChars(jip, ip);
}
*/

// ----------------------------------------------------------------------------
static JNINativeMethod gMethods[] = {
    { "nativeConnectWifiSource", "(Ljava/lang/String;I)V",
            (void*) connect_to_wifi_source },
    //{ "nativeConnectRTSPUri", "(Ljava/lang/String;)V",
    //        (void*) connect_to_rtsp_uri },
  { "nativeDisconnectSink", "()V",
            (void*) disconnectSink },
  //{ "nativeSourceStart", "(Ljava/lang/String;)V",
    //        (void*) run_as_source },
  //{ "nativeSourceStop", "()V",
    //        (void*) source_stop },
};

int register_com_amlogic_miracast_WiFiDirectActivity(JNIEnv *env) {
    static const char* const kClassPathName = "com/amlogic/miracast/SinkActivity";

    return jniRegisterNativeMethods(env, kClassPathName, gMethods, sizeof(gMethods) / sizeof(gMethods[0]));
}

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env = NULL;
    jint result = -1;

    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        ALOGI("ERROR: GetEnv failed\n");
        goto bail;
    }
    assert(env != NULL);

  /*
    if (AndroidRuntime::registerNativeMethods(env, "com/android/server/am/ActivityStack", gMethods, NELEM(gMethods)) < 0){
        LOGE("Can't register ActivityStack");
        goto bail;
    }*/

    if(register_com_amlogic_miracast_WiFiDirectActivity(env) < 0){
        ALOGE("Can't register WiFiDirectActivity");
        goto bail;
    }

    /* success -- return valid version number */
    result = JNI_VERSION_1_4;

bail:
    return result;
}



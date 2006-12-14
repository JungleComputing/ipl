#ifndef __IBIS_NET_GM_JAVA_PROPERTIES_H__
#define  __IBIS_NET_GM_JAVA_PROPERTIES_H__

#include <jni.h>

jclass ni_findClass(JNIEnv *env, const char *name);
jmethodID ni_getMethod(JNIEnv *env, jclass clazz, const char *name, const char *sig);
jmethodID ni_getStaticMethod(JNIEnv *env, jclass clazz, const char *name, const char *sig);
jfieldID ni_getField(JNIEnv *env, jclass clazz, const char *name, const char *sig);
jfieldID ni_getStaticField(JNIEnv *env, jclass clazz, const char *name, const char *sig);

char *ni_getProperty(JNIEnv *env, const char *name);
int ni_getBooleanPropertyDflt(JNIEnv *env, const char *name, int dflt);
int ni_getBooleanProperty(JNIEnv *env, const char *name);

#endif

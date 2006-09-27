#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <assert.h>

#include <jni.h>

#include "java_properties.h"

static int	initialized = 0;
static jclass	cls_System;
static jmethodID md_getProperty;



static void
init(JNIEnv *env)
{
    if (initialized) {
	return;
    }

    initialized = 1;

    cls_System = ni_findClass(env, "java/lang/System");
    cls_System = (*env)->NewGlobalRef(env, cls_System);
    md_getProperty = ni_getStaticMethod(env, cls_System,
				 "getProperty",
				 "(Ljava/lang/String;)Ljava/lang/String;");
}


jclass
ni_findClass(JNIEnv *env, const char *name)
{
    jclass	clazz;

    init(env);

    clazz = (*env)->FindClass(env, name);
    if (clazz == NULL) {
	fprintf(stderr, "Cannot find class %s\n", name);
    }

    return clazz;
}


jmethodID
ni_getMethod(JNIEnv *env, jclass clazz, const char *name, const char *sig)
{
    jmethodID m;

    init(env);

    m = (*env)->GetMethodID(env, clazz, name, sig);
    if (m == NULL) {
	fprintf(stderr, "Cannot find method \"%s%s\" class %p\n",
		name, sig, clazz);
    }

    return m;
}


jmethodID
ni_getStaticMethod(JNIEnv *env, jclass clazz, const char *name, const char *sig)
{
    jmethodID m;

    init(env);

    m = (*env)->GetStaticMethodID(env, clazz, name, sig);
    if (m == NULL) {
	fprintf(stderr, "Cannot find static method \"%s%s\" class %p\n",
		name, sig, clazz);
    }

    return m;
}


jfieldID
ni_getField(JNIEnv *env, jclass clazz, const char *name, const char *sig)
{
    jfieldID f;

    init(env);

    f = (*env)->GetFieldID(env, clazz, name, sig);
    if (f == NULL) {
	fprintf(stderr, "Cannot find field \"%s:%s\" class %p\n",
		name, sig, clazz);
    }

    return f;
}


jfieldID
ni_getStaticField(JNIEnv *env, jclass clazz, const char *name, const char *sig)
{
    jfieldID f;

    init(env);

    f = (*env)->GetStaticFieldID(env, clazz, name, sig);
    if (f == NULL) {
	fprintf(stderr, "Cannot find static field \"%s:%s\" class %p\n",
		name, sig, clazz);
    }

    return f;
}


char *
ni_getProperty(JNIEnv *env, const char *name)
{
    jstring s_name;
    jstring p;
    const jbyte *val;
    char *copy;

    init(env);

    s_name = (*env)->NewStringUTF(env, name);
    p = (jstring)(*env)->CallStaticObjectMethod(env, cls_System, md_getProperty, s_name);

    if (p == NULL) {
	return NULL;
    }

    val = (*env)->GetStringUTFChars(env, p, NULL);
    copy = malloc(strlen(val) + 1);
    strcpy(copy, val);
    (*env)->ReleaseStringUTFChars(env, p, val);

    return copy;
}


int
ni_getBooleanPropertyDflt(JNIEnv *env, const char *name, int dflt)
{
    char *val = ni_getProperty(env, name);

    if (val == NULL) {
	return dflt;
    }

    if (strcmp(val, "yes") == 0 ||
	    strcmp(val, "true") == 0 ||
	    strcmp(val, "on") == 0 ||
	    strcmp(val, "1") == 0) {
	return 1;
    }

    return 0;
}


int
ni_getBooleanProperty(JNIEnv *env, const char *name)
{
    return ni_getBooleanPropertyDflt(env, name, 0);
}

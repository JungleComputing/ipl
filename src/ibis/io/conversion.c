#include <string.h>
#include "jni.h"

#include "ibis_io_Conversion.h"

#define BYTE2TYPE(ntype, jtype, Type) \
void Java_ibis_io_Conversion_n_1byte2 ## ntype( \
	JNIEnv *env, \
	jclass clazz, \
	jbyteArray buffer, \
        jint off2, \
	j ## ntype ## Array array, \
	jint off, \
	jint len) \
{ \
    jbyte      *buf; \
    jtype      *a; \
    \
    a = (*env)->Get ## Type ## ArrayElements(env, array, NULL); \
    buf = (*env)->GetByteArrayElements(env, buffer, NULL); \
    \
    memcpy(a + off, buf + off2, len * sizeof(*a)); \
    \
    (*env)->Release ## Type ## ArrayElements(env, array, a, 0); \
    (*env)->ReleaseByteArrayElements(env, buffer, buf, 0); \
}

BYTE2TYPE(boolean, jboolean, Boolean)
BYTE2TYPE(short,   jshort,   Short)
BYTE2TYPE(char,    jchar,    Char)
BYTE2TYPE(int,     jint,     Int)
BYTE2TYPE(long,    jlong,    Long)
BYTE2TYPE(float,   jfloat,   Float)
BYTE2TYPE(double,  jdouble,  Double)

#define TYPE2BYTE(ntype, jtype, Type) \
void Java_ibis_io_Conversion_n_1 ## ntype ## 2byte( \
	JNIEnv *env, \
	jclass clazz, \
	j ## ntype ## Array array, \
	jint off, \
	jint len, \
	jbyteArray buffer, \
        jint off2) \
{ \
    jbyte      *buf; \
    jtype      *a; \
    \
    a = (*env)->Get ## Type ## ArrayElements(env, array, NULL); \
    buf = (*env)->GetByteArrayElements(env, buffer, NULL); \
    \
    memcpy(buf + off2, a + off, len * sizeof(*a)); \
    \
    (*env)->Release ## Type ## ArrayElements(env, array, a, 0); \
    (*env)->ReleaseByteArrayElements(env, buffer, buf, 0); \
}

TYPE2BYTE(boolean, jboolean, Boolean)
TYPE2BYTE(short,   jshort,   Short)
TYPE2BYTE(char,    jchar,    Char)
TYPE2BYTE(int,     jint,     Int)
TYPE2BYTE(long,    jlong,    Long)
TYPE2BYTE(float,   jfloat,   Float)
TYPE2BYTE(double,  jdouble,  Double)

#include "ibis_io_MantaInputStream.h"

JNIEXPORT jobject JNICALL Java_ibis_io_MantaInputStream_createUninitializedObject
  (JNIEnv *, jobject, jclass);

jobject Java_ibis_io_MantaInputStream_createUninitializedObject(
	JNIEnv *env, 
	jobject this, 
	jclass type) 
{ 
	return (*env)->AllocObject(env, type);
}

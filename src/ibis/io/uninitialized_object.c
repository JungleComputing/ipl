#include <string.h>
#include "jni.h"

#include "ibis_io_IbisSerializationInputStream.h"

JNIEXPORT jobject JNICALL Java_ibis_io_IbisSerializationInputStream_createUninitializedObject
  (JNIEnv *, jobject, jclass, jclass);

#if THIS_CANNOT_BE
JNIEXPORT void JNICALL Java_ibis_io_IbisSerializationInputStream_setFieldDouble
  (JNIEnv *, jobject, jobject, jstring, jdouble);
JNIEXPORT void JNICALL Java_ibis_io_IbisSerializationInputStream_setFieldLong
  (JNIEnv *, jobject, jobject, jstring, jlong);
JNIEXPORT void JNICALL Java_ibis_io_IbisSerializationInputStream_setFieldFloat
  (JNIEnv *, jobject, jobject, jstring, jfloat);
JNIEXPORT void JNICALL Java_ibis_io_IbisSerializationInputStream_setFieldInt
  (JNIEnv *, jobject, jobject, jstring, jint);
JNIEXPORT void JNICALL Java_ibis_io_IbisSerializationInputStream_setFieldShort
  (JNIEnv *, jobject, jobject, jstring, jshort);
JNIEXPORT void JNICALL Java_ibis_io_IbisSerializationInputStream_setFieldChar
  (JNIEnv *, jobject, jobject, jstring, jchar);
JNIEXPORT void JNICALL Java_ibis_io_IbisSerializationInputStream_setFieldByte
  (JNIEnv *, jobject, jobject, jstring, jbyte);
JNIEXPORT void JNICALL Java_ibis_io_IbisSerializationInputStream_setFieldBoolean
  (JNIEnv *, jobject, jobject, jstring, jboolean);
JNIEXPORT void JNICALL Java_ibis_io_IbisSerializationInputStream_setFieldObject
  (JNIEnv *, jobject, jobject, jstring, jstring, jobject);
#endif

JNIEXPORT jobject JNICALL Java_ibis_io_IbisSerializationInputStream_createUninitializedObject(
	JNIEnv *env, 
	jobject this, 
	jclass type,
	jclass non_serializable_super) 
{ 
	jmethodID constructor_id = (*env)->GetMethodID(env, non_serializable_super, "<init>", "()V");
	jobject obj = (*env)->AllocObject(env, type);

	if (constructor_id != 0) {
	    (*env)->CallNonvirtualVoidMethod(env, obj, non_serializable_super, constructor_id);
	}
	return obj;
}

JNIEXPORT void JNICALL Java_ibis_io_IbisSerializationInputStream_setFieldDouble(
    JNIEnv *env,
    jobject this,
    jobject ref,
    jstring field,
    jdouble d)
{
    const char *field_name = (*env)->GetStringUTFChars(env, field, NULL);
    jfieldID field_id = (*env)->GetFieldID(env, (*env)->GetObjectClass(env, ref), field_name, "D");
    (*env)->ReleaseStringUTFChars(env, field, field_name);
    if (field_id != 0) {
	(*env)->SetDoubleField(env, ref, field_id, d);
    }
}

JNIEXPORT void JNICALL Java_ibis_io_IbisSerializationInputStream_setFieldLong(
    JNIEnv *env,
    jobject this,
    jobject ref,
    jstring field,
    jlong l)
{
    const char *field_name = (*env)->GetStringUTFChars(env, field, NULL);
    jfieldID field_id = (*env)->GetFieldID(env, (*env)->GetObjectClass(env, ref), field_name, "J");
    (*env)->ReleaseStringUTFChars(env, field, field_name);
    if (field_id != 0) {
	(*env)->SetLongField(env, ref, field_id, l);
    }
}

JNIEXPORT void JNICALL Java_ibis_io_IbisSerializationInputStream_setFieldFloat(
    JNIEnv *env,
    jobject this,
    jobject ref,
    jstring field,
    jfloat f)
{
    const char *field_name = (*env)->GetStringUTFChars(env, field, NULL);
    jfieldID field_id = (*env)->GetFieldID(env, (*env)->GetObjectClass(env, ref), field_name, "F");
    (*env)->ReleaseStringUTFChars(env, field, field_name);
    if (field_id != 0) {
	(*env)->SetFloatField(env, ref, field_id, f);
    }
}

JNIEXPORT void JNICALL Java_ibis_io_IbisSerializationInputStream_setFieldInt(
    JNIEnv *env,
    jobject this,
    jobject ref,
    jstring field,
    jint i)
{
    const char *field_name = (*env)->GetStringUTFChars(env, field, NULL);
    jfieldID field_id = (*env)->GetFieldID(env, (*env)->GetObjectClass(env, ref), field_name, "I");
    (*env)->ReleaseStringUTFChars(env, field, field_name);
    if (field_id != 0) {
	(*env)->SetIntField(env, ref, field_id, i);
    }
}

JNIEXPORT void JNICALL Java_ibis_io_IbisSerializationInputStream_setFieldShort(
    JNIEnv *env,
    jobject this,
    jobject ref,
    jstring field,
    jshort s)
{
    const char *field_name = (*env)->GetStringUTFChars(env, field, NULL);
    jfieldID field_id = (*env)->GetFieldID(env, (*env)->GetObjectClass(env, ref), field_name, "S");
    (*env)->ReleaseStringUTFChars(env, field, field_name);
    if (field_id != 0) {
	(*env)->SetShortField(env, ref, field_id, s);
    }
}

JNIEXPORT void JNICALL Java_ibis_io_IbisSerializationInputStream_setFieldChar(
    JNIEnv *env,
    jobject this,
    jobject ref,
    jstring field,
    jchar s)
{
    const char *field_name = (*env)->GetStringUTFChars(env, field, NULL);
    jfieldID field_id = (*env)->GetFieldID(env, (*env)->GetObjectClass(env, ref), field_name, "C");
    (*env)->ReleaseStringUTFChars(env, field, field_name);
    if (field_id != 0) {
	(*env)->SetCharField(env, ref, field_id, s);
    }
}

JNIEXPORT void JNICALL Java_ibis_io_IbisSerializationInputStream_setFieldByte(
    JNIEnv *env,
    jobject this,
    jobject ref,
    jstring field,
    jbyte s)
{
    const char *field_name = (*env)->GetStringUTFChars(env, field, NULL);
    jfieldID field_id = (*env)->GetFieldID(env, (*env)->GetObjectClass(env, ref), field_name, "B");
    (*env)->ReleaseStringUTFChars(env, field, field_name);
    if (field_id != 0) {
	(*env)->SetByteField(env, ref, field_id, s);
    }
}

JNIEXPORT void JNICALL Java_ibis_io_IbisSerializationInputStream_setFieldBoolean(
    JNIEnv *env,
    jobject this,
    jobject ref,
    jstring field,
    jboolean s)
{
    const char *field_name = (*env)->GetStringUTFChars(env, field, NULL);
    jfieldID field_id = (*env)->GetFieldID(env, (*env)->GetObjectClass(env, ref), field_name, "Z");
    (*env)->ReleaseStringUTFChars(env, field, field_name);
    if (field_id != 0) {
	(*env)->SetBooleanField(env, ref, field_id, s);
    }
}

JNIEXPORT void JNICALL Java_ibis_io_IbisSerializationInputStream_setFieldObject(
    JNIEnv *env,
    jobject this,
    jobject ref,
    jstring field,
    jstring sig,
    jobject s)
{
    const char *field_name = (*env)->GetStringUTFChars(env, field, NULL);
    const char *signature = (*env)->GetStringUTFChars(env, sig, NULL);
    jfieldID field_id = (*env)->GetFieldID(env, (*env)->GetObjectClass(env, ref), field_name, signature);
    (*env)->ReleaseStringUTFChars(env, field, field_name);
    (*env)->ReleaseStringUTFChars(env, sig, signature);
    if (field_id != 0) {
	(*env)->SetObjectField(env, ref, field_id, s);
    }
}

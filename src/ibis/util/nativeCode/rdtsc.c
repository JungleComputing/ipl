#include "ibis_util_nativeCode_Rdtsc.h"
#include <jni.h>


JNIEXPORT jlong JNICALL Java_ibis_util_nativeCode_Rdtsc_rdtsc(JNIEnv *env, jclass cls)
{
    jlong time;

    __asm__ __volatile__ ("rdtsc" : "=A" (time));

    return time;
}


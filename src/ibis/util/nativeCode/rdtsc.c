#include "ibis_util_nativeCode_Rdtsc.h"
#include <jni.h>


JNIEXPORT jlong JNICALL Java_ibis_util_nativeCode_Rdtsc_rdtsc(JNIEnv *env, jclass cls)
{
    jlong time;

#ifdef __GNUC__
    __asm__ __volatile__ ("rdtsc" : "=A" (time));
#else
    fprintf(stderr, "No RDTSC asm support for this platform\n");
#endif

    return time;
}


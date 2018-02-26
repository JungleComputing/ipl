#include <jni.h>

typedef unsigned long long timestamp;

JNIEXPORT jlong JNICALL Java_ibis_ipl_benchmarks_LogP_Native_timestamp
(JNIEnv *env, jclass jc)
{
#if defined(__GNUC__)
#if defined(__i386__)
  timestamp ret;

  __asm__ __volatile__("rdtsc": "=A" (ret));
  return ret;
#elif defined(__x86_64__)
  unsigned int low, high; 

  __asm__ __volatile__("rdtsc" : "=a" (low), "=d" (high)); 
  return ((((timestamp) high) << 32) | (timestamp) low); 
#else
  return (timestamp) 0;
#endif
#else
  return (timestamp) 0;
#endif
}


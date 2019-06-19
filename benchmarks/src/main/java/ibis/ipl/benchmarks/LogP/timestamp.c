/**
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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


#ifndef __IBIS_MANTA_IBIS_IMPL_MESSAGEPASSING_IBP_POLL_H__
#define __IBIS_MANTA_IBIS_IMPL_MESSAGEPASSING_IBP_POLL_H__

#include <jni.h>

void ibmp_poll_register(int (*poll)(JNIEnv *env));
jboolean ibmp_poll_locked(JNIEnv *env);

void ibmp_poll_init(JNIEnv *env);
void ibmp_poll_end(JNIEnv *env);

#endif

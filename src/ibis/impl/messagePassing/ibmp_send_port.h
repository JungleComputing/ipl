#ifndef __IBIS_MANTA_IBIS_IMPL_PANDA_IBP_SEND_PORT_H__
#define __IBIS_MANTA_IBIS_IMPL_PANDA_IBP_SEND_PORT_H__

#include <jni.h>


jboolean ibmp_send_port_new(JNIEnv *env, jstring type, jstring name,
			   jstring ibisId, jint cpu, jint port,
			   jint receiver_port, jint serializationType);
void ibmp_send_port_disconnect(JNIEnv *env, jint sender, jint port,
			      jint receiver_port, jint messageCount);

void ibmp_send_port_init(JNIEnv *env);
void ibmp_send_port_end(JNIEnv *env);

#endif

#ifndef __IBIS_MANTA_IBIS_IMPL_PANDA_IBP_RECEIVE_PORT_NS_H__
#define __IBIS_MANTA_IBIS_IMPL_PANDA_IBP_RECEIVE_PORT_NS_H__

#include <jni.h>

extern int	ibmp_ns_server;

void ibmp_receive_port_ns_bind(JNIEnv *env, jbyteArray id, jint sender, jint client);
void ibmp_receive_port_ns_lookup(JNIEnv *env, jstring name, jint sender, jint client);
void ibmp_receive_port_ns_unbind(JNIEnv *env, jstring name);

void ibmp_receive_port_ns_init(JNIEnv *env);
void ibmp_receive_port_ns_end(JNIEnv *env);

#endif

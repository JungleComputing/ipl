#ifndef __IBIS_MANTA_IBIS_IMPL_PANDA_IBP_H__
#define __IBIS_MANTA_IBIS_IMPL_PANDA_IBP_H__

#include <stdio.h>
#include <jni.h>
#include <pan_sys.h>

typedef struct IBP_MSG	ibp_msg_t, *ibp_msg_p;

extern int	ibp_me;
extern int	ibp_nr;

int ibp_pid_me(void);
int ibp_pid_nr(void);

int ibmp_poll(JNIEnv *env);

int ibp_consume(JNIEnv *env, ibp_msg_p msg, void *buf, int len);
jstring ibp_string_consume(JNIEnv *env, ibp_msg_p msg, int len);
int ibp_string_push(JNIEnv *env, jstring s, pan_iovec_p iov);
jbyteArray ibp_byte_array_consume(JNIEnv *env, ibp_msg_p msg, int len);
int ibp_byte_array_push(JNIEnv *env, jbyteArray a, pan_iovec_p iov);

void ibp_msg_clear(JNIEnv *env, ibp_msg_p msg);
int ibp_msg_consume_left(ibp_msg_p msg);
int ibp_msg_sender(ibp_msg_p msg);
void *ibp_proto_create(unsigned int size);
void ibp_proto_clear(void *proto);

void ibp_intr_enable(JNIEnv *env);
void ibp_intr_disable(JNIEnv *env);

void ibp_report(JNIEnv *env, FILE *f);

void ibp_init(JNIEnv *env, int *argc, char *argv[]);
void ibp_start(JNIEnv *env);
void ibp_end(JNIEnv *env);

#endif

/*---------------------------------------------------------------
 *
 * Layer between Panda MP and Ibis/Panda.
 *
 * Send messages through here.
 * Received messages are demultiplexed and given an JNIEnv * parameter
 * (which is I guess the only reason why this layer exists).
 */

#ifndef __MANTA_IBIS_IMPL_PANDA_IBP_MP_H__
#define __MANTA_IBIS_IMPL_PANDA_IBP_MP_H__

#include <pan_sys.h>

#include <jni.h>


int ibp_mp_proto_offset(void);

int ibp_mp_port_register(int (*upcall)(JNIEnv *, ibp_msg_p msg, void *proto));
void ibp_mp_port_unregister(int port);

void ibp_mp_send_sync(JNIEnv *env, int cpu, int port,
		      pan_iovec_p iov, int iov_size,
		      void *proto, int proto_size);
void ibp_mp_send_async(JNIEnv *env, int cpu, int port,
		       pan_iovec_p iov, int iov_size,
		       void *proto, int proto_size,
		       pan_clear_p sent_upcall, void *arg);

void ibp_mp_bcast(JNIEnv *env, int port,
		  pan_iovec_p iov, int iov_size,
		  void *proto, int proto_size);

int ibp_mp_poll(JNIEnv *env);

void ibp_mp_init(JNIEnv *env);
void ibp_mp_end(JNIEnv *env);

#endif

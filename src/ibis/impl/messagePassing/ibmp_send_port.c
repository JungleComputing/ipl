#include <assert.h>
#include <stdlib.h>

#include <jni.h>

#include "ibis_impl_messagePassing_SendPort.h"

#include <pan_sys.h>
#include <pan_align.h>

#include "ibp.h"
#include "ibp_mp.h"

#include "ibmp.h"
#include "ibmp_send_port.h"

static jclass		cls_ShadowSendPort;
static jmethodID	md_createSSP;
static jmethodID	md_bind_group;
static jmethodID	md_disconnect;
static jfieldID		fld_connect_allowed;


#define GROUP_ID_SERVER	0

static int		global_group_count = 0;

static jclass		cls_Syncer;
static jmethodID	md_s_signal;

static jclass		cls_SendPort;
static jfieldID		fld_group;


static int		ibmp_group_id_proto_start;
static int		ibmp_group_id_proto_size;
static int		ibmp_group_id_request_port;
static int		ibmp_group_id_reply_port;

typedef struct IBMP_GROUP_ID_HDR ibmp_group_id_hdr_t, *ibmp_group_id_hdr_p;

struct IBMP_GROUP_ID_HDR {
    jobject	port;
    jobject	syncer;
    jint	group_id;
};


static ibmp_group_id_hdr_p
ibmp_group_id_hdr(void *proto)
{
    return (ibmp_group_id_hdr_p)((char *)proto + ibmp_group_id_proto_start);
}


static int
ibmp_group_id_request_handle(JNIEnv *env, ibp_msg_p msg, void *proto)
{
    ibmp_group_id_hdr_p hdr = ibmp_group_id_hdr(proto);
    int		sender = ibp_msg_sender(msg);
    void       *reply_proto = ibp_proto_create(ibmp_group_id_proto_size);
    ibmp_group_id_hdr_p reply_hdr = ibmp_group_id_hdr(reply_proto);

    ibmp_lock_check_owned(env);

    reply_hdr->syncer  = hdr->syncer;
    reply_hdr->port    = hdr->port;
    /* We own the lock. No fear for concurrent increments. */
    reply_hdr->group_id = global_group_count++;
fprintf(stderr, "%2d: hand out group id %d to sender %d\n", ibmp_me, reply_hdr->group_id, sender);

    ibp_mp_send_async(env, sender,
		      ibmp_group_id_reply_port,
		      NULL, 0,
		      reply_proto, ibmp_group_id_proto_size,
		      ibp_proto_clear, reply_proto);

    return 0;
}


static int
ibmp_group_id_reply_handle(JNIEnv *env, ibp_msg_p msg, void *proto)
{
    ibmp_group_id_hdr_p hdr = ibmp_group_id_hdr(proto);

fprintf(stderr, "%2d: receive group id %d for sendport %p\n", ibmp_me, hdr->group_id, hdr->port);
    (*env)->SetIntField(env, hdr->port, fld_group, hdr->group_id);
    (*env)->CallVoidMethod(env, hdr->syncer, md_s_signal, JNI_TRUE);
    (*env)->DeleteGlobalRef(env, hdr->port);
    (*env)->DeleteGlobalRef(env, hdr->syncer);

    return 0;
}


JNIEXPORT void JNICALL
Java_ibis_impl_messagePassing_SendPort_requestGroupID(
			JNIEnv *env,
			jobject this,
			jobject syncer)
{
    void       *proto = ibp_proto_create(ibmp_group_id_proto_size);
    ibmp_group_id_hdr_p hdr = ibmp_group_id_hdr(proto);

    hdr->syncer = (*env)->NewGlobalRef(env, syncer);
    hdr->port   = (*env)->NewGlobalRef(env, this);
    ibp_mp_send_sync(env, (int)GROUP_ID_SERVER,
		     ibmp_group_id_request_port,
		     NULL, 0,
		     proto, ibmp_group_id_proto_size);

    ibp_proto_clear(proto);
}


static int	ibmp_bind_group_proto_start;
static int	ibmp_bind_group_proto_size;
static int	ibmp_bind_group_port;

typedef struct IBP_BIND_GROUP_HDR ibmp_bind_group_hdr_t, *ibmp_bind_group_hdr_p;

struct IBP_BIND_GROUP_HDR {
    int		send_length;
    jint	group;
};


static ibmp_bind_group_hdr_p
ibmp_bind_group_hdr(void *proto)
{
    return (ibmp_bind_group_hdr_p)((char *)proto + ibmp_bind_group_proto_start);
}


static int
ibmp_bind_group_handle(JNIEnv *env, ibp_msg_p msg, void *proto)
{
    ibmp_bind_group_hdr_p hdr = ibmp_bind_group_hdr(proto);
    jint sender = (jint)ibp_msg_sender(msg);
    jbyteArray sendPortId = ibp_byte_array_consume(env, msg, hdr->send_length);

    assert(env == ibp_JNIEnv);

    ibmp_lock_check_owned(env);
    IBP_VPRINTF(100, env, ("ibp MP port %d start upcall bind_group_handle()\n",
		    ibmp_bind_group_port, (int)sender));

    IBP_VPRINTF(900, env, ("Now call this method createShadowSendPort...\n"));
    (*env)->CallStaticVoidMethod(env,
				 cls_ShadowSendPort,
				 md_bind_group,
				 sender,
				 sendPortId,
				 hdr->group);

    assert(env == ibp_JNIEnv);

    if ((*env)->ExceptionOccurred(env) != NULL) {
	(*env)->ExceptionDescribe(env);
    }

    IBP_VPRINTF(100, env, ("ibp MP port %d done upcall bind_group_handle()\n",
		      ibmp_bind_group_port));

    return 0;
}


JNIEXPORT void JNICALL
Java_ibis_impl_messagePassing_SendPort_sendBindGroupRequest(
	JNIEnv *env,
	jobject this,
	jint rcve_cpu,
	jbyteArray sendPortId,
	jint group)
{
    void       *proto;
    ibmp_bind_group_hdr_p hdr;
    pan_iovec_t iov[1];

    proto = ibp_proto_create(ibmp_bind_group_proto_size);
    hdr = ibmp_bind_group_hdr(proto);

    hdr->send_length = ibp_byte_array_push(env, sendPortId, &iov[0]);
    hdr->group  = group;

    ibp_mp_send_sync(env, (int)rcve_cpu, ibmp_bind_group_port,
		     iov, sizeof(iov) / sizeof(iov[0]),
		     proto, ibmp_bind_group_proto_size);

    ibp_proto_clear(proto);

    (*env)->ReleaseByteArrayElements(env, sendPortId, iov[0].data, JNI_ABORT);
}


jboolean
ibmp_send_port_new(JNIEnv *env,
		   jbyteArray rcvePort,
		   jbyteArray sendPort,
		   jint group)
{
    jobject s;
    jthrowable exc;

    IBP_VPRINTF(900, env, ("Now call this method createShadowSendPort...\n"));
    s = (*env)->CallStaticObjectMethod(env,
				       cls_ShadowSendPort,
				       md_createSSP,
				       rcvePort,
				       sendPort,
				       group);
    /* The call to md_createSSP may have mucked up ibp_JNIEnv. Restore it. */
    ibp_set_JNIEnv(env);

    if ((exc = (*env)->ExceptionOccurred(env)) != NULL) {
	(*env)->ExceptionDescribe(env);
    }

    return (*env)->GetBooleanField(env, s, fld_connect_allowed);
}


void
ibmp_send_port_disconnect(JNIEnv *env,
			  jbyteArray rcvePort,
			  jbyteArray sendPort,
			  jint messageCount)
{
    (*env)->CallStaticVoidMethod(env,
				 cls_ShadowSendPort,
				 md_disconnect,
				 rcvePort,
				 sendPort,
				 messageCount);

    /* The call to md_createSSP may have mucked up ibp_JNIEnv. Restore it. */
    ibp_set_JNIEnv(env);
}


void
ibmp_send_port_init(JNIEnv *env)
{
    cls_ShadowSendPort = (*env)->FindClass(env,
				   "ibis/impl/messagePassing/ShadowSendPort");
    if (cls_ShadowSendPort == NULL) {
	fprintf(stderr, "%s.%d Cannot find class ibis/impl/messagePassing/ShadowSendPort\n", __FILE__, __LINE__);
	abort();
    }
    cls_ShadowSendPort = (jclass)(*env)->NewGlobalRef(env, (jobject)cls_ShadowSendPort);

    md_createSSP = (*env)->GetStaticMethodID(
			env,
			cls_ShadowSendPort,
			"createShadowSendPort",
			"([B[BI)Libis/impl/messagePassing/ShadowSendPort;");

    if (md_createSSP == NULL) {
	if ((*env)->ExceptionOccurred(env)) {
	    (*env)->ExceptionDescribe(env);
	}
	fprintf(stderr, "%s.%d Cannot find method createShadowSendPort([B[B)Libis.impl.messagePassing.ShadowSendPort;\n", __FILE__, __LINE__);
	abort();
    }

    md_disconnect = (*env)->GetStaticMethodID(env, cls_ShadowSendPort,
					      "disconnect", "([B[BI)V");
    if (md_disconnect == NULL) {
	fprintf(stderr, "%s.%d Cannot find static method disconnect([B[BI)V\n", __FILE__, __LINE__);
	abort();
    }

    md_bind_group = (*env)->GetStaticMethodID(env, cls_ShadowSendPort,
					      "bindGroup", "(I[BI)V");
    if (md_bind_group == NULL) {
	fprintf(stderr, "%s.%d Cannot find static method bindGroup([B[BI)V\n", __FILE__, __LINE__);
	abort();
    }

    fld_connect_allowed = (*env)->GetFieldID(env, cls_ShadowSendPort,
					     "connect_allowed", "Z");

    ibmp_group_id_request_port = ibp_mp_port_register(ibmp_group_id_request_handle);
    ibmp_group_id_reply_port = ibp_mp_port_register(ibmp_group_id_reply_handle);
    ibmp_group_id_proto_start = align_to(ibp_mp_proto_offset(),
					 ibmp_group_id_hdr_t);
    ibmp_group_id_proto_size  = ibmp_group_id_proto_start +
					sizeof(ibmp_group_id_hdr_t);

    cls_Syncer = (*env)->FindClass(env, "ibis/impl/messagePassing/Syncer");
    if (cls_Syncer == NULL) {
	ibmp_error(env, "Cannot find class ibis/impl/messagePassing/Syncer\n");
    }
    cls_Syncer = (jclass)(*env)->NewGlobalRef(env, (jobject)cls_Syncer);

    md_s_signal = (*env)->GetMethodID(env,
				      cls_Syncer,
				      "s_signal",
				      "(Z)V");
    if (md_s_signal == NULL) {
	ibmp_error(env, "Cannot find method s_signal(Z)V\n");
    }

    ibmp_bind_group_port = ibp_mp_port_register(ibmp_bind_group_handle);
    ibmp_bind_group_proto_start = align_to(ibp_mp_proto_offset(), ibmp_bind_group_hdr_t);
    ibmp_bind_group_proto_size  = ibmp_bind_group_proto_start + sizeof(ibmp_bind_group_hdr_t);

    cls_SendPort = (*env)->FindClass(env, "ibis/impl/messagePassing/SendPort");
    if (cls_SendPort == NULL) {
	ibmp_error(env, "Cannot find class ibis/impl/messagePassing/SendPort\n");
    }
    cls_SendPort = (jclass)(*env)->NewGlobalRef(env, (jobject)cls_SendPort);

    fld_group = (*env)->GetFieldID(env, cls_SendPort, "group", "I");
}


void
ibmp_send_port_end(JNIEnv *env)
{
}

#include <assert.h>

#include <jni.h>

#include "ibis_impl_messagePassing_SendPort.h"

#include <pan_sys.h>
#include <pan_align.h>

#include "ibp.h"
#include "ibp_mp.h"

#include "ibmp.h"
#include "ibmp_connect.h"
#include "ibmp_send_port.h"


static jmethodID	md_wakeup;

static jfieldID		fld_connect_allowed;


static int	ibmp_connect_reply_proto_start;
static int	ibmp_connect_reply_proto_size;
static int	ibmp_connect_reply_port;


typedef enum IBBMP_CONNECT_TYPE {
    IBMP_CONNECT,
    IBMP_DISCONNECT
} ibmp_connect_type_t, *ibmp_connect_type_p;


typedef struct IBP_CONNECT_REPLY_HDR ibmp_connect_reply_hdr_t, *ibmp_connect_reply_hdr_p;

struct IBP_CONNECT_REPLY_HDR {
    jobject		syncer;
    jboolean		accept;
    ibmp_connect_type_t	type;
};

static ibmp_connect_reply_hdr_p
ibmp_connect_reply_hdr(void *proto)
{
    return (ibmp_connect_reply_hdr_p)((char *)proto + ibmp_connect_reply_proto_start);
}


static void
connect_reply(JNIEnv *env,
	      jint sender,
	      jobject syncer,
	      jboolean accept,
	      ibmp_connect_type_t tp)
{
    void       *proto = ibp_proto_create(ibmp_connect_reply_proto_size);
    ibmp_connect_reply_hdr_p hdr = ibmp_connect_reply_hdr(proto);

    hdr->syncer = syncer;
    hdr->accept = accept;
    hdr->type   = tp;

    IBP_VPRINTF(10, env, ("Send %sconnect ack to %d, syncer %p\n", tp == IBMP_CONNECT ? "" : "dis", (int)sender, syncer));

    ibp_mp_send_async(env, (int)sender, ibmp_connect_reply_port, NULL, 0,
		      proto, ibmp_connect_reply_proto_size,
		      ibp_proto_clear, proto);
}


static int
ibmp_connect_reply_handle(JNIEnv *env, ibp_msg_p msg, void *proto)
{
    ibmp_connect_reply_hdr_p hdr = ibmp_connect_reply_hdr(proto);

    ibmp_lock_check_owned(env);

    IBP_VPRINTF(10, env, ("Do %sconnect ack from %d, syncer %p\n", hdr->type == IBMP_CONNECT ? "" : "dis", ibp_msg_sender(msg), hdr->syncer));

    (*env)->CallVoidMethod(env, hdr->syncer, md_wakeup, hdr->accept);

    (*env)->DeleteGlobalRef(env, hdr->syncer);

    return 0;
}


static int	ibmp_connect_proto_start;
static int	ibmp_connect_proto_size;
static int	ibmp_connect_port;

typedef struct IBP_CONNECT_HDR ibmp_connect_hdr_t, *ibmp_connect_hdr_p;

struct IBP_CONNECT_HDR {
    int		rcve_length;
    int		send_length;
    jobject	syncer;
    jobject	delayed_syncer;
    jint	serializationType;
    jint	startSeqno;
    jint	group;
    jint	groupStartSeqno;
};

static ibmp_connect_hdr_p
ibmp_connect_hdr(void *proto)
{
    return (ibmp_connect_hdr_p)((char *)proto + ibmp_connect_proto_start);
}


JNIEXPORT void JNICALL
Java_ibis_impl_messagePassing_SendPort_ibmp_1connect(
	JNIEnv *env,
	jobject this,
	jint rcve_cpu,
	jbyteArray rcvePortId,
	jbyteArray sendPortId,
	jobject syncer,
	jobject delayed_syncer,
	jint startSeqno,
	jint group,
	jint groupStartSeqno)
{
    void       *proto;
    ibmp_connect_hdr_p hdr;
    pan_iovec_t iov[2];

    proto = ibp_proto_create(ibmp_connect_proto_size);
    hdr = ibmp_connect_hdr(proto);

    hdr->rcve_length = ibp_byte_array_push(env, rcvePortId, &iov[0]);
    hdr->send_length = ibp_byte_array_push(env, sendPortId, &iov[1]);
    if (syncer != NULL) {
	syncer = (*env)->NewGlobalRef(env, syncer);
    }
    if (delayed_syncer != NULL) {
	delayed_syncer = (*env)->NewGlobalRef(env, delayed_syncer);
    }
    hdr->syncer          = syncer;
    hdr->delayed_syncer  = delayed_syncer;
    hdr->startSeqno      = startSeqno;
    hdr->group           = group;
    hdr->groupStartSeqno = groupStartSeqno;
    IBP_VPRINTF(10, env, ("Connect to remote port at %d, syncer %p delayed_syncer %p\n",
		(int)rcve_cpu, syncer, delayed_syncer));

    ibp_mp_send_sync(env, (int)rcve_cpu, ibmp_connect_port,
		     iov, sizeof(iov) / sizeof(iov[0]),
		     proto, ibmp_connect_proto_size);

    ibp_proto_clear(proto);

    (*env)->ReleaseByteArrayElements(env, rcvePortId, iov[0].data, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, sendPortId, iov[1].data, JNI_ABORT);
}


JNIEXPORT void JNICALL
Java_ibis_impl_messagePassing_ShadowSendPort_sendConnectAck(
	JNIEnv *env,
	jclass clazz,
	jint rcve_cpu,
	jint isyncer,
	jboolean accept)
{
    jobject syncer = (jobject)isyncer;

    if (syncer != NULL) {
	connect_reply(env, rcve_cpu, (jobject)syncer, accept, IBMP_CONNECT);
    }
}


JNIEXPORT void JNICALL
Java_ibis_impl_messagePassing_ShadowSendPort_sendDisconnectAck(
	JNIEnv *env,
	jclass clazz,
	jint rcve_cpu,
	jint isyncer,
	jboolean accept)
{
    jobject syncer = (jobject)isyncer;

    if (syncer != NULL) {
	connect_reply(env, rcve_cpu, (jobject)syncer, accept, IBMP_DISCONNECT);
    }
}


static int
ibmp_connect_handle(JNIEnv *env, ibp_msg_p msg, void *proto)
{
    ibmp_connect_hdr_p hdr = ibmp_connect_hdr(proto);
    jint sender = (jint)ibp_msg_sender(msg);
    jbyteArray rcvePortId = ibp_byte_array_consume(env, msg, hdr->rcve_length);
    jbyteArray sendPortId = ibp_byte_array_consume(env, msg, hdr->send_length);
    jboolean accept;

    assert(env == ibp_JNIEnv);

    ibmp_lock_check_owned(env);
    IBP_VPRINTF(10, env, ("Do connect upcall from %d msg %p delayed syncer %p startSeqno %d group %d groupStart %d\n", (int)sender, msg, hdr->delayed_syncer, hdr->startSeqno, hdr->group, hdr->groupStartSeqno));
    IBP_VPRINTF(100, env, ("ibp MP port %d from %d start upcall connect_handle()\n",
		    ibmp_connect_port, (int)sender));
    accept = ibmp_send_port_new(env,
	    			rcvePortId,
				sendPortId,
				hdr->startSeqno,
				hdr->group,
				hdr->groupStartSeqno,
				(jint)hdr->delayed_syncer);

    if (hdr->syncer != NULL) {
	IBP_VPRINTF(10, env, ("Do immediate connect ack to %d syncer %p\n", (int)sender, hdr->syncer));
	IBP_VPRINTF(100, env, ("ibp MP port %d send connect_reply to %d syncer %p\n",
			  ibmp_connect_port, (int)sender, hdr->syncer));
	connect_reply(env, sender, hdr->syncer, accept, IBMP_CONNECT);
    }

    IBP_VPRINTF(100, env, ("ibp MP port %d done upcall connect_handle()\n",
		      ibmp_connect_port));

    return 0;
}


void
ibmp_connect_init(JNIEnv *env)
{
    jclass	cls_Syncer;
    jclass	cls_ShadowSendPort;

    ibmp_connect_port = ibp_mp_port_register(ibmp_connect_handle);
    ibmp_connect_proto_start = align_to(ibp_mp_proto_offset(), ibmp_connect_hdr_t);
    ibmp_connect_proto_size  = ibmp_connect_proto_start + sizeof(ibmp_connect_hdr_t);

    ibmp_connect_reply_port = ibp_mp_port_register(ibmp_connect_reply_handle);
    ibmp_connect_reply_proto_start = align_to(ibp_mp_proto_offset(),
					      ibmp_connect_reply_hdr_t);
    ibmp_connect_reply_proto_size  = ibmp_connect_reply_proto_start +
					    sizeof(ibmp_connect_reply_hdr_t);

    cls_ShadowSendPort = (*env)->FindClass(env,
					   "ibis/impl/messagePassing/ShadowSendPort");
    if (cls_ShadowSendPort == NULL) {
	ibmp_error(env, "Cannot find class ibis/impl/messagePassing/ShadowSendPort\n");
    }
    cls_ShadowSendPort = (jclass)(*env)->NewGlobalRef(env, (jobject)cls_ShadowSendPort);

    fld_connect_allowed = (*env)->GetFieldID(env, cls_ShadowSendPort,
					     "connect_allowed", "Z");

    cls_Syncer = (*env)->FindClass(env, "ibis/impl/messagePassing/ConnectAcker");
    if (cls_Syncer == NULL) {
	ibmp_error(env, "Cannot find class ibis/impl/messagePassing/ConnectAcker\n");
    }

    md_wakeup = (*env)->GetMethodID(env,
					  cls_Syncer,
					  "wakeup",
					  "(Z)V");
    if (md_wakeup == NULL) {
	ibmp_error(env, "Cannot find method wakeup(Z)V\n");
    }

}


void
ibmp_connect_end(JNIEnv *env)
{
    IBP_VPRINTF(10, env, ("Unregister ibmp_connect_port %d\n", ibmp_connect_port));
    ibp_mp_port_unregister(ibmp_connect_port);
    IBP_VPRINTF(10, env, ("Unregister ibmp_connect_reply_port %d\n", ibmp_connect_reply_port));
    ibp_mp_port_unregister(ibmp_connect_reply_port);
}

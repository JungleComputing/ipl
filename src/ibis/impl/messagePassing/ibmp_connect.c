#include <jni.h>

#include <pan_sys.h>
#include <pan_align.h>

#include "ibmp.h"
#include "ibmp_send_port.h"

#include "ibis_ipl_impl_messagePassing_OutputConnection.h"

#include "ibp.h"
#include "ibp_mp.h"
#include "ibmp_connect.h"


static jclass		cls_Syncer;
static jmethodID	md_s_signal;

static jclass		cls_ShadowSendPort;
static jfieldID		fld_connect_allowed;


static int	ibmp_connect_reply_proto_start;
static int	ibmp_connect_reply_proto_size;
static int	ibmp_connect_reply_port;

typedef struct IBP_CONNECT_REPLY_HDR ibmp_connect_reply_hdr_t, *ibmp_connect_reply_hdr_p;

struct IBP_CONNECT_REPLY_HDR {
    jobject	syncer;
    jboolean	accept;
};

static ibmp_connect_reply_hdr_p
ibmp_connect_reply_hdr(void *proto)
{
    return (ibmp_connect_reply_hdr_p)((char *)proto + ibmp_connect_reply_proto_start);
}


static void
connect_reply(JNIEnv *env, jint sender, jobject syncer, jboolean accept)
{
    void       *proto = ibp_proto_create(ibmp_connect_reply_proto_size);
    ibmp_connect_reply_hdr_p hdr = ibmp_connect_reply_hdr(proto);

    hdr->syncer = syncer;
    hdr->accept = accept;

    ibp_mp_send_async(env, (int)sender, ibmp_connect_reply_port, NULL, 0,
		      proto, ibmp_connect_reply_proto_size,
		      ibp_proto_clear, proto);
}


static int
ibmp_connect_reply_handle(JNIEnv *env, ibp_msg_p msg, void *proto)
{
    ibmp_connect_reply_hdr_p hdr = ibmp_connect_reply_hdr(proto);

    ibmp_lock_check_owned(env);

    (*env)->CallVoidMethod(env, hdr->syncer, md_s_signal, hdr->accept);

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
    jint	serializationType;
};

static ibmp_connect_hdr_p
ibmp_connect_hdr(void *proto)
{
    return (ibmp_connect_hdr_p)((char *)proto + ibmp_connect_proto_start);
}


JNIEXPORT void JNICALL
Java_ibis_ipl_impl_messagePassing_OutputConnection_ibmp_1connect(
	JNIEnv *env,
	jobject this,
	jint rcve_cpu,
	jbyteArray rcvePortId,
	jbyteArray sendPortId,
	jobject syncer)
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
    hdr->syncer = syncer;

    ibp_mp_send_sync(env, (int)rcve_cpu, ibmp_connect_port,
		     iov, sizeof(iov) / sizeof(iov[0]),
		     proto, ibmp_connect_proto_size);

    ibp_proto_clear(proto);

    (*env)->ReleaseByteArrayElements(env, rcvePortId, iov[0].data, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, sendPortId, iov[1].data, JNI_ABORT);
}


static int
ibmp_connect_handle(JNIEnv *env, ibp_msg_p msg, void *proto)
{
    ibmp_connect_hdr_p hdr = ibmp_connect_hdr(proto);
    jint sender = (jint)ibp_msg_sender(msg);
    jbyteArray rcvePortId = ibp_byte_array_consume(env, msg, hdr->rcve_length);
    jbyteArray sendPortId = ibp_byte_array_consume(env, msg, hdr->send_length);
    jboolean accept;

    ibmp_lock_check_owned(env);
    IBP_VPRINTF(100, env, ("ibp MP port %d start upcall connect_handle()\n",
		    ibmp_connect_port, (int)sender));
    accept = ibmp_send_port_new(env, rcvePortId, sendPortId);

    if (hdr->syncer != NULL) {
	IBP_VPRINTF(100, env, ("ibp MP port %d send connect_reply()\n",
			  ibmp_connect_port));
	connect_reply(env, sender, hdr->syncer, accept);
    }

    IBP_VPRINTF(100, env, ("ibp MP port %d done upcall connect_handle()\n",
		      ibmp_connect_port));

    return 0;
}


void
ibmp_connect_init(JNIEnv *env)
{
    ibmp_connect_port = ibp_mp_port_register(ibmp_connect_handle);
    ibmp_connect_proto_start = align_to(ibp_mp_proto_offset(), ibmp_connect_hdr_t);
    ibmp_connect_proto_size  = ibmp_connect_proto_start + sizeof(ibmp_connect_hdr_t);

    ibmp_connect_reply_port = ibp_mp_port_register(ibmp_connect_reply_handle);
    ibmp_connect_reply_proto_start = align_to(ibp_mp_proto_offset(),
					      ibmp_connect_reply_hdr_t);
    ibmp_connect_reply_proto_size  = ibmp_connect_reply_proto_start +
					    sizeof(ibmp_connect_reply_hdr_t);

    cls_Syncer = (*env)->FindClass(env,
	    "ibis/ipl/impl/messagePassing/Syncer");
    if (cls_Syncer == NULL) {
	ibmp_error(env, "Cannot find class ibis/ipl/impl/messagePassing/Syncer\n");
    }

    md_s_signal = (*env)->GetMethodID(env,
					  cls_Syncer,
					  "s_signal",
					  "(Z)V");
    if (md_s_signal == NULL) {
	ibmp_error(env, "Cannot find method s_signal(Z)V\n");
    }

    cls_ShadowSendPort = (*env)->FindClass(env,
					   "ibis/ipl/impl/messagePassing/ShadowSendPort");
    if (cls_ShadowSendPort == NULL) {
	ibmp_error(env, "Cannot find class ibis/ipl/impl/messagePassing/ShadowSendPort\n");
    }
    cls_ShadowSendPort = (jclass)(*env)->NewGlobalRef(env, (jobject)cls_ShadowSendPort);

    fld_connect_allowed = (*env)->GetFieldID(env, cls_ShadowSendPort,
					     "connect_allowed", "Z");

}


void
ibmp_connect_end(JNIEnv *env)
{
    IBP_VPRINTF(10, env, ("Unregister ibmp_connect_port %d\n", ibmp_connect_port));
    ibp_mp_port_unregister(ibmp_connect_port);
    IBP_VPRINTF(10, env, ("Unregister ibmp_connect_reply_port %d\n", ibmp_connect_reply_port));
    ibp_mp_port_unregister(ibmp_connect_reply_port);
}

/*
 * Native methods for
 * 	ibis.ipl.impl.messagePassing.ReceivePortNameServer
 * 	ibis.ipl.impl.messagePassing.ReceivePortNameServerClient
 */

#include <string.h>
#include <jni.h>

#include <pan_sys.h>
#include <pan_align.h>
#include <pan_util.h>

#include "ibmp.h"
#include "ibmp_receive_port_ns.h"

#include "ibis_ipl_impl_messagePassing_ReceivePortNameServer.h"
#include "ibis_ipl_impl_messagePassing_ReceivePortNameServerClient.h"

#include "ibp.h"
#include "ibp_mp.h"
#include "ibmp_receive_port_ns_lookup.h"


static jclass		cls_PandaReceivePortNameServerClient;
static jmethodID	md_NameServerClient_lookup_reply;

static jint		PORT_KNOWN;
static jint		PORT_UNKNOWN;


static int	ibp_ns_lookup_port;
static int	ibp_ns_lookup_proto_start;
static int	ibp_ns_lookup_proto_size;

typedef struct IBP_NS_LOOKUP_HDR {
    int		name_length;
    jint	client;
} ibp_ns_lookup_hdr_t, *ibp_ns_lookup_hdr_p;

static ibp_ns_lookup_hdr_p
ibp_ns_lookup_hdr(void *proto)
{
    return (ibp_ns_lookup_hdr_p)((char *)proto + ibp_ns_lookup_proto_start);
}


void
Java_ibis_ipl_impl_messagePassing_ReceivePortNameServerClient_ns_1lookup(
	JNIEnv *env,
	jobject this,
	jstring name)
{
    void       *proto = ibp_proto_create(ibp_ns_lookup_proto_size);
    ibp_ns_lookup_hdr_p hdr = ibp_ns_lookup_hdr(proto);
    pan_iovec_t	iov[1];
    jobject	client = (*env)->NewGlobalRef(env, this);

    ibmp_lock_check_owned(env);

    hdr->name_length = ibp_string_push(env, name, &iov[0]);
    hdr->client = (jint)client;

    IBP_VPRINTF(50, env, ("Send a lookup request to server %d, name %s\n",
		ibmp_ns_server, (char *)iov[0].data));
    ibp_mp_send_sync(env, ibmp_ns_server, ibp_ns_lookup_port,
		     iov, sizeof(iov) / sizeof(iov[0]),
		     proto, ibp_ns_lookup_proto_size);

    ibp_proto_clear(proto);

    (*env)->ReleaseStringUTFChars(env, name, iov[0].data);
}


static int
ibp_ns_lookup_handle(JNIEnv *env, ibp_msg_p msg, void *proto)
{
    ibp_ns_lookup_hdr_p	hdr = ibp_ns_lookup_hdr(proto);
    jstring	name;
    jint	sender = (jint)ibp_msg_sender(msg);

    name = ibp_string_consume(env, msg, hdr->name_length);
    ibmp_receive_port_ns_lookup(env, name, sender, hdr->client);

    return 0;
}


static int	ibp_ns_lookup_reply_port;
static int	ibp_ns_lookup_reply_proto_start;
static int	ibp_ns_lookup_reply_proto_size;

typedef struct ibp_ns_lookup_reply_hdr {
    jint	ret;
    jint	cpu;
    jint	port;
    int		rcve_length;
    jint	client;
} ibp_ns_lookup_reply_hdr_t, *ibp_ns_lookup_reply_hdr_p;

static ibp_ns_lookup_reply_hdr_p
ibp_ns_lookup_reply_hdr(void *proto)
{
    return (ibp_ns_lookup_reply_hdr_p)((char *)proto + ibp_ns_lookup_reply_proto_start);
}


typedef struct LOOKUP_REPLY {
    void       *proto;
    pan_iovec_t	iov[1];
} lookup_reply_t, *lookup_reply_p;


static void
lookup_reply_clear(void *v)
{
    lookup_reply_p	r = v;

    ibp_proto_clear(r->proto);
    pan_free(r->iov[0].data);

    pan_free(r);
}


void
Java_ibis_ipl_impl_messagePassing_ReceivePortNameServer_lookup_1reply(
	JNIEnv *env,
	jobject this,
	jint ret,
	jint tag,
	jint client,
	jbyteArray rcvePortId)
{
    ibp_ns_lookup_reply_hdr_p hdr;
    int		iov_len;
    lookup_reply_p r;

    ibmp_lock_check_owned(env);
    r = pan_malloc(sizeof(*r));

    r->proto = ibp_proto_create(ibp_ns_lookup_reply_proto_size);
    hdr = ibp_ns_lookup_reply_hdr(r->proto);

    if (ret == PORT_KNOWN) {
	jchar *c;

	hdr->rcve_length = ibp_byte_array_push(env, rcvePortId, &r->iov[0]);
	c = pan_malloc(r->iov[0].len);
	memcpy(c, r->iov[0].data, r->iov[0].len);
	(*env)->ReleaseByteArrayElements(env, rcvePortId, r->iov[0].data, JNI_ABORT);
	r->iov[0].data = c;

	iov_len = 1;

    } else {
	r->iov[0].data = NULL;
	iov_len = 0;
    }

    hdr->ret = ret;
    hdr->client = client;

    ibp_mp_send_async(env, (int)tag, ibp_ns_lookup_reply_port,
		      r->iov, iov_len,
		      r->proto, ibp_ns_lookup_reply_proto_size,
		      lookup_reply_clear, r);
}


static int
ibp_ns_lookup_reply_handle(JNIEnv *env, ibp_msg_p msg, void *proto)
{
    ibp_ns_lookup_reply_hdr_p hdr = ibp_ns_lookup_reply_hdr(proto);
    jobject	client = (jobject)hdr->client;
    jbyteArray	rcvePortId = NULL;

    if (hdr->ret == PORT_KNOWN) {
	rcvePortId = ibp_byte_array_consume(env, msg, hdr->rcve_length);
    }

    (*env)->CallVoidMethod(env,
			   client,
			   md_NameServerClient_lookup_reply,
			   rcvePortId);

    (*env)->DeleteGlobalRef(env, client);

    return 0;
}


void
ibmp_receive_port_ns_lookup_init(JNIEnv *env)
{
    jclass	cls;
    jfieldID	fld;

    // ibmp_lock_check_owned(env);
    ibp_ns_lookup_port = ibp_mp_port_register(ibp_ns_lookup_handle);
    ibp_ns_lookup_proto_start = align_to(ibp_mp_proto_offset(), ibp_ns_lookup_hdr_t);
    ibp_ns_lookup_proto_size  = ibp_ns_lookup_proto_start + sizeof(ibp_ns_lookup_hdr_t);

    ibp_ns_lookup_reply_port = ibp_mp_port_register(ibp_ns_lookup_reply_handle);
    ibp_ns_lookup_reply_proto_start = align_to(ibp_mp_proto_offset(), ibp_ns_lookup_reply_hdr_t);
    ibp_ns_lookup_reply_proto_size  = ibp_ns_lookup_reply_proto_start + sizeof(ibp_ns_lookup_reply_hdr_t);

    cls_PandaReceivePortNameServerClient = (*env)->FindClass(
					    env,
					    "ibis/ipl/impl/messagePassing/ReceivePortNameServerClient");
    if (cls_PandaReceivePortNameServerClient == NULL) {
	ibmp_error(env, "Cannot find class ibis/ipl/impl/messagePassing/ReceivePortNameServerClient\n");
    }
    cls_PandaReceivePortNameServerClient = (jclass)(*env)->NewGlobalRef(env, (jobject)cls_PandaReceivePortNameServerClient);

    md_NameServerClient_lookup_reply = (*env)->GetMethodID(
					    env,
					    cls_PandaReceivePortNameServerClient,
					    "lookup_reply",
					    "([B)V");
    if (md_NameServerClient_lookup_reply == NULL) {
	ibmp_error(env, "Cannot find method lookup_reply(Libis/ipl/impl/messagePassing/ReceivePortIdentifier;)V\n");
    }

#if MUST_USE_INTERFACE
    cls = (*env)->FindClass(env,
			    "ibis/ipl/impl/messagePassing/ReceivePortNameServerProtocol");
    if (cls == NULL) {
	ibmp_error(env, "Cannot find class ibis/ipl/impl/messagePassing/ReceivePortNameServer\n");
    }
#else
    cls = (*env)->FindClass(env,
			    "ibis/ipl/impl/messagePassing/ReceivePortNameServer");
    if (cls == NULL) {
	ibmp_error(env, "Cannot find class ibis/ipl/impl/messagePassing/ReceivePortName\n");
    }
#endif

    fld = (*env)->GetStaticFieldID(env,
				cls,
				"PORT_KNOWN",
				"B");
    if (fld == NULL) {
	ibmp_error(env, "Cannot find field PORT_KNOWN in ibis/ipl/impl/messagePassing/ReceivePortNameServer\n");
    }
    PORT_KNOWN = (*env)->GetStaticByteField(env,
				cls,
				fld);

    fld = (*env)->GetStaticFieldID(env,
				cls,
				"PORT_UNKNOWN",
				"B");
    if (fld == NULL) {
	ibmp_error(env, "Cannot find field PORT_KNOWN in ibis/ipl/impl/messagePassing/ReceivePortNameServer\n");
    }
    PORT_UNKNOWN = (*env)->GetStaticByteField(env,
				cls,
				fld);
}


void
ibmp_receive_port_ns_lookup_end(JNIEnv *env)
{
    IBP_VPRINTF(10, env, ("Unregister ibp_ns_lookup_port %d\n", ibp_ns_lookup_port));
    ibp_mp_port_unregister(ibp_ns_lookup_port);
    IBP_VPRINTF(10, env, ("Unregister ibp_ns_lookup_reply_port %d\n", ibp_ns_lookup_reply_port));
    ibp_mp_port_unregister(ibp_ns_lookup_reply_port);
}

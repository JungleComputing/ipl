/*
 * Native methods for
 * 	ibis.ipl.impl.messagePassing.ReceivePortNameServer
 * 	ibis.ipl.impl.messagePassing.ReceivePortNameServerClient
 */

#include <assert.h>
#include <stdio.h>

#include <jni.h>

#include <pan_sys.h>
#include <pan_align.h>

#include "ibmp.h"
#include "ibmp_receive_port_identifier.h"
#include "ibmp_receive_port_ns.h"

#include "ibis_ipl_impl_messagePassing_ReceivePortNameServer.h"
#include "ibis_ipl_impl_messagePassing_ReceivePortNameServerClient.h"

#include "ibp.h"
#include "ibp_mp.h"
#include "ibmp_receive_port_ns_bind.h"




static int	ibp_ns_bind_port;
static int	ibp_ns_bind_proto_start;
static int	ibp_ns_bind_proto_size;

typedef struct IBP_NS_BIND_HDR {
    jint	cpu;
    jint	port;
    int		name_length;
    int		type_length;
    jint	client;
} ibp_ns_bind_hdr_t, *ibp_ns_bind_hdr_p;

static ibp_ns_bind_hdr_p
ibp_ns_bind_hdr(void *proto)
{
    return (ibp_ns_bind_hdr_p)((char *)proto + ibp_ns_bind_proto_start);
}


void
Java_ibis_ipl_impl_messagePassing_ReceivePortNameServerClient_ns_1bind(
	JNIEnv *env,
	jobject this,
	jstring name,
	jstring type,
	jint cpu,
	jint port)
{
    void       *proto = ibp_proto_create(ibp_ns_bind_proto_size);
    ibp_ns_bind_hdr_p hdr = ibp_ns_bind_hdr(proto);
    pan_iovec_t	iov[2];
    jobject	client = (*env)->NewGlobalRef(env, this);

    hdr->name_length = ibp_string_push(env, name, &iov[0]);
    hdr->type_length = ibp_string_push(env, type, &iov[1]);
    assert(hdr->name_length == iov[0].len);

    hdr->client = (jint)client;
    hdr->port   = port;
    hdr->cpu    = cpu;

    IBP_VPRINTF(50, env, ("send MP bind request \"%s\", client %p\n",
			ibmp_jstring2c(env, name), client));
    ibp_mp_send_sync(env, ibmp_ns_server, ibp_ns_bind_port, iov, 2,
		     proto, ibp_ns_bind_proto_size);

    ibp_proto_clear(proto);

    (*env)->ReleaseStringUTFChars(env, name, iov[0].data);
    (*env)->ReleaseStringUTFChars(env, type, iov[1].data);
}


static int
ibp_ns_bind_handle(JNIEnv *env, ibp_msg_p msg, void *proto)
{
    ibp_ns_bind_hdr_p	hdr = ibp_ns_bind_hdr(proto);
    jobject	id;
    jstring	name;
    jstring	type;
    jint	sender = (jint)ibp_msg_sender(msg);

    name = ibp_string_consume(env, msg, hdr->name_length);
    type = ibp_string_consume(env, msg, hdr->type_length);

    id = ibmp_new_ReceivePortIdentifier(env, name, type, hdr->cpu, hdr->port);
    IBP_VPRINTF(50, env, ("In rp-ns bind: made new ReceivePortId for client 0x%x = %d\n", (int)hdr->client, (int)hdr->client));
    ibmp_receive_port_ns_bind(env, id, sender, hdr->client);
    IBP_VPRINTF(50, env, ("Exit rp-ns bind\n"));

    return 0;
}


static jmethodID	md_NameServerClient_bind_reply;

static int	ibp_ns_bind_reply_port;
static int	ibp_ns_bind_reply_proto_start;
static int	ibp_ns_bind_reply_proto_size;

typedef struct IBP_NS_BIND_REPLY_HDR {
    jint	ret;
    jint	client;
} ibp_ns_bind_reply_hdr_t, *ibp_ns_bind_reply_hdr_p;

static ibp_ns_bind_reply_hdr_p
ibp_ns_bind_reply_hdr(void *proto)
{
    return (ibp_ns_bind_reply_hdr_p)((char *)proto + ibp_ns_bind_reply_proto_start);
}


void
Java_ibis_ipl_impl_messagePassing_ReceivePortNameServer_bind_1reply(
	JNIEnv *env,
	jobject this,
	jint ret,
	jint tag,
	jint client)
{
    void       *proto = ibp_proto_create(ibp_ns_bind_reply_proto_size);
    ibp_ns_bind_reply_hdr_p hdr = ibp_ns_bind_reply_hdr(proto);

    hdr->ret = (int)ret;
    hdr->client = client;

    IBP_VPRINTF(50, env, ("send bind reply message to %d client %p\n",
		(int)tag, (jobject)hdr->client));
    ibp_mp_send_async(env, (int)tag, ibp_ns_bind_reply_port, NULL, 0,
		     proto, ibp_ns_bind_reply_proto_size,
		     ibp_proto_clear, proto);
}


static int
ibp_ns_bind_reply_handle(JNIEnv *env, ibp_msg_p msg, void *proto)
{
    ibp_ns_bind_reply_hdr_p hdr = ibp_ns_bind_reply_hdr(proto);
    jobject client = (jobject)hdr->client;

    IBP_VPRINTF(50, env, ("handle bind reply message for client %p\n",
		client));
    (*env)->CallVoidMethod(env,
			   client,
			   md_NameServerClient_bind_reply);
    IBP_VPRINTF(50, env, ("handled bind reply message for client %p\n",
		client));

    (*env)->DeleteGlobalRef(env, client);
    IBP_VPRINTF(50, env, ("delete global ref for client %p\n", client));

    return 0;
}



void
ibmp_receive_port_ns_bind_init(JNIEnv *env)
{
    jclass cls_NameServerClient;

    // ibmp_lock_check_owned(env);
    ibp_ns_bind_port = ibp_mp_port_register(ibp_ns_bind_handle);
    ibp_ns_bind_proto_start = align_to(ibp_mp_proto_offset(),
				       ibp_ns_bind_hdr_t);
    ibp_ns_bind_proto_size  = ibp_ns_bind_proto_start +
				    sizeof(ibp_ns_bind_hdr_t);

    ibp_ns_bind_reply_port = ibp_mp_port_register(ibp_ns_bind_reply_handle);
    ibp_ns_bind_reply_proto_start = align_to(ibp_mp_proto_offset(),
					     ibp_ns_bind_reply_hdr_t);
    ibp_ns_bind_reply_proto_size  = ibp_ns_bind_reply_proto_start +
					sizeof(ibp_ns_bind_reply_hdr_t);

    cls_NameServerClient = (*env)->FindClass(env,
		    "ibis/ipl/impl/messagePassing/ReceivePortNameServerClient");
    if (cls_NameServerClient == NULL) {
	ibmp_error("Cannot find class ibis/ipl/impl/messagePassing/ReceivePortNameServerClient\n");
    }

    md_NameServerClient_bind_reply = (*env)->GetMethodID(env,
					cls_NameServerClient,
					"bind_reply",
					"()V");
    if (md_NameServerClient_bind_reply == NULL) {
	ibmp_error("Cannot find method bind_reply()V\n");
    }
}


void
ibmp_receive_port_ns_bind_end(JNIEnv *env)
{
    IBP_VPRINTF(10, env, ("Unregister ibp_ns_bind_port %d\n", ibp_ns_bind_port));
    ibp_mp_port_unregister(ibp_ns_bind_port);
    IBP_VPRINTF(10, env, ("Unregister ibp_ns_bind_reply_port %d\n", ibp_ns_bind_reply_port));
    ibp_mp_port_unregister(ibp_ns_bind_reply_port);
}

/*
 * Native methods for
 * 	ibis.impl.messagePassing.ReceivePortNameServer
 * 	ibis.impl.messagePassing.ReceivePortNameServerClient
 */

#include <jni.h>

#include <pan_sys.h>
#include <pan_align.h>

#include "ibmp.h"
#include "ibmp_receive_port_ns.h"

#include "ibis_impl_messagePassing_ReceivePortNameServer.h"
#include "ibis_impl_messagePassing_ReceivePortNameServerClient.h"


#include "ibp.h"
#include "ibp_mp.h"
#include "ibmp_receive_port_ns_unbind.h"


static int	ibp_ns_unbind_port;
static int	ibp_ns_unbind_proto_start;
static int	ibp_ns_unbind_proto_size;

typedef struct ibp_ns_unbind_hdr {
    int		name_length;
} ibp_ns_unbind_hdr_t, *ibp_ns_unbind_hdr_p;

static ibp_ns_unbind_hdr_p
ibp_ns_unbind_hdr(void *proto)
{
    return (ibp_ns_unbind_hdr_p)((char *)proto + ibp_ns_unbind_proto_start);
}


JNIEXPORT void JNICALL
Java_ibis_impl_messagePassing_ReceivePortNameServerClient_ns_1unbind(
	JNIEnv *env,
	jobject this,
	jstring name)
{
    void       *proto = ibp_proto_create(ibp_ns_unbind_proto_size);
    ibp_ns_unbind_hdr_p hdr = ibp_ns_unbind_hdr(proto);
    pan_iovec_t	iov[1];

    hdr->name_length = ibp_string_push(env, name, &iov[0]);

    ibp_mp_send_sync(env, ibmp_ns_server, ibp_ns_unbind_port,
		     iov, sizeof(iov) / sizeof(iov[0]),
		     proto, ibp_ns_unbind_proto_size);

    ibp_proto_clear(proto);

    (*env)->ReleaseStringUTFChars(env, name, iov[0].data);
}


static int
ibp_ns_unbind_handle(JNIEnv *env, ibp_msg_p msg, void *proto)
{
    ibp_ns_unbind_hdr_p	hdr = ibp_ns_unbind_hdr(proto);
    jstring	name;

    name = ibp_string_consume(env, msg, hdr->name_length);
    ibmp_receive_port_ns_unbind(env, name);

    return 0;
}


void
ibmp_receive_port_ns_unbind_init(JNIEnv *env)
{
    ibp_ns_unbind_port = ibp_mp_port_register(ibp_ns_unbind_handle);
    ibp_ns_unbind_proto_start = align_to(ibp_mp_proto_offset(), ibp_ns_unbind_hdr_t);
    ibp_ns_unbind_proto_size  = ibp_ns_unbind_proto_start + sizeof(ibp_ns_unbind_hdr_t);
}


void
ibmp_receive_port_ns_unbind_end(JNIEnv *env)
{
    IBP_VPRINTF(10, env, ("Unregister ibp_ns_unbind_port %d\n", ibp_ns_unbind_port));
    ibp_mp_port_unregister(ibp_ns_unbind_port);
}

#include <assert.h>

#include <jni.h>

#include <pan_sys.h>
#include <pan_align.h>

#include "ibis_impl_messagePassing_OutputConnection.h"

#include "ibp.h"
#include "ibp_mp.h"

#include "ibmp.h"
#include "ibmp_send_port.h"
#include "ibmp_disconnect.h"


static int	ibmp_disconnect_proto_start;
static int	ibmp_disconnect_proto_size;
static int	ibmp_disconnect_port;


typedef struct IBP_DISCONNECT_HDR ibmp_disconnect_hdr_t, *ibmp_disconnect_hdr_p;

struct IBP_DISCONNECT_HDR {
    jint	rcve_length;
    jint	send_length;
    jint	messageCount;
};

static ibmp_disconnect_hdr_p
ibmp_disconnect_hdr(void *proto)
{
    return (ibmp_disconnect_hdr_p)((char *)proto + ibmp_disconnect_proto_start);
}


JNIEXPORT void JNICALL
Java_ibis_impl_messagePassing_OutputConnection_ibmp_1disconnect(
	JNIEnv *env,
	jobject this,
	jint cpu,
	jbyteArray rcvePortId,
	jbyteArray sendPortId,
	jint messageCount)
{
    void       *proto = ibp_proto_create(ibmp_disconnect_proto_size);
    ibmp_disconnect_hdr_p hdr = ibmp_disconnect_hdr(proto);
    pan_iovec_t	iov[2];

    hdr->rcve_length  = ibp_byte_array_push(env, rcvePortId, &iov[0]);
    hdr->send_length  = ibp_byte_array_push(env, sendPortId, &iov[1]);
    hdr->messageCount = messageCount;

    IBP_VPRINTF(10, env, ("Disconnect local port\n"));
    ibp_mp_send_sync(env, (int)cpu, ibmp_disconnect_port,
		     iov, sizeof(iov) / sizeof(iov[0]),
		     proto, ibmp_disconnect_proto_size);

    ibp_proto_clear(proto);

    (*env)->ReleaseByteArrayElements(env, rcvePortId, iov[0].data, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, sendPortId, iov[1].data, JNI_ABORT);
}


static int
ibmp_disconnect_handle(JNIEnv *env, ibp_msg_p msg, void *proto)
{
    ibmp_disconnect_hdr_p hdr = ibmp_disconnect_hdr(proto);
    jbyteArray rcvePortId = ibp_byte_array_consume(env, msg, hdr->rcve_length);
    jbyteArray sendPortId = ibp_byte_array_consume(env, msg, hdr->send_length);

    assert(env == ibp_JNIEnv);

    IBP_VPRINTF(10, env, ("Rcve disconnect upcall local port\n"));
    (void)ibmp_send_port_disconnect(env, rcvePortId, sendPortId,
				    hdr->messageCount);

    return 0;
}


void
ibmp_disconnect_init(JNIEnv *env)
{
    ibmp_disconnect_port = ibp_mp_port_register(ibmp_disconnect_handle);
    ibmp_disconnect_proto_start = align_to(ibp_mp_proto_offset(),
					   ibmp_disconnect_hdr_t);
    ibmp_disconnect_proto_size  = ibmp_disconnect_proto_start +
					sizeof(ibmp_disconnect_hdr_t);
}


void
ibmp_disconnect_end(JNIEnv *env)
{
    IBP_VPRINTF(10, env, ("Unregister ibmp_disconnect_port %d\n", ibmp_disconnect_port));
    ibp_mp_port_unregister(ibmp_disconnect_port);
}

#include <jni.h>

#include <pan_sys.h>
#include <pan_align.h>

#include "ibmp.h"
#include "ibmp_send_port.h"

#include "ibis_ipl_impl_messagePassing_OutputConnection.h"

#include "ibp.h"
#include "ibp_mp.h"
#include "ibmp_disconnect.h"


static int	ibmp_disconnect_proto_start;
static int	ibmp_disconnect_proto_size;
static int	ibmp_disconnect_port;


typedef struct IBP_DISCONNECT_HDR ibmp_disconnect_hdr_t, *ibmp_disconnect_hdr_p;

struct IBP_DISCONNECT_HDR {
    jint	send_port;
    jint	rcve_port;
    jint	messageCount;
};

static ibmp_disconnect_hdr_p
ibmp_disconnect_hdr(void *proto)
{
    return (ibmp_disconnect_hdr_p)((char *)proto + ibmp_disconnect_proto_start);
}


void
Java_ibis_ipl_impl_messagePassing_OutputConnection_ibmp_1disconnect(
	JNIEnv *env,
	jobject this,
	jint cpu,
	jint rcve_port,
	jint send_port,
	jint messageCount)
{
    void       *proto = ibp_proto_create(ibmp_disconnect_proto_size);
    ibmp_disconnect_hdr_p hdr = ibmp_disconnect_hdr(proto);

    hdr->send_port = send_port;
    hdr->rcve_port = rcve_port;
    hdr->messageCount     = messageCount;

    IBP_VPRINTF(10, env, ("Disconnect local port %d from cpu %d port %d\n",
		(int)send_port, (int)cpu,
		(int)rcve_port));
    ibp_mp_send_sync(env, (int)cpu, ibmp_disconnect_port, NULL, 0,
		     proto, ibmp_disconnect_proto_size);

    ibp_proto_clear(proto);
}


static int
ibmp_disconnect_handle(JNIEnv *env, ibp_msg_p msg, void *proto)
{
    ibmp_disconnect_hdr_p hdr = ibmp_disconnect_hdr(proto);
    jint sender = (jint)ibp_msg_sender(msg);
    IBP_VPRINTF(10, env, ("Rcve disconnect upcall local port %d from cpu %d port %d\n",
		(int)hdr->rcve_port, (int)sender,
		(int)hdr->send_port));
    (void)ibmp_send_port_disconnect(env, sender, hdr->send_port,
				    hdr->rcve_port, hdr->messageCount);

    return 0;
}


void
ibmp_disconnect_init(JNIEnv *env)
{
    // ibmp_lock_check_owned(env);
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

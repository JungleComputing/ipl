#include <string.h>
#include <assert.h>

#include <pan_sys.h>
#include <pan_align.h>

#include "ibmp.h"

#include "ibp.h"
#include "ibp_mp.h"

#include "ibis_ipl_impl_messagePassing_Ibis.h"

#include "ibmp_join.h"


static int	proto_start;
static int	proto_top;
static int	join_port;
static int	leave_port;

static jmethodID	md_join_upcall;
static jmethodID	md_leave_upcall;


typedef struct JOIN_HDR {
    int		len;
} join_hdr_t, *join_hdr_p;


static join_hdr_p
join_hdr(void *proto)
{
    return (join_hdr_p)((char *)proto + proto_start);
}


void
Java_ibis_ipl_impl_messagePassing_Ibis_send_1join(
		JNIEnv *env,
		jobject this,
		jint host,
		jstring id)
{
    void       *proto = ibp_proto_create(proto_top);
    join_hdr_p	hdr = join_hdr(proto);
    pan_iovec_t	iov;

    IBP_VPRINTF(80, env, ("Send join message to %d", (int)host));

    hdr->len = ibp_string_push(env, id, &iov);

    ibp_mp_send_sync(env, (int)host, join_port, &iov, 1, proto, proto_top);

    ibp_proto_clear(proto);
}


static int
join_upcall(JNIEnv *env, ibp_msg_p msg, void *proto)
{
    join_hdr_p	hdr = join_hdr(proto);
    jstring	id = ibp_string_consume(env, msg, hdr->len);

    IBP_VPRINTF(80, env, ("Receive join message from %d", ibp_msg_sender(msg)));

    (*env)->CallVoidMethod(env, ibmp_obj_Ibis_ibis, md_join_upcall, id, ibp_msg_sender(msg));

    return 0;
}


void
Java_ibis_ipl_impl_messagePassing_Ibis_send_1leave(
		JNIEnv *env,
		jobject this,
		jint host,
		jstring id)
{
    void       *proto = ibp_proto_create(proto_top);
    join_hdr_p	hdr = join_hdr(proto);
    pan_iovec_t	iov;

    hdr->len = ibp_string_push(env, id, &iov);

    ibp_mp_send_sync(env, (int)host, leave_port, &iov, 1, proto, proto_top);

    ibp_proto_clear(proto);
}


static int
leave_upcall(JNIEnv *env, ibp_msg_p msg, void *proto)
{
    join_hdr_p	hdr = join_hdr(proto);
    jstring	id = ibp_string_consume(env, msg, hdr->len);

    (*env)->CallVoidMethod(env, ibmp_obj_Ibis_ibis, md_leave_upcall, id, ibp_msg_sender(msg));

    return 0;
}


void
ibmp_join_init(JNIEnv *env)
{
    md_join_upcall = (*env)->GetMethodID(env,
					 ibmp_cls_Ibis,
					 "join_upcall",
					 "(Ljava/lang/String;I)V");
    if (md_join_upcall == NULL) {
	ibmp_error("Cannot find method join_upcall(Ljava/lang/String;I)V\n");
    }

    md_leave_upcall = (*env)->GetMethodID(env,
					  ibmp_cls_Ibis,
					  "leave_upcall",
					  "(Ljava/lang/String;I)V");
    if (md_leave_upcall == NULL) {
	ibmp_error("Cannot find method leave_upcall(Ljava/lang/String;I)V\n");
    }

    // ibmp_lock_check_owned(env);
    join_port = ibp_mp_port_register(join_upcall);
    leave_port = ibp_mp_port_register(leave_upcall);

    proto_start = align_to(ibp_mp_proto_offset(), join_hdr_t);
    proto_top   = proto_start + sizeof(join_hdr_t);
}


void
ibmp_join_end(JNIEnv *env)
{
}

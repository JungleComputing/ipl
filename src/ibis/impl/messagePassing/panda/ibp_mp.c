/*---------------------------------------------------------------
 *
 * Layer between Panda MP and Ibis/Panda.
 *
 * Send messages through here.
 * Received messages are demultiplexed and given an JNIEnv * parameter
 * (which is I guess the only reason why this layer exists).
 */
#include <jni.h>

#include <assert.h>

#include <pan_sys.h>
#include <pan_mp.h>
#include <pan_align.h>

#include "../ibmp.h"
#include "../ibmp_poll.h"

#include "ibp.h"
#include "ibp_mp.h"

#include "ibp_env.h"


static int	ibp_upcall_done;


static int		ibp_n_upcall = 0;
static int	     (**ibp_upcall)(JNIEnv *, ibp_msg_p, void *) = NULL;


#if UNUSED
static void
proto_dump(void *proto, int size)
{
    int i;
    int *p = (int *)proto;

    fprintf(stderr, "proto(%d) %p = [", ibp_mp_proto_offset(), proto);
    for (i = 0; i < size / sizeof(int); i++) {
	fprintf(stderr, "%d ", p[i]);
    }
    fprintf(stderr, "]\n");
}
#endif


static int	ibp_mp_proto_start;
static int	ibp_mp_proto_top;
static int	ibp_mp_port;

#ifndef NDEBUG
static int     *ibp_send_seqno;
static int     *ibp_rcve_seqno;
#endif


typedef struct IBP_MP_HDR ibp_mp_hdr_t, *ibp_mp_hdr_p;

struct IBP_MP_HDR {
    int			port;
#ifndef NDEBUG
    int			seqno;
#endif
};

static ibp_mp_hdr_p
ibp_mp_hdr(void *proto)
{
    return (ibp_mp_hdr_p)((char *)proto + ibp_mp_proto_start);
}

int
ibp_mp_proto_offset(void)
{
    return ibp_mp_proto_top;
}


#ifdef IBP_VERBOSE
static int *mp_upcalls = 0;
static int *mp_sends = 0;
#endif


static void
ibp_mp_upcall(pan_msg_p msg, void *proto)
{
    ibp_mp_hdr_p	hdr = ibp_mp_hdr(proto);

    //    if ((*vm)->GetEnv(vm, &env, JNI_VERSION_1_2) == JNI_OK) {
    //	    printf("Got a *env!\n");
    //    } else {
    //	    printf("Failed to get a *env!\n");
    //    }

    assert(ibp_JNIEnv != NULL);
    assert(hdr->seqno == ibp_rcve_seqno[pan_msg_sender(msg)]++);

    IBP_VPRINTF(200, ibp_JNIEnv,
		     ("Receive ibp MP upcall %d msg %p port %d sender %d size %d\n",
		       mp_upcalls[pan_msg_sender(msg)]++, msg, hdr->port, pan_msg_sender(msg),
		       pan_msg_consume_left(msg)));

    ibp_upcall_done = 1;	/* Keep polling */
    if (! ibp_upcall[hdr->port](ibp_JNIEnv, (ibp_msg_p)msg, proto)) {
	IBP_VPRINTF(50, ibp_JNIEnv, ("clear panda msg %p\n", msg));
	assert(ibp_JNIEnv == NULL || (ibmp_lock_check_owned(ibp_JNIEnv), 1));
	pan_msg_clear(msg);
    }
}


static int
ibp_mp_poll(JNIEnv *env)
{
    int		done_anything = 0;
// fprintf(stderr, "ibp_mp-poll\n");

    ibmp_lock_check_owned(env);
    ibp_set_JNIEnv(env);
    while (1) {
	ibp_upcall_done = 0;
	pan_poll();
	if (! ibp_upcall_done) {
	    break;
	}
	done_anything = 1;
    }
    ibp_unset_JNIEnv();

    return done_anything;
}


int
ibp_mp_port_register(int (*upcall)(JNIEnv *, ibp_msg_p, void *))
{
    int		port = ibp_n_upcall;

    ibp_n_upcall++;
    ibp_upcall = realloc(ibp_upcall, ibp_n_upcall * sizeof(*ibp_upcall));
    ibp_upcall[port] = upcall;

    return port;
}


static int
no_such_upcall(JNIEnv *env, ibp_msg_p msg, void *proto)
{
    fprintf(stderr, "%2d: receive a IBIS/panda MP message for a port already cleared\n", pan_my_pid());
    return 0;
}


void
ibp_mp_port_unregister(int port)
{
    ibp_upcall[port] = no_such_upcall;
}


void
ibp_mp_send_sync(JNIEnv *env, int cpu, int port,
		 pan_iovec_p iov, int iov_size,
		 void *proto, int proto_size)
{
    ibp_mp_hdr_p hdr = ibp_mp_hdr(proto);

#ifndef NDEBUG
    hdr->seqno = ibp_send_seqno[cpu]++;
#endif
    ibp_set_JNIEnv(env);
    IBP_VPRINTF(200, env, ("Do a Panda MP send %d port %d to %d size %d env %p\n",
		mp_sends[cpu]++, port, cpu, ibmp_iovec_len(iov, iov_size), env));
    hdr->port = port;
    pan_mp_send_sync(cpu, ibp_mp_port, iov, iov_size, proto, proto_size, PAN_MP_DELAYED);
    IBP_VPRINTF(200, env, ("Done a Panda MP send\n"));
    ibp_unset_JNIEnv();
}


void
ibp_mp_send_async(JNIEnv *env, int cpu, int port,
		  pan_iovec_p iov, int iov_size,
		  void *proto, int proto_size,
		  pan_clear_p sent_upcall, void *arg)
{
    ibp_mp_hdr_p hdr = ibp_mp_hdr(proto);

#ifndef NDEBUG
    hdr->seqno = ibp_send_seqno[cpu]++;
#endif
    ibp_set_JNIEnv(env);
    IBP_VPRINTF(200, env, ("Do a Panda MP send %d async to %d size %d\n",
		mp_sends[cpu]++, cpu, ibmp_iovec_len(iov, iov_size)));
    hdr->port = port;
    pan_mp_send_async(cpu, ibp_mp_port, iov, iov_size,
		      proto, proto_size, PAN_MP_DELAYED,
		      sent_upcall, arg);

    ibp_unset_JNIEnv();
}


void
ibp_mp_init(JNIEnv *env)
{
    IBP_VPRINTF(2000, env, ("here...\n"));
    pan_init(NULL, NULL); // No no is reentrant (what do you think RFHH): THIS HAS ALLREADY BEEN DONE ??? JASON
    IBP_VPRINTF(2000, env, ("here...\n"));
    pan_mp_init(NULL, NULL);
    IBP_VPRINTF(2000, env, ("here...\n"));

    ibp_mp_proto_start = align_to(pan_mp_proto_offset(), ibp_mp_hdr_t);
    ibp_mp_proto_top   = ibp_mp_proto_start + sizeof(ibp_mp_hdr_t);
    IBP_VPRINTF(2000, env, ("here...\n"));

    ibp_mp_port = pan_mp_register_port(ibp_mp_upcall);
    IBP_VPRINTF(2000, env, ("here...\n"));

    ibmp_poll_register(ibp_mp_poll);	// Finding this had been commented out by Jason took me another day... RFHH
    IBP_VPRINTF(2000, env, ("here...\n"));

#ifdef IBP_VERBOSE
    // ibmp_lock_check_owned(env);
    mp_upcalls = calloc(pan_nr_processes(), sizeof(*mp_upcalls));
    mp_sends = calloc(pan_nr_processes(), sizeof(*mp_sends));
#endif
#ifndef NDEBUG
    ibp_send_seqno = calloc(pan_nr_processes(), sizeof(*ibp_send_seqno));
    ibp_rcve_seqno = calloc(pan_nr_processes(), sizeof(*ibp_rcve_seqno));
#endif
}


void
ibp_mp_end(JNIEnv *env)
{
    ibp_mp_poll(env);

    pan_mp_free_port(ibp_mp_port);
    pan_mp_end();
    pan_end();
}

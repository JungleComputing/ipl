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

#include "ibp.h"
#include "ibp_poll.h"
#include "ibp_mp.h"


static int	ibp_upcall_done;


static int		ibp_n_upcall = 0;
static int	     (**ibp_upcall)(JNIEnv *, pan_msg_p, void *) = NULL;


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


static int	ibp_mp_proto_start;
static int	ibp_mp_proto_top;
static int	ibp_mp_port;


typedef struct IBP_MP_HDR ibp_mp_hdr_t, *ibp_mp_hdr_p;

struct IBP_MP_HDR {
    int			port;
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

    //    JNIEnv *env;

    //    if ((*vm)->GetEnv(vm, &env, JNI_VERSION_1_2) == JNI_OK) {
    //	    printf("Got a *env!\n");
    //    } else {
    //	    printf("Failed to get a *env!\n");
    //    }

    assert(ibmp_JNIEnv != NULL);

    IBP_VPRINTF(200, ibmp_JNIEnv,
		     ("Receive ibp MP upcall %d msg %p sender %d size %d\n",
		       mp_upcalls[pan_msg_sender(msg)]++, msg, pan_msg_sender(msg),
		       pan_msg_consume_left(msg)));

    ibp_upcall_done = 1;	/* Keep polling */
    if (! ibp_upcall[hdr->port](ibmp_JNIEnv, msg, proto)) {
	IBP_VPRINTF(50, ibmp_JNIEnv, ("clear panda msg %p\n", msg));
	pan_msg_clear(msg);
    }
}


static void
ibp_mp_poll(JNIEnv *env)
{
    //fprintf(stderr, "ibp_mp-poll\n");

    ibmp_set_JNIEnv(env);
    do {
	ibp_upcall_done = 0;
	pan_poll();
    } while (ibp_upcall_done);
    ibmp_unset_JNIEnv();
}


int
ibp_mp_port_register(int (*upcall)(JNIEnv *, pan_msg_p, void *))
{
    int		port = ibp_n_upcall;

    ibp_n_upcall++;
    ibp_upcall = pan_realloc(ibp_upcall, ibp_n_upcall * sizeof(*ibp_upcall));
    ibp_upcall[port] = upcall;

    return port;
}


static int
no_such_upcall(JNIEnv *env, pan_msg_p msg, void *proto)
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
ibp_mp_send_sync(JNIEnv *env, int cpu, int port, pan_iovec_p iov, int iov_size,
		 void *proto, int proto_size, pan_mp_ack_t ack)
{
    ibp_mp_hdr_p hdr = ibp_mp_hdr(proto);

    ibmp_set_JNIEnv(env);
    IBP_VPRINTF(200, env, ("Do a Panda MP send %d to %d size %d env %p\n",
		mp_sends[cpu]++, cpu, pan_msg_iovec_len(iov, iov_size), env));
    hdr->port = port;
    pan_mp_send_sync(cpu, ibp_mp_port, iov, iov_size, proto, proto_size, ack);
    IBP_VPRINTF(200, env, ("Done a Panda MP send\n"));
    ibmp_unset_JNIEnv();
}


int
ibp_mp_send_async(JNIEnv *env, int cpu, int port, pan_iovec_p iov, int iov_size,
		  void *proto, int proto_size, pan_mp_ack_t ack,
		  pan_clear_p sent_upcall, void *arg)
{
    ibp_mp_hdr_p hdr = ibp_mp_hdr(proto);
    int		ticket;

    ibmp_set_JNIEnv(env);
    IBP_VPRINTF(200, env, ("Do a Panda MP send %d async to %d size %d\n",
		mp_sends[cpu]++, cpu, pan_msg_iovec_len(iov, iov_size)));
    hdr->port = port;
    ticket = pan_mp_send_async(cpu, ibp_mp_port, iov, iov_size,
			       proto, proto_size, ack,
			       sent_upcall, arg);

    ibmp_unset_JNIEnv();
    return ticket;
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

    ibp_poll_register(ibp_mp_poll);	// Finding this had been commented out by Jason took me another day... RFHH
    IBP_VPRINTF(2000, env, ("here...\n"));

#ifdef IBP_VERBOSE
    mp_upcalls = pan_calloc(pan_nr_processes(), sizeof(*mp_upcalls));
    mp_sends = pan_calloc(pan_nr_processes(), sizeof(*mp_sends));
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

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


static int	ibp_upcall_done;


static int		ibp_n_upcall = 0;
static int	     (**ibp_upcall)(JNIEnv *, ibp_msg_p, void *) = NULL;

static int		ibp_mp_alive = 0;


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

static int	ibp_group_port;

#ifndef NDEBUG
static int     *ibp_send_seqno;
static int     *ibp_rcve_seqno;
static int	ibp_group_send_seqno;
static int     *ibp_group_rcve_seqno;
#endif


typedef struct IBP_MP_HDR ibp_mp_hdr_t, *ibp_mp_hdr_p;

struct IBP_MP_HDR {
    int			port;
#ifndef NDEBUG
    int			seqno;
    int			bcast;
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
static int     *mp_upcalls = 0;
static int     *mp_sends = 0;
static int	group_upcalls = 0;
static int	group_sends = 0;
#endif


static void
ibp_mp_upcall(pan_msg_p msg, void *proto)
{
    ibp_mp_hdr_p	hdr = ibp_mp_hdr(proto);
    JNIEnv	       *env = ibp_JNIEnv;

#if JASON
    JavaVM *vm = current_VM();

    if ((*vm)->GetEnv(vm, &env, JNI_VERSION_1_2) == JNI_OK) {
        printf("Got a *env!\n");
    } else {
        printf("Failed to get a *env!\n");
    }
#endif

    assert(ibp_JNIEnv != NULL);
    assert(hdr->bcast || hdr->seqno == ibp_rcve_seqno[pan_msg_sender(msg)]++);
    assert(! hdr->bcast || hdr->seqno == ibp_group_rcve_seqno[pan_msg_sender(msg)]++);

    IBP_VPRINTF(200, ibp_JNIEnv,
		     ("Receive ibp MP upcall %d msg %p port %d sender %d size %d\n",
		       mp_upcalls[pan_msg_sender(msg)]++, msg, hdr->port, pan_msg_sender(msg),
		       pan_msg_consume_left(msg)));

    ibp_upcall_done = 1;	/* Keep polling */
    if (! ibp_upcall[hdr->port](env, (ibp_msg_p)msg, proto)) {
	/* The upcall must maintain ibp_JNIEnv, possibly restoring it when
	 * it has released/reacquired the Ibis lock */
	assert(ibp_JNIEnv == env);

	IBP_VPRINTF(50, env, ("clear panda msg %p\n", msg));
	pan_msg_clear(msg);
	assert(ibp_JNIEnv == env);
	IBP_VPRINTF(50, ibp_JNIEnv, ("cleared panda msg %p\n", msg));
    }
    assert(ibp_JNIEnv == env);
}


int
ibp_mp_poll(JNIEnv *env)
{
    int		done_anything = 0;

    ibmp_lock_check_owned(env);
    ibp_set_JNIEnv(env);
    while (1) {
	ibp_upcall_done = 0;
	pan_poll();
	assert(env == ibp_JNIEnv);
	if (! ibp_upcall_done) {
	    break;
	}
	done_anything = 1;
    }

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

    if (! ibp_mp_alive) {
	(*env)->ThrowNew(env, cls_java_io_IOException, "Ibis/Panda MP closed");
    }

#ifndef NDEBUG
    hdr->seqno = ibp_send_seqno[cpu]++;
    hdr->bcast = 0;
#endif
    ibp_set_JNIEnv(env);
    IBP_VPRINTF(200, env, ("Do a Panda MP send %d port %d to %d size %d env %p\n",
		mp_sends[cpu]++, port, cpu, ibmp_iovec_len(iov, iov_size), env));
    hdr->port = port;
    pan_mp_send_sync(cpu, ibp_mp_port, iov, iov_size, proto, proto_size, PAN_MP_DELAYED);
    IBP_VPRINTF(800, env, ("Done a Panda MP send\n"));
    assert(ibp_JNIEnv == env);
}


void
ibp_mp_send_async(JNIEnv *env, int cpu, int port,
		  pan_iovec_p iov, int iov_size,
		  void *proto, int proto_size,
		  pan_clear_p sent_upcall, void *arg)
{
    ibp_mp_hdr_p hdr = ibp_mp_hdr(proto);

    if (! ibp_mp_alive) {
	(*env)->ThrowNew(env, cls_java_io_IOException, "Ibis/Panda MP closed");
    }

#ifndef NDEBUG
    hdr->seqno = ibp_send_seqno[cpu]++;
    hdr->bcast = 0;
#endif
    ibp_set_JNIEnv(env);
    IBP_VPRINTF(200, env, ("Do a Panda MP send %d async to %d size %d\n",
		mp_sends[cpu]++, cpu, ibmp_iovec_len(iov, iov_size)));
    hdr->port = port;
    pan_mp_send_async(cpu, ibp_mp_port, iov, iov_size,
		      proto, proto_size, PAN_MP_DELAYED,
		      sent_upcall, arg);
    IBP_VPRINTF(800, env, ("Done a Panda MP send %d async to %d size %d\n",
		mp_sends[cpu], cpu, ibmp_iovec_len(iov, iov_size)));
}


void
ibp_mp_bcast(JNIEnv *env, int port,
	     pan_iovec_p iov, int iov_size,
	     void *proto, int proto_size)
{
    ibp_mp_hdr_p hdr = ibp_mp_hdr(proto);

    if (! ibp_mp_alive) {
	(*env)->ThrowNew(env, cls_java_io_IOException, "Ibis/Panda Group closed");
    }

#ifndef NDEBUG
    hdr->bcast = 1;
    hdr->seqno = ibp_group_send_seqno++;
#endif
    ibp_set_JNIEnv(env);
    IBP_VPRINTF(200, env, ("Do a Panda group send %d\n",
		group_sends++, ibmp_iovec_len(iov, iov_size)));
    hdr->port = port;
    pan_group_send(ibp_group_port, iov, iov_size, proto, proto_size);
    IBP_VPRINTF(800, env, ("Done a Panda group send %d async to %d size %d\n",
		group_sends, ibmp_iovec_len(iov, iov_size)));
}


#ifndef NDEBUG
#include <signal.h>

static void
sigabort(int sig)
{
    fprintf(stderr, "SIGBART: Now throw an exception\n");
    ibmp_dumpStack(ibp_JNIEnv);
    (*ibp_JNIEnv)->ThrowNew(ibp_JNIEnv,
			    cls_java_io_IOException,
			    "Receive a SIGABORT in native code");
}

#endif


void
ibp_mp_init(JNIEnv *env)
{
    int		mp_offset;
    int		group_offset;

    IBP_VPRINTF(2000, env, ("here...\n"));
    pan_init(NULL, NULL); /* No no is reentrant (what do you think RFHH): THIS HAS ALLREADY BEEN DONE ??? JASON */
    IBP_VPRINTF(2000, env, ("here...\n"));
    pan_mp_init(NULL, NULL);
    IBP_VPRINTF(2000, env, ("here...\n"));
    pan_group_init(NULL, NULL);
    IBP_VPRINTF(2000, env, ("here...\n"));

    mp_offset    = align_to(pan_mp_proto_offset(), ibp_mp_hdr_t);
    group_offset = align_to(pan_group_proto_offset(), ibp_mp_hdr_t);
    ibp_mp_proto_start = mp_offset > group_offset ? mp_offset : group_offset;
    ibp_mp_proto_top   = ibp_mp_proto_start + sizeof(ibp_mp_hdr_t);
    IBP_VPRINTF(2000, env, ("here...\n"));

    ibp_mp_port = pan_mp_register_port(ibp_mp_upcall);
    IBP_VPRINTF(2000, env, ("here...\n"));

    ibp_group_port = pan_group_register_port(ibp_mp_upcall);

    ibmp_poll_register(ibp_mp_poll);	/* Finding this had been commented out by Jason took me another day... RFHH */
    IBP_VPRINTF(2000, env, ("here...\n"));

#ifdef IBP_VERBOSE
    mp_upcalls = calloc(pan_nr_processes(), sizeof(*mp_upcalls));
    mp_sends = calloc(pan_nr_processes(), sizeof(*mp_sends));
#endif
#ifndef NDEBUG
    ibp_send_seqno = calloc(pan_nr_processes(), sizeof(*ibp_send_seqno));
    ibp_rcve_seqno = calloc(pan_nr_processes(), sizeof(*ibp_rcve_seqno));
    ibp_group_rcve_seqno = calloc(pan_nr_processes(), sizeof(*ibp_group_rcve_seqno));
#endif

#ifndef NDEBUG
    signal(SIGABRT, sigabort);
#endif
    ibp_mp_alive = 1;
}


void
ibp_mp_end(JNIEnv *env)
{
    ibp_mp_poll(env);

    ibp_mp_alive = 0;

    pan_comm_intr_disable();
    pan_mp_free_port(ibp_mp_port);
    pan_mp_end();
    pan_group_end();
    pan_end();
}

/*---------------------------------------------------------------
 *
 * Layer between messagePassing/MPI and Ibis/MPI.
 *
 * Send messages through here.
 * Received messages are demultiplexed and given an JNIEnv * parameter
 * (which is I guess the only reason why this layer exists).
 */

#include <assert.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>

#include <das_time.h>

#include <jni.h>

#define USE_STDARG
#include <mpi.h>

#include "../ibmp.h"
#include "../ibmp_poll.h"

#include "ibp.h"
#include "ibp_mp.h"

#include "ibmpi_mp.h"


#define IBMPI_MSG_TAG	1


static int		ibmpi_msg_count;

static int		ibmpi_n_upcall = 0;
static int	     (**ibmpi_upcall)(JNIEnv *, ibp_msg_p, void *) = NULL;

static int		ibmpi_unused_port;


static int		ibmpi_msg_cache_size = RCVE_PROTO_CACHE_SIZE;
static ibp_msg_p	ibmpi_msg_freelist;

int			ibmpi_alive = 0;

static das_time_t	t_rcve_poll;
static int		n_rcve_poll;
static das_time_t	t_send_poll;
static int		n_send_poll;

#ifndef NDEBUG
static int	       *ibmpi_send_seqno;
static int	       *ibmpi_rcve_seqno;
#endif


static ibp_msg_p
ibmpi_msg_create(int sender, int size)
{
    ibp_msg_p	msg;

    if (size <= ibmpi_msg_cache_size) {
	msg = ibmpi_msg_freelist;
	if (msg == NULL) {
	    msg = malloc(sizeof(ibp_msg_t) + ibmpi_msg_cache_size);
	} else {
	    ibmpi_msg_freelist = msg->next;
	}
    } else {
	msg = malloc(sizeof(ibp_msg_t) + size);
    }

    msg->sender = sender;
    msg->size   = size;

    return msg;
}


void
ibp_msg_clear(JNIEnv *env, ibp_msg_p msg)
{
    if (msg->size <= ibmpi_msg_cache_size) {
	msg->next = ibmpi_msg_freelist;
	ibmpi_msg_freelist = msg;
    } else {
	free(msg);
    }
}


static void
ibmpi_mp_upcall(JNIEnv *env, MPI_Status *status)
{
    ibp_msg_p	msg;
    int		size;
    ibmpi_proto_p proto;
    MPI_Status	rcve_status;
#ifdef IBP_VERBOSE
    static int	mp_upcalls = 0;
#endif

#if JASON
    JavaVM *vm = current_VM();

    if ((*vm)->GetEnv(vm, &env, JNI_VERSION_1_2) == JNI_OK) {
        printf("Got a *env!\n");
    } else {
        printf("Failed to get a *env!\n");
    }
#endif

    assert(ibmpi_alive);

    if (MPI_Get_count(status, MPI_PACKED, &size) != MPI_SUCCESS) {
	ibmp_error(env, "Cannot successfully query message size\n");
    }
    msg = ibmpi_msg_create(status->MPI_SOURCE, size);
    proto = (ibmpi_proto_p)(msg + 1);

    IBP_VPRINTF(200, env,
		 ("Receive ibp MP upcall %d msg %p sender %d size %d\n",
		   mp_upcalls++, msg, status->MPI_SOURCE, size));

    if (0) {
	IBP_VPRINTF(200, env,
		 ("Receive ibp MP upcall %d msg %p sender %d size %d\n",
		   mp_upcalls++, msg, ibp_msg_sender(msg),
		   ibp_msg_consume_left(msg)));
    }

    if (MPI_Recv(proto, size, MPI_PACKED,
		 status->MPI_SOURCE, status->MPI_TAG,
		 MPI_COMM_WORLD, &rcve_status) != MPI_SUCCESS) {
	ibmp_error(env, "Cannot successfully receive protocol msg\n");
    }
    msg->start = proto->proto_size;
    assert(proto->seqno == ibmpi_rcve_seqno[status->MPI_SOURCE]++);

    assert(proto->port != ibmpi_unused_port);
    if (! ibmpi_upcall[proto->port](env, msg, proto)) {
	ibp_msg_clear(env, msg);
    }
}


static int
ibmpi_rcve_poll(JNIEnv *env)
{
    MPI_Status status;
    int	present;

    assert(ibmpi_alive);

    MPI_Iprobe(MPI_ANY_SOURCE, IBMPI_MSG_TAG, MPI_COMM_WORLD, &present,
	       &status);

    if (! present) {
	return 0;
    }

    ibmpi_mp_upcall(env, &status);

    return 1;
}


typedef enum IBMPI_FINISHED_STATE {
    IBMPI_free,
    IBMPI_allocated,
    IBMPI_active
} ibmpi_finished_state_t, *ibmpi_finished_state_p;

typedef struct IBMPI_FINISHED_T ibmpi_finished_t, *ibmpi_finished_p;

struct IBMPI_FINISHED_T {
    void	      (*callback)(void *);
    void	       *arg;
    void	       *to_free;
    ibmpi_finished_state_t	status;
    int			index;
    ibmpi_finished_p	next;
};


static int		finished_alloc;
static int		n_finished;
static int		finished_max;
static ibmpi_finished_p	finished_freelist;
static ibmpi_finished_p	finished;
static MPI_Request     *finished_req;
static int	       *finished_index;
static MPI_Status      *finished_status;


static void
finished_allocate(void)
{
#define FINISHED_INCR	32
    int	i;
    int	n = finished_alloc + FINISHED_INCR;

    finished_req = realloc(finished_req, n * sizeof(*finished_req));
    finished_index = realloc(finished_index, n * sizeof(*finished_index));
    finished_status = realloc(finished_status, n * sizeof(*finished_status));

    finished = realloc(finished, n * sizeof(*finished));
    for (i = finished_alloc; i < n; i++) {
	if (i == n - 1) {
	    finished[i].next = NULL;
	} else {
	    finished[i].next = &finished[i + 1];
	}
	finished[i].status = IBMPI_free;
	finished[i].index = i;
	finished_req[i] = MPI_REQUEST_NULL;
    }
    finished_freelist = &finished[finished_alloc];

    finished_alloc = n;
}


static ibmpi_finished_p
ibmpi_finished_get(void)
{
    ibmpi_finished_p f;
    int		i;

    f = finished_freelist;
    if (f == NULL) {
	finished_allocate();
	f = finished_freelist;
    }
    finished_freelist = f->next;
    n_finished++;

    i = f->index;

    if (i > finished_max) {
	finished_max = i;
	// fprintf(stderr, "%d: finished_max := %d\n", ibmp_me, i);
    }

    f->status = IBMPI_allocated;
    assert(finished_req[i] == MPI_REQUEST_NULL);

    return f;
}


static void
ibmpi_finished_put(ibmpi_finished_p f)
{
    assert(f->status == IBMPI_allocated);

    f->next = finished_freelist;
    finished_freelist = f;
    f->status = IBMPI_free;

    if (finished_max == f->index) {
	int	i;

	for (i = finished_max - 1; i >= 0; i--) {
	    if (finished[i].status != IBMPI_free) {
		break;
	    }
	}
	// fprintf(stderr, "%d: finished_max := %d\n", ibmp_me, i);
	finished_max = i;
    }
}


static int
ibmpi_finished_poll(JNIEnv *env)
{
    int		i;
    int		x;
    ibmpi_finished_p f;
    int		hits;

    assert(ibmpi_alive);

    if (n_finished == 0) {
	return 0;
    }

    if (MPI_Testsome(finished_max + 1,
		     finished_req,
		     &hits,
		     finished_index,
		     finished_status) != MPI_SUCCESS) {
	ibmp_error(env, "MPI_Testsome fais\n");
    }

    assert(hits != MPI_UNDEFINED);

    for (i = 0; i < hits; i++) {
	x = finished_index[i];
	f = &finished[x];
	// fprintf(stderr, "Testsome says [%d] = [[%d]] completed, finished= %p\n", x, i, f);
	f->callback(f->arg);
	if (f->to_free != NULL) {
	    free(f->to_free);
	}
	ibmpi_finished_put(f);
    }

    n_finished -= hits;

    return hits;
}


int
ibp_mp_poll(JNIEnv *env)
{
    int		ibmpi_upcall_done;
    int		done_anything = 0;

    //fprintf(stderr, "ibmpi_mp-poll\n");

    do {
	das_time_t	start;
	das_time_t	stop;

	if (! ibmpi_alive) {
	    break;
	}

	ibmpi_upcall_done = 1;
	das_time_get(&start);
	if (ibmpi_rcve_poll(env)) {
	    ibmpi_upcall_done = 0;
	    done_anything = 1;
	}
	das_time_get(&stop);
	t_rcve_poll +=  stop - start;
	n_rcve_poll++;
	das_time_get(&start);
	if (ibmpi_finished_poll(env) > 0) {
	    ibmpi_upcall_done = 0;
	    done_anything = 1;
	}
	das_time_get(&stop);
	t_send_poll +=  stop - start;
	n_send_poll++;
    } while (! ibmpi_upcall_done);

    return done_anything;
}


void
ibp_mp_send_sync(JNIEnv *env, int cpu, int port,
		 pan_iovec_p iov, int iov_size,
		 void *v_proto, int proto_size)
{
    int		len = ibmp_iovec_len(iov, iov_size);
    ibmpi_proto_p proto = v_proto;
    void       *buf;
    int		off;
    int		i;

    assert(ibmpi_alive);

    proto->proto_size = proto_size;
    proto->msg_id = ibmpi_msg_count++;
    proto->port = port;
    assert(port != ibmpi_unused_port);
#ifndef NDEBUG
    proto->seqno = ibmpi_send_seqno[cpu]++;
#endif

    len += proto_size;
    if (len > SEND_PROTO_CACHE_SIZE) {
	buf = malloc(len);
	memcpy(buf, proto, proto_size);
    } else {
	buf = proto;
    }
    off = proto_size;
    for (i = 0; i < iov_size; i++) {
	memcpy((char *)buf + off, iov[i].data, iov[i].len);
	off += iov[i].len;
    }
    assert(off == len);

    IBP_VPRINTF(200, env, ("Do an MPI send to %d size %d\n", cpu, len));
    MPI_Send(buf, len, MPI_PACKED, cpu, IBMPI_MSG_TAG, MPI_COMM_WORLD);
    IBP_VPRINTF(200, env, ("Done an MPI send\n"));

    if (len > SEND_PROTO_CACHE_SIZE) {
	free(buf);
    }

    ibp_mp_poll(env);
}


void
ibp_mp_send_async(JNIEnv *env, int cpu, int port,
		  pan_iovec_p iov, int iov_size,
		  void *v_proto, int proto_size,
		  pan_clear_p sent_upcall, void *arg)
{
    ibmpi_finished_p f = ibmpi_finished_get();
    int		len = ibmp_iovec_len(iov, iov_size);
    ibmpi_proto_p proto = v_proto;
    void       *buf;
    int		off;
    int		i;

    assert(ibmpi_alive);

    proto->proto_size = proto_size;
    proto->msg_id = ibmpi_msg_count++;
    proto->port = port;
    assert(port != ibmpi_unused_port);
#ifndef NDEBUG
    proto->seqno = ibmpi_send_seqno[cpu]++;
#endif

    f->callback = sent_upcall;
    f->arg      = arg;

    len += proto_size;
    if (len > SEND_PROTO_CACHE_SIZE) {
	buf = malloc(len);
	memcpy(buf, proto, proto_size);
	f->to_free = buf;
    } else {
	buf = proto;
	f->to_free = NULL;
    }
    off = proto_size;
    for (i = 0; i < iov_size; i++) {
	memcpy((char *)buf + off, iov[i].data, iov[i].len);
	off += iov[i].len;
    }
    assert(off == len);

    IBP_VPRINTF(200, env, ("Do a Panda MP send async to %d size %d\n",
		cpu, len));
    MPI_Isend(buf, len, MPI_PACKED, cpu, IBMPI_MSG_TAG, MPI_COMM_WORLD,
	      &finished_req[f->index]);

    ibp_mp_poll(env);
}


int
ibp_mp_port_register(int (*upcall)(JNIEnv *, ibp_msg_p, void *))
{
    int		port = ibmpi_n_upcall;

    ibmpi_n_upcall++;
    ibmpi_upcall = realloc(ibmpi_upcall, ibmpi_n_upcall * sizeof(*ibmpi_upcall));
    ibmpi_upcall[port] = upcall;

    return port;
}


static int
no_such_upcall(JNIEnv *env, ibp_msg_p msg, void *proto)
{
    fprintf(stderr, "%2d: receive a Ibis/MPI MP message for a port already cleared\n", ibmp_me);

    abort();

    return 0;
}


void
ibp_mp_port_unregister(int port)
{
    ibmpi_upcall[port] = no_such_upcall;
}


int ibp_mp_proto_offset(void)
{
    return sizeof(ibmpi_proto_t);
}


void
ibp_mp_init(JNIEnv *env)
{
    ibmp_poll_register(ibp_mp_poll);
    ibmpi_unused_port = ibp_mp_port_register(no_such_upcall);
#ifndef NDEBUG
    ibmpi_send_seqno = calloc(ibmp_nr, sizeof(*ibmpi_send_seqno));
    ibmpi_rcve_seqno = calloc(ibmp_nr, sizeof(*ibmpi_send_seqno));
#endif
}


void
ibp_mp_end(JNIEnv *env)
{
    ibp_mp_poll(env);
    fprintf(stdout, "%2d: t_rcve_poll total %.06f (av %.06f in %d)\n",
	    ibmp_me, das_time_t2d(&t_rcve_poll),
	    das_time_t2d(&t_rcve_poll) / n_rcve_poll, n_rcve_poll);
    fprintf(stdout, "%2d: t_send_poll total %.06f (av %.06f in %d)\n",
	    ibmp_me, das_time_t2d(&t_send_poll),
	    das_time_t2d(&t_send_poll) / n_send_poll, n_send_poll);
}

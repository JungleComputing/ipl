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

#include "ibp.h"
#include "ibmpi_poll.h"
#include "ibmpi_mp.h"


static int	ibmpi_upcall_done;


static int		ibmpi_n_upcall = 0;
static ibmpi_upcall_t   ibmpi_upcall[IBMPI_DATA_STREAM_PORT];


typedef enum IBMPI_FINISHED_STATE {
    IBMPI_free,
    IBMPI_allocated,
    IBMPI_active
} ibmpi_finished_state_t, *ibmpi_finished_state_p;

typedef struct IBMPI_FINISHED_T ibmpi_finished_t, *ibmpi_finished_p;

struct IBMPI_FINISHED_T {
    void      (*upcall)(void *);
    void       *arg;
    ibmpi_finished_state_t	status;
    ibmpi_finished_p	next;
};


static int		n_finished;
static ibmp_finished_p	finished_freelist;
static ibmp_finished_p	finished;
static MPI_Request     *finished_req;
static int		finished_alloc;
static int		finished_max;


static int
ibmp_finished_get(void)
{
    ibmp_finished_p f;
    int		i;

    f = finished_freelist;
    if (f == NULL) {
#define FINISHED_INCR	32
	int	i;
	int	n = finished_max + FINISHED_INCR;

	finished = realloc(finished, n * sizeof(*finished));
	finish[finished_alloc].next = finished_freelist;
	finish[finished_alloc].status = IBMPI_free;
	finished_req[finished_alloc] = MPI_REQUEST_NULL;
	for (i = finished_alloc + 1; i < n; i++) {
	    finish[i].next = &finish[i - 1];
	    finish[i].status = IBMPI_free;
	    finished_req[i] = MPI_REQUEST_NULL;
	}
	finished_req = realloc(finished_req, n * sizeof(*finished_req));
	finished_alloc = n;
	f = finished_freelist;
    }
    finished_freelist = f->next;
    n_finished++;

    i = f - finished;

    if (i > finished_max) {
	finished_max = i;
    }

    f->status = IBMPI_allocated;
    assert(finished_req[i] == MPI_REQUEST_NULL);

    return i;
}


static void
ibmp_finished_put(ibmp_finished_p f)
{
    f->next = finished_freelist;
    finished_freelist = f;

    if (finished_max == f - finished) {
	int	i;

	for (i = finished_max - 1; i >= 0; i++) {
	    if (finished[i].status != IBMPI_free) {
		finished_max = i;
		break;
	    }
	}
    }
}


static void
ibmp_finished_register(ibmp_finished_p f)
{
    f->next = finished_q;
    finished_q = f;
}


static void
ibmp_finished_poll(void)
{
    int		i;
    int		x;

    if (n_finished == 0) {
	return;
    }

    MPI_Testsome(max_finished, finished_req, &hits, indir, finished_state);

    for (i = 0; i < hits; i++) {
	x = indir[i];
	finished[x].callback(finished[x].arg);
	finished[x].status = IBMPI_free;
	finished[x].next = finished_freelist;
	finished_freelist = &finished[x];
    }

    n_finished -= hits;
}



static void
ibmpi_mp_upcall(MPI_Status *status);
{
    ibmpi_mp_hdr_p	hdr = ibmpi_mp_hdr(proto);
    ibmpi_tag_t		tag;
#ifdef IBP_VERBOSE
    static int mp_upcalls = 0;
#endif

    //    JNIEnv *env;

    //    if ((*vm)->GetEnv(vm, &env, JNI_VERSION_1_2) == JNI_OK) {
    //	    printf("Got a *env!\n");
    //    } else {
    //	    printf("Failed to get a *env!\n");
    //    }

    assert(ibmpi_JNIEnv != NULL);

    tag.i = status->MPI_TAG;

    IBP_VPRINTF(200, ibmpi_JNIEnv,
		         ("Receive ibp MP upcall %d msg %p sender %d size %d\n",
			   mp_upcalls++, msg, pan_msg_sender(msg),
			   pan_msg_consume_left(msg)));

    ibmpi_upcall_done = 1;	/* Keep polling */
    if (IBMPI_IS_DATA_MSG(tag)) {
	ibmpi_data_stream_upcall(status);
    } else {
	char   *buffer;
	int	size;

	MPI_GET_COUNT(status, MPI_PACKED, &size);
	buffer = malloc(size);
	if (MPI_RECV(buffer, count, MPI_PACKED, status.MPI_SOURCE,
		     tag.i, MPI_COMM_WORLD, &rcve_status) != MPI_SUCCESS) {
	    fprintf(stderr, "Cannot successfully receive protocol msg\n");
	    abort();
	}

	if (! ibmpi_upcall[tag.ss.send](ibmpi_JNIEnv, msg, proto)) {
	    free(buffer);
	}
    }
}


static void
ibmpi_mp_poll(JNIEnv *env)
{
    //fprintf(stderr, "ibmpi_mp-poll\n");
@@@@@@@@@@@@@ hier nog van alles intikken: --- test naar gearriveerde msg, test naar finished send, ...
    ibmpi_set_JNIEnv(env);

    do {
	ibmpi_upcall_done = 0;
	pan_poll();
    } while (ibmpi_upcall_done);
    ibmpi_unset_JNIEnv();
}


int
ibmpi_mp_port_register(int (*upcall)(JNIEnv *, void *, MPI_Status *))
{
    int		port = ibmpi_n_upcall;
    MPI_Comm	c;

    if (port == IBMPI_DATA_STREAM_PORT) {
	fprintf(stderr, "More user-defined special ports requested than provided (%d)\n", port);
	abort();
    }

    ibmpi_n_upcall++;
    ibmpi_upcall[port] = upcall;

    return port;
}


static void
no_such_upcall(JNIEnv *env, void *buf, MPI_Status *status)
{
    fprintf(stderr, "%2d: receive a IBIS/panda MP message for a port already cleared\n", pan_my_pid());
    pan_msg_clear(msg);
}


void
ibmpi_mp_port_unregister(int port)
{
    ibmpi_upcall[port] = no_such_upcall;
}


void
ibmpi_send_sync(JNIEnv *env, int cpu, int tag, void *buf, int len)
{
    ibmpi_set_JNIEnv(env);
    IBP_VPRINTF(200, env, ("Do an MPI send to %d size %d\n", cpu, len));
    MPI_Send(buf, len, MPI_PACKED, cpu, tag, MPI_COMM_WORLD);
    IBP_VPRINTF(200, env, ("Done an MPI send\n"));
    ibmpi_unset_JNIEnv();
}


void
ibmpi_send_async(JNIEnv *env, int cpu, int tag, void *buf, int len,
		 void (*sent_upcall)(void *), void *arg)
{
    ibmpi_mp_hdr_p hdr = ibmpi_mp_hdr(proto);
    int		i_finished = ibmp_finished_get();
    ibmpi_finished_p finished = &finished[i_finished];

    finished->upcall = sent_upcall;
    finished->arg    = arg;

    ibmpi_set_JNIEnv(env);
    IBP_VPRINTF(200, env, ("Do a Panda MP send async to %d size %d\n",
		cpu, len));
    MPI_Isend(buf, len, MPI_PACKED, cpu, tag, MPI_COMM_WORLD,
	      &finished_req[i_finished);
    ibmpi_unset_JNIEnv();
}


void
ibmpi_mp_init(JNIEnv *env)
{
    ibmpi_poll_register(ibmpi_mp_poll);
}


void
ibmpi_mp_end(JNIEnv *env)
{
    ibmpi_mp_poll(env);
}

/*
 * Code shared by natives for package ibis.ipl.impl.messagePassing.mpi
 */

#include <string.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <unistd.h>
#include <stdarg.h>
#include <assert.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>

#include <jni.h>

#define USE_STDARG
#include <mpi.h>

#include "../ibis_ipl_impl_messagePassing_Ibis.h"

#include "../ibmp.h"

#include "ibp.h"
#include "ibp_mp.h"

#include "ibmpi_mp.h"


jlong
Java_ibis_ipl_impl_messagePassing_Ibis_currentTime(JNIEnv *env, jclass c)
{
    union lt {
	double	t;
	jlong	l;
    } lt;

    lt.t = MPI_Wtime();

    return lt.l;
}


jdouble
Java_ibis_ipl_impl_messagePassing_Ibis_t2d(JNIEnv *env, jclass c, jlong l)
{
    union lt {
	double	t;
	jlong	l;
    } lt;

    lt.l = l;
    return (jdouble)lt.t;
}


static ibmpi_proto_p	proto_freelist;


void *
ibp_proto_create(unsigned int size)
{
    ibmpi_proto_p	p;

    if (size <= SEND_PROTO_CACHE_SIZE) {
	p = proto_freelist;
	if (p == NULL) {
	    p = malloc(SEND_PROTO_CACHE_SIZE);
	} else {
	    proto_freelist = p->sn.next;
	}
    } else {
	p = malloc(size);
    }
    p->sn.size = size;
	
    return p;
}


void
ibp_proto_clear(void *proto)
{
    ibmpi_proto_p p = proto;

    if (p->sn.size <= SEND_PROTO_CACHE_SIZE) {
	p->sn.next = proto_freelist;
	proto_freelist = p;
    } else {
	free(proto);
    }
}


int ibp_msg_consume_left(ibp_msg_p msg)
{
    return msg->size - msg->start;
}


int ibp_msg_sender(ibp_msg_p msg)
{
    return msg->sender;
}


int
ibp_consume(JNIEnv *env, ibp_msg_p msg, void *buf, int len)
{
    if (msg->size == msg->start) {
	MPI_Status status;
	/* Use the blast channel. */

	if (MPI_Recv(buf, len, MPI_PACKED, msg->sender, msg->send_port_id,
		     MPI_COMM_WORLD, &status) != MPI_SUCCESS) {
	    ibmp_error("MPI_Recv of blast message fails");
	}
    } else {
	if (len > msg->size - msg->start) {
	    len = msg->size - msg->start;
	}

	memcpy(buf, (char *)(msg + 1) + msg->start, len);
	msg->start += len;
    }

    return len;
}


jstring
ibp_string_consume(JNIEnv *env, ibp_msg_p msg, int len)
{
    char	buf[len + 1];

    ibp_consume(env, msg, buf, len);
    buf[len] = '\0';
    IBP_VPRINTF(400, env, ("Msg consume string[%d] = \"%s\"\n", len, (char *)buf));
    return (*env)->NewStringUTF(env, buf);
}


int
ibp_string_push(JNIEnv *env, jstring s, pan_iovec_p iov)
{
    iov->len = (int)(*env)->GetStringUTFLength(env, s);
    iov->data = (void *)(*env)->GetStringUTFChars(env, s, NULL);
    IBP_VPRINTF(400, env, ("Msg push string[%d] = \"%s\"\n",
			    iov->len, (char *)iov->data));

    return iov->len;
}


static char *p4_options[] = {
    "help",
    "pg",
    "dbg",
    "rdbg",
    "gm",
    "dmn",
    "out",
    "rout",
    "ssport",
    "norem",
    "log",
    "version",
    "wd",
    "amslave"
};

static int p4_option_arg[] = {
    0,	/* "help", */
    1,	/* "pg", */
    1,	/* "dbg", */
    1,	/* "rdbg", */
    1,	/* "gm", */
    1,	/* "dmn", */
    1,	/* "out", */
    1,	/* "rout", */
    1,	/* "ssport", */
    0,	/* "norem", */
    0,	/* "log", */
    0,	/* "version" */
    1,	/* "wd", */
    0 	/* "amslave" */
};


#define P4_MAX_ARGLEN	32
#define P4_OPTIONS	(sizeof(p4_options) / sizeof(*p4_options))


static char **
ibmpi_p4_args(int *argc, char *argv[])
{
    char	p4_env[P4_OPTIONS][P4_MAX_ARGLEN];
    extern char **environ;
    char      **p4_argv;
    int		p4_argc;
    int		x;
    int		i;
    int		j;

    p4_argc = *argc + 1;
    p4_argv = malloc((p4_argc + 1) * sizeof(*p4_argv));
    p4_argv[0] = "Ibis.mpi.application";
    for (i = 1; i < p4_argc; i++) {
	p4_argv[i] = strdup(argv[i - 1]);
    }
    p4_argv[i] = NULL;

    for (i = 0; i < P4_OPTIONS; i++) {
	sprintf(p4_env[i], "P4_%s", p4_options[i]);
	for (j = 0; j < strlen(p4_env[i]); j++) {
	    p4_env[i][j] = toupper(p4_env[i][j]);
	}
    }

    for (x = 0; environ[x] != NULL; x++) {
	char *e = environ[x];
	char *eq = strchr(e, '=');
	int	n;

	if (eq == NULL) {
	    fprintf(stderr, "Malformed environment var \"%s\"\n", e);
	    exit(33);
	}

	if (strncmp(e, "P4_ARG", strlen("P4_ARG")) == 0) {
	    fprintf(stderr, "Found p4 extra argument \"%s\"\n", eq + 1);
	    n = p4_argc;
	    p4_argc += 1;
	    p4_argv = realloc(p4_argv, (p4_argc + 1) * sizeof(*p4_argv));
	    p4_argv[n] = strdup(eq + 1);

	} else {
	    for (i = 0; i < P4_OPTIONS; i++) {
		if (strncmp(e, p4_env[i], eq - e) == 0) {
		    fprintf(stderr, "Found p4 option %s\n", p4_options[i]);
		    n = p4_argc;
		    p4_argc += 1 + p4_option_arg[i];
		    p4_argv = realloc(p4_argv, (p4_argc + 1) * sizeof(*p4_argv));
		    p4_argv[n] = malloc(strlen(p4_options[i]) + 4);
		    sprintf(p4_argv[n], "-p4%s", p4_options[i]);
		    if (p4_option_arg[i]) {
			p4_argv[n + 1] = strdup(eq + 1);
		    }
		}
	    }
	}
    }

    *argc = p4_argc;

    return p4_argv;
}


void
ibp_init(JNIEnv *env, int *argc, char *argv[])
{
    int		p4_argc = *argc;

    ibmp_check_ibis_name(env, "ibis.ipl.impl.messagePassing.MPIIbis");

    argv = ibmpi_p4_args(&p4_argc, argv);

    MPI_Init(&p4_argc, &argv);
    ibmpi_alive = 1;

    if (MPI_Comm_size(MPI_COMM_WORLD, &ibmp_nr) != MPI_SUCCESS) {
	ibmp_error("MPI_Comm_size() fails\n");
    }
    if (MPI_Comm_rank(MPI_COMM_WORLD, &ibmp_me) != MPI_SUCCESS) {
	ibmp_error("MPI_Comm_rank() fails\n");
    }

    IBP_VPRINTF(10, env, ("ibpi_init\n"));
}


void
ibp_start(JNIEnv *env)
{
    IBP_VPRINTF(10, env, ("%s.%d: ibp_start\n", __FILE__, __LINE__));
}


void
ibp_end(JNIEnv *env)
{
    ibmpi_alive = 0;
    MPI_Finalize();
    IBP_VPRINTF(10, env, ("%s.%d ibp_end()\n", __FILE__, __LINE__));
}

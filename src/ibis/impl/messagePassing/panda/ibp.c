/*
 * Code shared by natives for package ibis.ipl.impl.messagePassing.panda
 */

#include <string.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <unistd.h>

#include <jni.h>

#include <pan_sys.h>
#include <pan_time.h>
#include <pan_util.h>

#include "../ibmp.h"

#include "ibis_ipl_impl_messagePassing_panda_PandaIbis.h"

#include "ibp.h"
#include "ibp_mp.h"
#include "ibp_receive_port_ns_bind.h"
#include "ibp_receive_port_ns_lookup.h"
#include "ibp_receive_port_ns_unbind.h"
#include "ibp_connect.h"
#include "ibp_disconnect.h"
#include "ibp_byte_input_stream.h"
#include "ibp_byte_output_stream.h"
#include "ibp_poll.h"
#include "ibp_join.h"


jlong
Java_ibis_ipl_impl_messagePassing_panda_PandaIbis_currentTime(JNIEnv *env, jclass c)
{
    union lt {
	struct pan_time t;
	jlong	l;
    } lt;

    pan_time_get(&lt.t);

    return lt.l;
}


jdouble
Java_ibis_ipl_impl_messagePassing_panda_PandaIbis_t2d(JNIEnv *env, jclass c, jlong l)
{
    union lt {
	struct pan_time t;
	jlong	l;
    } lt;

    lt.l = l;
    return (jdouble)pan_time_t2d(&lt.t);
}


int
ibp_consume(JNIEnv *env, pan_msg_p msg, void *buf, int len)
{
    int		rd;

#ifndef NDEBUG
    if (! pan_msg_sane(msg)) {
	fprintf(stderr, "This seems an insane msg: %p\n", msg);
	ibmp_dumpStack(env);
    }
#endif

#ifndef NON_BLOCKING_CONSUME
    rd = pan_msg_consume_left(msg);
    if (rd < len) {
	len = rd;
    }
    if (len == 0) {
	return -1;
    }
#endif

    ibmp_set_JNIEnv(env);

    rd = 0;
    while (1) {
#ifndef NON_BLOCKING_CONSUME
	rd += pan_msg_consume(msg, (char *)buf + rd, (int)len);
#else
	rd += pan_msg_consume_non_blocking(msg, (char *)buf + rd, (int)len);
#endif
	if (rd == len) {
	    break;
	}
	if (pan_msg_consume_left(msg) == 0) {
	    if (rd == 0) {
		rd = -1;
	    }
	    break;
	}

	ibmp_unlock(env);
	ibmp_thread_yield(env);
	ibmp_lock(env);
    }

    ibmp_unset_JNIEnv();

    return rd;
}


jstring
ibp_string_consume(JNIEnv *env, pan_msg_p msg, int len)
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


static int
hostname_equal(char *h0, char *h1)
{
    char       *dot0;
    char       *dot1;

    if (strcmp(h0, h1) == 0) {
	return 1;
    }
    dot0 = strchr(h0, '.');
    dot1 = strchr(h1, '.');

    if (dot0 == NULL) {
	if (dot1 == NULL) {
	    return 0;
	}
	return (strncmp(h0, h1, dot1 - h1) == 0);
    }
    if (dot1 == NULL) {
	return (strncmp(h0, h1, dot0 - h0) == 0);
    }
    return 0;
}


static void
ibp_pan_init(void)
{
    int         argc;
    char       *argv[4];
    char        hostname[256];
    char        myproc[32];
    char        nprocs[32];
    int         me;
    char       *hosts;
    char       *name;
    char       *orig_hosts;
    int		i;
    struct hostent *h;
    char       *env_host_id;
    char      **fs_host = NULL;
    int		fs_nhosts = 0;
    struct in_addr *fs_host_inet;

    orig_hosts = getenv("HOSTS");
    if (orig_hosts == NULL) {
	fprintf(stderr, "HOSTS env var does not exist: use prun\n");
	exit(-6);
    }
    hosts = pan_strdup(orig_hosts);
// fprintf(stderr, "hosts copy = %s\n", hosts);

    fs_nhosts = 0;
    name = strtok(hosts, " \t");
    while (name != NULL) {
	fs_host = realloc(fs_host, (fs_nhosts + 1) * sizeof(char *));
	fs_host[fs_nhosts] = strdup(name);
	fs_nhosts++;
	name = strtok(NULL, " \t");
    }
    pan_free(hosts);
    fs_host_inet = pan_malloc(fs_nhosts * sizeof(struct in_addr));
    for (i = 0; i < fs_nhosts; i++) {
	h = gethostbyname(fs_host[i]);
	if (h == NULL) {
	    perror("gethostbyname fails");
	    exit(33);
	}
	if (h->h_length != sizeof(fs_host_inet)) {
	    pan_panic("Inet address won't fit");
	}
	memcpy(&fs_host_inet[i], h->h_addr_list[0], h->h_length);
    }

    /* Try to derive our identity from the environment RFHH */

    argc = 3;
    argv[0] = "PandaIbis_executable";
    argv[1] = myproc;
    argv[2] = nprocs;
    argv[3] = NULL;

    if (gethostname(hostname, 256) == -1) {
	exit(-5);
    }

    env_host_id = getenv("PRUN_HOST_INDEX");
    if (env_host_id == NULL) {
	me = -1;
	for (i = 0; i < fs_nhosts; i++) {
	    if (hostname_equal(fs_host[i], hostname)) {
		me = i;
		break;
	    }
	}
	if (i == fs_nhosts) {
	    fprintf(stderr, "Host name %s does not occur in HOSTS env var %s\n",
		    hostname, orig_hosts);
	    exit(-7);
	}
    } else {
	if (sscanf(env_host_id, "%d", &me) != 1) {
	    fprintf(stderr, "Host id is not a number: %s\n", env_host_id);
	    exit(-7);
	}
	if (me < 0 || me >= fs_nhosts) {
	    fprintf(stderr, "Host id is out of range: %d\n", me);
	    exit(-7);
	}
    }
    sprintf(myproc, "%d", me);
    sprintf(nprocs, "%d", fs_nhosts);

    pan_free(fs_host_inet);

// fprintf(stderr, "call pan_init(%d, %s %s %s %s)\n", argc, argv[0], argv[1], argv[2], argv[3]);
    pan_init(&argc, argv);
}


void
Java_ibis_ipl_impl_messagePassing_panda_PandaIbis_ibmp_1init(JNIEnv *env, jobject this)
{
    jfieldID	fld_PandaIbis_nrCpus;
    jfieldID	fld_PandaIbis_myCpu;

    ibmp_init(env, this);
    IBP_VPRINTF(2000, env, ("here...\n"));

    ibp_mp_init(env);
    IBP_VPRINTF(2000, env, ("here...\n"));

    fld_PandaIbis_nrCpus = (*env)->GetFieldID(env, ibmp_cls_Ibis, "nrCpus", "I");
    if (fld_PandaIbis_nrCpus == NULL) {
	fprintf(stderr, "%s.%d Cannot find static field nrCpus:I\n", __FILE__, __LINE__);
    }
    IBP_VPRINTF(2000, env, ("here...\n"));
    fld_PandaIbis_myCpu  = (*env)->GetFieldID(env, ibmp_cls_Ibis, "myCpu", "I");
    if (fld_PandaIbis_myCpu == NULL) {
	fprintf(stderr, "%s.%d Cannot find static field myCpu:I\n", __FILE__, __LINE__);
    }
    IBP_VPRINTF(2000, env, ("here...\n"));

    ibp_pan_init();
    IBP_VPRINTF(2000, env, ("here...\n"));

    (*env)->SetIntField(env, ibmp_obj_Ibis_ibis, fld_PandaIbis_nrCpus,
		(jint)pan_nr_processes());
    IBP_VPRINTF(2000, env, ("here...\n"));
    (*env)->SetIntField(env, ibmp_obj_Ibis_ibis, fld_PandaIbis_myCpu,
		(jint)pan_my_pid());
    IBP_VPRINTF(2000, env, ("here...\n"));

    ibp_join_init(env);
    IBP_VPRINTF(2000, env, ("here...\n"));

    ibp_receive_port_ns_bind_init(env);
    IBP_VPRINTF(2000, env, ("here...\n"));
    ibp_receive_port_ns_lookup_init(env);
    IBP_VPRINTF(2000, env, ("here...\n"));
    ibp_receive_port_ns_unbind_init(env);
    IBP_VPRINTF(2000, env, ("here...\n"));

    ibp_connect_init(env);
    IBP_VPRINTF(2000, env, ("here...\n"));
    ibp_disconnect_init(env);
    IBP_VPRINTF(2000, env, ("here...\n"));
    ibp_byte_output_stream_init(env);
    IBP_VPRINTF(2000, env, ("here...\n"));
    ibp_byte_input_stream_init(env);
    IBP_VPRINTF(2000, env, ("here...\n"));

    ibp_poll_init(env);
    IBP_VPRINTF(2000, env, ("here...\n"));
}


void
Java_ibis_ipl_impl_messagePassing_panda_PandaIbis_ibmp_1start(JNIEnv *env, jobject this)
{
    pan_start();
    IBP_VPRINTF(10, env, ("%s.%d: ibp_start\n", __FILE__, __LINE__));
}


void
Java_ibis_ipl_impl_messagePassing_panda_PandaIbis_ibmp_1end(JNIEnv *env, jobject this)
{
    IBP_VPRINTF(10, env, ("%s.%d ibp_end()\n", __FILE__, __LINE__));

    ibp_mp_end(env);
    IBP_VPRINTF(10, env, ("%s.%d ibp_end()\n", __FILE__, __LINE__));
    ibp_poll_end(env);

    IBP_VPRINTF(10, env, ("%s.%d ibp_end()\n", __FILE__, __LINE__));
    ibp_byte_input_stream_end(env);
    IBP_VPRINTF(10, env, ("%s.%d ibp_end()\n", __FILE__, __LINE__));
    ibp_byte_output_stream_end(env);
    IBP_VPRINTF(10, env, ("%s.%d ibp_end()\n", __FILE__, __LINE__));
    ibp_disconnect_end(env);
    IBP_VPRINTF(10, env, ("%s.%d ibp_end()\n", __FILE__, __LINE__));
    ibp_connect_end(env);
    IBP_VPRINTF(10, env, ("%s.%d ibp_end()\n", __FILE__, __LINE__));

    ibp_receive_port_ns_bind_end(env);
    ibp_receive_port_ns_lookup_end(env);
    ibp_receive_port_ns_unbind_end(env);
    IBP_VPRINTF(10, env, ("%s.%d ibp_end()\n", __FILE__, __LINE__));

    ibp_join_end(env);

    ibmp_end(env, this);

    pan_end();
    IBP_VPRINTF(10, env, ("%s.%d ibp_end()\n", __FILE__, __LINE__));
}

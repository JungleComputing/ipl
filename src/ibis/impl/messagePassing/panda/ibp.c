/*
 * Code for Panda natives for package ibis.ipl.impl.messagePassing
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

#include "../ibis_ipl_impl_messagePassing_Ibis.h"

#include "../ibmp.h"

#include "ibp.h"
#include "ibp_mp.h"

#include "ibp_env.h"

#ifndef NDEBUG
pan_key_p		ibp_env_key;
#endif

JNIEnv  *ibp_JNIEnv = NULL;


void
ibp_msg_clear(JNIEnv *env, ibp_msg_p msg)
{
    ibp_set_JNIEnv(env);
    pan_msg_clear((pan_msg_p)msg);
    ibp_unset_JNIEnv();
}


int
ibp_msg_consume_left(ibp_msg_p msg)
{
    return pan_msg_consume_left((pan_msg_p)msg);
}


int
ibp_msg_sender(ibp_msg_p msg)
{
    return pan_msg_sender((pan_msg_p)msg);
}


void *
ibp_proto_create(unsigned int size)
{
    return pan_proto_create(size);
}


void
ibp_proto_clear(void *proto)
{
    pan_proto_clear(proto);
}


jlong
Java_ibis_ipl_impl_messagePassing_Ibis_currentTime(JNIEnv *env, jclass c)
{
    union lt {
	struct pan_time t;
	jlong	l;
    } lt;

    pan_time_get(&lt.t);

    return lt.l;
}


jdouble
Java_ibis_ipl_impl_messagePassing_Ibis_t2d(JNIEnv *env, jclass c, jlong l)
{
    union lt {
	struct pan_time t;
	jlong	l;
    } lt;

    lt.l = l;

    return (jdouble)pan_time_t2d(&lt.t);
}


int
ibp_consume(JNIEnv *env, ibp_msg_p msg, void *buf, int len)
{
    int		rd;

#ifndef NDEBUG
    extern int pan_msg_sane(pan_msg_p);

    if (! pan_msg_sane((pan_msg_p)msg)) {
	ibmp_error(env, "This seems an insane msg: %p\n", msg);
    }
#endif

#ifndef NON_BLOCKING_CONSUME
    rd = pan_msg_consume_left((pan_msg_p)msg);
    if (rd < len) {
	len = rd;
    }
    if (len == 0) {
	return -1;
    }
#endif

    ibp_set_JNIEnv(env);

    rd = 0;
    while (1) {
#ifndef NON_BLOCKING_CONSUME
	rd += pan_msg_consume((pan_msg_p)msg, (char *)buf + rd, (int)len);
#else
	rd += pan_msg_consume_non_blocking((pan_msg_p)msg, (char *)buf + rd, (int)len);
#endif
	if (rd == len) {
	    break;
	}
	if (pan_msg_consume_left((pan_msg_p)msg) == 0) {
	    if (rd == 0) {
		rd = -1;
	    }
	    break;
	}

	// ibp_unset_JNIEnv();
	ibmp_unlock(env);
	ibmp_thread_yield(env);
	ibmp_lock(env);
	// ibp_set_JNIEnv(env);
    }

    ibp_unset_JNIEnv();

    return rd;
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
ibp_pan_init(JNIEnv *env, int *java_argc, char **java_argv)
{
    int         argc;
    char       *argv[*java_argc + 4];
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
	ibmp_error(env, "HOSTS env var does not exist: use prun\n");
    }
    hosts = strdup(orig_hosts);

    fs_nhosts = 0;
    name = strtok(hosts, " \t");
    while (name != NULL) {
	fs_host = realloc(fs_host, (fs_nhosts + 1) * sizeof(char *));
	fs_host[fs_nhosts] = strdup(name);
	fs_nhosts++;
	name = strtok(NULL, " \t");
    }
    free(hosts);
    fs_host_inet = malloc(fs_nhosts * sizeof(struct in_addr));
    for (i = 0; i < fs_nhosts; i++) {
	h = gethostbyname(fs_host[i]);
	if (h == NULL) {
	    ibmp_error(env, "gethostbyname fails");
	}
	if (h->h_length != sizeof(fs_host_inet)) {
	    pan_panic("Inet address won't fit");
	}
	memcpy(&fs_host_inet[i], h->h_addr_list[0], h->h_length);
    }

    /* Try to derive our identity from the environment RFHH */

    argv[0] = "PandaIbis_executable";
    argv[1] = myproc;
    argv[2] = nprocs;
    for (i = 0; i < *java_argc + 1; i++) {
	argv[i + 3] = java_argv[i];
    }
    argc = *java_argc + 3;

    if (gethostname(hostname, 256) == -1) {
	ibmp_error(env, "Cannot get hostname");
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
	    ibmp_error(env, "Host name %s does not occur in HOSTS env var %s\n",
			hostname, orig_hosts);
	}
    } else {
	if (sscanf(env_host_id, "%d", &me) != 1) {
	    ibmp_error(env, "Host id is not a number: %s\n", env_host_id);
	}
	if (me < 0 || me >= fs_nhosts) {
	    ibmp_error(env, "Host id is out of range: %d\n", me);
	}
    }
    sprintf(myproc, "%d", me);
    sprintf(nprocs, "%d", fs_nhosts);

    free(fs_host_inet);

    pan_init(&argc, argv);

    for (i = 3; i < argc; i++) {
	java_argv[i - 3] = argv[i];
    }
    *java_argc = argc - 3;
}


void
ibp_init(JNIEnv *env, int *argc, char *argv[])
{
    ibmp_check_ibis_name(env, "ibis.ipl.impl.messagePassing.PandaIbis");

    ibp_pan_init(env, argc, argv);
    IBP_VPRINTF(2000, env, ("here...\n"));

    ibmp_nr = pan_nr_processes();
    ibmp_me = pan_my_pid();

#ifndef NDEBUG
    ibp_env_key = pan_key_create();
#endif
}


void
ibp_start(JNIEnv *env)
{
    pan_start();
    IBP_VPRINTF(10, env, ("%s.%d: ibp_start\n", __FILE__, __LINE__));
}


void
ibp_end(JNIEnv *env)
{
    IBP_VPRINTF(10, env, ("%s.%d ibp_end()\n", __FILE__, __LINE__));
    pan_end();
}

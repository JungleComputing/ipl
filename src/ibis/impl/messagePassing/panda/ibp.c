/*
 * Code for Panda natives for package ibis.impl.messagePassing
 */

#include <string.h>
#ifdef _M_IX86
#include <winsock2.h>
#else
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <unistd.h>
#endif

#include <jni.h>

#include <pan_sys.h>
#include <pan_time.h>
#include <pan_util.h>

#include "ibis_impl_messagePassing_Ibis.h"

#include "../ibmp.h"

#include "ibp.h"
#include "ibp_mp.h"


#define ATTACH_THREAD_OVER_CALLS	1


/*
 * Since JNI 1.4 native threads can be attached as daemons.
 * That would make things faster & still allow termination.
 */
#if ATTACH_THREAD_OVER_CALLS
#define ATTACH_THREAD(vm, penv, arg) \
	AttachCurrentThreadAsDaemon(vm, penv, arg)
#else
#define ATTACH_THREAD(vm, penv, arg) \
	AttachCurrentThread(vm, penv, arg)
#endif


static int	ibp_intr_enabled = 1;


int		ibp_me;
int		ibp_nr;

int		ibp_intpts = 0;


int
ibp_pid_me(void)
{
    return ibp_me;
}


int
ibp_pid_nr(void)
{
    ibp_nr = pan_nr_processes();
}


void
ibp_msg_clear(JNIEnv *env, ibp_msg_p msg)
{
    ibp_set_JNIEnv(env);
    pan_msg_clear((pan_msg_p)msg);
    assert(env == ibp_JNIEnv);
}


ibp_msg_p
ibp_msg_clone(JNIEnv *env, ibp_msg_p msg, void **proto)
{
    return (ibp_msg_p)pan_msg_clone((pan_msg_p)msg, proto);
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


JNIEXPORT jlong JNICALL
Java_ibis_impl_messagePassing_Ibis_currentTime(JNIEnv *env, jclass c)
{
    union lt {
	struct pan_time t;
	jlong	l;
    } lt;

    pan_time_get(&lt.t);

    return lt.l;
}


JNIEXPORT jdouble JNICALL
Java_ibis_impl_messagePassing_Ibis_t2d(JNIEnv *env, jclass c, jlong l)
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
	rd += pan_msg_consume((pan_msg_p)msg, (char *)buf + rd, len - rd);
#else
	rd += pan_msg_consume_non_blocking((pan_msg_p)msg, (char *)buf + rd, len - rd);
#endif
assert(ibp_JNIEnv == env);
	if (rd == len) {
	    break;
	}
	if (pan_msg_consume_left((pan_msg_p)msg) == 0) {
	    if (rd == 0) {
		rd = -1;
	    }
	    break;
	}

	ibmp_unlock(env);
	ibmp_thread_yield(env);
	ibmp_lock(env);
    }

    assert(ibp_JNIEnv == env);

    return rd;
}


jstring
ibp_string_consume(JNIEnv *env, ibp_msg_p msg, int len)
{
#define STACK_STRING	    1024
    char	stack_buf[STACK_STRING + 1];
    char       *buf;
    jstring	str;

    if (len > STACK_STRING) {
	buf = malloc(len + 1);
    } else {
	buf = stack_buf;
    }

    ibp_consume(env, msg, buf, len);
assert(ibp_JNIEnv == env);
    buf[len] = '\0';
    IBP_VPRINTF(400, env, ("Msg %p consume string[%d] = \"%s\"\n", msg, len, (char *)buf));
    str = (*env)->NewStringUTF(env, buf);

    if (len > STACK_STRING) {
	free(buf);
    }

#undef STACK_STRING

    return str;
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


jbyteArray
ibp_byte_array_consume(JNIEnv *env, ibp_msg_p msg, int len)
{
    jbyteArray	a = (*env)->NewByteArray(env, len);
#define STACK_STRING	    1024
    jbyte	stack_buf[STACK_STRING];
    jbyte      *buf;

    if (a == NULL) {
	fprintf(stderr, "Created a NULL byte array[%d] object from native\n",
		len);
	abort();
    }

    if (len > STACK_STRING) {
	buf = malloc(len);
    } else {
	buf = stack_buf;
    }

    ibp_consume(env, msg, buf, len);
assert(ibp_JNIEnv == env);

    (*env)->SetByteArrayRegion(env, a, 0, len, buf);
assert(ibp_JNIEnv == env);

    if (len > STACK_STRING) {
	free(buf);
    }
#undef STACK_STRING

    return a;
}


int
ibp_byte_array_push(JNIEnv *env, jbyteArray a, pan_iovec_p iov)
{
    iov->len  = (int)(*env)->GetArrayLength(env, a);
    iov->data = (*env)->GetByteArrayElements(env, a, NULL);

    return iov->len;
}

#ifdef _M_IX86
#define IBP_NO_INTERRUPTS	1
#endif

#if IBP_NO_INTERRUPTS

static void
ibp_intr_poll(void)
{
}

static void
ibp_intr_lock(void)
{
}

static void
ibp_intr_unlock(void)
{
}

#else		/* IBP_NO_INTERRUPTS */

#define INTERRUPTS_AS_UPCALLS	1

#if INTERRUPTS_AS_UPCALLS

static int	intpt_running = 0;


static JavaVM *
current_VM(void)
{
#define MAX_VM_NUM	16
    JavaVM *VM[MAX_VM_NUM];
    int	nVMs;

    if (JNI_GetCreatedJavaVMs(VM, MAX_VM_NUM, &nVMs) != 0) {
	fprintf(stderr, "JNI_GetCreatedJavaVMs fails\n");
	abort();
    }
    if (nVMs == 0) {
	fprintf(stderr, "No VM!\n");
	abort();
    }
    if (nVMs > 1) {
	fprintf(stderr, "%d VMs alive, choose only the first\n", nVMs);
    }

    return VM[0];
}


static JNIEnv *
intpt_env_create(void)
{
    JNIEnv *env;
    JavaVM     *vm = current_VM();

    if ((*vm)->ATTACH_THREAD(vm, (void *)&env, NULL) != 0) {
	fprintf(stderr, "AttachCurrentThread fails\n");
	abort();
    }
#if ATTACH_THREAD_OVER_CALLS
    if (ibp_me == 0) {
	fprintf(stderr, "%2d: Hand out intpt thread JNIEnv * %p\n",
		ibp_me, env);
    }
#endif

    return env;
}


static int
intpt_env_check(JNIEnv *env)
{
    JNIEnv     *intpt_env;
    jint	r;
    JavaVM     *vm = current_VM();

    r = (*vm)->GetEnv(vm, (void *)&intpt_env, JNI_VERSION_1_2);
    if (r != JNI_OK) {
	fprintf(stderr, "GetEnv returns %d\n", r);
	abort();
    }

    return (intpt_env == env);
}


static JNIEnv *intpt_JNIEnv = NULL;


static JNIEnv *
intpt_env_get(void)
{
    if (intpt_running++ != 0) {
	fprintf(stderr,
		"At env_get: some other interrupt handler active -- abort\n");
	abort();
    }

    if (intpt_JNIEnv == NULL) {
	intpt_JNIEnv = intpt_env_create();
    }
    assert(intpt_env_check(intpt_JNIEnv));

    return intpt_JNIEnv;
}


static void
intpt_env_release(JNIEnv *env)
{
    if (--intpt_running != 0) {
	fprintf(stderr,
		"At env_release: some other interrupt handler active -- abort\n");
	abort();
    }

#if ! ATTACH_THREAD_OVER_CALLS
    intpt_JNIEnv = NULL;
    {
	JavaVM *vm = current_VM();
	if ((*vm)->DetachCurrentThread(vm) != 0) {
	    fprintf(stderr, "DetachCurrentThread fails\n");
	    abort();
	}
    }
#endif
}

#define POLLS_PER_INTERRUPT	1	/* 4 */

static void
ibp_intr_poll(void)
{
    JNIEnv *env = intpt_env_get();
    int		i;

    IBP_VPRINTF(410, env, ("start intpt %d\n", ibp_me, ibp_intpts));
    ibp_intpts++;

    IBP_VPRINTF(2000, env, ("interrupt...\n"));

    ibmp_lock_check_owned(env);
    for (i = 0; i < POLLS_PER_INTERRUPT; i++) {
	IBP_VPRINTF(2100, env, ("Do a poll[%d] from interrupt handler\n", i));
	while (ibmp_poll(env));
    }
    IBP_VPRINTF(410, env, ("finish intpt %d\n", ibp_me, ibp_intpts - 1));

    intpt_env_release(env);
}


#ifdef MANTA_JNI
#include "../manta_rts_lock.h"
#endif


static void
ibp_intr_lock(void)
{
    JNIEnv *env = intpt_env_get();

    IBP_VPRINTF(2100, env, ("Enter interrupt LOCK\n"));

#ifdef MANTA_JNI
    MANTA_RTS_POST_BLOCK_SYS_CALL(thread_list_current_thread());
#endif

    ibmp_lock(env);

    intpt_env_release(env);
}


static void
ibp_intr_unlock(void)
{
    JNIEnv *env = intpt_env_get();

    IBP_VPRINTF(2100, env, ("Enter interrupt UNLOCK\n"));

    ibmp_unlock(env);

#ifdef MANTA_JNI
    MANTA_RTS_PRE_BLOCK_SYS_CALL(thread_list_current_thread());
#endif

    intpt_env_release(env);
}

#else		/* INTERRUPTS_AS_UPCALLS */

#include <sys/types.h>
#include <signal.h>
#include <errno.h>
#include <unistd.h>

#include <pthread.h>

/*
 * This should be handled inside LFC: the first thread that switches on
 * interrupts should be the thread that requests the SIGIOs.
 * There, we are also aware of using pthreads (which we are not, here,
 * really).
 */
static void
ibp_intr_poll(void)
{
    int		r;
    extern pthread_t ibmp_sigcatcher_pthread;

    fprintf(stderr, "Interrupt thread: gonna throw a SIGIO to myself\n");
    fflush(stderr);

    if ((r = pthread_kill(ibmp_sigcatcher_pthread, SIGIO)) != 0) {
	fprintf(stderr, "ibp_intr_poll->kill(SIGIO) fails, status %d errno %d\n", r, errno);
	perror("kill(SIGIO) fails");
    }
}


static void
ibp_intr_lock(void)
{
}


static void
ibp_intr_unlock(void)
{
}

#endif

#endif		/* IBP_NO_INTERRUPTS */


void
ibp_intr_enable(JNIEnv *env)
{
    pan_comm_intr_enable();
}


void
ibp_intr_disable(JNIEnv *env)
{
    pan_comm_intr_disable();
}




void
ibp_report(JNIEnv *env, FILE *f)
{
#if INTERRUPTS_AS_UPCALLS
    /* Interrupt prints are handled one level up */
    if (0) {
	fprintf(f, "intpts %d ", ibp_intpts);
    }
#endif
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
    char      **argv = malloc((*java_argc + 4) * sizeof(*argv));
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
	orig_hosts = getenv("PRUN_HOSTNAMES");
	if (orig_hosts == NULL) {
	    ibmp_error(env, "HOSTS env var does not exist: use prun\n");
	    return;
	}
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

#ifdef _M_IX86
    {
	WSADATA wsaData;
	WORD wVersion = MAKEWORD(2, 2);
	int err;

	if ((err = WSAStartup(wVersion, &wsaData)) != 0) {
	    fprintf(stderr, "WSAStartup fails, error %d\n", err);
	}
    }
#endif

    fs_host_inet = malloc(fs_nhosts * sizeof(struct in_addr));
    for (i = 0; i < fs_nhosts; i++) {
	h = gethostbyname(fs_host[i]);
	if (h == NULL) {
	    ibmp_error(env, "gethostbyname fails");
	    return;
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
	return;
    }

    env_host_id = getenv("PRUN_CPU_RANK");
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
	    return;
	}
    } else {
	if (sscanf(env_host_id, "%d", &me) != 1) {
	    ibmp_error(env, "Host id is not a number: %s\n", env_host_id);
	    return;
	}
	if (me < 0 || me >= fs_nhosts) {
	    ibmp_error(env, "Host id is out of range: %d\n", me);
	    return;
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

    free(argv);
}


void
ibp_init(JNIEnv *env, int *argc, char *argv[])
{
    IBP_VPRINTF(2000, env, ("here...\n"));

    ibmp_check_ibis_name(env, "ibis.impl.messagePassing.PandaIbis");
    IBP_VPRINTF(2000, env, ("here...\n"));

    ibp_pan_init(env, argc, argv);
    IBP_VPRINTF(2000, env, ("here...\n"));

    if (pan_arg_bool(argc, argv, "-ibp-no-intr") == 1) {
	ibp_intr_enabled = 0;
    }
    if (pan_arg_bool(argc, argv, "-ibp-intr") == 1) {
	ibp_intr_enabled = 1;
    }

    ibp_nr = pan_nr_processes();
    ibp_me = pan_my_pid();

    if (ibp_intr_enabled) {
	void pan_comm_intr_register(void (*)(void), void (*)(void), void (*)(void));

	pan_comm_intr_register(ibp_intr_poll, ibp_intr_lock, ibp_intr_unlock);
    }
}


void
ibp_start(JNIEnv *env)
{
    pan_start();
    IBP_VPRINTF(10, env, ("%s.%d: ibp_start\n", __FILE__, __LINE__));

    if (ibp_intr_enabled) {
	if (ibp_me == 0) {
	    fprintf(stderr, "Enable Panda interrupts\n");
	}
	pan_comm_intr_enable();
    } else {
	if (ibp_me == 0) {
	    fprintf(stderr, "Don't enable Panda interrupts (yet)\n");
	}
    }
}


void
ibp_end(JNIEnv *env)
{
    ibp_report(env, stdout);
    IBP_VPRINTF(10, env, ("%s.%d ibp_end()\n", __FILE__, __LINE__));
    pan_comm_intr_disable();
    IBP_VPRINTF(2000, env, ("here...\n"));
    pan_end();
    IBP_VPRINTF(2000, env, ("here...\n"));
}

/* #define NDEBUG */

#include <jni.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stddef.h>
#include <assert.h>

#include "java_properties.h"

#define DEPRECATED		0

/* How do we easily configure this in a portable way? */
#define GM_ENABLE_HERALDS	0	/* 1 */

#include <gm.h>

#define HOSTNAMELEN	1024

static char hostname[HOSTNAMELEN];

#ifndef NDEBUG

#if REPLACE_DEFINITION
#undef assert
#define assert(c) \
((c) ? 1 : do_assert(__FILE__, __LINE__, #c), 1)

#include <unistd.h>

static int
do_assert(char *file, int line, char *c)
{
    fprintf(stderr, "%s.%d: Assertion failed: %s\n",
		   file, line, c);

    while (1) {
	fprintf(stderr, "%s %d: attach me please\n", hostname, getpid());
	sleep(1);
    }
}

#else

void __assert_fail(__const char *__assertion, __const char *__file,
		   unsigned int __line, __const char *__function)
{
    fprintf(stderr, "%s.%d: Assertion failed in %s: %s\n",
		   __file, __line, __function, __assertion);

    while (1) {
	fprintf(stderr, "%s %d: attach me please\n", hostname, getpid());
	sleep(1);
    }
}

#endif

#endif

#ifdef __BORLANDC__
#pragma warn . 8008
// #pragma warn . 8027
#pragma warn . 8066
#pragma warn . 8071
// #pragma warn . 8080

#pragma warn - 8057
#endif

#include "ibis_impl_net_gm_Driver.h"
#include "ibis_impl_net_gm_GmInput.h"
#include "ibis_impl_net_gm_GmOutput.h"


#if defined _M_IX86
#define EXCEPTIONS_WORK		0
#else
#define EXCEPTIONS_WORK		1
#endif

/*
 *  Macros
 */
#if 0
#define __RDTSC__ ({jlong time;__asm__ __volatile__ ("rdtsc" : "=A" (time));time;})
#else
#define __RDTSC__ 0LL
#endif


#define MIN(x, y)	((x) < (y) ? (x) : (y))


/* Debugging macros */
#if 0
#ifdef __GNUC__
#define __trace__(s, p...) fprintf(stderr, "[%Ld]:%s:%d: "s"\n", __RDTSC__, __FUNCTION__, __LINE__ , ## p)
#else
#define __trace(s) fprintf(stderr, "[%Ld]:%s:%d: "s"\n", __RDTSC__, __FUNCTION__, __LINE__)
#endif
#else
#ifdef __GNUC__
#define __trace__(s, p...)
#else
#define __trace(s)
#endif
#endif

#if 0
#ifdef __GNUC__
#define __disp__(s, p...) fprintf(stderr, "[%Ld]:%s:%d: "s"\n", __RDTSC__, __FUNCTION__, __LINE__ , ## p)
#else
static void
__disp__(const char *s, ...)
{
}
#endif
#define __in__()          fprintf(stderr, "[%Ld]:%s:%d: -->\n", __RDTSC__, __FUNCTION__, __LINE__)
#define __out__()         fprintf(stderr, "[%Ld]:%s:%d: <--\n", __RDTSC__, __FUNCTION__, __LINE__)
#define __err__()         fprintf(stderr, "[%Ld]:%s:%d: <!!\n", __RDTSC__, __FUNCTION__, __LINE__)
#else
#ifdef __GNUC__
#define __disp__(s, p...)
#else
static void
__disp__(const char *s, ...)
{
}
#endif
#define __in__()
#define __out__()
#define __err__()
#endif

#ifndef NDEBUG
#define VERBOSE		1
#else
#define VERBOSE		0
#endif


#if VERBOSE

#include <stdarg.h>

static int	verbose		= 0;

static int
stderr_printf(char *fmt, ...)
{
    va_list     ap;
    int         ret;

    va_start(ap, fmt);
    ret = vfprintf(stderr, fmt, ap);
    va_end(ap);
    fflush(stderr);

    return ret;
}

#define VPRINTF(n, msg) \
		do { \
		    if ((n) <= verbose) { \
			fprintf(stderr, "%s.%d: ", __FILE__, __LINE__); \
			stderr_printf msg; \
		    } \
		} while (0)
#define VPRINTSTR(n, msg) \
		do { \
		    if ((n) <= verbose) { \
			stderr_printf msg; \
		    } \
		} while (0)
#else
#define VPRINTF(n, msg)
#define VPRINTSTR(n, msg)
#endif

/* Error message macros */
#if 1
#ifdef __GNUC__
#define __error__(s, p...) fprintf(stderr, "[%Ld]:%s:%d: *error* "s"\n", __RDTSC__, __FUNCTION__, __LINE__ , ## p)
#else
static void
__error__(const char *s, ...)
{
}
#endif
#else
#ifdef __GNUC__
#define __error__(s, p...)
#else
static void
__error__(const char *s, ...)
{
}
#endif
#endif


/*
 *  Constants
 */

/* Cache settings */
#define CACHE_SIZE 10
#define CACHE_GRANULARITY 0x1000

/* The name of the NetIbis exception. */
#define NI_IBIS_EXCEPTION  "java.io.IOException"

/* The minimal valid GM port ID.*/
#define NI_GM_MIN_PORT_NUM 2

/* The maximal atomic block size. */
#define NI_GM_MAX_BLOCK_LEN    (2*1024*1024)

static int NI_GM_BLOCK_SIZE;


typedef enum NI_GM_MSG_TYPE {
    NI_GM_MSG_TYPE_NONE,
    NI_GM_MSG_TYPE_CONNECT,
    NI_GM_MSG_TYPE_EAGER,
    NI_GM_MSG_TYPE_RENDEZ_VOUS_REQ,
    NI_GM_MSG_TYPE_RENDEZ_VOUS_ACK,
    NI_GM_MSG_TYPE_RENDEZ_VOUS_DATA,
    NI_GM_MSG_TYPE_N
} ni_gm_msg_type_t, *ni_gm_msg_type_p;


static char *
ni_gm_msg_type(ni_gm_msg_type_t type)
{
    switch (type) {
    case NI_GM_MSG_TYPE_NONE:
	return "NI_GM_MSG_TYPE_NONE";
    case NI_GM_MSG_TYPE_CONNECT:
	return "NI_GM_MSG_TYPE_CONNECT";
    case NI_GM_MSG_TYPE_EAGER:
	return "NI_GM_MSG_TYPE_EAGER";
    case NI_GM_MSG_TYPE_RENDEZ_VOUS_REQ:
	return "NI_GM_MSG_TYPE_RENDEZ_VOUS_REQ";
    case NI_GM_MSG_TYPE_RENDEZ_VOUS_ACK:
	return "NI_GM_MSG_TYPE_RENDEZ_VOUS_ACK";
    case NI_GM_MSG_TYPE_RENDEZ_VOUS_DATA:
	return "NI_GM_MSG_TYPE_RENDEZ_VOUS_DATA";
    case NI_GM_MSG_TYPE_N:
	return "NI_GM_MSG_TYPE_N";
    }
}

typedef struct NI_GM_HDR {
    ni_gm_msg_type_t	type;
    int			length;
    int			mux_id;
    int			byte_buffer_offset;
#ifndef NDEBUG
    long long unsigned seqno;
#endif
} ni_gm_hdr_t, *ni_gm_hdr_p;

#define NI_GM_PACKET_HDR_LEN	((int)sizeof(ni_gm_hdr_t))
#define NI_GM_PACKET_BODY_LEN	ibis_impl_net_gm_Driver_packetMTU
#define NI_GM_PACKET_LEN	(NI_GM_PACKET_HDR_LEN + NI_GM_PACKET_BODY_LEN)
#define NI_GM_BYTE_BUFFER_OFFSET \
				((int)offsetof(ni_gm_hdr_t, byte_buffer_offset))

#define NI_GM_MIN_PACKETS	12
#define NI_GM_MAX_PACKETS	18


static int JNI_RENDEZ_VOUS_REQUEST;


/*
 *  Types
 */

typedef enum {
        E_UNKNOWN = 0,
        E_BUFFER,
        E_BOOLEAN,
        E_BYTE,
        E_SHORT,
        E_CHAR,
        E_INT,
        E_LONG,
        E_FLOAT,
        E_DOUBLE,
} e_type;

/* The driver */
#if 0
struct s_mutex {
        jobject   ref;
        jmethodID unlock_id;
};
#endif

struct s_lock {
        jobject   ref;
        jmethodID lock_id;
        jmethodID unlock_id;
	jfieldID	lock_array;
	jfieldID	lock_array_value;
	jfieldID	lock_array_front;
        jint      id;
};

struct s_access_lock {
        jobject   ref;
        jmethodID lock_id;
        jmethodID unlock_id;
        jboolean  priority;
};

struct s_drv {
	int                    ref_count;
        int                    nb_dev;
        struct s_dev         **pp_dev;
};

struct s_request {
        struct s_port   *p_port;
        struct s_output *p_out;
        struct s_input  *p_in;
        gm_status_t      status;
};

/* A Myricom NIC. */
struct s_dev {
	int            id;
        struct s_port *p_port;
	int            ref_count;
        struct s_drv  *p_drv;
};

struct s_packet {
        unsigned char *data;
        struct s_packet *next;
        struct s_packet *previous;
	struct s_output *p_out;
};

/* A NIC port. */
struct s_port {
        struct s_cache   *cache_head;
	struct gm_port   *p_gm_port;
	int               port_id;
	unsigned int      node_id;
	struct s_dev     *p_dev;
	int               ref_count;

        struct s_input  **local_input_array;
        int               local_input_array_size;

        struct s_output **local_output_array;
        int               local_output_array_size;

        struct s_input   *active_input;

        unsigned int      packet_size;

        struct s_packet  *packet_head;
        int               nb_packets;
        struct s_packet  *send_packet_cache;

	int		ni_gm_send_tokens;
};

union u_j_array {
        jbyteArray	j_buffer;
        jbooleanArray	j_boolean;
        jbyteArray	j_byte;
        jshortArray	j_short;
        jcharArray	j_char;
        jintArray	j_int;
        jlongArray	j_long;
        jfloatArray	j_float;
        jdoubleArray	j_double;
};


typedef enum NI_GM_SEND_STATE {
    NI_GM_SENDER_IDLE,
    NI_GM_SENDER_SENDING_EAGER,
    NI_GM_SENDER_SENDING_RNDZVS_REQ,
    NI_GM_SENDER_SENDING_RNDZVS_DATA,
    NI_GM_SENDER_SENDING_RNDZVS_ACK,
    NI_GM_SENDER_STATES
} ni_gm_send_state_t;


static char *
ni_gm_sender_state(ni_gm_send_state_t state)
{
    switch (state) {
	case NI_GM_SENDER_IDLE:
	    return "NI_GM_SENDER_IDLE";
	case NI_GM_SENDER_SENDING_EAGER:
	    return "NI_GM_SENDER_SENDING_EAGER";
	case NI_GM_SENDER_SENDING_RNDZVS_REQ:
	    return "NI_GM_SENDER_SENDING_RNDZVS_REQ";
	case NI_GM_SENDER_SENDING_RNDZVS_DATA:
	    return "NI_GM_SENDER_SENDING_RNDZVS_DATA";
	case NI_GM_SENDER_SENDING_RNDZVS_ACK:
	    return "NI_GM_SENDER_SENDING_RNDZVS_ACK";
	case NI_GM_SENDER_STATES:
	    return "NI_GM_SENDER_STATES";
    }
}


/* NetIbis output internal information. */
struct s_output {
        struct s_lock    *p_lock;
        struct s_cache   *p_cache;
        e_type            type;
        union u_j_array   java;
        void             *array;
	int               offset;	// Used for packetized buffers
	int               byte_buffer;	// Used for byte buffers within packetized buffers
        int               length;
	int               is_copy;
	struct s_port    *p_port;
	int               dst_port_id;
	unsigned int      dst_node_id;
        int               local_mux_id;
        int               remote_mux_id;
        struct s_packet  *packet;
        unsigned int      packet_size;
        ni_gm_send_state_t	state;
	int               ack_arrived;	/* This should fold into a state update */
	long long unsigned seqno;
        struct s_request  request;
};

/* NetIbis input internal information. */
struct s_input {
        jobject           ref;
        jfieldID          len_id;
        struct   s_lock  *p_lock;
        struct   s_cache *p_cache;
        volatile int      data_available;
        e_type            type;
        union u_j_array   java;
        void             *array;
	struct s_packet  *packet;	// Used for packetized buffers
	int               offset;	// Used for packetized buffers
	int               data_size;	// Used for packetized buffers
	int               data_left;	// Used for packetized buffers
	int               byte_buffer_consumed;	// Used for packetized buffers
        int               length;	// Used for rendez-vous data size
	int               is_copy;
	struct   s_port  *p_port;
	int               src_port_id;
	unsigned int      src_node_id;
        int               local_mux_id;
        int               remote_mux_id;
        unsigned char    *ack_packet;
        unsigned int      packet_size;
        struct s_packet  *packet_head;
	long long unsigned seqno;
        struct s_request  request;
};

struct s_cache {
        unsigned char  *ptr;
        int             len;
        int             ref_count;
        struct s_cache *next;
};


/* Union used for conversions between pointers and handles. */
union u_conv {
        jlong  handle;
        void  *ptr;
};

/*
 *  Static variables
 */

/* Flag indicating whether the driver has been initialized. */
static int		initialized              =    0;

/* Flag indicating whether the initialization of GM was successful. */
static int		successfully_initialized =    0;

static volatile int	success_flag             =    0;

/* Driver's own data structure.  */
static struct s_drv    *volatile ni_gm_p_drv = NULL;
static struct s_access_lock *ni_gm_access_lock;

static JavaVM	       *_p_vm = NULL;
static JNIEnv	       *_current_env = NULL;

static const int	pub_port_array[] = { 2, 4, 5, 6, 7 };
static const int	nb_pub_ports   = 5;

static int		mtu; /* Read this from Driver.mtu - should be immutable */

static jboolean		ni_gm_copy_get_elts;


#define COPY_SMALL_LOG	6
#define COPY_SMALL	(1 << COPY_SMALL_LOG)
#define COPY_REGION	(ni_gm_copy_get_elts)
#define COPY_THRESHOLD	COPY_SMALL
// #define COPY_THRESHOLD	0

#include <limits.h>
#define UINT_BITS	(CHAR_BIT * sizeof(unsigned long))


static int	sent_eager;
static int	sent_rndvz_req;
static int	sent_rndvz_data;

#define CACHE_LIMIT	(mtu)

typedef struct CACHE_MSG cache_msg_t, *cache_msg_p;


/* Data is embedded after this struct, so it is accessed without indirections */
struct CACHE_MSG {
    jsize	start;
    jsize	len;
    jsize	log_size;
    cache_msg_p	next;
};


static void *
cache2data(cache_msg_p c)
{
    return c + 1;
}


static cache_msg_p
data2cache(void *data)
{
    return (cache_msg_p)data - 1;
}


static cache_msg_p	cache[UINT_BITS - COPY_SMALL_LOG];


/* Cache implementation that uses buckets per size. */
static void *
cache_msg_get(struct gm_port *gm_port, int len, int start)
{
    cache_msg_p	c;
    int		twopow;

    /* Round towards nearest upper power of two. This hopefully helps
     * against unlimited growth of the cache. */
    twopow = COPY_SMALL_LOG;
    while ((1 << twopow) < len) {
	twopow++;
    }

    if (len > CACHE_LIMIT) {
	c = gm_dma_malloc(gm_port, sizeof(*c) + (1UL << twopow));
    } else {
	c = cache[twopow - COPY_SMALL_LOG];
	if (c == NULL) {
	    c = gm_dma_malloc(gm_port, sizeof(*c) + (1UL << twopow));
	    if (c == NULL) {
		fprintf(stderr, "Ughhhh... out of memory -- quits\n");
		exit(17);
	    }
	} else {
	    cache[twopow - COPY_SMALL_LOG] = c->next;
	}
    }

    c->log_size = twopow;
    c->len = len;
    c->start = start;

    return cache2data(c);
}


static void
cache_msg_put(void *data)
{
    cache_msg_p	c = data2cache(data);

    if (c->len > CACHE_LIMIT) {
	free(c);
    } else {
	c->next = cache[c->log_size];
	cache[c->log_size] = c;
    }
}


static int
cache_msg_len(void *data)
{
    return data2cache(data)->len;
}


static int
cache_msg_start(void *data)
{
    return data2cache(data)->start;
}



#define HAND_PROF	0
#if HAND_PROF

#include <das_time.h>

typedef enum FUNC {
    SEND_REQUEST,
    SEND_BUFFER_REQ,
    SEND_BUFFER,
    POST_BUFFER,
    GM_BLOCKING_THREAD,
    GM_THREAD,
    GM_SEND,
    GM_SEND_BUFFER,
    GM_CHECK_TOKEN,
    GET_ARRAY,
    RELEASE_ARRAY,
    N_FUNCS
} func_t;

static char *pname[N_FUNCS] = {
    "SEND_REQUEST",
    "SEND_BUFFER_REQ",
    "SEND_BUFFER",
    "POST_BUFFER",
    "GM_BLOCKING_THREAD",
    "GM_THREAD",
    "GM_SEND",
    "GM_SEND_BUFFER",
    "GM_CHECK_TOKEN",
    "GET_ARRAY",
    "RELEASE_ARRAY"
};

static das_time_t	pt[N_FUNCS];
static das_time_t	ptotal[N_FUNCS];
static int		pcall[N_FUNCS];

static __inline void pstart(int func)
{
    das_time_get(&pt[func]);
}

static __inline void pend(int func)
{
    das_time_t	t;

    das_time_get(&t);
    ptotal[func] += t - pt[func];
    pcall[func]++;
}

static void pdump(void)
{
    int		i;

    for (i = 0; i < N_FUNCS; i++) {
	fprintf(stderr, "%-16s: calls %8d total %.3lf per call %.06lf\n",
		pname[i], pcall[i], das_time_t2d(&ptotal[i]),
		das_time_t2d(&ptotal[i]) / pcall[i]);
    }
}

#else
#define pstart(x)
#define pend(x)
#define pdump()
#endif

/*
 *  Prototypes
 */



/*
 *  Functions
 */

/*
 * Convert a pointer to a 'jlong' handle.
 */
static
jlong
ni_gm_ptr2handle(void  *ptr) {
        union u_conv u;

        u.handle = 0;
        u.ptr    = ptr;

        return u.handle;
}

/*
 * Convert a 'jlong' handle to a pointer.
 */
static
void *
ni_gm_handle2ptr(jlong handle) {
        union u_conv u;

        u.handle = handle;

        return u.ptr;
}

/* GM error message display fonction. */
static
void
ni_gm_control(gm_status_t gm_status, int line)
{
	char *msg = NULL;

	switch (gm_status) {
	case GM_SUCCESS:
		break;

        case GM_FAILURE:
                msg = "GM failure";
                break;

        case GM_INPUT_BUFFER_TOO_SMALL:
                msg = "GM input buffer too small";
                break;

        case GM_OUTPUT_BUFFER_TOO_SMALL:
                msg = "GM output buffer too small";
                break;

        case GM_TRY_AGAIN:
                msg = "GM try again";
                break;

        case GM_BUSY:
                msg = "GM busy";
                break;

        case GM_MEMORY_FAULT:
                msg = "GM memory fault";
                break;

        case GM_INTERRUPTED:
                msg = "GM interrupted";
                break;

        case GM_INVALID_PARAMETER:
                msg = "GM invalid parameter";
                break;

        case GM_OUT_OF_MEMORY:
                msg = "GM out of memory";
                break;

        case GM_INVALID_COMMAND:
                msg = "GM invalid command";
                break;

        case GM_PERMISSION_DENIED:
                msg = "GM permission denied";
                break;

        case GM_INTERNAL_ERROR:
                msg = "GM internal error";
                break;

        case GM_UNATTACHED:
                msg = "GM unattached";
                break;

        case GM_UNSUPPORTED_DEVICE:
                msg = "GM unsupported device";
                break;

        case GM_SEND_TIMED_OUT:
		msg = "GM send timed out";
                break;

        case GM_SEND_REJECTED:
		msg = "GM send rejected";
                break;

        case GM_SEND_TARGET_PORT_CLOSED:
		msg = "GM send target port closed";
                break;

        case GM_SEND_TARGET_NODE_UNREACHABLE:
		msg = "GM send target node unreachable";
                break;

        case GM_SEND_DROPPED:
		msg = "GM send dropped";
                break;

        case GM_SEND_PORT_CLOSED:
		msg = "GM send port closed";
                break;

        case GM_NODE_ID_NOT_YET_SET:
                msg = "GM id not yet set";
                break;

        case GM_STILL_SHUTTING_DOWN:
                msg = "GM still shutting down";
                break;

        case GM_CLONE_BUSY:
                msg = "GM clone busy";
                break;

        case GM_NO_SUCH_DEVICE:
                msg = "GM no such device";
                break;

        case GM_ABORTED:
                msg = "GM aborted";
                break;

#if GM_API_VERSION >= GM_API_VERSION_1_5
        case GM_INCOMPATIBLE_LIB_AND_DRIVER:
                msg = "GM incompatible lib and driver";
                break;

        case GM_UNTRANSLATED_SYSTEM_ERROR:
                msg = "GM untranslated system error";
                break;

        case GM_ACCESS_DENIED:
                msg = "GM access denied";
                break;
#endif

	default:
		msg = "unknown GM error";
		break;
	}

	if (msg) {
		fprintf(stderr, "%d:%s\n", line, msg);
                gm_perror ("gm_message", gm_status);
	}
}

static
int
ni_gm_lock_init(JNIEnv          *env,
                int              id,
                struct s_lock  **pp_lock)
{
    struct s_lock *p_lock = NULL;
    jclass   driver_class;
    jclass   lock_class;
    jfieldID fid;
    jobject  lock_array;
    jclass   lock_lock_class;

    __in__();

    p_lock = malloc(sizeof(*p_lock));
    assert(p_lock);

    driver_class      = ni_findClass(env, "ibis/impl/net/gm/Driver");
    fid               = ni_getStaticField(env, driver_class, "gmLockArray", "Libis/impl/net/NetLockArray;");

    lock_array        = (*env)->GetStaticObjectField(env, driver_class, fid);
    if (lock_array == NULL) {
	fprintf(stderr, "Cannot get field \"%s\" of %s\n",
		"gmLockArray", "ibis/impl/net/gm/Driver\n");
    }

    p_lock->ref       = (*env)->NewGlobalRef(env, lock_array);
    if (p_lock->ref == NULL) {
	fprintf(stderr, "Cannot create global ref for %s\n",
		"lock_array\n");
    }

    lock_class        = ni_findClass(env, "ibis/impl/net/NetLockArray");
    p_lock->lock_id   = ni_getMethod(env, lock_class,
				 "lock", "(I)V");
    p_lock->unlock_id = ni_getMethod(env, lock_class,
				 "unlock", "(I)V");
    p_lock->lock_array = ni_getField(env, lock_class,
				 "lock", "[Libis/impl/net/NetLockArray$Lock;");
    lock_lock_class    = ni_findClass(env, "ibis/impl/net/NetLockArray$Lock");
    p_lock->lock_array_value = ni_getField(env, lock_lock_class,
	    			 "v", "I");
    p_lock->lock_array_front = ni_getField(env, lock_lock_class,
	    			 "front",
				 "Libis/impl/net/NetLockArray$WaitingOn;");

    p_lock->id = (jint)id;
    assert(!*pp_lock);
    *pp_lock = p_lock;

    __out__();

    return 0;
}

static
int
ni_gm_lock_unlock(struct s_lock *p_lock)
{
    JNIEnv	       *env = _current_env;
    jint		value;
    jobjectArray	lock_array;
    jobject		lock;
    jobject		front;

    __in__();

    if (!env) {
	(*_p_vm)->AttachCurrentThread(_p_vm, (void **)&env, NULL);
    }

    VPRINTF(1000, ("Here...\n"));
    VPRINTF(1000, ("unlock nonnotify(%d)\n", p_lock->id));

    lock_array = (jobjectArray)(*env)->GetObjectField(
			    env, p_lock->ref, p_lock->lock_array);
    lock = (*env)->GetObjectArrayElement(env, lock_array, p_lock->id);
    front = (*env)->GetObjectField(env, lock, p_lock->lock_array_front);
    if (front != NULL) {
	(*env)->CallVoidMethod(env, p_lock->ref, p_lock->unlock_id, p_lock->id);
    } else {
	value = (*env)->GetIntField(env, lock, p_lock->lock_array_value);
	value++;
	(*env)->SetIntField(env, lock, p_lock->lock_array_value, value);
    }

    __out__();

    return 0;
}

static
int
ni_gm_access_lock_init(JNIEnv *env)
{
    jclass	driver_class;
    jfieldID	fld_lock;
    jobject	alock_obj;
    jclass	cls_Monitor;

    __in__();
    ni_gm_access_lock = malloc(sizeof(*ni_gm_access_lock));
    assert(ni_gm_access_lock);

    driver_class = ni_findClass(env, "ibis/impl/net/gm/Driver");
    fld_lock = ni_getStaticField(env, driver_class, "gmAccessLock", "Libis/util/Monitor;");
    alock_obj = (*env)->GetStaticObjectField(env, driver_class, fld_lock);
    if (alock_obj == NULL) {
	fprintf(stderr, "cannot get field \"%s\" class %s\n",
		"gmAccessLock", "Libis/util/Monitor;");
    }

    ni_gm_access_lock->ref = (*env)->NewGlobalRef(env, alock_obj);
    if (ni_gm_access_lock->ref == NULL) {
	fprintf(stderr, "cannot create global ref \"%s\" class %s\n",
		"gmAccessLock", "Libis/util/Monitor;");
    }

    cls_Monitor = ni_findClass(env, "ibis/util/Monitor");
    ni_gm_access_lock->lock_id = ni_getMethod(env, cls_Monitor, "lock", "(Z)V");
    ni_gm_access_lock->unlock_id = ni_getMethod(env, cls_Monitor, "unlock", "()V");

    ni_gm_access_lock->priority = JNI_FALSE;
    __out__();

    return 0;
}

static
int
ni_gm_access_lock_lock(JNIEnv *env)
{
        __in__();
        (*env)->CallVoidMethod(env, ni_gm_access_lock->ref, ni_gm_access_lock->lock_id, ni_gm_access_lock->priority);
        __out__();

        return 0;
}

static
int
ni_gm_access_lock_unlock(JNIEnv *env)
{
        __in__();
        (*env)->CallVoidMethod(env, ni_gm_access_lock->ref, ni_gm_access_lock->unlock_id);
        __out__();

        return 0;
}

static
int
ni_gm_input_unlock(struct s_input *p_in, int len) {
        struct s_lock *p_lock = NULL;
        JNIEnv        *env    = _current_env;

        __in__();
        if (!env) {
                (*_p_vm)->AttachCurrentThread(_p_vm, (void **)&env, NULL);
        }

        (*env)->SetIntField(env, p_in->ref, p_in->len_id, len);
        p_lock = p_in->p_lock;
	VPRINTF(900, ("unlock(%d)\n", p_lock->id));
        (*env)->CallVoidMethod(env, p_lock->ref, p_lock->unlock_id, p_lock->id);
        __out__();

        return 0;
}

#if 0
static
int
ni_gm_mutex_init(JNIEnv          *env,
                 jobject          object,
                 char            *field,
                 struct s_mutex **pp_mutex) {
        struct s_mutex *p_mutex = NULL;

        __in__();
        p_mutex = malloc(sizeof(struct s_mutex));
        assert(p_mutex);

        {
                jclass   object_class = 0;
                jclass   mutex_class  = 0;
                jfieldID fid          = 0;
                jobject  mutex        = 0;

                object_class = (*env)->GetObjectClass(env, object);
                assert(object_class);

                fid = (*env)->GetFieldID(env, object_class, field,
                                         "Libis/impl/net/NetMutex;");
                assert(fid);

                mutex = (*env)->GetObjectField(env, object, fid);
                assert(mutex);

                p_mutex->ref = (*env)->NewGlobalRef(env, mutex);
                assert(p_mutex->ref);

                mutex_class = (*env)->FindClass(env, "ibis/impl/net/NetMutex");
                assert(mutex_class);

                p_mutex->unlock_id =
                        (*env)->GetMethodID(env, mutex_class, "unlock", "()V");
                assert(p_mutex->unlock_id);
        }

        assert(!*pp_mutex);
        *pp_mutex = p_mutex;
        __out__();

        return 0;
}

static
int
ni_gm_mutex_unlock(struct s_mutex *p_mutex) {
        JNIEnv        *env    = _current_env;

        __in__();
        if (!env) {
                (*_p_vm)->AttachCurrentThread(_p_vm, (void **)&env, NULL);
        }
        (*env)->CallVoidMethod(env, p_mutex->ref, p_mutex->unlock_id);
        __out__();

        return 0;
}
#endif

static
int
ni_gm_release_output_array(struct s_output *p_out) {
        JNIEnv        *env    = _current_env;
	e_type type = p_out->type;
	union u_j_array *pb = &p_out->java;
	void *ptr = p_out->array;

        __in__();
        if (!env) {
                (*_p_vm)->AttachCurrentThread(_p_vm, (void **)&env, NULL);
        }

#define RELEASE_ARRAY(E_TYPE, jarray, Jtype) \
		case E_TYPE: \
			if (p_out->is_copy) { \
			    cache_msg_put(ptr); \
			} else { \
			    (*env)->Release ## Jtype ## ArrayElements(env, pb->jarray, ptr, JNI_ABORT); \
			} \
			(*env)->DeleteGlobalRef(env, pb->jarray); \
			break;

	switch (type) {
	RELEASE_ARRAY(E_BUFFER,  j_buffer,  Byte)
	RELEASE_ARRAY(E_BOOLEAN, j_boolean, Boolean)
	RELEASE_ARRAY(E_BYTE,    j_byte,    Byte)
	RELEASE_ARRAY(E_SHORT,   j_short,   Short)
	RELEASE_ARRAY(E_CHAR,    j_char,    Char)
	RELEASE_ARRAY(E_INT,     j_int,     Int)
	RELEASE_ARRAY(E_LONG,    j_long,    Long)
	RELEASE_ARRAY(E_FLOAT,   j_float,   Float)
	RELEASE_ARRAY(E_DOUBLE,  j_double,  Double)

	default:
		goto error;
	}
#undef RELEASE_ARRAY
        __out__();

        return 0;

 error:
        __err__();
	return -1;
}

static
int
ni_gm_release_input_array(struct s_input *p_in, int length) {
        JNIEnv          *env  = _current_env;
        e_type           type = p_in->type;
	union u_j_array *pb   = &p_in->java;
	void            *ptr  = p_in->array;

        __in__();
        if (!env) {
                (*_p_vm)->AttachCurrentThread(_p_vm, (void **)&env, NULL);
        }

#define RELEASE_ARRAY(E_TYPE, jarray, Jtype) \
		case E_TYPE: \
		    if (p_in->is_copy) { \
			    if (cache_msg_len(ptr) < length) { \
				fprintf(stderr, "Expect message length %d but receive %d -- quit\n", \
					cache_msg_len(ptr), length); \
				exit(33); \
			    } \
			    (*env)->Set ## Jtype ## ArrayRegion(env, pb->jarray, cache_msg_start(ptr), length, ptr); \
			    cache_msg_put(ptr); \
		    } else { \
			    (*env)->Release ## Jtype ## ArrayElements(env, pb->jarray, ptr, 0); \
		    } \
		    (*env)->DeleteGlobalRef(env, pb->jarray); \
		    break;

        switch (type) {
	RELEASE_ARRAY(E_BUFFER,  j_buffer,  Byte)
	RELEASE_ARRAY(E_BOOLEAN, j_boolean, Boolean)
	RELEASE_ARRAY(E_BYTE,    j_byte,    Byte)
	RELEASE_ARRAY(E_SHORT,   j_short,   Short)
	RELEASE_ARRAY(E_CHAR,    j_char,    Char)
	RELEASE_ARRAY(E_INT,     j_int,     Int)
	RELEASE_ARRAY(E_LONG,    j_long,    Long)
	RELEASE_ARRAY(E_FLOAT,   j_float,   Float)
	RELEASE_ARRAY(E_DOUBLE,  j_double,  Double)

        default:
                goto error;
        }
#undef RELEASE_ARRAY

        __out__();

        return 0;

 error:
        __err__();
        return -1;
}

static
int
ni_gm_check_send_tokens(struct s_port *p_port) {
        unsigned int nb_tokens = 0;

        __in__();
        nb_tokens = gm_num_send_tokens(p_port->p_gm_port);

        if (nb_tokens < 1) {
                __error__("no more send tokens");
                goto error;
        }

        __out__();
        return 0;

 error:
        __err__();
        return -1;
}

static
int
ni_gm_check_receive_tokens(struct s_port   *p_port) {
        unsigned int nb_tokens = 0;

        __in__();
        nb_tokens = gm_num_receive_tokens(p_port->p_gm_port);

        if (nb_tokens < 1) {
                __error__("no more receive tokens");
                goto error;
        }

        __out__();
        return 0;

 error:
        __err__();
        return -1;
}


static
int
ni_gm_register_block(struct s_port   *p_port,
                     void            *v_ptr,
                     int              len,
                     struct s_cache **_pp_cache) {
	struct gm_port *p_gm_port = p_port->p_gm_port;
        gm_status_t     gms       = GM_SUCCESS;
        struct s_cache  *p_cache  = NULL;
        struct s_cache **pp_cache = &p_port->cache_head;
	unsigned char   *ptr      = v_ptr;

        assert(ptr);
        assert(len);

        {
                unsigned long mask    = (CACHE_GRANULARITY - 1);
                unsigned long lng     = (unsigned long)ptr;
                unsigned long tmp_lng = (lng & ~mask);
                unsigned long offset  = lng - tmp_lng;

                ptr -= offset;
                len += offset;

                if (len & mask) {
                        len = (len & ~mask) + CACHE_GRANULARITY;
                }
        }

        while ((p_cache = *pp_cache) != NULL) {
                if (ptr >= p_cache->ptr
                    &&
                    ptr+len <= p_cache->ptr+p_cache->len) {
                        p_cache->ref_count++;

                        if (pp_cache != &p_port->cache_head) {
                                /* Move the cache entry at the head */
                                *pp_cache = p_cache->next;
                                p_cache->next = p_port->cache_head;
                                p_port->cache_head = p_cache;
                        }

                        goto success;
                }

                pp_cache = &(p_cache->next);
        }

        p_cache = malloc(sizeof(struct s_cache));
        assert(p_cache);

        gms = gm_register_memory(p_gm_port, ptr, len);
        if (gms) {
                __error__("memory registration failed (ptr = %p, len = %d)", ptr, len);
                ni_gm_control(gms, __LINE__);
                free(p_cache);
                p_cache = NULL;
                goto error;
        }

        p_cache->ptr = ptr;
        p_cache->len = len;
        p_cache->ref_count = 1;
        p_cache->next = p_port->cache_head;
        p_port->cache_head = p_cache;

 success:
        *_pp_cache = p_cache;
        return 0;


 error:
        return -1;
}

static
int
ni_gm_deregister_block(struct s_port  *p_port,
                       struct s_cache *p_cache)
{
	struct gm_port *p_gm_port = p_port->p_gm_port;

        if (!--p_cache->ref_count) {
                gm_status_t     gms       = GM_SUCCESS;
                struct s_cache **pp_cache = &p_port->cache_head;
                int             i         = 0;

                while (*pp_cache != p_cache) {
                        pp_cache = &((*pp_cache)->next);
                        i++;
                }

                if (i >= CACHE_SIZE) {
                        gms = gm_deregister_memory(p_gm_port,
                                                   p_cache->ptr,
                                                   p_cache->len);
                        if (gms) {
                                __error__("memory deregistration failed");
                                ni_gm_control(gms, __LINE__);
                                goto error;
                        }

                        p_cache->ptr = NULL;
                        p_cache->len =    0;

                        *pp_cache = p_cache->next;
                        p_cache->next = NULL;

                        free(p_cache);
                }
        }

        return 0;

 error:
        return -1;
}


/* Opens a GM port and sets up a s_port structure for representing it. */
static
int
ni_gm_open_port(struct s_dev *p_dev) {
	struct s_port  *p_port    = NULL;
	struct gm_port *p_gm_port = NULL;
	gm_status_t     gms       = GM_SUCCESS;
	int             port_id   =  NI_GM_MIN_PORT_NUM;
        int             i         =  0;
	unsigned int    node_id   =  0;

	__in__();

        while (i < nb_pub_ports) {
                port_id = pub_port_array[i];
                __disp__("trying to open GM port %d on device %d", port_id, p_dev->id);

                gms     = gm_open(&p_gm_port, p_dev->id, port_id,
                                  "net_ibis_gm", GM_API_VERSION_1_1);
                __disp__("status %d", gms);

                if (gms == GM_SUCCESS) {
                        __disp__("port ok");
                        goto found;
                }


                if (gms != GM_BUSY) {
                        __error__("gm_open failed");
                        ni_gm_control(gms, __LINE__);
                        goto error;
                }

                __disp__("port busy");
                i++;
        }

        __error__("no more GM port");
        goto error;

 found:
        ;

	p_port = malloc(sizeof(struct s_port));
        assert(p_port);

	gms = gm_get_node_id(p_gm_port, &node_id);
	if (gms != GM_SUCCESS) {
                __error__("gm_get_node_id failed");
		ni_gm_control(gms, __LINE__);
                goto error;
	}
	VPRINTF(1, ("My node_id %d\n", node_id));

        p_port->cache_head        = NULL;
	p_port->p_gm_port         = p_gm_port;
	p_port->port_id           = port_id;
	p_port->node_id           = node_id;
	p_port->p_dev             = p_dev;
	p_port->ref_count         = 0;

        p_port->local_input_array_size  = 0;
        p_port->local_input_array       = NULL;
        p_port->local_output_array_size = 0;
        p_port->local_output_array      = NULL;

        p_port->active_input = NULL;
        p_port->nb_packets   = 0;

        p_port->packet_size   = gm_min_size_for_length(NI_GM_PACKET_LEN);

	p_port->send_packet_cache = NULL;

        {
                struct s_packet *p_packet = NULL;
                const int nb = 16;
                int i = 0;

                if (ni_gm_check_receive_tokens(p_port)) {
                        goto error;
                }

                p_packet = malloc(sizeof(struct s_request));
                assert(p_packet);

                p_packet->data      = gm_dma_malloc(p_gm_port, NI_GM_PACKET_LEN);
                assert(p_packet->data);

                p_port->packet_head = p_packet;
                p_packet->next      = p_packet;
                p_packet->previous  = p_packet;
                gm_provide_receive_buffer_with_tag(p_port->p_gm_port, p_packet->data,
                                           p_port->packet_size,
                                           GM_HIGH_PRIORITY, 1);
                p_port->nb_packets++;

                while (i++ < nb && gm_num_receive_tokens(p_port->p_gm_port) > 1) {
                         p_packet = malloc(sizeof(struct s_request));
                         assert(p_packet);

                         p_packet->data = gm_dma_malloc(p_gm_port, NI_GM_PACKET_LEN);
                         assert(p_packet->data);

                         p_packet->previous       = p_port->packet_head->previous;
                         p_packet->next           = p_port->packet_head;
                         p_packet->previous->next = p_packet;
                         p_packet->next->previous = p_packet;
                         p_port->packet_head      = p_packet;
                         gm_provide_receive_buffer_with_tag(p_port->p_gm_port,
                                                            p_packet->data,
                                                            p_port->packet_size,
                                                            GM_HIGH_PRIORITY, 1);
                         p_port->nb_packets++;
                }
        }

	p_port->ni_gm_send_tokens = gm_num_send_tokens(p_gm_port);

        p_dev->p_port     = p_port;
	p_dev->ref_count++;

        __out__();
	return 0;

 error:
        __err__();
        return -1;
}

static
int
ni_gm_expand_array(unsigned char **pp,
                   int            *ps,
                   int             i,
                   const size_t    o) {
        unsigned char *p = *pp;
        int   s = *ps;

        __in__();
        if (!p) {
                assert(!s);
                p = malloc(o);
                assert(p);
                memset(p, 0, o);
                s = 1;
        } else if (s <= i) {
                int ns = i + 1;

                p = realloc(p, ns * o);
                assert(p);
                memset(p+s*o, 0, (ns-s)*o);
                s = ns;
        }

        *pp = p;
        *ps = s;

        __out__();
	return 0;
}


static
void
ni_gm_add_packet_to_list_head(struct s_packet **pp_packet,
                              struct s_packet  *p_packet) {
        struct s_packet *list_head = NULL;

        __in__();
        list_head = *pp_packet;

        if (list_head) {
                p_packet->previous       = list_head->previous;
                p_packet->next           = list_head;
                p_packet->previous->next = p_packet;
                p_packet->next->previous = p_packet;
        } else {
                p_packet->next      = p_packet;
                p_packet->previous  = p_packet;
        }

        list_head  = p_packet;
        *pp_packet = list_head;
        __out__();
}

static
void
ni_gm_add_packet_to_list_tail(struct s_packet **pp_packet,
                              struct s_packet  *p_packet) {
        struct s_packet *list_head = NULL;

        __in__();
        list_head = *pp_packet;

        if (list_head) {
                p_packet->previous       = list_head->previous;
                p_packet->next           = list_head;
                p_packet->previous->next = p_packet;
                p_packet->next->previous = p_packet;
        } else {

                p_packet->next      = p_packet;
                p_packet->previous  = p_packet;
                list_head           = p_packet;
        }

        *pp_packet = list_head;
        __out__();
}

static
struct s_packet *
ni_gm_remove_packet_from_list(struct s_packet **pp_packet) {
        struct s_packet *list_head = NULL;
        struct s_packet *p_packet = NULL;

        __in__();
        list_head = *pp_packet;

        if (list_head) {
                p_packet = list_head;

                if (p_packet->next != p_packet) {
                        list_head                 = p_packet->next;
                        list_head->previous       = p_packet->previous;
                        list_head->previous->next = list_head;
                } else {
                        list_head = NULL;
                }

                *pp_packet = list_head;
        }

        __out__();

        return p_packet;
}


#ifndef NDEBUG
#define hdr_set_seqno(hdr, p_out) \
		((hdr)->seqno = (p_out)->seqno++)
#define hdr_check_seqno(hdr, p_in) \
		do { assert((hdr)->seqno == (p_in)->seqno++); } while (0)
#else
#define hdr_set_seqno(hdr, p_out)
#define hdr_check_seqno(hdr, p_in)
#endif


static struct s_packet *
ni_gm_packet_get(struct s_output *p_out)
{
    struct s_packet *packet;
    struct s_port   *port = p_out->p_port;
    ni_gm_hdr_p hdr;

    packet = port->send_packet_cache;
    if (packet == NULL) {
	packet = malloc(sizeof(*packet));
	packet->data = gm_dma_malloc(port->p_gm_port, NI_GM_PACKET_LEN);
    } else {
	port->send_packet_cache = packet->next;
    }

    hdr = (ni_gm_hdr_p)packet->data;
    hdr->mux_id = p_out->remote_mux_id;
#ifndef NDEBUG
    hdr->type = NI_GM_MSG_TYPE_NONE;	// WHY????
#endif

    packet->p_out = p_out;

    return packet;
}


static void
ni_gm_packet_put(struct s_port *port, struct s_packet *packet)
{
    packet->next = port->send_packet_cache;
    port->send_packet_cache = packet;
#ifndef NDEBUG
    ((ni_gm_hdr_p)packet->data)->byte_buffer_offset = -1;
#endif
}


static
void
ni_gm_eager_callback(struct gm_port *port,
		     void           *ptr,
		     gm_status_t     gms)
{
    struct s_packet  *packet = ptr;
    struct s_output  *p_out = packet->p_out;
    struct s_request *p_rq = &p_out->request;

    __in__();

    p_rq->status = gms;

    VPRINTF(100, ("Receive a ni_gm_eager_callback; p_out %p packet %p current state %s\n", p_out, packet, ni_gm_sender_state(p_out->state)));

    assert(!p_rq->p_in);

    assert(p_out->state == NI_GM_SENDER_SENDING_EAGER);
    assert(p_out->p_port->ni_gm_send_tokens++ >= 0);

    p_out->state = NI_GM_SENDER_IDLE;
    ni_gm_packet_put(p_out->p_port, packet);
    __disp__("ni_gm_eager_callback: unlock(%d)\n", p_out->p_lock->id);
    ni_gm_lock_unlock(p_out->p_lock);
    VPRINTF(120, ("Received a ni_gm_eager_callback; p_out %p state := %s tokens := %d\n", p_out, ni_gm_sender_state(p_out->state), p_out->p_port->ni_gm_send_tokens));

    __out__();
}


static
void
ni_gm_callback(struct gm_port *port,
               void           *ptr,
               gm_status_t     gms) {
        struct s_request *p_rq   = NULL;
        struct s_port    *p_port = NULL;

        __in__();

        p_rq         = ptr;
        p_rq->status = gms;
        p_port       = p_rq->p_port;

        assert(p_rq->p_out || p_rq->p_in);
	assert(p_port->ni_gm_send_tokens++ >= 0);

        if (p_rq->p_out) {
                struct s_output *p_out = p_rq->p_out;

		VPRINTF(100, ("Receive a ni_gm_callback; p_out %p current state %s tokens := %d\n", p_out, ni_gm_sender_state(p_out->state), p_port->ni_gm_send_tokens));

                assert(!p_rq->p_in);

                assert(p_out->state == NI_GM_SENDER_SENDING_RNDZVS_REQ
			|| p_out->state == NI_GM_SENDER_SENDING_RNDZVS_DATA
			|| p_out->state == NI_GM_SENDER_SENDING_EAGER);

                if (p_out->state == NI_GM_SENDER_SENDING_RNDZVS_REQ) {
                        p_out->state = NI_GM_SENDER_SENDING_RNDZVS_ACK;
                        __disp__("ni_gm_callback: unlock(%d)\n", p_out->p_lock->id);
                        ni_gm_lock_unlock(p_out->p_lock);
                } else if (p_out->state == NI_GM_SENDER_SENDING_EAGER) {
                        p_out->state = NI_GM_SENDER_IDLE;
                        __disp__("ni_gm_callback: unlock(%d)\n", p_out->p_lock->id);
                        ni_gm_lock_unlock(p_out->p_lock);
                } else if (p_out->state == NI_GM_SENDER_SENDING_RNDZVS_DATA) {
                        ni_gm_deregister_block(p_port, p_out->p_cache);
                        ni_gm_release_output_array(p_out);

                        p_out->p_cache = NULL;
                        p_out->length  = 0;
                        p_out->state   = NI_GM_SENDER_IDLE;
                        __disp__("ni_gm_callback: unlock(%d)\n", p_out->p_lock->id);
                        ni_gm_lock_unlock(p_out->p_lock);
                } else {
                        abort();
                }
		VPRINTF(100, ("Received a ni_gm_callback; p_out %p state := %s\n", p_out, ni_gm_sender_state(p_out->state)));
        } else if (p_rq->p_in) {
                struct s_input *p_in = NULL;

                assert(!p_rq->p_out);
                p_in = p_rq->p_in;
		VPRINTF(100, ("Receive a ni_gm_callback; p_in %p tokens := %d\n", p_in, p_port->ni_gm_send_tokens));
                __disp__("ni_gm_callback: unlock(%d)\n", p_in->p_lock->id);
                ni_gm_lock_unlock(p_in->p_lock);
        } else {
                abort();
        }

        __out__();
}

static
int
ni_gm_init_output(struct s_dev     *p_dev,
                  struct s_output **pp_out) {
        struct s_output *p_out  = NULL;
        struct s_port   *p_port = NULL;

        __in__();
        p_port = p_dev->p_port;

        p_out = malloc(sizeof(struct s_output));
        assert(p_out);

        p_out->p_port         = p_port;
        p_out->p_lock         = NULL;
        p_out->dst_port_id    = 0;
        p_out->dst_node_id    = 0;
        p_out->local_mux_id   = p_port->local_output_array_size;
#ifndef NDEBUG
	p_out->seqno          = 0;
#endif
        __disp__("%p: mux_id = %d\n", p_out, p_out->local_mux_id);

        p_out->remote_mux_id  = 0;

        ni_gm_expand_array((unsigned char **)&p_port->local_output_array,
                           &p_port->local_output_array_size,
                           p_out->local_mux_id,
                           sizeof(struct s_output *));
        assert(!p_port->local_output_array[p_out->local_mux_id]);
        p_out->packet_size   = gm_min_size_for_length(NI_GM_PACKET_LEN);
	p_out->offset        = NI_GM_PACKET_HDR_LEN;
	p_out->byte_buffer   = 0;

        p_out->state = NI_GM_SENDER_IDLE;

        p_out->request.p_port = p_port;
        p_out->request.p_out  = p_out;
        p_out->request.p_in   = NULL;
        p_out->request.status = GM_SUCCESS;

        p_port->local_output_array[p_out->local_mux_id] = p_out;
        p_port->ref_count++;

        *pp_out = p_out;

        __out__();

        return 0;
}

static
int
ni_gm_init_input(struct s_dev    *p_dev,
                 struct s_input **pp_in) {
        struct s_input *p_in  = NULL;
        struct s_port   *p_port = NULL;

        __in__();
        p_port = p_dev->p_port;

        p_in = malloc(sizeof(struct s_input));
        assert(p_in);

        p_in->p_lock         = NULL;
        p_in->data_available = 0;
        p_in->length         = 0;
	p_in->data_size      = 0;
	p_in->offset         = NI_GM_PACKET_HDR_LEN;
	p_in->data_left      = 0;
        p_in->p_port         = p_port;
        p_in->src_port_id    = 0;
        p_in->src_node_id    = 0;
        p_in->local_mux_id   = p_port->local_input_array_size;
        p_in->remote_mux_id  = 0;
#ifndef NDEBUG
	p_in->seqno          = 0;
#endif
        __disp__("%p: mux_id = %d\n", p_in, p_in->local_mux_id);

        ni_gm_expand_array((unsigned char **)&p_port->local_input_array,
                           &p_port->local_input_array_size,
                           p_in->local_mux_id,
                           sizeof(struct s_input *));
        assert(!p_port->local_input_array[p_in->local_mux_id]);
        p_in->ack_packet = gm_dma_malloc(p_port->p_gm_port, NI_GM_PACKET_LEN);
        assert(p_in->ack_packet);
        p_in->packet_size   = gm_min_size_for_length(NI_GM_PACKET_LEN);

        p_in->request.p_port = p_port;
        p_in->request.p_out  = NULL;
        p_in->request.p_in   = p_in;
        p_in->request.status = GM_SUCCESS;

        p_in->packet_head = NULL;

        p_port->local_input_array[p_in->local_mux_id] = p_in;
        p_port->ref_count++;

        *pp_in = p_in;

        __out__();
        return 0;
}


static
int
ni_gm_get_output_node_id(struct s_output *p_out, int *p_id) {
        __in__();
        *p_id = p_out->p_port->node_id;
        __out__();
        return 0;
}

static
int
ni_gm_get_input_node_id(struct s_input *p_in, int *p_id) {
        __in__();
        *p_id = p_in->p_port->node_id;
        __out__();
        return 0;
}

static
int
ni_gm_get_output_port_id(struct s_output *p_out, int *p_id) {
        __in__();
        *p_id = p_out->p_port->port_id;
        __out__();
        return 0;
}

static
int
ni_gm_get_input_port_id(struct s_input *p_in, int *p_id) {
        __in__();
        *p_id = p_in->p_port->port_id;
        __out__();
        return 0;
}

static
int
ni_gm_get_output_mux_id(struct s_output *p_out, int *p_id) {
        __in__();
        *p_id = p_out->local_mux_id;
        __out__();
        return 0;
}

static
int
ni_gm_get_input_mux_id(struct s_input *p_in, int *p_id) {
        __in__();
        *p_id = p_in->local_mux_id;
        __out__();
        return 0;
}

static
int
ni_gm_connect_output(struct s_output *p_out,
                     int              remote_node_id,
                     int              remote_port_id,
                     int              remote_mux_id) {
        struct s_port *p_port = NULL;

        __in__();
        assert(!p_out->dst_node_id);
        assert(!p_out->dst_port_id);
        assert(!p_out->remote_mux_id);

        p_port = p_out->p_port;
        p_out->dst_node_id   = remote_node_id;
        p_out->dst_port_id   = remote_port_id;
        p_out->remote_mux_id = remote_mux_id;
        __disp__("ni_gm_connect_output: %d -> %d, lock = %d\n", p_out->local_mux_id, p_out->remote_mux_id, p_out->p_lock->id);

        p_out->packet = ni_gm_packet_get(p_out);
        assert(p_out->packet);
        assert(p_out->packet->data);

        __out__();

        return 0;
}

static
int
ni_gm_connect_input(struct s_input *p_in,
                    int             remote_node_id,
                    int             remote_port_id,
                    int             remote_mux_id) {
        struct s_port *p_port = NULL;
	ni_gm_hdr_p hdr;

        __in__();
        assert(!p_in->src_node_id);
        assert(!p_in->src_port_id);
        assert(!p_in->remote_mux_id);

        p_port = p_in->p_port;

        p_in->src_node_id = remote_node_id;
        p_in->src_port_id = remote_port_id;
        p_in->remote_mux_id = remote_mux_id;
        __disp__("ni_gm_connect_input: %d <- %d, lock = %d\n", p_in->local_mux_id, p_in->remote_mux_id, p_in->p_lock->id);

	hdr = (ni_gm_hdr_p)p_in->ack_packet;
	hdr->type = NI_GM_MSG_TYPE_CONNECT;
	hdr->length = 0;	// What is this good for? RFHH
	hdr->mux_id = remote_mux_id;

        __out__();

        return 0;
}


/*
 * Send the request for a rendez-vous transfer
 */
static
int
ni_gm_output_send_request(struct s_output *p_out) {
        struct s_port    *p_port = NULL;
        struct s_request *p_rq   = NULL;
	unsigned char    *packet_data = p_out->packet->data;
	ni_gm_hdr_p hdr;

        __in__();
        assert(p_out->state == NI_GM_SENDER_IDLE);

        p_out->state = NI_GM_SENDER_SENDING_RNDZVS_REQ;

	hdr = (ni_gm_hdr_p)packet_data;
	hdr->type = NI_GM_MSG_TYPE_RENDEZ_VOUS_REQ;
	hdr->length = p_out->length;
	hdr_set_seqno(hdr, p_out);

        p_port = p_out->p_port;

        p_rq = &p_out->request;
        p_rq->status = GM_SUCCESS;

        if (ni_gm_check_send_tokens(p_port)) {
                goto error;
        }
	VPRINTF(100, ("Send HIGH rendez-vous request p_out %p for size %d tokens %d\n", p_out, p_out->length, p_port->ni_gm_send_tokens));
	assert(--p_port->ni_gm_send_tokens >= 0);
        gm_send_with_callback(p_port->p_gm_port,
			      packet_data,
                              p_out->packet_size,
			      NI_GM_PACKET_HDR_LEN,
                              GM_HIGH_PRIORITY,
                              p_out->dst_node_id,
			      p_out->dst_port_id,
                              ni_gm_callback, p_rq);
sent_rndvz_req++;
        __out__();
        return 0;

 error:
        __err__();
        return -1;
}


/*
 * Flush the buffer
 */
static
int
ni_gm_output_flush(struct s_output *p_out)
{
    struct s_port    *p_port;
    struct s_request *p_rq;
    struct s_packet  *packet = p_out->packet;
    unsigned char    *packet_data = packet->data;
    ni_gm_hdr_p hdr;

    __in__();
    pstart(GM_SEND_BUFFER);
    assert(p_out->offset > NI_GM_PACKET_HDR_LEN);
    assert(p_out->state == NI_GM_SENDER_IDLE);

    p_out->state = NI_GM_SENDER_SENDING_EAGER;
    p_port = p_out->p_port;

    hdr = (ni_gm_hdr_p)packet_data;
    hdr->type = NI_GM_MSG_TYPE_EAGER;
    hdr->length = p_out->offset - NI_GM_PACKET_HDR_LEN;
    hdr_set_seqno(hdr, p_out);

    VPRINTF(100, ("flush eager packet size %d type %s byte buffer at %d\n",
		    p_out->offset, ni_gm_msg_type(hdr->type),
		    hdr->byte_buffer_offset));
#if VERBOSE
    {
	int i;
	for (i = 0; i < MIN((int)p_out->offset, 32); i++) {
	    VPRINTSTR(550, ("0x%x ", packet_data[i]));
	}
	VPRINTSTR(550, ("\n"));
    }
#endif

    p_rq = &p_out->request;
    p_rq->status = GM_SUCCESS;

    pstart(GM_CHECK_TOKEN);
    if (ni_gm_check_send_tokens(p_port)) {
	    goto error;
    }
    pend(GM_CHECK_TOKEN);

    pstart(GM_SEND);
    VPRINTF(100, ("Send to %d HIGH data message p_out %p seqno %llu packet %p size %d mux_id %d send tokens %d\n", p_out->dst_node_id, p_out, hdr->seqno, packet, p_out->offset, hdr->mux_id, p_port->ni_gm_send_tokens));
    assert(--p_port->ni_gm_send_tokens >= 0);
    gm_send_with_callback(p_port->p_gm_port,
			  packet_data,
			  p_out->packet_size,
			  p_out->offset,
			  GM_HIGH_PRIORITY,
			  p_out->dst_node_id,
			  p_out->dst_port_id,
			  ni_gm_eager_callback,
			  packet);
    p_out->packet = ni_gm_packet_get(p_out);
    pend(GM_SEND);
    pend(GM_SEND_BUFFER);
sent_eager++;
    p_out->offset = NI_GM_PACKET_HDR_LEN;
    p_out->byte_buffer = 0;
    ((ni_gm_hdr_p)p_out->packet->data)->byte_buffer_offset = -1;

    __out__();
    return 0;

error:
    __err__();
    return -1;
}


/*
 * Send the data part of the rendez-vous message
 */
static
int
ni_gm_output_send_buffer(struct s_output *p_out, void *b, int len) {
        struct s_port    *p_port = NULL;
        struct s_request *p_rq   = NULL;
	ni_gm_hdr_p hdr;

        __in__();
        assert(b);
        assert(len);
        assert(p_out->state == NI_GM_SENDER_SENDING_RNDZVS_ACK);
        p_out->state = NI_GM_SENDER_SENDING_RNDZVS_DATA;

	hdr = (ni_gm_hdr_p)p_out->packet->data;
	hdr->type = NI_GM_MSG_TYPE_RENDEZ_VOUS_DATA;
	//???????? hdr->length = len;

        p_port = p_out->p_port;
        ni_gm_register_block(p_port, b, len, &p_out->p_cache);
        p_out->length = len;
        p_rq = &p_out->request;
        p_rq->status = GM_SUCCESS;

        if (ni_gm_check_send_tokens(p_port)) {
                goto error;
        }

	VPRINTF(100, ("Send LOW rendez-vous data p_out %p size %d tokens %d\n", p_out, len, p_port->ni_gm_send_tokens));
	assert(--p_port->ni_gm_send_tokens >= 0);
        gm_send_with_callback(p_port->p_gm_port,
			      b,
                              NI_GM_BLOCK_SIZE,
			      len,
                              GM_LOW_PRIORITY,
                              p_out->dst_node_id,
			      p_out->dst_port_id,
                              ni_gm_callback, p_rq);
sent_rndvz_data++;

        __out__();
        return 0;

 error:
        __err__();
        return -1;
}

static void ni_gm_throw_exception(JNIEnv *env, char *msg);


static int
ni_gm_packet_clear(struct s_packet *p_packet, struct s_port *p_port)
{
	if (p_port->nb_packets < NI_GM_MAX_PACKETS) {
		if (ni_gm_check_receive_tokens(p_port)) {
			return 0;
		}

		ni_gm_add_packet_to_list_head(&p_port->packet_head, p_packet);
		gm_provide_receive_buffer_with_tag(p_port->p_gm_port,
						   p_packet->data,
						   p_port->packet_size,
						   GM_HIGH_PRIORITY, 1);
		p_port->nb_packets++;
	} else {
		gm_dma_free(p_port->p_gm_port, p_packet->data);
		p_packet->data     = NULL;
		p_packet->previous = NULL;
		p_packet->next     = NULL;
		free(p_packet);
		p_packet = NULL;
	}
	return 1;
}


static int
ni_gm_input_packet_get(struct s_input *p_in)
{
    struct s_packet *packet;
    int		is_rendez_vous;
    ni_gm_hdr_p	hdr;

    if (p_in->data_left > 0) {
	assert(p_in->packet != NULL);
	return 0;
    }

    VPRINTF(100, ("At post: no pending data (offset %d data_size %d); remove from queue %p\n", p_in->offset, p_in->data_size, p_in->packet_head));
    p_in->packet = ni_gm_remove_packet_from_list(&p_in->packet_head);
    packet = p_in->packet;

    hdr = (ni_gm_hdr_p)packet->data;
    is_rendez_vous = (hdr->type == NI_GM_MSG_TYPE_RENDEZ_VOUS_REQ);
    assert(is_rendez_vous || hdr->type == NI_GM_MSG_TYPE_EAGER);
    hdr_check_seqno(hdr, p_in);

    p_in->data_size = hdr->length + NI_GM_PACKET_HDR_LEN;
    p_in->offset = NI_GM_PACKET_HDR_LEN;
    p_in->data_left = p_in->data_size - NI_GM_PACKET_HDR_LEN;
    p_in->byte_buffer_consumed = 0;
    VPRINTF(100, ("At remove: Packet %p type %s seqno %llu data_size %d byte buffer at %d\n",
		    packet, ni_gm_msg_type(hdr->type), hdr->seqno, p_in->data_size,
		    hdr->byte_buffer_offset));
    if (VERBOSE) {
	int i;
	for (i = 0; i < MIN((int)hdr->length + NI_GM_PACKET_HDR_LEN, 32); i ++) {
	    VPRINTSTR(750, ("0x%x ", packet->data[i]));
	}
	VPRINTSTR(750, ("\n"));
    }

    return is_rendez_vous;
}


static
jint
ni_gm_input_post_byte(struct s_input *p_in)
{
    struct s_port      *p_port;
    struct s_packet    *p_packet;
    jint		result;
    jbyte	       *buffer;
    ni_gm_hdr_p	hdr;

    __in__();
    p_port = p_in->p_port;
    ni_gm_input_packet_get(p_in);
    p_packet = p_in->packet;
    assert(p_packet != NULL);

    buffer = (jbyte *)(p_packet->data + p_in->offset);
    result = ((int)buffer[0]) & 0xFF;
    p_in->offset += sizeof(jbyte);
    VPRINTF(100, ("Remove one byte=%d\n", (char)result));

    if (p_in->offset == p_in->data_size) {
	if (! ni_gm_packet_clear(p_packet, p_port)) {
	    goto error;
	}
    } else {
	/* There is pending data. Signal this for the next post. */
        ni_gm_lock_unlock(p_in->p_lock);
    }

    __out__();
    return result;

error:
    __err__();
    return -1;
}


#define GM_POST_BUFFER(jtype, jarray, Jtype) \
\
static \
int \
ni_gm_input_post_ ## Jtype ## _rndz_vous_data(JNIEnv *env, \
					      struct s_input *p_in, \
					      jtype ## Array b, \
					      jint len, \
					      jint offset, \
					      int data_size) \
{ \
    int get_region = ni_gm_copy_get_elts || data_size < COPY_THRESHOLD; \
    struct s_request *p_rq; \
    jtype            *buffer; \
    struct s_port    *p_port = p_in->p_port; \
    ni_gm_hdr_p	      hdr; \
    \
    __in__(); \
    VPRINTF(100, ("Post rendez-vous %s buffer size %d\n", #Jtype, len)); \
    if (get_region) { \
	buffer = cache_msg_get(p_port->p_gm_port, data_size, offset); \
	offset = 0; \
    } else { \
	buffer = (*env)->Get ## Jtype ## ArrayElements(env, b, NULL); \
    } \
    \
    ni_gm_register_block(p_port, buffer, len, &p_in->p_cache); \
    p_in->length       = len; \
    p_in->java.jarray  = (jtype ## Array)(*env)->NewGlobalRef(env, b); \
    p_in->array        = buffer; \
    p_in->is_copy      = get_region; \
    if (ni_gm_check_receive_tokens(p_port)) { \
	goto error; \
    } \
    \
    gm_provide_receive_buffer_with_tag(p_port->p_gm_port, \
				       buffer, \
				       NI_GM_BLOCK_SIZE, \
				       GM_LOW_PRIORITY, \
				       0); \
    \
    p_port->active_input = p_in; \
    \
    p_rq = &p_in->request; \
    p_rq->status = GM_SUCCESS; \
    \
    if (ni_gm_check_send_tokens(p_port)) { \
	goto error; \
    } \
    \
    hdr = (ni_gm_hdr_p)p_in->ack_packet; \
    hdr->type = NI_GM_MSG_TYPE_RENDEZ_VOUS_ACK; \
    \
    assert(--p_port->ni_gm_send_tokens >= 0); \
    gm_send_with_callback(p_port->p_gm_port, \
			  p_in->ack_packet, \
			  p_in->packet_size, \
			  NI_GM_PACKET_HDR_LEN, \
			  GM_HIGH_PRIORITY, \
			  p_in->src_node_id, \
			  p_in->src_port_id, \
			  ni_gm_callback, p_rq); \
    \
    __out__(); \
    return 0; \
    \
error: \
    __err__(); \
    return -1; \
} \
 \
 \
static \
int \
ni_gm_input_post_ ## Jtype ## _buffer(JNIEnv *env, \
				      struct s_input *p_in, \
				      jtype ## Array b, \
				      jint len, \
				      jint offset, \
				      int *result) { \
    struct s_packet  *p_packet; \
    int               data_size; \
    int               is_rendez_vous; \
    jtype            *buffer; \
    \
    __in__(); \
    assert(b); \
    assert(len); \
    is_rendez_vous = ni_gm_input_packet_get(p_in); \
    p_packet = p_in->packet; \
    assert(p_packet != NULL); \
    \
    VPRINTF(100, ("At post: %p pending data? (offset %d data_size %d); require %d bytes of type %s\n", p_packet, p_in->offset, p_in->data_size, len, #Jtype)); \
    \
    if (! is_rendez_vous) { \
	/* memcpy(&data_size, p_packet->data + p_in->offset, sizeof(data_size)); */ \
	data_size = *(int *)(p_packet->data + p_in->offset); \
	buffer = (jtype *)(p_packet->data + p_in->offset + sizeof(data_size)); \
	(*env)->Set ## Jtype ## ArrayRegion(env, \
					    b, \
					    offset / sizeof(jtype), \
					    data_size / sizeof(jtype), \
					    buffer); \
	VPRINTF(100, ("Copy received %s msg offset %d len %d (in bytes)\n", \
		      #Jtype, offset, data_size)); \
	if (VERBOSE) { \
	    int i; \
	    for (i = 0; i < MIN((int)(data_size / sizeof(jtype)), 32); i ++) { \
		VPRINTSTR(750, ("0x%x ", buffer[i])); \
	    } \
	    VPRINTSTR(750, ("\n")); \
	} \
	p_in->offset += data_size + sizeof(data_size); \
	p_in->data_left -= data_size + sizeof(data_size); \
	VPRINTF(100, ("Copied in post: data_size %d p_in->data_size %d p_in->offset %d p_in->data_left %d\n", \
		      data_size, p_in->data_size, p_in->offset, p_in->data_left)); \
    } else { \
	ni_gm_hdr_p hdr = (ni_gm_hdr_p)p_packet->data; \
	data_size = hdr->length; \
	p_in->data_left -= data_size; /* Trigger clear/refresh condition */ \
	VPRINTF(100, ("Post gets rendez-vous req data size %d data_left %d\n", data_size, p_in->data_left)); \
    } \
    assert(p_in->data_left >= 0); \
    \
    if (p_in->data_left == 0) { \
	if (! ni_gm_packet_clear(p_packet, p_in->p_port)) { \
	    goto error; \
	} \
    } else { \
	/* There is pending data. Signal this for the next post. */ \
	ni_gm_lock_unlock(p_in->p_lock); \
    } \
    \
    if (is_rendez_vous) { \
	if (ni_gm_input_post_ ## Jtype ## _rndz_vous_data(env, \
							  p_in, \
							  b, \
							  len, \
							  offset, \
							  data_size) == -1) { \
	    goto error; \
	} \
	*result = JNI_RENDEZ_VOUS_REQUEST; \
    } else { \
	*result = data_size; \
    } \
    \
    __out__(); \
    return 0; \
    \
error: \
    __err__(); \
    return -1; \
}

GM_POST_BUFFER(jboolean, j_boolean, Boolean)
GM_POST_BUFFER(jbyte,    j_byte,    Byte)
GM_POST_BUFFER(jshort,   j_short,   Short)
GM_POST_BUFFER(jchar,    j_char,    Char)
GM_POST_BUFFER(jint,     j_int,     Int)
GM_POST_BUFFER(jlong,    j_long,    Long)
GM_POST_BUFFER(jfloat,   j_float,   Float)
GM_POST_BUFFER(jdouble,  j_double,  Double)


/*
 * See comments in the Java code.
 */
static
int
ni_gm_input_post_byte_buffer(JNIEnv *env,
			     struct s_input *p_in,
			     jbyteArray b,
			     jint len,
			     jint offset,
			     int *result) {
    struct s_packet  *p_packet;
    int		data_size;
    int		is_rendez_vous;
    jbyte      *buffer;
    int		byte_buffer_offset;
    ni_gm_hdr_p	hdr;

    __in__();
    assert(b);
    assert(len);
    is_rendez_vous = ni_gm_input_packet_get(p_in);
    VPRINTF(100, ("At post: Got packet (offset %d data_size %d data_left %d); require %d bytes of type %s\n", p_in->offset, p_in->data_size, p_in->data_left, len, "Byte"));
    p_packet = p_in->packet;
    assert(p_packet != NULL);
    assert(! p_in->byte_buffer_consumed);

    VPRINTF(100, ("At post: %p pending data? (offset %d data_size %d data_left %d); require %d bytes of type %s\n", p_packet, p_in->offset, p_in->data_size, p_in->data_left, len, "Byte"));

    hdr = (ni_gm_hdr_p)p_packet->data;
    if (! is_rendez_vous) {
	assert(hdr->byte_buffer_offset >= 0);
	byte_buffer_offset = hdr->byte_buffer_offset;
	VPRINTF(500, ("Byte buffer packet: byte buffer offset %d\n", byte_buffer_offset));

	memcpy(&data_size, p_packet->data + byte_buffer_offset, sizeof(data_size));
	byte_buffer_offset += sizeof(data_size);
	buffer = (jbyte *)(p_packet->data + byte_buffer_offset);
	(*env)->SetByteArrayRegion(env, b, offset, data_size, buffer);
	VPRINTF(100, ("Copy received %s msg offset %d len %d (in bytes)\n",
		      "Byte", offset, data_size));
	if (VERBOSE) {
	    int i;
	    for (i = 0; i < MIN((int)data_size, 32); i ++) {
		VPRINTSTR(750, ("0x%x ", buffer[i]));
	    }
	    VPRINTSTR(750, ("\n"));
	}

	p_in->data_left -= data_size + sizeof(data_size);
	p_in->byte_buffer_consumed = 1;
	VPRINTF(100, ("Copied in post: data_size %d p_in->data_size %d p_in->offset %d p_in->data_left %d\n",
		      data_size, p_in->data_size, p_in->offset, p_in->data_left));
    } else {
	data_size = hdr->length;
	p_in->data_left -= data_size;	/* Trigger clear/refresh condition */
	VPRINTF(100, ("Post gets rendez-vous req data size %d data_left %d\n", data_size, p_in->data_left));
    }

    assert(p_in->data_left >= 0);
    assert(data_size <= p_in->data_size - p_in->offset + NI_GM_PACKET_HDR_LEN);

    if (p_in->data_left == 0) {
	if (! ni_gm_packet_clear(p_packet, p_in->p_port)) {
	    goto error;
	}
    } else {
	/* There is pending data. Signal this for the next post. */
	ni_gm_lock_unlock(p_in->p_lock);
    }

    if (is_rendez_vous) {
	if (ni_gm_input_post_Byte_rndz_vous_data(env,
						 p_in,
						 b,
						 len,
						 offset,
						 data_size) == -1) {
	    goto error;
	}
	*result = JNI_RENDEZ_VOUS_REQUEST;
    } else {
	*result = data_size;
    }

    __out__();
    return 0;

error:
    __err__();
    return -1;
}


static
int
ni_gm_output_flow_control(struct s_port   *p_port,
                          unsigned char   *msg,
                          unsigned char   *packet) {
        struct s_output *p_out          = NULL;
        int              len            =    0;
        int              mux_id         =    0;
	ni_gm_hdr_p	hdr;

        __in__();
        if (msg) {
		hdr = (ni_gm_hdr_p)msg;
        } else {
		hdr = (ni_gm_hdr_p)packet;
        }
	mux_id = hdr->mux_id;

        p_out = p_port->local_output_array[mux_id];

        if (ni_gm_check_receive_tokens(p_port)) {
                goto error;
        }

        gm_provide_receive_buffer_with_tag(p_port->p_gm_port,
                                           packet,
                                           p_port->packet_size,
                                           GM_HIGH_PRIORITY,
					   1);

        ni_gm_lock_unlock(p_out->p_lock);

        __out__();
        return 0;

 error:
        __err__();
        return -1;
}

static
int
ni_gm_input_flow_control(struct s_port  *p_port,
                         unsigned char *msg,
                         unsigned char *packet,
			 int            packet_length) {
        struct s_input  *p_in     = NULL;
        struct s_packet *p_packet = p_port->packet_head;
        int              len            =    0;
        int              mux_id         =    0;
	ni_gm_hdr_p	hdr;

        __in__();
        __disp__("0");
        if (msg) {
		hdr = (ni_gm_hdr_p)msg;
		if (hdr->type == NI_GM_MSG_TYPE_RENDEZ_VOUS_REQ) {
		    memcpy(packet, msg, packet_length);
		} else {
		    memcpy(packet, msg, hdr->length + NI_GM_PACKET_HDR_LEN);
		}
        } else {
		hdr = (ni_gm_hdr_p)packet;
        }
	mux_id = hdr->mux_id;
	VPRINTF(200, ("here... msg %p type %s len %d mux_id %d\n",
			msg, ni_gm_msg_type(hdr->type), hdr->length, hdr->mux_id));

        __disp__("1");
        p_in = p_port->local_input_array[mux_id];

        do {
                if (packet == p_port->packet_head->data) {
                        goto found;
		}

                p_port->packet_head = p_port->packet_head->next;
        } while (p_port->packet_head != p_packet);

        __error__("invalid packet: %p", packet);
        goto error;

 found:
        __disp__("2");
        p_packet = ni_gm_remove_packet_from_list(&p_port->packet_head);
        p_port->nb_packets--;
        ni_gm_add_packet_to_list_tail(&p_in->packet_head, p_packet);
	VPRINTF(300, ("At insert: p_in %p lockId %d Packet %p type = %s\n",
			p_in, p_in->p_lock->id, p_packet,
			ni_gm_msg_type(hdr->type)));

        __disp__("3");
        if (p_port->nb_packets < NI_GM_MIN_PACKETS) {
                p_packet = malloc(sizeof(struct s_request));
                assert(p_packet);

                p_packet->data = gm_dma_malloc(p_port->p_gm_port, NI_GM_PACKET_LEN);
                assert(p_packet->data);

                if (ni_gm_check_receive_tokens(p_port)) {
                        goto error;
                }

                ni_gm_add_packet_to_list_head(&p_port->packet_head, p_packet);
                gm_provide_receive_buffer_with_tag(p_port->p_gm_port,
                                                   p_packet->data,
                                                   p_port->packet_size,
                                                   GM_HIGH_PRIORITY,
						   1);
                p_port->nb_packets++;
        }
        __disp__("4");
        __disp__("gm_high_receive_event: unlock(%d)\n", p_in->p_lock->id);

        ni_gm_lock_unlock(p_in->p_lock);

        __out__();
        return 0;

 error:
        __err__();
        return -1;
}

#if HANDLE_FAST_SPECIALLY

static
int
ni_gm_process_fast_high_recv_event(struct s_port   *p_port,
                                   gm_recv_event_t *p_event) {
        unsigned char *msg            = NULL;
        unsigned char *packet         = NULL;
	int            packet_length;
	ni_gm_hdr_p	hdr;

        __in__();
        packet = gm_ntohp(p_event->recv.buffer);
        msg    = gm_ntohp(p_event->recv.message);
	hdr    = (ni_gm_hdr_p)msg;
	VPRINTF(150, ("Receive FAST/HIGH packet type %s\n",
		    ni_gm_msg_type(hdr->type)));
	packet_length = (int)gm_ntohl(p_event->recv.length);

	switch (hdr->type) {
	case NI_GM_MSG_TYPE_CONNECT:
	case NI_GM_MSG_TYPE_RENDEZ_VOUS_REQ:
	case NI_GM_MSG_TYPE_EAGER:
	    if (ni_gm_input_flow_control(p_port, msg, packet, packet_length)) {
		goto error;
	    }
	    break;

	default:
	    if (ni_gm_output_flow_control(p_port, msg, packet)) {
		goto error;
	    }
        }

        __out__();
        return 0;

 error:
        __err__();
        return -1;
}

#endif

static
int
ni_gm_process_high_recv_event(struct s_port   *p_port,
                              gm_recv_event_t *p_event)
{
        unsigned char *packet         = NULL;
	int            packet_length;
	ni_gm_hdr_p	hdr;

        __in__();
        packet = gm_ntohp(p_event->recv.buffer);
	hdr    = (ni_gm_hdr_p)packet;
	packet_length = (int)gm_ntohl(p_event->recv.length);
	VPRINTF(150, ("Receive HIGH packet type %s length %d seqno %llu\n",
		    ni_gm_msg_type(hdr->type), packet_length, hdr->seqno));

	switch (hdr->type) {

	case NI_GM_MSG_TYPE_EAGER:
	case NI_GM_MSG_TYPE_RENDEZ_VOUS_REQ:
	case NI_GM_MSG_TYPE_CONNECT:
	    if (ni_gm_input_flow_control(p_port, NULL, packet, packet_length)) {
		goto error;
	    }
	    break;

	default:
	    if (ni_gm_output_flow_control(p_port, NULL, packet)) {
		goto error;
	    }
        }

        __out__();
        return 0;

 error:
        __err__();
        return -1;
}


/*
 * Receive a RENDEZ_VOUS data packet
 */
static
int
ni_gm_process_recv_event(struct s_port   *p_port,
                         gm_recv_event_t *p_event)
{
        struct s_input *p_in;
        int            remote_node_id;
	int            length = gm_ntohl(p_event->recv.length);

        __in__();
        p_in = p_port->active_input;

        remote_node_id = gm_ntohs(p_event->recv.sender_node_id);
        assert(remote_node_id == (int)p_in->src_node_id);

        ni_gm_deregister_block(p_port, p_in->p_cache);
        ni_gm_release_input_array(p_in, length);
        ni_gm_input_unlock(p_in, length);
        __out__();

        return 0;
}


static
int
ni_gm_output_exit(struct s_output *p_out)
{
        __in__();
        p_out->p_port->ref_count--;
        memset(p_out->p_lock, 0, sizeof(struct s_lock));
        free(p_out->p_lock);
        memset(p_out, 0, sizeof(struct s_output));
        free(p_out);

        __out__();
        return 0;
}

static
int
ni_gm_input_exit(struct s_input *p_in)
{
        __in__();
        if (p_in->packet_head) {
	    fprintf(stderr, "Warning: GM has an unprocessed packet at exit\n");
	    if (0) {
                __error__("unprocessed packet");
                goto error;
	    }
        }

        p_in->p_port->ref_count--;
        memset(p_in->p_lock, 0, sizeof(struct s_lock));
        free(p_in->p_lock);
        memset(p_in, 0, sizeof(struct s_input));
        free(p_in);
        __out__();

        return 0;

 error:
        __err__();
        return -1;
}


static
int
ni_gm_dev_init(struct s_drv  *p_drv,
               int            dev_num,
               struct s_dev **pp_dev) {
        struct s_dev *p_dev = NULL;

        *pp_dev = NULL;

        if (dev_num >= p_drv->nb_dev
            ||
            !p_drv->pp_dev[dev_num]) {

                p_dev = malloc(sizeof(struct s_dev));
                if (!p_dev) {
                        __error__("memory allocation failed");
                        goto error;
                }

                if (!p_drv->nb_dev) {
                        p_drv->pp_dev = malloc(sizeof(struct s_dev *));
                        if (!p_drv->pp_dev) {
                                free(p_dev);
                                p_dev = NULL;

                                __error__("memory allocation failed");
                                goto error;
                        }

                        p_drv->nb_dev = 1;
                } else if (dev_num >= p_drv->nb_dev) {
                        p_drv->pp_dev = realloc(p_drv->pp_dev,
                                                dev_num * sizeof(struct s_dev *));
                        if (!p_drv->pp_dev) {
                                free(p_dev);
                                p_dev = NULL;

                                __error__("memory reallocation failed");
                                goto error;
                        }

                        while (p_drv->nb_dev + 1 < dev_num) {
                                p_drv->pp_dev[p_drv->nb_dev++] = NULL;
                        }

                        p_drv->nb_dev++;
                }

                p_dev->id        = dev_num;
                p_dev->ref_count = 0;
                p_dev->p_port    = NULL;
                p_dev->p_drv     = p_drv;

                if (ni_gm_open_port(p_dev)) {
                        __error__("port opening failed");
                        goto error;
                }

                p_drv->pp_dev[dev_num] = p_dev;
        } else {
                p_dev = p_drv->pp_dev[dev_num];
        }

        p_dev->ref_count++;
        *pp_dev = p_dev;

        __out__();
        return 0;

 error:
        __err__();
        return -1;
}

/* Frees a s_port structure and its associated GM port. */
static
int
ni_gm_close_port(struct s_port *p_port) {
        __in__();
	assert(!p_port->ref_count);

        {
                while (p_port->packet_head) {
                        struct s_packet *p_packet = NULL;

                        p_packet =
                                ni_gm_remove_packet_from_list(&p_port->packet_head);
                        p_port->nb_packets--;

                        gm_dma_free(p_port->p_gm_port, p_packet->data);
                        p_packet->data     = NULL;
                        p_packet->previous = NULL;
                        p_packet->next     = NULL;
                        free(p_packet);
                }
        }

        assert(!p_port->nb_packets);

        while (p_port->cache_head) {
                if (ni_gm_deregister_block(p_port, p_port->cache_head)) {
                        __error__("block deregistration failed");
                        goto error;
                }
        }

	gm_close(p_port->p_gm_port);

        free(p_port->local_input_array);
        free(p_port->local_output_array);

        memset(p_port, 0, sizeof(struct s_port));
        free(p_port);

        __out__();
	return 0;

 error:
        __err__();
        return -1;
}

static
int
ni_gm_dev_exit(struct s_dev *p_dev) {
        __in__();
        p_dev->ref_count--;
        __disp__("dev ref_count = %d", p_dev->ref_count);
pdump();

        if (!p_dev->ref_count) {
                if (ni_gm_close_port(p_dev->p_port)) {
                        __error__("port closing failed");
                        goto error;
                }

                p_dev->p_drv->ref_count--;

                memset(p_dev, 0, sizeof(struct s_dev));
                free(p_dev);
        }

        __out__();
        return 0;

 error:

        __err__();
        return -1;
}

static
int
ni_gm_init(struct s_drv **pp_drv) {
	struct s_drv *p_drv = NULL;
	gm_status_t   gms   = GM_SUCCESS;

        __in__();
	if (initialized) {
		return successfully_initialized;
	}

	gms = gm_init();

	if (gms != GM_SUCCESS) {
                __error__("gm_init failed");
		ni_gm_control(gms, __LINE__);
		goto error;
	}

	p_drv = malloc(sizeof(struct s_drv));
	if (!p_drv) {
                __error__("memory allocation failed");
		goto error;
        }

	p_drv->ref_count = 1;
        p_drv->nb_dev    = 0;
        p_drv->pp_dev    = NULL;

	*pp_drv = p_drv;

	NI_GM_BLOCK_SIZE = gm_min_size_for_length(NI_GM_MAX_BLOCK_LEN);

	initialized = 1;

        __out__();
	return 0;

 error:
        __err__();
        return -1;
}

static
int
ni_gm_exit(struct s_drv *p_drv) {
        __in__();
	if (!successfully_initialized)
		goto error;

	if (p_drv) {
                if (p_drv->ref_count > 1)
                        goto error;

                p_drv->ref_count = 0;
                p_drv->nb_dev    = 0;
                free(p_drv->pp_dev);
                p_drv->pp_dev    = NULL;

                free(p_drv);
                p_drv = NULL;

                gm_finalize();
        }

        successfully_initialized = 0;

        __out__();
	return 0;

 error:
        __err__();
        return -1;
}

/* ____________________________________________________________________________
 *
 *
 * Java Native Interfacing code
 *
 * ____________________________________________________________________________
 */
static
void
ni_gm_throw_exception(JNIEnv *env,
                      char   *msg) {
        jclass cls = 0;

	if (! EXCEPTIONS_WORK) {
	    fprintf(stderr, "would throw an exception \"%s\"-- ignore\n", msg);
	    return;
	}
fprintf(stderr, "NI GM: throw an exception \"%s\"\n", msg);

        __in__();
        __trace__("ni_gm_throw_exception-->");
        assert(env);
        __trace__("ni_gm_throw_exception - 1");
        cls = ni_findClass(env, NI_IBIS_EXCEPTION);
        __trace__("ni_gm_throw_exception - 2");
        (*env)->ThrowNew(env, cls, msg);
        __trace__("ni_gm_throw_exception<--");
        __out__();
}


/*
 * Class:     GmOutput
 * Method:    nInitOutput
 * Signature: (J)J
 */
JNIEXPORT
jlong
JNICALL
Java_ibis_impl_net_gm_GmOutput_nInitOutput(JNIEnv  *env,
                                           jobject  output,
                                           jlong    device_handle) {
        struct s_dev    *p_dev  = NULL;
        struct s_output *p_out  = NULL;
        jlong            result =    0;

        __in__();
        p_dev = ni_gm_handle2ptr(device_handle);
        ni_gm_init_output(p_dev, &p_out);
        ni_gm_lock_init(env, p_out->local_mux_id*2 + 1, &p_out->p_lock);
        result = ni_gm_ptr2handle(p_out);
        __out__();
        return result;
}

/*
 * Class:     GmInput
 * Method:    nInitInput
 * Signature: (J)J
 */
JNIEXPORT
jlong
JNICALL
Java_ibis_impl_net_gm_GmInput_nInitInput(JNIEnv  *env,
                                         jobject  input,
                                         jlong    device_handle) {
        struct s_dev   *p_dev  = NULL;
        struct s_input *p_in   = NULL;
        jlong           result =    0;
	jclass in_cls = 0;
	jfieldID fld_RVR;

        __in__();
        p_dev = ni_gm_handle2ptr(device_handle);
        ni_gm_init_input(p_dev, &p_in);
        ni_gm_lock_init(env, p_in->local_mux_id*2 + 2, &p_in->p_lock);

	p_in->ref = (*env)->NewGlobalRef(env, input);
	assert(p_in->ref);

	in_cls    = (*env)->GetObjectClass(env, p_in->ref);
	assert(in_cls);

	p_in->len_id = ni_getField(env, in_cls, "blockLen", "I");
	assert(p_in->len_id);

	fld_RVR = ni_getStaticField(env, in_cls, "RENDEZ_VOUS_REQUEST", "I");
	assert(fld_RVR);
	JNI_RENDEZ_VOUS_REQUEST = (int)(*env)->GetStaticIntField(env, in_cls, fld_RVR);
	assert(JNI_RENDEZ_VOUS_REQUEST < 0);

        result = ni_gm_ptr2handle(p_in);
        __out__();
        return result;
}

/*
 * Class:     GmOutput
 * Method:    nGetOutputNodeId
 * Signature: (J)I
 */
JNIEXPORT
jint
JNICALL
Java_ibis_impl_net_gm_GmOutput_nGetOutputNodeId(JNIEnv  *env,
                                                jobject  output,
                                                jlong    output_handle){
        struct s_output *p_out  = NULL;
        int              result = 0;

        __in__();
        p_out = ni_gm_handle2ptr(output_handle);
        ni_gm_get_output_node_id(p_out, &result);
        __out__();

        return (jint)result;
}

/*
 * Class:     GmInput
 * Method:    nGetInputNodeId
 * Signature: (J)I
 */
JNIEXPORT
jint
JNICALL
Java_ibis_impl_net_gm_GmInput_nGetInputNodeId(JNIEnv  *env,
                                              jobject  input,
                                               jlong    input_handle) {
        struct s_input *p_in  = NULL;
        int             result = 0;

        __in__();
        p_in = ni_gm_handle2ptr(input_handle);
        ni_gm_get_input_node_id(p_in, &result);
        __out__();

        return (jint)result;
}

/*
 * Class:     GmOutput
 * Method:    nGetOutputPortId
 * Signature: (J)I
 */
JNIEXPORT
jint
JNICALL
Java_ibis_impl_net_gm_GmOutput_nGetOutputPortId(JNIEnv  *env,
                                                jobject  output,
                                                jlong    output_handle){
        struct s_output *p_out  = NULL;
        int              result = 0;

        __in__();
        p_out = ni_gm_handle2ptr(output_handle);
        ni_gm_get_output_port_id(p_out, &result);

        __out__();
        return (jint)result;
}

/*
 * Class:     GmInput
 * Method:    nGetInputPortId
 * Signature: (J)I
 */
JNIEXPORT
jint
JNICALL
Java_ibis_impl_net_gm_GmInput_nGetInputPortId(JNIEnv  *env,
                                              jobject  input,
                                              jlong    input_handle) {
        struct s_input *p_in   = NULL;
        int             result = 0;

        __in__();
        p_in = ni_gm_handle2ptr(input_handle);
        ni_gm_get_input_port_id(p_in, &result);
        __out__();

        return (jint)result;
}

/*
 * Class:     GmOutput
 * Method:    nGetOutputMuxId
 * Signature: (J)I
 */
JNIEXPORT
jint
JNICALL
Java_ibis_impl_net_gm_GmOutput_nGetOutputMuxId(JNIEnv  *env,
                                               jobject  output,
                                               jlong    output_handle){
        struct s_output *p_out  = NULL;
        int              result = 0;

        __in__();
        p_out = ni_gm_handle2ptr(output_handle);
        ni_gm_get_output_mux_id(p_out, &result);

        __out__();
        return (jint)result;
}

/*
 * Class:     GmInput
 * Method:    nGetInputMuxId
 * Signature: (J)I
 */
JNIEXPORT
jint
JNICALL
Java_ibis_impl_net_gm_GmInput_nGetInputMuxId(JNIEnv  *env,
                                             jobject  input,
                                             jlong    input_handle) {
        struct s_input *p_in   = NULL;
        int             result = 0;

        __in__();
        p_in = ni_gm_handle2ptr(input_handle);
        ni_gm_get_input_mux_id(p_in, &result);
        __out__();

        return (jint)result;
}

/*
 * Class:     GmOutput
 * Method:    nConnectOutput
 * Signature: (JIII)V
 */
JNIEXPORT
void
JNICALL
Java_ibis_impl_net_gm_GmOutput_nConnectOutput(JNIEnv  *env,
                                              jobject  output,
                                              jlong    output_handle,
                                              jint     remote_node_id,
                                              jint     remote_port_id,
                                              jint     remote_mux_id) {
        struct s_output *p_out = NULL;

        __in__();
        p_out = ni_gm_handle2ptr(output_handle);
        ni_gm_connect_output(p_out,
                             (int)remote_node_id,
                             (int)remote_port_id,
                             (int)remote_mux_id);
        __out__();
}

/*
 * Class:     GmInput
 * Method:    nConnectInput
 * Signature: (JIII)V
 */
JNIEXPORT
void
JNICALL
Java_ibis_impl_net_gm_GmInput_nConnectInput(JNIEnv  *env,
                                            jobject  input,
                                            jlong    input_handle,
                                            jint     remote_node_id,
                                            jint     remote_port_id,
                                            jint     remote_mux_id) {
        struct s_input *p_in = NULL;

        __in__();
        p_in = ni_gm_handle2ptr(input_handle);
        ni_gm_connect_input(p_in,
                            (int)remote_node_id,
                            (int)remote_port_id,
                            (int)remote_mux_id);
        __out__();
}


/*
 * Send a request for a rendez-vous message transfer
 */
JNIEXPORT
void
JNICALL
Java_ibis_impl_net_gm_GmOutput_nSendRequest(JNIEnv     *env,
                                            jobject     output,
                                            jlong       output_handle,
					    jint        offset,
					    jint        length) {
        struct s_output *p_out   = NULL;

        __in__();
	pstart(SEND_REQUEST);
        p_out = ni_gm_handle2ptr(output_handle);
	assert(p_out->offset == NI_GM_PACKET_HDR_LEN);
	p_out->length = length;
	VPRINTF(100, ("Send a rndvz req; p_out->offset %d\n", p_out->offset));
        if (ni_gm_output_send_request(p_out)) {
                ni_gm_throw_exception(env, "could not send a request");
                goto error;
        }

	pend(SEND_REQUEST);
        __out__();
        return;

 error:
        __err__();
	return;
}


JNIEXPORT
void
JNICALL
Java_ibis_impl_net_gm_GmOutput_nFlush(JNIEnv *env,
				      jobject this,
				      jlong output_handle)
{
    struct s_output *p_out;

    VPRINTF(300, ("In native nFlush\n"));
    p_out = ni_gm_handle2ptr(output_handle);
    ni_gm_output_flush(p_out);
}


JNIEXPORT
jboolean
JNICALL
Java_ibis_impl_net_gm_GmOutput_nTryFlush(JNIEnv *env,
					 jobject this,
					 jlong output_handle,
					 jint length)
{
    struct s_output *p_out = ni_gm_handle2ptr(output_handle);

    if (p_out == NULL) {
	fprintf(stderr, "I get a NULL handle. Havoc!\n");
	return JNI_FALSE;
    }

    if (p_out->offset > NI_GM_PACKET_HDR_LEN &&
	    p_out->offset + length + sizeof(int) > NI_GM_PACKET_LEN) {
	VPRINTF(100, ("Should flush packet; offset %d, size %d, packet_size %d\n", p_out->offset, length, NI_GM_PACKET_LEN));
	return JNI_TRUE;
    }

    return JNI_FALSE;
}


JNIEXPORT
void
JNICALL
Java_ibis_impl_net_gm_GmOutput_nSendByte(JNIEnv *env,
					 jobject this,
					 jlong output_handle,
					 jbyte value)
{
    struct s_output *p_out = ni_gm_handle2ptr(output_handle);
    jbyte      *buffer;

    assert(p_out->offset == NI_GM_PACKET_HDR_LEN ||
	    p_out->offset + sizeof(value) <= NI_GM_PACKET_LEN);
    buffer = (jbyte *)(p_out->packet->data + p_out->offset);
    buffer[0] = value;
    p_out->offset += sizeof(value);
    VPRINTF(100, ("Pushed single byte=%d size %d, offset now %d\n",
		    value, sizeof(value), p_out->offset));
}


#define SEND_BUFFER(E_TYPE, jtype, jarray, Jtype) \
 \
/* Send an "eager" message, with a data buffer folded in */ \
 \
JNIEXPORT \
void \
JNICALL \
Java_ibis_impl_net_gm_GmOutput_nSend ## Jtype ## BufferIntoRequest( \
			JNIEnv     *env, \
			jobject     output, \
			jlong       output_handle, \
			jtype ## Array  b, \
			jint        offset, \
			jint        length) \
{ \
    struct s_output *p_out; \
    jtype      *buffer  = NULL; \
    int		data_size = length; \
    unsigned char *packet_data; \
    \
    __in__(); \
    pstart(SEND_BUFFER_REQ); \
    p_out = ni_gm_handle2ptr(output_handle); \
    \
    VPRINTF(250, ("Here %s p_out %p\n", #Jtype, p_out)); \
    assert(p_out->offset == NI_GM_PACKET_HDR_LEN || \
	   p_out->offset + sizeof(data_size) + data_size <= NI_GM_PACKET_LEN); \
    \
    packet_data = p_out->packet->data; \
    memcpy(packet_data + p_out->offset, &data_size, sizeof(data_size)); \
    p_out->offset += sizeof(data_size); \
    pstart(GET_ARRAY); \
    buffer = (jtype *)(packet_data + p_out->offset); \
    (*env)->Get ## Jtype ## ArrayRegion(env, b, offset / sizeof(jtype), \
					length / sizeof(jtype), buffer); \
    if (VERBOSE) { \
	int i; \
	for (i = offset / sizeof(jtype); i < MIN((int)(length/sizeof(jtype)), 32); i++) { \
	    VPRINTSTR(750, ("0x%x ", buffer[i])); \
	} \
	VPRINTSTR(750, ("\n")); \
    } \
    p_out->offset += length; \
    VPRINTF(100, ("Pushed " #Jtype " buffer size %d, offset now %d\n", data_size, p_out->offset)); \
    pend(GET_ARRAY); \
    \
    pend(SEND_BUFFER_REQ); \
    __out__(); \
    return; \
    \
error: \
    __err__(); \
} \
 \
/* Send the data part of a rendez-vous message */ \
 \
JNIEXPORT \
void \
JNICALL \
Java_ibis_impl_net_gm_GmOutput_nSend ## Jtype ## Buffer(JNIEnv     *env, \
			jobject     output, \
			jlong       output_handle, \
			jtype ## Array  b, \
			jint        offset, \
			jint        length) \
{ \
    struct s_output *p_out   = ni_gm_handle2ptr(output_handle); \
    jtype           *buffer  = NULL; \
    int              get_region = ni_gm_copy_get_elts || length < COPY_THRESHOLD; \
    \
    __in__(); \
    pstart(SEND_BUFFER); \
    if (p_out->offset > NI_GM_PACKET_HDR_LEN) { \
	ni_gm_output_flush(p_out); \
    } \
    p_out->java.jarray	= (jtype ## Array)(*env)->NewGlobalRef(env, b); \
    \
    if (get_region) { \
	buffer = cache_msg_get(p_out->p_port->p_gm_port, length, offset); \
	(*env)->Get ## Jtype ## ArrayRegion(env, b, offset / sizeof(jtype), \
					    length / sizeof(jtype), buffer); \
	offset = 0; \
    } else { \
	buffer  = (*env)->Get ## Jtype ## ArrayElements(env, p_out->java.jarray, NULL); \
    } \
    \
    assert(buffer); \
    assert(length); \
    \
    if (!buffer) { \
	    ni_gm_throw_exception(env, "could not get array elements"); \
	    goto error; \
    } \
    \
    p_out->array	= buffer; \
    p_out->is_copy      = get_region; \
    p_out->type         = E_TYPE; \
    \
    if (ni_gm_output_send_buffer(p_out, (unsigned char *)buffer + offset, (int)length)) { \
	    ni_gm_throw_exception(env, "could not send a buffer"); \
	    goto error; \
    } \
    \
    pend(SEND_BUFFER); \
    __out__(); \
    return; \
    \
error: \
    __err__(); \
}

SEND_BUFFER(E_BOOLEAN, jboolean, j_boolean, Boolean)
SEND_BUFFER(E_BYTE,    jbyte,    j_byte,    Byte)
SEND_BUFFER(E_SHORT,   jshort,   j_short,   Short)
SEND_BUFFER(E_CHAR,    jchar,    j_char,    Char)
SEND_BUFFER(E_INT,     jint,     j_int,     Int)
SEND_BUFFER(E_LONG,    jlong,    j_long,    Long)
SEND_BUFFER(E_FLOAT,   jfloat,   j_float,   Float)
SEND_BUFFER(E_DOUBLE,  jdouble,  j_double,  Double)


/*
 * See comments in the Java code.
 */
JNIEXPORT
void
JNICALL
Java_ibis_impl_net_gm_GmOutput_nSendBufferIntoRequest(
		JNIEnv     *env,
		jobject     output,
		jlong       output_handle,
		jbyteArray  b,
		jint        offset,
		jint        length)
{
    struct s_output *p_out;
    jbyte      *buffer  = NULL;
    int		data_size = length;
    unsigned char *packet_data;
    ni_gm_hdr_p	hdr;

    __in__();
    p_out = ni_gm_handle2ptr(output_handle);
    assert(! p_out->byte_buffer);

    if (p_out->byte_buffer) {
	fprintf(stderr, "Only one byte buffer allowed!\n");
	goto error;
    }

    pstart(SEND_BUFFER_REQ);

    VPRINTF(250, ("Here %s p_out %p\n", "byte buffer", p_out));
    assert(p_out->offset == NI_GM_PACKET_HDR_LEN ||
	    p_out->offset + sizeof(data_size) + data_size <= NI_GM_PACKET_LEN);

    packet_data = p_out->packet->data;
    hdr = (ni_gm_hdr_p)packet_data;
    hdr->byte_buffer_offset = p_out->offset;
    p_out->byte_buffer = 1;
    memcpy(packet_data + p_out->offset, &data_size, sizeof(data_size));
    p_out->offset += sizeof(data_size);

    pstart(GET_ARRAY);
    buffer = (jbyte *)(packet_data + p_out->offset);
    (*env)->GetByteArrayRegion(env, b, offset, length, buffer);
    if (VERBOSE) {
	int i;
	for (i = offset; i < MIN(length, 32); i++) {
	    VPRINTSTR(750, ("0x%x ", buffer[i]));
	}
	VPRINTSTR(750, ("\n"));
    }
    p_out->offset += length;
    VPRINTF(100, ("Pushed " "byte buffer" " buffer size %d, offset now %d\n",
		    data_size, p_out->offset));
    pend(GET_ARRAY);

    pend(SEND_BUFFER_REQ);
    __out__();
    return;

error:
    __err__();
}

JNIEXPORT
void
JNICALL
Java_ibis_impl_net_gm_GmOutput_nSendBuffer(JNIEnv     *env,
                                           jobject     output,
                                           jlong       output_handle,
                                           jbyteArray  b,
                                           jint        offset,
                                           jint        length)
{
    Java_ibis_impl_net_gm_GmOutput_nSendByteBuffer(env,
						       output,
						       output_handle,
						       b,
						       offset,
						       length);
}


JNIEXPORT
jint
JNICALL
Java_ibis_impl_net_gm_GmInput_nPostByte(JNIEnv *env,
					jobject this,
					jlong   input_handle)
{
    struct s_input *p_in = NULL;
    jbyte          *buffer;
    int             result = 0;

    __in__();

    pstart(POST_BUFFER);
    p_in = ni_gm_handle2ptr(input_handle);
    if (!p_in) {
	ni_gm_throw_exception(env, "could not get s_input from handle");
	goto error;
    }

    result = ni_gm_input_post_byte(p_in);
    if (result == -1) {
	ni_gm_throw_exception(env, "could not post a buffer");
	goto error;
    }
    pend(POST_BUFFER);

    __out__();
    __disp__("Java_ibis_impl_net_gm_GmInput_nPostByte: returning %d", (int)result);
    return ((jint)result) & 0xFF;

error:
    __err__();
    return -1;
}


#define POST_BUFFER(E_TYPE, jtype, jarray, Jtype) \
JNIEXPORT \
jint \
JNICALL \
Java_ibis_impl_net_gm_GmInput_nPost ## Jtype ## Buffer(JNIEnv     *env, \
                                              jobject     input, \
                                              jlong       input_handle, \
                                              jtype ## Array  b, \
                                              jint        offset, \
                                              jint        len) \
{ \
    struct s_input *p_in = NULL; \
    jtype          *buffer; \
    int             result = 0; \
    \
    __in__(); \
    \
    pstart(POST_BUFFER); \
    p_in = ni_gm_handle2ptr(input_handle); \
    if (!p_in) { \
	ni_gm_throw_exception(env, "could not get s_input from handle"); \
	goto error; \
    } \
    \
    p_in->type = E_TYPE; \
    if (ni_gm_input_post_ ## Jtype ## _buffer(env, p_in, b, len, offset, &result)) { \
	ni_gm_throw_exception(env, "could not post a buffer"); \
	goto error; \
    } \
    pend(POST_BUFFER); \
    \
    __out__(); \
    __disp__("Java_ibis_impl_net_gm_GmInput_nPost%s: returning %d", #Jtype, (int)result); \
    return result; \
    \
error: \
    __err__(); \
    return 0; \
}

POST_BUFFER(E_BOOLEAN, jboolean, j_boolean, Boolean)
POST_BUFFER(E_BYTE,    jbyte,    j_byte,    Byte)
POST_BUFFER(E_SHORT,   jshort,   j_short,   Short)
POST_BUFFER(E_CHAR,    jchar,    j_char,    Char)
POST_BUFFER(E_INT,     jint,     j_int,     Int)
POST_BUFFER(E_LONG,    jlong,    j_long,    Long)
POST_BUFFER(E_FLOAT,   jfloat,   j_float,   Float)
POST_BUFFER(E_DOUBLE,  jdouble,  j_double,  Double)


/*
 * Read from the "out of band" byte buffer.
 * Cannot just use the standard nPostByteBuffer.
 */
JNIEXPORT
jint
JNICALL
Java_ibis_impl_net_gm_GmInput_nPostBuffer(JNIEnv    *env,
					  jobject    input,
					  jlong      input_handle,
					  jbyteArray b,
					  jint       offset,
					  jint       len)
{
    struct s_input *p_in = NULL;
    jbyte          *buffer;
    int             result = 0;

    __in__();

    pstart(POST_BUFFER);
    p_in = ni_gm_handle2ptr(input_handle);
    if (!p_in) {
	ni_gm_throw_exception(env, "could not get s_input from handle");
	goto error;
    }

    p_in->type = E_BYTE;
    if (ni_gm_input_post_byte_buffer(env, p_in, b, len, offset, &result)) {
	ni_gm_throw_exception(env, "could not post a buffer");
	goto error;
    }
    pend(POST_BUFFER);

    __out__();
    __disp__("Java_ibis_impl_net_gm_GmInput_nPost%s: returning %d", "Buffer", (int)result);
    return result;

error:
    __err__();
    return 0;
}


JNIEXPORT
void
JNICALL
Java_ibis_impl_net_gm_GmOutput_nCloseOutput(JNIEnv  *env,
                                            jobject  output,
                                            jlong    output_handle) {
        struct s_output *p_out = NULL;

        __in__();
        p_out = ni_gm_handle2ptr(output_handle);
        if (ni_gm_output_exit(p_out)) {
                ni_gm_throw_exception(env, "could not close output");
                goto error;
        }

        __out__();
        return;

 error:
        __err__();
}

JNIEXPORT
void
JNICALL
Java_ibis_impl_net_gm_GmInput_nCloseInput(JNIEnv *env, jobject input, jlong input_handle) {
        struct s_input *p_in = NULL;

        __in__();
        p_in = ni_gm_handle2ptr(input_handle);
        if (ni_gm_input_exit(p_in)) {
		ni_gm_throw_exception(env, "could not close input");
                goto error;
        }

        __out__();
        return;

 error:
        __err__();
}


static int
mtu_init(JNIEnv *env)
{
	jclass   driver_class;
	jfieldID fid;
	jbyte   *buffer;
	jbyteArray b;

        __in__();

	driver_class      = ni_findClass(env, "ibis/impl/net/gm/Driver");
	fid               = ni_getStaticField(env, driver_class, "mtu", "I");
	mtu               = (*env)->GetStaticIntField(env, driver_class, fid);

	/* Find out whether GetByteArrayElements makes a copy of the
	 * element array for this JVM. Record this in global variable
	 * ni_gm_copy_get_elts. */
	b = (*env)->NewByteArray(env, mtu);
	buffer  = (*env)->GetByteArrayElements(env, b, &ni_gm_copy_get_elts);

	fprintf(stderr, "%s: NetGM native array: makes %s copy\n",
		hostname, ni_gm_copy_get_elts ? "a" : "no");

        __out__();
	return 0;

 error:
        __err__();
        return -1;
}


static int		ni_gm_stat_intr = 0;

#if GM_ENABLE_HERALDS

#include <pthread.h>

static int		NI_GM_INTR_FIRST = 100;	/* us */

static pthread_t	ni_gm_sigthread;
static int		ni_gm_sigthread_running = 0;
static pthread_mutex_t	ni_gm_sigthread_lock = PTHREAD_MUTEX_INITIALIZER;
static int		ni_gm_sigthread_runs = 0;
static pthread_cond_t	ni_gm_sigthread_awake = PTHREAD_COND_INITIALIZER;
static int		ni_gm_intr_disabled = 0;

static JNIEnv *
attach_sigthread(void)
{
#define MAX_VM_NUM	16
    JavaVM     *VM[MAX_VM_NUM];
    int		nVMs;
    JavaVM     *vm;
    JNIEnv     *env;

    if (JNI_GetCreatedJavaVMs(VM, MAX_VM_NUM, &nVMs) != 0) {
	__error__("JNI_GetCreatedJavaVMs fails");
    }
    if (nVMs == 0) {
	__error__("No VM!");
    }
    if (nVMs > 1) {
	fprintf(stderr, "%d VMs alive, choose only the first\n", nVMs);
    }

    vm = VM[0];

    if ((*vm)->AttachCurrentThread(vm, (void *)&env, NULL) != 0) {
	__error__("AttachCurrentThread fails");
    }

    return env;
}


static void
ni_gm_intr_handle(JNIEnv *env)
{
    while (! Java_ibis_impl_net_gm_Driver_nGmThread(env, NULL)) {
	// poll the thing
    }
    // fprintf(stderr, "Would like to generate an interrupt NOW\n");
}


static void *
sigthread(void *arg)
{
    int		next_dev = 0;
    struct s_drv *p_drv;
    JNIEnv     *env;

    pthread_mutex_lock(&ni_gm_sigthread_lock);
    ni_gm_sigthread_runs = 1;
    pthread_cond_signal(&ni_gm_sigthread_awake);
    pthread_mutex_unlock(&ni_gm_sigthread_lock);

    env = attach_sigthread();

    ni_gm_access_lock_lock(env);
    ni_gm_sigthread_running++;

    for (;;) {
	gm_recv_event_t *evt;
	int		evt_type;
	struct s_port   *p_port;
	struct s_dev    *p_dev;
	int              dev;

	p_drv = ni_gm_p_drv;

	if (next_dev >= p_drv->nb_dev) {
	    VPRINTF(1700, ("%s.%d: here... next_dev %d p_drv->nb_dev %d\n", __FILE__, __LINE__, next_dev, p_drv->nb_dev));
	    VPRINTSTR(1200, ("p"));
	    next_dev = 0;
	}

	dev     = next_dev++;
	p_dev   = p_drv->pp_dev[dev];
	p_port  = p_dev->p_port;

       /* Block in kernel for something new to arrive.
	* We get back an internal _SLEEP event, which we pass to
	* gm_unknown() to actually block this thread in the kernel.
	*/
	evt = gm_wake_when_no_herald(p_port->p_gm_port, NI_GM_INTR_FIRST);
	evt_type = (gm_ntohc(evt->recv.type) & 0xFF);

	if (evt_type == _GM_SLEEP_EVENT) {
	    ni_gm_sigthread_running--;
	    ni_gm_access_lock_unlock(env);
		// fprintf(stderr, "NetGM: intr thread sleeps\n");
	    gm_unknown(p_port->p_gm_port, evt);
		// fprintf(stderr, "NetGM: intr thread wakes up, evt %d intr_disabled %d\n", evt_type, ni_gm_intr_disabled);
	    ni_gm_access_lock_lock(env);
	    ni_gm_sigthread_running++;
	    ni_gm_stat_intr++;
		// fprintf(stderr, "NetGM: have the intr lock again\n");

	    if (ni_gm_intr_disabled) {
	        /* got interrupt just when it was being disabled;
		 * go back to sleep.
		 */
		// fprintf(stderr, "NetGM: intr thread sees a disable, continue\n");
	        continue;
	    }
	} else {
	    /* can this actually happen? */
	    gm_unknown(p_port->p_gm_port, evt);
#if 0
#else
	    __error__("net-gm: bad evt_type for intr");
	    continue;
#endif
	}

	/* A message has arrived. Hand it to one of the receiver threads. */
	ni_gm_intr_handle(env);
    }

    ni_gm_sigthread_running--;
    ni_gm_access_lock_unlock(env);
}


void
ni_gm_intr_enable(JNIEnv *env)
{
    int		i;

    for (i = 0; i < ni_gm_p_drv->nb_dev; i++) {
	struct s_dev  *p_dev  = ni_gm_p_drv->pp_dev[i];
	struct s_port *p_port = p_dev->p_port;

	if (ni_gm_intr_disabled > 0) {
	    __asm__ __volatile__ ("lock decl %0" : "=m" (ni_gm_intr_disabled));
	    gm_enable_wakeup(p_port->p_gm_port);
	} else {
	    /* indicates bug */
	    __error__("ni_gm_intr_enable(): already enabled!");
	}
    }
}


void
ni_gm_intr_disable(JNIEnv *env)
{
    int		i;

    for (i = 0; i < ni_gm_p_drv->nb_dev; i++) {
	struct s_dev  *p_dev  = ni_gm_p_drv->pp_dev[i];
	struct s_port *p_port = p_dev->p_port;

	__asm__ __volatile__ ("lock incl %0" : "=m" (ni_gm_intr_disabled));
	gm_disable_wakeup(p_port->p_gm_port);

	/* TEMP HACK: if a signal is already being processed,
	 * block in the disable itself (only needed for CRL?).
	 */
	if (ni_gm_sigthread_running &&
		! pthread_equal(ni_gm_sigthread, pthread_self())) {
	    while (ni_gm_sigthread_running) {
		ni_gm_access_lock_unlock(env);
		ni_gm_access_lock_lock(env);
	    }
	}
    }
}


static int
ni_gm_intr_init(JNIEnv *env)
{
    pthread_attr_t thread_attr;
    char *cenv;

    cenv = getenv("NI_GM_INTR_FIRST");
    if (cenv != NULL) {
	if (sscanf(cenv, "%d", &NI_GM_INTR_FIRST) != 1) {
	    __error__("NI_GM_INTR_FIRST should have an int value");
	}
	fprintf(stderr, "NI_GM_INTR_FIRST %d\n", NI_GM_INTR_FIRST);
    }

    cenv = ni_getProperty(env, "ibis.net.gm.intr.first");
    if (cenv != NULL) {
	if (sscanf(cenv, "%d", &NI_GM_INTR_FIRST) != 1) {
	    __error__("-Dibis.net.gm.intr.first should have an int value");
	}
	fprintf(stderr, "NI_GM_INTR_FIRST %d\n", NI_GM_INTR_FIRST);
    }

    cenv = getenv("NI_GM_NO_INTR");
    if (cenv != NULL ||
	    ! ni_getBooleanPropertyDflt(env, "ibis.net.gm.intr", 1)) {
	fprintf(stderr, "NetIbis GM: interrupts disabled\n");
	return 0;
    }

    if (pthread_attr_init(&thread_attr) != 0) {
	__error__("pthread_attr_init failed");
    }
    if (pthread_attr_setdetachstate(&thread_attr,
				    PTHREAD_CREATE_DETACHED) != 0) {
	__error__("pthread_attr_setdetachstate failed");
    }
    if (pthread_create(&ni_gm_sigthread, &thread_attr, sigthread, NULL) != 0) {
	__error__("pthread_create failed");
    }

    /* Nicely stop the current thread until the signal thread
     * may have finished its malloc'ing etc. */
    pthread_mutex_lock(&ni_gm_sigthread_lock);
    while (! ni_gm_sigthread_runs) {
	pthread_cond_wait(&ni_gm_sigthread_awake, &ni_gm_sigthread_lock);
    }
    pthread_mutex_unlock(&ni_gm_sigthread_lock);

fprintf(stderr, "Intpt thread dispatched\n");

    return 0;
}

#endif


JNIEXPORT
jlong
JNICALL
Java_ibis_impl_net_gm_Driver_nInitDevice(JNIEnv *env, jobject driver, jint device_num) {
        struct s_dev *p_dev  = NULL;
        jlong         result =    0;

        __in__();
        if (!ni_gm_p_drv) {
                struct s_drv *p_drv = NULL;

		gethostname(hostname, sizeof(hostname) / sizeof(hostname[0]));

                if (ni_gm_init(&p_drv))
                        goto error;

                if (ni_gm_access_lock_init(env))
                        goto error;

		if (mtu_init(env)) {
			goto error;
		}

                ni_gm_p_drv = p_drv;

#if GM_ENABLE_HERALDS
		if (ni_gm_intr_init(env)) {
		    goto error;
		}
#endif

                successfully_initialized = 1;
        }


        if (ni_gm_dev_init(ni_gm_p_drv, (int)device_num, &p_dev)) {
                ni_gm_throw_exception(env, "GM device initialization failed");
                goto error;
        }

        result = ni_gm_ptr2handle(p_dev);
        __out__();
        return result;

 error:
        __err__();
        return result;
}

JNIEXPORT
void
JNICALL
Java_ibis_impl_net_gm_Driver_nCloseDevice(JNIEnv *env, jobject driver, jlong device_handle) {
        struct s_dev *p_dev  = NULL;

        __in__();
        p_dev = ni_gm_handle2ptr(device_handle);
        if (ni_gm_dev_exit(p_dev)) {
                ni_gm_throw_exception(env, "GM device closing failed");
        }
        if (!ni_gm_p_drv->ref_count) {
                if (ni_gm_access_lock) {
                        free(ni_gm_access_lock);
                        ni_gm_access_lock = NULL;
                }

                ni_gm_exit(ni_gm_p_drv);
                ni_gm_p_drv = NULL;
                initialized = 0;
        }
        __out__();
}


#if PRINT_OWNED_MONITORS

#include <jvmdi.h>

static jthread
currentThread(JNIEnv *env)
{
    static jclass	cls_Thread;
    static jmethodID	md_currentThread;

    if (cls_Thread == NULL) {
	cls_Thread = ni_findClass(env, "java/lang/Thread");
	md_currentThread = ni_getStaticMethod(env, cls_Thread, "currentThread", "()Ljava/lang/Thread;");
    }

    return (jthread)(*env)->CallStaticObjectMethod(env, cls_Thread, md_currentThread);
}


static void
dump_monitors(JNIEnv *env)
{
    jthread	me = currentThread(env);
    JVMDI_owned_monitor_info info;
    int		i;
    int		n;
    static JavaVM *vm;
    static JVMDI_Interface_1 *jvmdi;

    if (jvmdi == NULL) {
	if ((*env)->GetJavaVM(env, &vm) != 0) {
	    fprintf(stderr, "GetJavaVM fails\n");
	    return;
	} else {
	    fprintf(stderr, "GetJavaVM -> %p\n", vm);
	}
	if ((*vm)->GetEnv(vm, &jvmdi, JVMDI_VERSION_1_3) != JNI_OK) {
	    fprintf(stderr, "GetEnv fails\n");
	    return;
	} else {
	    fprintf(stderr, "GetEnv -> jvmdi %p\n", jvmdi);
	}
    }
    if ((jvmdi)->GetOwnedMonitorInfo(me, &info) != JVMDI_ERROR_NONE) {
	fprintf(stderr, "GetOwnedMonitorInfo call fails\n");
	return;
    }
    for (i = 0; i < info.owned_monitor_count; i++) {
	fprintf(stderr, "Own monitor %d %p\n", i, info.owned_monitors[i]);
    }
    for (i = 0; i < info.owned_monitor_count; i++) {
	(*env)->DeleteGlobalRef(env, info.owned_monitors[i]);
    }
}

#else

#define dump_monitors(env)

#endif


JNIEXPORT
jboolean
JNICALL
Java_ibis_impl_net_gm_Driver_nGmThread(JNIEnv *env, jclass driver_class) {
        static int next_dev = 0;

	jboolean result = JNI_FALSE;

        _current_env = env;

dump_monitors(env);
        __in__();
	pstart(GM_THREAD);

	for (;;) {
	    struct s_port   *p_port;
	    gm_recv_event_t *p_event;
	    struct s_drv    *p_drv;
	    struct s_dev    *p_dev;
	    int              dev;

	    if (!ni_gm_p_drv->nb_dev) {
		break;
	    }

	    /*__disp__("__poll__");*/

	    p_drv = ni_gm_p_drv;

	    if (next_dev >= p_drv->nb_dev) {
		VPRINTF(1700, ("%s.%d: here... next_dev %d p_drv->nv_dev %d\n", __FILE__, __LINE__, next_dev, p_drv->nb_dev));
		VPRINTSTR(1200, ("p"));
		next_dev = 0;
		if (result) {
		    break;
		}
	    }

	    dev     = next_dev++;
	    p_dev   = p_drv->pp_dev[dev];
	    p_port  = p_dev->p_port;
	    p_event = gm_receive(p_port->p_gm_port);
	    //p_event = gm_blocking_receive(p_port->p_gm_port);

	    switch (gm_ntohc(p_event->recv.type)) {

#if HANDLE_FAST_SPECIALLY
	    case GM_FAST_HIGH_PEER_RECV_EVENT:
	    case GM_FAST_HIGH_RECV_EVENT:
		VPRINTF(150, ("Receive FAST/HIGH packet size %d\n", gm_ntohl(p_event->recv.length)));
		if (ni_gm_process_fast_high_recv_event(p_port, p_event))
			goto error;
		result = JNI_TRUE;
		break;
#endif

	    case GM_HIGH_PEER_RECV_EVENT:
	    case GM_HIGH_RECV_EVENT:
		VPRINTF(150, ("Receive from %d HIGH packet size %d\n", gm_ntohs(p_event->recv.sender_node_id), gm_ntohl(p_event->recv.length)));
		if (ni_gm_process_high_recv_event(p_port, p_event))
			goto error;
		result = JNI_TRUE;
		break;

	    case GM_PEER_RECV_EVENT:
	    case GM_RECV_EVENT:
		VPRINTF(150, ("Receive data packet size %d\n", gm_ntohl(p_event->recv.length)));
		if (ni_gm_process_recv_event(p_port, p_event))
			goto error;
		result = JNI_TRUE;
		break;

	    case GM_NO_RECV_EVENT:
		VPRINTSTR(1700, ("_"));
		result = JNI_TRUE;
		break;

	    default:
		gm_unknown(p_port->p_gm_port, p_event);
		break;
	    }
	}

	pend(GM_THREAD);
        __out__();
        _current_env = NULL;
        return result;

 error:
        __err__();
        _current_env = NULL;
fprintf(stderr, "Oho -- error in nGmThread\n");
}


#if DEPRECATED

JNIEXPORT
void
JNICALL
Java_ibis_impl_net_gm_Driver_nGmBlockingThread(JNIEnv *env, jclass driver_class) {
        static int next_dev = 0;
        _current_env = env;

        __in__();
	pstart(GM_BLOCKING_THREAD);
        success_flag = 0;

        for (;;) {
                struct s_port   *p_port  = NULL;
                gm_recv_event_t *p_event = NULL;
                struct s_drv    *p_drv   = NULL;
                struct s_dev    *p_dev   = NULL;
                int              dev     = 0;

                if (!ni_gm_p_drv->nb_dev) {
                        break;
                }

                /*__disp__("__poll__");*/

                p_drv = ni_gm_p_drv;

                if (next_dev >= p_drv->nb_dev) {
                        next_dev = 0;

                        if (success_flag) {
                                break;
                        }
                }

                dev     = next_dev++;
                p_dev   = p_drv->pp_dev[dev];
                p_port  = p_dev->p_port;
                //p_event = gm_receive(p_port->p_gm_port);
                p_event = gm_blocking_receive(p_port->p_gm_port);
                //p_event = gm_blocking_receive_no_spin(p_port->p_gm_port);

                switch (gm_ntohc(p_event->recv.type)) {

#if HANDLE_FAST_SPECIALLY
                case GM_FAST_HIGH_PEER_RECV_EVENT:
                case GM_FAST_HIGH_RECV_EVENT:
			success_flag = 1;
			if (ni_gm_process_fast_high_recv_event(p_port, p_event))
				goto error;
                        break;
#endif

                case GM_HIGH_PEER_RECV_EVENT:
                case GM_HIGH_RECV_EVENT:
			success_flag = 1;
			if (ni_gm_process_high_recv_event(p_port, p_event))
				goto error;
                        break;

                case GM_PEER_RECV_EVENT:
                case GM_RECV_EVENT:
			success_flag = 1;
			if (ni_gm_process_recv_event(p_port, p_event))
				goto error;
                        break;

                case GM_NO_RECV_EVENT:
			success_flag = 1;
                        break;

#if 0
                case _GM_SLEEP_EVENT:
                        ni_gm_access_lock_unlock(env);
                        gm_unknown (p_port->p_gm_port, p_event);
                        ni_gm_access_lock_lock(env);
                        break;
#endif

                default:
                        gm_unknown(p_port->p_gm_port, p_event);
                        break;
                }
        }

	pend(GM_BLOCKING_THREAD);
        __out__();
        _current_env = NULL;
        return;

 error:
        __err__();
        _current_env = NULL;
}

#endif


JNIEXPORT
void
JNICALL
Java_ibis_impl_net_gm_Driver_nStatistics(JNIEnv  *env,
					 jclass  clazz)
{
    fprintf(stderr, "%s: Net GM: sent: eager %d; rndvz req %d data %d intpt %d\n", hostname, sent_eager, sent_rndvz_req, sent_rndvz_data, ni_gm_stat_intr);
}


JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved) {
        __in__();
        _p_vm = vm;

	/* JNI 1.2 should be enough for now */
        __out__();
#if VERBOSE
	{
	    char *env = getenv("NET_GM_VERBOSE");
	    if (env != NULL) {
		if (sscanf(env, "%d", &verbose) != 1) {
		    fprintf(stderr, "NET_GM_VERBOSE should contain an int value\n");
		}
	    }
	    fprintf(stderr, "net GM verbose is %d\n", verbose);
	}
#endif
	return JNI_VERSION_1_2;
}

JNIEXPORT void JNICALL
JNI_OnUnload(JavaVM *vm, void *reserved) {
        __in__();
        _p_vm = NULL;
	pdump();
        __out__();
}

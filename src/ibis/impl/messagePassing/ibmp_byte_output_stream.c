/* Native methods for ibis.impl.messagePassing.ByteOutputStream
 */

#include <string.h>
#include <jni.h>

#include <pan_sys.h>
#include <pan_align.h>
#include <pan_util.h>
#include <pan_time.h>

#include "ibmp.h"
#include "ibmp_poll.h"
#include "ibmp_inttypes.h"

#include "ibis_impl_messagePassing_ByteOutputStream.h"
#include "ibis_impl_messagePassing_SendPort.h"

#include "ibp.h"
#include "ibp_mp.h"
#include "ibmp_byte_input_stream.h"
#include "ibmp_byte_output_stream.h"

#define JASON	0	/* 0 */

#if JASON
static struct pan_time now;
static struct pan_time start;
#endif

jint		ibmp_byte_stream_NO_BCAST_GROUP;

static int	ibmp_send_sync = 0;


static int	ibmp_byte_stream_port;
static int	ibmp_byte_stream_proto_size;
int	ibmp_byte_stream_proto_start;

static int	ibmp_byte_output_stream_alive = 0;

#ifdef IBP_VERBOSE
static int	sent_data = 0;
#endif

static int	send_frag = 0;
static int	send_first_frag = 0;
static int	send_last_frag = 0;
static int	send_msg = 0;
static int	send_frag_skip = 0;
static int	send_sync = 0;

static int	bcast_frag = 0;
static int	bcast_first_frag = 0;
static int	bcast_last_frag = 0;
static int	bcast_msg = 0;
static int	bcast_sync = 0;

unsigned	IBMP_FIRST_FRAG_BIT;
unsigned	IBMP_LAST_FRAG_BIT;
unsigned	IBMP_SEQNO_FRAG_BITS;


#define COUNT_GLOBAL_REFS 0
#if COUNT_GLOBAL_REFS
static int	ibmp_global_refs = 0;
#define IBMP_GLOBAL_REF_INC() \
	do { \
	    ibmp_global_refs++; \
	    if ((ibmp_global_refs % 1000) == 0 && ibmp_global_refs != 0) { \
		fprintf(stderr, "%2d: Ibis MP global refs %d\n", \
			ibmp_me, ibmp_global_refs); \
	    } \
	} while (0)
#define IBMP_GLOBAL_REF_DEC() \
	do { \
	    ibmp_global_refs--; \
	} while (0)
#else
#define IBMP_GLOBAL_REF_INC()
#define IBMP_GLOBAL_REF_DEC()
#endif

static jclass cls_ByteOutputStream;

static jfieldID fld_nativeByteOS;
static jfieldID fld_waitingInPoll;
static jfieldID fld_fragWaiting;
static jfieldID fld_outstandingFrags;
static jfieldID fld_makeCopy;
static jfieldID fld_msgCount;
static jfieldID fld_allocator;

static jmethodID md_finished_upcall;
static jmethodID md_wakeupFragWaiter;

typedef void (*release_func_t)(JNIEnv *env, void *array, void *data, jint mode);

typedef enum jprim_type {
    jprim_Boolean,
    jprim_Byte,
    jprim_Char,
    jprim_Short,
    jprim_Int,
    jprim_Long,
    jprim_Float,
    jprim_Double,
    jprim_n_types
} jprim_type_t;

typedef struct RELEASE {
    void	       *array;
    void	       *buf;
    jprim_type_t	type;
} release_t, *release_p;


typedef enum MSG_STATE {
    MSG_STATE_UNTOUCHED,
    MSG_STATE_ACCUMULATING,
    MSG_STATE_SYNC_SEND,
    MSG_STATE_ASYNC_SEND,
    MSG_STATE_CLEARING,
    MSG_STATE_N
} msg_state_t, *msg_state_p;


typedef struct IBMP_BUFFER_CACHE ibmp_buffer_cache_t, *ibmp_buffer_cache_p;

struct IBMP_BUFFER_CACHE {
    void	       *array;
    void	       *buf;
    ibmp_buffer_cache_p next;
};


static ibmp_buffer_cache_p ibmp_buffer_cache_freelist = NULL;


typedef struct IBMP_MSG ibmp_msg_t, *ibmp_msg_p;

typedef struct IBMP_BYTE_OS ibmp_byte_os_t, *ibmp_byte_os_p;

struct IBMP_MSG {
    ibmp_byte_os_p	byte_os;
    pan_iovec_p		iov;
    release_p		release;
    int			iov_len;
    int			iov_alloc_len;
    int			split_count;
    void              **proto;
    int			outstanding_send;
    jboolean		outstanding_final;
    int			copy;
    ibmp_msg_p		next;
    char		*buf;
    int			buf_alloc_len;
    int			buf_len;
    msg_state_t		state;
};

#define IOV_CHUNK	16
#define BUF_CHUNK	4096
#define COPY_THRESHOLD	64

struct IBMP_BYTE_OS {
    ibmp_msg_p		msg;
    jobject		byte_output_stream;
    int			msgSeqno;
    ibmp_msg_p		ibmp_msg_freelist;
    int			freelist_size;
    ibmp_buffer_cache_p *buffer_cache;
    ibmp_buffer_cache_p	global_refs;
};


static ibmp_msg_p	ibmp_sent_msg_q;


#ifndef NDEBUG


static void
ibmp_msg_freelist_verify(JNIEnv *env, int line, ibmp_byte_os_p byte_os)
{
    ibmp_msg_p	scan;
    int		size = 0;

    for (scan = byte_os->ibmp_msg_freelist; scan != NULL; scan = scan->next) {
	if (scan->outstanding_send != 0) {
	    fprintf(stderr, "line %d\n", line);
	}
	assert(scan->outstanding_send == 0);
	if (scan->state != MSG_STATE_UNTOUCHED) {
	    IBP_VPRINTF(1, env, ("scan %p ->state = %d\n", scan, scan->state));
	}
	assert(scan->state == MSG_STATE_UNTOUCHED);
	size++;
    }
    if (size != byte_os->freelist_size) {
	fprintf(stderr, "Free list corrupt: current size %d should be %d call from %s.%d\n", size, byte_os->freelist_size, __FILE__, line);
    }
    assert(size == byte_os->freelist_size);
}

#else
#define ibmp_msg_freelist_verify(env, line, byte_os)
#endif


static void
splitter_increase(ibmp_msg_p msg, int splitTotal)
{
    int		i;
    int		n = msg->split_count;

    msg->proto = realloc(msg->proto, sizeof(msg->proto[0]) * splitTotal);
    for (i = n; i < splitTotal; i++) {
	msg->proto[i] = pan_proto_create(ibmp_byte_stream_proto_size);
    }
    msg->split_count = splitTotal;
}



static ibmp_msg_p
ibmp_msg_get(JNIEnv *env, ibmp_byte_os_p byte_os)
{
    ibmp_msg_p msg = byte_os->ibmp_msg_freelist;

    if (msg == NULL) {
	IBP_VPRINTF(750, env, ("Do a poll in the hopes that we can recycle a msg struct\n"));
	ibmp_poll_locked(env);
	msg = byte_os->ibmp_msg_freelist;
    }

    if (msg == NULL) {
	static int live_msgs;

	msg = pan_malloc(sizeof(*msg));
	msg->split_count = 0;
	msg->proto = NULL;
	splitter_increase(msg, 1);
	msg->iov_alloc_len = IOV_CHUNK;
	msg->iov = pan_malloc(msg->iov_alloc_len * sizeof(pan_iovec_t));
	msg->release = pan_malloc(msg->iov_alloc_len * sizeof(release_t));
	msg->outstanding_send = 0;
	msg->iov_len = 0;
	msg->buf_len = 0;
	msg->buf_alloc_len = BUF_CHUNK;
	msg->buf = pan_malloc(BUF_CHUNK);
	msg->state = MSG_STATE_ACCUMULATING;
	msg->byte_os = byte_os;

    } else {
	assert(msg->iov_len == 0);
	assert(msg->buf_len == 0);
	assert(msg->state == MSG_STATE_UNTOUCHED);
	assert(msg->byte_os == byte_os);
	byte_os->ibmp_msg_freelist = msg->next;
#ifndef NDEBUG
	byte_os->freelist_size--;
	msg->state = MSG_STATE_ACCUMULATING;
#else
	if (msg->state != MSG_STATE_ACCUMULATING) {
	    fprintf(stderr, "%s.%d: inconsistent msg state %d\n", msg->state);
	}
#endif
    }

#ifndef NDEBUG
    msg->next = NULL;
#endif
    assert(msg->outstanding_send == 0);
    ibmp_msg_freelist_verify(env, __LINE__, byte_os);

    return msg;
}


static void
ibmp_msg_put(JNIEnv *env, ibmp_byte_os_p byte_os, ibmp_msg_p msg)
{
    IBP_VPRINTF(300, env, ("Now start free msg %p iov %d\n", msg, msg->iov_len));

    ibmp_msg_freelist_verify(env, __LINE__, byte_os);

#ifndef NDEBUG
    assert(msg->outstanding_send == 0);
    assert(msg->next == NULL);
    assert(msg->state == MSG_STATE_ACCUMULATING);
    msg->state = MSG_STATE_UNTOUCHED;
    byte_os->freelist_size++;
#endif

    msg->next = byte_os->ibmp_msg_freelist;
    byte_os->ibmp_msg_freelist = msg;
    ibmp_msg_freelist_verify(env, __LINE__, byte_os);
    IBP_VPRINTF(300, env, ("Now finished free msg %p iov %d\n", msg, msg->iov_len));
}


static void
ibmp_msg_enq(ibmp_msg_p msg)
{
    msg->next = ibmp_sent_msg_q;
    ibmp_sent_msg_q = msg;
}


static ibmp_buffer_cache_p
ibmp_buffer_cache_get(void)
{
    ibmp_buffer_cache_p c = ibmp_buffer_cache_freelist;

    if (c == NULL) {
	c = malloc(sizeof(*c));
    } else {
	ibmp_buffer_cache_freelist = c->next;
    }

    c->next = NULL;

    return c;
}


static void
ibmp_buffer_cache_put(ibmp_buffer_cache_p c)
{
    c->next = ibmp_buffer_cache_freelist;
    ibmp_buffer_cache_freelist = c;
}


JNIEXPORT void JNICALL
Java_ibis_impl_messagePassing_ByteOutputStream_clearGlobalRefs(
	JNIEnv *env, jobject this)
{
    jint	byteOS = (*env)->GetIntField(env, this, fld_nativeByteOS);
    ibmp_byte_os_p byte_os = (ibmp_byte_os_p)byteOS;
    ibmp_buffer_cache_p c;

    while (byte_os->global_refs != NULL) {
	c = byte_os->global_refs;
	byte_os->global_refs = c->next;
	IBP_VPRINTF(300, env, ("Now delete global ref %p\n", c->array));
	(*env)->DeleteGlobalRef(env, c->array);
	IBMP_GLOBAL_REF_DEC();
	IBP_VPRINTF(755, env, ("Now deleted global ref %p\n", c->array));
	ibmp_buffer_cache_put(c);
    }
}


#define RELEASE_ARRAY(JType, jtype) \
\
static void \
release_ ## JType ## _array(JNIEnv *env, jtype ## Array array, jtype *buf) \
{ \
    IBP_VPRINTF(800, NULL, ("%s: Now release type %s array %p buf %p\n", \
		ibmp_currentThread(env), #JType, array, buf)); \
    (*env)->Release ## JType ## ArrayElements(env, array, buf, JNI_ABORT); \
    IBP_VPRINTF(300, env, ("Now delete global ref %p\n", array)); \
    (*env)->DeleteGlobalRef(env, array); \
    IBMP_GLOBAL_REF_DEC(); \
    IBP_VPRINTF(755, env, ("Now deleted global ref %p\n", array)); \
} \
\
\
JNIEXPORT jtype ## Array JNICALL \
Java_ibis_impl_messagePassing_ByteOutputStream_getCached ## JType ## Buffer( \
	JNIEnv *env, jobject this) \
{ \
    jint	byteOS = (*env)->GetIntField(env, this, fld_nativeByteOS); \
    ibmp_byte_os_p byte_os = (ibmp_byte_os_p)byteOS; \
    ibmp_buffer_cache_p c; \
    \
    assert(byte_os != NULL); \
    c = byte_os->buffer_cache[jprim_ ## JType]; \
    if (c == NULL) { \
	return NULL; \
    } \
    \
    byte_os->buffer_cache[jprim_ ## JType] = c->next; \
    \
    IBP_VPRINTF(800, NULL, ("%s: Now release type %s array %p buf %p\n", \
		ibmp_currentThread(env), #JType, c->array, c->buf)); \
    (*env)->Release ## JType ## ArrayElements(env, \
		    c->array, c->buf, JNI_ABORT); \
    \
    /* Enqueue in the list of global refs to be cleared. We can clear the \
     * global ref only *after* it has been returned to Java space. */ \
    c->next = byte_os->global_refs; \
    byte_os->global_refs = c; \
    \
    return c->array; \
}

RELEASE_ARRAY(Boolean, jboolean)
RELEASE_ARRAY(Byte, jbyte)
RELEASE_ARRAY(Char, jchar)
RELEASE_ARRAY(Short, jshort)
RELEASE_ARRAY(Int, jint)
RELEASE_ARRAY(Long, jlong)
RELEASE_ARRAY(Float, jfloat)
RELEASE_ARRAY(Double, jdouble)

#undef RELEASE_ARRAY


JNIEXPORT void JNICALL
Java_ibis_impl_messagePassing_ByteOutputStream_releaseCachedBuffers(
	JNIEnv *env, jobject this)
{
    jint	byteOS = (*env)->GetIntField(env, this, fld_nativeByteOS);
    ibmp_byte_os_p byte_os = (ibmp_byte_os_p)byteOS;
    int		i;
    ibmp_buffer_cache_p c;

    assert(byte_os != NULL);
#define RELEASE_ARRAY(JType) \
    while ((c = byte_os->buffer_cache[jprim_ ## JType]) != NULL) { \
	byte_os->buffer_cache[jprim_ ## JType] = c->next; \
	release_ ## JType ## _array(env, c->array, c->buf); \
	ibmp_buffer_cache_put(c); \
    }

RELEASE_ARRAY(Boolean)
RELEASE_ARRAY(Byte)
RELEASE_ARRAY(Char)
RELEASE_ARRAY(Short)
RELEASE_ARRAY(Int)
RELEASE_ARRAY(Long)
RELEASE_ARRAY(Float)
RELEASE_ARRAY(Double)

#undef RELEASE_ARRAY
}



static void
ibmp_msg_release_iov(JNIEnv *env, ibmp_msg_p msg)
{
    int		i;

    if (msg->state == MSG_STATE_ACCUMULATING) {
	/* Lazy call. Our work has already been done. */
	return;
    }

    assert(msg->state == MSG_STATE_ASYNC_SEND ||
	    msg->state == MSG_STATE_SYNC_SEND);
#ifndef NDEBUG
    msg->state = MSG_STATE_CLEARING;
#endif
    if (msg->copy) {
	ibmp_lock_check_owned(env);
	for (i = 0; i < msg->iov_len; i++) {
	    IBP_VPRINTF(800, NULL, ("Now free msg %p iov %p size %d\n",
			msg, msg->iov[i].data, msg->iov[i].len));
	    pan_free(msg->iov[i].data);
	}
    } else {
	jobject allocator = (*env)->GetObjectField(env,
					   msg->byte_os->byte_output_stream,
					   fld_allocator);
	for (i = 0; i < msg->iov_len; i++) {
	    IBP_VPRINTF(800, NULL, ("%s: Now cache msg %p iov %p size %d release type %d array %p buf %p\n",
			ibmp_currentThread(env),
			msg, msg->iov[i].data, msg->iov[i].len,
			msg->release[i].type, msg->release[i].array,
			msg->release[i].buf));
	    if (msg->release[i].type != jprim_n_types) {
		if (allocator == NULL) {
		    switch (msg->release[i].type) {
#define RELEASE_ARRAY(JType) \
		    case jprim_ ## JType: \
			release_ ## JType ## _array(env, \
						    msg->release[i].array, \
						    msg->release[i].buf); \
			break; 

		    RELEASE_ARRAY(Boolean)
		    RELEASE_ARRAY(Byte)
		    RELEASE_ARRAY(Char)
		    RELEASE_ARRAY(Short)
		    RELEASE_ARRAY(Int)
		    RELEASE_ARRAY(Long)
		    RELEASE_ARRAY(Float)
		    RELEASE_ARRAY(Double)
#undef RELEASE_ARRAY

		    default:
			break;
		    }

		} else {
		    ibmp_buffer_cache_p c = ibmp_buffer_cache_get();
		    ibmp_byte_os_p byte_os = msg->byte_os;

		    IBP_VPRINTF(280, env, ("enqueue array%d %p byte_os %p for reuse\n",
				msg->release[i].type, msg->release[i].array,
				byte_os));
		    c->next = byte_os->buffer_cache[msg->release[i].type];
		    byte_os->buffer_cache[msg->release[i].type] = c;

		    c->array = msg->release[i].array;
		    c->buf = msg->release[i].buf;
#ifndef NDEBUG
		    msg->release[i].array = NULL;
#endif
		}
	    }
	}
    }
    msg->iov_len = 0;
    msg->buf_len = 0;

    msg->state = MSG_STATE_ACCUMULATING;
}


static void
handle_finished_send(JNIEnv *env, ibmp_msg_p msg)
{
#if JASON
   struct pan_time tt1;
   struct pan_time tt2;
#endif
    ibmp_msg_p	handle;
    jboolean waitingInPoll;
    jboolean fragWaiting;
    jint outstandingFrags;
    ibmp_byte_os_p byte_os = msg->byte_os;

    IBP_VPRINTF(300, env, ("Do a finished upcall msg %p obj %p byte_os->msgSeqno %d\n",
		msg, byte_os->byte_output_stream,
		byte_os->msgSeqno));

#if JASON
    pan_time_get(&now);
    pan_time_sub(&now, &start);
    printf("%2d: %lf: delayed clear\n", ibp_me, pan_time_t2d(&now));
    pan_time_get(&tt1);
#endif

#ifndef NDEBUG
    assert(msg->state == MSG_STATE_ASYNC_SEND);
#endif

    // IBP_VPRINTF(300, env, ("Here...\n"));

    ibmp_msg_release_iov(env, msg);

    assert(msg->state == MSG_STATE_ACCUMULATING);

    /* We'd better be sure we own the PandaIbis lock, because these
     * (test and set)s (handle, outstandingFrags, msg->byte_output_stream)
     * otherwise are no way atomic */
    // IBP_VPRINTF(300, env, ("Here...\n"));

    waitingInPoll = (*env)->GetBooleanField(env, byte_os->byte_output_stream,
					    fld_waitingInPoll);

    IBP_VPRINTF(300, env, ("Here... waitingInPoll %d\n", waitingInPoll));

    outstandingFrags = (*env)->GetIntField(env,
					   byte_os->byte_output_stream,
					   fld_outstandingFrags);

    if (waitingInPoll && outstandingFrags == 1) {
	IBP_VPRINTF(300, env, ("Here... outstandingFrags %d\n", outstandingFrags));
	(*env)->CallVoidMethod(env, byte_os->byte_output_stream, md_finished_upcall);
    } else {
	IBP_VPRINTF(300, env, ("Here... outstandingFrags %d\n", outstandingFrags));
	outstandingFrags--;
	(*env)->SetIntField(env,
			    byte_os->byte_output_stream,
			    fld_outstandingFrags,
			    outstandingFrags);
	fragWaiting = (*env)->GetBooleanField(env, byte_os->byte_output_stream,
					    fld_fragWaiting);
	if (fragWaiting) {
	    (*env)->CallVoidMethod(env, byte_os->byte_output_stream, md_wakeupFragWaiter);
	}

    }

    IBP_VPRINTF(300, env, ("Here...\n"));

    handle = byte_os->msg;

    // IBP_VPRINTF(300, env, ("Here... handle %x\n", handle));
    if (handle == NULL) {
	/**
	 * Ensure that there is a msg ready for a push/send for the next
	 * message
	 */
	IBP_VPRINTF(720, env, ("After Async send restore obj %p byte_os->msg to %p\n", byte_os->byte_output_stream, msg));
	byte_os->msg = msg;
    } else if (handle != msg) {
	assert(msg->next == NULL);
	ibmp_msg_put(env, byte_os, msg);
	assert(msg->state == MSG_STATE_UNTOUCHED);
    }

    IBP_VPRINTF(300, env, ("Here...\n"));

#if JASON
    pan_time_get(&tt2);
    pan_time_sub(&tt2, &tt1);
    printf("upcall = %lf \n", pan_time_t2d(&tt2));
#endif
}


static int
ibmp_msg_q_poll(JNIEnv *env)
{
    ibmp_msg_p msg = ibmp_sent_msg_q;
    ibmp_msg_p prev = NULL;
    ibmp_msg_p next;
    int		done_anything = 0;

    if (0 && msg != NULL) {
	fprintf(stderr, "Queue { ");
	for (next = msg; next != NULL; next = next->next) {
	    fprintf(stderr, "%p[%d] ", msg, msg->outstanding_send);
	}
	fprintf(stderr, " } ");
    }

    while (msg != NULL) {
	next = msg->next;
	if (msg->outstanding_send == 0 /* && msg->outstanding_final */) {
	    IBP_VPRINTF(800, env, ("Handle sent upcall for msg %p\n", msg));
#ifndef NDEBUG
	    msg->next = NULL;
#endif
	    handle_finished_send(env, msg);
	    IBP_VPRINTF(800, env, ("Handled sent upcall for msg %p, byte_os->msgSeqno %d\n", msg, msg->byte_os->msgSeqno));
	    done_anything = 1;

	    if (prev == NULL) {
		ibmp_sent_msg_q = next;
	    } else {
		prev->next = next;
	    }
	} else {
	    prev = msg;
	}
	msg = next;
    }

    return done_anything;
}


static void
ibmp_msg_deq(ibmp_msg_p msg)
{
    ibmp_msg_p scan = ibmp_sent_msg_q;
    ibmp_msg_p prev = NULL;
    ibmp_msg_p next;

    while (scan != NULL) {
	next = scan->next;
	if (msg == scan) {
	    if (prev == NULL) {
		ibmp_sent_msg_q = next;
	    } else {
		prev->next = next;
	    }
#ifndef NDEBUG
	    msg->next = NULL;
#endif
	    break;
	}
	scan = next;
    }
    assert(scan != NULL);
}


#ifdef IBP_VERBOSE
static int ibmp_sent_msg_out;
#endif


static void
sent_upcall(void *arg)
{
    ibmp_msg_p msg = arg;
    msg->outstanding_send--;
    IBP_VPRINTF(1, NULL, ("sent upcall msg %p outstanding := %d, missing := %d\n", msg, msg->outstanding_send, --ibmp_sent_msg_out));
#if 0 && defined IBP_VERBOSE
    {
	int i;
	int j;

	fprintf(stderr, "Iovec we will clear:\n");
	for (i = 0; i < msg->iov_len; i++) {
	    fprintf(stderr, "buffer %p: ", msg->iov[i]);
	    for (j = 0; j < 24; j++) {
		fprintf(stderr, "%d ", ((int *)(msg->iov[i].data))[j]);
	    }
	    fprintf(stderr, "\n");
	}
    }
#endif
}



#ifdef IBP_VERBOSE

static void
print_byte(FILE *out, uint8_t x)
{
    fprintf(out, "0x%x ", x);
}

#define DUMP_LIMIT	128
#define DUMP_DATA(jtype, fmt, cast) \
static void dump_ ## jtype(jtype *b, int len) \
{ \
    int		i; \
    \
    if (ibmp_verbose < 300) return; \
    fprintf(stderr, "sizeof(cast) = %d\n", sizeof(cast)); \
    \
    for (i = 0; i < len; i++) { \
	if (sizeof(jtype) == sizeof(jbyte)) { \
	    print_byte(stderr, (uint8_t)b[i]); \
	} else { \
	    fprintf(stderr, "%" fmt, (cast)b[i]); \
	} \
	if (i * sizeof(jtype) >= DUMP_LIMIT) { \
	    fprintf(stderr, " ..."); \
	    break; \
	} \
    } \
    fprintf(stderr, "\n"); \
}
#else
#define DUMP_DATA(jtype, fmt, cast) \
static void dump_ ## jtype(jtype *b, int len) \
{ \
}
#endif

DUMP_DATA(jboolean, "d ", int8_t)
DUMP_DATA(jbyte, "c", int8_t)
DUMP_DATA(jchar, "d ", int16_t)
DUMP_DATA(jshort, "d ", int16_t)
DUMP_DATA(jint, "d ", int32_t)
DUMP_DATA(jlong, "lld ", int64_t)
DUMP_DATA(jfloat, "f ", float)
DUMP_DATA(jdouble, "f ", double)

#undef DUMP_DATA


static ibmp_msg_p
ibmp_msg_check(JNIEnv *env,
	       ibmp_byte_os_p byte_os,
	       jint locked)
{
    ibmp_msg_p msg;

    if (byte_os->msg != NULL) {
	IBP_VPRINTF(1000, env, ("Return current msg %p\n", byte_os->msg));
	return byte_os->msg;
    }

    if (! locked) {
	ibmp_lock(env);
    }

    ibmp_msg_freelist_verify(env, __LINE__, byte_os);

    msg = ibmp_msg_get(env, byte_os);
    byte_os->msg = msg;
    ibmp_msg_freelist_verify(env, __LINE__, byte_os);
    if (! locked) {
	ibmp_unlock(env);
    }

    msg->copy = (int)(*env)->GetBooleanField(env,
					     byte_os->byte_output_stream,
					     fld_makeCopy);
    IBP_VPRINTF(820, env,
		("Make %s intermediate copy in this BytOutputStream\n",
		 msg->copy ? "an" : "NO"));

    return msg;
}


static void
check_home_msg(jint cpu)
{
    static int first_pass = 1;

    if (cpu == ibmp_me) {
	if (first_pass) {
	    first_pass = 0;
	    fprintf(stderr, "Send message to my own Ibis; implement shortcut?\n");
	}
    }
}


static int
empty_frag(JNIEnv *env,
	   ibmp_msg_p msg,
	   int firstFrag,
	   int lastFrag)
{
    if ((lastFrag && firstFrag) || (msg != NULL && msg->iov_len != 0)) {
	return 0;
    }

    IBP_VPRINTF(250, env, ("Skip send of an empty non-single fragment msg %p, firstFrag %s lastFrag %s\n", msg, firstFrag ? "yes" : "no", lastFrag ? "yes" : "no"));

    send_frag_skip++;

    return 1;
}


static int
finished_from_send(JNIEnv *env,
		   ibmp_msg_p msg)
{
    /* You never know whether the async msg has already been acked,
     * so let's check.
     * If we would poll the send completion queue here, the sent upcall might
     * be cleared and enqueued and in the wrong state, but a poll of the MP
     * layer below this is OK. */
    ibp_mp_poll(env);

    if (msg->outstanding_send != 0 || msg->state != MSG_STATE_ASYNC_SEND) {
	return 0;
    }

    IBP_VPRINTF(450, env, ("Dequeue msg %p\n", msg));
    ibmp_msg_deq(msg);
    ibmp_msg_release_iov(env, msg);

    return 1;
}


/*
 * Send msg async style to one of the connects.
 * Return whether the send has already been acked
 */
static jboolean
send_async(JNIEnv *env,
	   jobject this,
	   jint cpu,
	   ibmp_byte_os_p byte_os,
	   ibmp_msg_p msg,
	   jint i,
	   jint splitTotal)
{
    IBP_VPRINTF(300, env, ("ByteOS %p Enqueue a send-finish upcall msg %p obj %p, missing := %d proto %p\n",
		byte_os->byte_output_stream, msg, this, ++ibmp_sent_msg_out,
		msg->proto[i]));
#ifdef IBP_VERBOSE
    if (ibmp_verbose >= 1 && ibmp_verbose < 300) {
	++ibmp_sent_msg_out;
    }
#endif

    if (i == 0) {
	assert(msg->state == MSG_STATE_ACCUMULATING);
	assert(ibmp_equals(env, byte_os->byte_output_stream, this));
	assert(msg->outstanding_send == 0);

	msg->outstanding_send += splitTotal;
	msg->outstanding_final = 1;

	ibmp_msg_freelist_verify(env, __LINE__, byte_os);
	assert(msg->next == NULL);

	msg->state = MSG_STATE_ASYNC_SEND;
	ibmp_msg_enq(msg);

	ibmp_msg_freelist_verify(env, __LINE__, byte_os);
	IBP_VPRINTF(450, env, ("Enqueued msg %p\n", msg));
    } else {
	assert(msg->state == MSG_STATE_ASYNC_SEND);
    }

    ibp_mp_send_async(env, (int)cpu, ibmp_byte_stream_port,
		      msg->iov, msg->iov_len,
		      msg->proto[i], ibmp_byte_stream_proto_size,
		      sent_upcall, msg);

    if (finished_from_send(env, msg)) {
	return JNI_TRUE;
    }

    ibmp_msg_freelist_verify(env, __LINE__, byte_os);
    IBP_VPRINTF(450, env, ("Msg %p must be acked/released from upcall\n", msg));

    if (i == splitTotal - 1) {
	byte_os->msg = NULL;
    }

    return JNI_FALSE;
}



/*
 * Send msg to one of the connects.
 * Return whether the send has already been acked
 */
JNIEXPORT jboolean JNICALL
Java_ibis_impl_messagePassing_ByteOutputStream_msg_1send(
	JNIEnv *env, 
	jobject this,
	jint cpu,
	jint port,
	jint my_port,
	jint msgSeqno,
	jint i,			/* Split count */
	jint splitTotal,
	jboolean lastFrag	/* Frag is sent as last frag of a message */
	)
{
#if JASON
    struct pan_time st;
    struct pan_time et;
#endif
    jint	nativeByteOS = (*env)->GetIntField(env, this, fld_nativeByteOS);
    ibmp_byte_os_p byte_os = (ibmp_byte_os_p)nativeByteOS;
    ibmp_msg_p	msg = byte_os->msg;
    ibmp_byte_stream_hdr_p hdr;
    int		len;
    long long int up_to_now;
    int		lastSplitter = (i == splitTotal - 1);
    int		firstFrag;

#if JASON
    pan_time_get(&st);
#endif

    IBP_VPRINTF(100, env, ("msg_send: %d to-port %d my-port %d seqno %d, splitter %d last %d\n", cpu, port, my_port, msgSeqno, i, splitTotal));

    if (! ibmp_byte_output_stream_alive) {
	(*env)->ThrowNew(env, cls_java_io_IOException, "Ibis MessagePassing ByteOutputStream closed");
	return JNI_TRUE;
    }

    check_home_msg(cpu);

    firstFrag = (byte_os->msgSeqno != msgSeqno);

    if (empty_frag(env, msg, firstFrag, lastFrag)) {
	return JNI_TRUE;
    }

    if (firstFrag) {
	send_msg++;
	assert(byte_os->msgSeqno + 1 == msgSeqno);
	if (lastSplitter) {
	    byte_os->msgSeqno = msgSeqno;
	}
    }

    send_frag++;
    if (firstFrag) send_first_frag++;
    if (lastFrag && lastSplitter) send_last_frag++;

    assert(msg != NULL);
    assert(ibmp_equals(env, byte_os->byte_output_stream, this));

    splitter_increase(msg, splitTotal);

    hdr = ibmp_byte_stream_hdr(msg->proto[i]);

    hdr->dest_port = port;
    hdr->src_port  = my_port;
    if (lastFrag) {
	msgSeqno |= IBMP_LAST_FRAG_BIT;
    }
    if (firstFrag) {
	msgSeqno |= IBMP_FIRST_FRAG_BIT;
    }
    hdr->msgSeqno = msgSeqno;
    hdr->group = ibmp_byte_stream_NO_BCAST_GROUP;

    len = ibmp_iovec_len(msg->iov, msg->iov_len);
    up_to_now = (*env)->GetLongField(env, this, fld_msgCount);
    (*env)->SetLongField(env, this, fld_msgCount, len + up_to_now);

#ifdef IBP_VERBOSE
    sent_data += len;
#endif

    assert(ibmp_equals(env, byte_os->byte_output_stream, this));

    if (pan_thread_nonblocking() || len >= ibmp_send_sync) {
	IBP_VPRINTF(250, env, ("ByteOS %p Do this send in Async fashion msg %p seqno %d; lastFrag=%s data size %d iov_size %d\n", byte_os->byte_output_stream, msg, msgSeqno, lastFrag ? "yes" : "no", ibmp_iovec_len(msg->iov, msg->iov_len), msg->iov_len));

	return send_async(env, this, cpu, byte_os, msg, i, splitTotal);
    }

    /* Wow, we're allowed to do a sync send! */
    IBP_VPRINTF(250, env, ("ByteOS %p Do this send in sync fashion msg %p seqno %d; lastFrag=%s data size %d iov_size %d\n", byte_os->byte_output_stream, msg, msgSeqno, lastFrag ? "yes" : "no", ibmp_iovec_len(msg->iov, msg->iov_len), msg->iov_len));

    if (i == 0) {
	assert(msg->state == MSG_STATE_ACCUMULATING);
	msg->state = MSG_STATE_SYNC_SEND;
    } else {
	assert(msg->state == MSG_STATE_SYNC_SEND);
    }

    ibp_mp_send_sync(env, (int)cpu, ibmp_byte_stream_port,
		     msg->iov, msg->iov_len,
		     msg->proto[i], ibmp_byte_stream_proto_size);

    if (lastSplitter) {
	ibmp_msg_release_iov(env, msg);
    }
    send_sync++;

    return JNI_TRUE;
}


/*
 * Do native broadcast to this group.
 * Return whether the send has already been acked
 */
JNIEXPORT jboolean JNICALL
Java_ibis_impl_messagePassing_ByteOutputStream_msg_1bcast(
	JNIEnv *env, 
	jobject this,
	jint group,
	jint msgSeqno,
	jboolean lastFrag	/* Frag is sent as last frag of a message */
	)
{
#if JASON
    struct pan_time st;
    struct pan_time et;
#endif
    jint	nativeByteOS = (*env)->GetIntField(env, this, fld_nativeByteOS);
    ibmp_byte_os_p byte_os = (ibmp_byte_os_p)nativeByteOS;
    ibmp_msg_p	msg = byte_os->msg;
    ibmp_byte_stream_hdr_p hdr;
    int		len;
    long long int up_to_now;
    int		firstFrag;

#if JASON
    pan_time_get(&st);
#endif

    IBP_VPRINTF(100, env, ("msg_bcast: msg %p group %d seqno %d\n", msg, (int)group, (int)msgSeqno));

    if (! ibmp_byte_output_stream_alive) {
	(*env)->ThrowNew(env, cls_java_io_IOException, "Ibis MessagePassing ByteOutputStream closed");
	return JNI_TRUE;
    }

    firstFrag = byte_os->msgSeqno != msgSeqno;

    if (empty_frag(env, msg, firstFrag, lastFrag)) {
	return JNI_TRUE;
    }

    if (firstFrag) {
	bcast_msg++;
	byte_os->msgSeqno = msgSeqno;
    }

    bcast_frag++;
    if (firstFrag) bcast_first_frag++;
    if (lastFrag) bcast_last_frag++;

    assert(msg != NULL);
    assert(ibmp_equals(env, byte_os->byte_output_stream, this));

    hdr = ibmp_byte_stream_hdr(msg->proto[0]);

    if (lastFrag) {
	msgSeqno |= IBMP_LAST_FRAG_BIT;
    }
    if (firstFrag) {
	msgSeqno |= IBMP_FIRST_FRAG_BIT;
    }
    hdr->msgSeqno = msgSeqno;
    hdr->group = group;
    hdr->home_msg = msg;

    len = ibmp_iovec_len(msg->iov, msg->iov_len);
    up_to_now = (*env)->GetLongField(env, this, fld_msgCount);
    (*env)->SetLongField(env, this, fld_msgCount, len + up_to_now);

#ifdef IBP_VERBOSE
    sent_data += len;
#endif

    IBP_VPRINTF(250, env, ("ByteOS %p Do this bcast in Async fashion msg %p seqno %d; lastFrag=%s data size %d iov_size %d\n",
		byte_os->byte_output_stream, msg, msgSeqno,
		lastFrag ? "yes" : "no",
		ibmp_iovec_len(msg->iov, msg->iov_len), msg->iov_len));

    IBP_VPRINTF(300, env, ("ByteOS %p Enqueue a bcast-finish upcall msg %p obj %p, missing := %d proto %p\n",
		byte_os->byte_output_stream, msg, this, ++ibmp_sent_msg_out,
		msg->proto[0]));
#ifdef IBP_VERBOSE
    if (ibmp_verbose >= 1 && ibmp_verbose < 300) {
	++ibmp_sent_msg_out;
    }
#endif

    assert(msg->state == MSG_STATE_ACCUMULATING);
    assert(ibmp_equals(env, byte_os->byte_output_stream, this));
    assert(msg->outstanding_send == 0);

    msg->outstanding_send++;
    msg->outstanding_final = lastFrag;

    ibmp_msg_freelist_verify(env, __LINE__, byte_os);
    assert(msg->next == NULL);

    msg->state = MSG_STATE_ASYNC_SEND;
    ibmp_msg_enq(msg);

    ibmp_msg_freelist_verify(env, __LINE__, byte_os);
    IBP_VPRINTF(450, env, ("Enqueued msg %p object %p\n", msg, byte_os->byte_output_stream));

    ibp_mp_bcast(env, ibmp_byte_stream_port,
		 msg->iov, msg->iov_len,
		 msg->proto[0], ibmp_byte_stream_proto_size);

    assert(ibmp_equals(env, byte_os->byte_output_stream, this));

    if (finished_from_send(env, msg)) {
	bcast_sync++;
	return JNI_TRUE;
    }

#ifdef IBP_VERBOSE
    if (! pan_thread_nonblocking() && len < ibmp_send_sync) {
	IBP_VPRINTF(25, env, ("would like to do a sync bcast size %d, but alas\n",
		    ibmp_me, len));
    }
#endif

    ibmp_msg_freelist_verify(env, __LINE__, byte_os);
    IBP_VPRINTF(450, env, ("Msg %p must be acked/released from upcall\n", msg));

    byte_os->msg = NULL;

    return JNI_FALSE;
}


void
ibmp_bcast_home_ack(ibmp_byte_stream_hdr_p hdr)
{
    ibmp_msg_p msg = hdr->home_msg;

    msg->outstanding_send--;
    IBP_VPRINTF(1, NULL, ("bcast sent upcall msg %p group %d outstanding := %d, missing := %d\n", msg, hdr->group, msg->outstanding_send, --ibmp_sent_msg_out));
}


JNIEXPORT jint JNICALL
Java_ibis_impl_messagePassing_ByteOutputStream_init(
	JNIEnv *env,
	jobject this)
{
    jobject bos = (*env)->NewGlobalRef(env, this);
    ibmp_byte_os_p byte_os = calloc(1, sizeof(*byte_os));
    ibmp_msg_p msg;

    byte_os->byte_output_stream = bos;
    byte_os->buffer_cache = calloc(jprim_n_types,
				   sizeof(*byte_os->buffer_cache));
    byte_os->msgSeqno = -1;

    ibmp_msg_check(env, byte_os, 0);
    assert(ibmp_equals(env, byte_os->byte_output_stream, this));

    return (jint)byte_os;
}


JNIEXPORT void JNICALL
Java_ibis_impl_messagePassing_ByteOutputStream_close(
	JNIEnv *env, 
	jobject this)
{
    ibmp_error(env, "close not implemented\n");
}


static void
iovec_grow(JNIEnv *env, ibmp_msg_p msg, int locked)
{
    if (msg->iov_len == msg->iov_alloc_len) {
	if (! locked) {
	    ibmp_lock(env);
	}
	if (msg->iov_alloc_len == 0) {
	    msg->iov_alloc_len = IOV_CHUNK;
	} else {
	    msg->iov_alloc_len *= 2;
	}
	msg->iov = pan_realloc(msg->iov,
			       msg->iov_alloc_len * sizeof(pan_iovec_t));
	msg->release = pan_realloc(msg->release,
				   msg->iov_alloc_len * sizeof(release_t));
	if (! locked) {
	    ibmp_unlock(env);
	}
    }
}


static int
buf_grow(JNIEnv *env, ibmp_msg_p msg, int incr, int locked)
{
    assert(((incr + 7) & ~7) >= incr);

    incr = (incr + 7) & ~7;
    if (msg->buf_len + incr > msg->buf_alloc_len) {
	char *old_buf = msg->buf;
	int i;

	if (! locked) {
	    ibmp_lock(env);
	}
	while (msg->buf_len + incr > msg->buf_alloc_len) {
	    if (msg->buf_alloc_len == 0) {
		msg->buf_alloc_len = BUF_CHUNK;
	    } else {
		msg->buf_alloc_len *= 2;
	    }
	}
	msg->buf = pan_realloc(msg->buf, msg->buf_alloc_len);
	if (! locked) {
	    ibmp_unlock(env);
	}
	for (i = 0; i < msg->iov_len; i++) {
	    if ((char *)(msg->iov[i].data) >= old_buf &&
		(char *)(msg->iov[i].data) < old_buf + msg->buf_len) {
		int diff = (char *) (msg->iov[i].data) - old_buf;
		msg->iov[i].data = msg->buf + diff;
	    }
	}
    }
    return incr;
}


JNIEXPORT void JNICALL
Java_ibis_impl_messagePassing_ByteOutputStream_write(
	JNIEnv *env, 
	jobject this,
	jint b)
{
    jint	nativeByteOS = (*env)->GetIntField(env, this, fld_nativeByteOS);
    ibmp_byte_os_p byte_os = (ibmp_byte_os_p)nativeByteOS;
    ibmp_msg_p	msg = ibmp_msg_check(env, byte_os, 0);

    if (! msg->copy) {
	int incr = buf_grow(env, msg, sizeof(jbyte), 0);
	iovec_grow(env, msg, 0);
	*((unsigned char *) (&msg->buf[msg->buf_len])) = (unsigned char)(b & 0xFF);
	msg->iov[msg->iov_len].data = &(msg->buf[msg->buf_len]);
	msg->iov[msg->iov_len].len = sizeof(jbyte);
	msg->release[msg->iov_len].type = jprim_n_types;
	msg->buf_len += incr;

    } else {
	unsigned char *buf;

	ibmp_lock(env);
	iovec_grow(env, msg, 1);

	buf = pan_malloc(sizeof(unsigned char));
	ibmp_unlock(env);
	*buf = (unsigned char)(b & 0xFF);
	msg->iov[msg->iov_len].data = buf;
	msg->iov[msg->iov_len].len = sizeof(jbyte);
    }
    IBP_VPRINTF(300, env, ("Now push byte ByteOS %p msg %p data %p size %d iov %d, value %d\n",
		msg->byte_os->byte_output_stream, msg,
		msg->iov[msg->iov_len].data, msg->iov[msg->iov_len].len,
		msg->iov_len, b));
    msg->iov_len++;
}


JNIEXPORT void JNICALL
Java_ibis_impl_messagePassing_ByteOutputStream_report(
	JNIEnv *env, 
	jobject this)
{
    jint	nativeByteOS = (*env)->GetIntField(env, this, fld_nativeByteOS);
    ibmp_byte_os_p byte_os = (ibmp_byte_os_p)nativeByteOS;
    ibmp_msg_p	msg = ibmp_msg_check(env, byte_os, 0);
    int		total;
    int		i;

    total = 0;
    for (i = 0; i < msg->iov_len; i++) {
	total += msg->iov[i].len;
    }
    IBP_VPRINTF(300, env, ("ByteOutputStream: bytes %d items %d\n",
		total, msg->iov_len));
}



#define ARRAY_WRITE(JType, jtype, JPrim) \
JNIEXPORT void JNICALL \
Java_ibis_impl_messagePassing_ByteOutputStream_writeArray___3 ## JPrim ## II( \
	JNIEnv *env,  \
	jobject this, \
	jtype ## Array b, \
	jint off, \
	jint len) \
{ \
    jint	nativeByteOS = (*env)->GetIntField(env, this, fld_nativeByteOS); \
    ibmp_byte_os_p byte_os = (ibmp_byte_os_p)nativeByteOS; \
    ibmp_msg_p	msg = ibmp_msg_check(env, byte_os, 0); \
    jtype      *buf; \
    int		sz = len * sizeof(jtype); \
    \
    if (! msg->copy && sz >= COPY_THRESHOLD) { \
	b = (*env)->NewGlobalRef(env, b); \
	IBMP_GLOBAL_REF_INC(); \
	IBP_VPRINTF(800, env, ("%s: Now create global ref %p\n", \
		    ibmp_currentThread(env), b)); \
    } \
    if (msg->copy) { \
	ibmp_lock(env); \
	iovec_grow(env, msg, 1); \
	buf = pan_malloc(sz); \
	ibmp_unlock(env); \
	msg->iov[msg->iov_len].data = buf; \
	msg->iov[msg->iov_len].len = sz; \
	(*env)->Get ## JType ## ArrayRegion(env, b, (jsize) off, (jsize) len, (jtype *) buf); \
    } else { \
	iovec_grow(env, msg, 0); \
	if (sz < COPY_THRESHOLD) { \
	    int incr = buf_grow(env, msg, sz, 0); \
	    (*env)->Get ## JType ## ArrayRegion(env, b, (jsize) off, (jsize) len, (jtype *) &(msg->buf[msg->buf_len])); \
	    msg->iov[msg->iov_len].data = &(msg->buf[msg->buf_len]); \
	    msg->buf_len += incr; \
	    msg->iov[msg->iov_len].len  = sz; \
	    msg->release[msg->iov_len].type  = jprim_n_types; \
	} \
	else { \
	    jtype *a = (*env)->Get ## JType ## ArrayElements(env, b, NULL); \
	    msg->iov[msg->iov_len].data = a + off; \
	    msg->iov[msg->iov_len].len  = sz; \
	    msg->release[msg->iov_len].array = b; \
	    msg->release[msg->iov_len].buf   = a; \
	    msg->release[msg->iov_len].type  = jprim_ ## JType; \
	} \
    } \
    IBP_VPRINTF(300, env, ("Now push ByteOS %p msg %p %s source %p data %p size %d iov %d total %d [%d,%d,%d,%d,...]\n", \
		msg->byte_os->byte_output_stream, msg, #JType, b, msg->iov[msg->iov_len].data, \
		msg->iov[msg->iov_len].len, msg->iov_len, ibmp_iovec_len(msg->iov, msg->iov_len + 1), msg->iov[0].len, msg->iov[1].len, msg->iov[2].len, msg->iov[3].len)); \
    dump_ ## jtype((jtype *)(msg->iov[msg->iov_len].data), len); \
    msg->iov_len++; \
}

ARRAY_WRITE(Boolean, jboolean, Z)
ARRAY_WRITE(Byte, jbyte, B)
ARRAY_WRITE(Char, jchar, C)
ARRAY_WRITE(Short, jshort, S)
ARRAY_WRITE(Int, jint, I)
ARRAY_WRITE(Long, jlong, J)
ARRAY_WRITE(Float, jfloat, F)
ARRAY_WRITE(Double, jdouble, D)

#undef ARRAY_WRITE


void
ibmp_byte_output_stream_report(JNIEnv *env, FILE *f)
{
#ifdef IBP_VERBOSE
    fprintf(stderr, "%2d: ByteOutputStream.sent data %d\n", ibmp_me, sent_data);
#endif
    fprintf(f, "%2d: send msg %d frag %d (skip %d sync %d) bcast %d frag %d sync %d intpts %d",
	    ibmp_me, send_last_frag, send_frag, send_frag_skip, send_sync,
	    bcast_last_frag, bcast_frag, bcast_sync, ibp_intpts);
}


void
ibmp_byte_output_stream_init(JNIEnv *env)
{
    jfieldID	fld;
    jclass	cls_SendPort;

    cls_ByteOutputStream = (*env)->FindClass(env,
			 "ibis/impl/messagePassing/ByteOutputStream");
    if (cls_ByteOutputStream == NULL) {
	ibmp_error(env, "Cannot find class ibis/impl/messagePassing/ByteOutputStream\n");
    }
    cls_ByteOutputStream = (jclass)(*env)->NewGlobalRef(env, (jobject)cls_ByteOutputStream);

    fld_nativeByteOS       = (*env)->GetFieldID(env,
					 cls_ByteOutputStream,
					 "nativeByteOS", "I");
    if (fld_nativeByteOS == NULL) {
	ibmp_error(env, "Cannot find static field nativeByteOS:I\n");
    }

    fld_waitingInPoll    = (*env)->GetFieldID(env,
					 cls_ByteOutputStream,
					 "waitingInPoll", "Z");
    if (fld_waitingInPoll == NULL) {
	ibmp_error(env, "Cannot find static field waitingInPoll:Z\n");
    }

    fld_fragWaiting    = (*env)->GetFieldID(env,
					 cls_ByteOutputStream,
					 "fragWaiting", "Z");
    if (fld_fragWaiting == NULL) {
	ibmp_error(env, "Cannot find static field fragWaiting:Z\n");
    }

    fld_outstandingFrags = (*env)->GetFieldID(env,
					 cls_ByteOutputStream,
					 "outstandingFrags", "I");
    if (fld_outstandingFrags == NULL) {
	ibmp_error(env, "Cannot find static field outstandingFrags:I\n");
    }

    fld_makeCopy = (*env)->GetFieldID(env,
					 cls_ByteOutputStream,
					 "makeCopy", "Z");
    if (fld_makeCopy == NULL) {
	ibmp_error(env, "Cannot find static field makeCopy:Z\n");
    }

    fld_msgCount = (*env)->GetFieldID(env,
					 cls_ByteOutputStream,
					 "msgCount", "J");
    if (fld_msgCount == NULL) {
	ibmp_error(env, "Cannot find static field msgCount:I\n");
    }

    fld_allocator = (*env)->GetFieldID(env,
					 cls_ByteOutputStream,
					 "allocator",
					 "Libis/io/DataAllocator;");
    if (fld_allocator == NULL) {
	ibmp_error(env, "Cannot find static field allocator:Libis.io.DataAllocator;\n");
    }

    md_finished_upcall    = (*env)->GetMethodID(env,
						cls_ByteOutputStream,
						"finished_upcall", "()V");
    if (md_finished_upcall == NULL) {
	ibmp_error(env, "Cannot find method finished_upcall()V\n");
    }

    md_wakeupFragWaiter    = (*env)->GetMethodID(env,
						cls_ByteOutputStream,
						"wakeupFragWaiter", "()V");
    if (md_wakeupFragWaiter == NULL) {
	ibmp_error(env, "Cannot find method wakeupFragWaiter()V\n");
    }

    fld = (*env)->GetStaticFieldID(env, cls_ByteOutputStream,
	    				"FIRST_FRAG_BIT",
					"I");
    if (fld == NULL) {
	ibmp_error(env, "Cannot find static field FIRST_FRAG_BIT");
    }
    IBMP_FIRST_FRAG_BIT = (*env)->GetStaticIntField(env, cls_ByteOutputStream, fld);

    fld = (*env)->GetStaticFieldID(env, cls_ByteOutputStream,
	    				"LAST_FRAG_BIT",
					"I");
    if (fld == NULL) {
	ibmp_error(env, "Cannot find static field LAST_FRAG_BIT");
    }

    fld = (*env)->GetStaticFieldID(env, cls_ByteOutputStream,
	    				"SEQNO_FRAG_BITS",
					"I");
    if (fld == NULL) {
	ibmp_error(env, "Cannot find static field SEQNO_FRAG_BITS");
    }
    IBMP_SEQNO_FRAG_BITS = (*env)->GetStaticIntField(env, cls_ByteOutputStream, fld);

    cls_SendPort = (*env)->FindClass(env, "ibis/impl/messagePassing/SendPort");
    if (cls_SendPort == NULL) {
	ibmp_error(env, "Cannot find class ibis/impl/messagePassing/SendPort");
    }
    fld = (*env)->GetStaticFieldID(env, cls_SendPort,
	    				"NO_BCAST_GROUP",
					"I");
    if (fld == NULL) {
	ibmp_error(env, "Cannot find static field NO_BCAST_GROUP");
    }
    ibmp_byte_stream_NO_BCAST_GROUP =
	(*env)->GetStaticIntField(env, cls_SendPort, fld);

    ibmp_byte_stream_port = ibp_mp_port_register(ibmp_byte_stream_handle);
    ibmp_byte_stream_proto_start = align_to(ibp_mp_proto_offset(), ibmp_byte_stream_hdr_t);
    ibmp_byte_stream_proto_size  = ibmp_byte_stream_proto_start + sizeof(ibmp_byte_stream_hdr_t);

    ibmp_poll_register(ibmp_msg_q_poll);

    if (pan_arg_int(NULL, NULL, "-ibp-send-sync", &ibmp_send_sync) == -1) {
	ibmp_error(env, "-ibp-send-sync requires an int argument\n");
    }

    ibmp_byte_output_stream_alive = 1;
#if JASON
    pan_time_get(&start);
#endif
}


void
ibmp_byte_output_stream_end(JNIEnv *env)
{
    ibmp_byte_output_stream_alive = 0;

    ibmp_byte_output_stream_report(env, stderr);
}

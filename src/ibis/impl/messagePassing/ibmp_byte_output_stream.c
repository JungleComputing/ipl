/* Native methods for ibis.ipl.impl.messagePassing.ByteOutputStream
 */

#include <string.h>
#include <jni.h>

#include <pan_sys.h>
#include <pan_align.h>
#include <pan_util.h>
#include <pan_time.h>

#include "ibmp.h"
#include "ibmp_poll.h"

#include "ibis_ipl_impl_messagePassing_ByteOutputStream.h"

#include "ibp.h"
#include "ibp_mp.h"
#include "ibmp_byte_input_stream.h"
#include "ibmp_byte_output_stream.h"


static int	ibmp_send_sync = 0;


static int	ibmp_byte_stream_port;
static int	ibmp_byte_stream_proto_size;
int	ibmp_byte_stream_proto_start;

#ifdef IBP_VERBOSE
static int	sent_data = 0;
#endif

#define DISABLE_SENDER_INTERRUPTS	1

#if DISABLE_SENDER_INTERRUPTS
static int	intr_enable = 0;
static int	intr_disable = 0;
#endif
static int	send_frag = 0;
static int	send_first_frag = 0;
static int	send_last_frag = 0;
static int	send_msg = 0;
static int	send_frag_skip = 0;


static jclass cls_PandaByteOutputStream;

static jfieldID fld_msgHandle;
static jfieldID fld_waitingInPoll;
static jfieldID fld_outstandingFrags;
static jfieldID fld_makeCopy;

static jmethodID md_finished_upcall;

typedef void (*release_func_t)(JNIEnv *env, void *array, void *data, jint mode);

typedef struct RELEASE {
    void	       *array;
    void	       *buf;
    release_func_t	func;
} release_t, *release_p;


typedef struct IBMP_MSG ibmp_msg_t, *ibmp_msg_p;

struct IBMP_MSG {
    jobject		byte_output_stream;
    pan_iovec_p		iov;
    release_p		release;
    int			iov_len;
    int			iov_alloc_len;
    void	       *proto;
    int			outstanding_send;
    jboolean		outstanding_final;
    int			copy;
    int			firstFrag;
    ibmp_msg_p		next;
    int			is_free;
    char		*buf;
    int			buf_alloc_len;
    int			buf_len;
};

#define IOV_CHUNK	16
#define BUF_CHUNK	4096
#define COPY_THRESHOLD	64

static ibmp_msg_p	ibmp_msg_freelist;
static ibmp_msg_p	ibmp_sent_msg_q;


#ifndef NDEBUG

static void ibmp_msg_freelist_verify(void)
{
    ibmp_msg_p scan;

    for (scan = ibmp_msg_freelist; scan != NULL; scan = scan->next) {
	assert(scan->outstanding_send == 0);
	assert(scan->is_free);
    }
}

#else
#define ibmp_msg_freelist_verify()
#endif


static ibmp_msg_p
ibmp_msg_get(void)
{
    ibmp_msg_p msg = ibmp_msg_freelist;

    if (msg == NULL) {
	msg = pan_malloc(sizeof(*msg));
	msg->proto = ibp_proto_create(ibmp_byte_stream_proto_size);
	msg->iov_alloc_len = IOV_CHUNK;
	msg->iov = pan_malloc(msg->iov_alloc_len * sizeof(pan_iovec_t));
	msg->release = pan_malloc(msg->iov_alloc_len * sizeof(release_t));
	msg->outstanding_send = 0;
	msg->iov_len = 0;
	msg->buf_len = 0;
	msg->buf_alloc_len = BUF_CHUNK;
	msg->buf = pan_malloc(BUF_CHUNK);
    } else {
	assert(msg->is_free);
	assert(msg->iov_len == 0);
	assert(msg->buf_len == 0);
	ibmp_msg_freelist = msg->next;
    }
    msg->firstFrag = 1;
#ifndef NDEBUG
    msg->is_free = 0;
#endif
    assert(msg->outstanding_send == 0);
    ibmp_msg_freelist_verify();

    return msg;
}


static void
ibmp_msg_put(JNIEnv *env, ibmp_msg_p msg)
{
    IBP_VPRINTF(300, env, ("Now freed msg %p iov %d\n", msg, msg->iov_len));
    assert(msg->outstanding_send == 0);

    ibmp_msg_freelist_verify();
    msg->next = ibmp_msg_freelist;
    ibmp_msg_freelist = msg;
#ifndef NDEBUG
    msg->is_free = 1;
#endif
    ibmp_msg_freelist_verify();
}


static void
ibmp_msg_enq(ibmp_msg_p msg)
{
    msg->next = ibmp_sent_msg_q;
    ibmp_sent_msg_q = msg;
}


static void
ibmp_msg_release_iov(JNIEnv *env, ibmp_msg_p msg)
{
    int		i;

    if (msg->copy) {
	ibmp_lock_check_owned(env);
	for (i = 0; i < msg->iov_len; i++) {
	    IBP_VPRINTF(300, NULL, ("Now free msg %p iov %p size %d\n",
			msg, msg->iov[i].data, msg->iov[i].len));
	    pan_free(msg->iov[i].data);
	}
    } else {
	for (i = 0; i < msg->iov_len; i++) {
	    if (msg->release[i].func != NULL) {
		msg->release[i].func(env,
				     msg->release[i].array,
				     msg->release[i].buf,
				     JNI_ABORT);
		(*env)->DeleteGlobalRef(env, msg->release[i].array);
	    }
	}
    }
    msg->iov_len = 0;
    msg->buf_len = 0;
}


static void
handle_finished_send(JNIEnv *env, ibmp_msg_p msg)
{
//   union lt {
//	struct pan_time t;
//	jlong	l;
//    } tt1, tt2;
    int handle;
    jobject byte_output_stream;
    jboolean waitingInPoll;
    jint outstandingFrags;

    IBP_VPRINTF(300, env, ("Do a finished upcall obj %p\n",
		msg->byte_output_stream));

//	    pan_time_get(&tt1.t);

    byte_output_stream = msg->byte_output_stream;

    ibmp_msg_release_iov(env, msg);

    /* We'd better be sure we own the PandaIbis lock, because this test
     * and set otherwise is no way atomic */
    handle = (*env)->GetIntField(env, byte_output_stream, fld_msgHandle);
    if (handle == 0) {
	IBP_VPRINTF(720, env, ("After Async send set msgHandle to %p\n", msg));
	(*env)->SetIntField(env, byte_output_stream, fld_msgHandle, (jint)msg);
    } else {
	ibmp_msg_put(env, msg);
    }

    waitingInPoll = (*env)->GetBooleanField(env, byte_output_stream, fld_waitingInPoll);

    outstandingFrags = (*env)->GetIntField(env,
				    byte_output_stream,
				    fld_outstandingFrags);
// fprintf(stderr, "waitingInPoll = %d, outstandingFrags = %d\n", waitingInPoll, outstandingFrags);
    if (waitingInPoll && outstandingFrags == 1) {
	(*env)->CallVoidMethod(env, byte_output_stream, md_finished_upcall);
    } else {
	(*env)->SetIntField(env,
			    byte_output_stream,
			    fld_outstandingFrags,
			    outstandingFrags - 1);
    }

    (*env)->DeleteGlobalRef(env, byte_output_stream);

//	    pan_time_get(&tt2.t);
//	    printf("upcall + globalref = %lld \n", (tt2.l-tt1.l));
}


static int
ibmp_msg_q_poll(JNIEnv *env)
{
    ibmp_msg_p msg = ibmp_sent_msg_q;
    ibmp_msg_p prev = NULL;
    ibmp_msg_p next;
    int		done_anything = 0;

    //    fprintf(stderr, "msg-q-poll\n");

    while (msg != NULL) {
	next = msg->next;
	if (msg->outstanding_send == 0 /* && msg->outstanding_final */) {
	    IBP_VPRINTF(800, env, ("Handle sent upcall for msg %p\n", msg));
	    handle_finished_send(env, msg);
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
	    break;
	}
	scan = next;
    }
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
}


#ifdef IBP_VERBOSE
#define DUMP_LIMIT	128
#define DUMP_DATA(jtype, fmt, cast) \
static void dump_ ## jtype(jtype *b, int len) \
{ \
    int		i; \
    \
    if (ibmp_verbose < 750) return; \
    \
    for (i = 0; i < len; i++) { \
	fprintf(stderr, "%" fmt, (cast)b[i]); \
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


static ibmp_msg_p
ibmp_msg_check(JNIEnv *env, jobject this, int msgHandle, int locked)
{
    ibmp_msg_p msg;

    if (msgHandle != 0) {
	return (ibmp_msg_p)msgHandle;
    }

    if (! locked) {
	ibmp_lock(env);
    }
    msg = ibmp_msg_get();
    if (! locked) {
	ibmp_unlock(env);
    }
    ibmp_msg_freelist_verify();

    msg->copy = (int)(*env)->GetBooleanField(env, this, fld_makeCopy);
// fprintf(stderr, "Make %s intermediate copy in this BytOutputStream\n", msg->copy ? "an" : "NO");
    IBP_VPRINTF(820, env,
		("Make %s intermediate copy in this BytOutputStream\n",
		 msg->copy ? "an" : "NO"));
    (*env)->SetIntField(env, this, fld_msgHandle, (jint)msg);

    return msg;
}


jboolean
Java_ibis_ipl_impl_messagePassing_ByteOutputStream_msg_1send(
	JNIEnv *env, 
	jobject this,
	jint cpu,
	jint port,
	jint my_port,
	jint msgSeqno,
	jint msgHandle,
	jboolean lastSplitter,	/* Frag is sent to last of the splitters in our 1-to-many */
	jboolean lastFrag	/* Frag is sent as last frag of a message */
	)
{
//    union lt {
//	struct pan_time t;
//	jlong	l;
//    } st, tt1, tt2, et;
    ibmp_msg_p	msg = (ibmp_msg_p)msgHandle;
    ibmp_byte_stream_hdr_p hdr;
    int		len;

//    pan_time_get(&st.t);

    send_frag++;
    if (msg != NULL && msg->firstFrag) send_first_frag++;
    if (lastFrag && lastSplitter) send_last_frag++;

    if (msg == NULL || (! (lastFrag && msg->firstFrag) && msg->iov_len == 0)) {
	IBP_VPRINTF(250, env, ("Skip send of an empty non-single fragment msg seqno %d\n", msgSeqno));

	if (msg != NULL && lastFrag && lastSplitter && ! msg->firstFrag) {
#if DISABLE_SENDER_INTERRUPTS
	    // ibp_intr_enable(env);
	    intr_enable++;
#endif
	    msg->firstFrag = 1;	/* Reset for next time round */
	}
	send_frag_skip++;

	return JNI_FALSE;
    }

    if (lastFrag && lastSplitter) {
	if (! msg->firstFrag) {
#if DISABLE_SENDER_INTERRUPTS
	    // ibp_intr_enable(env);
	    intr_enable++;
#endif
	    msg->firstFrag = 1;	/* Reset for next time round */
	}
	send_msg++;
    } else {
#if DISABLE_SENDER_INTERRUPTS
	if (! lastFrag && msg->firstFrag) {
	    // ibp_intr_disable(env);
	    intr_disable++;
	}
#endif
	msg->firstFrag = 0;
    }

    hdr = ibmp_byte_stream_hdr(msg->proto);

    hdr->dest_port = port;
    hdr->src_port  = my_port;
    if (lastFrag) {
	hdr->msgSeqno = -msgSeqno;
    } else {
	hdr->msgSeqno = msgSeqno;
    }

    len = ibmp_iovec_len(msg->iov, msg->iov_len);
// fprintf(stderr, "send, len = %d, target = %d\n", len, cpu);
#ifdef IBP_VERBOSE
    sent_data += len;
#endif

// fprintf(stderr, ">");

    if (ibmp_send_sync > 0 &&
	    ! pan_thread_nonblocking() &&
	    len < ibmp_send_sync) {
	/* Wow, we're allowed to do a sync send! */
	IBP_VPRINTF(250, env, ("ByteOS %p Do this send in sync fashion msg %p seqno %d; lastFrag=%s data size %d iov_size %d\n", this, msg, msgSeqno, lastFrag ? "yes" : "no", ibmp_iovec_len(msg->iov, msg->iov_len), msg->iov_len));
	ibp_mp_send_sync(env, (int)cpu, ibmp_byte_stream_port,
			 msg->iov, msg->iov_len,
			 msg->proto, ibmp_byte_stream_proto_size);
	return JNI_FALSE;
    }

    IBP_VPRINTF(250, env, ("ByteOS %p Do this send in Async fashion msg %p seqno %d; lastFrag=%s data size %d iov_size %d\n", this, msg, msgSeqno, lastFrag ? "yes" : "no", ibmp_iovec_len(msg->iov, msg->iov_len), msg->iov_len));
    msg->outstanding_send++;
    msg->outstanding_final = lastFrag;
    ibmp_msg_freelist_verify();

//    pan_time_get(&tt1.t);

    msg->byte_output_stream = (*env)->NewGlobalRef(env, this);

//    pan_time_get(&tt2.t);

    IBP_VPRINTF(300, env, ("ByteOS %p Enqueue a send-finish upcall obj %p, missing := %d\n",
		msg->byte_output_stream, this, ++ibmp_sent_msg_out));
    ibmp_msg_freelist_verify();
    ibmp_msg_enq(msg);
    ibmp_msg_freelist_verify();

    ibp_mp_send_async(env, (int)cpu, ibmp_byte_stream_port,
		      msg->iov, msg->iov_len,
		      msg->proto, ibmp_byte_stream_proto_size,
		      sent_upcall, msg);

//    pan_time_get(&et.t);

//    printf("start->end = %lld globalref = %lld \n", (et.l-st.l), (tt2.l-tt1.l));

    /* You never know whether the async msg has already been acked,
     * so let's check */
    if (msg->outstanding_send == 0) {
	(*env)->DeleteGlobalRef(env, msg->byte_output_stream);
	ibmp_msg_release_iov(env, msg);
	ibmp_msg_deq(msg);
	return JNI_FALSE;
    }

    ibmp_msg_freelist_verify();

    return JNI_TRUE;
}


void
Java_ibis_ipl_impl_messagePassing_ByteOutputStream_init(
	JNIEnv *env,
	jobject this)
{
    (void)ibmp_msg_check(env, this, 0, 0);
}


void
Java_ibis_ipl_impl_messagePassing_ByteOutputStream_resetMsg(
	JNIEnv *env, 
	jobject this)
{
    jint	m = (*env)->GetIntField(env, this, fld_msgHandle);
    ibmp_msg_p	msg = (ibmp_msg_p)m;

    IBP_VPRINTF(300, env, ("Now reset msg %p in PandaByteOutputStream\n", msg));
    ibmp_msg_release_iov(env, msg);
}


void
Java_ibis_ipl_impl_messagePassing_ByteOutputStream_close(
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
// fprintf(stderr, "Grow iov from %d to %d\n", msg->iov_len, msg->iov_alloc_len);
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
    incr = (incr + 7) & ~7;
    if (msg->buf_len + incr > msg->buf_alloc_len) {
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
    }
    return incr;
}


void
Java_ibis_ipl_impl_messagePassing_ByteOutputStream_write(
	JNIEnv *env, 
	jobject this,
	jint b)
{
    jint	m = (*env)->GetIntField(env, this, fld_msgHandle);
    ibmp_msg_p	msg = ibmp_msg_check(env, this, m, 0);
    int       *buf;

    if (! msg->copy) {
	int incr = buf_grow(env, msg, sizeof(jint), 0);
	iovec_grow(env, msg, 0);
	*((jint *) (&msg->buf[msg->buf_len])) = b;
	msg->iov[msg->iov_len].data = &(msg->buf[msg->buf_len]);
	msg->iov[msg->iov_len].len = sizeof(jint);
	msg->release[msg->iov_len].func = NULL;
	msg->buf_len += incr;
    }
    else {
	ibmp_lock(env);
	iovec_grow(env, msg, 1);

	buf = pan_malloc(sizeof(jint));
	ibmp_unlock(env);
	*buf = (int)b;
	msg->iov[msg->iov_len].data = buf;
	msg->iov[msg->iov_len].len = sizeof(jint);
    }
    IBP_VPRINTF(300, env, ("Now push msg %p data %p size %d iov %d, value %d\n",
		msg, msg->iov[msg->iov_len].data, msg->iov[msg->iov_len].len,
		msg->iov_len, b));
    msg->iov_len++;
}


void
Java_ibis_ipl_impl_messagePassing_ByteOutputStream_report(
	JNIEnv *env, 
	jobject this)
{
    jint	m = (*env)->GetIntField(env, this, fld_msgHandle);
    ibmp_msg_p	msg = ibmp_msg_check(env, this, m, 0);
    int		total;
    int		i;

    total = 0;
    for (i = 0; i < msg->iov_len; i++) {
	total += msg->iov[i].len;
    }
    IBP_VPRINTF(300, env, ("PandaByteOutputStream: bytes %d items %d\n",
		total, msg->iov_len));
}



#define ARRAY_WRITE(JType, jtype) \
void Java_ibis_ipl_impl_messagePassing_ByteOutputStream_write ## JType ## Array( \
	JNIEnv *env,  \
	jobject this, \
	jtype ## Array b, \
	jint off, \
	jint len) \
{ \
    jint	m = (*env)->GetIntField(env, this, fld_msgHandle); \
    ibmp_msg_p	msg = ibmp_msg_check(env, this, m, 0); \
    jtype      *buf; \
    int		sz = len * sizeof(jtype); \
    \
    if (! msg->copy && sz >= COPY_THRESHOLD) { \
	b = (*env)->NewGlobalRef(env, b); \
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
if (0) { \
fprintf(stderr, "iov_len = %d, buf_len = %d\n", msg->iov_len, msg->buf_len); \
} \
	    (*env)->Get ## JType ## ArrayRegion(env, b, (jsize) off, (jsize) len, (jtype *) &(msg->buf[msg->buf_len])); \
	    msg->iov[msg->iov_len].data = &(msg->buf[msg->buf_len]); \
	    msg->buf_len += incr; \
	    msg->iov[msg->iov_len].len  = sz; \
	    msg->release[msg->iov_len].func  = NULL; \
	} \
	else { \
	    jtype *a = (*env)->Get ## JType ## ArrayElements(env, b, NULL); \
	    msg->iov[msg->iov_len].data = a + off; \
	    msg->iov[msg->iov_len].len  = sz; \
	    msg->release[msg->iov_len].array = b; \
	    msg->release[msg->iov_len].buf   = a; \
	    msg->release[msg->iov_len].func  = (release_func_t)(*env)->Release ## JType ## ArrayElements; \
	} \
    } \
    IBP_VPRINTF(300, env, ("Now push msg %p %s source %p data %p size %d iov %d total %d [%d,%d,%d,%d,...]\n", \
		msg, #JType, b, msg->iov[msg->iov_len].data, \
		msg->iov[msg->iov_len].len, msg->iov_len, ibmp_iovec_len(msg->iov, msg->iov_len + 1), msg->iov[0].len, msg->iov[1].len, msg->iov[2].len, msg->iov[3].len)); \
    dump_ ## jtype((jtype *)(msg->iov[msg->iov_len].data), len); \
    msg->iov_len++; \
}

ARRAY_WRITE(Boolean, jboolean)
ARRAY_WRITE(Byte, jbyte)
ARRAY_WRITE(Char, jchar)
ARRAY_WRITE(Short, jshort)
ARRAY_WRITE(Int, jint)
ARRAY_WRITE(Long, jlong)
ARRAY_WRITE(Float, jfloat)
ARRAY_WRITE(Double, jdouble)


void
ibmp_byte_output_stream_report(JNIEnv *env)
{
#ifdef IBP_VERBOSE
    fprintf(stderr, "%2d: PandaBufferedOutputStream.sent data %d\n", ibmp_me, sent_data);
#endif
#if DISABLE_SENDER_INTERRUPTS
    fprintf(stderr, "%2d: IBP intr enable %d disable %d send msg %d frag %d (first %d last %d skip %d) \n",
	    ibmp_me, intr_enable, intr_disable, send_msg, send_frag, send_first_frag, send_last_frag, send_frag_skip);
#else
    fprintf(stderr, "%2d: IBP send msg %d frag %d (skip %d) \n",
	    ibmp_me, send_msg, send_frag, send_frag_skip);
#endif
}


void
ibmp_byte_output_stream_init(JNIEnv *env)
{
    cls_PandaByteOutputStream = (*env)->FindClass(env,
			 "ibis/ipl/impl/messagePassing/ByteOutputStream");
    if (cls_PandaByteOutputStream == NULL) {
	ibmp_error(env, "Cannot find class ibis/ipl/impl/messagePassing/ByteOutputStream\n");
    }
    cls_PandaByteOutputStream = (jclass)(*env)->NewGlobalRef(env, (jobject)cls_PandaByteOutputStream);

    fld_msgHandle        = (*env)->GetFieldID(env,
					 cls_PandaByteOutputStream,
					 "msgHandle", "I");
    if (fld_msgHandle == NULL) {
	ibmp_error(env, "Cannot find static field msgHandle:I\n");
    }

    fld_waitingInPoll    = (*env)->GetFieldID(env,
					 cls_PandaByteOutputStream,
					 "waitingInPoll", "Z");
    if (fld_waitingInPoll == NULL) {
	ibmp_error(env, "Cannot find static field waitingInPoll:Z\n");
    }

    fld_outstandingFrags = (*env)->GetFieldID(env,
					 cls_PandaByteOutputStream,
					 "outstandingFrags", "I");
    if (fld_outstandingFrags == NULL) {
	ibmp_error(env, "Cannot find static field outstandingFrags:I\n");
    }

    fld_makeCopy = (*env)->GetFieldID(env,
					 cls_PandaByteOutputStream,
					 "makeCopy", "Z");
    if (fld_makeCopy == NULL) {
	ibmp_error(env, "Cannot find static field makeCopy:Z\n");
    }

    md_finished_upcall    = (*env)->GetMethodID(env,
						cls_PandaByteOutputStream,
						"finished_upcall", "()V");
    if (md_finished_upcall == NULL) {
	ibmp_error(env, "Cannot find method finished_upcall()V\n");
    }

    // ibmp_lock_check_owned(env);
    ibmp_byte_stream_port = ibp_mp_port_register(ibmp_byte_stream_handle);
    ibmp_byte_stream_proto_start = align_to(ibp_mp_proto_offset(), ibmp_byte_stream_hdr_t);
    ibmp_byte_stream_proto_size  = ibmp_byte_stream_proto_start + sizeof(ibmp_byte_stream_hdr_t);

    ibmp_poll_register(ibmp_msg_q_poll);

    if (pan_arg_int(NULL, NULL, "-ibp-send-sync", &ibmp_send_sync) == -1) {
	ibmp_error(env, "-ibp-send-sync requires an int argument\n");
    }
}


void
ibmp_byte_output_stream_end(JNIEnv *env)
{
    ibmp_byte_output_stream_report(env);
}

/* Native methods for
 * ibis.ipl.impl.messagePassing.ByteInputStream.java
 */

#include <assert.h>

#include <jni.h>

#include <pan_sys.h>

#include "ibmp.h"

#include "ibis_ipl_impl_messagePassing_ByteInputStream.h"

#include "ibp.h"
#include "ibmp_byte_input_stream.h"
#include "ibmp_byte_output_stream.h"


static jclass	cls_PandaByteInputStream;
static jfieldID	fld_msgHandle;


jint
Java_ibis_ipl_impl_messagePassing_ByteInputStream_lockedRead(
	JNIEnv *env,
	jobject this)
{
    int		r;
    int		rd;
    ibp_msg_p	msg = (ibp_msg_p)(*env)->GetIntField(env, this, fld_msgHandle);

    assert(msg != NULL);

    IBP_VPRINTF(250, env, ("Consume 1 int from msg %p, currently holds %d\n",
		msg, ibp_msg_consume_left(msg)));
    rd = 0;
    while (rd < sizeof(int)) {
	rd += ibp_consume(env, msg, (char *)&r + rd, sizeof(int) - rd);
    }

    IBP_VPRINTF(250, env, ("Consumed 1 int from msg %p, value = %d\n", msg, r));

    return (jint)r;
}


jlong
Java_ibis_ipl_impl_messagePassing_ByteInputStream_skip(
	JNIEnv *env,
	jobject this,
	jlong len)
{
    ibp_msg_p	msg = (ibp_msg_p)(*env)->GetIntField(env, this, fld_msgHandle);

    return (jlong)ibp_consume(env, msg, NULL, (int)len);
}


jint
Java_ibis_ipl_impl_messagePassing_ByteInputStream_available(
	JNIEnv *env,
	jobject this)
{
    ibp_msg_p	msg = (ibp_msg_p)(*env)->GetIntField(env, this, fld_msgHandle);

    return (jint)ibp_msg_consume_left(msg);
}


void
Java_ibis_ipl_impl_messagePassing_ByteInputStream_resetMsg(
	JNIEnv *env,
	jobject this,
	jint msgHandle)
{
    /* This CANNOT be field_msgHandle because we are not sure the fragment
     * that clears us is actually the current fragment */
    ibp_msg_p	msg = (ibp_msg_p)msgHandle;

    IBP_VPRINTF(250, env, ("clear MP msg %p\n", msg));
    ibp_msg_clear(env, msg);
}


typedef struct IBMP_MSG ibmp_msg_t, *ibmp_msg_p;

struct IBMP_MSG {
    ibp_msg_p	msg;
    void       *proto;
    ibmp_msg_p	next;
};

typedef struct PANDA_MSG_Q {
    ibmp_msg_p	front;
    ibmp_msg_p	tail;
} ibmp_msg_q_t, *ibmp_msg_q_p;


static ibmp_msg_q_t	ibmp_msg_q;
static ibmp_msg_p	ibmp_msg_freelist;


static void
ibmp_msg_q_enq(ibp_msg_p msg, void *proto)
{
    ibmp_msg_p	m;

    m = ibmp_msg_freelist;
    if (m == NULL) {
	m = pan_malloc(sizeof(*m));
    } else {
	ibmp_msg_freelist = m->next;
    }
    m->msg = msg;
    m->proto = proto;

    m->next = NULL;
    if (ibmp_msg_q.front == NULL) {
	ibmp_msg_q.front = m;
    } else {
	ibmp_msg_q.tail->next = m;
    }
    ibmp_msg_q.tail = m;
}


static ibp_msg_p
ibmp_msg_q_deq(void **proto)
{
    ibmp_msg_p	m;
    ibp_msg_p	msg;

    m = ibmp_msg_q.front;
    if (m == NULL) {
	return NULL;
    }

    msg = m->msg;
    *proto = m->proto;

    ibmp_msg_q.front = m->next;
    m->next = ibmp_msg_freelist;
    ibmp_msg_freelist = m;

    return msg;
}

#include <inttypes.h>

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
#define DUMP_DATA(jtype, fmt) \
static void dump_ ## jtype(jtype *b, int len) \
{ \
}
#endif


DUMP_DATA(jboolean, "d ", int32_t)
DUMP_DATA(jbyte, "c", int8_t)
DUMP_DATA(jchar, "d ", int16_t)
DUMP_DATA(jshort, "d ", int16_t)
DUMP_DATA(jint, "d ", int32_t)
DUMP_DATA(jlong, "lld ", int64_t)
DUMP_DATA(jfloat, "f ", float)
DUMP_DATA(jdouble, "f ", double)


#define ARRAY_READ(JType, jtype) \
jint Java_ibis_ipl_impl_messagePassing_ByteInputStream_read ## JType ## Array( \
		JNIEnv *env, \
		jobject this, \
		jtype ## Array a, \
		jint off, \
		jint len) \
{ \
    jtype      *buf = (*env)->Get ## JType ## ArrayElements(env, a, NULL); \
    jint	m = (*env)->GetIntField(env, this, fld_msgHandle); \
    ibp_msg_p	msg = (ibp_msg_p)m; \
    int		rd; \
    \
    assert(msg != NULL); \
    \
    rd = ibp_consume(env, msg, buf + off, (int)len * sizeof(jtype)); \
    IBP_VPRINTF(250, env, ("Consumed %d (requested %d) %s from msg %p into buf %p, currently holds %d\n", \
		rd / sizeof(jtype), (int)len, #JType, msg, buf, \
		ibp_msg_consume_left(msg))); \
if (0 && sizeof(jtype) == sizeof(short)) { \
int i; \
int n = rd < len ? rd : len; \
fprintf(stderr, "Data = ["); \
for (i = off; i < n; i++) { \
fprintf(stderr, "%d ", (short)buf[i]); \
} \
fprintf(stderr, "]\n"); \
} \
    dump_ ## jtype(buf + off, rd < len ? rd : len); \
    \
    (*env)->Release ## JType ## ArrayElements(env, a, buf, 0); \
    \
    return (jint)(rd / sizeof(jtype)); \
}

ARRAY_READ(Boolean, jboolean)
ARRAY_READ(Byte, jbyte)
ARRAY_READ(Char, jchar)
ARRAY_READ(Short, jshort)
ARRAY_READ(Int, jint)
ARRAY_READ(Long, jlong)
ARRAY_READ(Float, jfloat)
ARRAY_READ(Double, jdouble)


jboolean Java_ibis_ipl_impl_messagePassing_ByteInputStream_getInputStreamMsg(
		JNIEnv *env,
		jclass this,
		jarray jtags)
{
    jint       *tags;
    ibp_msg_p   msg;
    void       *proto;
    ibmp_byte_stream_hdr_p hdr;

    msg = ibmp_msg_q_deq(&proto);

    if (msg == NULL) {
	return JNI_FALSE;
    }

    hdr = ibmp_byte_stream_hdr(proto);
    tags = (*env)->GetIntArrayElements(env, jtags, NULL);
    tags[0] = ibp_msg_sender(msg);
    tags[1] = hdr->src_port;
    tags[2] = hdr->dest_port;
    tags[3] = (jint)msg;
    tags[4] = (jint)ibp_msg_consume_left(msg);
    tags[5] = hdr->msgSeqno;
    (*env)->ReleaseIntArrayElements(env, jtags, tags, 0);

    return JNI_TRUE;
}


int
ibmp_byte_stream_handle(JNIEnv *env, ibp_msg_p msg, void *proto)
{
    ibmp_lock_check_owned(env);

    ibmp_msg_q_enq(msg, proto);

    return 1;
}


void
ibmp_byte_input_stream_init(JNIEnv *env)
{
    cls_PandaByteInputStream = (*env)->FindClass(env,
			    "ibis/ipl/impl/messagePassing/ByteInputStream");
    if (cls_PandaByteInputStream == NULL) {
	ibmp_error(env, "Cannot find class ibis/ipl/impl/messagePassing/ByteInputStream\n");
    }
    cls_PandaByteInputStream = (jobject)(*env)->NewGlobalRef(env, (jobject)cls_PandaByteInputStream);

    fld_msgHandle = (*env)->GetFieldID(env, cls_PandaByteInputStream, "msgHandle", "I");
    if (fld_msgHandle == NULL) {
	ibmp_error(env, "Cannot find field PandaByteInputStream.msgHandle:I");
    }

}


void
ibmp_byte_input_stream_end(JNIEnv *env)
{
}

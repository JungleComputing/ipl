#ifndef __IBIS_MANTA_IBIS_IMPL_MESSAGEPASSING_IBP_BYTE_STREAM_H__
#define __IBIS_MANTA_IBIS_IMPL_MESSAGEPASSING_IBP_BYTE_STREAM_H__

/* Native methods for ibis.impl.messagePassing.ByteOutputStream
 */

#include <stdio.h>
#include <jni.h>

extern jint	ibmp_byte_stream_NO_BCAST_GROUP;

extern int	ibmp_byte_stream_proto_start;

typedef struct IBP_BYTE_STREAM_HDR ibmp_byte_stream_hdr_t, *ibmp_byte_stream_hdr_p;

struct IBP_BYTE_STREAM_HDR {
    jint	dest_port;
    jint	src_port;
    jint	msgSeqno;
    jint	group;
    void       *home_msg;
};

#define ibmp_byte_stream_hdr(proto) \
    (ibmp_byte_stream_hdr_p)((char *)proto + ibmp_byte_stream_proto_start)

void ibmp_bcast_home_ack(ibmp_byte_stream_hdr_p hdr);

void ibmp_byte_output_stream_report(JNIEnv *env, FILE *f);

void ibmp_byte_output_stream_init(JNIEnv *env);
void ibmp_byte_output_stream_end(JNIEnv *env);

extern unsigned	IBMP_FIRST_FRAG_BIT;
extern unsigned	IBMP_LAST_FRAG_BIT;
extern unsigned	IBMP_SEQNO_FRAG_BITS;

#endif

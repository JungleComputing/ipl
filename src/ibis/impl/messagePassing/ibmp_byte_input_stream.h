#ifndef __IBIS_MANTA_IBIS_IMPL_MESSAGEPASSING_IBP_BYTE_INPUT_STREAM_H__
#define __IBIS_MANTA_IBIS_IMPL_MESSAGEPASSING_IBP_BYTE_INPUT_STREAM_H__

#include <jni.h>
#include <pan_sys.h>

int ibmp_byte_stream_handle(JNIEnv *env, ibp_msg_p msg, void *proto);

void ibmp_byte_input_stream_init(JNIEnv *env);
void ibmp_byte_input_stream_end(JNIEnv *env);

#endif

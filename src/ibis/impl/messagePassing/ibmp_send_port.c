#include <jni.h>

#include "ibmp_send_port.h"


static jclass		cls_ShadowSendPort;
static jmethodID	ctor_ShadowSendPort;
static jmethodID	md_disconnect;
static jfieldID		fld_connect_allowed;


jboolean
ibmp_send_port_new(JNIEnv *env, jstring type, jstring name, jstring ibisId,
		   jint cpu, jint port,
		   jint receiver_port)
{
    jobject s = (*env)->NewObject(env,
			     cls_ShadowSendPort,
			     ctor_ShadowSendPort,
			     type,
			     name,
			     ibisId,
			     cpu,
			     port,
			     receiver_port);
    return (*env)->GetBooleanField(env, s, fld_connect_allowed);
}


void
ibmp_send_port_disconnect(JNIEnv *env, jint sender, jint send_port,
			  jint rcve_port, jint msgCount)
{
    (*env)->CallStaticVoidMethod(env, cls_ShadowSendPort, md_disconnect, sender,
				 send_port, rcve_port, msgCount);
}


void
ibmp_send_port_init(JNIEnv *env)
{
    cls_ShadowSendPort = (*env)->FindClass(env,
				   "manta/ibis/impl/messagePassing/ShadowSendPort");
    if (cls_ShadowSendPort == NULL) {
	fprintf(stderr, "%s.%d Cannot find class manta/ibis/impl/messagePassing/ShadowSendPort\n", __FILE__, __LINE__);
    }
    cls_ShadowSendPort = (jclass)(*env)->NewGlobalRef(env, (jobject)cls_ShadowSendPort);

    ctor_ShadowSendPort = (*env)->GetMethodID(env, cls_ShadowSendPort, "<init>",
				   "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;III)V");
    if (ctor_ShadowSendPort == NULL) {
	fprintf(stderr, "%s.%d Cannot find constructor ShadowSendPort(Ljava/lang/String;Ljava/lang/String;III)V\n", __FILE__, __LINE__);
    }
    md_disconnect = (*env)->GetStaticMethodID(env, cls_ShadowSendPort,
					      "disconnect", "(IIII)V");
    if (md_disconnect == NULL) {
	fprintf(stderr, "%s.%d Cannot find static method disconnect(IIII)V\n", __FILE__, __LINE__);
    }

    fld_connect_allowed = (*env)->GetFieldID(env, cls_ShadowSendPort,
					     "connect_allowed", "Z");
}


void
ibmp_send_port_end(JNIEnv *env)
{
}

#include <stdlib.h>

#include <jni.h>

#include "ibmp.h"
#include "ibmp_send_port.h"


static jclass		cls_ShadowSendPort;
static jmethodID	md_createSSP;
static jmethodID	md_disconnect;
static jfieldID		fld_connect_allowed;



jboolean
ibmp_send_port_new(JNIEnv *env, jstring type, jstring name, jstring ibisId,
		   jint cpu, jint port, jint receiver_port, jint serializationType)
{
    jobject s;

    IBP_VPRINTF(900, env, ("Now call this method createShadowSendPort...\n"));
    s = (*env)->CallStaticObjectMethod(env,
				       cls_ShadowSendPort,
				       md_createSSP,
				       type,
				       name,
				       ibisId,
				       cpu,
				       port,
				       receiver_port,
				       serializationType);
    if ((*env)->ExceptionOccurred(env) != NULL) {
	(*env)->ExceptionDescribe(env);
    }

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
				   "ibis/ipl/impl/messagePassing/ShadowSendPort");
    if (cls_ShadowSendPort == NULL) {
	fprintf(stderr, "%s.%d Cannot find class ibis/ipl/impl/messagePassing/ShadowSendPort\n", __FILE__, __LINE__);
	abort();
    }
    cls_ShadowSendPort = (jclass)(*env)->NewGlobalRef(env, (jobject)cls_ShadowSendPort);

    md_createSSP = (*env)->GetStaticMethodID(env, cls_ShadowSendPort, "createShadowSendPort",
					     "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IIII)Libis/ipl/impl/messagePassing/ShadowSendPort;");
    if (md_createSSP == NULL) {
	if ((*env)->ExceptionOccurred(env)) {
	    (*env)->ExceptionDescribe(env);
	}
	fprintf(stderr, "%s.%d Cannot find method createShadowSendPort(Ljava/lang/String;Ljava/lang/String;IIII)Libis.ipl.impl.messagePassing.ShadowSendPort;\n", __FILE__, __LINE__);
	abort();
    }
    md_disconnect = (*env)->GetStaticMethodID(env, cls_ShadowSendPort,
					      "disconnect", "(IIII)V");
    if (md_disconnect == NULL) {
	fprintf(stderr, "%s.%d Cannot find static method disconnect(IIII)V\n", __FILE__, __LINE__);
	abort();
    }

    fld_connect_allowed = (*env)->GetFieldID(env, cls_ShadowSendPort,
					     "connect_allowed", "Z");
}


void
ibmp_send_port_end(JNIEnv *env)
{
}

/*
 * Code shared by natives for package ibis.ipl.impl.messagePassing
 */

#include <stdlib.h>
#include <string.h>

#include <jni.h>

#include <pan_util.h>

#include "ibp.h"
#include "ibp_mp.h"

#include "ibis_ipl_impl_messagePassing_Ibis.h"

#include "ibmp.h"
#include "ibmp_receive_port_ns.h"
#include "ibmp_receive_port_ns_bind.h"
#include "ibmp_receive_port_ns_lookup.h"
#include "ibmp_receive_port_ns_unbind.h"
#include "ibmp_connect.h"
#include "ibmp_disconnect.h"
#include "ibmp_join.h"
#include "ibmp_poll.h"
#include "ibmp_send_port.h"
#include "ibmp_byte_input_stream.h"
#include "ibmp_byte_output_stream.h"


static jclass		cls_Thread;
static jmethodID	md_yield;
static jmethodID	md_currentThread;
static jmethodID	md_dumpStack;

static jclass		cls_Object;
static jmethodID	md_toString;

static jmethodID	md_checkLockOwned;
static jmethodID	md_checkLockNotOwned;

static jmethodID	md_lock;
static jmethodID	md_unlock;

static jmethodID	md_poll;

jclass		ibmp_cls_Ibis;
jobject		ibmp_obj_Ibis_ibis;

static jclass		cls_IbisIOException;

int		ibmp_me;
int		ibmp_nr;

static int		ibmp_core_on_error;


#ifdef IBP_VERBOSE

int		ibmp_verbose;

int
ibmp_stderr_printf(char *fmt, ...)
{
    va_list	ap;
    int		ret;

    va_start(ap, fmt);
    ret = vfprintf(stderr, fmt, ap);
    va_end(ap);
    fflush(stderr);

    return ret;
}

#endif


void
ibmp_error_printf(JNIEnv *env, const char *fmt, ...)
{
    va_list	ap;
    char	msg[1024];

    va_start(ap, fmt);
    fprintf(stderr, "%2d: Fatal Ibis/MessagePassing error: ", ibmp_me);
    vfprintf(stderr, fmt, ap);
    va_end(ap);
    fprintf(stderr, "\n");

    va_start(ap, fmt);
    sprintf(msg, "%2d: Fatal Ibis/MessagePassing error: ", ibmp_me);
    vsprintf(strchr(msg, '\0'), fmt, ap);
    va_end(ap);

    if (ibmp_core_on_error) {
	abort();
    } else {
	(*env)->ThrowNew(env, cls_IbisIOException, msg);
	// exit(33);
    }
}


char *
ibmp_currentThread(JNIEnv *env)
{
    jobject	cT;
    jstring	t;
    jbyte      *b;
    static char *c = NULL;
    static int	n_c = 0;

    cT = (*env)->CallStaticObjectMethod(env, cls_Thread, md_currentThread);
    if (cT == NULL) {
	return "--system idle thread--";
    }

    t = (*env)->CallObjectMethod(env, cT, md_toString);

    b = (jbyte *)(*env)->GetStringUTFChars(env, t, NULL);
    if (b == NULL) {
	ibmp_error(env, "Cannot get string bytes\n");
    }
    if (strlen(b) + 1 > n_c) {
	n_c = 2 * strlen(b) + 1;
	c = pan_realloc(c, n_c);
    }
    sprintf(c, "%s", b);

    (*env)->ReleaseStringUTFChars(env, t, b);

    return c;
}


char *
ibmp_jstring2c(JNIEnv *env, jstring name)
{
    jbyte *b;
    char *c;

    if (name == NULL) {
	return "<null>";
    }

    b = (jbyte *)(*env)->GetStringUTFChars(env, name, NULL);
    c = strdup((char *)b);

    (*env)->ReleaseStringUTFChars(env, name, b);
    return c;
}


void
ibmp_object_toString(JNIEnv *env, jobject obj)
{
    jstring name;
    jbyte *b;
IBP_VPRINTF(100, env, ("\n"));

    name = (*env)->CallObjectMethod(env, obj, md_toString);
IBP_VPRINTF(100, env, ("\n"));
    if (name == NULL) {
	ibmp_error(env, "Cannot call toString()\n");
    }

    b = (jbyte *)(*env)->GetStringUTFChars(env, name, NULL);
IBP_VPRINTF(100, env, ("\n"));
    if (b == NULL) {
	ibmp_error(env, "Cannot get string bytes\n");
    }

    printf("object = %s", b);
    fflush(stdout);

    (*env)->ReleaseStringUTFChars(env, name, b);
}



void
ibmp_thread_yield(JNIEnv *env)
{
    (*env)->CallStaticVoidMethod(env, cls_Thread, md_yield);
}


void
ibmp_lock(JNIEnv *env)
{
    (*env)->CallVoidMethod(env, ibmp_obj_Ibis_ibis, md_lock);
}


void
ibmp_unlock(JNIEnv *env)
{
    (*env)->CallVoidMethod(env, ibmp_obj_Ibis_ibis, md_unlock);
}


int
ibmp_poll(JNIEnv *env)
{
    return (int)(*env)->CallBooleanMethod(env, ibmp_obj_Ibis_ibis, md_poll);
}


#undef ibmp_lock_check_owned
#undef ibmp_lock_check_not_owned

void
ibmp_lock_check_owned(JNIEnv *env)
{
    (*env)->CallVoidMethod(env, ibmp_obj_Ibis_ibis, md_checkLockOwned);
}


void
ibmp_lock_check_not_owned(JNIEnv *env)
{
    (*env)->CallVoidMethod(env, ibmp_obj_Ibis_ibis, md_checkLockNotOwned);
}


static char **
java2c_args(JNIEnv *env, jarray java_args, int *argc)
{
    char      **argv;
    int		i;
    const jbyte *b;

    if (java_args == NULL) {
	*argc = 0;
    } else {
	*argc = (*env)->GetArrayLength(env, java_args);
    }
    argv = malloc((*argc + 1) * sizeof(*argv));
    for (i = 0; i < *argc; i++) {
	jstring	s = (jstring)(*env)->GetObjectArrayElement(env, java_args, i);
	int	len = (*env)->GetStringUTFLength(env, s);

	b = (*env)->GetStringUTFChars(env, s, NULL);
	argv[i] = malloc(len + 1);
	memcpy(argv[i], b, len);
	argv[i][len] = '\0';
	(*env)->ReleaseStringUTFChars(env, s, b);
    }
    argv[*argc] = NULL;

    return argv;
}


static jarray
c2java_args(JNIEnv *env, int argc, char **argv)
{
    jclass	cls_String = (*env)->FindClass(env, "java/lang/String");
    jarray	a = (*env)->NewObjectArray(env, argc, cls_String, NULL);
    int		i;

    for (i = 0; i < argc; i++) {
	jstring s = (*env)->NewStringUTF(env, argv[i]);
	(*env)->SetObjectArrayElement(env, a, i, s);
    }

    return a;
}


void
ibmp_check_ibis_name(JNIEnv *env, const char *name)
{
    jclass classClass;
    jclass c;
    jmethodID getName;
    jstring s;
    const jbyte *class_name;

    IBP_VPRINTF(2000, env, ("here...\n"));
    c = (*env)->GetObjectClass(env, ibmp_obj_Ibis_ibis);
    IBP_VPRINTF(2000, env, ("here...\n"));
    classClass = (*env)->GetObjectClass(env, (jobject)c);
    IBP_VPRINTF(2000, env, ("here...\n"));
    getName = (*env)->GetMethodID(env, (jobject)classClass, "getName", "()Ljava/lang/String;");
    if (getName == NULL) {
	IBP_VPRINTF(2000, env, ("here...\n"));
	ibmp_error(env, "Cannot find method java.lang.Class.getName()Ljava/lang/String;");
    }
    IBP_VPRINTF(2000, env, ("here...\n"));
    s = (jstring)(*env)->CallObjectMethod(env, (jobject)c, getName);
    IBP_VPRINTF(2000, env, ("here...\n"));
    class_name = (*env)->GetStringUTFChars(env, s, NULL);
    IBP_VPRINTF(2000, env, ("here...\n"));
    if (strcmp(class_name, name) != 0) {
	IBP_VPRINTF(2000, env, ("here...\n"));
	ibmp_error(env, "Linked %s native lib with %s Ibis", name, class_name);
    }
    IBP_VPRINTF(2000, env, ("here...\n"));
    (*env)->ReleaseStringUTFChars(env, s, class_name);
}


void
Java_ibis_ipl_impl_messagePassing_Ibis_ibmp_1report(JNIEnv *env, jobject this, jint out)
{
    ibmp_byte_output_stream_report(env);
    ibp_report(env, out);
}


jarray
Java_ibis_ipl_impl_messagePassing_Ibis_ibmp_1init(JNIEnv *env, jobject this, jarray java_args)
{
    jfieldID	fld_Ibis_ibis;
    jfieldID	fld_Ibis_nrCpus;
    jfieldID	fld_Ibis_myCpu;
    int		argc;
    char      **argv;

#ifdef IBP_VERBOSE
    if (pan_arg_int(NULL, NULL, "-ibp-v", &ibmp_verbose) == -1) {
	ibmp_error(env, "-ibp-v requires an integer argument\n");
    }
    fprintf(stderr, "ibmp_verbose = %d\n", ibmp_verbose);
#endif

    if (pan_arg_bool(NULL, NULL, "-ibp-core") == 1) {
	ibmp_core_on_error = 1;
    }
    
    cls_Thread = (*env)->FindClass(env, "java/lang/Thread");
    if (cls_Thread == NULL) {
	ibmp_error(env, "Cannot find class java/lang/Thread\n");
    }
    cls_Thread = (jclass)(*env)->NewGlobalRef(env, (jobject)cls_Thread);

    md_yield   = (*env)->GetStaticMethodID(env, cls_Thread, "yield", "()V");
    if (md_yield == NULL) {
	ibmp_error(env, "Cannot find static method yield()V\n");
    }

    md_currentThread   = (*env)->GetStaticMethodID(env, cls_Thread, "currentThread", "()Ljava/lang/Thread;");
    if (md_currentThread == NULL) {
	ibmp_error(env, "Cannot find static method currentThread()Ljava/lang/Thread;\n");
    }

    cls_Object = (*env)->FindClass(env, "java/lang/Object");
    if (cls_Object == NULL) {
	ibmp_error(env, "Cannot find class java/lang/Object\n");
    }
    cls_Object = (jclass)(*env)->NewGlobalRef(env, (jobject)cls_Object);

    md_toString = (*env)->GetMethodID(env, cls_Object, "toString", "()Ljava/lang/String;");
    if (md_toString == NULL) {
	ibmp_error(env, "Cannot find method toString\n");
    }

    md_dumpStack   = (*env)->GetStaticMethodID(env, cls_Thread, "dumpStack", "()V");
    // md_dumpStack   = (*env)->GetStaticMethodID(env, ibmp_cls_Ibis, "dumpStack", "()V");
    if (md_dumpStack == NULL) {
	ibmp_error(env, "Cannot find static method dumpStack()V\n");
    }

    ibmp_cls_Ibis = (*env)->FindClass(env, "ibis/ipl/impl/messagePassing/Ibis");
    if (ibmp_cls_Ibis == NULL) {
	ibmp_error(env, "Cannot find class ibis/ipl/impl/messagePassing/Ibis\n");
    }
    ibmp_cls_Ibis = (jclass)(*env)->NewGlobalRef(env, (jobject)ibmp_cls_Ibis);

    fld_Ibis_ibis = (*env)->GetStaticFieldID(env, ibmp_cls_Ibis, "myIbis", "Libis/ipl/impl/messagePassing/Ibis;");
    if (fld_Ibis_ibis == NULL) {
	ibmp_error(env, "Cannot find static field myIbis:Libis/ipl/impl/messagePassing/Ibis;\n");
    }

    cls_IbisIOException = (*env)->FindClass(env, "ibis/ipl/IbisIOException");
    if (cls_IbisIOException == NULL) {
	ibmp_error(env, "Cannot find class ibis/ipl/IbisIOException\n");
    }
    cls_IbisIOException = (jclass)(*env)->NewGlobalRef(env, (jobject)cls_IbisIOException);

    ibmp_obj_Ibis_ibis = (*env)->GetStaticObjectField(env, ibmp_cls_Ibis, fld_Ibis_ibis);
    ibmp_obj_Ibis_ibis = (*env)->NewGlobalRef(env, ibmp_obj_Ibis_ibis);

    md_checkLockOwned = (*env)->GetMethodID(env, ibmp_cls_Ibis, "checkLockOwned",
				       "()V");
    if (md_checkLockOwned == NULL) {
	ibmp_error(env, "Cannot find method checkLockOwned\n");
    }
    IBP_VPRINTF(2000, env, ("here..\n"));

    md_checkLockNotOwned = (*env)->GetMethodID(env, ibmp_cls_Ibis, "checkLockNotOwned",
				       "()V");
    if (md_checkLockNotOwned == NULL) {
	ibmp_error(env, "Cannot find method checkLockNotOwned\n");
    }
    IBP_VPRINTF(2000, env, ("here..\n"));

    if (0) {
	argv = java2c_args(env, java_args, &argc);
	ibp_init(env, &argc, argv);
	java_args = c2java_args(env, argc, argv);
    } else {
	int argc = 1;
	char *argv[2];

	argv[0] = "ibis-executable";
	argv[1] = NULL;
	ibp_init(env, &argc, argv);
    }

    md_lock = (*env)->GetMethodID(env, ibmp_cls_Ibis, "lock", "()V");
    if (md_lock == NULL) {
	ibmp_error(env, "Cannot find method lock\n");
    }
    IBP_VPRINTF(2000, env, ("here..\n"));

    md_unlock = (*env)->GetMethodID(env, ibmp_cls_Ibis, "unlock", "()V");
    if (md_unlock == NULL) {
	ibmp_error(env, "Cannot find method unlock\n");
    }
    IBP_VPRINTF(2000, env, ("here..\n"));

    md_poll = (*env)->GetMethodID(env, ibmp_cls_Ibis, "pollLocked", "()Z");
    if (md_poll == NULL) {
	ibmp_error(env, "Cannot find method pollLocked\n");
    }
    IBP_VPRINTF(2000, env, ("here..\n"));

    fld_Ibis_nrCpus = (*env)->GetFieldID(env, ibmp_cls_Ibis, "nrCpus", "I");
    if (fld_Ibis_nrCpus == NULL) {
	ibmp_error(env, "Cannot find field nrCpus:I\n");
	return java_args;
    }
    IBP_VPRINTF(2000, env, ("here...\n"));
    fld_Ibis_myCpu  = (*env)->GetFieldID(env, ibmp_cls_Ibis, "myCpu", "I");
    if (fld_Ibis_myCpu == NULL) {
	ibmp_error(env, "Cannot find field myCpu:I\n");
    }
    IBP_VPRINTF(2000, env, ("here...\n"));

    (*env)->SetIntField(env, ibmp_obj_Ibis_ibis, fld_Ibis_nrCpus,
		(jint)ibmp_nr);
    IBP_VPRINTF(2000, env, ("here...\n"));
    (*env)->SetIntField(env, ibmp_obj_Ibis_ibis, fld_Ibis_myCpu,
		(jint)ibmp_me);
    IBP_VPRINTF(2000, env, ("here...\n"));

    ibp_mp_init(env);

    ibmp_poll_init(env);
    IBP_VPRINTF(2000, env, ("here..\n"));
    ibmp_receive_port_ns_init(env);
    IBP_VPRINTF(2000, env, ("here..\n"));

    ibmp_receive_port_ns_bind_init(env);
    IBP_VPRINTF(2000, env, ("here...\n"));
    ibmp_receive_port_ns_lookup_init(env);
    IBP_VPRINTF(2000, env, ("here...\n"));
    ibmp_receive_port_ns_unbind_init(env);
    IBP_VPRINTF(2000, env, ("here...\n"));

    ibmp_connect_init(env);
    IBP_VPRINTF(2000, env, ("here...\n"));
    ibmp_disconnect_init(env);
    IBP_VPRINTF(2000, env, ("here...\n"));

    ibmp_join_init(env);
    IBP_VPRINTF(2000, env, ("here...\n"));

    ibmp_send_port_init(env);
    IBP_VPRINTF(2000, env, ("here..\n"));

    ibmp_byte_output_stream_init(env);
    IBP_VPRINTF(2000, env, ("here...\n"));
    ibmp_byte_input_stream_init(env);
    IBP_VPRINTF(2000, env, ("here...\n"));

    return java_args;
}


void
Java_ibis_ipl_impl_messagePassing_Ibis_ibmp_1start(JNIEnv *env, jobject this)
{
    ibp_start(env);
}


void
Java_ibis_ipl_impl_messagePassing_Ibis_ibmp_1end(JNIEnv *env, jobject this)
{
fprintf(stderr, "%2d: ibmp_1end: hi folks... \n", ibmp_me);
    ibmp_byte_input_stream_end(env);
    ibmp_byte_output_stream_end(env);
    ibmp_send_port_end(env);
    ibmp_join_end(env);
    ibmp_disconnect_end(env);
    ibmp_connect_end(env);
    ibmp_receive_port_ns_bind_end(env);
    ibmp_receive_port_ns_lookup_end(env);
    ibmp_receive_port_ns_unbind_end(env);
    ibmp_receive_port_ns_end(env);
    ibmp_poll_end(env);
    IBP_VPRINTF(2000, env, ("here...\n"));

    ibp_mp_end(env);
    IBP_VPRINTF(2000, env, ("here...\n"));
    ibp_end(env);
    IBP_VPRINTF(2000, env, ("here...\n"));
}

/*
 * Code shared by natives for package ibis.ipl.impl.messagePassing
 */

#include <stdlib.h>
#include <string.h>

#include <jni.h>

#include "ibis_ipl_impl_messagePassing_Ibis.h"

#include "ibmp.h"
#include "ibmp_receive_port_ns.h"
#include "ibmp_receive_port_identifier.h"
#include "ibmp_send_port.h"


JNIEnv  *ibmp_JNIEnv = NULL;


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

jclass		ibmp_cls_Ibis;
jobject		ibmp_obj_Ibis_ibis;


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

    return ret;
}

#endif


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
	fprintf(stderr, "Cannot get string bytes\n");
    }
    if (strlen(b) + 1 > n_c) {
	n_c = 2 * strlen(b) + 1;
	c = realloc(c, n_c);
    }
    sprintf(c, "%s", b);

    (*env)->ReleaseStringUTFChars(env, t, b);

    return c;
}


void
ibmp_dumpStack(JNIEnv *env)
{
    (*env)->CallStaticVoidMethod(env, cls_Thread, md_dumpStack);
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
	fprintf(stderr, "Cannot call toString()\n");
    }

    b = (jbyte *)(*env)->GetStringUTFChars(env, name, NULL);
IBP_VPRINTF(100, env, ("\n"));
    if (b == NULL) {
	fprintf(stderr, "Cannot get string bytes\n");
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


void
ibmp_init(JNIEnv *env, jobject this)
{
    jfieldID	fld_Ibis_ibis;

#ifdef IBP_VERBOSE
    if (pan_arg_int(NULL, NULL, "-ibp-v", &ibmp_verbose) == -1) {
	fprintf(stderr, "-ibp-v requires an integer argument\n");
    }
    fprintf(stderr, "ibmp_verbose = %d\n", ibmp_verbose);
#endif
    
    cls_Thread = (*env)->FindClass(env, "java/lang/Thread");
    if (cls_Thread == NULL) {
	fprintf(stderr, "%s.%d Cannot find class java/lang/Thread\n", __FILE__, __LINE__);
	abort();
    }
    cls_Thread = (jclass)(*env)->NewGlobalRef(env, (jobject)cls_Thread);

    md_yield   = (*env)->GetStaticMethodID(env, cls_Thread, "yield", "()V");
    if (md_yield == NULL) {
	fprintf(stderr, "%s.%d Cannot find static method yield()V\n", __FILE__, __LINE__);
	abort();
    }

    md_currentThread   = (*env)->GetStaticMethodID(env, cls_Thread, "currentThread", "()Ljava/lang/Thread;");
    if (md_currentThread == NULL) {
	fprintf(stderr, "%s.%d Cannot find static method currentThread()Ljava/laang/Thread;\n", __FILE__, __LINE__);
	abort();
    }

    cls_Object = (*env)->FindClass(env, "java/lang/Object");
    if (cls_Object == NULL) {
	fprintf(stderr, "Cannot find class java/lang/Object\n");
	abort();
    }
    cls_Object = (jclass)(*env)->NewGlobalRef(env, (jobject)cls_Object);

    md_toString = (*env)->GetMethodID(env, cls_Object, "toString", "()Ljava/lang/String;");
    if (md_toString == NULL) {
	fprintf(stderr, "Cannot find method toString\n");
	abort();
    }

    md_dumpStack   = (*env)->GetStaticMethodID(env, cls_Thread, "dumpStack", "()V");
    if (md_dumpStack == NULL) {
	fprintf(stderr, "%s.%d Cannot find static method dumpStack()V\n", __FILE__, __LINE__);
	abort();
    }

    ibmp_cls_Ibis = (*env)->FindClass(env, "ibis/ipl/impl/messagePassing/Ibis");
    if (ibmp_cls_Ibis == NULL) {
	fprintf(stderr, "%s.%d Cannot find class ibis/ipl/impl/messagePassing/Ibis\n", __FILE__, __LINE__);
	abort();
    }
    ibmp_cls_Ibis = (jclass)(*env)->NewGlobalRef(env, (jobject)ibmp_cls_Ibis);

    fld_Ibis_ibis = (*env)->GetStaticFieldID(env, ibmp_cls_Ibis, "myIbis", "Libis/ipl/impl/messagePassing/Ibis;");
    if (fld_Ibis_ibis == NULL) {
	fprintf(stderr, "%s.%d Cannot find static field myIbis:Libis/ipl/impl/messagePassing/Ibis;\n", __FILE__, __LINE__);
	abort();
    }

    ibmp_obj_Ibis_ibis = (*env)->GetStaticObjectField(env, ibmp_cls_Ibis, fld_Ibis_ibis);
    ibmp_obj_Ibis_ibis = (*env)->NewGlobalRef(env, ibmp_obj_Ibis_ibis);

    md_checkLockOwned = (*env)->GetMethodID(env, ibmp_cls_Ibis, "checkLockOwned",
				       "()V");
    if (md_checkLockOwned == NULL) {
	fprintf(stderr, "Cannot find method checkLockOwned\n");
	abort();
    }
    IBP_VPRINTF(2000, env, ("here..\n"));

    md_checkLockNotOwned = (*env)->GetMethodID(env, ibmp_cls_Ibis, "checkLockNotOwned",
				       "()V");
    if (md_checkLockNotOwned == NULL) {
	fprintf(stderr, "Cannot find method checkLockNotOwned\n");
	abort();
    }
    IBP_VPRINTF(2000, env, ("here..\n"));

    md_lock = (*env)->GetMethodID(env, ibmp_cls_Ibis, "lock", "()V");
    if (md_lock == NULL) {
	fprintf(stderr, "Cannot find method lock\n");
	abort();
    }
    IBP_VPRINTF(2000, env, ("here..\n"));

    md_unlock = (*env)->GetMethodID(env, ibmp_cls_Ibis, "unlock", "()V");
    if (md_unlock == NULL) {
	fprintf(stderr, "Cannot find method unlock\n");
	abort();
    }
    IBP_VPRINTF(2000, env, ("here..\n"));

    ibmp_receive_port_identifier_init(env);
    IBP_VPRINTF(2000, env, ("here..\n"));
    ibmp_receive_port_ns_init(env);
    IBP_VPRINTF(2000, env, ("here..\n"));
    ibmp_send_port_init(env);
    IBP_VPRINTF(2000, env, ("here..\n"));
}


void
ibmp_end(JNIEnv *env, jobject this)
{
    ibmp_send_port_end(env);
    ibmp_receive_port_ns_end(env);
    ibmp_receive_port_identifier_end(env);
}

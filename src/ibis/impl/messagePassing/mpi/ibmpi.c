/*
 * Code shared by natives for package ibis.ipl.impl.messagePassing.mpi
 */

#include <string.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <unistd.h>

#include <jni.h>

#include <mpi.h>

#include "ibis_ipl_impl_mpi_MPIbis.h"

#include "ibmpi.h"
#include "ibmpi_mp.h"
#include "ibmpi_connect.h"
#include "ibmpi_disconnect.h"
#include "ibmpi_receive_port_ns.h"
#include "ibmpi_receive_port_identifier.h"
#include "ibmpi_send_port.h"
#include "ibmpi_byte_input_stream.h"
#include "ibmpi_byte_output_stream.h"
#include "ibmpi_poll.h"
#include "ibmpi_join.h"


JNIEnv  *ibmpi_JNIEnv = NULL;


static jclass		cls_Thread;
static jmethodID	md_yield;
static jmethodID	md_currentThread;
static jmethodID	md_dumpStack;

static jclass		cls_Object;
static jmethodID	md_toString;

static jmethodID	md_checkLockOwned;
static jmethodID	md_checkLockNotOwned;

jclass		ibmpi_cls_MPIbis;
jobject		ibmpi_obj_MPIbis_ibis;
JavaVM          *vm;


#ifdef IBMPI_VERBOSE

int		ibmpi_verbose;

int
ibmpi_stderr_printf(char *fmt, ...)
{
    va_list	ap;
    int		ret;

    va_start(ap, fmt);
    ret = vfprintf(stderr, fmt, ap);
    va_end(ap);

    return ret;
}

#endif


JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved)
{
    vm = jvm;  /* cache the JavaVM pointer */
    return JNI_VERSION_1_2;
}

char *
ibmpi_currentThread(JNIEnv *env)
{
    jobject	cT = (*env)->CallStaticObjectMethod(env, cls_Thread, md_currentThread);
    jstring	t = (*env)->CallObjectMethod(env, cT, md_toString);
    jbyte      *b;
    static char *c = NULL;
    static int	n_c = 0;

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


jlong
Java_ibis_ipl_impl_mpi_MPIbis_currentTime(JNIEnv *env, jclass c)
{
    union lt {
	double	t;
	jlong	l;
    } lt;

    lt.t = MPI_Wtime();

    return lt.l;
}


jdouble
Java_ibis_ipl_impl_mpi_MPIbis_t2d(JNIEnv *env, jclass c, jlong l)
{
    union lt {
	struct pan_time t;
	jlong	l;
    } lt;

    lt.l = l;
    return (jdouble)lt.t;
}


void
ibmpi_dumpStack(JNIEnv *env)
{
    (*env)->CallStaticVoidMethod(env, cls_Thread, md_dumpStack);
}


void
ibmpi_object_toString(JNIEnv *env, jobject obj)
{
    jstring name;
    jbyte *b;
IBMPI_VPRINTF(100, env, ("\n"));

    name = (*env)->CallObjectMethod(env, obj, md_toString);
IBMPI_VPRINTF(100, env, ("\n"));
    if (name == NULL) {
	fprintf(stderr, "Cannot call toString()\n");
    }

    b = (jbyte *)(*env)->GetStringUTFChars(env, name, NULL);
IBMPI_VPRINTF(100, env, ("\n"));
    if (b == NULL) {
	fprintf(stderr, "Cannot get string bytes\n");
    }

    printf("object = %s", b);
    fflush(stdout);

    (*env)->ReleaseStringUTFChars(env, name, b);
}



void
ibmpi_thread_yield(JNIEnv *env)
{
    (*env)->CallStaticVoidMethod(env, cls_Thread, md_yield);
}


void
Java_ibis_ipl_impl_mpi_MPIbis_lock(JNIEnv *env, jobject lock)
{
    if ((*env)->MonitorEnter(env, lock) < 0) {
	fprintf(stderr, "Illegal MonitorEnter\n");
	abort();
    }
}


void
Java_ibis_ipl_impl_mpi_MPIbis_unlock(JNIEnv *env, jobject lock)
{
    if ((*env)->MonitorExit(env, lock) < 0) {
	fprintf(stderr, "Illegal MonitorExit\n");
	abort();
    }
}


void
ibmpi_lock(JNIEnv *env)
{
    ibmpi_lock_check_not_owned(env);
    Java_ibis_ipl_impl_mpi_MPIbis_lock(env, ibmpi_obj_MPIbis_ibis);
}


void
ibmpi_unlock(JNIEnv *env)
{
    ibmpi_lock_check_owned(env);
    return Java_ibis_ipl_impl_mpi_MPIbis_unlock(env, ibmpi_obj_MPIbis_ibis);
}

#undef ibmpi_lock_check_owned
#undef ibmpi_lock_check_not_owned

void
ibmpi_lock_check_owned(JNIEnv *env)
{
    (*env)->CallVoidMethod(env, ibmpi_obj_MPIbis_ibis, md_checkLockOwned);
}


void
ibmpi_lock_check_not_owned(JNIEnv *env)
{
    (*env)->CallVoidMethod(env, ibmpi_obj_MPIbis_ibis, md_checkLockNotOwned);
}


int
ibmpi_consume(JNIEnv *env, pan_msg_p msg, void *buf, int len)
{
    int		rd;

#ifndef NDEBUG
    if (! pan_msg_sane(msg)) {
	fprintf(stderr, "This seems an insane msg: %p\n", msg);
	ibmpi_dumpStack(env);
    }
#endif

#ifndef NON_BLOCKING_CONSUME
    rd = pan_msg_consume_left(msg);
    if (rd < len) {
	len = rd;
    }
    if (len == 0) {
	return -1;
    }
#endif

    ibmpi_set_JNIEnv(env);

    rd = 0;
    while (1) {
#ifndef NON_BLOCKING_CONSUME
	rd += pan_msg_consume(msg, (char *)buf + rd, (int)len);
#else
	rd += pan_msg_consume_non_blocking(msg, (char *)buf + rd, (int)len);
#endif
	if (rd == len) {
	    break;
	}
	if (pan_msg_consume_left(msg) == 0) {
	    if (rd == 0) {
		rd = -1;
	    }
	    break;
	}

	ibmpi_unlock(env);
	ibmpi_thread_yield(env);
	ibmpi_lock(env);
    }

    ibmpi_unset_JNIEnv();

    return rd;
}


jstring
ibmpi_string_consume(JNIEnv *env, pan_msg_p msg, int len)
{
    char	buf[len + 1];

    ibmpi_consume(env, msg, buf, len);
    buf[len] = '\0';
    IBMPI_VPRINTF(400, env, ("Msg consume string[%d] = \"%s\"\n", len, (char *)buf));
    return (*env)->NewStringUTF(env, buf);
}


void
ibmpi_string_push(JNIEnv *env, void *v_buf, int *offset, jstring s, int len)
{
    const jbyte *c;
    char       *buf = v_buf;

    assert(len == (*env)->GetStringUTFLength(env, s));

    c = (void *)(*env)->GetStringUTFChars(env, s, NULL);
    mempcy(buf + *offset, c, len);
    IBMPI_VPRINTF(400, env, ("Msg push string[%d] = \"%s\"\n", len, (char *)c));
    (*env)->ReleaseStringUTFChars(env, s, buf + *offset);

    *offset += len;
}


static int
hostname_equal(char *h0, char *h1)
{
    char       *dot0;
    char       *dot1;

    if (strcmp(h0, h1) == 0) {
	return 1;
    }
    dot0 = strchr(h0, '.');
    dot1 = strchr(h1, '.');

    if (dot0 == NULL) {
	if (dot1 == NULL) {
	    return 0;
	}
	return (strncmp(h0, h1, dot1 - h1) == 0);
    }
    if (dot1 == NULL) {
	return (strncmp(h0, h1, dot0 - h0) == 0);
    }
    return 0;
}


static void
ibmpi_mpi_init(void)
{
    MPI_Init(NULL, NULL);

#ifdef IBMPI_VERBOSE
    if (pan_arg_int(&argc, argv, "-ibp-v", &ibmpi_verbose) == -1) {
	fprintf(stderr, "-ibp-v requires an integer argument\n");
    }
    fprintf(stderr, "ibmpi_verbose = %d\n", ibmpi_verbose);
#endif
}


void
Java_ibis_ipl_impl_mpi_MPIbis_mpi_1init(JNIEnv *env, jobject this)
{
    jfieldID	fld_MPIbis_ibis;
    jfieldID	fld_MPIbis_nrCpus;
    jfieldID	fld_MPIbis_myCpu;
    int		me;
    int		nr;

    ibmp_init(env, this);

    cls_Thread = (*env)->FindClass(env, "java/lang/Thread");
    if (cls_Thread == NULL) {
	fprintf(stderr, "%s.%d Cannot find class java/lang/Thread\n", __FILE__, __LINE__);
    }
    cls_Thread = (jclass)(*env)->NewGlobalRef(env, (jobject)cls_Thread);

    md_yield   = (*env)->GetStaticMethodID(env, cls_Thread, "yield", "()V");
    if (md_yield == NULL) {
	fprintf(stderr, "%s.%d Cannot find static method yield()V\n", __FILE__, __LINE__);
    }

    md_currentThread   = (*env)->GetStaticMethodID(env, cls_Thread, "currentThread", "()Ljava/lang/Thread;");
    if (md_currentThread == NULL) {
	fprintf(stderr, "%s.%d Cannot find static method currentThread()Ljava/laang/Thread;\n", __FILE__, __LINE__);
    }

    md_dumpStack   = (*env)->GetStaticMethodID(env, cls_Thread, "dumpStack", "()V");
    if (md_dumpStack == NULL) {
	fprintf(stderr, "%s.%d Cannot find static method dumpStack()V\n", __FILE__, __LINE__);
    }

    ibmpi_cls_MPIbis = (*env)->FindClass(env, "ibis/ipl/impl/mpi/MPIbis");
    if (ibmpi_cls_MPIbis == NULL) {
	fprintf(stderr, "%s.%d Cannot find class ibis/ipl/impl/mpi/MPIbis\n", __FILE__, __LINE__);
    }
    ibmpi_cls_MPIbis = (jclass)(*env)->NewGlobalRef(env, (jobject)ibmpi_cls_MPIbis);

    fld_MPIbis_ibis = (*env)->GetStaticFieldID(env, ibmpi_cls_MPIbis, "myIbis", "Libis/ipl/impl/mpi/MPIbis;");
    if (fld_MPIbis_ibis == NULL) {
	fprintf(stderr, "%s.%d Cannot find static field myIbis:Libis/ipl/impl/mpi/MPIbis;\n", __FILE__, __LINE__);
    }

    ibmpi_obj_MPIbis_ibis = (*env)->GetStaticObjectField(env, ibmpi_cls_MPIbis, fld_MPIbis_ibis);
    ibmpi_obj_MPIbis_ibis = (*env)->NewGlobalRef(env, ibmpi_obj_MPIbis_ibis);

    md_checkLockOwned = (*env)->GetMethodID(env, ibmpi_cls_MPIbis, "checkLockOwned",
				       "()V");
    if (md_checkLockOwned == NULL) {
	fprintf(stderr, "Cannot find method checkLockOwned\n");
    }

    md_checkLockNotOwned = (*env)->GetMethodID(env, ibmpi_cls_MPIbis, "checkLockNotOwned",
				       "()V");
    if (md_checkLockNotOwned == NULL) {
	fprintf(stderr, "Cannot find method checkLockNotOwned\n");
    }

    fld_MPIbis_nrCpus = (*env)->GetFieldID(env, ibmpi_cls_MPIbis, "nrCpus", "I");
    if (fld_MPIbis_nrCpus == NULL) {
	fprintf(stderr, "%s.%d Cannot find static field nrCpus:I\n", __FILE__, __LINE__);
    }
    fld_MPIbis_myCpu  = (*env)->GetFieldID(env, ibmpi_cls_MPIbis, "myCpu", "I");
    if (fld_MPIbis_myCpu == NULL) {
	fprintf(stderr, "%s.%d Cannot find static field myCpu:I\n", __FILE__, __LINE__);
    }

    cls_Object = (*env)->FindClass(env, "java/lang/Object");
    if (cls_Object == NULL) {
	fprintf(stderr, "Cannot find class java/lang/Object\n");
    }
    cls_Object = (jclass)(*env)->NewGlobalRef(env, (jobject)cls_Object);

    md_toString = (*env)->GetMethodID(env, cls_Object, "toString", "()Ljava/lang/String;");
    if (md_toString == NULL) {
	fprintf(stderr, "Cannot find method toString\n");
    }

    ibmpi_mpi_init();
    if (MPI_COMM_SIZE(MPI_COMM_WORLD, &nr) != MPI_SUCCESS) {
	fprintf(stderr, "%s:%d MPI_COMM_SIZE() fails\n", __FILE__, __LINE__);
    }
    if (MPI_COMM_RANK(MPI_COMM_WORLD, &me) != MPI_SUCCESS) {
	fprintf(stderr, "%s:%d MPI_COMM_RANK() fails\n", __FILE__, __LINE__);
    }

    (*env)->SetIntField(env, ibmpi_obj_MPIbis_ibis, fld_MPIbis_nrCpus,
		(jint)nr());
    (*env)->SetIntField(env, ibmpi_obj_MPIbis_ibis, fld_MPIbis_myCpu,
		(jint)me());

    ibmpi_mp_init(env);

    ibmpi_join_init(env);

    ibmpi_receive_port_identifier_init(env);
    ibmpi_receive_port_ns_init(env);

    ibmpi_connect_init(env);
    ibmpi_disconnect_init(env);
    ibmpi_send_port_init(env);
    ibmpi_byte_output_stream_init(env);
    ibmpi_byte_input_stream_init(env);

    ibmpi_poll_init(env);
    
    pan_start();
    IBMPI_VPRINTF(10, env, ("%s.%d: ibmpi_init\n", __FILE__, __LINE__));
}


void
Java_ibis_ipl_impl_mpi_MPIbis_ibmpi_1end(JNIEnv *env, jobject this)
{
    IBMPI_VPRINTF(10, env, ("%s.%d ibmpi_end()\n", __FILE__, __LINE__));

    ibmpi_mp_end(env);
    IBMPI_VPRINTF(10, env, ("%s.%d ibmpi_end()\n", __FILE__, __LINE__));
    ibmpi_poll_end(env);

    IBMPI_VPRINTF(10, env, ("%s.%d ibmpi_end()\n", __FILE__, __LINE__));
    ibmpi_byte_input_stream_end(env);
    IBMPI_VPRINTF(10, env, ("%s.%d ibmpi_end()\n", __FILE__, __LINE__));
    ibmpi_byte_output_stream_end(env);
    IBMPI_VPRINTF(10, env, ("%s.%d ibmpi_end()\n", __FILE__, __LINE__));
    ibmpi_send_port_end(env);
    IBMPI_VPRINTF(10, env, ("%s.%d ibmpi_end()\n", __FILE__, __LINE__));
    ibmpi_disconnect_end(env);
    IBMPI_VPRINTF(10, env, ("%s.%d ibmpi_end()\n", __FILE__, __LINE__));
    ibmpi_connect_end(env);
    IBMPI_VPRINTF(10, env, ("%s.%d ibmpi_end()\n", __FILE__, __LINE__));

    ibmpi_receive_port_ns_end(env);
    IBMPI_VPRINTF(10, env, ("%s.%d ibmpi_end()\n", __FILE__, __LINE__));
    ibmpi_receive_port_identifier_end(env);
    IBMPI_VPRINTF(10, env, ("%s.%d ibmpi_end()\n", __FILE__, __LINE__));

    ibmpi_join_end(env);

    MPI_Finalize();
    IBMPI_VPRINTF(10, env, ("%s.%d ibmpi_end()\n", __FILE__, __LINE__));
}

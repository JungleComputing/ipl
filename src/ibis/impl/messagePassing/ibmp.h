#ifndef __IBIS_MANTA_IBIS_IMPL_MESSAGE_PASSING_IBP_H__
#define __IBIS_MANTA_IBIS_IMPL_MESSAGE_PASSING_IBP_H__

#include <stdio.h>

#include <jni.h>

#include <pan_sys.h>

void ibmp_object_toString(JNIEnv *env, jobject obj, FILE *out);
char *ibmp_jstring2c(JNIEnv *env, jstring name);
void ibmp_thread_yield(JNIEnv *env);
void ibmp_lock(JNIEnv *env);
void ibmp_unlock(JNIEnv *env);
char *ibmp_currentThread(JNIEnv *env);
void ibmp_dumpStack(JNIEnv *env);
int ibmp_equals(JNIEnv *env, jobject obj1, jobject obj2);
void ibmp_throwable_printStackTrace(JNIEnv *env, jthrowable exc);

void ibmp_lock_check_owned(JNIEnv *env);
void ibmp_lock_check_not_owned(JNIEnv *env);

#ifndef NDEBUG
#define IBP_VERBOSE
#define IBP_STATISTICS
#endif

#ifdef __GNUC__
#define IBP_INLINE	    inline
#elif defined _M_IX86
#define IBP_INLINE	    __inline
#else
#define IBP_INLINE
#endif


#ifdef IBP_VERBOSE
extern int ibmp_verbose;
int ibmp_stderr_printf(char *fmt, ...);
#define IBP_VPRINTF(n, env, s) \
	    do if ((n) <= ibmp_verbose) { \
		fprintf(stderr, "%2d: %s.%d ", ibmp_pid_me(), __FILE__, __LINE__); \
		if ((env) != NULL) fprintf(stderr, "%s: ", ibmp_currentThread(env)); \
		ibmp_stderr_printf s; \
		fflush(stderr); \
	    } while (0)

#else
#define ibmp_lock_check_owned(env)
#define ibmp_lock_check_not_owned(env)
#define IBP_VPRINTF(n, env, s)
#endif


/**
 * The execution model with upcalls introduces one important requirement.
 *
 * When a Java thread wants to enter the native MessagePassing (MP) module,
 * it must own the Ibis lock. Upcalls that result from calls to the MP module
 * are in requirement of a valid JNIEnv pointer, which is thread-specific.
 * The solution is to assign the global variable ipb_JNIEnv with the JNIEnv
 * pointer of the thread that does an MP function call that may cause an upcall
 * (like poll, send, consume, ...), and the upcall uses that global JNIEnv
 * pointer.
 * However, if the upcall releases (and later re-acquires) the Ibis lock,
 * other threads may have visited the MP module in between, and the global
 * ibp_JNIEnv is overridden by the thread-specific JNIEnv values of these
 * other threads. Therefore the global value must be restored to the value
 * of the upcall thread's JNIEnv immediately after it has reacquired the
 * lock.
 * This is especially so in the case of calls from native code to Java
 * methods that may invoke unlock() or wait(). After returning from such
 * calls the global value must be restored before any other upcall-prone
 * MP call is done.
 */

extern JNIEnv	       *ibp_JNIEnv;

#define ibp_get_JNIEnv()	(ibp_JNIEnv)
#define ibp_set_JNIEnv(env)	do { ibp_JNIEnv = (env); } while (0)


extern jclass		ibmp_cls_Ibis;
extern jobject		ibmp_obj_Ibis_ibis;

extern jclass		cls_java_io_IOException;

extern int		ibmp_me;
extern int		ibmp_nr;

int ibmp_pid_me(void);
int ibmp_pid_nr(void);

void ibmp_check_ibis_name(JNIEnv *env, const char *name);


/* Replicate pan_msg_iovec_len here to allow inlining :-( */
static IBP_INLINE int
ibmp_iovec_len(pan_iovec_p iov, int n)
{
    int		i;
    int		len = 0;

    for (i = 0; i < n; i++) {
	len += iov[i].len;
    }

    return len;
}


#include <stdio.h>
#define ibmp_error	\
	fprintf(stderr, "%s.%d: ", __FILE__, __LINE__); ibmp_error_printf
void ibmp_error_printf(JNIEnv *env, const char *fmt, ...);

void ibmp_init(JNIEnv *env, jobject this);
void ibmp_end(JNIEnv *env, jobject this);

#endif

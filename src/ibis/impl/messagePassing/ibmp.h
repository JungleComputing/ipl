#ifndef __IBIS_MANTA_IBIS_IMPL_MESSAGE_PASSING_IBP_H__
#define __IBIS_MANTA_IBIS_IMPL_MESSAGE_PASSING_IBP_H__

#include <jni.h>

#include <pan_sys.h>

void ibmp_object_toString(JNIEnv *env, jobject obj);
char *ibmp_jstring2c(JNIEnv *env, jstring name);
void ibmp_thread_yield(JNIEnv *env);
void ibmp_lock(JNIEnv *env);
void ibmp_unlock(JNIEnv *env);
char *ibmp_currentThread(JNIEnv *env);
void ibmp_dumpStack(JNIEnv *env);

void ibmp_lock_check_owned(JNIEnv *env);
void ibmp_lock_check_not_owned(JNIEnv *env);

#ifndef NDEBUG
#define IBP_VERBOSE
#define IBP_STATISTICS
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

extern jclass		ibmp_cls_Ibis;
extern jobject		ibmp_obj_Ibis_ibis;

extern jclass		cls_IbisIOException;

extern int		ibmp_me;
extern int		ibmp_nr;

int ibmp_pid_me(void);
int ibmp_pid_nr(void);

void ibmp_check_ibis_name(JNIEnv *env, const char *name);


/* Replicate pan_msg_iovec_len here to allow inlining :-( */
static inline int
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

/*
 * Native code for the interrupt catch thread
 */

#include <jni.h>

#include "ibmp.h"

#include "ibis_impl_messagePassing_InterruptCatcher.h"


static jfieldID	fld_signo;


static void
init_fields(JNIEnv *env)
{
    static int	inited = 0;
    jclass	cls_InterruptCatcher;

    if (inited) {
	return;
    }

    inited = 1;

    cls_InterruptCatcher = (*env)->FindClass(env, "ibis/impl/messagePassing/InterruptCatcher");
    if (cls_InterruptCatcher == NULL) {
	ibmp_error(env, "Cannot finr class ibis/impl/messagePassing/InterruptCatcher\n");
    }

    fld_signo = (*env)->GetFieldID(env, cls_InterruptCatcher, "signo", "I");
}


JNIEXPORT jboolean JNICALL
Java_ibis_impl_messagePassing_InterruptCatcher_supported(JNIEnv *env, jobject this)
{
    return JNI_TRUE;
}

#ifdef _M_IX86
#define IBP_NO_INTPT
#endif

#ifndef IBP_NO_INTPT

#include <signal.h>

#include <pthread.h>

/*
 * This should be handled inside LFC: the first thread that switches on
 * interrupts should be the thread that requests the SIGIOs.
 * There, we are also aware of using pthreads (which we are not, here,
 * really).
 */
pthread_t ibmp_sigcatcher_pthread;
#endif

JNIEXPORT void JNICALL
Java_ibis_impl_messagePassing_InterruptCatcher_registerHandler(JNIEnv *env, jobject this)
{
#ifndef IBP_NO_INTPT
    extern void pan_comm_intr_enable(void);

    ibmp_sigcatcher_pthread = pthread_self();
    pan_comm_intr_enable();
#endif
}


JNIEXPORT void JNICALL
Java_ibis_impl_messagePassing_InterruptCatcher_waitForSignal(JNIEnv *env, jobject this)
{
#ifndef IBP_NO_INTPT
    sigset_t	mask;
    int		signo;

    init_fields(env);
    signo = (*env)->GetIntField(env, this, fld_signo);

    if (sigemptyset(&mask) != 0) {
	ibmp_error(env, "sigemptyset fails\n");
    }
    if (sigaddset(&mask, (int)signo) != 0) {
	ibmp_error(env, "sigaddset fails\n");
    }

    fprintf(stderr, "Go to sleep in sigwait\n");
    if (sigwait(&mask, &signo) != 0) {
	ibmp_error(env, "sigwait fails\n");
    }
    fprintf(stderr, "Woken up from sigwait\n");
#endif
}

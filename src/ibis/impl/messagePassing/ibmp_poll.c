#include <sys/types.h>
#include <signal.h>
#ifndef _M_IX86
#include <unistd.h>
#endif
#include <stdlib.h>

#include <jni.h>

#include <pan_sys.h>

#include "ibis_ipl_impl_messagePassing_Poll.h"

#include "ibmp_poll.h"


static int   (**poll_func)(JNIEnv *env) = NULL;
static int	n_poll_func = 0;

#ifdef IBP_STATISTICS
static long long	poll_calls = 0;
#endif


void
ibmp_poll_register(int (*poll)(JNIEnv *env))
{
    poll_func = pan_realloc(poll_func, (n_poll_func + 1) * sizeof(*poll_func));
    poll_func[n_poll_func] = poll;
    n_poll_func++;
}


JNIEXPORT jboolean JNICALL
Java_ibis_ipl_impl_messagePassing_Poll_msg_1poll(JNIEnv *env, jobject this)
{
    int	i;
    jboolean poll_succeeded = JNI_FALSE;

#ifdef IBP_STATISTICS
    poll_calls++;
#endif
    for (i = 0; i < n_poll_func; i++) {
	if (poll_func[i](env)) {
	    poll_succeeded = JNI_TRUE;
	}
    }

    return poll_succeeded;
}


JNIEXPORT void JNICALL
Java_ibis_ipl_impl_messagePassing_Poll_abort(JNIEnv *env, jobject this)
{
#ifdef _M_IX86
    fprintf(stderr, "Poll.abort not implemented for win32\n");
#else
    kill(getpid(), SIGQUIT);
#endif
}



void
ibmp_poll_init(JNIEnv *env)
{
}


void
ibmp_poll_end(JNIEnv *env)
{
#ifdef IBP_STATISTICS
    fprintf(stderr, "poll calls %lld\n", poll_calls);
#endif
}

#include <sys/types.h>
#include <signal.h>
#include <unistd.h>
#include <stdlib.h>

#include <jni.h>

#include <pan_sys.h>

#include "ibis_ipl_impl_messagePassing_Poll.h"

#include "ibmp_poll.h"


static void  (**poll_func)(JNIEnv *env) = NULL;
static int	n_poll_func = 0;

#ifdef IBP_STATISTICS
static long long	poll_calls = 0;
#endif


void
ibmp_poll_register(void (*poll)(JNIEnv *env))
{
    poll_func = pan_realloc(poll_func, (n_poll_func + 1) * sizeof(*poll_func));
    poll_func[n_poll_func] = poll;
    n_poll_func++;
}


void
Java_ibis_ipl_impl_messagePassing_Poll_msg_1poll(JNIEnv *env, jobject this)
{
    int	i;

#ifdef IBP_STATISTICS
    poll_calls++;
#endif
    for (i = 0; i < n_poll_func; i++) {
	poll_func[i](env);
    }
}


void
Java_ibis_ipl_impl_messagePassing_Poll_abort(JNIEnv *env, jobject this)
{
    kill(getpid(), SIGQUIT);
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

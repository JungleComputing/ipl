#include <sys/types.h>
#include <signal.h>
#include <unistd.h>

#include <jni.h>

#include "ibis_ipl_impl_messagePassing_Poll.h"


void
Java_ibis_ipl_impl_messagePassing_Poll_abort(JNIEnv *env, jobject this)
{
    kill(getpid(), SIGQUIT);
}

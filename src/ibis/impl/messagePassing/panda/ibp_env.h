#ifndef __IBIS_IPL_IMPL_MP_PANDA_IBP_ENV_H__
#define __IBIS_IPL_IMPL_MP_PANDA_IBP_ENV_H__

#include <jni.h>

#include <pan_sys.h>

extern int		ibp_intr_enabled;
extern JNIEnv	       *ibp_JNIEnv;

#ifndef NDEBUG
extern pan_key_p	ibp_env_key;
#define ibp_set_JNIEnv(env) \
	{ \
	    JNIEnv *old_env = ibp_JNIEnv; \
	    pan_key_setspecific(ibp_env_key, env); \
	    ibp_JNIEnv = (env);
#define ibp_unset_JNIEnv() \
	    ibp_JNIEnv = old_env; \
	}
#define ibp_get_JNIEnv()	pan_key_getspecific(ibp_env_key)
#else
#define ibp_get_JNIEnv()	(ibp_JNIEnv)
#define ibp_set_JNIEnv(env)	do ibp_JNIEnv = (env); while (0)
#define ibp_unset_JNIEnv()
#endif

#endif

#include <stdlib.h>

#include <jni.h>

#include "ibmp.h"
#include "ibmp_receive_port_ns.h"

static jobject		obj_Ibis_nameServer;

static jmethodID	md_NameServer_bind;
static jmethodID	md_NameServer_lookup;
static jmethodID	md_NameServer_unbind;


int		ibmp_ns_server = 0;


void
ibmp_receive_port_ns_bind(JNIEnv *env,
			  jstring name,
			  jbyteArray id,
			  jint sender,
			  jint client)
{
    IBP_VPRINTF(50, env, ("Do java call NameServer.bind this = %p client = 0x%x = %d\n",
		obj_Ibis_nameServer, (int)client, (int)client));
    IBP_VPRINTF(50, env, ("%s", (ibmp_object_toString(env, obj_Ibis_nameServer, stderr), "\n")));
    IBP_VPRINTF(50, env, ("%s", (ibmp_object_toString(env, id, stderr), "\n")));

    (*env)->CallVoidMethod(env,
			   obj_Ibis_nameServer,
			   md_NameServer_bind,
			   name,
			   id,
			   sender,
			   client);
}


void
ibmp_receive_port_ns_lookup(JNIEnv *env, jstring name, jint sender, jint client)
{
    (*env)->CallVoidMethod(env,
			   obj_Ibis_nameServer,
			   md_NameServer_lookup,
			   name,
			   sender,
			   client);
}


void
ibmp_receive_port_ns_unbind(JNIEnv *env, jstring name)
{
    (*env)->CallVoidMethod(env,
			   obj_Ibis_nameServer,
			   md_NameServer_unbind,
			   name);
}


void
ibmp_receive_port_ns_init(JNIEnv *env)
{
    jclass	cls_Registry;
    jfieldID	fld_ibis;
    jobject	obj_ibis;
    jfieldID	fld_registry;
    jobject	obj_registry;
    jfieldID	fld_ns;
    jclass	cls_ns;

    IBP_VPRINTF(2000, env, ("here..\n"));
    fld_ibis = (*env)->GetStaticFieldID(env,
				        ibmp_cls_Ibis,
				        "myIbis",
				        "Libis/ipl/impl/messagePassing/Ibis;");
    if (fld_ibis == NULL) {
	fprintf(stderr, "%s.%d Cannot find static field myIbis:Libis/ipl/impl/messagePassing/Ibis;\n", __FILE__, __LINE__);
	abort();
    }
    IBP_VPRINTF(2000, env, ("here..\n"));

    obj_ibis = (*env)->GetStaticObjectField(env, ibmp_cls_Ibis, fld_ibis);

    fld_registry = (*env)->GetFieldID(env,
				ibmp_cls_Ibis,
				"registry",
				"Libis/ipl/impl/messagePassing/Registry;");
    if (fld_registry == NULL) {
	fprintf(stderr, "%s.%d Cannot find field registry:Libis/ipl/impl/messagePassing/Registry;\n", __FILE__, __LINE__);
	abort();
    }
    IBP_VPRINTF(2000, env, ("here..\n"));

    obj_registry = (*env)->GetObjectField(env, obj_ibis, fld_registry);

    cls_Registry = (*env)->FindClass(env, "ibis/ipl/impl/messagePassing/Registry");
    if (cls_Registry == NULL) {
	fprintf(stderr, "%s.%d Cannot find class ibis/ipl/impl/messagePassing/Registry\n", __FILE__, __LINE__);
	abort();
    }
    cls_Registry = (*env)->NewGlobalRef(env, cls_Registry);
    IBP_VPRINTF(2000, env, ("here..\n"));

    fld_ns = (*env)->GetFieldID(env,
				cls_Registry,
				"nameServer",
				"Libis/ipl/impl/messagePassing/ReceivePortNameServer;");
    if (fld_ns == NULL) {
	fprintf(stderr, "%s.%d Cannot find field nameServer:Libis/ipl/impl/messagePassing/ReceivePortNameServer;\n", __FILE__, __LINE__);
	abort();
    }
    IBP_VPRINTF(2000, env, ("here..\n"));

    if (obj_registry != NULL) {
	obj_Ibis_nameServer = (*env)->GetObjectField(env,
				    obj_registry,
				    fld_ns);
	IBP_VPRINTF(2000, env, ("here..\n"));
	obj_Ibis_nameServer = (*env)->NewGlobalRef(env, obj_Ibis_nameServer);
	IBP_VPRINTF(2000, env, ("here..\n"));
    }

    cls_ns = (*env)->FindClass(env,
			    "ibis/ipl/impl/messagePassing/ReceivePortNameServer");
    IBP_VPRINTF(2000, env, ("here..\n"));
    if (cls_ns == NULL) {
	fprintf(stderr, "%s.%d Cannot find class ibis/ipl/impl/messagePassing/ReceivePortNameServer\n", __FILE__, __LINE__);
	abort();
    }
    IBP_VPRINTF(2000, env, ("here..\n"));
    cls_ns = (*env)->NewGlobalRef(env, cls_ns);
    IBP_VPRINTF(2000, env, ("here..\n"));

    md_NameServer_bind = (*env)->GetMethodID(env,
					     cls_ns,
					     "bind",
					     "(Ljava/lang/String;[BII)V");
    if (md_NameServer_bind == NULL) {
	fprintf(stderr, "%s.%d Cannot find method bind([BII)V\n", __FILE__, __LINE__);
	abort();
    }
    IBP_VPRINTF(2000, env, ("here..\n"));
    md_NameServer_lookup = (*env)->GetMethodID(env,
					       cls_ns,
					       "lookup",
					       "(Ljava/lang/String;II)V");
    if (md_NameServer_lookup == NULL) {
	fprintf(stderr, "%s.%d Cannot find method lookup(Ljava/lang/String;II)V\n", __FILE__, __LINE__);
	abort();
    }
    IBP_VPRINTF(2000, env, ("here..\n"));
    md_NameServer_unbind = (*env)->GetMethodID(env,
					       cls_ns,
					       "unbind",
					       "(Ljava/lang/String;)V");
    if (md_NameServer_unbind == NULL) {
	fprintf(stderr, "%s.%d Cannot find method unbind(Ljava/lang/String;)V\n", __FILE__, __LINE__);
	abort();
    }

}


void
ibmp_receive_port_ns_end(JNIEnv *env)
{
}

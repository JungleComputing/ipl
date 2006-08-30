#include <string.h>
#include <stddef.h>
#include <stdlib.h>

#include <jni.h>

int main(int argc, char *argv[])
{
    int		i;
    int		options = 0;
    char      **properties = malloc(argc * sizeof(char *));
    int		n_properties = 0;
    char       *main_class = NULL;
    char      **java_options = malloc(argc * sizeof(char *));
    int		n_java_options = 0;
    char       *classpath = NULL;
    JavaVM     *jvm;
    JNIEnv     *env;
    JDK1_1InitArgs vm_args;
    jclass	cls_main;
    jmethodID	md_main;
    jclass	cls_String;
    jarray	jargv;
    jstring	jarg;

    for (i = 1; i < argc; i++) {
	if (0) {
	} else if (strcmp(argv[i], "-classpath") == 0) {
	    classpath = argv[++i];
	} else if (strncmp(argv[i], "-D", 2) == 0) {
	    properties[n_properties++] = argv[i];
	} else if (options == 0) {
	    main_class = argv[i];
	    options++;
	} else {
	    java_options[n_java_options++] = argv[i];
	}
    }

    if (main_class == NULL) {
	fprintf(stderr, "Need to specify a class\n");
	exit(33);
    }

    vm_args.version = 0x00010001;
    JNI_GetDefaultJavaVMInitArgs(&vm_args);
    vm_args.classpath = classpath;

    JNI_CreateJavaVM(&jvm, (void **)&env, &vm_args);
    cls_main = (*env)->FindClass(env, main_class);
    if (cls_main == NULL) {
	fprintf(stderr, "Cannot find class %s\n", main_class);
	abort();
    }

    md_main = (*env)->GetStaticMethodID(env, cls_main, "main", "([Ljava/lang/String;)V");
    if (md_main == NULL) {
	fprintf(stderr, "Cannot find method main(String[])\n");
	abort();
    }

    cls_String = (*env)->FindClass(env, "Ljava/lang/String;");
    if (cls_String == NULL) {
	fprintf(stderr, "Cannot find class String\n");
	abort();
    }

    jargv = (*env)->NewObjectArray(env, n_java_options, cls_String, NULL);
    if (jargv == NULL) {
	fprintf(stderr, "Cannot create String[] argv\n");
    }
    for (i = 0; i < n_java_options; i++) {
	jarg = (*env)->NewStringUTF(env, java_options[i]);
	(*env)->SetObjectArrayElement(env, jargv, i, (jobject)jarg);
    }

    (*env)->CallStaticVoidMethod(env, cls_main, md_main, jargv);

    return 0;
}

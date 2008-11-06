/* handlemanager.c */
#include "handlemanager.h"

jint hmBlocks;
jint hmBlockSize;
jint hmBlocksInUse;
mx_request_t **handles;

/* HandleManager.init() */
JNIEXPORT jboolean JNICALL Java_ibis_ipl_impl_mx_channels_JavaMx_00024HandleManager_init
  (JNIEnv *env, jobject jobj, jint blockSize, jint maxBlocks) {
	hmBlocks = maxBlocks;
	hmBlockSize = blockSize;
	hmBlocksInUse = 0;
	handles = (mx_request_t **) calloc(hmBlocks, sizeof(mx_request_t *));
	if (handles == NULL) {
		return JNI_FALSE;
	}
	return JNI_TRUE;
}

/* HandleManager.addBlock() */
JNIEXPORT jboolean JNICALL Java_ibis_ipl_impl_mx_channels_JavaMx_00024HandleManager_addBlock
  (JNIEnv *env, jobject jobj) {
	if(hmBlocksInUse >= hmBlocks) {
		return JNI_FALSE;
	}
	handles[hmBlocksInUse] = (mx_request_t *) malloc(hmBlockSize * sizeof(mx_request_t));
	if (handles[hmBlocksInUse] == NULL) {
		return JNI_FALSE;
	}
	hmBlocksInUse++;
	return JNI_TRUE;
}

mx_request_t *getRequest(jint handle) {
	/* gets the mx_request_t corresponding to "handle" */
	jint block, offset;
	block = handle / hmBlockSize;
	offset = handle % hmBlockSize;
	if(block > hmBlocksInUse) {
		return NULL;
	}
	return &(handles[(int)block][(int)offset]);
}


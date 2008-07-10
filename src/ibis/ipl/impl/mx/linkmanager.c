/* linkmanager.c */
#include "linkmanager.h"

//TODO store endpointId in link information?

jint lmBlocks;
jint lmBlockSize;
jint lmBlocksInUse;
mx_endpoint_addr_t **links;

/* LinkManager.init() */
JNIEXPORT jboolean JNICALL Java_ibis_ipl_impl_mx_JavaMx_00024LinkManager_init
  (JNIEnv *env, jobject jobj, jint blockSize, jint maxBlocks) {
	lmBlocks = maxBlocks;
	lmBlockSize = blockSize;
	lmBlocksInUse = 0;
	links = (mx_endpoint_addr_t **) calloc(lmBlocks, sizeof(mx_request_t *));
	if (links == NULL) {
		return JNI_FALSE;
	}
	return JNI_TRUE;	
}

/* LinkManager.addBlock() */
JNIEXPORT jboolean JNICALL Java_ibis_ipl_impl_mx_JavaMx_00024LinkManager_addBlock
  (JNIEnv *env, jobject jobj) {
	if(lmBlocksInUse >= lmBlocks) {
		return JNI_FALSE;
	}
	links[lmBlocksInUse] = (mx_endpoint_addr_t *) malloc(lmBlockSize * sizeof(mx_request_t));
	if (links[lmBlocksInUse] == NULL) {
		return JNI_FALSE;
	}
	lmBlocksInUse++;
	return JNI_TRUE;
}

mx_endpoint_addr_t *getAddress(jint link) {
	/* gets the mx_endpoint_addr_t corresponding to "link" */
	jint block, offset;
	block = link / lmBlockSize;
	offset = link % lmBlockSize;
	if(block > lmBlocksInUse) {
		return NULL;
	}
	return &(links[(int)block][(int)offset]);
}

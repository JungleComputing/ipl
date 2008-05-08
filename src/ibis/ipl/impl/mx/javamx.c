/* javamx.c */
#include <stdlib.h>
#include "myriexpress.h"

#include "ibis_ipl_impl_mx_JavaMx.h"
#include "ibis_ipl_impl_mx_JavaMx_LinkManager.h"
#include "ibis_ipl_impl_mx_JavaMx_HandleManager.h"

//TODO I think I should throw Exceptions in case of errors
//TODO store handlerId in link information

/****************** Handle Manager **********************/

jint hmBlocks;
jint hmBlockSize;
jint hmBlocksInUse;
mx_request_t **handles;

/* HandleManager.init() */
JNIEXPORT jboolean JNICALL Java_ibis_ipl_impl_mx_JavaMx_00024HandleManager_init
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
JNIEXPORT jboolean JNICALL Java_ibis_ipl_impl_mx_JavaMx_00024HandleManager_addBlock
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
	return &(handles[block][offset]);
}

/****************** Link Manager ************************/

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
	return &(links[block][offset]);
}

/********************* JavaMx ***************************/

mx_endpoint_t myEndpoint = NULL;
int initialized = 0;


/* init() */
JNIEXPORT jboolean JNICALL Java_ibis_ipl_impl_mx_JavaMx_init
  (JNIEnv *env, jclass jcl) {
	mx_return_t rc;

	if (initialized == 1) {
		//already initialized
		return JNI_TRUE;
	}
	// Attach an error handler that doesn't quit on errors automatically
	mx_set_error_handler(MX_ERRORS_RETURN);
	// now init the MX library
	rc = mx_init();
	if(rc != MX_SUCCESS) {
		//TODO throw exception
		fprintf(stderr, "error: %s\n", mx_strerror(rc));
		return JNI_FALSE;
	}
	initialized = 1;
	return JNI_TRUE;
}

/* deInit() */
JNIEXPORT jboolean JNICALL Java_ibis_ipl_impl_mx_JavaMx_deInit
  (JNIEnv *env, jclass jcl) {
	//TODO check whether all endpoints are closed? Or close them ourselves? Or do nothing?
	
	/* Finalize the MX library */
	mx_finalize();
	initialized = 0;
	return JNI_TRUE;
}

/* newHandler() */
JNIEXPORT jint JNICALL Java_ibis_ipl_impl_mx_JavaMx_newHandler
  (JNIEnv *env, jclass jcl, jint filter) {
	mx_return_t rc;
	//TODO support for multiple handlers
	/* open an endpoint */
	rc = mx_open_endpoint(MX_ANY_NIC, MX_ANY_ENDPOINT, filter, 0, 0, &myEndpoint);
	if(rc != MX_SUCCESS) {
		myEndpoint = NULL;
		fprintf(stderr, "error: %s\n", mx_strerror(rc));
		//TODO throw exception
		return -1;
	}
	return 1;
}

/* closeHandler() */
JNIEXPORT jboolean JNICALL Java_ibis_ipl_impl_mx_JavaMx_closeHandler
  (JNIEnv *env, jclass jcl, jint handlerId) {
	if(handlerId != 1) {
		//wrong handlerId
		return JNI_FALSE;
	}
	
	if(myEndpoint == NULL) {
		//endpoint already closed
		return JNI_TRUE;
	}
	
	/* close endpoint */
	mx_wakeup(myEndpoint); // notify all threads that are blocked on this endpoint
	mx_close_endpoint(myEndpoint);
	return JNI_TRUE;
}

/* getMyNicId() */
JNIEXPORT jlong JNICALL Java_ibis_ipl_impl_mx_JavaMx_getMyNicId
  (JNIEnv *env, jclass jcl, jint handlerId) {
	mx_endpoint_addr_t myAddr;
	uint64_t nicId;
	uint32_t epId;
	
	if(handlerId != 1) {
		// not a valid handler
		// TODO multiple endpoint support
		// TODO throw exception
		return 0;
	}
	mx_get_endpoint_addr(myEndpoint, &myAddr);	
	mx_decompose_endpoint_addr(myAddr, &nicId, &epId);
	
	return nicId;
}

/* getMyEndpointId() */
JNIEXPORT jint JNICALL Java_ibis_ipl_impl_mx_JavaMx_getMyEndpointId
  (JNIEnv *env, jclass jcl, jint handlerId) {
	mx_endpoint_addr_t myAddr;
	uint64_t nicId;
	uint32_t epId;
	
	if(handlerId != 1) {
		// not a valid handler
		// TODO multiple endpoint support
		// TODO throw exception
		return 0;
	}
	mx_get_endpoint_addr(myEndpoint, &myAddr);	
	mx_decompose_endpoint_addr(myAddr, &nicId, &epId);
	
	return epId;
}

/* getNicId() */
JNIEXPORT jlong JNICALL Java_ibis_ipl_impl_mx_JavaMx_getNicId
  (JNIEnv *env, jclass jcl, jstring name) {
	mx_return_t rc;
	uint64_t nicId;
	const char *hostname;
	hostname = (*env)->GetStringUTFChars(env, name, NULL);
	if (hostname == NULL) {
		// TODO throw exception
		return 0;
	}

	rc = mx_hostname_to_nic_id(hostname, &nicId); //compiler warning here, because *hostname is const
	(*env)->ReleaseStringUTFChars(env, name, hostname);
	if(rc != MX_SUCCESS) {
		fprintf(stderr, "error: %s\n", mx_strerror(rc));
		return 0;
	}	
	return nicId;
}

/* connect() without timeout */
JNIEXPORT jboolean JNICALL Java_ibis_ipl_impl_mx_JavaMx_connect__IIJII
  (JNIEnv *env, jclass jcl, jint handlerId, jint link, jlong nicId, jint endpointId, jint filter) {
	return Java_ibis_ipl_impl_mx_JavaMx_connect__IIJIIJ(env, jcl, handlerId, link, nicId, endpointId, filter, MX_INFINITE);
}

/* connect() with timeout */
JNIEXPORT jboolean JNICALL Java_ibis_ipl_impl_mx_JavaMx_connect__IIJIIJ
  (JNIEnv *env, jclass jcl, jint handlerId, jint link, jlong nicId, jint endpointId, jint filter, jlong timeout) {
	mx_return_t rc;
	mx_endpoint_addr_t *address;

	if(handlerId != 1) {
		// not a valid handler
		// TODO multiple endpoint support
		// TODO throw exception
		return JNI_FALSE;
	}
	/* retrieve the address of the link */
	address = getAddress(link);
	if(address == NULL) {
		//no valid link
		//TODO throw exception
		return JNI_FALSE;
	}
	
	/* now connect to the client */
	rc = mx_connect(myEndpoint, nicId, endpointId, filter, (uint32_t)timeout, address); 
	if(rc != MX_SUCCESS) {
		//TODO throw exception
		fprintf(stderr, "error: %s\n", mx_strerror(rc));
		return JNI_FALSE;
	}	
	return JNI_TRUE;
}

/* disconnect() */
JNIEXPORT jboolean JNICALL Java_ibis_ipl_impl_mx_JavaMx_disconnect
  (JNIEnv *env, jclass jcl, jint link) {
	// actually we could do nothing here, but let's check whether the link is present
	
	mx_endpoint_addr_t *address;
	/* retrieve the address of the link */
	address = getAddress(link);
	if(address == NULL) {
		//no valid link
		//TODO throw exception
		return JNI_FALSE;
	}
	return JNI_TRUE;
}


/* send() */
JNIEXPORT void JNICALL Java_ibis_ipl_impl_mx_JavaMx_send
  (JNIEnv *env, jclass jcl, jobject buffer, jint bufferSize, jint handlerId, jint link, jint handle, jlong matchData) {
	mx_return_t rc;
	mx_segment_t bufferDesc[1];
	mx_request_t *request;
	mx_endpoint_addr_t *target;
	
	//TODO: send multiple buffers in one message
	//TODO: multiple endpoint support
	
	if(handlerId != 1) {
		// not a valid handler
		// TODO multiple endpoint support
		// TODO throw exception
		return;
	}
	/* retrieve the target address */
	target = getAddress(link);
	if(target == NULL) {
		//no valid link
		//TODO throw exception
		return;
	}
	/* retrieve the request handle */
	request = getRequest(handle);
	if(request == NULL) {
		//no valid handle
		//TODO throw exception
		return;
	}
	
	/* TODO: check if target is valid */
	bufferDesc[0].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer);
	bufferDesc[0].segment_length = (uint32_t)bufferSize;
	
	rc = mx_isend(myEndpoint, bufferDesc, 1, *target, matchData, NULL, request);
	if(rc != MX_SUCCESS) {
		fprintf(stderr, "error: %s\n", mx_strerror(rc));
		//TODO throw exception
		return;
	}
	return;
}

/* sendSynchronous() */
JNIEXPORT void JNICALL Java_ibis_ipl_impl_mx_JavaMx_sendSynchronous
  (JNIEnv *env, jclass jcl, jobject buffer, jint bufferSize, jint handlerId, jint link, jint handle, jlong matchData) {
	mx_return_t rc;
	mx_segment_t bufferDesc[1];
	mx_request_t *request;
	mx_endpoint_addr_t *target;
	
	//TODO: send multiple buffers in one message
	//TODO: multiple endpoint support
	
	if(handlerId != 1) {
		// not a valid handler
		// TODO multiple endpoint support
		// TODO throw exception
		return;
	}
	/* retrieve the target address */
	target = getAddress(link);
	if(target == NULL) {
		//no valid link
		//TODO throw exception
		return;
	}
	/* retrieve the request handle */
	request = getRequest(handle);
	if(request == NULL) {
		//no valid handle
		//TODO throw exception
		return;
	}
	
	/* TODO: check if target is valid */
	bufferDesc[0].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer);
	bufferDesc[0].segment_length = (uint32_t)bufferSize;
	
	rc = mx_issend(myEndpoint, bufferDesc, 1, *target, matchData, NULL, request);
	if(rc != MX_SUCCESS) {
		fprintf(stderr, "error: %s\n", mx_strerror(rc));
		//TODO throw exception
		return;
	}
	return;
}

/* recv() without mask */
JNIEXPORT void JNICALL Java_ibis_ipl_impl_mx_JavaMx_recv__Ljava_nio_ByteBuffer_2IIIJ
  (JNIEnv *env, jclass jcl, jobject buffer, jint bufferSize, jint handlerId, jint handle, jlong matchData) {
	Java_ibis_ipl_impl_mx_JavaMx_recv__Ljava_nio_ByteBuffer_2IIIJJ(env, jcl, buffer, bufferSize, handlerId, handle, matchData, MX_MATCH_MASK_NONE);
}

/* recv() with mask */
JNIEXPORT void JNICALL Java_ibis_ipl_impl_mx_JavaMx_recv__Ljava_nio_ByteBuffer_2IIIJJ
  (JNIEnv *env, jclass jcl, jobject buffer, jint bufferSize, jint handlerId, jint handle, jlong matchData, jlong matchMask) {
	mx_return_t rc;
	mx_segment_t buffer_desc[1];

	mx_request_t *request;
	
	if(handlerId != 1) {
		// not a valid handler
		// TODO multiple endpoint support 
		// TODO throw exception
		return;
	}
	/* retrieve the request handle */
	request = getRequest(handle);
	if(request == NULL) {
		//no valid handle
		//TODO throw exception
		return;
	}
	
	buffer_desc[0].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer);
	buffer_desc[0].segment_length = (uint32_t)bufferSize;

	rc = mx_irecv(myEndpoint, buffer_desc, 1, matchData, matchMask, NULL, request);
	if(rc != MX_SUCCESS) {
		//TODO throw exception
		fprintf(stderr, "error: %s\n", mx_strerror(rc));
		return;
	}
	return ;
}

/* wait() without timeout */
JNIEXPORT jint JNICALL Java_ibis_ipl_impl_mx_JavaMx_wait__II
  (JNIEnv *env, jclass jcl, jint handlerId, jint handle) {
	return Java_ibis_ipl_impl_mx_JavaMx_wait__IIJ(env, jcl, handlerId, handle, MX_INFINITE);
}

/* wait() with timeout */
JNIEXPORT jint JNICALL Java_ibis_ipl_impl_mx_JavaMx_wait__IIJ
  (JNIEnv *env, jclass jcl, jint handlerId, jint handle, jlong timeout) {
	mx_return_t rc;
	mx_status_t status;
	uint32_t result;

	mx_request_t *request;
	
	if(handlerId != 1) {
		// not a valid handler
		// TODO multiple endpoint support 
		// TODO throw exception
		return -1;
	}
	/* retrieve the request handle */
	request = getRequest(handle);
	if(request == NULL) {
		//no valid handle
		//TODO throw exception
		return -1;
	}
	
	rc = mx_wait(myEndpoint, request, (uint32_t)timeout, &status, &result);
	if(rc != MX_SUCCESS) {
		//TODO throw Exception
		fprintf(stderr, "error: %s\n", mx_strerror(rc));
		return -1;
	}
	if (result == 0) {
		/* recv error */
		fprintf(stderr, "wait error\n");
		return -1;
	}
	
	if(status.code != MX_STATUS_SUCCESS) {
		//TODO: check status code for timeout
		//TODO throw exception
		return -1;
	}
	
	return (jint)(status.xfer_length); // Note: MX documentation incorrect about status structure field names, see header files instead
}

/* test() */
JNIEXPORT jint JNICALL Java_ibis_ipl_impl_mx_JavaMx_test
  (JNIEnv *env, jclass jcl, jint handlerId, jint handle) {
	mx_return_t rc;
	mx_status_t status;
	uint32_t result;

	mx_request_t *request;
	
	if(handlerId != 1) {
		// not a valid handler
		// TODO multiple endpoint support 
		// TODO throw exception
		return -1;
	}
	/* retrieve the request handle */
	request = getRequest(handle);
	if(request == NULL) {
		//no valid handle
		//TODO throw exception
		return -1;
	}
	
	rc = mx_test(myEndpoint, request, &status, &result);
	if(rc != MX_SUCCESS) {
		//TODO throw Exception
		fprintf(stderr, "error: %s\n", mx_strerror(rc));
		return -1;
	}
	if (result == 0) {
		/* recv error */
		fprintf(stderr, "wait error\n");
		return -1;
	}
	
	if(status.code != MX_STATUS_SUCCESS) {
		//TODO: check status code for timeout
		//TODO throw exception
		return -1;
	}
	
	return (jint)(status.xfer_length); // Note: MX documentation incorrect about status structure field names, see header files instead
}

/* cancel() */
JNIEXPORT jboolean JNICALL Java_ibis_ipl_impl_mx_JavaMx_cancel
  (JNIEnv *env, jclass jcl, jint handlerId, jint handle) {
	mx_request_t *request;
	uint32_t result;
	if(handlerId != 1) {
		// not a valid handler
		// TODO multiple endpoint support 
		// TODO throw exception
		return JNI_FALSE;
	}
	/* retrieve the request handle */
	request = getRequest(handle);
	if(request == NULL) {
		//no valid handle
		//TODO throw exception
		return JNI_FALSE;
	}
	mx_cancel(myEndpoint, request, &result);
	if(result ==0) {
		return JNI_FALSE;
	}
	return JNI_TRUE;
}

/* wakeup() */
JNIEXPORT void JNICALL Java_ibis_ipl_impl_mx_JavaMx_wakeup
  (JNIEnv *env, jclass jcl, jint handlerId) {
	if(handlerId != 1) {
		// not a valid handler
		// TODO multiple endpoint support 
		// TODO throw exception
		return;
	}
	mx_wakeup(myEndpoint);
}

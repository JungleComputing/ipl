/* javamx.c */
#include "linkmanager.h"
#include "handlemanager.h"
#include "ibis_ipl_impl_mx_channels_JavaMx.h"

#include <stdlib.h>
#include "myriexpress.h"


#define MAX_ENDPOINTS 4
mx_endpoint_t endpoints[MAX_ENDPOINTS];   //[MX_MAX_ENDPOINTS];  //TODO use this
//mx_endpoint_t myEndpoint = NULL;
int initialized = 0;
jclass MxException;

void throwException(JNIEnv *env, const char *message) {
	(*env)->ThrowNew(env, MxException, message);
}

void printAddress(const mx_endpoint_addr_t* addr) {
	uint64_t nic;
	uint32_t endpoint;
	
	mx_decompose_endpoint_addr(*addr, &nic, &endpoint);
	//fprintf(stderr, "nic: %0" PRIx64 ", endpoint: %0" PRIx32 "\n", nic, endpoint);
}


/* init() */
JNIEXPORT jboolean JNICALL Java_ibis_ipl_impl_mx_channels_JavaMx_init
  (JNIEnv *env, jclass jcl) {
	mx_return_t rc;
	jclass localRef;

	if (initialized == 1) {
		//already initialized
		return JNI_TRUE;
	}
	
	// setup a global reference to the MxException class
	localRef = (*env)->FindClass(env, "ibis/ipl/impl/mx/channels/MxException");
	if (localRef == NULL) {
		// MxException class not found, break off initialization
		// Exception thrown by JNI
	    return JNI_FALSE;
	}
    MxException = (*env)->NewGlobalRef(env, localRef);
    (*env)->DeleteLocalRef(env, localRef);
    if (MxException == NULL) {
    	// JNI will also throw an exception here
        return JNI_FALSE;
    }
    
	// Attach an error handler that doesn't quit on errors automatically
	mx_set_error_handler(MX_ERRORS_RETURN);
	// now init the MX library
	rc = mx_init();
	if(rc != MX_SUCCESS) {
		throwException(env, mx_strerror(rc));
		return JNI_FALSE;
	}
	
	// setup endpoint structure
	int i;
	for (i = 0; i < MAX_ENDPOINTS; i++) {
		endpoints[i] = NULL;
	}
		
	initialized = 1;
	return JNI_TRUE;
}

/* deInit() */
JNIEXPORT jboolean JNICALL Java_ibis_ipl_impl_mx_channels_JavaMx_deInit
  (JNIEnv *env, jclass jcl) {
	//TODO check whether all endpoints are closed? Or close them ourselves? Or do nothing?
	
	/* Finalize the MX library */
	mx_finalize();
	(*env)->DeleteGlobalRef(env, MxException);
	initialized = 0;
	return JNI_TRUE;
}

/* newEndpoint() */
JNIEXPORT jint JNICALL Java_ibis_ipl_impl_mx_channels_JavaMx_newEndpoint
  (JNIEnv *env, jclass jcl, jint filter) {
	mx_return_t rc;
	int result;
	int i;
	result = -1;
	for (i = 0; i < MAX_ENDPOINTS; i++) {
		if(endpoints[i] == NULL) {
			result = i;
			break;
		}
	}
	if (result == -1) {
		throwException(env, "No free endpoints left");
		return (jint)result;
	}
	/* open an endpoint */
	rc = mx_open_endpoint(MX_ANY_NIC, MX_ANY_ENDPOINT, filter, 0, 0, &(endpoints[result]));
	if(rc != MX_SUCCESS) {
		endpoints[result] = NULL;
		throwException(env, mx_strerror(rc));
		return -1;
	}
	return (jint)result;
}

/* closeEndpoint() */
JNIEXPORT void JNICALL Java_ibis_ipl_impl_mx_channels_JavaMx_closeEndpoint
  (JNIEnv *env, jclass jcl, jint endpointId) {
	if(endpointId < 0 || endpointId >= MAX_ENDPOINTS || endpoints[endpointId] == NULL) {
		//unknown endpointId, so this one is not open...
		throwException(env, "Invalid endpoint");
		return;
	}
	
	/* close endpoint */
	mx_wakeup(endpoints[endpointId]); // notify all threads that are blocked on this endpoint
	mx_close_endpoint(endpoints[endpointId]);
	endpoints[endpointId] = NULL;
	return;
}

/* getMyNicId() */
JNIEXPORT jlong JNICALL Java_ibis_ipl_impl_mx_channels_JavaMx_getMyNicId
  (JNIEnv *env, jclass jcl, jint endpointId) {
	mx_endpoint_addr_t myAddr;
	uint64_t nicId;
	uint32_t epId;
	if(endpointId < 0 || endpointId >= MAX_ENDPOINTS || endpoints[endpointId] == NULL) {
		// not a valid endpoint
		throwException(env, "Invalid Endpoint");
		return -1;
	}
	mx_get_endpoint_addr(endpoints[endpointId], &myAddr);	
	mx_decompose_endpoint_addr(myAddr, &nicId, &epId);
	
	return nicId;
}

/* getMyEndpointId() */
JNIEXPORT jint JNICALL Java_ibis_ipl_impl_mx_channels_JavaMx_getMyEndpointId
  (JNIEnv *env, jclass jcl, jint endpointId) {
	mx_endpoint_addr_t myAddr;
	uint64_t nicId;
	uint32_t epId;
	
	if(endpointId < 0 || endpointId >= MAX_ENDPOINTS || endpoints[endpointId] == NULL) {
		// not a valid endpoint
		throwException(env, "Invalid Endpoint");
		return -1;
	}
	mx_get_endpoint_addr(endpoints[endpointId], &myAddr);	
	mx_decompose_endpoint_addr(myAddr, &nicId, &epId);
	
	return epId;
}

/* getNicId() */
JNIEXPORT jlong JNICALL Java_ibis_ipl_impl_mx_channels_JavaMx_getNicId
  (JNIEnv *env, jclass jcl, jstring name) {
	mx_return_t rc;
	uint64_t nicId;
	const char *hostname;
	hostname = (*env)->GetStringUTFChars(env, name, NULL);
	if (hostname == NULL) {
		throwException(env, "Host Not Found");
		return 0;
	}

	rc = mx_hostname_to_nic_id(hostname, &nicId); //compiler warning here, because *hostname is const
	(*env)->ReleaseStringUTFChars(env, name, hostname);
	if(rc != MX_SUCCESS) {
		throwException(env, mx_strerror(rc));		
		return 0;
	}	
	return nicId;
}

/* connect() without timeout */
JNIEXPORT jboolean JNICALL Java_ibis_ipl_impl_mx_channels_JavaMx_connect__IIJII
  (JNIEnv *env, jclass jcl, jint endpointId, jint link, jlong targetNicId, jint targetEndpointId, jint filter) {
	return Java_ibis_ipl_impl_mx_channels_JavaMx_connect__IIJIIJ(env, jcl, endpointId, link, targetNicId, targetEndpointId, filter, MX_INFINITE);
}

/* connect() with timeout */
JNIEXPORT jboolean JNICALL Java_ibis_ipl_impl_mx_channels_JavaMx_connect__IIJIIJ
  (JNIEnv *env, jclass jcl, jint endpointId, jint link, jlong targetNicId, jint targetEndpointId, jint filter, jlong timeout) {
	mx_return_t rc;
	mx_endpoint_addr_t *address;

	if(endpointId < 0 || endpointId >= MAX_ENDPOINTS || endpoints[endpointId] == NULL) {
		// not a valid endpoint
		throwException(env, "Invalid Endpoint");
		return JNI_FALSE;
	}

	/* retrieve the address of the link */
	address = getAddress(link);
	if(address == NULL) {
		//no valid link
		throwException(env, "Invalid Link");
		return JNI_FALSE;
	}
	//fprintf(stderr, "connecting to nic: %0" PRIx64 ", endpoint: %0" PRIx32 "\n", targetNicId, targetEndpointId);
	/* now connect to the client */
	rc = mx_connect(endpoints[endpointId], targetNicId, targetEndpointId, filter, (uint32_t)timeout, address); 
	if(rc != MX_SUCCESS) {
		throwException(env, mx_strerror(rc));
		return JNI_FALSE;
	}
	//fprintf(stderr, "connected to:\n");
	//printAddress(address);
	return JNI_TRUE;
}

/* disconnect() */
JNIEXPORT jboolean JNICALL Java_ibis_ipl_impl_mx_channels_JavaMx_disconnect
  (JNIEnv *env, jclass jcl, jint link) {
	// actually we could do nothing here, but let's check whether the link is present
	
	mx_endpoint_addr_t *address;
	/* retrieve the address of the link */
	address = getAddress(link);
	if(address == NULL) {
		//no valid link
		throwException(env, "Invalid Link");
		return JNI_FALSE;
	}
	return JNI_TRUE;
}

/* send() */
JNIEXPORT void JNICALL Java_ibis_ipl_impl_mx_channels_JavaMx_send__Ljava_nio_ByteBuffer_2IIIIIJ
  (JNIEnv *env, jclass jcl, jobject buffer, jint offset, jint bufferSize, jint endpointId, jint link, jint handle, jlong matchData) {
	mx_return_t rc;
	mx_segment_t bufferDesc[1];
	mx_request_t *request;
	mx_endpoint_addr_t *target;
	
	if(endpointId < 0 || endpointId >= MAX_ENDPOINTS || endpoints[endpointId] == NULL) {
		// not a valid endpoint
		throwException(env, "Invalid Endpoint");
		return;
	}
	
	/* retrieve the target address */
	target = getAddress(link);
	if(target == NULL) {
		//no valid link
		throwException(env, "Invalid Link");
		return;
	}
	//fprintf(stderr, "send() to:\n");
	//printAddress(target);
	//fprintf(stderr, "MatchData: %0" PRIx64 "\n", matchData);
	
	/* retrieve the request handle */
	request = getRequest(handle);
	if(request == NULL) {
		//no valid handle
		throwException(env, "Invalid Handle");
		return;
	}
	
	if(bufferSize == 0) {
		rc = mx_isend(endpoints[endpointId], NULL, 0, *target, matchData, NULL, request);
	} else {
		bufferDesc[0].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer) + offset;
		bufferDesc[0].segment_length = (uint32_t)bufferSize;
		rc = mx_isend(endpoints[endpointId], bufferDesc, 1, *target, matchData, NULL, request);
	}
	if(rc != MX_SUCCESS) {
		throwException(env, mx_strerror(rc));
		return;
	}
	return;
}

/* sendSynchronous() */
JNIEXPORT void JNICALL Java_ibis_ipl_impl_mx_channels_JavaMx_sendSynchronous__Ljava_nio_ByteBuffer_2IIIIIJ
  (JNIEnv *env, jclass jcl, jobject buffer, jint offset, jint bufferSize, jint endpointId, jint link, jint handle, jlong matchData) {
	mx_return_t rc;
	mx_segment_t bufferDesc[1];
	mx_request_t *request;
	mx_endpoint_addr_t *target;
	
	if(endpointId < 0 || endpointId >= MAX_ENDPOINTS || endpoints[endpointId] == NULL) {
		// not a valid endpoint
		throwException(env, "Invalid Endpoint");
		return;
	}
	
	/* retrieve the target address */
	target = getAddress(link);
	if(target == NULL) {
		//no valid link
		throwException(env, "Invalid Link");
		return;
	}
	
	//fprintf(stderr, "sendSync() to:\n");
	//printAddress(target);
	//fprintf(stderr, "MatchData: %0" PRIx64 "\n", matchData);
	
	/* retrieve the request handle */
	request = getRequest(handle);
	if(request == NULL) {
		//no valid handle
		throwException(env, "Invalid Handle");
		return;
	}
	//fprintf(stderr, "JavaMx::sendSynchronous: to handle %d\n", handle);
	/* TODO: check if target is valid */
	if(bufferSize == 0) {
			rc = mx_issend(endpoints[endpointId], NULL, 0, *target, matchData, NULL, request);
	} else {
		bufferDesc[0].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer) + offset;
		bufferDesc[0].segment_length = (uint32_t)bufferSize;
		rc = mx_issend(endpoints[endpointId], bufferDesc, 1, *target, matchData, NULL, request);
	}
	
	if(rc != MX_SUCCESS) {
		throwException(env, mx_strerror(rc));
		return;
	}
	return;
}



/* send() for SendBuffers */
JNIEXPORT void JNICALL Java_ibis_ipl_impl_mx_channels_JavaMx_send__Ljava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2IIIIJ
  (JNIEnv *env, jclass jcl, 
		  jobject buffer1, jint bufferSize1,
		  jobject buffer2, jint bufferSize2,
		  jobject buffer3, jint bufferSize3,
		  jobject buffer4, jint bufferSize4,
		  jobject buffer5, jint bufferSize5,
		  jobject buffer6, jint bufferSize6,
		  jobject buffer7, jint bufferSize7,
		  jobject buffer8, jint bufferSize8,
		  jobject buffer9, jint bufferSize9,
		  jint endpointId, jint link, jint handle, jlong matchData
		  ) {
	mx_return_t rc;
	mx_segment_t bufferDesc[9];
	mx_request_t *request;
	mx_endpoint_addr_t *target;
		
	if(endpointId < 0 || endpointId >= MAX_ENDPOINTS || endpoints[endpointId] == NULL) {
		// not a valid endpoint
		throwException(env, "Invalid Endpoint");
		return;
	}
	/* retrieve the target address */
	target = getAddress(link);
	if(target == NULL) {
		//no valid link
		throwException(env, "Invalid Link");
		return;
	}
	
	/* retrieve the request handle */
	request = getRequest(handle);
	if(request == NULL) {
		//no valid handle
		throwException(env, "Invalid Handle");
		return;
	}
	
	bufferDesc[0].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer1);
	bufferDesc[0].segment_length = (uint32_t)bufferSize1;
	bufferDesc[1].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer2);
	bufferDesc[1].segment_length = (uint32_t)bufferSize2;
	bufferDesc[2].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer3);
	bufferDesc[2].segment_length = (uint32_t)bufferSize3;
	bufferDesc[3].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer4);
	bufferDesc[3].segment_length = (uint32_t)bufferSize4;
	bufferDesc[4].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer5);
	bufferDesc[4].segment_length = (uint32_t)bufferSize5;
	bufferDesc[5].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer6);
	bufferDesc[5].segment_length = (uint32_t)bufferSize6;
	bufferDesc[6].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer7);
	bufferDesc[6].segment_length = (uint32_t)bufferSize7;
	bufferDesc[7].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer8);
	bufferDesc[7].segment_length = (uint32_t)bufferSize8;
	bufferDesc[8].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer9);
	bufferDesc[8].segment_length = (uint32_t)bufferSize9;
	
	rc = mx_isend(endpoints[endpointId], bufferDesc, 9, *target, matchData, NULL, request);
	if(rc != MX_SUCCESS) {
		throwException(env, mx_strerror(rc));
		return;
	}
	return;
}
 
/* sendSynchronous() for SendBuffers */
JNIEXPORT void JNICALL Java_ibis_ipl_impl_mx_channels_JavaMx_sendSynchronous__Ljava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2IIIIJ
  (JNIEnv *env, jclass jcl, 
		  jobject buffer1, jint bufferSize1,
		  jobject buffer2, jint bufferSize2,
		  jobject buffer3, jint bufferSize3,
		  jobject buffer4, jint bufferSize4,
		  jobject buffer5, jint bufferSize5,
		  jobject buffer6, jint bufferSize6,
		  jobject buffer7, jint bufferSize7,
		  jobject buffer8, jint bufferSize8,
		  jobject buffer9, jint bufferSize9,
		  jint endpointId, jint link, jint handle, jlong matchData
		  ) {
	mx_return_t rc;
	mx_segment_t bufferDesc[9];
	mx_request_t *request;
	mx_endpoint_addr_t *target;
	
	if(endpointId < 0 || endpointId >= MAX_ENDPOINTS || endpoints[endpointId] == NULL) {
		// not a valid endpoint
		throwException(env, "Invalid Endpoint");
		return;
	}
	/* retrieve the target address */
	target = getAddress(link);
	if(target == NULL) {
		//no valid link
		throwException(env, "Invalid Link");
		return;
	}
	
	/* retrieve the request handle */
	request = getRequest(handle);
	if(request == NULL) {
		//no valid handle
		throwException(env, "Invalid Handle");
		return;
	}
	
	bufferDesc[0].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer1);
	bufferDesc[0].segment_length = (uint32_t)bufferSize1;
	bufferDesc[1].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer2);
	bufferDesc[1].segment_length = (uint32_t)bufferSize2;
	bufferDesc[2].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer3);
	bufferDesc[2].segment_length = (uint32_t)bufferSize3;
	bufferDesc[3].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer4);
	bufferDesc[3].segment_length = (uint32_t)bufferSize4;
	bufferDesc[4].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer5);
	bufferDesc[4].segment_length = (uint32_t)bufferSize5;
	bufferDesc[5].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer6);
	bufferDesc[5].segment_length = (uint32_t)bufferSize6;
	bufferDesc[6].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer7);
	bufferDesc[6].segment_length = (uint32_t)bufferSize7;
	bufferDesc[7].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer8);
	bufferDesc[7].segment_length = (uint32_t)bufferSize8;
	bufferDesc[8].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer9);
	bufferDesc[8].segment_length = (uint32_t)bufferSize9;
	
	rc = mx_isend(endpoints[endpointId], bufferDesc, 9, *target, matchData, NULL, request);
	if(rc != MX_SUCCESS) {
		throwException(env, mx_strerror(rc));
		return;
	}
	return;
}



/* send() for SendBuffers, no header */
JNIEXPORT void JNICALL Java_ibis_ipl_impl_mx_channels_JavaMx_send__Ljava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2IIIIJ
  (JNIEnv *env, jclass jcl, 
		  jobject buffer1, jint bufferSize1,
		  jobject buffer2, jint bufferSize2,
		  jobject buffer3, jint bufferSize3,
		  jobject buffer4, jint bufferSize4,
		  jobject buffer5, jint bufferSize5,
		  jobject buffer6, jint bufferSize6,
		  jobject buffer7, jint bufferSize7,
		  jobject buffer8, jint bufferSize8,
		  jint endpointId, jint link, jint handle, jlong matchData
		  ) {
	mx_return_t rc;
	mx_segment_t bufferDesc[8];
	mx_request_t *request;
	mx_endpoint_addr_t *target;
		
	if(endpointId < 0 || endpointId >= MAX_ENDPOINTS || endpoints[endpointId] == NULL) {
		// not a valid endpoint
		throwException(env, "Invalid Endpoint");
		return;
	}
	/* retrieve the target address */
	target = getAddress(link);
	if(target == NULL) {
		//no valid link
		throwException(env, "Invalid Link");
		return;
	}
	
	/* retrieve the request handle */
	request = getRequest(handle);
	if(request == NULL) {
		//no valid handle
		throwException(env, "Invalid Handle");
		return;
	}
	
	bufferDesc[0].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer1);
	bufferDesc[0].segment_length = (uint32_t)bufferSize1;
	bufferDesc[1].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer2);
	bufferDesc[1].segment_length = (uint32_t)bufferSize2;
	bufferDesc[2].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer3);
	bufferDesc[2].segment_length = (uint32_t)bufferSize3;
	bufferDesc[3].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer4);
	bufferDesc[3].segment_length = (uint32_t)bufferSize4;
	bufferDesc[4].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer5);
	bufferDesc[4].segment_length = (uint32_t)bufferSize5;
	bufferDesc[5].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer6);
	bufferDesc[5].segment_length = (uint32_t)bufferSize6;
	bufferDesc[6].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer7);
	bufferDesc[6].segment_length = (uint32_t)bufferSize7;
	bufferDesc[7].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer8);
	bufferDesc[7].segment_length = (uint32_t)bufferSize8;

	
	rc = mx_isend(endpoints[endpointId], bufferDesc, 8, *target, matchData, NULL, request);
	if(rc != MX_SUCCESS) {
		throwException(env, mx_strerror(rc));
		return;
	}
	return;
}
 
/* sendSynchronous() for SendBuffers, no header */
JNIEXPORT void JNICALL Java_ibis_ipl_impl_mx_channels_JavaMx_sendSynchronous__Ljava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2IIIIJ
  (JNIEnv *env, jclass jcl, 
		  jobject buffer1, jint bufferSize1,
		  jobject buffer2, jint bufferSize2,
		  jobject buffer3, jint bufferSize3,
		  jobject buffer4, jint bufferSize4,
		  jobject buffer5, jint bufferSize5,
		  jobject buffer6, jint bufferSize6,
		  jobject buffer7, jint bufferSize7,
		  jobject buffer8, jint bufferSize8,
		  jint endpointId, jint link, jint handle, jlong matchData
		  ) {
	mx_return_t rc;
	mx_segment_t bufferDesc[8];
	mx_request_t *request;
	mx_endpoint_addr_t *target;
	
	if(endpointId < 0 || endpointId >= MAX_ENDPOINTS || endpoints[endpointId] == NULL) {
		// not a valid endpoint
		throwException(env, "Invalid Endpoint");
		return;
	}
	/* retrieve the target address */
	target = getAddress(link);
	if(target == NULL) {
		//no valid link
		throwException(env, "Invalid Link");
		return;
	}
	
	/* retrieve the request handle */
	request = getRequest(handle);
	if(request == NULL) {
		//no valid handle
		throwException(env, "Invalid Handle");
		return;
	}
	
	bufferDesc[0].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer1);
	bufferDesc[0].segment_length = (uint32_t)bufferSize1;
	bufferDesc[1].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer2);
	bufferDesc[1].segment_length = (uint32_t)bufferSize2;
	bufferDesc[2].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer3);
	bufferDesc[2].segment_length = (uint32_t)bufferSize3;
	bufferDesc[3].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer4);
	bufferDesc[3].segment_length = (uint32_t)bufferSize4;
	bufferDesc[4].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer5);
	bufferDesc[4].segment_length = (uint32_t)bufferSize5;
	bufferDesc[5].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer6);
	bufferDesc[5].segment_length = (uint32_t)bufferSize6;
	bufferDesc[6].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer7);
	bufferDesc[6].segment_length = (uint32_t)bufferSize7;
	bufferDesc[7].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer8);
	bufferDesc[7].segment_length = (uint32_t)bufferSize8;
	
	rc = mx_isend(endpoints[endpointId], bufferDesc, 8, *target, matchData, NULL, request);
	if(rc != MX_SUCCESS) {
		throwException(env, mx_strerror(rc));
		return;
	}
	return;
}





/* recv() without mask */
JNIEXPORT void JNICALL Java_ibis_ipl_impl_mx_channels_JavaMx_recv__Ljava_nio_ByteBuffer_2IIIIJ
  (JNIEnv *env, jclass jcl, jobject buffer, jint offset, jint bufferSize, jint endpointId, jint handle, jlong matchData) {
	Java_ibis_ipl_impl_mx_channels_JavaMx_recv__Ljava_nio_ByteBuffer_2IIIIJJ(env, jcl, buffer, offset, bufferSize, endpointId, handle, matchData, MX_MATCH_MASK_NONE);
}

/* recv() with mask */
JNIEXPORT void JNICALL Java_ibis_ipl_impl_mx_channels_JavaMx_recv__Ljava_nio_ByteBuffer_2IIIIJJ
  (JNIEnv *env, jclass jcl, jobject buffer, jint offset, jint bufferSize, jint endpointId, jint handle, jlong matchData, jlong matchMask) {
	mx_return_t rc;
	mx_segment_t buffer_desc[1];

	mx_request_t *request;

	if(endpointId < 0 || endpointId >= MAX_ENDPOINTS || endpoints[endpointId] == NULL) {
		// not a valid endpoint
		throwException(env, "Invalid Endpoint");
		return;
	}

	/* retrieve the request handle */
	request = getRequest(handle);
	if(request == NULL) {
		//no valid handle
		throwException(env, "Invalid Handle");
		return;
	}
	
	if (bufferSize == 0) {
		rc = mx_irecv(endpoints[endpointId], NULL, 0, matchData, matchMask, NULL, request);		
	} else {
		buffer_desc[0].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer) + offset;
		buffer_desc[0].segment_length = (uint32_t)bufferSize;
		rc = mx_irecv(endpoints[endpointId], buffer_desc, 1, matchData, matchMask, NULL, request);
	}	

	if(rc != MX_SUCCESS) {
		throwException(env, mx_strerror(rc));
		return;
	}
	return ;
}

/* recv() 2 buffers without mask */
JNIEXPORT void JNICALL Java_ibis_ipl_impl_mx_channels_JavaMx_recv__Ljava_nio_ByteBuffer_2IILjava_nio_ByteBuffer_2IIIIJ
  (JNIEnv *env, jclass jcl, jobject buffer, jint offset, jint bufferSize, jobject buffer2, jint offset2, jint bufferSize2, jint endpointId, jint handle, jlong matchData) {
	Java_ibis_ipl_impl_mx_channels_JavaMx_recv__Ljava_nio_ByteBuffer_2IILjava_nio_ByteBuffer_2IIIIJJ
		(env, jcl, buffer, offset, bufferSize, buffer2, offset2, bufferSize2, endpointId, handle, matchData, MX_MATCH_MASK_NONE);
}

/* recv() 2 buffers with mask */
JNIEXPORT void JNICALL Java_ibis_ipl_impl_mx_channels_JavaMx_recv__Ljava_nio_ByteBuffer_2IILjava_nio_ByteBuffer_2IIIIJJ
  (JNIEnv *env, jclass jcl, jobject buffer, jint offset, jint bufferSize, jobject buffer2, jint offset2, jint bufferSize2, jint endpointId, jint handle, jlong matchData, jlong matchMask) {
	mx_return_t rc;
	mx_segment_t buffer_desc[2];

	mx_request_t *request;

	if(endpointId < 0 || endpointId >= MAX_ENDPOINTS || endpoints[endpointId] == NULL) {
		// not a valid endpoint
		throwException(env, "Invalid Endpoint");
		return;
	}

	/* retrieve the request handle */
	request = getRequest(handle);
	if(request == NULL) {
		//no valid handle
		throwException(env, "Invalid Handle");
		return;
	}
	
	buffer_desc[0].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer) + offset;
	buffer_desc[0].segment_length = (uint32_t)bufferSize;
	buffer_desc[1].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer2) + offset2;
	buffer_desc[1].segment_length = (uint32_t)bufferSize2;

	//fprintf(stderr, "Recv:: MatchData: %0" PRIx64 ", MatchMask: %0" PRIx64 "\n", matchData, matchMask);
	rc = mx_irecv(endpoints[endpointId], buffer_desc, 2, matchData, matchMask, NULL, request);
	if(rc != MX_SUCCESS) {
		throwException(env, mx_strerror(rc));
		return;
	}
	return ;
}



/* wait() without timeout */
JNIEXPORT jint JNICALL Java_ibis_ipl_impl_mx_channels_JavaMx_wait__II
  (JNIEnv *env, jclass jcl, jint endpointId, jint handle) {
	return Java_ibis_ipl_impl_mx_channels_JavaMx_wait__IIJ(env, jcl, endpointId, handle, MX_INFINITE);
}

/* wait() with timeout */
JNIEXPORT jint JNICALL Java_ibis_ipl_impl_mx_channels_JavaMx_wait__IIJ
  (JNIEnv *env, jclass jcl, jint endpointId, jint handle, jlong timeout) {
	mx_return_t rc;
	mx_status_t status;
	uint32_t result;

	mx_request_t *request;
	
	if(endpointId < 0 || endpointId >= MAX_ENDPOINTS || endpoints[endpointId] == NULL) {
		// not a valid endpoint
		throwException(env, "Invalid Endpoint");
		return -1;
	}
	/* retrieve the request handle */
	request = getRequest(handle);
	if(request == NULL) {
		//no valid handle
		throwException(env, "Invalid Handle");
		return -1;
	}
	
	//fprintf(stderr, "JavaMx::wait: waiting for handle %d\n", handle);
	rc = mx_wait(endpoints[endpointId], request, (uint32_t)timeout, &status, &result);
	if(rc != MX_SUCCESS) {
		throwException(env, mx_strerror(rc));
		return -1;
	}
	if (result == 0) {
		// Request is not finished yet
		return -1;
	}
	
	// TODO this block of code doesn't look coorrect to me
	if(status.code != MX_STATUS_SUCCESS) {
		// some kind of error occured
		throwException(env, mx_strstatus(status.code));
		return -1;
	}
	
	return (jint)(status.xfer_length); // Note: MX documentation incorrect about status structure field names, see header files instead
}

/* test() */
JNIEXPORT jint JNICALL Java_ibis_ipl_impl_mx_channels_JavaMx_test
  (JNIEnv *env, jclass jcl, jint endpointId, jint handle) {
	mx_return_t rc;
	mx_status_t status;
	uint32_t result;

	mx_request_t *request;
	
	if(endpointId < 0 || endpointId >= MAX_ENDPOINTS || endpoints[endpointId] == NULL) {
		// not a valid endpoint
		throwException(env, "Invalid Endpoint");
		return -1;
	}
	
	/* retrieve the request handle */
	request = getRequest(handle);
	if(request == NULL) {
		//no valid handle
		throwException(env, "Invalid Handle");
		return -1;
	}
	
	rc = mx_test(endpoints[endpointId], request, &status, &result);
	if(rc != MX_SUCCESS) {
		throwException(env, mx_strerror(rc));
		return -1;
	}
	if (result == 0) {
		// Request is not finished yet
		return -1;
	}
	if(status.code != MX_STATUS_SUCCESS) {
		throwException(env, mx_strstatus(rc));
		return -1;
	}
	
	return (jint)(status.xfer_length); // Note: MX documentation incorrect about status structure field names, see header files instead
}

/* iprobe() */
JNIEXPORT jint JNICALL Java_ibis_ipl_impl_mx_channels_JavaMx_iprobe
  (JNIEnv *env, jclass jcl, jint endpointId, jlong matchData, jlong matchMask) {
	mx_status_t status;
	uint32_t result;
	mx_return_t ret;
	if(endpointId < 0 || endpointId >= MAX_ENDPOINTS || endpoints[endpointId] == NULL) {
		// not a valid endpoint
		throwException(env, "Invalid Endpoint");
		return -1;
	}
	/* retrieve the request handle */
	ret = mx_iprobe(endpoints[endpointId], matchData, matchMask, &status, &result);
	if(ret != MX_SUCCESS) {
			//fprintf(stderr, "JavaMx::iprobe: failed!: %s\n", mx_strerror(ret));
			throwException(env, mx_strerror(ret));
			return -1;
		}
	if(result == 0) {
		// no message available
		//fprintf(stderr, "JavaMx::iprobe: no message: %s\n", mx_strstatus(status.code));
		return -1;
	}
	//fprintf(stderr, "JavaMx::iprobe: message: %s\n", mx_strstatus(status.code));
	//fprintf(stderr, "JavaMx::iprobe: retval: %" PRId32 "\n", status.xfer_length);
	return (jint)(status.xfer_length);
}

/* probe() */
JNIEXPORT jint JNICALL Java_ibis_ipl_impl_mx_channels_JavaMx_probe
  (JNIEnv *env, jclass jcl, jint endpointId, jlong timeout, jlong matchData, jlong matchMask) {
	
	mx_status_t status;
	uint32_t result;
	mx_return_t ret;
	
	if(endpointId < 0 || endpointId >= MAX_ENDPOINTS || endpoints[endpointId] == NULL) {
		// not a valid endpoint
		throwException(env, "Invalid Endpoint");
		return -1;
	}

	/* retrieve the request handle */
	ret = mx_probe(endpoints[endpointId], (uint32_t)timeout, matchData, matchMask, &status, &result);
	if(ret != MX_SUCCESS) {
		//fprintf(stderr, "JavaMx::probe: failed!: %s\n", mx_strerror(ret));
		throwException(env, mx_strerror(ret));
	}
	if(result == 0) {
		// no message available
		//fprintf(stderr, "JavaMx::probe: no message: %s\n", mx_strstatus(status.code));
		return -1;
	}
	//fprintf(stderr, "JavaMx::probe: message: %s\n", mx_strstatus(status.code));
	//fprintf(stderr, "JavaMx::probe: retval: %" PRId32 "\n", status.xfer_length);
	return (jint)(status.xfer_length);
}


/* cancel() */
JNIEXPORT jboolean JNICALL Java_ibis_ipl_impl_mx_channels_JavaMx_cancel
  (JNIEnv *env, jclass jcl, jint endpointId, jint handle) {
	mx_request_t *request;
	uint32_t result;
	if(endpointId < 0 || endpointId >= MAX_ENDPOINTS || endpoints[endpointId] == NULL) {
		// not a valid endpoint
		throwException(env, "Invalid Endpoint");
		return JNI_FALSE;
	}
	
	/* retrieve the request handle */
	request = getRequest(handle);
	if(request == NULL) {
		//no valid handle
		throwException(env, "Invalid Handle");
		return JNI_FALSE;
	}
	//fprintf(stderr, "invoking mx_cancel()...\n");
	mx_cancel(endpoints[endpointId], request, &result);
	if(result == 0) {
		// request not canceled, but already delivered
		return JNI_FALSE;
	}
	return JNI_TRUE;
}

/* wakeup() */
JNIEXPORT void JNICALL Java_ibis_ipl_impl_mx_channels_JavaMx_wakeup
  (JNIEnv *env, jclass jcl, jint endpointId) {
	if(endpointId < 0 || endpointId >= MAX_ENDPOINTS || endpoints[endpointId] == NULL) {
		// not a valid endpoint
		throwException(env, "Invalid Endpoint");
		return;
	}
	mx_wakeup(endpoints[endpointId]);
}

/* waitForMessage() */
JNIEXPORT jlong JNICALL Java_ibis_ipl_impl_mx_channels_JavaMx_waitForMessage
  (JNIEnv *env, jclass jcl, jint endpointId, jlong timeout, jlong matchData, jlong matchMask) {
	mx_status_t status;
	uint32_t result;
	//uint32_t timeout = MX_INFINITE; // no timeout
	if(endpointId < 0 || endpointId >= MAX_ENDPOINTS || endpoints[endpointId] == NULL) {
		// not a valid endpoint
		throwException(env, "Invalid Endpoint");
		return 0;
	}
	
	/* retrieve the request handle */
	mx_probe(endpoints[endpointId], (uint32_t)timeout, matchData, matchMask, &status, &result);
	if(result == 0) {
		// no message available
		return 0;
	}
	return status.match_info;
}

/* pollForMessage() */
JNIEXPORT jlong JNICALL Java_ibis_ipl_impl_mx_channels_JavaMx_pollForMessage
  (JNIEnv *env, jclass jcl, jint endpointId, jlong matchData, jlong matchMask) {
	mx_status_t status;
	uint32_t result;
	// uint32_t timeout = 10000; // no timeout
	if(endpointId < 0 || endpointId >= MAX_ENDPOINTS || endpoints[endpointId] == NULL) {
		// not a valid endpoint
		throwException(env, "Invalid Endpoint");
		return 0;
	}
	
	/* retrieve the request handle */
	mx_iprobe(endpoints[endpointId], matchData, matchMask, &status, &result);
	if(result == 0) {
		// no message available
		return 0;
	}
	return status.match_info;
}

/* select()  */
JNIEXPORT jint JNICALL Java_ibis_ipl_impl_mx_channels_JavaMx_select
  (JNIEnv *env, jclass jcl, jint endpointId, jlong timeout, jlong matchData, jlong matchMask, jobject matchedConnection) {
	mx_return_t rc;
	mx_status_t status;
	uint32_t result;

	
	jlong *matchedConn = (*env)->GetDirectBufferAddress(env, matchedConnection);
	
	if(endpointId < 0 || endpointId >= MAX_ENDPOINTS || endpoints[endpointId] == NULL) {
		// not a valid endpoint
		throwException(env, "Invalid Endpoint");
		return -1;
	}

	
	//fprintf(stderr, "JavaMx::wait: waiting for handle %d\n", handle);
	rc = mx_wait_any(endpoints[endpointId], (uint32_t)timeout, matchData, matchMask, &status, &result);
	if(rc != MX_SUCCESS) {
		throwException(env, mx_strerror(rc));
		return -1;
	}
	if (result == 0) {
		// Request is not finished yet
		return -1;
	}
	
	// TODO this block of code doesn't look coorrect to me
	if(status.code != MX_STATUS_SUCCESS) {
		// some kind of error occured
		throwException(env, mx_strstatus(status.code));
		return -1;
	}
	
	*matchedConn = status.match_info;
	return (jint)(status.xfer_length); // Note: MX documentation incorrect about status structure field names, see header files instead
	  
}



/* javamx.c */

#include "ibis_ipl_impl_mx_io_JavaMx.h"
#include "myriexpress.h"

uint64_t match_mask = MX_MATCH_MASK_NONE;
uint32_t filter = 0xdada0123;

#define MAX_LINKS 128
#define MAX_HANDLES 512

mx_endpoint_t my_endpoint;

jint tidBlockSize;
mx_endpoint_addr_t **targetIds;

jint tidBlockSize;
mx_endpoint_addr_t **handles;


/* JNI methods */
/*
 * Class:     ibis_ipl_impl_mx_io_JavaMx
 * Method:    jmx_init
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_ibis_ipl_impl_mx_io_JavaMx_jmx_1init
  (JNIEnv *env, jclass jcl) {
	/* Initialize the MX library */
	mx_return_t rc;
	jint i;

	/* Attach an error handler */
	mx_set_error_handler(MX_ERRORS_RETURN);

	/* now init the MX library */
	rc = mx_init();
	if(rc != MX_SUCCESS) {
		fprintf(stderr, "error: %s\n", mx_strerror(rc));
		return JNI_FALSE;
	}
	
	return JNI_TRUE;
}

/*
 * Class:     ibis_ipl_impl_mx_io_JavaMx
 * Method:    jmx_finalize
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_ibis_ipl_impl_mx_io_JavaMx_jmx_1finalize
  (JNIEnv *env, jclass jcl) {
	mx_return_t rc;
	/* close endpoint */
	rc = mx_close_endpoint(my_endpoint);
	if(rc != MX_SUCCESS) {
		fprintf(stderr, "error: %s\n", mx_strerror(rc));
		return JNI_FALSE;
	}
	/* Finalize the MX library */
	rc = mx_finalize();
	if(rc != MX_SUCCESS) {
		fprintf(stderr, "error: %s\n", mx_strerror(rc));
		return JNI_FALSE;
	}
	return JNI_TRUE;
}


/*
 * Class:     ibis_ipl_impl_mx_io_JavaMx
 * Method:    jmx_openEndpoint
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_ibis_ipl_impl_mx_io_JavaMx_jmx_1openEndpoint
  (JNIEnv *env, jclass jcl) {
	/* open an endpoint */
	rc = mx_open_endpoint(MX_ANY_NIC, MX_ANY_ENDPOINT, filter, 0, 0, &my_endpoint);
	if(rc != MX_SUCCESS) {
		fprintf(stderr, "error: %s\n", mx_strerror(rc));
		return JNI_FALSE;
	}
}


/*
 * Class:     ibis_ipl_impl_mx_io_JavaMx
 * Method:    jmx_getMyNicId
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_ibis_ipl_impl_mx_io_JavaMx_jmx_1getMyNicId
  (JNIEnv *env, jclass jcl) {
	mx_return_t rc;
	mx_endpoint_addr_t my_addr;
	uint64_t nic_id;
	uint32_t ep_id;
	rc = mx_get_endpoint_addr(my_endpoint, &my_addr);
	if(rc != MX_SUCCESS) {
		fprintf(stderr, "error: %s\n", mx_strerror(rc));
		return rc;
	}	
	rc = mx_decompose_endpoint_addr(my_addr, &nic_id, &ep_id);
	if(rc != MX_SUCCESS) {
		fprintf(stderr, "error: %s\n", mx_strerror(rc));
		return rc;
	}
	return nic_id;
}

/*
 * Class:     ibis_ipl_impl_mx_io_JavaMx
 * Method:    jmx_getMyEndpointId
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_ibis_ipl_impl_mx_io_JavaMx_jmx_1getMyEndpointId
  (JNIEnv *env, jclass jcl) {
	mx_return_t rc;
	mx_endpoint_addr_t my_addr;
	uint64_t nic_id;
	uint32_t ep_id;
	rc = mx_get_endpoint_addr(my_endpoint, &my_addr);
	if(rc != MX_SUCCESS) {
		fprintf(stderr, "error: %s\n", mx_strerror(rc));
		return rc;
	}	
	rc = mx_decompose_endpoint_addr(my_addr, &nic_id, &ep_id);
	if(rc != MX_SUCCESS) {
		fprintf(stderr, "error: %s\n", mx_strerror(rc));
		return rc;
	}
	return ep_id;
}

/*
 * Class:     ibis_ipl_impl_mx_io_JavaMx
 * Method:    jmx_getNicId
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_ibis_ipl_impl_mx_io_JavaMx_jmx_1getNicId
  (JNIEnv *env, jclass jcl , jstring name) {
	mx_return_t rc;
	uint64_t nic_id;
	const char *hostname;
	hostname = (*env)->GetStringUTFChars(env, name, NULL);
	if (hostname == NULL) {
		return 0; /* OutOfMemoryError already thrown */
	}

	rc = mx_hostname_to_nic_id(hostname, &nic_id);
	if(rc != MX_SUCCESS) {
		fprintf(stderr, "error: %s\n", mx_strerror(rc));
		return 0;
	}	
	(*env)->ReleaseStringUTFChars(env, name, hostname);
	return nic_id;
}

/*
 * Class:     ibis_ipl_impl_mx_io_JavaMx
 * Method:    jmx_connect
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_ibis_ipl_impl_mx_io_JavaMx_jmx_1connect
  (JNIEnv *env, jclass jcl, jlong nic_id, jint endpoint_id) {
	jint link;
	mx_return_t rc;
	/* get a link number from the free_links_list*/
	link = free_sendlinks_list;
	if (link < 0) {
		// no free link available
		fprintf(stderr, "error: out of link resources\n");
		return -1;
	}
	free_sendlinks_list = sendlink[link].next_free;
	sendlinks_in_use++;
	
	/* now connect to the client */
	rc = mx_connect(my_endpoint, nic_id, endpoint_id, filter, MX_INFINITE, &(sendlink[link].address)); 
	if(rc != MX_SUCCESS) {
		fprintf(stderr, "error: %s\n", mx_strerror(rc));
		return -1;
	}
	
	return link;
}

/*
 * Class:     ibis_ipl_impl_mx_io_JavaMx
 * Method:    jmx_disconnect
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_ibis_ipl_impl_mx_io_JavaMx_jmx_1disconnect
  (JNIEnv *env, jclass jcl, jint link) {
	/* TODO: check if link is indeed in use */	
	sendlink[link].next_free = free_sendlinks_list;
	free_sendlinks_list = link;
	sendlinks_in_use--;
	
	return JNI_TRUE;
}

/*
 * Class:     ibis_ipl_impl_mx_io_JavaMx
 * Method:    jmx_send
 * Signature: (Ljava/nio/ByteBuffer;IJ)I
 */
JNIEXPORT jint JNICALL Java_ibis_ipl_impl_mx_io_JavaMx_jmx_1send
  (JNIEnv *env, jclass jcl, jobject buffer, jint target, jlong match_send) {
	mx_return_t rc;
	mx_segment_t buffer_desc[1];
	jint handle;
	mx_request_t *req_handle;
	
	handle = gethandle();
	if (handle < 0) {
		// no free link available
		fprintf(stderr, "error: out of handles\n");
		return -1;
	}
	req_handle = &(handle_list[handle].request);
	
	/* TODO: check if target is valid */
	buffer_desc[0].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer);
	buffer_desc[0].segment_length = (*env)->GetDirectBufferCapacity(env, buffer);
	
	rc = mx_isend(my_endpoint, buffer_desc, 1, sendlink[target].address, match_send, NULL, req_handle);
	if(rc != MX_SUCCESS) {
		fprintf(stderr, "error: %s\n", mx_strerror(rc));
		releasehandle(handle);
		return -1;
	}
	return handle;
}

/*
 * Class:     ibis_ipl_impl_mx_io_JavaMx
 * Method:    jmx_sendSynchronous
 * Signature: (Ljava/nio/ByteBuffer;IJ)I
 */
JNIEXPORT jint JNICALL Java_ibis_ipl_impl_mx_io_JavaMx_jmx_1sendSynchronous
  (JNIEnv *env, jclass jcl, jobject buffer, jint target, jlong match_send) {
	mx_return_t rc;
	mx_segment_t buffer_desc[1];
	jint handle;
	mx_request_t *req_handle;
	
	handle = gethandle();
	if (handle < 0) {
		// no free link available
		fprintf(stderr, "error: out of handles\n");
		return -1;
	}
	req_handle = &(handle_list[handle].request);
	
	/* TODO: check if target is valid */
	buffer_desc[0].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer);
	buffer_desc[0].segment_length = (*env)->GetDirectBufferCapacity(env, buffer);
	
	rc = mx_issend(my_endpoint, buffer_desc, 1, sendlink[target].address, match_send, NULL, req_handle);
	if(rc != MX_SUCCESS) {
		fprintf(stderr, "error: %s\n", mx_strerror(rc));
		releasehandle(handle);
		return -1;
	}
	return handle;
}



/*
 * Class:     ibis_ipl_impl_mx_io_JavaMx
 * Method:    jmx_recv
 * Signature: (Ljava/nio/ByteBuffer;J)I
 */
JNIEXPORT jint JNICALL Java_ibis_ipl_impl_mx_io_JavaMx_jmx_1recv__Ljava_nio_ByteBuffer_2J
  (JNIEnv *env, jclass jcl, jobject buffer, jlong match_recv) {
	mx_return_t rc;
	mx_segment_t buffer_desc[1];
	mx_request_t req_handle;
	mx_status_t status;
	uint32_t result;
		
	rc = mx_probe(my_endpoint, MX_INFINITE, match_recv, match_mask, &status, &result);
	if(rc != MX_SUCCESS) {
		fprintf(stderr, "error: %s\n", mx_strerror(rc));
		return -1;
	}
	if (result == 0) {
		/* recv error */
		printf("recv error\n");
		return -1;
	}

	buffer_desc[0].segment_length =	status.msg_length; //TODO: check buffer size
	buffer_desc[0].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer);
		
	rc = mx_irecv(my_endpoint, buffer_desc, 1, match_recv, match_mask, NULL, &req_handle);
	if(rc != MX_SUCCESS) {
		fprintf(stderr, "error: %s\n", mx_strerror(rc));
		return -1;
	}
	rc = mx_wait(my_endpoint, &req_handle, MX_INFINITE, &status, &result);
	if(rc != MX_SUCCESS) {
		fprintf(stderr, "error: %s\n", mx_strerror(rc));
		return -1;
	}
	if (result == 0) {
		/* recv error */
		printf("recv error\n");
		return -1;
	}
	return buffer_desc[0].segment_length;
}

/*
 * Class:     ibis_ipl_impl_mx_io_JavaMx
 * Method:    jmx_arecv
 * Signature: (Ljava/nio/ByteBuffer;J)I
 */
JNIEXPORT jint JNICALL Java_ibis_ipl_impl_mx_io_JavaMx_jmx_1arecv
  (JNIEnv *env, jclass jcl, jobject buffer, jlong match_recv) {
	mx_return_t rc;
	mx_segment_t buffer_desc[1];
	jint handle;
	mx_request_t *req_handle;
//	mx_status_t status;
//	uint32_t result;
	
	handle = gethandle();
		if (handle < 0) {
			// no free link available
			fprintf(stderr, "error: out of handles\n");
			return -1;
		}
	req_handle = &(handle_list[handle].request);
	
/*	rc = mx_probe(my_endpoint, MX_INFINITE, match_recv, match_mask, &status, &result);
	if(rc != MX_SUCCESS) {
		releasehandle(handle);
		fprintf(stderr, "error: %s\n", mx_strerror(rc));
		return -1;
	}
	if (result == 0) {
		// recv error
		releasehandle(handle);
		printf("recv error\n");
		return -1;
	}
*/
//	buffer_desc[0].segment_length =	status.msg_length; //TODO: check buffer size
//	buffer_desc[0].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer);

	buffer_desc[0].segment_ptr = (*env)->GetDirectBufferAddress(env, buffer);
	buffer_desc[0].segment_length = (*env)->GetDirectBufferCapacity(env, buffer);
		
	rc = mx_irecv(my_endpoint, buffer_desc, 1, match_recv, match_mask, NULL, req_handle);
	if(rc != MX_SUCCESS) {
		releasehandle(handle);
		fprintf(stderr, "error: %s\n", mx_strerror(rc));
		return -1;
	}
	
	return handle;
}

/*
 * Class:     ibis_ipl_impl_mx_io_JavaMx
 * Method:    jmx_wait
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_ibis_ipl_impl_mx_io_JavaMx_jmx_1wait
  (JNIEnv *env, jclass jcl, jint handle) {
	mx_return_t rc;
	mx_status_t status;
	uint32_t result;

	rc = mx_wait(my_endpoint, &(handle_list[handle].request), MX_INFINITE, &status, &result);
	if(rc != MX_SUCCESS) {
		fprintf(stderr, "error: %s\n", mx_strerror(rc));
		return JNI_FALSE;
	}
	if (result == 0) {
		/* recv error */
		printf("wait error\n");
		return JNI_FALSE;
	}
	
	releasehandle(handle);
	return JNI_TRUE;
}


/*
 * Class:     ibis_ipl_impl_mx_io_JavaMx
 * Method:    jmx_test
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_ibis_ipl_impl_mx_io_JavaMx_jmx_1test
  (JNIEnv *env, jclass jcl, jint handle) {
	mx_return_t rc;
	mx_status_t status;
	uint32_t result;

	rc = mx_test(my_endpoint, &(handle_list[handle].request), &status, &result);
	if(rc != MX_SUCCESS) {
		fprintf(stderr, "error: %s\n", mx_strerror(rc));
		return JNI_FALSE;
	}
	if (result == 0) {
		/* recv error */
		printf("test error\n");
		return JNI_FALSE;
	}
	
	releasehandle(handle);
	return JNI_TRUE;
}




/*---------------------------------------------------------------
 *
 * Layer between messagePassing/MPI and Ibis/MPI.
 *
 * Send messages through here.
 * Received messages are demultiplexed and given an JNIEnv * parameter
 * (which is I guess the only reason why this layer exists).
 */

#ifndef __MANTA_IBIS_IMPL_MESSAGEPASSING_MPI_IBP_MP_H__
#define __MANTA_IBIS_IMPL_MESSAGEPASSING_MPI_IBP_MP_H__

#include <pan_sys.h>

#include <jni.h>

#include "ibp.h"

#define SEND_PROTO_CACHE_SIZE	4096
#define RCVE_PROTO_CACHE_SIZE	4096

struct IBP_MSG {
    int		sender;
    int		size;
    int		blast_size;
    int		start;
    int		send_port_id;
    ibp_msg_p	next;
};


typedef enum IBMPI_PROTO_STATUS {
    PROTO_IN_USE	= 33,
    PROTO_FREE		= 107
} ibmpi_proto_status_t;


typedef struct IBMPI_PROTO ibmpi_proto_t, *ibmpi_proto_p;

struct IBMPI_PROTO {
    union {
	int		size;
	ibmpi_proto_p	next;
    } sn;
    int		port;
    int		msg_id;
    int		proto_size;
#ifndef NDEBUG
    ibmpi_proto_status_t	status;
    int		seqno;
#endif
};


extern int	ibmpi_alive;


#endif

#ifndef __IBIS_IMPL_MP_MPI_IBMPI_PORT_H__
#define __IBIS_IMPL_MP_MPI_IBMPI_PORT_H__

#ifndef NDEBUG
#include <limits.h>
#endif


#define IBMPI_DATA_STREAM_TAG	1024


typedef union IBMPI_TAG {
    int		i;
    struct {
	unsigned short	send;
	unsigned short	rcve;
    } ss;
} ibmpi_tag_t, *ibmpi_tag_p;


#define IBMPI_SEND_PORT(tag)	((tag).ss.send - IBMPI_DATA_STREAM_TAG)
#define IBMPI_RCVE_PORT(tag)	((tag).ss.rcve - IBMPI_DATA_STREAM_TAG)
#define IBMPI_SET_SEND_PORT(tag, port) \
	do { \
	    assert((port) < USHRT_MAX - IBMPI_DATA_STREAM_TAG); \
	    (tag).ss.send = (port) - IBMPI_DATA_STREAM_TAG; \
	} while (0)
#define IBMPI_SET_RCVE_PORT(tag, port) \
	do { \
	    assert((port) < USHRT_MAX - IBMPI_DATA_STREAM_TAG); \
	    (tag).ss.rcve = (port) - IBMPI_DATA_STREAM_TAG; \
	} while (0)

#define IBMPI_IS_DATA_MSG(tag)	((tag).ss.send >= IBMPI_DATA_STREAM_TAG)

#endif

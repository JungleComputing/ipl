#include <jni.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>
#include <sched.h>
#include <gm.h>

#include "ibis_ipl_impl_net_gm_Driver.h"
#include "ibis_ipl_impl_net_gm_GmInput.h"
#include "ibis_ipl_impl_net_gm_GmOutput.h"


/*
 *  Macros
 */
#if 0
#define __RDTSC__ ({jlong time;__asm__ __volatile__ ("rdtsc" : "=A" (time));time;})
#else
#define __RDTSC__ 0
#endif

/* Debugging macros */
#if 0
#define __trace__(s, p...) fprintf(stderr, "[%Ld]:%s:%d: "s"\n", __RDTSC__, __FUNCTION__, __LINE__ , ## p)
#else
#define __trace__(s, p...)
#endif

#if 0
#define __disp__(s, p...) fprintf(stderr, "[%Ld]:%s:%d: "s"\n", __RDTSC__, __FUNCTION__, __LINE__ , ## p)
#define __in__()          fprintf(stderr, "[%Ld]:%s:%d: -->\n", __RDTSC__, __FUNCTION__, __LINE__)
#define __out__()         fprintf(stderr, "[%Ld]:%s:%d: <--\n", __RDTSC__, __FUNCTION__, __LINE__)
#define __err__()         fprintf(stderr, "[%Ld]:%s:%d: <!!\n", __RDTSC__, __FUNCTION__, __LINE__)
#else
#define __disp__(s, p...)
#define __in__() 
#define __out__()
#define __err__()
#endif


/*
 *  Constants
 */

/* The name of the Ibis exception for I/O related failures. */
#define NI_IBIS_EXCEPTION  "ibis.ipl.IbisIOException"

/* The minimal valid GM port ID.*/
#define NI_GM_MIN_PORT_NUM 2

/* The maximal atomic block size. */
#define NI_GM_MAX_BLOCK_LEN    (2*1024*1024)

/*
 *  Types
 */

/* The driver */
struct s_drv {
	int ref_count;
        
        /* Drivers */
        int            nb_dev;

        /* The set of devices */
        struct s_dev **pp_dev;
};

/* A Myricom NIC. */
struct s_dev {

	/* Device ID param for 'gm_open'. */
	int id;

        /* Port */
        struct s_port *p_port;

	/* Reference counter to this structure. */
	int ref_count;

        /* The driver */
        struct s_drv *p_drv;
};

/* A NIC port. */
struct s_port {

        struct s_cache *cache_head;

	/* GM's port structure. */
	struct gm_port *p_gm_port;

	/* Allocated Port ID. */
	int          port_id;

	/* Local Node ID. */
	unsigned int node_id;

	/* Associated device. */
	struct s_dev *p_dev;

	/* Reference counter to this structure. */
	int ref_count;

        struct s_input **input_array;

        int input_array_size;
};

/* NetIbis output internal information. */
struct s_output {

        /* Lock for asynchronous request completion detection. */
        jobject  mutex_ref;
        jmethodID mutex_unlock_id;
        struct s_cache *p_cache;
        void      *buffer;

        /* GM status of the last sending request. */
        gm_status_t  request_status;

	/* Related port */
	struct s_port *p_port;
	
	/* Remote Port ID */
	int          dst_port_id;
	
	/* Remote Node ID */
	unsigned int dst_node_id;

};

/* NetIbis input internal information. */
struct s_input {
        volatile int data_available;

        jobject  mutex_ref;
        jmethodID mutex_unlock_id;
        struct s_cache *p_cache;

        /* Current buffer */
        void *buffer;

        /* Length of data in buffer */
        int length;

	/* Related port */
	struct s_port *p_port;
	
	/* Remote Port ID */
	int          src_port_id;

	/* Remote Node ID */
	unsigned int src_node_id;
};

struct s_cache {
        void *ptr;
        int   len;
        int   ref_count;
        struct s_cache *next;
};


/* Union used for conversions between pointers and handles. */
union u_conv {
        jlong  handle;
        void  *ptr;
};

/*
 *  Prototypes
 */

static
jlong
ni_gm_ptr2handle(void  *ptr);

static
void *
ni_gm_handle2ptr(jlong handle);

static
void
ni_gm_control(gm_status_t gm_status, int line);

static
int
ni_gm_open_port(struct s_dev *p_dev);

static
int
ni_gm_close_port(struct s_port *p_port);

static
int
ni_gm_output_init(struct s_dev     *p_dev,
                  struct s_output **pp_out);

static 
int 
ni_gm_get_output_node_id(struct s_output *p_out, int *p_id);

static 
int 
ni_gm_get_output_port_id(struct s_output *p_out, int *p_id);

static 
int 
ni_gm_connect_output(struct s_output *p_out,
                     int              remote_node_id,
                     int              remote_port_id);

static
void
ni_gm_send_callback(struct gm_port *port,
                    void           *ptr,
                    gm_status_t     gms);

static
int
ni_gm_output_send_post(JNIEnv          *env,
                       struct s_output *p_out,
                       void            *b,
                       int              len);


static
int
ni_gm_output_send_complete(JNIEnv          *env,
                           struct s_output *p_out,
                           void            *b,
                           int              len);

static
int
ni_gm_output_exit(struct s_output *p_out);

static
int
ni_gm_input_init(struct s_dev    *p_dev,
                 struct s_input **pp_in);

static
int
ni_gm_post_buffer(JNIEnv *env,
                  struct s_input *p_in,
                  void           *b,
                  int             length);

static
int
ni_gm_receive(struct s_input *p_in);

static 
int 
ni_gm_get_input_node_id(struct s_input *p_in,
                        int            *p_id);

static 
int 
ni_gm_get_input_port_id(struct s_input *p_in,
                        int            *p_id);

static 
int 
ni_gm_connect_input(struct s_input *p_in,
                    int             remote_node_id,
                    int             remote_port_id);

static
int
ni_gm_input_exit(struct s_input *p_in);

static
int
ni_gm_dev_init(JNIEnv        *env,
               struct s_drv  *p_drv,
               int            dev_num,
               struct s_dev **pp_dev);
static
int
ni_gm_dev_exit(JNIEnv *env, struct s_dev *p_dev);

static
int
ni_gm_init(struct s_drv **pp_drv);

static
int
ni_gm_exit(struct s_drv *p_drv);

static 
void
ni_gm_throw_exception(JNIEnv *env, char *msg);

/*
 *  Static variables
 */

/* Flag indicating whether the driver has been initialized. */
static int 	      initialized              =    0;

/* Flag indicating whether the initialization of GM was successful. */
static int 	      successfully_initialized =    0;

/* Driver's own data structure.  */
static struct s_drv * volatile _p_drv = NULL;

static JavaVM *_p_vm = NULL;







#define CACHE_SIZE 10
#define CACHE_GRANULARITY 0x1000
//static struct s_cache *cache_head = NULL;


/*
 *  Functions
 */

/*
 * Convert a pointer to a 'jlong' handle.
 */
static
jlong
ni_gm_ptr2handle(void  *ptr) {
        union u_conv u;
        
        assert(sizeof(u.handle) >= sizeof(u.ptr));
        u.handle = 0;
        u.ptr    = ptr;

        return u.handle;
}

/*
 * Convert a 'jlong' handle to a pointer.
 */
static
void *
ni_gm_handle2ptr(jlong handle) {
        union u_conv u;
        
        assert(sizeof(u.handle) >= sizeof(u.ptr));
        u.handle = handle;

        return u.ptr;
}

/*
static
void
ni_gm_yield(JNIEnv *env) {
        jclass thread_class = 0;
        jmethodID yield_id  = 0;

        __in__();
        thread_class =(*env)->FindClass(env, "java/lang/Thread");
        assert(thread_class);
        yield_id = (*env)->GetStaticMethodID(env, thread_class, "yield", "()V");
        assert(yield_id);
        __out__();
}
*/

/* GM error message display fonction. */
static
void
ni_gm_control(gm_status_t gm_status, int line)
{
	char *msg = NULL;

	switch (gm_status) {
	case GM_SUCCESS:
		break;

        case GM_FAILURE:
                msg = "GM failure";
                break;
                
        case GM_INPUT_BUFFER_TOO_SMALL:
                msg = "GM input buffer too small";
                break;
                        
        case GM_OUTPUT_BUFFER_TOO_SMALL:
                msg = "GM output buffer too small";
                break;
                
        case GM_TRY_AGAIN:
                msg = "GM try again";
                break;

        case GM_BUSY:
                msg = "GM busy";
                break;
                
        case GM_MEMORY_FAULT:
                msg = "GM memory fault";
                break;

        case GM_INTERRUPTED:
                msg = "GM interrupted";
                break;
                
        case GM_INVALID_PARAMETER:
                msg = "GM invalid parameter";
                break;
                
        case GM_OUT_OF_MEMORY:
                msg = "GM out of memory";
                break;
                
        case GM_INVALID_COMMAND:
                msg = "GM invalid command";
                break;

        case GM_PERMISSION_DENIED:
                msg = "GM permission denied";
                break;

        case GM_INTERNAL_ERROR:
                msg = "GM internal error";
                break;

        case GM_UNATTACHED:
                msg = "GM unattached";
                break;

        case GM_UNSUPPORTED_DEVICE:
                msg = "GM unsupported device";
                break;

        case GM_SEND_TIMED_OUT:
		msg = "GM send timed out";
                break;

        case GM_SEND_REJECTED:
		msg = "GM send rejected";
                break;

        case GM_SEND_TARGET_PORT_CLOSED:
		msg = "GM send target port closed";
                break;

        case GM_SEND_TARGET_NODE_UNREACHABLE:
		msg = "GM send target node unreachable";
                break;

        case GM_SEND_DROPPED:
		msg = "GM send dropped";
                break;

        case GM_SEND_PORT_CLOSED:
		msg = "GM send port closed";
                break;

        case GM_NODE_ID_NOT_YET_SET:
                msg = "GM id not yet set";
                break;

        case GM_STILL_SHUTTING_DOWN:
                msg = "GM still shutting down";
                break;

        case GM_CLONE_BUSY:
                msg = "GM clone busy";
                break;

        case GM_NO_SUCH_DEVICE:
                msg = "GM no such device";
                break;

        case GM_ABORTED:
                msg = "GM aborted";
                break;

#if GM_API_VERSION >= GM_API_VERSION_1_5
        case GM_INCOMPATIBLE_LIB_AND_DRIVER:
                msg = "GM incompatible lib and driver";
                break;

        case GM_UNTRANSLATED_SYSTEM_ERROR:
                msg = "GM untranslated system error";
                break;

        case GM_ACCESS_DENIED:
                msg = "GM access denied";
                break;
#endif

	default:
		msg = "unknown GM error";
		break;
	}

	if (msg) {
		fprintf(stderr, "%d:%s\n", line, msg);
                gm_perror ("gm_message", gm_status);
	}
}

static
int
ni_gm_register_block(struct s_port   *p_port,
                     void            *ptr,
                     int              len,
                     struct s_cache **_pp_cache) {
	struct gm_port *p_gm_port = p_port->p_gm_port;
        gm_status_t     gms       = GM_SUCCESS;
        struct s_cache  *p_cache  = NULL;
        struct s_cache **pp_cache = &p_port->cache_head;

        {
                unsigned long mask    = (CACHE_GRANULARITY - 1);
                unsigned long lng     = (long)ptr;
                unsigned long tmp_lng = (lng & ~mask);
                unsigned long offset  = lng - tmp_lng;

                ptr -= offset;
                len += offset;

                if (len & mask) {
                        len = (len & ~mask) + CACHE_GRANULARITY;
                }
        }
        

        while ((p_cache = *pp_cache)) {
                if (ptr >= p_cache->ptr
                    &&
                    ptr+len <= p_cache->ptr+p_cache->len) {
                        p_cache->ref_count++;

                        if (pp_cache != &p_port->cache_head) {
                                /* Move the cache entry at the head */
                                *pp_cache = p_cache->next;
                                p_cache->next = p_port->cache_head;
                                p_port->cache_head = p_cache;
                        }

                        goto success;
                }

                pp_cache = &(p_cache->next);
        }

#if 0
        while (p_cache) {
                if (ptr >= p_cache->ptr
                    &&
                    ptr+len <= p_cache->ptr+p_cache->len) {
                        p_cache->ref_count++;

                        goto success;
                }

                p_cache = p_cache->next;
        }
#endif

        if (!(p_cache = malloc(sizeof(struct s_cache))))
                goto error;

        gms = gm_register_memory(p_gm_port, ptr, len);
        if (gms) {
                ni_gm_control(gms, __LINE__);
                free(p_cache);
                p_cache = NULL;
                goto error;
        }

        p_cache->ptr = ptr;
        p_cache->len = len;
        p_cache->ref_count = 1;
        p_cache->next = p_port->cache_head;
        p_port->cache_head = p_cache;
                
 success:
        *_pp_cache = p_cache;
        return 0;
        

 error:
        return -1;
}

static
int
ni_gm_deregister_block(struct s_port  *p_port, 
                       struct s_cache *p_cache) {
	struct gm_port *p_gm_port = p_port->p_gm_port;

        if (!--p_cache->ref_count) {
                gm_status_t     gms       = GM_SUCCESS;
                struct s_cache **pp_cache = &p_port->cache_head;
                int             i         = 0;

                while (*pp_cache != p_cache) {
                        pp_cache = &((*pp_cache)->next);
                        i++;
                }
                
                if (i >= CACHE_SIZE) {
                        gms = gm_deregister_memory(p_gm_port,
                                                   p_cache->ptr,
                                                   p_cache->len);
                        if (gms) {
                                ni_gm_control(gms, __LINE__);
                                goto error;
                        }

                        p_cache->ptr = NULL;
                        p_cache->len =    0;

                        *pp_cache = p_cache->next;
                        p_cache->next = NULL;

                        free(p_cache);
                }
        }

        return 0;
        
 error:
        return -1;
}


/* Opens a GM port and sets up a s_port structure for representing it. */
static
int
ni_gm_open_port(struct s_dev *p_dev) {
	struct s_port  *p_port    = NULL;
	struct gm_port *p_gm_port = NULL;
	gm_status_t     gms       = GM_SUCCESS;
	int             port_id   =  0;
	unsigned int    node_id   =  0;

	__in__();
        port_id = 2;
        __disp__("opening GM port %d on device %d", port_id, p_dev->id);
        
	gms     = gm_open(&p_gm_port, p_dev->id, port_id,
			  "net_ibis_gm", GM_API_VERSION_1_1);
        __disp__("status %d", gms);
	if (gms != GM_SUCCESS) {
		ni_gm_control(gms, __LINE__);
                goto error;
	}
	
	p_port = malloc(sizeof(struct s_port));
	if (!p_port) {
                goto error;
	}

	gms = gm_get_node_id(p_gm_port, &node_id);
	if (gms != GM_SUCCESS) {
		ni_gm_control(gms, __LINE__);
                goto error;
	}

        p_port->cache_head       = NULL;
	p_port->p_gm_port        = p_gm_port;
	p_port->port_id          = port_id;
	p_port->node_id          = node_id;
	p_port->p_dev            = p_dev;
	p_port->ref_count        = 0;
        p_port->input_array_size = 0;
        p_port->input_array      = NULL;

        p_dev->p_port     = p_port;
	p_dev->ref_count++;
        __out__();
	return 0;

 error:
        __err__();
        return -1;
}

/* Frees a s_port structure and its associated GM port. */
static
int
ni_gm_close_port(struct s_port *p_port) {
        __in__();
	if (p_port->ref_count)
		goto error;

        while (p_port->cache_head) {
                if (ni_gm_deregister_block(p_port, p_port->cache_head))
                        goto error;
        }
        
	gm_close(p_port->p_gm_port);

	p_port->p_gm_port = NULL;
	p_port->port_id   =    0;
	p_port->node_id   =    0;

	p_port->p_dev->ref_count--;
	p_port->p_dev->p_port = NULL;
	p_port->p_dev         = NULL;

	free(p_port);
	
        __out__();
	return 0;

 error:
        __err__();
        return -1;
}

/* Initializes an output structure. */
static
int
ni_gm_output_init(struct s_dev     *p_dev,
                  struct s_output **pp_out) {
        struct s_output *p_out  = NULL;
        struct s_port   *p_port = NULL;

        __in__();
        p_port = p_dev->p_port;

        p_out = malloc(sizeof(struct s_output));
        if (!p_out) {
                goto error;
        }

        p_out->request_status = 0;
        p_out->p_port         = p_port;
        p_out->dst_port_id    = 0;
        p_out->dst_node_id    = 0;

        *pp_out = p_out;
        __out__();
        
        return 0;
        
 error:
        __err__();
        return -1;
}


static 
int 
ni_gm_get_output_node_id(struct s_output *p_out, int *p_id) {
        __in__();
        *p_id = p_out->p_port->node_id;
        __out__();
        return 0;
}        

static 
int 
ni_gm_get_output_port_id(struct s_output *p_out, int *p_id) {
        __in__();
        *p_id = p_out->p_port->port_id;
        __out__();
        return 0;
}        

static 
int 
ni_gm_connect_output(struct s_output *p_out,
                     int              remote_node_id,
                     int              remote_port_id) {
        __in__();
        if (p_out->dst_node_id
            ||
            p_out->dst_port_id) {
                goto error;
        }

        p_out->dst_node_id = remote_node_id;
        p_out->dst_port_id = remote_port_id;

        __out__();
        return 0;

 error:
        return -1;
}

static
void
ni_gm_send_callback(struct gm_port *port,
                    void           *ptr,
                    gm_status_t     gms) {
        struct s_output *p_out = ptr;
        struct s_port   *p_port = NULL;
        struct s_dev    *p_dev  = NULL;
        JNIEnv          *env   = NULL;
        
        __in__();
        p_port = p_out->p_port;
        p_dev  = p_port->p_dev;
        assert(!(*_p_vm)->AttachCurrentThread(_p_vm, (void **)&env, NULL));
        assert(p_port->p_gm_port == port);
         (*env)->CallVoidMethod(env, p_out->mutex_ref, p_out->mutex_unlock_id);

        p_out->request_status = gms;
        __out__();
}


static
int
ni_gm_output_send_post(JNIEnv *env, struct s_output *p_out, void *b, int len) {
        struct s_port *p_port = NULL;

        __in__();
        p_port = p_out->p_port;

        __disp__("registering memory");

        __trace__("registering %p[%d]", b, len);

        if (ni_gm_register_block(p_port, b, len, &p_out->p_cache))
                goto error;

        __disp__("sending %d bytes with callback", len);

        gm_send_with_callback(p_port->p_gm_port, b,
                              gm_min_size_for_length(NI_GM_MAX_BLOCK_LEN), len,
                              GM_LOW_PRIORITY,
                              p_out->dst_node_id, p_out->dst_port_id,
                              ni_gm_send_callback, p_out);

        __disp__("waiting for the callback to unlock the output");
        __out__();
        return 0;

 error:
        __err__();
        return -1;
}

static
int
ni_gm_output_send_complete(JNIEnv *env, struct s_output *p_out, void *b, int len) {
        struct s_port *p_port = NULL;

        __in__();
        p_port = p_out->p_port;
                
        __trace__("deregistering %p[%d]", b, len);
        if (ni_gm_deregister_block(p_port, p_out->p_cache)) {
                goto error;
        }
        
        p_out->p_cache = NULL;

        __disp__("send completed");
        if (p_out->request_status) {
                ni_gm_control(p_out->request_status, __LINE__);
                goto error;
        } 

        __out__();
        return 0;

 error:
        __err__();
        return -1;
}


static
int
ni_gm_output_exit(struct s_output *p_out) {
        __in__();
        p_out->p_port      = NULL;
        p_out->dst_port_id =    0;
        p_out->dst_node_id =    0;
        free(p_out);
        
        __out__();
        return 0;
}


static
int
ni_gm_input_init(struct s_dev    *p_dev,
                 struct s_input **pp_in) {
        struct s_input *p_in  = NULL;
        struct s_port   *p_port = NULL;

        __in__();
        p_port = p_dev->p_port;

        p_in = malloc(sizeof(struct s_input));
        if (!p_in) {
                goto error;
        }

        p_in->data_available = 0;
        p_in->buffer         = NULL;
        p_in->length         = 0;
        p_in->p_port         = p_port;
        p_in->src_port_id    = 0;
        p_in->src_node_id    = 0;

        *pp_in = p_in;
        
        __out__(); 
        return 0;

 error:
        __err__();
        return -1;        
}

static
int
ni_gm_post_buffer(JNIEnv *env, struct s_input *p_in, void *b, int length) {
        struct s_port *p_port = NULL;

        __in__();
        
        /* Check that no buffer is already pre-posted */
        assert(!p_in->buffer);
        
        p_port = p_in->p_port;
        __trace__("registering %p[%d]", b, length);
        if (ni_gm_register_block(p_port, b, length, &p_in->p_cache))
                goto error;

        p_in->data_available = 0;
        gm_provide_receive_buffer_with_tag(p_port->p_gm_port, b,
                                           gm_min_size_for_length(NI_GM_MAX_BLOCK_LEN),
                                           GM_LOW_PRIORITY, 0);
        p_in->buffer = b;

        __out__();
        return 0;
       
 error:                
        __err__();
       
        return -1;
}

static
int
ni_gm_receive(struct s_input *p_in) {
        struct s_port *p_port  = NULL;

        __in__();
        assert(p_in->data_available);
        
        p_port = p_in->p_port;
        __trace__("deregistering %p[%d]", p_in->buffer, NI_GM_MAX_BLOCK_LEN);
        if (ni_gm_deregister_block(p_port, p_in->p_cache)) {
                goto error;
        }
        
        p_in->p_cache = NULL;

        __out__(); 

        return 0;

 error:
        __err__();
        return -1;
}

static 
int 
ni_gm_get_input_node_id(struct s_input *p_in, int *p_id) {
        __in__();
        *p_id = p_in->p_port->node_id;
        __out__(); 
        return 0;
}        

static 
int 
ni_gm_get_input_port_id(struct s_input *p_in, int *p_id) {
        __in__();
        *p_id = p_in->p_port->port_id;
        __out__(); 
        return 0;
}        

static 
int 
ni_gm_connect_input(struct s_input *p_in,
                    int             remote_node_id,
                    int             remote_port_id) {
        struct s_port *p_port = NULL;

        __in__();
        if (p_in->src_node_id
            ||
            p_in->src_port_id) {
                goto error;
        }

        p_port = p_in->p_port;

        if (!p_port->input_array) {
                assert(!p_port->input_array_size);
                int s = remote_node_id + 1;
                p_port->input_array = malloc(s * sizeof(struct s_input *));
                if (!p_port->input_array)
                        goto error;

                p_port->input_array_size = s;
                memset(p_port->input_array, 0, s * sizeof(struct s_input *));
        } else if (p_port->input_array_size <= remote_node_id) {
                int   s   = remote_node_id + 1;
                void *ptr = NULL;
                
                assert(p_port->input_array);
                ptr = realloc(p_port->input_array, s * sizeof(struct s_input *));
                if (!ptr)
                        goto error;

                p_port->input_array = ptr;
                memset(p_port->input_array + p_port->input_array_size, 0,
                       s * sizeof(struct s_input *));
                p_port->input_array_size = s;
        }
        
        if (p_port->input_array[remote_node_id])
                goto error;
        
        p_port->input_array[remote_node_id] = p_in;

        p_in->src_node_id = remote_node_id;
        p_in->src_port_id = remote_port_id;

        __out__(); 
        return 0;

 error:
        __err__();
        return -1;
}

static int ni_gm_input_exit(struct s_input *p_in) {
        __in__();
        p_in->p_port      = NULL;
        p_in->src_port_id =    0;
        p_in->src_node_id =    0;
        free(p_in);
        __out__(); 
        
        return 0;
}


static
int
ni_gm_dev_init(JNIEnv        *env,
               struct s_drv  *p_drv,
               int            dev_num,
               struct s_dev **pp_dev) {
        struct s_dev *p_dev = NULL;
        
        *pp_dev = NULL;

        if (dev_num >= p_drv->nb_dev
            ||
            !p_drv->pp_dev[dev_num]) {
                struct gm_port *p_gm_port = NULL;
                gm_status_t     gms       = GM_SUCCESS;

                /* Try to open the first public port on requested device. */
                gms = gm_open(&p_gm_port, dev_num, NI_GM_MIN_PORT_NUM,
                              "net_ibis_gm", GM_API_VERSION_1_1);
                if (gms != GM_SUCCESS) {
                        ni_gm_control(gms, __LINE__);
                        goto error;
                }

                gm_close(p_gm_port);
                p_gm_port = NULL;

                p_dev = malloc(sizeof(struct s_dev));
                if (!p_dev)
                        goto error;
                
                if (!p_drv->nb_dev) {
                        p_drv->pp_dev = malloc(sizeof(struct s_dev *));
                        if (!p_drv->pp_dev) {
                                free(p_dev);
                                p_dev = NULL;

                                goto error;
                        }

                        p_drv->nb_dev = 1;
                } else if (dev_num >= p_drv->nb_dev) {
                        p_drv->pp_dev = realloc(p_drv->pp_dev,
                                                dev_num * sizeof(struct s_dev *));
                        if (!p_drv->pp_dev) {
                                free(p_dev);
                                p_dev = NULL;

                                goto error;
                        }

                        while (p_drv->nb_dev + 1 < dev_num) {
                                p_drv->pp_dev[p_drv->nb_dev++] = NULL;
                        }

                        p_drv->nb_dev++;
                }
                
                p_dev->id        = dev_num;
                p_dev->ref_count = 0;
                p_dev->p_port    = NULL;
                p_dev->p_drv     = p_drv;

                if (ni_gm_open_port(p_dev)) {
                        goto error;
                }

                p_drv->pp_dev[dev_num] = p_dev;
        } else {
                p_dev = p_drv->pp_dev[dev_num];
        }
        
        p_dev->ref_count++;
        *pp_dev = p_dev;

        __out__();
        return 0;

 error:
        __err__();
        return -1;
}

static
int
ni_gm_dev_exit(JNIEnv *env, struct s_dev *p_dev) {
        p_dev->ref_count--;

        if (!p_dev->ref_count) {
                if (ni_gm_close_port(p_dev->p_port)) {
                        goto error;
                }

                p_dev->p_drv->pp_dev[p_dev->id] = NULL;
                p_dev->p_drv->ref_count--;
                p_dev->p_drv        = NULL;
                p_dev->id           =    0;

                free(p_dev);
        }

        __out__();
        return 0;

 error:

        __err__();
        return -1;
}

static
int
ni_gm_init(struct s_drv **pp_drv) {
	struct s_drv *p_drv = NULL;
	gm_status_t   gms   = GM_SUCCESS;

        __in__();
	if (initialized) {
		return successfully_initialized;
	}

	gms = gm_init();
        
	if (gms != GM_SUCCESS) {
		ni_gm_control(gms, __LINE__);
		goto error;
	}

	p_drv = malloc(sizeof(struct s_drv));
	if (!p_drv)
		goto error;

	p_drv->ref_count = 1;
        p_drv->nb_dev    = 0;
        p_drv->pp_dev    = NULL;

	*pp_drv = p_drv;
	initialized = 1;

        __out__();
	return 0;

 error:
        __err__();
        return -1;
}

static
int
ni_gm_exit(struct s_drv *p_drv) {
        __in__();
	if (!successfully_initialized)
		goto error;
	
	if (p_drv) {
                if (p_drv->ref_count > 1)
                        goto error;

                p_drv->ref_count = 0;
                p_drv->nb_dev    = 0;
                free(p_drv->pp_dev);
                p_drv->pp_dev    = NULL;
                
                free(p_drv);
                p_drv = NULL;

                gm_finalize();
        }

        successfully_initialized = 0;
        
        __out__();
	return 0;

 error:
        __err__();
        return -1;
}

/*
 * Java Native Interfacing code
 */
static 
void
ni_gm_throw_exception(JNIEnv *env, char *msg) {
        jclass cls = (*env)->FindClass(env, NI_IBIS_EXCEPTION);
        if (cls) {
                (*env)->ThrowNew(env, cls, msg);
        }
}


/*
 * Class:     GmOutput
 * Method:    nInitOutput
 * Signature: (J)J
 */
JNIEXPORT
jlong
JNICALL
Java_ibis_ipl_impl_net_gm_GmOutput_nInitOutput (JNIEnv *env, jobject output, jlong device_handle) {
        struct s_dev    *p_dev  = NULL;
        struct s_output *p_out  = NULL;
        jlong            result =    0;

        __in__();
        p_dev = ni_gm_handle2ptr(device_handle);
        if (ni_gm_output_init(p_dev, &p_out)) {
                ni_gm_throw_exception(env, "could not initialize output");
                goto error;
        }

        {
                jclass output_class = 0;
                jclass mutex_class  = 0;
                jfieldID fid        = 0;
                jobject  mutex      = 0;
                
                output_class = (*env)->GetObjectClass(env, output);
                assert(output_class);
                fid = (*env)->GetFieldID(env, output_class, "mutex",
                                 "Libis/ipl/impl/net/NetMutex;");
                assert(fid);
                mutex = (*env)->GetObjectField(env, output, fid);
                assert(mutex);

                p_out->mutex_ref = (*env)->NewGlobalRef(env, mutex);
                assert(p_out->mutex_ref);
                
                mutex_class = (*env)->FindClass(env, "ibis/ipl/impl/net/NetMutex");
                assert(mutex_class);

                p_out->mutex_unlock_id =
                        (*env)->GetMethodID(env, mutex_class, "unlock", "()V");
                assert(p_out->mutex_unlock_id);
        }
        

        result = ni_gm_ptr2handle(p_out);
        __out__();
        return result;

 error:        
        __err__();
        return result;
}

/*
 * Class:     GmOutput
 * Method:    nGetOutputNodeId
 * Signature: (J)I
 */
JNIEXPORT
jint 
JNICALL
Java_ibis_ipl_impl_net_gm_GmOutput_nGetOutputNodeId (JNIEnv *env, jobject output, jlong output_handle){
        struct s_output *p_out  = NULL;
        int              result = 0;
        
        __in__();
        p_out = ni_gm_handle2ptr(output_handle);
        if (ni_gm_get_output_node_id(p_out, &result)) {
                ni_gm_throw_exception(env, "could not get output node id");
                goto error;
        }
        __out__();
        return (jint)result;

 error:
        __err__();
        return (jint)result;
}


/*
 * Class:     GmOutput
 * Method:    nGetOutputPortId
 * Signature: (J)I
 */
JNIEXPORT 
jint 
JNICALL 
Java_ibis_ipl_impl_net_gm_GmOutput_nGetOutputPortId (JNIEnv *env,
                                jobject output,
                                jlong output_handle){
        struct s_output *p_out  = NULL;
        int              result = 0;
        
        __in__();
        p_out = ni_gm_handle2ptr(output_handle);
        if (ni_gm_get_output_port_id(p_out, &result)) {
                ni_gm_throw_exception(env, "could not get output port id");
                goto error;
        }

        __out__();
        return (jint)result;

 error:
        __err__();
        return (jint)result;
}


/*
 * Class:     GmOutput
 * Method:    nConnectOutput
 * Signature: (JII)V
 */
JNIEXPORT
void
JNICALL 
Java_ibis_ipl_impl_net_gm_GmOutput_nConnectOutput(JNIEnv *env,
                             jobject output,
                             jlong output_handle,
                             jint remote_node_id,
                             jint remote_port_id) {
        struct s_output *p_out = NULL;
        
        __in__();
        p_out = ni_gm_handle2ptr(output_handle);
        if (ni_gm_connect_output(p_out, (int)remote_node_id, (int)remote_port_id)) {
                ni_gm_throw_exception(env, "could not connect output");
                goto error;
        }

        __out__();
        return;

 error:
        __err__();
}

/*
 * Class:     GmOutput
 * Method:    nPostSend
 * Signature: (J[BII)V
 */
JNIEXPORT
void
JNICALL
Java_ibis_ipl_impl_net_gm_GmOutput_nPostSend(JNIEnv     *env,
                                             jobject     output,
                                             jlong       output_handle,
                                             jbyteArray  b,
                                             jint        base, 
                                             jint        length) {
        struct s_output *p_out   = NULL;
        jboolean         is_copy = JNI_TRUE;
        void            *buffer  = NULL;

        __in__();
        p_out = ni_gm_handle2ptr(output_handle);
        p_out->buffer = buffer = base + (*env)->GetByteArrayElements(env, b, &is_copy);
        if (!buffer) {
                ni_gm_throw_exception(env, "could not get array elements");
                goto error;
        }
        if (ni_gm_output_send_post(env, p_out, buffer, (int)length)) {
                ni_gm_throw_exception(env, "could not send a buffer");
                goto error;
        } 

        __out__();
        return;

 error:
        __err__();
}


/*
 * Class:     GmOutput
 * Method:    nCompleteSend
 * Signature: (J[BII)V
 */
JNIEXPORT
void
JNICALL
Java_ibis_ipl_impl_net_gm_GmOutput_nCompleteSend (JNIEnv     *env,
                                                  jobject     output,
                                                  jlong       output_handle,
                                                  jbyteArray  b,
                                                  jint        base,
                                                  jint        length) {
        struct s_output *p_out = NULL;

        __in__();
        p_out = ni_gm_handle2ptr(output_handle);
        if (ni_gm_output_send_complete(env, p_out, p_out->buffer, (int)length)) {
                ni_gm_throw_exception(env, "could not complete a send request");
                goto error;
        } 

        (*env)->ReleaseByteArrayElements(env, b, p_out->buffer - base, 0);
        p_out->buffer = NULL;
        __out__();
        return;

 error:
        __err__();
}

/*
 * Class:     GmOutput
 * Method:    nCloseOutput
 * Signature: (J)V
 */
JNIEXPORT
void
JNICALL
Java_ibis_ipl_impl_net_gm_GmOutput_nCloseOutput(JNIEnv  *env,
                                                jobject  output,
                                                jlong    output_handle) {
        struct s_output *p_out = NULL;
        
        __in__();
        p_out = ni_gm_handle2ptr(output_handle);
        if (ni_gm_output_exit(p_out)) {
                ni_gm_throw_exception(env, "could not close output");
                goto error;
        }

        __out__();
        return;

 error:
        __err__();
}


/*
 * Class:     GmInput
 * Method:    nInitInput
 * Signature: (J)J
 */
JNIEXPORT
jlong
JNICALL
Java_ibis_ipl_impl_net_gm_GmInput_nInitInput (JNIEnv *env, jobject input, jlong device_handle) {
        struct s_dev    *p_dev = NULL;
        struct s_input *p_in   = NULL;
        jlong           result =    0;

        __in__();
        p_dev = ni_gm_handle2ptr(device_handle);
        if (ni_gm_input_init(p_dev, &p_in)) {
                ni_gm_throw_exception(env, "could not initialize input");
                goto error;
        }

        {
                jclass input_class = 0;
                jclass mutex_class  = 0;
                jfieldID fid        = 0;
                jobject  mutex      = 0;
                
                input_class = (*env)->GetObjectClass(env, input);
                assert(input_class);
                fid = (*env)->GetFieldID(env, input_class, "mutex",
                                 "Libis/ipl/impl/net/NetMutex;");
                assert(fid);
                mutex = (*env)->GetObjectField(env, input, fid);
                assert(mutex);

                p_in->mutex_ref = (*env)->NewGlobalRef(env, mutex);
                assert(p_in->mutex_ref);
                
                mutex_class = (*env)->FindClass(env, "ibis/ipl/impl/net/NetMutex");
                assert(mutex_class);

                p_in->mutex_unlock_id =
                        (*env)->GetMethodID(env, mutex_class, "unlock", "()V");
                assert(p_in->mutex_unlock_id);
        }
        
        result = ni_gm_ptr2handle(p_in);
        __out__();
        return result;

 error:
        __err__();
        return result;

}

/*
 * Class:     GmInput
 * Method:    nGetInputNodeId
 * Signature: (J)I
 */
JNIEXPORT 
jint
JNICALL 
Java_ibis_ipl_impl_net_gm_GmInput_nGetInputNodeId (JNIEnv  *env,
                              jobject  input,
                              jlong    input_handle) {
        struct s_input *p_in  = NULL;
        int             result = 0;

        __in__();
        __disp__("nGetInputNodeId: input handle %qd", input_handle);
        p_in = ni_gm_handle2ptr(input_handle);
        if (ni_gm_get_input_node_id(p_in, &result)) {
                ni_gm_throw_exception(env, "could not get input node id");
                goto error;
        }

        __out__(); 
        return (jint)result;

 error:
        __err__();
        return (jint)result;
}


/*
 * Class:     GmInput
 * Method:    nGetInputPortId
 * Signature: (J)I
 */
JNIEXPORT 
jint
JNICALL 
Java_ibis_ipl_impl_net_gm_GmInput_nGetInputPortId (JNIEnv  *env,
                              jobject  input,
                              jlong    input_handle) {
        struct s_input *p_in   = NULL;
        int             result = 0;
        
        __in__();
        p_in = ni_gm_handle2ptr(input_handle);
        if (ni_gm_get_input_port_id(p_in, &result)) {
                ni_gm_throw_exception(env, "could not get input port id");
                goto error;
        }

        __out__(); 
        return (jint)result;

 error:
        __err__(); 
        return (jint)result;
}

/*
 * Class:     GmInput
 * Method:    nConnectInput
 * Signature: (JII)V
 */
JNIEXPORT 
void 
JNICALL 
Java_ibis_ipl_impl_net_gm_GmInput_nConnectInput (JNIEnv  *env,        
                            jobject  input,
                            jlong    input_handle,
                            jint     remote_node_id,
                            jint     remote_port_id) {
        struct s_input *p_in = NULL;
        
        __in__();
        p_in = ni_gm_handle2ptr(input_handle);
        if (ni_gm_connect_input(p_in, (int)remote_node_id, (int)remote_port_id)) {
                ni_gm_throw_exception(env, "could not connect input");
                goto error;
        }

        __out__(); 
        return;
        
 error:
        __err__(); 
}

/*
 * Class:     GmInput
 * Method:    nPrepostBuffer
 * Signature: (J[BII)V
 */
JNIEXPORT 
void 
JNICALL 
Java_ibis_ipl_impl_net_gm_GmInput_nPrepostBuffer (JNIEnv     *env,
                                                  jobject     input,
                                                  jlong       input_handle,
                                                  jbyteArray  b,
                                                  jint        base,
                                                  jint        length) {
        struct s_input *p_in = NULL;
        jboolean         is_copy = JNI_TRUE;
        void            *buffer  = NULL;
        jsize            len     = 0;        

        __in__();
        p_in = ni_gm_handle2ptr(input_handle);

        len    = (*env)->GetArrayLength(env, b) - base;
        if (length < len) {
                len = length;
        }
        
        buffer = (*env)->GetByteArrayElements(env, b, &is_copy);
        if (!buffer) {
                ni_gm_throw_exception(env, "could not get array elements");
                goto error;
        }

        buffer += base;
        
        if (ni_gm_post_buffer(env, p_in, buffer, (int)len)) {
                ni_gm_throw_exception(env, "could not post a buffer");
                goto error;
        }

        __out__(); 
        return;
        
 error:
        __err__(); 
}

/*
 * Class:     GmInput
 * Method:    nReceiveBuffer
 * Signature: (J[BII)I
 */
JNIEXPORT 
jint
JNICALL 
Java_ibis_ipl_impl_net_gm_GmInput_nReceiveBuffer (JNIEnv     *env,
                                                  jobject     input,
                                                  jlong       input_handle,
                                                  jbyteArray  b,
                                                  jint        base,
                                                  jint        length) {
        struct s_input *p_in = NULL;
        jsize           len  = 0;
        
        __in__();
        p_in = ni_gm_handle2ptr(input_handle);
        assert(b);
        assert(p_in->data_available);
        assert(p_in->buffer);

        if (ni_gm_receive(p_in)) {
                ni_gm_throw_exception(env, "could not receive data");
                goto error;
        }       

        (*env)->ReleaseByteArrayElements(env, b, p_in->buffer - base, 0);

        p_in->buffer         = NULL;
        p_in->data_available = 0;
        len                  = (jsize)p_in->length;
        assert(len);
        assert(len <= length);
        p_in->length         = 0;

        __out__();
        return len;

 error:
        __err__();
        return len;
}

/*
 * Class:     GmInput
 * Method:    nCloseInput
 * Signature: (J)V
 */
JNIEXPORT
void
JNICALL
Java_ibis_ipl_impl_net_gm_GmInput_nCloseInput (JNIEnv *env, jobject input, jlong input_handle) {
        struct s_input *p_in = NULL;
        
        __in__();
        p_in = ni_gm_handle2ptr(input_handle);
        if (ni_gm_input_exit(p_in)) {
                ni_gm_throw_exception(env, "could not close input");
                goto error;
        }        

        __out__(); 
        return;
        
 error:
        __err__(); 
}

/*
 * Class:     Driver
 * Method:    nInitDevice
 * Signature: (I)J
 */
JNIEXPORT 
jlong 
JNICALL 
Java_ibis_ipl_impl_net_gm_Driver_nInitDevice(JNIEnv *env, jobject driver, jint device_num) {
        struct s_dev *p_dev  = NULL;
        jlong         result =    0;

        __in__();
        if (!_p_drv) {
                struct s_drv *p_drv = NULL;

                if (ni_gm_init(&p_drv))
                        goto error;

                successfully_initialized = 1;
                _p_drv = p_drv;
        }
        

        if (ni_gm_dev_init(env, _p_drv, (int)device_num, &p_dev)) {
                ni_gm_throw_exception(env, "GM device initialization failed");
                goto error;
        }

        result = ni_gm_ptr2handle(p_dev);
        __out__();
        return result;

 error:
        __err__();
        return result;
}

/*
 * Class:     Driver
 * Method:    nCloseDevice
 * Signature: (J)V
 */
JNIEXPORT 
void 
JNICALL
Java_ibis_ipl_impl_net_gm_Driver_nCloseDevice (JNIEnv *env, jobject driver, jlong device_handle) {
        struct s_dev *p_dev  = NULL;

        __in__();
        p_dev = ni_gm_handle2ptr(device_handle);
        if (ni_gm_dev_exit(env, p_dev)) {
                ni_gm_throw_exception(env, "GM device closing failed");
        }
        if (!_p_drv->ref_count) {
                ni_gm_exit(_p_drv);
                _p_drv = NULL;
                initialized = 0;
        }        
        __out__();
}
/*
 * Class:     ibis_ipl_impl_net_gm_Driver
 * Method:    nGmThread
 * Signature: ()V
 */
JNIEXPORT
void
JNICALL
Java_ibis_ipl_impl_net_gm_Driver_nGmThread (JNIEnv *env, jclass driver_class) {
        static int next_dev = 0;

        __in__();
        while (1) {
                struct s_port   *p_port  = NULL;
                gm_recv_event_t *p_event = NULL;
                struct s_drv    *p_drv   = NULL;
                struct s_dev    *p_dev   = NULL;
                int              dev     = 0;
                
                if (!_p_drv->nb_dev) {
                        break;
                }

                __disp__("__poll__");
                
                p_drv = _p_drv;

                if (next_dev >= p_drv->nb_dev) {
                        next_dev = 0;
                        break;
                }      
                
                dev     = next_dev++;
                p_dev   = p_drv->pp_dev[dev];
                p_port  = p_dev->p_port;
                p_event = gm_receive(p_port->p_gm_port);
                
                switch (gm_ntohc (p_event->recv.type)) {
                case GM_RECV_EVENT: 
                        {
                                struct s_input *p_input        = NULL;
                                int             sender_node_id =
                                        gm_ntohs (p_event->recv.sender_node_id);
                                p_input = p_port->input_array[sender_node_id];
                                p_input->data_available = 1;
                                p_input->length = gm_ntohl (p_event->recv.length);

                                (*env)->CallVoidMethod(env, p_input->mutex_ref,
                                                       p_input->mutex_unlock_id);
                        }
                        
                        break;
                        
                case GM_NO_RECV_EVENT:
                        break;
                        
                default:
                        gm_unknown (p_port->p_gm_port, p_event);
                        break;
                }
        }

        __out__();
}


jint 
JNI_OnLoad(JavaVM *vm, void *reserved) {
        __in__();
        _p_vm = vm;

	/* JNI 1.2 should be enough for now */
        __out__();
	return JNI_VERSION_1_2;
}

void
JNI_OnUnload(JavaVM *vm, void *reserved) {
        __in__();
        _p_vm = NULL;
        __out__();
} 

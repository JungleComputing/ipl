#ifndef __IBIS_IMPL_MP_MPI_PAN_SYS_H__
#define __IBIS_IMPL_MP_MPI_PAN_SYS_H__


typedef struct PAN_IOVEC {
    void       *data;
    int		len;
} pan_iovec_t, *pan_iovec_p;

typedef int	pan_key_t, *pan_key_p;

typedef	void (*pan_clear_p)(void *);

void   *pan_malloc(size_t size);
void	pan_free(void *ptr);
void   *pan_realloc(void *ptr, size_t size);

int	pan_thread_nonblocking(void);

void   *pan_malloc(size_t s);
void   *pan_realloc(void *ptr, size_t s);
void   *pan_calloc(size_t elts, size_t s);
void	pan_free(void *ptr);

#endif

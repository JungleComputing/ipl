#ifndef __IBIS_IPL_IMPL_MESSAGEPASSING_IBP_INTTYPES_H__
#define __IBIS_IPL_IMPL_MESSAGEPASSING_IBP_INTTYPES_H__

#if defined __GNUC__
#include <inttypes.h>

#elif defined _M_IX86
#include <windows.h>
typedef __int8		int8_t;
typedef __int16		int16_t;
typedef __int32		int32_t;
typedef __int64		int64_t;

/* Unsigned.  */
typedef unsigned __int8		uint8_t;
typedef unsigned __int16	uint16_t;
typedef unsigned __int32	uint32_t;
typedef unsigned __int64	uint64_t;

#else
#define __WORDSIZE	32
typedef signed char	int8_t;
typedef short int	int16_t;
typedef int		int32_t;
# if __WORDSIZE == 64
typedef long int	int64_t;
# else
typedef long long int	int64_t;
# endif

/* Unsigned.  */
typedef unsigned char		uint8_t;
typedef unsigned short int	uint16_t;
typedef unsigned int		uint32_t;
#if __WORDSIZE == 64
typedef unsigned long int	uint64_t;
#else
typedef unsigned long long int	uint64_t;
#endif

#endif

#endif

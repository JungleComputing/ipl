#include "ibis_util_nativeCode_Rdtsc.h"
#include <jni.h>

#if _M_IX86 >= 400
#define STRICT
#include <windows.h>
#endif

JNIEXPORT jlong JNICALL Java_ibis_util_nativeCode_Rdtsc_rdtsc(JNIEnv *env, jclass cls)
{
    jlong time;

#ifdef __GNUC__
    __asm__ __volatile__ ("rdtsc" : "=A" (time));

#elif _M_IX86 >= 400
    unsigned __int64 *ret;

    // get high-precision time:
    __try
    {
	unsigned __int64 *dest = (unsigned __int64 *)&time;
	__asm 
	{
	    _emit 0xf        // these two bytes form the 'rdtsc' asm instruction,
	    _emit 0x31       //  available on Pentium I and later.
	    mov esi, dest
	    mov [esi  ], eax    // lower 32 bits of tsc
	    mov [esi+4], edx    // upper 32 bits of tsc
	}
    }
    __except(EXCEPTION_EXECUTE_HANDLER)
    {
	return 0;
    }

#else
    fprintf(stderr, "No RDTSC asm support for this platform\n");
#endif

    return time;
}


JNIEXPORT jfloat JNICALL Java_ibis_util_naticeCode_Rdtsc_getMHz(JNIEnv *env, jclass clazz)
{
#if defined(__linux)
#define HOST_MHZ_DEFAULT 1000.0
    /* code borrowed from Panda 4.0 */
#   define LINE_SIZE       512
    char line[LINE_SIZE];
    FILE *f;
    float time_host_mhz = -1.0;

    f = fopen("/proc/cpuinfo", "r");
    if (f != NULL) {
        while (! feof(f)) {
            fgets(line, LINE_SIZE, f);
            if (sscanf(line, " cpu MHz : %lf", &time_host_mhz) == 1) {
                break;
            }
        }
        if (time_host_mhz == -1.0) {
            while (! feof(f)) {
                fgets(line, LINE_SIZE, f);
                if (sscanf(line, " bogomips : %lf", &time_host_mhz) == 1) {
                    break;
                }
            }
        }
        fclose(f);
    }
    if (time_host_mhz < 0.0) {
        time_host_mhz = HOST_MHZ_DEFAULT;
        fprintf(stderr,
                "Cannot find /proc/cpuinfo, assume clock speed = %.1f MHz\n",
                time_host_mhz);
    }

    return (jfloat)time_host_mhz;

#elif _M_IX86 >= 400
    /* Code borrowed from http://www.geisswerks.com/ryan/FAQS/timing.html */
    HKEY                        hKey; 
    DWORD                       cbBuffer; 
    LONG                        rc; 
    float                       frequency;

    frequency = 0.0;

    rc = RegOpenKeyEx( 
	     HKEY_LOCAL_MACHINE, 
	     "Hardware\\Description\\System\\CentralProcessor\\0", 
	     0, 
	     KEY_READ, 
	     &hKey 
	 ); 

    if (rc == ERROR_SUCCESS) 
    { 
	DWORD freq_mhz;

	cbBuffer = sizeof (freq_mhz);
	rc = RegQueryValueEx 
	     ( 
	       hKey, 
	       "~MHz", 
	       NULL, 
	       NULL, 
	       (LPBYTE)(&freq_mhz), 
	       &cbBuffer 
	     ); 
	if (rc == ERROR_SUCCESS)
	    frequency = freq_mhz;
	RegCloseKey (hKey); 
    } 

    return (jfloat)frequency;

#else
    fprintf(stderr, "No RDTSC asm support for this platform\n");
    return 0.0;
#endif
}

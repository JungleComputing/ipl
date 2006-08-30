#include "ibis_util_nativeCode_Rdtsc.h"
#include <jni.h>

#if (defined _M_IX86)
#include <windows.h>
#elif (defined __POWERPC__)
#include <OmniTimer/OTTimer.h>
#endif

JNIEXPORT jlong JNICALL Java_ibis_util_nativeCode_Rdtsc_rdtsc(JNIEnv *env, jclass cls)
{
    jlong time;

#if defined(__linux) && defined __GNUC__ && defined __i386__
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

#elif (defined _M_IX86)
    QueryPerformanceCounter((LARGE_INTEGER *)&time);

#elif (defined __POWERPC__)
    OTStamp t;

    OTReadCounter(&t);
    return (jlong)t.ull;

#else
    fprintf(stderr, "No RDTSC asm support for this platform\n");
#endif

    return time;
}


/*
 * This call also serves as a static initializer
 */
JNIEXPORT jfloat JNICALL Java_ibis_util_nativeCode_Rdtsc_getMHz(JNIEnv *env, jclass clazz)
{
#if defined(__linux) && defined __GNUC__ && defined __i386__
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
            if (sscanf(line, " cpu MHz : %f", &time_host_mhz) == 1) {
                break;
            }
        }
        if (time_host_mhz == -1.0) {
            while (! feof(f)) {
                fgets(line, LINE_SIZE, f);
                if (sscanf(line, " bogomips : %f", &time_host_mhz) == 1) {
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

#elif (defined _M_IX86)
    __int64   frequency;

    QueryPerformanceFrequency((LARGE_INTEGER *)&frequency);

    return (jfloat)(frequency / 1000000.0);

#elif (defined __POWERPC__)
    double interval;

    OTSetup();
    interval = OTSecondsPerStampUnit();

    return (jfloat)(0.000001 / interval);

#else
    fprintf(stderr, "No RDTSC asm support for this platform\n");
    return 0.0;
#endif
}

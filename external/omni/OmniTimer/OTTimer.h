// Copyright 1997-2001 Omni Development, Inc.  All rights reserved.
//
// This software may only be used and reproduced according to the
// terms in the file OmniSourceLicense.html, which should be
// distributed with this project and can also be found at
// http://www.omnigroup.com/DeveloperResources/OmniSourceLicense.html.
//
// $Header$

#ifndef __OTTIMER_H__
#define __OTTIMER_H__

#ifdef __cplusplus
extern "C" {
#endif

// An easy way to store time stamp information.
typedef union _OTStamp {
   unsigned long long ull;
   struct _ul {
#ifdef __LITTLE_ENDIAN__
       unsigned long low;
       unsigned long high;
#elif __BIG_ENDIAN__
       unsigned long high;
       unsigned long low;
#else
#error Endianness not defined
#endif
   } ul;
} OTStamp;

// Convenient way to zero an OTStamp.
static inline void OTZeroStamp(OTStamp *stamp)
{
    stamp->ul.low  = 0;
    stamp->ul.high = 0;
}


// These are implemented in separate files for each architecture.

extern void OTSetup();
    // Configures the architecture dependant timer support

extern void OTReadCounter(OTStamp *stamp);
    // Reads the processor time stamp counter.

extern void OTDeltaTimers(const OTStamp *startTimer, const OTStamp *endTimer, OTStamp *deltaTimer);
    // Computes the delta between a start and stop timer.

extern void OTSumTimers(const OTStamp *timer1, const OTStamp *timer2, OTStamp *sumTimer);
extern void OTAddDeltaSinceStart(const OTStamp *start, OTStamp *sum);
extern void OTAddDeltaSinceStartAndRecordTime(const OTStamp *start, OTStamp *sum, OTStamp *currentTime);

extern double OTSecondsPerStampUnit();
    // Returns the number of seconds represented by an increment of one in a stamp
    
// These are implemented in C, in OTTimer.c.
double OTStampToSeconds(const OTStamp *stamp);

#ifdef __cplusplus
}
#endif

#endif __OTTIMER_H__

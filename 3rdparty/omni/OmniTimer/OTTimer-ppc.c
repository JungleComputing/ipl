// Copyright 1997-2001 Omni Development, Inc.  All rights reserved.
//
// This software may only be used and reproduced according to the
// terms in the file OmniSourceLicense.html, which should be
// distributed with this project and can also be found at
// http://www.omnigroup.com/DeveloperResources/OmniSourceLicense.html.

#ifdef __ppc__

#include "OTTimer.h"

#include <sys/time.h>

static OTStamp        startStamp;
static struct timeval startTime;

void OTSetup()
{
    gettimeofday(&startTime, NULL);
    OTReadCounter(&startStamp);
}

void OTReadCounter(OTStamp *stamp)
{
    unsigned long high1, high2, low;

    asm volatile (
    "mftbu %0\n"
    "mftb  %1\n"
    "mftbu %2\n"
    "cmpw  %3,%4\n"
    "bne-  $-16\n"
    : "=r" (high1), "=r" (low), "=r" (high2)
    : "0" (high1), "2" (high2));
    
    stamp->ul.high = high1;
    stamp->ul.low = low;
}

void OTDeltaTimers(const OTStamp *startTimer, const OTStamp *endTimer, OTStamp *deltaTimer)
{
    deltaTimer->ull = endTimer->ull - startTimer->ull;
}

void OTSumTimers(const OTStamp *timer1, const OTStamp *timer2, OTStamp *sumTimer)
{
    sumTimer->ull = timer1->ull + timer2->ull;
}
void OTAddDeltaSinceStart(const OTStamp *start, OTStamp *sum)
{
    OTStamp tmp;
    
    OTReadCounter(&tmp);
    sum->ull += (tmp.ull - start->ull);
}

void OTAddDeltaSinceStartAndRecordTime(const OTStamp *start, OTStamp *sum, OTStamp *currentTime)
{
    OTReadCounter(currentTime);
    sum->ull += (currentTime->ull - start->ull);
}

double OTSecondsPerStampUnit()
{
    OTStamp        endStamp;
    struct timeval endTime;
    double         seconds;
    
    gettimeofday(&endTime, NULL);
    OTReadCounter(&endStamp);
    
    seconds  = (endTime.tv_sec * 1.0e6 + endTime.tv_usec) - (startTime.tv_sec * 1.0e6 + startTime.tv_usec);
    seconds /= 1.0e6;
    
    return seconds / (endStamp.ull - startStamp.ull);
}

#endif

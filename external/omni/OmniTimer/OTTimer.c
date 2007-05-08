// Copyright 1997-2001 Omni Development, Inc.  All rights reserved.
//
// This software may only be used and reproduced according to the
// terms in the file OmniSourceLicense.html, which should be
// distributed with this project and can also be found at
// http://www.omnigroup.com/DeveloperResources/OmniSourceLicense.html.

#include "OTTimer.h"

static int    conversionFactorSet = 0;
static double secondsPerStampUnit = 0.0;
static double highFactor = 4294967296.0;

double OTStampToSeconds(const OTStamp *stamp)
{
    double steps;

    if (!conversionFactorSet) {
        conversionFactorSet = 1;
        secondsPerStampUnit = OTSecondsPerStampUnit();
    }
    
    steps = (double)stamp->ul.high * highFactor + (double)stamp->ul.low;
    return steps * secondsPerStampUnit;
}

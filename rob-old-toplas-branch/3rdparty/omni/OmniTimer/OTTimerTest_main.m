// Copyright 1997-2001 Omni Development, Inc.  All rights reserved.
//
// This software may only be used and reproduced according to the
// terms in the file OmniSourceLicense.html, which should be
// distributed with this project and can also be found at
// http://www.omnigroup.com/DeveloperResources/OmniSourceLicense.html.

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

#include <OmniTimer/OmniTimer.h>

// Serves as a simple example, and a test to make sure the framework is actually
// work properly on the current architecture.

#define DURATION (5)

void testBasicStamps()
{
    OTStamp start, end, delta;
    
    OTReadCounter(&start);
    sleep(DURATION);
    OTReadCounter(&end);

    printf("start   = %qu\n", start.ull);
    printf("end     = %qu\n", end.ull);

    OTDeltaTimers(&start, &end, &delta);
    printf("delta   = %qd\n", delta.ull);

    printf("steps per seconds = %f\n", (float)delta.ull / (float)DURATION);
    printf("time = %f\n", OTStampToSeconds(&delta));
}

void testStack()
{
    OTStackNode *root, *parent1, *parent2, *child;
    
    root = OTStackNodeCreate("root");
    parent1 = OTStackNodeCreate("parent1");
    parent2 = OTStackNodeCreate("parent2");
    child = OTStackNodeCreate("child");

    OTStackPushRoot(root);
      sleep(1);
      OTStackPush(parent1);
        sleep(1);
        OTStackPush(child);
          sleep(1);
        OTStackPop();
      OTStackPop();
      OTStackPush(parent2);
        sleep(1);
        OTStackPush(child);
          sleep(1);
        OTStackPop();
      OTStackPop();
      
    OTStackPopRoot();
    
    OTStackReportResults(NULL);
}



int main(int argc, char *argv[])
{
    OTSetup();
    
    //testBasicStamps();
    testStack();
    
    return 0;
}


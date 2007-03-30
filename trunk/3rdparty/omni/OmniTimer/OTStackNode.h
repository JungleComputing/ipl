// Copyright 1997-2001 Omni Development, Inc.  All rights reserved.
//
// This software may only be used and reproduced according to the
// terms in the file OmniSourceLicense.html, which should be
// distributed with this project and can also be found at
// http://www.omnigroup.com/DeveloperResources/OmniSourceLicense.html.
//
// $Header$

#ifndef __OTSTACKNODE_H__
#define __OTSTACKNODE_H__

#ifdef __cplusplus
extern "C" {
#endif

#include <OmniTimer/OTTimer.h>
#include <stdio.h>

/*
OTStackNode takes a different approach to collecting time information.  Instead of building a tree of nodes as in OTTimerNode, each node is self contained.  Each time a node is started, the currently running node is stopped and the new node is made the current node.  When a node is stopped, the previously running node is started.  So, each node only reports the time that it was on the top of the stack, i.e., the time spent 'locally'.

This approach works better for systems that have non-tree architectures.  For example, you might have a common routine that can be called from many locations.  You may or may not want to know what percentage of the calls are due to a single call site.  If you just want the total time spent in the routine, this approach will give you that, while OTTimerNode will give you parent to child relationships.

Note that in order to avoid 'if' statements ever time a node is started and stopped, the root of the stack is treated specially -- you must call OTStackStartRoot() and OTStackStopRoot() for the root node to avoid a crash.  Likewise, you must call OTStackStart() and OTStackStop() on non-root nodes to avoid screwing stuff up.
*/


typedef struct _OTStackNode {
    struct _OTStackNode   *previous;
    const char            *name;
    OTStamp                sum;
    OTStamp                lastStart;
    unsigned int           startCount;
} OTStackNode;

// Don't mess with this directly -- this is just here for the inlines
extern OTStackNode *_OTStackTop;


extern OTStackNode *OTStackNodeCreate(const char *name);
extern void         OTStackNodeDestroy(OTStackNode *node);

// These operator on ALL stack nodes -- we don't currently support disjoint groups of stack nodes
extern void         OTStackReset();
extern void         OTStackReportResults(FILE *file);


static inline void OTStackPushRoot(OTStackNode *node)
{
    OTReadCounter(&node->lastStart);
    node->startCount++;
    _OTStackTop = node;
}

static inline void OTStackPopRoot()
{
    OTStackNode *node;
    
    node = _OTStackTop;
    _OTStackTop = NULL;
    OTAddDeltaSinceStart(&node->lastStart, &node->sum);
}

static inline void OTStackPush(OTStackNode *node)
{
    OTAddDeltaSinceStartAndRecordTime(&_OTStackTop->lastStart, &_OTStackTop->sum, &node->lastStart);
    node->startCount++;
    node->previous = _OTStackTop;
    _OTStackTop = node;
}

static inline void OTStackPop()
{
    OTStackNode *node;
    
    node = _OTStackTop;
    _OTStackTop = node->previous;
    
    OTAddDeltaSinceStartAndRecordTime(&node->lastStart, &node->sum, &_OTStackTop->lastStart);
}

#ifdef __cplusplus
}
#endif

#endif // __OTSTACKNODE_H__

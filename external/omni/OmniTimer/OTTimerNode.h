// Copyright 1997-2001 Omni Development, Inc.  All rights reserved.
//
// This software may only be used and reproduced according to the
// terms in the file OmniSourceLicense.html, which should be
// distributed with this project and can also be found at
// http://www.omnigroup.com/DeveloperResources/OmniSourceLicense.html.
//
// $Header$

#ifndef __OTTIMERNODE_H__
#define __OTTIMERNODE_H__

#ifdef __cplusplus
extern "C" {
#endif

#include <OmniTimer/OTTimer.h>

// This can be defined when building the framework to enable checking of nesting
// of timers.  For the least error in timing measurements, though, this should
// be turned off.
#define OT_DEBUG_NESTING


typedef struct _OTTimerNode {
    struct _OTTimerNode           *parentNode;
    const char                    *name;
    unsigned int                   childCount;
    struct _OTTimerNode          **children;
    OTStamp                        sum;
    OTStamp                        lastStart;
    unsigned int                   startCount;
} OTTimerNode;

#ifdef OT_DEBUG_NESTING
extern void OTIllegalNestingBreak();

extern OTTimerNode *_OTCurrentNode;
extern void  OTIllegalNesting(const OTTimerNode *node, const char *operation);
extern void  OTPrintTimerName(const OTTimerNode *node);
#endif

extern void         OTEnable(int yn);

extern OTTimerNode *OTTimerNodeCreate(const char *name, OTTimerNode *parentNode);
extern void         OTTimerNodeDestroy(OTTimerNode *node);
extern void         OTTimerNodeReset(OTTimerNode *node);

extern void OTReportResults(OTTimerNode *root);



static inline void OTNodeStart(OTTimerNode *node)
{
#ifdef OT_DEBUG_NESTING
    if (_OTCurrentNode != node->parentNode)
        OTIllegalNesting(node, "start");
    _OTCurrentNode = node;
#endif
    OTReadCounter(&node->lastStart);
    node->startCount++;
}

static inline void OTNodeStop(OTTimerNode *node)
{
    OTAddDeltaSinceStart(&node->lastStart, &node->sum);
#ifdef OT_DEBUG_NESTING
    if (_OTCurrentNode != node)
        OTIllegalNesting(node, "stop");
    _OTCurrentNode = node->parentNode;
#endif
}

#ifdef __cplusplus
}
#endif

#endif __OTTIMERNODE_H__

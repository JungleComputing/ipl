// Copyright 1997-2001 Omni Development, Inc.  All rights reserved.
//
// This software may only be used and reproduced according to the
// terms in the file OmniSourceLicense.html, which should be
// distributed with this project and can also be found at
// http://www.omnigroup.com/DeveloperResources/OmniSourceLicense.html.

#include <OmniTimer/OTStackNode.h>
#include <stdlib.h>

OTStackNode *_OTStackTop = NULL;

static OTStackNode **_OTAllStackNodes = NULL;
static unsigned int  _OTStackNodeCount = 0;

OTStackNode *OTStackNodeCreate(const char *name)
{
    OTStackNode *node;
    
    node = calloc(1, sizeof(*node));
    node->name = name;
    
    // This won't get called all that often, so don't worry about doing doubling reallocs
    if (!_OTAllStackNodes) {
        _OTAllStackNodes = malloc(sizeof(*_OTAllStackNodes));
    } else {
        _OTAllStackNodes = realloc(_OTAllStackNodes, (_OTStackNodeCount + 1) * sizeof(*_OTAllStackNodes));
    }
    
    _OTAllStackNodes[_OTStackNodeCount] = node;
    _OTStackNodeCount++;
    
    return node;
}

void OTStackNodeDestroy(OTStackNode *node)
{
    unsigned int nodeIndex;
    
    for (nodeIndex = 0; nodeIndex < _OTStackNodeCount; nodeIndex++) {
        if (_OTAllStackNodes[nodeIndex] == node) {
            free(node);
            _OTAllStackNodes[nodeIndex] = _OTAllStackNodes[_OTStackNodeCount - 1];
            _OTStackNodeCount--;
            return;
        }
    }
}

void OTStackReset()
{
    unsigned int nodeIndex;
    
    if (_OTStackTop) {
        fprintf(stderr, "*** Attempt to reset OmniTimer stack while node is running.  This may produced weird results.\n");
    }
    
    for (nodeIndex = 0; nodeIndex < _OTStackNodeCount; nodeIndex++) {
        OTZeroStamp(&_OTAllStackNodes[nodeIndex]->sum);
        _OTAllStackNodes[nodeIndex]->startCount = 0;
    }
}

static int _OTCompareNodesByTime(const void *a, const void *b)
{
    const OTStackNode *nodeA = *(const OTStackNode **)a;
    const OTStackNode *nodeB = *(const OTStackNode **)b;
    
    if (nodeA->sum.ull > nodeB->sum.ull)
        return 1;
    else if (nodeA->sum.ull < nodeB->sum.ull)
        return -1;
    return 0;
}

void OTStackReportResults(FILE *file)
{
    unsigned int nodeIndex;
    double totalTime;

    if (!file)
        file = stderr;
        
    if (_OTStackTop) {
        fprintf(file, "*** Attempt to report OmniTimer stack while node is running.  This may produced weird results.\n");
    }
    
    totalTime = 0.0;
    for (nodeIndex = 0; nodeIndex < _OTStackNodeCount; nodeIndex++)
        totalTime += OTStampToSeconds(&_OTAllStackNodes[nodeIndex]->sum);
    
    qsort(_OTAllStackNodes, _OTStackNodeCount, sizeof(*_OTAllStackNodes), _OTCompareNodesByTime);

    fprintf(file, "\n[Stack nodes in percentage order]\n");
    nodeIndex = _OTStackNodeCount;
    while (nodeIndex--) {
        OTStackNode *node;
        double time;
        
        node = _OTAllStackNodes[nodeIndex];
        time = OTStampToSeconds(&node->sum);
        fprintf(stderr, "[%0.2f sec, %0.2f%%, %d calls, %f avg] -- %s\n",
                time,
                time / totalTime * 100.0,
                node->startCount,
                time / node->startCount,
                node->name);
    }
}

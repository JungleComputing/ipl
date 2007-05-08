// Copyright 1997-2001 Omni Development, Inc.  All rights reserved.
//
// This software may only be used and reproduced according to the
// terms in the file OmniSourceLicense.html, which should be
// distributed with this project and can also be found at
// http://www.omnigroup.com/DeveloperResources/OmniSourceLicense.html.

#include <OmniTimer/OTTimerNode.h>
#include <stdio.h>
#include <stdlib.h>

static int OTEnabled = 1;

#ifdef OT_DEBUG_NESTING

OTTimerNode *_OTCurrentNode = NULL;


void OTPrintTimerName(const OTTimerNode *node)
{
    if (node)
        fprintf(stderr, "%s", node->name);
    else
        fprintf(stderr, "*NIL*");
}

void OTIllegalNestingBreak()
{
}

void OTIllegalNesting(const OTTimerNode *node, const char *operation)
{
    if (!OTEnabled)
        return;
    
    fprintf(stderr, "Illegal nesting of timer nodes.  Break on OTIllegalNestingBreak() to debug.\n");
    fprintf(stderr, "  Attempted to %s timer ", operation);
    OTPrintTimerName(node);
    if (node) {
        fprintf(stderr, " with parent ");
        OTPrintTimerName(node->parentNode);
    }
    fprintf(stderr, "\nCurrent node is ");
    OTPrintTimerName(_OTCurrentNode);
    fprintf(stderr, "\n");
    OTIllegalNestingBreak();
}

#endif


static void _OTTimerAddChild(OTTimerNode *parent, OTTimerNode *child)
{
    if (parent->children)
        parent->children = realloc(parent->children, sizeof(*parent->children) * (parent->childCount + 1));
    else
        parent->children = malloc(sizeof(*parent->children));
        
    parent->children[parent->childCount] = child;
    parent->childCount++;
}

static void _OTTimerRemoveChild(OTTimerNode *parent, OTTimerNode *child)
{
     unsigned int childIndex;
     
     for (childIndex = 0; childIndex < parent->childCount; childIndex++) {
         if (parent->children[childIndex] == child) {
             parent->children[childIndex] = parent->children[parent->childCount - 1];
             parent->childCount--;
             break;
         }
     }
}

void OTEnable(int yn)
{
    OTEnabled = yn;
}

OTTimerNode *OTTimerNodeCreate(const char *name, OTTimerNode *parentNode)
{
    OTTimerNode *node;
    
    node = calloc(1, sizeof(*node));
    
    node->name       = name;
    node->parentNode = parentNode;
    if (parentNode)
        _OTTimerAddChild(parentNode, node);
    OTZeroStamp(&node->sum);
    node->startCount = 0;
    
    return node;
}


void OTTimerNodeDestroy(OTTimerNode *node)
{
    if (node->parentNode)
        _OTTimerRemoveChild(node->parentNode, node);
    free(node);
}

static void _OTTimerNodeReset(OTTimerNode *node)
{
    unsigned int childIndex;
    
    OTZeroStamp(&node->sum);
    node->startCount = 0;
    
    for (childIndex = 0; childIndex < node->childCount; childIndex++)
        _OTTimerNodeReset(node->children[childIndex]);
}

void OTTimerNodeReset(OTTimerNode *root)
{
    if (root->parentNode) {
        fprintf(stderr, "OTTimerNodeReset called on a non-root node.   This is questionable since it won't remove the time from the parent node!\n"); 
    }
    _OTTimerNodeReset(root);
}

typedef struct _OTNodeList {
    OTTimerNode  **nodes;
    unsigned int   count;
    unsigned int   size;
} OTNodeList;

static double _OTReportResults(OTTimerNode *node, double parentTime, double totalTime, unsigned int depth, OTNodeList *leaves)
{
    double myTime, totalChildTime;
    unsigned int i;
    unsigned int childIndex;
    
    for (i = 0; i < depth; i++)
        fputc('\t', stderr);

    myTime = OTStampToSeconds(&node->sum);
    totalChildTime = 0.0;

    fprintf(stderr, "[%f, %0.2f%%, %0.2f%%, %d] %s\n",
            myTime,
            100.0 * (myTime / parentTime),
            100.0 * (myTime / totalTime),
            node->startCount,
            node->name);

    if (node->childCount) {
        for (childIndex = 0; childIndex < node->childCount; childIndex++) {
            OTTimerNode *childTimer;
    
            childTimer = node->children[childIndex];
            totalChildTime += _OTReportResults(childTimer, myTime, totalTime, depth + 1, leaves);
        }

        for (i = 0; i < depth; i++)
            fputc('\t', stderr);
        fprintf(stderr, "[%0.2f%% spent locally]\n", 100 * (myTime - totalChildTime) / myTime);
    } else {
        if (leaves->count >= leaves->size) {
            leaves->size *= 2;
            leaves->nodes = realloc(leaves->nodes, leaves->size * sizeof(*leaves->nodes));
        }
        leaves->nodes[leaves->count] = node;
        leaves->count++;
    }

    return myTime;
}

static int _OTCompareNodesByTime(const void *a, const void *b)
{
    const OTTimerNode **nodeA = (const OTTimerNode **)a;
    const OTTimerNode **nodeB = (const OTTimerNode **)b;
    
    if ((*nodeA)->sum.ull > (*nodeB)->sum.ull)
        return 1;
    else if ((*nodeA)->sum.ull < (*nodeB)->sum.ull)
        return -1;
    return 0;
}


void OTReportResults(OTTimerNode *root)
{
    double totalTime;
    OTNodeList leaves;
    unsigned int leafIndex;
    
    if (!OTEnabled)
        return;

    fprintf(stderr, "[seconds spent locally, %% of parent, %% of total, # of samples]\n");
    totalTime = OTStampToSeconds(&root->sum);
    
    leaves.size = 1;
    leaves.count = 0;
    leaves.nodes = malloc(leaves.size * sizeof(*leaves.nodes));
    
    _OTReportResults(root, totalTime, totalTime, 0, &leaves);
    
    qsort(leaves.nodes, leaves.count, sizeof(*leaves.nodes), _OTCompareNodesByTime);
    
    fprintf(stderr, "\n[Leaves in percentage order]\n");
    leafIndex = leaves.count;
    while (leafIndex--) {
        OTTimerNode *leaf;
        
        leaf = leaves.nodes[leafIndex];
        fprintf(stderr, "[%0.2f sec, %0.2f%%, %d calls] -- %s\n",
                OTStampToSeconds(&leaf->sum),
                OTStampToSeconds(&leaf->sum) / totalTime * 100.0,
                leaf->startCount, leaf->name);
    }
    
    free(leaves.nodes);
}



/* $Id$ */

package ibis.satin.impl;

class StatsMessage implements java.io.Serializable {
    long spawns;

    long syncs;

    long aborts;

    long jobsExecuted;

    long abortedJobs;

    long abortMessages;

    long stealAttempts;

    long stealSuccess;

    long asyncStealAttempts;

    long asyncStealSuccess;

    long tupleMsgs;

    long tupleBytes;

    long stolenJobs;

    long stealRequests;

    long interClusterMessages;

    long intraClusterMessages;

    long interClusterBytes;

    long intraClusterBytes;

    double stealTime;

    double handleStealTime;

    double abortTime;

    double idleTime;

    long idleCount;

    double pollTime;

    long pollCount;

    double tupleTime;

    double handleTupleTime;

    double invocationRecordWriteTime;

    long invocationRecordWriteCount;

    double returnRecordWriteTime;

    long returnRecordWriteCount;

    double invocationRecordReadTime;

    long invocationRecordReadCount;

    double returnRecordReadTime;

    long returnRecordReadCount;

    double tupleWaitTime;

    long tupleWaitCount;

    double tupleSeqTime;

    long tupleSeqCount;

    long returnRecordBytes;

    //fault tolerance
    long tableResultUpdates;

    long tableLockUpdates;

    long tableUpdateMessages;

    long tableLookups;

    long tableSuccessfulLookups;

    long tableRemoteLookups;

    long killedOrphans;

    long restartedJobs;

    double tableLookupTime;

    double tableUpdateTime;

    double tableHandleUpdateTime;

    double tableHandleLookupTime;

    double tableSerializationTime;

    double tableDeserializationTime;

    double tableCheckTime;

    double crashHandlingTime;

    double addReplicaTime;

    //shared objects
    long soInvocations;

    long soInvocationsBytes;

    long soTransfers;

    long soTransfersBytes;

    double broadcastSOInvocationsTime;
    
    double handleSOInvocationsTime;

    double soTransferTime;

    double handleSOTransferTime;
    
    double soSerializationTime;

    double soDeserializationTime;

    long soRealMessageCount;
    
    void add(StatsMessage s) {
        spawns += s.spawns;
        jobsExecuted += s.jobsExecuted;
        syncs += s.syncs;
        aborts += s.aborts;
        abortMessages += s.abortMessages;
        abortedJobs += s.abortedJobs;

        stealAttempts += s.stealAttempts;
        stealSuccess += s.stealSuccess;
        asyncStealAttempts += s.asyncStealAttempts;
        asyncStealSuccess += s.asyncStealSuccess;
        tupleMsgs += s.tupleMsgs;
        tupleBytes += s.tupleBytes;
        stolenJobs += s.stolenJobs;
        stealRequests += s.stealRequests;
        interClusterMessages += s.interClusterMessages;
        intraClusterMessages += s.intraClusterMessages;
        interClusterBytes += s.interClusterBytes;
        intraClusterBytes += s.intraClusterBytes;

        stealTime += s.stealTime;
        handleStealTime += s.handleStealTime;
        abortTime += s.abortTime;
        idleTime += s.idleTime;
        idleCount += s.idleCount;
        pollTime += s.pollTime;
        pollCount += s.pollCount;
        tupleTime += s.tupleTime;
	handleTupleTime += s.handleTupleTime;
        invocationRecordWriteTime += s.invocationRecordWriteTime;
        invocationRecordWriteCount += s.invocationRecordWriteCount;
        invocationRecordReadTime += s.invocationRecordReadTime;
        invocationRecordReadCount += s.invocationRecordReadCount;
        returnRecordWriteTime += s.returnRecordWriteTime;
        returnRecordWriteCount += s.returnRecordWriteCount;
        returnRecordReadTime += s.returnRecordReadTime;
        returnRecordReadCount += s.returnRecordReadCount;
        tupleWaitTime += s.tupleWaitTime;
        tupleWaitCount += s.tupleWaitCount;
        tupleSeqTime += s.tupleSeqTime;
        tupleSeqCount += s.tupleSeqCount;
	returnRecordBytes += s.returnRecordBytes;

        //fault tolerance
        tableResultUpdates += s.tableResultUpdates;
        tableLockUpdates += s.tableLockUpdates;
        tableUpdateMessages += s.tableUpdateMessages;
        tableLookups += s.tableLookups;
        tableSuccessfulLookups += s.tableSuccessfulLookups;
        tableRemoteLookups += s.tableRemoteLookups;
        killedOrphans += s.killedOrphans;
        restartedJobs += s.restartedJobs;

        tableLookupTime += s.tableLookupTime;
        tableUpdateTime += s.tableUpdateTime;
        tableHandleUpdateTime += s.tableHandleUpdateTime;
        tableHandleLookupTime += s.tableHandleLookupTime;
        tableSerializationTime += s.tableSerializationTime;
        tableDeserializationTime += s.tableDeserializationTime;
        tableCheckTime += s.tableCheckTime;
        crashHandlingTime += s.crashHandlingTime;
        addReplicaTime += s.addReplicaTime;

	//shared objects
	soInvocations += s.soInvocations;
	soInvocationsBytes += s.soInvocationsBytes;
	soTransfers += s.soTransfers;
	soTransfersBytes += s.soTransfersBytes;
	broadcastSOInvocationsTime += s.broadcastSOInvocationsTime;
	handleSOInvocationsTime += s.handleSOInvocationsTime;
	soTransferTime += s.soTransferTime;
	handleSOTransferTime += s.handleSOTransferTime;
	soSerializationTime += s.soSerializationTime;
	soDeserializationTime += s.soDeserializationTime;
    }
}

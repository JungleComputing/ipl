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

    void add(StatsMessage s) {
	spawns += s.spawns;
	jobsExecuted += s.jobsExecuted;
	syncs += s.syncs;
	aborts += s.aborts;
	abortMessages += s.abortMessages;
	abortedJobs += s.abortedJobs;

	stealAttempts += s.stealAttempts;
	stealSuccess += s.stealSuccess;
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
    }
}

package ibis.satin;

class SatinStats implements java.io.Serializable {
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
	double invocationRecordReadTime;
	long invocationRecordReadCount;

	void add(SatinStats s) {
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
	}
}

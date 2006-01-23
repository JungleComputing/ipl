/* $Id$ */

package ibis.satin.impl;

import ibis.util.Timer;

public abstract class Stats extends SharedObjects {

    protected StatsMessage createStats() {
        StatsMessage s = new StatsMessage();

        s.spawns = spawns;
        s.jobsExecuted = jobsExecuted;
        s.syncs = syncs;
        s.aborts = aborts;
        s.abortMessages = abortMessages;
        s.abortedJobs = abortedJobs;

        s.stealAttempts = stealAttempts;
        s.stealSuccess = stealSuccess;
        s.tupleMsgs = tupleMsgs;
        s.tupleBytes = tupleBytes;
        s.stolenJobs = stolenJobs;
        s.stealRequests = stealRequests;
        s.interClusterMessages = interClusterMessages;
        s.intraClusterMessages = intraClusterMessages;
        s.interClusterBytes = interClusterBytes;
        s.intraClusterBytes = intraClusterBytes;

        s.stealTime = stealTimer.totalTimeVal();
        s.handleStealTime = handleStealTimer.totalTimeVal();
        s.abortTime = abortTimer.totalTimeVal();
        s.idleTime = idleTimer.totalTimeVal();
        s.idleCount = idleTimer.nrTimes();
        s.pollTime = pollTimer.totalTimeVal();
        s.pollCount = pollTimer.nrTimes();
        s.tupleTime = tupleTimer.totalTimeVal();
        s.handleTupleTime = handleTupleTimer.totalTimeVal();
        s.tupleWaitTime = tupleOrderingWaitTimer.totalTimeVal();
        s.tupleWaitCount = tupleOrderingWaitTimer.nrTimes();

        s.invocationRecordWriteTime = invocationRecordWriteTimer.totalTimeVal();
        s.invocationRecordWriteCount = invocationRecordWriteTimer.nrTimes();
        s.invocationRecordReadTime = invocationRecordReadTimer.totalTimeVal();
        s.invocationRecordReadCount = invocationRecordReadTimer.nrTimes();

        s.returnRecordWriteTime = returnRecordWriteTimer.totalTimeVal();
        s.returnRecordWriteCount = returnRecordWriteTimer.nrTimes();
        s.returnRecordReadTime = returnRecordReadTimer.totalTimeVal();
        s.returnRecordReadCount = returnRecordReadTimer.nrTimes();

        s.returnRecordBytes = returnRecordBytes;

        //fault tolerance
        if (FAULT_TOLERANCE) {
            s.tableResultUpdates = globalResultTable.numResultUpdates;
            s.tableLockUpdates = globalResultTable.numLockUpdates;
            s.tableUpdateMessages = globalResultTable.numUpdateMessages;
            s.tableLookups = globalResultTable.numLookups;
            s.tableSuccessfulLookups = globalResultTable.numLookupsSucceded;
            s.tableRemoteLookups = globalResultTable.numRemoteLookups;
            s.killedOrphans = killedOrphans;
            s.restartedJobs = restartedJobs;

            s.tableLookupTime = lookupTimer.totalTimeVal();
            s.tableUpdateTime = updateTimer.totalTimeVal();
            s.tableHandleUpdateTime = handleUpdateTimer.totalTimeVal();
            s.tableHandleLookupTime = handleLookupTimer.totalTimeVal();
            s.tableSerializationTime = tableSerializationTimer.totalTimeVal();
            s.tableDeserializationTime = tableDeserializationTimer
                .totalTimeVal();
            s.tableCheckTime = redoTimer.totalTimeVal();
            s.crashHandlingTime = crashTimer.totalTimeVal();
            s.addReplicaTime = addReplicaTimer.totalTimeVal();
        }

        if (SHARED_OBJECTS) {
            s.soInvocations = soInvocations;
            s.soInvocationsBytes = soInvocationsBytes;
            s.soTransfers = soTransfers;
            s.soTransfersBytes = soTransfersBytes;
            s.handleSOInvocationsTime = handleSOInvocationsTimer.totalTimeVal();
            s.broadcastSOInvocationsTime = broadcastSOInvocationsTimer
                .totalTimeVal();
            s.soTransferTime = soTransferTimer.totalTimeVal();
            s.soSerializationTime = soSerializationTimer.totalTimeVal();
            s.soDeserializationTime = soDeserializationTimer.totalTimeVal();
            s.soBcastTime = soBroadcastTransferTimer.totalTimeVal();
            s.soBcastSerializationTime = soBroadcastSerializationTimer.totalTimeVal();
            s.soBcastDeserializationTime = soBroadcastDeserializationTimer.totalTimeVal();
            s.soRealMessageCount = soRealMessageCount;
            s.soBcasts = soBcasts;
            s.soBcastBytes = soBcastBytes;
        }

        return s;
    }

    private double perStats(double tm, long cnt) {
        if (cnt == 0) {
            return 0.0;
        }
        return tm / cnt;
    }

    protected void printStats() {
        int size;

        synchronized (this) {
            // size = victims.size();
            // No, this is one too few. (Ceriel)
            size = victims.size() + 1;
        }

        // add my own stats
        StatsMessage me = createStats();
        totalStats.add(me);

        java.text.NumberFormat nf = java.text.NumberFormat.getInstance();
        // pf.setMaximumIntegerDigits(3);
        // pf.setMinimumIntegerDigits(3);

        // for percentages
        java.text.NumberFormat pf = java.text.NumberFormat.getInstance();
        pf.setMaximumFractionDigits(3);
        pf.setMinimumFractionDigits(3);
        pf.setGroupingUsed(false);

        out.println("-------------------------------SATIN STATISTICS------"
            + "--------------------------");
        if (SPAWN_STATS) {
            out.println("SATIN: SPAWN:       " + nf.format(totalStats.spawns)
                + " spawns, " + nf.format(totalStats.jobsExecuted)
                + " executed, " + nf.format(totalStats.syncs) + " syncs");
            if (ABORTS) {
                out.println("SATIN: ABORT:       "
                    + nf.format(totalStats.aborts) + " aborts, "
                    + nf.format(totalStats.abortMessages) + " abort msgs, "
                    + nf.format(totalStats.abortedJobs) + " aborted jobs");
            }
        }

        if (TUPLE_STATS) {
            out.println("SATIN: TUPLE_SPACE: "
                + nf.format(totalStats.tupleMsgs) + " bcasts, "
                + nf.format(totalStats.tupleBytes) + " bytes");
        }

        if (POLL_FREQ != 0 && POLL_TIMING) {
            out.println("SATIN: POLL:        poll count = "
                + nf.format(totalStats.pollCount));
        }

        if (IDLE_TIMING) {
            out.println("SATIN: IDLE:        idle count = "
                + nf.format(totalStats.idleCount));
        }

        if (STEAL_STATS) {
            out.println("SATIN: STEAL:       "
                + nf.format(totalStats.stealAttempts)
                + " attempts, "
                + nf.format(totalStats.stealSuccess)
                + " successes ("
                + pf.format(perStats(totalStats.stealSuccess,
                    totalStats.stealAttempts) * 100.0) + " %)");

            if (totalStats.asyncStealAttempts != 0) {
                out.println("SATIN: ASYNCSTEAL:   "
                    + nf.format(totalStats.asyncStealAttempts)
                    + " attempts, "
                    + nf.format(totalStats.asyncStealSuccess)
                    + " successes ("
                    + pf.format(perStats(totalStats.asyncStealSuccess,
                        totalStats.asyncStealAttempts) * 100.0) + " %)");
            }

            out.println("SATIN: MESSAGES:    intra "
                + nf.format(totalStats.intraClusterMessages) + " msgs, "
                + nf.format(totalStats.intraClusterBytes) + " bytes; inter "
                + nf.format(totalStats.interClusterMessages) + " msgs, "
                + nf.format(totalStats.interClusterBytes) + " bytes");
        }

        if (FAULT_TOLERANCE && GRT_STATS) {
            out.println("SATIN: GLOBAL_RESULT_TABLE: result updates "
                + nf.format(totalStats.tableResultUpdates)
                + ",update messages "
                + nf.format(totalStats.tableUpdateMessages) + ", lock updates "
                + nf.format(totalStats.tableLockUpdates) + ",lookups "
                + nf.format(totalStats.tableLookups) + ",successful "
                + nf.format(totalStats.tableSuccessfulLookups) + ",remote "
                + nf.format(totalStats.tableRemoteLookups));
        }

        if (FAULT_TOLERANCE && FT_STATS) {
            out.println("SATIN: FAULT_TOLERANCE: killed orphans "
                + nf.format(totalStats.killedOrphans));
            out.println("SATIN: FAULT_TOLERANCE: restarted jobs "
                + nf.format(totalStats.restartedJobs));
        }

        if (SHARED_OBJECTS && SO_STATS) {
            out.println("SATIN: SO_CALLS:    "
                + nf.format(totalStats.soInvocations) + " invocations, " 
                + nf.format(totalStats.soInvocationsBytes) + " bytes, "
                + nf.format(totalStats.soRealMessageCount) + " messages");
            out.println("SATIN: SO_TRANSFER: " 
                + nf.format(totalStats.soTransfers) + " transfers, "  
                + nf.format(totalStats.soTransfersBytes) + " bytes ");
            out.println("SATIN: SO_BCAST:    " 
                + nf.format(totalStats.soBcasts) + " bcasts, "  
                + nf.format(totalStats.soBcastBytes) + " bytes ");
        }

        out.println("-------------------------------SATIN TOTAL TIMES"
            + "-------------------------------");
        if (STEAL_TIMING) {
            out.println("SATIN: STEAL_TIME:               total "
                + Timer.format(totalStats.stealTime)
                + " time/req    "
                + Timer.format(perStats(totalStats.stealTime,
                    totalStats.stealAttempts)));
            out.println("SATIN: HANDLE_STEAL_TIME:        total "
                + Timer.format(totalStats.handleStealTime)
                + " time/handle "
                + Timer.format(perStats(totalStats.handleStealTime,
                    totalStats.stealAttempts)));

            out.println("SATIN: INV SERIALIZATION_TIME:   total "
                + Timer.format(totalStats.invocationRecordWriteTime)
                + " time/write  "
                + Timer.format(perStats(totalStats.invocationRecordWriteTime,
                    totalStats.stealSuccess)));
            out.println("SATIN: INV DESERIALIZATION_TIME: total "
                + Timer.format(totalStats.invocationRecordReadTime)
                + " time/read   "
                + Timer.format(perStats(totalStats.invocationRecordReadTime,
                    totalStats.stealSuccess)));
            out.println("SATIN: RET SERIALIZATION_TIME:   total "
                + Timer.format(totalStats.returnRecordWriteTime)
                + " time/write  "
                + Timer.format(perStats(totalStats.returnRecordWriteTime,
                    totalStats.returnRecordWriteCount)));
            out.println("SATIN: RET DESERIALIZATION_TIME: total "
                + Timer.format(totalStats.returnRecordReadTime)
                + " time/read   "
                + Timer.format(perStats(totalStats.returnRecordReadTime,
                    totalStats.returnRecordReadCount)));
        }

        if (ABORT_TIMING) {
            out.println("SATIN: ABORT_TIME:               total "
                + Timer.format(totalStats.abortTime)
                + " time/abort  "
                + Timer
                    .format(perStats(totalStats.abortTime, totalStats.aborts)));
        }

        if (TUPLE_TIMING) {
            out.println("SATIN: TUPLE_SPACE_BCAST_TIME:   total "
                + Timer.format(totalStats.tupleTime)
                + " time/bcast  "
                + Timer.format(perStats(totalStats.tupleTime,
                    totalStats.tupleMsgs)));
            out.println("SATIN: TUPLE_SPACE_WAIT_TIME:    total "
                + Timer.format(totalStats.tupleWaitTime)
                + " time/bcast  "
                + Timer.format(perStats(totalStats.tupleWaitTime,
                    totalStats.tupleWaitCount)));
        }

        if (POLL_FREQ != 0 && POLL_TIMING) {
            out.println("SATIN: POLL_TIME:                total "
                + Timer.format(totalStats.pollTime)
                + " time/poll "
                + Timer.format(perStats(totalStats.pollTime,
                    totalStats.pollCount)));
        }

        if (IDLE_TIMING) {
            out.println("SATIN: IDLE_TIME:                total "
                + Timer.format(totalStats.idleTime)
                + " time/idle "
                + Timer.format(perStats(totalStats.idleTime,
                    totalStats.idleCount)));
        }

        if (FAULT_TOLERANCE && GRT_TIMING) {
            out
                .println("SATIN: GRT_UPDATE_TIME:          total "
                    + Timer.format(totalStats.tableUpdateTime)
                    + " time/update "
                    + Timer
                        .format(perStats(
                            totalStats.tableUpdateTime,
                            (totalStats.tableResultUpdates + totalStats.tableLockUpdates))));
            out.println("SATIN: GRT_LOOKUP_TIME:          total "
                + Timer.format(totalStats.tableLookupTime)
                + " time/lookup "
                + Timer.format(perStats(totalStats.tableLookupTime,
                    totalStats.tableLookups)));
            out.println("SATIN: GRT_HANDLE_UPDATE_TIME:   total "
                + Timer.format(totalStats.tableHandleUpdateTime)
                + " time/handle "
                + Timer.format(perStats(totalStats.tableHandleUpdateTime,
                    totalStats.tableResultUpdates * (size - 1))));
            out.println("SATIN: GRT_HANDLE_LOOKUP_TIME:   total "
                + Timer.format(totalStats.tableHandleLookupTime)
                + " time/handle "
                + Timer.format(perStats(totalStats.tableHandleLookupTime,
                    totalStats.tableRemoteLookups)));
            out.println("SATIN: GRT_SERIALIZATION_TIME:   total "
                + Timer.format(totalStats.tableSerializationTime));
            out.println("SATIN: GRT_DESERIALIZATION_TIME: total "
                + Timer.format(totalStats.tableDeserializationTime));
            out.println("SATIN: GRT_CHECK_TIME:           total "
                + Timer.format(totalStats.tableCheckTime)
                + " time/check "
                + Timer.format(perStats(totalStats.tableCheckTime,
                    totalStats.tableLookups)));

        }

        if (FAULT_TOLERANCE && CRASH_TIMING) {
            out.println("SATIN: CRASH_HANDLING_TIME:      total "
                + Timer.format(totalStats.crashHandlingTime));
        }

        if (FAULT_TOLERANCE && ADD_REPLICA_TIMING) {
            out.println("SATIN: ADD_REPLICA_TIME:         total "
                + Timer.format(totalStats.addReplicaTime));
        }

        if (SHARED_OBJECTS && SO_TIMING) {
            out.println("SATIN: BROADCAST_SO_INVOCATIONS: total "
                + Timer.format(totalStats.broadcastSOInvocationsTime)
                + " time/inv    "
                + Timer.format(perStats(totalStats.broadcastSOInvocationsTime,
                    totalStats.soInvocations)));
            out.println("SATIN: HANDLE_SO_INVOCATIONS:    total "
                + Timer.format(totalStats.handleSOInvocationsTime)
                + " time/inv    "
                + Timer.format(perStats(totalStats.handleSOInvocationsTime,
                    totalStats.soInvocations * (size - 1))));
            out.println("SATIN: SO_TRANSFERS:             total "
                + Timer.format(totalStats.soTransferTime)
                + " time/transf "
                + Timer.format(perStats(totalStats.soTransferTime,
                    totalStats.soTransfers)));
            out.println("SATIN: SO_SERIALIZATION:         total "
                + Timer.format(totalStats.soSerializationTime)
                + " time/transf "
                + Timer.format(perStats(totalStats.soSerializationTime,
                    totalStats.soTransfers)));
            out.println("SATIN: SO_DESERIALIZATION:       total "
                + Timer.format(totalStats.soDeserializationTime)
                + " time/transf "
                + Timer.format(perStats(totalStats.soDeserializationTime,
                    totalStats.soTransfers)));
            out.println("SATIN: SO_BCASTS:                total "
                + Timer.format(totalStats.soBcastTime)
                + " time/bcast  "
                + Timer.format(perStats(totalStats.soBcastTime,
                    totalStats.soBcasts)));
            out.println("SATIN: SO_BCAST_SERIALIZATION:   total "
                + Timer.format(totalStats.soBcastSerializationTime)
                + " time/bcast  "
                + Timer.format(perStats(totalStats.soBcastSerializationTime,
                    totalStats.soBcasts)));
            out.println("SATIN: SO_BCAST_DESERIALIZATION: total "
                + Timer.format(totalStats.soBcastDeserializationTime)
                + " time/bcast  "
                + Timer.format(perStats(totalStats.soBcastDeserializationTime,
                    totalStats.soBcasts)));
}

        out.println("-------------------------------SATIN RUN TIME "
            + "BREAKDOWN------------------------");
        out.println("SATIN: TOTAL_RUN_TIME:                           "
            + Timer.format(totalTimer.totalTimeVal()));

        double lbTime = (totalStats.stealTime + totalStats.handleStealTime
            - totalStats.invocationRecordReadTime - totalStats.invocationRecordWriteTime)
            / size;
        if (lbTime < 0.0) {
            lbTime = 0.0;
        }
        double lbPerc = lbTime / totalTimer.totalTimeVal() * 100.0;
        double serTime = (totalStats.invocationRecordWriteTime
            + totalStats.invocationRecordReadTime
            + totalStats.returnRecordWriteTime + totalStats.returnRecordReadTime)
            / size;
        double serPerc = serTime / totalTimer.totalTimeVal() * 100.0;
        double abortTime = totalStats.abortTime / size;
        double abortPerc = abortTime / totalTimer.totalTimeVal() * 100.0;
        double tupleTime = totalStats.tupleTime / size;
        double tuplePerc = tupleTime / totalTimer.totalTimeVal() * 100.0;
        double handleTupleTime = totalStats.handleTupleTime / size;
        double handleTuplePerc = handleTupleTime / totalTimer.totalTimeVal()
            * 100.0;
        double tupleWaitTime = totalStats.tupleWaitTime / size;
        double tupleWaitPerc = tupleWaitTime / totalTimer.totalTimeVal()
            * 100.0;
        double pollTime = totalStats.pollTime / size;
        double pollPerc = pollTime / totalTimer.totalTimeVal() * 100.0;

        double tableUpdateTime = totalStats.tableUpdateTime / size;
        double tableUpdatePerc = tableUpdateTime / totalTimer.totalTimeVal()
            * 100.0;
        double tableLookupTime = totalStats.tableLookupTime / size;
        double tableLookupPerc = tableLookupTime / totalTimer.totalTimeVal()
            * 100.0;
        double tableHandleUpdateTime = totalStats.tableHandleUpdateTime / size;
        double tableHandleUpdatePerc = tableHandleUpdateTime
            / totalTimer.totalTimeVal() * 100.0;
        double tableHandleLookupTime = totalStats.tableHandleLookupTime / size;
        double tableHandleLookupPerc = tableHandleLookupTime
            / totalTimer.totalTimeVal() * 100.0;
        double tableSerializationTime = totalStats.tableSerializationTime
            / size;
        double tableSerializationPerc = tableSerializationTime
            / totalTimer.totalTimeVal() * 100;
        double tableDeserializationTime = totalStats.tableDeserializationTime
            / size;
        double tableDeserializationPerc = tableDeserializationTime
            / totalTimer.totalTimeVal() * 100;
        double crashHandlingTime = totalStats.crashHandlingTime / size;
        double crashHandlingPerc = crashHandlingTime
            / totalTimer.totalTimeVal() * 100.0;
        double addReplicaTime = totalStats.addReplicaTime / size;
        double addReplicaPerc = addReplicaTime / totalTimer.totalTimeVal()
            * 100.0;
        double broadcastSOInvocationsTime = totalStats.broadcastSOInvocationsTime
            / size;
        double broadcastSOInvocationsPerc = broadcastSOInvocationsTime
            / totalTimer.totalTimeVal() * 100;
        double handleSOInvocationsTime = totalStats.handleSOInvocationsTime
            / size;
        double handleSOInvocationsPerc = handleSOInvocationsTime
            / totalTimer.totalTimeVal() * 100;
        double soTransferTime = totalStats.soTransferTime / size;
        double soTransferPerc = soTransferTime / totalTimer.totalTimeVal()
            * 100;
        double soSerializationTime = totalStats.soSerializationTime / size;
        double soSerializationPerc = soSerializationTime
            / totalTimer.totalTimeVal() * 100;
        double soDeserializationTime = totalStats.soDeserializationTime / size;
        double soDeserializationPerc = soDeserializationTime
            / totalTimer.totalTimeVal() * 100;

        double soBcastTime = totalStats.soBcastTime / size;
        double soBcastPerc = soBcastTime / totalTimer.totalTimeVal()
            * 100;
        double soBcastSerializationTime = totalStats.soBcastSerializationTime / size;
        double soBcastSerializationPerc = soBcastSerializationTime
            / totalTimer.totalTimeVal() * 100;
        double soBcastDeserializationTime = totalStats.soBcastDeserializationTime / size;
        double soBcastDeserializationPerc = soBcastDeserializationTime
            / totalTimer.totalTimeVal() * 100;
        
        
        double totalOverhead = abortTime
            + tupleTime
            + handleTupleTime
            + tupleWaitTime
            + pollTime
            + tableUpdateTime
            + tableLookupTime
            + tableHandleUpdateTime
            + tableHandleLookupTime
            + handleSOInvocationsTime
            + broadcastSOInvocationsTime
            + soTransferTime + soDeserializationTime
            + soBcastTime + soBcastDeserializationTime
            + (totalStats.stealTime + totalStats.handleStealTime
                + totalStats.returnRecordReadTime + totalStats.returnRecordWriteTime)
            / size;
        double totalPerc = totalOverhead / totalTimer.totalTimeVal() * 100.0;
        double appTime = totalTimer.totalTimeVal() - totalOverhead;
        if (appTime < 0.0) {
            appTime = 0.0;
        }
        double appPerc = appTime / totalTimer.totalTimeVal() * 100.0;

        if (STEAL_TIMING) {
            out.println("SATIN: LOAD_BALANCING_TIME:      avg. per machine "
                + Timer.format(lbTime) + " (" + (lbPerc < 10 ? " " : "")
                + pf.format(lbPerc) + " %)");
            out.println("SATIN: (DE)SERIALIZATION_TIME:   avg. per machine "
                + Timer.format(serTime) + " (" + (serPerc < 10 ? " " : "")
                + pf.format(serPerc) + " %)");
        }

        if (ABORT_TIMING) {
            out.println("SATIN: ABORT_TIME:               avg. per machine "
                + Timer.format(abortTime) + " (" + (abortPerc < 10 ? " " : "")
                + pf.format(abortPerc) + " %)");
        }

        if (TUPLE_TIMING) {
            out.println("SATIN: TUPLE_SPACE_BCAST_TIME:   avg. per machine "
                + Timer.format(tupleTime) + " (" + (tuplePerc < 10 ? " " : "")
                + pf.format(tuplePerc) + " %)");
            out.println("SATIN: TUPLE_SPACE_HANDLE_TIME:  avg. per machine "
                + Timer.format(handleTupleTime) + " ("
                + (handleTuplePerc < 10 ? " " : "")
                + pf.format(handleTuplePerc) + " %)");
            out.println("SATIN: TUPLE_SPACE_WAIT_TIME:    avg. per machine "
                + Timer.format(tupleWaitTime) + " ("
                + (tupleWaitPerc < 10 ? " " : "") + pf.format(tupleWaitPerc)
                + " %)");
        }

        if (POLL_FREQ != 0 && POLL_TIMING) {
            out.println("SATIN: POLL_TIME:                avg. per machine "
                + Timer.format(pollTime) + " (" + (pollPerc < 10 ? " " : "")
                + pf.format(pollPerc) + " %)");
        }

        if (FAULT_TOLERANCE && GRT_TIMING) {
            out.println("SATIN: GRT_UPDATE_TIME:          avg. per machine "
                + Timer.format(tableUpdateTime) + " ("
                + pf.format(tableUpdatePerc) + " %)");
            out.println("SATIN: GRT_LOOKUP_TIME:          avg. per machine "
                + Timer.format(tableLookupTime) + " ("
                + pf.format(tableLookupPerc) + " %)");
            out.println("SATIN: GRT_HANDLE_UPDATE_TIME:   avg. per machine "
                + Timer.format(tableHandleUpdateTime) + " ("
                + pf.format(tableHandleUpdatePerc) + " %)");
            out.println("SATIN: GRT_HANDLE_LOOKUP_TIME:   avg. per machine "
                + Timer.format(tableHandleLookupTime) + " ("
                + pf.format(tableHandleLookupPerc) + " %)");
            out.println("SATIN: GRT_SERIALIZATION_TIME:   avg. per machine "
                + Timer.format(tableSerializationTime) + " ("
                + pf.format(tableSerializationPerc) + " %)");
            out.println("SATIN: GRT_DESERIALIZATION_TIME: avg. per machine "
                + Timer.format(tableDeserializationTime) + " ("
                + pf.format(tableDeserializationPerc) + " %)");

        }

        if (FAULT_TOLERANCE && CRASH_TIMING) {
            out.println("SATIN: CRASH_HANDLING_TIME:      avg. per machine "
                + Timer.format(crashHandlingTime) + " ("
                + pf.format(crashHandlingPerc) + " %)");
        }

        if (FAULT_TOLERANCE && ADD_REPLICA_TIMING) {
            out.println("SATIN: ADD_REPLICA_TIME:         avg. per machine "
                + Timer.format(addReplicaTime) + " ("
                + pf.format(addReplicaPerc) + " %)");
        }

        if (SHARED_OBJECTS && SO_TIMING) {
            out.println("SATIN: BROADCAST_SO_INVOCATIONS: avg. per machine "
                + Timer.format(broadcastSOInvocationsTime) + " ( "
                + pf.format(broadcastSOInvocationsPerc) + " %)");
            out.println("SATIN: HANDLE_SO_INVOCATIONS:    avg. per machine "
                + Timer.format(handleSOInvocationsTime) + " ( "
                + pf.format(handleSOInvocationsPerc) + " %)");
            out.println("SATIN: SO_TRANSFERS:             avg. per machine "
                + Timer.format(soTransferTime) + " ( "
                + pf.format(soTransferPerc) + " %)");
            out.println("SATIN: SO_SERIALIZATION:         avg. per machine "
                + Timer.format(soSerializationTime) + " ( "
                + pf.format(soSerializationPerc) + " %)");
            out.println("SATIN: SO_DESERIALIZATION:       avg. per machine "
                + Timer.format(soDeserializationTime) + " ( "
                + pf.format(soDeserializationPerc) + " %)");
            out.println("SATIN: SO_BCASTS:                avg. per machine "
                + Timer.format(soBcastTime) + " ( "
                + pf.format(soBcastPerc) + " %)");
            out.println("SATIN: SO_BCAST_SERIALIZATION:   avg. per machine "
                + Timer.format(soBcastSerializationTime) + " ( "
                + pf.format(soBcastSerializationPerc) + " %)");
            out.println("SATIN: SO_BCAST_DESERIALIZATION: avg. per machine "
                + Timer.format(soBcastDeserializationTime) + " ( "
                + pf.format(soBcastDeserializationPerc) + " %)");
        }
        
        out.println("\nSATIN: TOTAL_PARALLEL_OVERHEAD:  avg. per machine "
            + Timer.format(totalOverhead) + " (" + (totalPerc < 10 ? " " : "")
            + pf.format(totalPerc) + " %)");

        out.println("SATIN: USEFUL_APP_TIME:          avg. per machine "
            + Timer.format(appTime) + " (" + (appPerc < 10 ? " " : "")
            + pf.format(appPerc) + " %)");

    }

    protected void printDetailedStats() {
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance();

        if (SPAWN_STATS) {
            out.println("SATIN '" + ident + "': SPAWN_STATS: spawns = "
                + spawns + " executed = " + jobsExecuted + " syncs = " + syncs);
            if (ABORTS) {
                out.println("SATIN '" + ident + "': ABORT_STATS 1: aborts = "
                    + aborts + " abort msgs = " + abortMessages
                    + " aborted jobs = " + abortedJobs);
            }
        }
        if (TUPLE_STATS) {
            out.println("SATIN '" + ident
                + "': TUPLE_STATS 1: tuple bcast msgs: " + tupleMsgs
                + ", bytes = " + nf.format(tupleBytes));
        }
        if (STEAL_STATS) {
            out.println("SATIN '" + ident + "': INTRA_STATS: messages = "
                + intraClusterMessages + ", bytes = "
                + nf.format(intraClusterBytes));

            out.println("SATIN '" + ident + "': INTER_STATS: messages = "
                + interClusterMessages + ", bytes = "
                + nf.format(interClusterBytes));

            out.println("SATIN '" + ident + "': STEAL_STATS 1: attempts = "
                + stealAttempts + " success = " + stealSuccess + " ("
                + (perStats(stealSuccess, stealAttempts) * 100.0)
                + " %)");

            out.println("SATIN '" + ident + "': STEAL_STATS 2: requests = "
                + stealRequests + " jobs stolen = " + stolenJobs);

            if (STEAL_TIMING) {
                out.println("SATIN '" + ident + "': STEAL_STATS 3: attempts = "
                    + stealTimer.nrTimes() + " total time = "
                    + stealTimer.totalTime() + " avg time = "
                    + stealTimer.averageTime());

                out.println("SATIN '" + ident
                    + "': STEAL_STATS 4: handleSteals = "
                    + handleStealTimer.nrTimes() + " total time = "
                    + handleStealTimer.totalTime() + " avg time = "
                    + handleStealTimer.averageTime());
                out.println("SATIN '" + ident
                    + "': STEAL_STATS 5: invocationRecordWrites = "
                    + invocationRecordWriteTimer.nrTimes() + " total time = "
                    + invocationRecordWriteTimer.totalTime() + " avg time = "
                    + invocationRecordWriteTimer.averageTime());
                out.println("SATIN '" + ident
                    + "': STEAL_STATS 6: invocationRecordReads = "
                    + invocationRecordReadTimer.nrTimes() + " total time = "
                    + invocationRecordReadTimer.totalTime() + " avg time = "
                    + invocationRecordReadTimer.averageTime());
                out.println("SATIN '" + ident
                    + "': STEAL_STATS 7: returnRecordWrites = "
                    + returnRecordWriteTimer.nrTimes() + " total time = "
                    + returnRecordWriteTimer.totalTime() + " avg time = "
                    + returnRecordWriteTimer.averageTime());
                out.println("SATIN '" + ident
                    + "': STEAL_STATS 8: returnRecordReads = "
                    + returnRecordReadTimer.nrTimes() + " total time = "
                    + returnRecordReadTimer.totalTime() + " avg time = "
                    + returnRecordReadTimer.averageTime());
            }

            if (ABORTS && ABORT_TIMING) {
                out.println("SATIN '" + ident + "': ABORT_STATS 2: aborts = "
                    + abortTimer.nrTimes() + " total time = "
                    + abortTimer.totalTime() + " avg time = "
                    + abortTimer.averageTime());
            }

            if (IDLE_TIMING) {
                out.println("SATIN '" + ident + "': IDLE_STATS: idle count = "
                    + idleTimer.nrTimes() + " total time = "
                    + idleTimer.totalTime() + " avg time = "
                    + idleTimer.averageTime());
            }

            if (POLL_FREQ != 0 && POLL_TIMING) {
                out.println("SATIN '" + ident + "': POLL_STATS: poll count = "
                    + pollTimer.nrTimes() + " total time = "
                    + pollTimer.totalTime() + " avg time = "
                    + pollTimer.averageTime());
            }

            if (STEAL_TIMING && IDLE_TIMING) {
                out.println("SATIN '"
                    + ident
                    + "': COMM_STATS: software comm time = "
                    + Timer.format(stealTimer.totalTimeVal()
                        + handleStealTimer.totalTimeVal()
                        - idleTimer.totalTimeVal()));
            }

            if (TUPLE_TIMING) {
                out.println("SATIN '" + ident + "': TUPLE_STATS 2: bcasts = "
                    + tupleTimer.nrTimes() + " total time = "
                    + tupleTimer.totalTime() + " avg time = "
                    + tupleTimer.averageTime());

                out.println("SATIN '" + ident + "': TUPLE_STATS 3: waits = "
                    + tupleOrderingWaitTimer.nrTimes() + " total time = "
                    + tupleOrderingWaitTimer.totalTime() + " avg time = "
                    + tupleOrderingWaitTimer.averageTime());

            }
            algorithm.printStats(out);
        }

        if (FAULT_TOLERANCE) {
            if (GRT_STATS) {
                out.println("SATIN '" + ident + "': "
                    + globalResultTable.numResultUpdates
                    + " result updates of the table.");
                out.println("SATIN '" + ident + "': "
                    + globalResultTable.numLockUpdates
                    + " lock updates of the table.");
                out
                    .println("SATIN '" + ident + "': "
                        + globalResultTable.numUpdateMessages
                        + " update messages.");
                out.println("SATIN '" + ident + "': "
                    + globalResultTable.numLookupsSucceded
                    + " lookups succeded, of which:");
                out.println("SATIN '" + ident + "': "
                    + globalResultTable.numRemoteLookups + " remote lookups.");
                out.println("SATIN '" + ident + "': "
                    + globalResultTable.maxNumEntries + " entries maximally.");
            }
            if (GRT_TIMING) {
                out.println("SATIN '" + ident + "': " + lookupTimer.totalTime()
                    + " spent in lookups");
                out.println("SATIN '" + ident + "': "
                    + lookupTimer.averageTime() + " per lookup");
                out.println("SATIN '" + ident + "': " + updateTimer.totalTime()
                    + " spent in updates");
                out.println("SATIN '" + ident + "': "
                    + updateTimer.averageTime() + " per update");
                out.println("SATIN '" + ident + "': "
                    + handleUpdateTimer.totalTime()
                    + " spent in handling updates");
                out.println("SATIN '" + ident + "': "
                    + handleUpdateTimer.averageTime() + " per update handle");
                out.println("SATIN '" + ident + "': "
                    + handleLookupTimer.totalTime()
                    + " spent in handling lookups");
                out.println("SATIN '" + ident + "': "
                    + handleLookupTimer.averageTime() + " per lookup handle");

            }
            if (CRASH_TIMING) {
                out.println("SATIN '" + ident + "': " + crashTimer.totalTime()
                    + " spent in handling crashes");
            }
            if (TABLE_CHECK_TIMING) {
                out.println("SATIN '" + ident + "': " + redoTimer.totalTime()
                    + " spent in redoing");

            }

            if (FT_STATS) {
                out.println("SATIN '" + ident + "': " + killedOrphans
                    + " orphans killed");
                out.println("SATIN '" + ident + "': " + restartedJobs
                    + " jobs restarted");
            }
        }
        if (SHARED_OBJECTS && SO_STATS) {
            out.println("SATIN '" + ident.name() + "': " + soInvocations
                + " shared object invocations sent.");
            out.println("SATIN '" + ident.name() + "': " + soTransfers
                + " shared objects transfered.");
        }
    }

}

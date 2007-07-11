/* $Id: StatsMessage.java 3698 2006-04-28 11:27:17Z rob $ */

package ibis.satin.impl;

import ibis.ipl.IbisIdentifier;
import ibis.util.Timer;

public final class Statistics implements java.io.Serializable, Config {
    /** 
     * Generated
     */
    private static final long serialVersionUID = 7954856934035669311L;

    public long spawns;

    public long syncs;

    public long abortsDone;

    public long jobsExecuted;

    public long abortedJobs;

    public long abortMessages;

    public long stealAttempts;

    public long stealSuccess;

    public long asyncStealAttempts;

    public long asyncStealSuccess;

    public long stolenJobs;

    public long stealRequests;

    public long interClusterMessages;

    public long intraClusterMessages;

    public long interClusterBytes;

    public long intraClusterBytes;

    public double stealTime;

    public double handleStealTime;

    public double abortTime;

    public double idleTime;

    public long idleCount;

    public long pollCount;

    public double invocationRecordWriteTime;

    public long invocationRecordWriteCount;

    public double returnRecordWriteTime;

    public long returnRecordWriteCount;

    public double invocationRecordReadTime;

    public long invocationRecordReadCount;

    public double returnRecordReadTime;

    public long returnRecordReadCount;

    public long returnRecordBytes;

    //fault tolerance
    public long tableResultUpdates;

    public long tableLockUpdates;

    public long tableUpdateMessages;

    public long tableLookups;

    public long tableSuccessfulLookups;

    public long tableRemoteLookups;

    public long tableMaxEntries;

    public long killedOrphans;

    public long restartedJobs;

    public double tableLookupTime;

    public double tableUpdateTime;

    public double tableHandleUpdateTime;

    public double tableHandleLookupTime;

    public double tableSerializationTime;

    public double tableDeserializationTime;

    public double tableCheckTime;

    public double crashHandlingTime;

    public int numCrashesHandled;

    //shared objects
    public long soInvocations;

    public long soInvocationsBytes;

    public long soTransfers;

    public long soTransfersBytes;

    public double broadcastSOInvocationsTime;

    public double handleSOInvocationsTime;

    public double getSOReferencesTime;

    public double soInvocationDeserializationTime;

    public double soTransferTime;

    public double soSerializationTime;

    public double soDeserializationTime;

    public double soBcastTime;

    public double soBcastSerializationTime;

    public double soBcastDeserializationTime;

    public double soGuardTime;

    public long soGuards;
    
    public long soRealMessageCount;

    public long soBcasts;

    public long soBcastBytes;

    public long handleSOInvocations;

    public long getSOReferences;

    public Timer totalTimer = Timer.createTimer();

    public Timer stealTimer = Timer.createTimer();

    public Timer handleStealTimer = Timer.createTimer();

    public Timer abortTimer = Timer.createTimer();

    public Timer idleTimer = Timer.createTimer();

    public Timer pollTimer = Timer.createTimer();

    public Timer invocationRecordWriteTimer = Timer.createTimer();

    public Timer returnRecordWriteTimer = Timer.createTimer();

    public Timer invocationRecordReadTimer = Timer.createTimer();

    public Timer returnRecordReadTimer = Timer.createTimer();

    public Timer lookupTimer = Timer.createTimer();

    public Timer updateTimer = Timer.createTimer();

    public Timer handleUpdateTimer = Timer.createTimer();

    public Timer handleLookupTimer = Timer.createTimer();

    public Timer tableSerializationTimer = Timer.createTimer();

    public Timer tableDeserializationTimer = Timer.createTimer();

    public Timer crashTimer = Timer.createTimer();

    public Timer redoTimer = Timer.createTimer();

    public Timer handleSOInvocationsTimer = Timer.createTimer();

    public Timer getSOReferencesTimer = Timer.createTimer();

    public Timer broadcastSOInvocationsTimer = Timer.createTimer();

    public Timer soTransferTimer = Timer.createTimer();

    public Timer soSerializationTimer = Timer.createTimer();

    public Timer soDeserializationTimer = Timer.createTimer();

    public Timer soBroadcastDeserializationTimer = Timer.createTimer();

    public Timer soBroadcastSerializationTimer = Timer.createTimer();

    public Timer soBroadcastTransferTimer = Timer.createTimer();

    public Timer soInvocationDeserializationTimer = Timer.createTimer();

    public Timer soGuardTimer = Timer.createTimer();

    public void add(Statistics s) {
        spawns += s.spawns;
        jobsExecuted += s.jobsExecuted;
        syncs += s.syncs;
        abortsDone += s.abortsDone;
        abortMessages += s.abortMessages;
        abortedJobs += s.abortedJobs;

        stealAttempts += s.stealAttempts;
        stealSuccess += s.stealSuccess;
        asyncStealAttempts += s.asyncStealAttempts;
        asyncStealSuccess += s.asyncStealSuccess;
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
        pollCount += s.pollCount;
        invocationRecordWriteTime += s.invocationRecordWriteTime;
        invocationRecordWriteCount += s.invocationRecordWriteCount;
        invocationRecordReadTime += s.invocationRecordReadTime;
        invocationRecordReadCount += s.invocationRecordReadCount;
        returnRecordWriteTime += s.returnRecordWriteTime;
        returnRecordWriteCount += s.returnRecordWriteCount;
        returnRecordReadTime += s.returnRecordReadTime;
        returnRecordReadCount += s.returnRecordReadCount;
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
        tableMaxEntries += s.tableMaxEntries;
        crashHandlingTime += s.crashHandlingTime;
        numCrashesHandled += s.numCrashesHandled;

        //shared objects
        soInvocations += s.soInvocations;
        soInvocationsBytes += s.soInvocationsBytes;
        soTransfers += s.soTransfers;
        soTransfersBytes += s.soTransfersBytes;
        broadcastSOInvocationsTime += s.broadcastSOInvocationsTime;
        handleSOInvocationsTime += s.handleSOInvocationsTime;
        handleSOInvocations += s.handleSOInvocations;
        getSOReferencesTime += s.getSOReferencesTime;
        getSOReferences += s.getSOReferences;
        soInvocationDeserializationTime += s.soInvocationDeserializationTime;
        soTransferTime += s.soTransferTime;
        soSerializationTime += s.soSerializationTime;
        soDeserializationTime += s.soDeserializationTime;
        soBcastTime += s.soBcastTime;
        soBcastSerializationTime += s.soBcastSerializationTime;
        soBcastDeserializationTime += s.soBcastDeserializationTime;
        soRealMessageCount += s.soRealMessageCount;
        soBcasts += s.soBcasts;
        soBcastBytes += s.soBcastBytes;
        soGuards += s.soGuards;
    }

    public void fillInStats() {
        stealTime = stealTimer.totalTimeVal();
        handleStealTime = handleStealTimer.totalTimeVal();
        abortTime = abortTimer.totalTimeVal();
        idleTime = idleTimer.totalTimeVal();
        idleCount = idleTimer.nrTimes();
        pollCount = pollTimer.nrTimes();

        invocationRecordWriteTime = invocationRecordWriteTimer.totalTimeVal();
        invocationRecordWriteCount = invocationRecordWriteTimer.nrTimes();
        invocationRecordReadTime = invocationRecordReadTimer.totalTimeVal();
        invocationRecordReadCount = invocationRecordReadTimer.nrTimes();

        returnRecordWriteTime = returnRecordWriteTimer.totalTimeVal();
        returnRecordWriteCount = returnRecordWriteTimer.nrTimes();
        returnRecordReadTime = returnRecordReadTimer.totalTimeVal();
        returnRecordReadCount = returnRecordReadTimer.nrTimes();

        tableLookupTime = lookupTimer.totalTimeVal();
        tableUpdateTime = updateTimer.totalTimeVal();
        tableHandleUpdateTime = handleUpdateTimer.totalTimeVal();
        tableHandleLookupTime = handleLookupTimer.totalTimeVal();
        tableSerializationTime = tableSerializationTimer.totalTimeVal();
        tableDeserializationTime = tableDeserializationTimer.totalTimeVal();
        tableCheckTime = redoTimer.totalTimeVal();
        crashHandlingTime = crashTimer.totalTimeVal();

        handleSOInvocations = handleSOInvocationsTimer.nrTimes();

        handleSOInvocationsTime = handleSOInvocationsTimer.totalTimeVal();
        getSOReferences = getSOReferencesTimer.nrTimes();

        getSOReferencesTime = getSOReferencesTimer.totalTimeVal();
        soInvocationDeserializationTime = soInvocationDeserializationTimer
            .totalTimeVal();
        broadcastSOInvocationsTime = broadcastSOInvocationsTimer.totalTimeVal();
        soTransferTime = soTransferTimer.totalTimeVal();
        soSerializationTime = soSerializationTimer.totalTimeVal();
        soDeserializationTime = soDeserializationTimer.totalTimeVal();
        soBcastTime = soBroadcastTransferTimer.totalTimeVal();
        soBcastSerializationTime = soBroadcastSerializationTimer.totalTimeVal();
        soBcastDeserializationTime = soBroadcastDeserializationTimer
            .totalTimeVal();
        soGuardTime = soGuardTimer.totalTimeVal();
        soGuards = soGuardTimer.nrTimes();
    }

    protected void printStats(int size, double totalTime) {
        java.io.PrintStream out = System.out;
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance();

        // for percentages
        java.text.NumberFormat pf = java.text.NumberFormat.getInstance();
        pf.setMaximumFractionDigits(3);
        pf.setMinimumFractionDigits(3);
        pf.setGroupingUsed(false);

        boolean haveAborts = abortsDone > 0 || abortedJobs > 0;
        boolean haveSteals = stealAttempts > 0;
        boolean haveCrashes = tableResultUpdates > 0 || tableLookups > 0
            || restartedJobs > 0;
        boolean haveSO = soInvocations > 0 || soTransfers > 0 || soBcasts > 0;

        out.println("-------------------------------SATIN STATISTICS------"
            + "--------------------------");
        out.println("SATIN: SPAWN:       " + nf.format(spawns) + " spawns, "
            + nf.format(jobsExecuted) + " executed, " + nf.format(syncs)
            + " syncs");
        if (haveAborts) {
            out.println("SATIN: ABORT:       " + nf.format(abortsDone)
                + " aborts, " + nf.format(abortMessages) + " abort msgs, "
                + nf.format(abortedJobs) + " aborted jobs");
        }
        if (haveSteals) {
            out.println("SATIN: STEAL:       " + nf.format(stealAttempts)
                + " attempts, " + nf.format(stealSuccess) + " successes ("
                + pf.format(perStats(stealSuccess, stealAttempts) * 100.0)
                + " %)");

            if (asyncStealAttempts != 0) {
                out
                    .println("SATIN: ASYNCSTEAL:   "
                        + nf.format(asyncStealAttempts)
                        + " attempts, "
                        + nf.format(asyncStealSuccess)
                        + " successes ("
                        + pf.format(perStats(asyncStealSuccess,
                            asyncStealAttempts) * 100.0) + " %)");
            }

            out.println("SATIN: MESSAGES:    intra "
                + nf.format(intraClusterMessages) + " msgs, "
                + nf.format(intraClusterBytes) + " bytes; inter "
                + nf.format(interClusterMessages) + " msgs, "
                + nf.format(interClusterBytes) + " bytes");
        }

        if (haveCrashes) {
            out.println("SATIN: GLOBAL_RESULT_TABLE: result updates "
                + nf.format(tableResultUpdates) + ",update messages "
                + nf.format(tableUpdateMessages) + ", lock updates "
                + nf.format(tableLockUpdates) + ",lookups "
                + nf.format(tableLookups) + ",successful "
                + nf.format(tableSuccessfulLookups) + ",remote "
                + nf.format(tableRemoteLookups));
            out.println("SATIN: FAULT_TOLERANCE: killed orphans "
                + nf.format(killedOrphans));
            out.println("SATIN: FAULT_TOLERANCE: restarted jobs "
                + nf.format(restartedJobs));
        }

        if (haveSO) {
            out.println("SATIN: SO_CALLS:    " + nf.format(soInvocations)
                + " invocations, " + nf.format(soInvocationsBytes) + " bytes, "
                + nf.format(soRealMessageCount) + " messages");
            out.println("SATIN: SO_TRANSFER: " + nf.format(soTransfers)
                + " transfers, " + nf.format(soTransfersBytes) + " bytes ");
            out.println("SATIN: SO_BCAST:    " + nf.format(soBcasts)
                + " bcasts, " + nf.format(soBcastBytes) + " bytes ");
            out.println("SATIN: SO_GUARDS:    " + nf.format(soGuards)
                + " guards executed");
        }

        if (haveAborts || haveSteals || haveCrashes || haveSO) {
            out.println("-------------------------------SATIN TOTAL TIMES"
                + "-------------------------------");
        }

        if (haveSteals) {
            out.println("SATIN: STEAL_TIME:                 total "
                + Timer.format(stealTime) + " time/req    "
                + Timer.format(perStats(stealTime, stealAttempts)));
            out.println("SATIN: HANDLE_STEAL_TIME:          total "
                + Timer.format(handleStealTime) + " time/handle "
                + Timer.format(perStats(handleStealTime, stealAttempts)));

            out.println("SATIN: INV SERIALIZATION_TIME:     total "
                + Timer.format(invocationRecordWriteTime)
                + " time/write  "
                + Timer
                    .format(perStats(invocationRecordWriteTime, stealSuccess)));
            out.println("SATIN: INV DESERIALIZATION_TIME:   total "
                + Timer.format(invocationRecordReadTime)
                + " time/read   "
                + Timer
                    .format(perStats(invocationRecordReadTime, stealSuccess)));
            out.println("SATIN: RET SERIALIZATION_TIME:     total "
                + Timer.format(returnRecordWriteTime)
                + " time/write  "
                + Timer.format(perStats(returnRecordWriteTime,
                    returnRecordWriteCount)));
            out.println("SATIN: RET DESERIALIZATION_TIME:   total "
                + Timer.format(returnRecordReadTime)
                + " time/read   "
                + Timer.format(perStats(returnRecordReadTime,
                    returnRecordReadCount)));
        }

        if (haveAborts) {
            out.println("SATIN: ABORT_TIME:                 total "
                + Timer.format(abortTime) + " time/abort  "
                + Timer.format(perStats(abortTime, abortsDone)));
        }

        if (haveCrashes) {
            out.println("SATIN: GRT_UPDATE_TIME:            total "
                + Timer.format(tableUpdateTime)
                + " time/update "
                + Timer.format(perStats(tableUpdateTime,
                    (tableResultUpdates + tableLockUpdates))));
            out.println("SATIN: GRT_LOOKUP_TIME:            total "
                + Timer.format(tableLookupTime) + " time/lookup "
                + Timer.format(perStats(tableLookupTime, tableLookups)));
            out.println("SATIN: GRT_HANDLE_UPDATE_TIME:     total "
                + Timer.format(tableHandleUpdateTime)
                + " time/handle "
                + Timer.format(perStats(tableHandleUpdateTime,
                    tableResultUpdates * (size - 1))));
            out.println("SATIN: GRT_HANDLE_LOOKUP_TIME:     total "
                + Timer.format(tableHandleLookupTime)
                + " time/handle "
                + Timer.format(perStats(tableHandleLookupTime,
                    tableRemoteLookups)));
            out.println("SATIN: GRT_SERIALIZATION_TIME:     total "
                + Timer.format(tableSerializationTime));
            out.println("SATIN: GRT_DESERIALIZATION_TIME:   total "
                + Timer.format(tableDeserializationTime));
            out.println("SATIN: GRT_CHECK_TIME:             total "
                + Timer.format(tableCheckTime) + " time/check "
                + Timer.format(perStats(tableCheckTime, tableLookups)));
            out.println("SATIN: CRASH_HANDLING_TIME:        total "
                + Timer.format(crashHandlingTime));
        }

        if (haveSO) {
            out.println("SATIN: BROADCAST_SO_INVOCATIONS:   total "
                + Timer.format(broadcastSOInvocationsTime)
                + " time/inv    "
                + Timer.format(perStats(broadcastSOInvocationsTime,
                    soInvocations)));
            out.println("SATIN: DESERIALIZE_SO_INVOCATIONS: total "
                + Timer.format(soInvocationDeserializationTime)
                + " time/inv    "
                + Timer.format(perStats(soInvocationDeserializationTime,
                    handleSOInvocations)));
            out.println("SATIN: HANDLE_SO_INVOCATIONS:      total "
                + Timer.format(handleSOInvocationsTime)
                + " time/inv    "
                + Timer.format(perStats(handleSOInvocationsTime,
                    handleSOInvocations)));
            out.println("SATIN: GET_SO_REFERENCES:          total "
                + Timer.format(getSOReferencesTime)
                + " time/inv    "
                + Timer.format(perStats(getSOReferencesTime,
                    getSOReferences)));
            out.println("SATIN: SO_TRANSFERS:               total "
                + Timer.format(soTransferTime) + " time/transf "
                + Timer.format(perStats(soTransferTime, soTransfers)));
            out.println("SATIN: SO_SERIALIZATION:           total "
                + Timer.format(soSerializationTime) + " time/transf "
                + Timer.format(perStats(soSerializationTime, soTransfers)));
            out.println("SATIN: SO_DESERIALIZATION:         total "
                + Timer.format(soDeserializationTime) + " time/transf "
                + Timer.format(perStats(soDeserializationTime, soTransfers)));
            out.println("SATIN: SO_BCASTS:                  total "
                + Timer.format(soBcastTime) + " time/bcast  "
                + Timer.format(perStats(soBcastTime, soBcasts)));
            out.println("SATIN: SO_BCAST_SERIALIZATION:     total "
                + Timer.format(soBcastSerializationTime) + " time/bcast  "
                + Timer.format(perStats(soBcastSerializationTime, soBcasts)));
            out.println("SATIN: SO_BCAST_DESERIALIZATION:   total "
                + Timer.format(soBcastDeserializationTime) + " time/bcast  "
                + Timer.format(perStats(soBcastDeserializationTime, soBcasts)));
            out.println("SATIN: SO_GUARDS:                  total "
                + Timer.format(soGuardTime) + " time/guard  "
                + Timer.format(perStats(soGuardTime, soGuards)));
        }

        out.println("-------------------------------SATIN RUN TIME "
            + "BREAKDOWN------------------------");
        out.println("SATIN: TOTAL_RUN_TIME:                              "
            + Timer.format(totalTime));

        double lbTime = (stealTime - invocationRecordReadTime
            - invocationRecordWriteTime - returnRecordReadTime - returnRecordWriteTime)
            / size;
        if (lbTime < 0.0) {
            lbTime = 0.0;
        }
        double lbPerc = lbTime / totalTime * 100.0;
        double serTimeAvg = (invocationRecordWriteTime
            + invocationRecordReadTime + returnRecordWriteTime + returnRecordReadTime)
            / size;
        double serPerc = serTimeAvg / totalTime * 100.0;
        double abortTimeAvg = abortTime / size;
        double abortPerc = abortTimeAvg / totalTime * 100.0;

        double tableUpdateTimeAvg = tableUpdateTime / size;
        double tableUpdatePerc = tableUpdateTimeAvg / totalTime * 100.0;
        double tableLookupTimeAvg = tableLookupTime / size;
        double tableLookupPerc = tableLookupTimeAvg / totalTime * 100.0;
        double tableHandleUpdateTimeAvg = tableHandleUpdateTime / size;
        double tableHandleUpdatePerc = tableHandleUpdateTimeAvg / totalTime
            * 100.0;
        double tableHandleLookupTimeAvg = tableHandleLookupTime / size;
        double tableHandleLookupPerc = tableHandleLookupTimeAvg / totalTime
            * 100.0;
        double tableSerializationTimeAvg = tableSerializationTime / size;
        double tableSerializationPerc = tableSerializationTimeAvg / totalTime
            * 100;
        double tableDeserializationTimeAvg = tableDeserializationTime / size;
        double tableDeserializationPerc = tableDeserializationTimeAvg
            / totalTime * 100;
        double crashHandlingTimeAvg = crashHandlingTime / size;
        double crashHandlingPerc = crashHandlingTimeAvg / totalTime * 100.0;
        double broadcastSOInvocationsTimeAvg = broadcastSOInvocationsTime
            / size;
        double broadcastSOInvocationsPerc = broadcastSOInvocationsTimeAvg
            / totalTime * 100;
        double handleSOInvocationsTimeAvg = handleSOInvocationsTime / size;
        double handleSOInvocationsPerc = handleSOInvocationsTimeAvg / totalTime
            * 100;
        double soInvocationDeserializationTimeAvg = soInvocationDeserializationTime
            / size;
        double soInvocationDeserializationPerc = soInvocationDeserializationTimeAvg
            / totalTime * 100;
        double soTransferTimeAvg = soTransferTime / size;
        double soTransferPerc = soTransferTimeAvg / totalTime * 100;
        double soSerializationTimeAvg = soSerializationTime / size;
        double soSerializationPerc = soSerializationTimeAvg / totalTime * 100;
        double soDeserializationTimeAvg = soDeserializationTime / size;
        double soDeserializationPerc = soDeserializationTimeAvg / totalTime
            * 100;

        double soBcastTimeAvg = soBcastTime / size;
        double soBcastPerc = soBcastTimeAvg / totalTime * 100;
        double soBcastSerializationTimeAvg = soBcastSerializationTime / size;
        double soBcastSerializationPerc = soBcastSerializationTimeAvg
            / totalTime * 100;
        double soBcastDeserializationTimeAvg = soBcastDeserializationTime
            / size;
        double soBcastDeserializationPerc = soBcastDeserializationTimeAvg
            / totalTime * 100;
        double soGuardTimeAvg = soGuardTime / size;
        double soGuardPerc = soGuardTimeAvg / totalTime * 100;
        
        double totalOverheadAvg = (abortTimeAvg + tableUpdateTimeAvg
            + tableLookupTimeAvg + tableHandleUpdateTimeAvg
            + tableHandleLookupTimeAvg + handleSOInvocationsTimeAvg
            + broadcastSOInvocationsTimeAvg + soTransferTimeAvg
            + soBcastTimeAvg + soBcastDeserializationTimeAvg + stealTime)
            / size;
        double totalPerc = totalOverheadAvg / totalTime * 100.0;
        double appTime = totalTime - totalOverheadAvg;
        if (appTime < 0.0) {
            appTime = 0.0;
        }
        double appPerc = appTime / totalTime * 100.0;

        if (haveSteals) {
            out.println("SATIN: LOAD_BALANCING_TIME:        agv. per machine "
                + Timer.format(lbTime) + " (" + (lbPerc < 10 ? " " : "")
                + pf.format(lbPerc) + " %)");
            out.println("SATIN: (DE)SERIALIZATION_TIME:     agv. per machine "
                + Timer.format(serTimeAvg) + " (" + (serPerc < 10 ? " " : "")
                + pf.format(serPerc) + " %)");
        }

        if (haveAborts) {
            out.println("SATIN: ABORT_TIME:                 agv. per machine "
                + Timer.format(abortTimeAvg) + " ("
                + (abortPerc < 10 ? " " : "") + pf.format(abortPerc) + " %)");
        }

        if (haveCrashes) {
            out.println("SATIN: GRT_UPDATE_TIME:            agv. per machine "
                + Timer.format(tableUpdateTimeAvg) + " ("
                + pf.format(tableUpdatePerc) + " %)");
            out.println("SATIN: GRT_LOOKUP_TIME:            agv. per machine "
                + Timer.format(tableLookupTimeAvg) + " ("
                + pf.format(tableLookupPerc) + " %)");
            out.println("SATIN: GRT_HANDLE_UPDATE_TIME:     agv. per machine "
                + Timer.format(tableHandleUpdateTimeAvg) + " ("
                + pf.format(tableHandleUpdatePerc) + " %)");
            out.println("SATIN: GRT_HANDLE_LOOKUP_TIME:     agv. per machine "
                + Timer.format(tableHandleLookupTimeAvg) + " ("
                + pf.format(tableHandleLookupPerc) + " %)");
            out.println("SATIN: GRT_SERIALIZATION_TIME:     agv. per machine "
                + Timer.format(tableSerializationTimeAvg) + " ("
                + pf.format(tableSerializationPerc) + " %)");
            out.println("SATIN: GRT_DESERIALIZATION_TIME:   agv. per machine "
                + Timer.format(tableDeserializationTimeAvg) + " ("
                + pf.format(tableDeserializationPerc) + " %)");
            out.println("SATIN: CRASH_HANDLING_TIME:        agv. per machine "
                + Timer.format(crashHandlingTimeAvg) + " ("
                + pf.format(crashHandlingPerc) + " %)");
        }

        if (haveSO) {
            out.println("SATIN: BROADCAST_SO_INVOCATIONS:   agv. per machine "
                + Timer.format(broadcastSOInvocationsTimeAvg) + " ("
                + pf.format(broadcastSOInvocationsPerc) + " %)");
            out.println("SATIN: HANDLE_SO_INVOCATIONS:      agv. per machine "
                + Timer.format(handleSOInvocationsTimeAvg) + " ("
                + pf.format(handleSOInvocationsPerc) + " %)");
            out.println("SATIN: DESERIALIZE_SO_INVOCATIONS: agv. per machine "
                + Timer.format(soInvocationDeserializationTimeAvg) + " ( "
                + pf.format(soInvocationDeserializationPerc) + " %)");
            out.println("SATIN: SO_TRANSFERS:               agv. per machine "
                + Timer.format(soTransferTimeAvg) + " ("
                + pf.format(soTransferPerc) + " %)");
            out.println("SATIN: SO_SERIALIZATION:           agv. per machine "
                + Timer.format(soSerializationTimeAvg) + " ("
                + pf.format(soSerializationPerc) + " %)");
            out.println("SATIN: SO_DESERIALIZATION:         agv. per machine "
                + Timer.format(soDeserializationTimeAvg) + " ("
                + pf.format(soDeserializationPerc) + " %)");
            out.println("SATIN: SO_BCASTS:                  agv. per machine "
                + Timer.format(soBcastTimeAvg) + " (" + pf.format(soBcastPerc)
                + " %)");
            out.println("SATIN: SO_BCAST_SERIALIZATION:     agv. per machine "
                + Timer.format(soBcastSerializationTimeAvg) + " ("
                + pf.format(soBcastSerializationPerc) + " %)");
            out.println("SATIN: SO_BCAST_DESERIALIZATION:   agv. per machine "
                + Timer.format(soBcastDeserializationTimeAvg) + " ("
                + pf.format(soBcastDeserializationPerc) + " %)");
            out.println("SATIN: SO_GUARD:                   agv. per machine "
                + Timer.format(soGuardTimeAvg) + " ("
                + pf.format(soGuardPerc) + " %)");
        }

        out.println("\nSATIN: TOTAL_PARALLEL_OVERHEAD:    agv. per machine "
            + Timer.format(totalOverheadAvg) + " ("
            + (totalPerc < 10 ? " " : "") + pf.format(totalPerc) + " %)");

        out.println("SATIN: USEFUL_APP_TIME:            agv. per machine "
            + Timer.format(appTime) + " (" + (appPerc < 10 ? " " : "")
            + pf.format(appPerc) + " %)");
    }

    public void printDetailedStats(IbisIdentifier ident) {
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance();
        java.io.PrintStream out = System.out;

        out.println("SATIN '" + ident + "': SPAWN_STATS: spawns = " + spawns
            + " executed = " + jobsExecuted + " syncs = " + syncs + " aborts = "
                + abortsDone + " abort msgs = " + abortMessages
                + " aborted jobs = " + abortedJobs + " total time = " + abortTimer.totalTime());

        out.println("SATIN '" + ident + "': MSG_STATS: intra = "
            + intraClusterMessages + ", bytes = "
            + nf.format(intraClusterBytes) +
            " inter = "
            + interClusterMessages + ", bytes = "
            + nf.format(interClusterBytes));

        out.println("SATIN '" + ident + "': STEAL_STATS: attempts = "
            + stealAttempts + " success = " + stealSuccess + " ("
            + (perStats(stealSuccess, stealAttempts) * 100.0) + " %)" 
            + " time = " + stealTimer.totalTime()
            + " requests = " + stealRequests + " jobs stolen = " + stolenJobs
            + " time = "
            + handleStealTimer.totalTime());
        
        out.println("SATIN '" + ident
            + "': SERIALIZATION_STATS: invocationRecordWrites = "
            + invocationRecordWriteTimer.nrTimes() + " total time = "
            + invocationRecordWriteTimer.totalTime()
            + " invocationRecordReads = "
            + invocationRecordReadTimer.nrTimes() + " total time = "
            + invocationRecordReadTimer.totalTime() 
            + " returnRecordWrites = "
            + returnRecordWriteTimer.nrTimes() + " total time = "
            + returnRecordWriteTimer.totalTime() 
            + " returnRecordReads = "
            + returnRecordReadTimer.nrTimes() + " total time = "
            + returnRecordReadTimer.totalTime() );

        out.println("SATIN '" + ident + 
        		"': GRT_STATS: updates = " + tableResultUpdates
        		+ " lock updates = " + tableLockUpdates
        		+ " update time = " + updateTimer.totalTime()
        		+ " handle update time = " + handleUpdateTimer.totalTime()
        		+ " msgs = " + tableUpdateMessages
        		+ " lookups = " + tableLookups  
        		+ " lookup time = " + lookupTimer.totalTime()
        		+ " handle lookup time = " + lookupTimer.totalTime()
        		+ " remote lookups = " + tableRemoteLookups 
        		+ " successful lookups = " + tableSuccessfulLookups
        		+ " max size = " + tableMaxEntries);

        out.println("SATIN '" + ident + "': FT_STATS:" 
        		+ " handle crash time = " + crashTimer.totalTime()
        		+ " redo time = " + redoTimer.totalTime()
        		+ " orphans killed = " + killedOrphans
        		+ " jobs restarted = " + restartedJobs);

        out.println("SATIN '" + ident + "': SO_STATS: "
        		+ " invocations = " + soInvocations
        		+ " size = " + soInvocationsBytes
        		+ " time = " + broadcastSOInvocationsTimer.totalTime()
        		+ " transfers = " + soTransfers 
        		+ " size = " + soTransfersBytes 
        		+ " time = " + soTransferTimer.totalTime());
    }

    private double perStats(double tm, long cnt) {
        if (cnt == 0) {
            return 0.0;
        }
        return tm / cnt;
    }
}

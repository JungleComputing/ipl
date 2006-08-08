/* $Id$ */

package ibis.satin.impl.faultTolerance;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.StaticProperties;
import ibis.ipl.WriteMessage;
import ibis.satin.impl.Config;
import ibis.satin.impl.Satin;
import ibis.satin.impl.loadBalancing.Victim;
import ibis.satin.impl.spawnSync.InvocationRecord;
import ibis.satin.impl.spawnSync.Stamp;
import ibis.util.Timer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

final class GlobalResultTable implements Config {
    private Satin s;

    /** The entries in the global result table. Entries are of type
     * GlobalResultTableValue. */
    private Map entries;

    private Map toSend;

    /** used for communication with other replicas of the table */
    private ReceivePort receive;

    /** Entries are Victims */
    private Map sends = new Hashtable();

    private int numReplicas = 0;

    private GlobalResultTableValue pointerValue = new GlobalResultTableValue(
        GlobalResultTableValue.TYPE_POINTER, null);

    private PortType globalResultTablePortType;

    protected GlobalResultTable(Satin s, StaticProperties requestedProperties) {
        this.s = s;
        entries = new Hashtable();
        toSend = new Hashtable();
    }

    protected GlobalResultTableValue lookup(Stamp key) {
        if (key == null) return null;

        s.stats.lookupTimer.start();
        Satin.assertLocked(s);

        GlobalResultTableValue value = (GlobalResultTableValue) entries
            .get(key);

        if (value != null) {
            grtLogger
                .debug("SATIN '" + s.ident + "': lookup successful " + key);
            if (value.type == GlobalResultTableValue.TYPE_POINTER) {
                if (!s.deadIbises.contains(value.owner)) {
                    s.stats.tableSuccessfulLookups++;
                    s.stats.tableRemoteLookups++;
                }
            } else {
                s.stats.tableSuccessfulLookups++;
            }
        }
        s.stats.tableRemoteLookups++;
        s.stats.lookupTimer.stop();

        return value;
    }

    protected void storeResult(InvocationRecord r) {
        Satin.assertLocked(s);

        Timer updateTimer = Timer.createTimer();
        updateTimer.start();

        GlobalResultTableValue value = new GlobalResultTableValue(
            GlobalResultTableValue.TYPE_RESULT, r);

        Stamp key = r.getStamp();
        Object oldValue = entries.get(key);
        entries.put(key, value);
        s.stats.tableResultUpdates++;
        if (entries.size() > s.stats.tableMaxEntries) {
            s.stats.tableMaxEntries = entries.size();
        }

        if (numReplicas > 0 && oldValue == null) {
            toSend.put(key, pointerValue);
            s.ft.updatesToSend = true;
        }

        grtLogger.debug("SATIN '" + s.ident + "': update complete: " + key
            + "," + value);

        updateTimer.stop();
        s.stats.updateTimer.add(updateTimer);
    }

    protected void updateAll(Map updates) {
        Satin.assertLocked(s);
        s.stats.updateTimer.start();
        entries.putAll(updates);
        toSend.putAll(updates);
        s.stats.tableResultUpdates += updates.size();
        s.stats.updateTimer.stop();
        s.ft.updatesToSend = true;
    }

    protected void sendUpdates() {
        Timer updateTimer = null;
        Timer tableSerializationTimer = null;

        synchronized (s) {
            s.ft.updatesToSend = false;
        }

        if (toSend.size() == 0) {
            return;
        }

        updateTimer = Timer.createTimer();
        updateTimer.start();

        Iterator sendIter = sends.entrySet().iterator();

        while (sendIter.hasNext()) {
            Map.Entry entry = (Map.Entry) sendIter.next();
            Victim send = (Victim) entry.getValue();
            WriteMessage m = null;

            try {
                m = send.newMessage();
            } catch (IOException e) {
                grtLogger.info("Got exception in newMessage()", e);
                continue;
                //always happens after a crash
            }

            tableSerializationTimer = Timer.createTimer();
            tableSerializationTimer.start();
            try {
                m.writeObject(toSend);
            } catch (IOException e) {
                grtLogger.info("Got exception in writeObject()", e);
                //always happens after a crash
            }
            tableSerializationTimer.stop();
            s.stats.tableSerializationTimer.add(tableSerializationTimer);

            try {
                long size = m.finish();

                grtLogger.debug("SATIN '" + s.ident + "': " + size
                    + " sent in "
                    + s.stats.tableSerializationTimer.lastTimeVal() + " to "
                    + entry.getKey());
            } catch (IOException e) {
                grtLogger.info("Got exception in finish()");
                //always happens after a crash
            }
        }

        s.stats.tableUpdateMessages++;

        updateTimer.stop();
        s.stats.updateTimer.add(updateTimer);
    }

    // Returns ready to send contents of the table.
    protected Map getContents() {
        Satin.assertLocked(s);

        // Replace "real" results with pointer values.
        Map newEntries = new HashMap();
        Iterator iter = entries.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry element = (Map.Entry) iter.next();
            GlobalResultTableValue value = (GlobalResultTableValue) element
                .getValue();
            Stamp key = (Stamp) element.getKey();
            switch (value.type) {
            case GlobalResultTableValue.TYPE_RESULT:
                newEntries.put(key, pointerValue);
                break;
            case GlobalResultTableValue.TYPE_POINTER:
                newEntries.put(key, value);
                break;
            default:
                grtLogger.error("SATIN '" + s.ident
                    + "': EEK invalid value type in getContents()");
            }
        }
        return newEntries;
    }

    protected void addContents(Map contents) {
        Satin.assertLocked(s);
        grtLogger.debug("adding contents");

        entries.putAll(contents);

        if (entries.size() > s.stats.tableMaxEntries) {
            s.stats.tableMaxEntries = entries.size();
        }
    }

    protected void addReplica(IbisIdentifier ident) {
        Victim v = s.victims.getVictim(ident);
        if (v == null) { // victim crashed
            return;
        }

        synchronized (s) {
            ftLogger.debug("addReplica: store victim");
            numReplicas++;
            sends.put(ident, v);
            ftLogger.debug("addReplica: store victim done");
        }
    }

    protected void removeReplica(IbisIdentifier ident) {
        Satin.assertLocked(s);

        Victim send = (Victim) sends.remove(ident);

        if (send != null) {
            numReplicas--;
        }
    }

    protected void handleGRTUpdate(ReadMessage m) {
        Map map = null;

        Timer handleUpdateTimer = Timer.createTimer();
        handleUpdateTimer.start();

        Timer tableDeserializationTimer = Timer.createTimer();
        tableDeserializationTimer.start();
        try {
            map = (Map) m.readObject();
        } catch (Exception e) {
            grtLogger.error("SATIN '" + s.ident
                + "': Global result table - error reading message", e);
        }
        tableDeserializationTimer.stop();
        s.stats.tableDeserializationTimer.add(tableDeserializationTimer);

        try {
            m.finish();
        } catch (IOException e) {
            //ignore
        }

        synchronized (s) {
            if (map != null) {
                entries.putAll(map);
            }
            if (entries.size() > s.stats.tableMaxEntries) {
                s.stats.tableMaxEntries = entries.size();
            }
        }

        grtLogger.debug("SATIN '" + s.ident + "': upcall finished: "
            + entries.size());

        handleUpdateTimer.stop();
        s.stats.handleUpdateTimer.add(handleUpdateTimer);
    }

    protected void print(java.io.PrintStream out) {
        synchronized (s) {
            out.println("=GRT: " + s.ident + "=");
            int i = 0;
            Iterator iter = entries.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                out.println("GRT[" + i + "]= " + entry.getKey() + ";"
                    + entry.getValue());
                i++;
            }
            out.println("=end of GRT " + s.ident + "=");
        }
    }
}

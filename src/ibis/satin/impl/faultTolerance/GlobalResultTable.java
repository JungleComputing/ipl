/* $Id$ */

// @@@ use lrmc here
package ibis.satin.impl.faultTolerance;

import ibis.ipl.ReadMessage;
import ibis.ipl.WriteMessage;
import ibis.satin.impl.Config;
import ibis.satin.impl.Satin;
import ibis.satin.impl.communication.Protocol;
import ibis.satin.impl.loadBalancing.Victim;
import ibis.satin.impl.spawnSync.InvocationRecord;
import ibis.satin.impl.spawnSync.Stamp;
import ibis.util.Timer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

final class GlobalResultTable implements Config, Protocol {
    private Satin s;

    /** The entries in the global result table. Entries are of type
     * GlobalResultTableValue. */
    private Map<Stamp, GlobalResultTableValue> entries;

    /** A list of updates that has to be broadcast to the other nodes. Elements are
     * of type (Stamp, GlobalResultTableValue). */
    private Map<Stamp, GlobalResultTableValue> toSend;

    private GlobalResultTableValue pointerValue = new GlobalResultTableValue(
        GlobalResultTableValue.TYPE_POINTER, null);

    protected GlobalResultTable(Satin s) {
        this.s = s;
        entries = new Hashtable<Stamp, GlobalResultTableValue>();
        toSend = new Hashtable<Stamp, GlobalResultTableValue>();
    }

    protected GlobalResultTableValue lookup(Stamp key) {
        if (key == null) return null;

        s.stats.lookupTimer.start();
        Satin.assertLocked(s);

        GlobalResultTableValue value = entries.get(key);

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

        s.stats.updateTimer.start();

        GlobalResultTableValue value = new GlobalResultTableValue(
            GlobalResultTableValue.TYPE_RESULT, r);

        Stamp key = r.getStamp();
        Object oldValue = entries.get(key);
        entries.put(key, value);
        s.stats.tableResultUpdates++;
        if (entries.size() > s.stats.tableMaxEntries) {
            s.stats.tableMaxEntries = entries.size();
        }

        if (oldValue == null) {
            toSend.put(key, pointerValue);
            s.ft.updatesToSend = true;
        }

        grtLogger.debug("SATIN '" + s.ident + "': update complete: " + key
            + "," + value);

        s.stats.updateTimer.stop();
    }

    protected void updateAll(Map<Stamp, GlobalResultTableValue> updates) {
        Satin.assertLocked(s);
        s.stats.updateTimer.start();
        entries.putAll(updates);
        toSend.putAll(updates);
        s.stats.tableResultUpdates += updates.size();
        if (entries.size() > s.stats.tableMaxEntries) {
            s.stats.tableMaxEntries = entries.size();
        }
        s.stats.updateTimer.stop();
        s.ft.updatesToSend = true;
    }

    protected void sendUpdates() {
        Timer updateTimer = null;
        Timer tableSerializationTimer = null;
        int size = 0;
        
        synchronized (s) {
            s.ft.updatesToSend = false;
            size = s.victims.size();
        }

        if (size == 0) return;

        updateTimer = Timer.createTimer();
        updateTimer.start();

        for(int i=0; i<size; i++) {
            Victim v;
            WriteMessage m = null;

            synchronized (s) {
                v = s.victims.getVictim(i);
            }

            if (v == null) {
                continue;
            }

            try {
                m = v.newMessage();
            } catch (Exception e) {
                //Catch any exception: the victim may not exist anymore
                grtLogger.info("Got exception in newMessage()", e);
                continue;
                //always happens after a crash
            }

            tableSerializationTimer = Timer.createTimer();
            tableSerializationTimer.start();
            try {
                m.writeByte(GRT_UPDATE);
                m.writeObject(toSend);
            } catch (IOException e) {
                grtLogger.info("Got exception in writeObject()", e);
                //always happens after a crash
            }
            tableSerializationTimer.stop();
            s.stats.tableSerializationTimer.add(tableSerializationTimer);

            try {
                long msgSize = v.finish(m);

                grtLogger.debug("SATIN '" + s.ident + "': " + msgSize
                    + " sent in "
                    + s.stats.tableSerializationTimer.lastTimeVal() + " to "
                    + v);
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
    protected Map<Stamp, GlobalResultTableValue> getContents() {
        Satin.assertLocked(s);

        // Replace "real" results with pointer values.
        Map<Stamp, GlobalResultTableValue> newEntries = new HashMap<Stamp, GlobalResultTableValue>();
        
        for(Map.Entry<Stamp, GlobalResultTableValue> entry : entries.entrySet()) {
            switch (entry.getValue().type) {
            case GlobalResultTableValue.TYPE_RESULT:
                newEntries.put(entry.getKey(), pointerValue);
                break;
            case GlobalResultTableValue.TYPE_POINTER:
                newEntries.put(entry.getKey(), entry.getValue());
                break;
            default:
                grtLogger.error("SATIN '" + s.ident
                    + "': EEK invalid value type in getContents()");
            }
        }

        return newEntries;
    }

    protected void addContents(Map<Stamp, GlobalResultTableValue> contents) {
        Satin.assertLocked(s);
        grtLogger.debug("adding contents");

        entries.putAll(contents);

        if (entries.size() > s.stats.tableMaxEntries) {
            s.stats.tableMaxEntries = entries.size();
        }
    }

    // No need to finish the message, we don't block.
    protected void handleGRTUpdate(ReadMessage m) {
        s.stats.handleUpdateTimer.start();
        s.stats.tableDeserializationTimer.start();
        try {
            @SuppressWarnings("unchecked")
            Map<Stamp, GlobalResultTableValue> map
                    = (Map<Stamp, GlobalResultTableValue>) m.readObject();
            synchronized (s) {
                addContents(map);
            }
        } catch (Exception e) {
            grtLogger.error("SATIN '" + s.ident
                + "': Global result table - error reading message", e);
        } finally {
            s.stats.tableDeserializationTimer.stop();
            s.stats.handleUpdateTimer.stop();
        }

    }

    protected void print(java.io.PrintStream out) {
        synchronized (s) {
            out.println("=GRT: " + s.ident + "=");
            int i = 0;

            for(Map.Entry<Stamp, GlobalResultTableValue> entry : entries.entrySet()) {
                out.println("GRT[" + i + "]= " + entry.getKey() + ";"
                    + entry.getValue());
                i++;                
            }
            out.println("=end of GRT " + s.ident + "=");            
        }
    }
}

/* $Id$ */

package ibis.satin.impl;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.Upcall;
import ibis.ipl.WriteMessage;
import ibis.util.Timer;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

public class GlobalResultTable implements Upcall, Config {

    static class Key implements java.io.Serializable {
        int stamp;

        ParameterRecord parameters;

        Key(InvocationRecord r) {
            if (Satin.this_satin.branchingFactor > 0) {
                this.stamp = r.stamp;
                this.parameters = null;
            } else {
                this.stamp = -1;
                this.parameters = r.getParameterRecord();
            }
        }

        public boolean equals(Object other) {
            Key otherKey = (Key) other;
            if (Satin.this_satin.branchingFactor > 0) {
                return this.stamp == otherKey.stamp;
            } else {
                if (other == null) {
                    return false;
                }
                return this.parameters.equals(otherKey.parameters);
            }
        }

        public int hashCode() {
            if (Satin.this_satin.branchingFactor > 0) {
                return this.stamp;
            } else {
                return this.parameters.hashCode();
            }
        }

        public String toString() {
            if (Satin.this_satin.branchingFactor > 0) {
                return Integer.toString(stamp);
            } else {
                return parameters.toString();
            }
        }
    }

    static class Value implements java.io.Serializable {
        static final int TYPE_LOCK = 0;

        static final int TYPE_RESULT = 1;

        static final int TYPE_POINTER = 2;

        int type;

        transient IbisIdentifier sendTo;
        // this field is never written --Rob
        // yes it is, in MessageHandler.handleResultRequest() --Gosia

        ReturnRecord result;

        IbisIdentifier owner;

        Value(int type, InvocationRecord r) {
            this.type = type;
            this.owner = Satin.this_satin.ident;
            if (type == TYPE_RESULT) {
                result = r.getReturnRecord();
            }
        }

        public String toString() {
            String str = "";
            switch (type) {
            case TYPE_LOCK:
                str += "(LOCK,sendTo:" + sendTo + ")";
                break;
            case TYPE_RESULT:
                str += "(RESULT,result:" + result + ")";
                break;
            case TYPE_POINTER:
                str += "(POINTER,owner:" + owner + ")";
                break;
            default:
                grtLogger.error("SATIN '" + Satin.this_satin.ident
                        + "': illegal type in value");
            }
            return str;
        }
    }

    private Satin satin;

    private Map entries;

    private Map toSend;

    /* used for communication with other replicas of the table */
    //private SendPort send;
    private ReceivePort receive;

    //a quick net ibis bug fix
    private Map sends = new Hashtable();

    private int numReplicas = 0;

    public int numResultUpdates = 0;

    public int numLockUpdates = 0;

    public int numUpdateMessages = 0;

    public int numLookups = 0;

    public int numLookupsSucceded = 0;

    public int maxNumEntries = 0;

    public int numRemoteLookups = 0;

    public final static int max = 20;

    private Value pointerValue = new Value(Value.TYPE_POINTER, null);

    GlobalResultTable(Satin sat) {

        satin = sat;
        entries = new Hashtable();
        if (GRT_MESSAGE_COMBINING) {
            toSend = new Hashtable();
        }
        try {
            receive = satin.globalResultTablePortType.createReceivePort(
                    "satin global result table receive port on "
                            + satin.ident.name(), this);
            // send = portType.createSendPort("satin global result table send "
            //                "port on " + satin.ident.name());
            receive.enableUpcalls();
            receive.enableConnections();

        } catch (IOException e) {
            grtLogger.error("SATIN '" + satin.ident
                    + "': Global result table - unable to create ports - "
                    + e.getMessage(), e);
        }
    }

    Value lookup(InvocationRecord r, boolean stats) {
        Key key = new Key(r);
        return lookup(key, stats);
    }

    Value lookup(Key key, boolean stats) {

        if (GRT_TIMING) {
            satin.lookupTimer.start();
        }

        if (ASSERTS) {
            Satin.assertLocked(satin);
        }

        Value value = (Value) entries.get(key);

        if (grtLogger.isDebugEnabled() && value != null) {
            grtLogger.debug("SATIN '" + satin.ident
                        + "': lookup successful " + key);
        }
        if (GRT_STATS && stats) {
            if (value != null) {
                if (value.type == Value.TYPE_POINTER) {
                    // if (satin.allIbises.contains(value.owner)) {
                    if (!satin.deadIbises.contains(value.owner)) {
                        numLookupsSucceded++;
                        numRemoteLookups++;
                    }
                } else {
                    numLookupsSucceded++;
                }
            }
            numLookups++;
        }

        if (GRT_TIMING) {
            satin.lookupTimer.stop();
        }

        return value;
    }

    void storeResult(InvocationRecord r) {
        Key key = new Key(r);
        Value value = new Value(Value.TYPE_RESULT, r);
        update(key, value);
    }

    void storeLock(InvocationRecord r) {
        Key key = new Key(r);
        Value value = new Value(Value.TYPE_LOCK, null);
        update(key, value);
    }

    void update(Key key, Value value) {
        Timer updateTimer = null;
        Timer tableSerializationTimer = null;

        if (GRT_TIMING) {
            updateTimer = satin.createTimer();
            updateTimer.start();
        }

        if (ASSERTS) {
            Satin.assertLocked(satin);
        }

        Object oldValue = entries.get(key);
        /* if (entries.size() < max) */entries.put(key, value);
        if (GRT_STATS) {
            if (entries.size() > maxNumEntries) {
                maxNumEntries = entries.size();
            }
        }

        if (numReplicas > 0 && oldValue == null) {
            if (GRT_MESSAGE_COMBINING) {
                if (GLOBAL_RESULT_TABLE_REPLICATED) {
                    toSend.put(key, value);
                } else {
                    toSend.put(key, pointerValue);
                }
                satin.updatesToSend = true;
            } else {
                if (grtLogger.isInfoEnabled()) {
                    grtLogger.info("SATIN '" + satin.ident
                            + "': sending update: " + key + "," + value);
                }

                //send an update message
                Iterator sendIter = sends.entrySet().iterator();
                long size = 0;
//                int i = 0;
                while (sendIter.hasNext()) {
                    Map.Entry entry = (Map.Entry) sendIter.next();
                    SendPort send = (SendPort) entry.getValue();
                    WriteMessage m = null;

                    try {
                        m = send.newMessage();
                    } catch (IOException e) {
                        continue;
                        //always happens after a crash
                    }

                    if (GRT_TIMING) {
                        tableSerializationTimer = satin.createTimer();
                        tableSerializationTimer.start();
                    }
                    try {
                        m.writeObject(key);

                        if (GLOBAL_RESULT_TABLE_REPLICATED) {
                            m.writeObject(value);
                        } else {
                            m.writeObject(pointerValue);
                            //m.writeObject(satin.ident);
                        }
                    } catch (IOException e) {
                        //always happens after a crash
                    }

                    if (GRT_TIMING) {
                        tableSerializationTimer.stop();
                        satin.tableSerializationTimer.add(
                                tableSerializationTimer);
                    }
                    try {
                        size = m.finish();

                        if (grtLogger.isDebugEnabled()) {
                            grtLogger.debug("SATIN '" + satin.ident
                                    + "': " + size + " sent in "
                                    + satin.tableSerializationTimer.lastTimeVal()
                                    + " to " + entry.getKey());
                        }

                    } catch (IOException e) {
                        //always happens after a crash
                    }

                }

                numUpdateMessages++;

                //send an update message
                /*
                 * try {
                 *     WriteMessage m = send.newMessage();
                 *     m.writeObject(key);
                 *     m.writeObject(value);
                 *     m.finish();
                 * } catch (IOException e) {
                 *     //always happens after the crash
                 * }
                 */
                if (grtLogger.isInfoEnabled()) {
                    grtLogger.info("SATIN '" + satin.ident
                            + "': update sent: " + key + "," + value);
                }
            }

        }
        if (GRT_STATS) {
            if (value.type == Value.TYPE_RESULT) {
                numResultUpdates++;
            }
            if (value.type == Value.TYPE_LOCK) {
                numLockUpdates++;
            }
        }
        if (grtLogger.isDebugEnabled()) {
            grtLogger.debug("SATIN '" + satin.ident
                + "': update complete: " + key + "," + value);
        }

        if (GRT_TIMING) {
            updateTimer.stop();
            satin.updateTimer.add(updateTimer);
        }

    }

    void updateAll(Map updates) {

        if (ASSERTS) {
            Satin.assertLocked(satin);
        }

        if (GRT_TIMING) {
            satin.updateTimer.start();
        }

        entries.putAll(updates);
        toSend.putAll(updates);

        if (GRT_STATS) {
            numResultUpdates += updates.size();
        }

        if (GRT_TIMING) {
            satin.updateTimer.stop();
        }

        satin.updatesToSend = true;

    }

    void sendUpdates() {
        Timer updateTimer = null;
        Timer tableSerializationTimer = null;

        if (ASSERTS && !GRT_MESSAGE_COMBINING) {
            grtLogger.error("SATIN '" + satin.ident
                    + "': EEK send updates with GRT_MESSAGE_COMBINING off!");
            return;
        }
        /*
         * if (ASSERTS) {
         *     satin.assertLocked(satin);
         * }
         */

        satin.updatesToSend = false;

        if (toSend.size() == 0) {
            return;
        }

        if (GRT_TIMING) {
            updateTimer = satin.createTimer();
            updateTimer.start();
        }

        Iterator sendIter = sends.entrySet().iterator();

        while (sendIter.hasNext()) {
            Map.Entry entry = (Map.Entry) sendIter.next();
            SendPort send = (SendPort) entry.getValue();
            WriteMessage m = null;

            try {
                m = send.newMessage();
            } catch (IOException e) {
                grtLogger.info("Got exception in newMessage()");
                continue;
                //always happens after a crash
            }

            if (GRT_TIMING) {
                tableSerializationTimer = satin.createTimer();
                tableSerializationTimer.start();
            }
            try {

                m.writeObject(toSend);

            } catch (IOException e) {
                grtLogger.info("Got exception in writeObject()");
                //always happens after a crash
            }

            if (GRT_TIMING) {
                tableSerializationTimer.stop();
                satin.tableSerializationTimer.add(tableSerializationTimer);
            }

            try {
                long size = m.finish();

                if (grtLogger.isDebugEnabled()) {
                    grtLogger.debug("SATIN '" + satin.ident + "': "
                            + size + " sent in "
                            + satin.tableSerializationTimer.lastTimeVal()
                            + " to " + entry.getKey());
                }

            } catch (IOException e) {
                grtLogger.info("Got exception in finish()");
                //always happens after a crash
            }

        }

        numUpdateMessages++;

        if (GRT_TIMING) {
            updateTimer.stop();
            satin.updateTimer.add(updateTimer);
        }

    }

    //returns ready to send contents of the table
    Map getContents() {
        if (ASSERTS) {
            Satin.assertLocked(satin);
        }

        if (GLOBAL_RESULT_TABLE_REPLICATED) {
            return (Map) ((Hashtable) entries).clone();
        } else {
            //replace "real" results with pointer values
            Map newEntries = new Hashtable();
            Iterator iter = entries.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry element = (Map.Entry) iter.next();
                Value value = (Value) element.getValue();
                Key key = (Key) element.getKey();
                switch (value.type) {
                case Value.TYPE_RESULT:
                case Value.TYPE_LOCK:
                    newEntries.put(key, pointerValue);
                    break;
                case Value.TYPE_POINTER:
                    newEntries.put(key, value);
                    break;
                default:
                    grtLogger.error("SATIN '" + satin.ident
                            + "': EEK invalid value type in getContents()");
                }
            }
            return newEntries;
        }
    }

    void addContents(Map contents) {
        if (ASSERTS) {
            Satin.assertLocked(satin);
        }

        if (grtLogger.isDebugEnabled()) {
            grtLogger.debug("adding contents");
        }

        entries.putAll(contents);

        if (GRT_STATS) {
            if (entries.size() > maxNumEntries) {
                maxNumEntries = entries.size();
            }
        }

    }

    void addReplica(IbisIdentifier ident) {
        if (ASSERTS) {
            Satin.assertLocked(satin);
        }

        try {
            SendPort send = satin.globalResultTablePortType.createSendPort(
                    "satin global result table send port on "
                    + satin.ident.name() + System.currentTimeMillis());
            sends.put(ident, send);
            ReceivePortIdentifier r = null;
            r = satin.lookup("satin global result table receive port on "
                    + ident.name());
            if (Satin.connect(send, r, satin.connectTimeout)) {
                numReplicas++;
            } else {
                grtLogger.error("SATN '" + satin.ident
                        + "': Transpositon table - unable to add new replica");
            }
        } catch (IOException e) {
            grtLogger.error("SATN '" + satin.ident
                    + "': Transpositon table - unable to add new replica", e);
        }

    }

    void removeReplica(IbisIdentifier ident) {
        if (ASSERTS) {
            Satin.assertLocked(satin);
        }

        if (sends.remove(ident) != null) {
            numReplicas--;
        }

    }

    void exit() {
        try {
            synchronized (satin) {
                if (numReplicas > 0) {
                    Iterator sendIter = sends.values().iterator();
                    while (sendIter.hasNext()) {
                        SendPort send = (SendPort) sendIter.next();
                        send.close();
                    }
                }
                // send.close();
            }
            receive.close();
        } catch (IOException e) {
            grtLogger.error("SATIN '" + satin.ident
                    + "': Unable to free global result table ports", e);
        }
    }

    public void upcall(ReadMessage m) {
        Map map = null;
        Key key = null;
        Value value = null;
        Timer handleUpdateTimer = null;
        Timer tableDeserializationTimer = null;

        if (GRT_TIMING) {
            handleUpdateTimer = satin.createTimer();
            handleUpdateTimer.start();
        }

        if (GRT_TIMING) {
            tableDeserializationTimer = satin.createTimer();
            tableDeserializationTimer.start();
        }

        try {

            if (GRT_MESSAGE_COMBINING) {
                map = (Map) m.readObject();
            } else {
                key = (Key) m.readObject();
                value = (Value) m.readObject();
                //IbisIdentifier ident = (IbisIdentifier) m.readObject();
                //Value value = new Value(Value.TYPE_POINTER, null);
                //value.owner = ident;
            }

        } catch (IOException e) {
            grtLogger.error("SATIN '" + satin.ident
                    + "': Global result table - error reading message", e);
        } catch (ClassNotFoundException e1) {
            grtLogger.error("SATIN '" + satin.ident
                    + "': Global result table - error reading message", e1);
        }

        if (GRT_TIMING) {
            tableDeserializationTimer.stop();
            satin.tableDeserializationTimer.add(tableDeserializationTimer);
        }

        try {
            m.finish();
        } catch (IOException e) {
            //ignore
        }

        synchronized (satin) {
            if (GRT_MESSAGE_COMBINING) {
                if (map != null) {
                    entries.putAll(map);
                }
            } else {
                if (key != null && value != null) {
                    /* if (entries.size() < max) */entries.put(key, value);
                }
            }
            if (GRT_STATS) {
                if (entries.size() > maxNumEntries) {
                    maxNumEntries = entries.size();
                }
            }
        }

        if (grtLogger.isDebugEnabled()) {
            if (GRT_MESSAGE_COMBINING) {
                grtLogger.debug("SATIN '" + satin.ident
                        + "': upcall finished: " + entries.size());
            } else {
                grtLogger.debug("SATIN '" + satin.ident
                        + "': upcall finished:" + key + "," + value + ","
                        + entries.size());
            }
        }

        if (GRT_TIMING) {
            handleUpdateTimer.stop();
            satin.handleUpdateTimer.add(handleUpdateTimer);
        }
    }

    public void print(java.io.PrintStream out) {
        synchronized (satin) {
            out.println("=GRT: " + satin.ident + "=");
            int i = 0;
            Iterator iter = entries.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                out.println("GRT[" + i + "]= " + entry.getKey() + ";"
                        + entry.getValue());
                i++;
            }
            out.println("=end of GRT " + satin.ident + "=");
        }
    }
}

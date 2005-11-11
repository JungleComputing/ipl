/* $Id$ */

package ibis.satin.impl;

import ibis.ipl.IbisError;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.WriteMessage;
import ibis.satin.ActiveTuple;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public abstract class TupleSpace extends Communication {

    static final boolean use_seq;

    private static HashMap space;

    private static boolean initialized = false;

    private static boolean tuple_connected = false;

    private static boolean tuple_connecting = false;

    static {
        space = new HashMap();
        use_seq = SUPPORT_TUPLE_MULTICAST && TUPLE_ORDERED;
    }

    public static void initTupleSpace() {
        synchronized (TupleSpace.class) {
            if (initialized) {
                return;
            }
            initialized = true;
            if (this_satin != null && !this_satin.closed) {
                tupleLogger.fatal("The tuple space currently only works with "
                        + "a closed world. Try running with -satin-closed");
                System.exit(1);
                // throw new IbisError("The tuple space currently only works "
                //         + "with a closed world. Try running with "
                //         + "-satin-closed");
            }
        }

        if (use_seq && this_satin != null) {
            enableActiveTupleOrdening();
        }
    }

    /**
     * Adds an element with the specified key to the global tuple space. If a
     * tuple with this key already exists, it is overwritten with the new
     * element. The propagation to other processors can take an arbitrary amount
     * of time, but it is guaranteed that after multiple updates by the same
     * processor, eventually all processors will have the latest value.
     * <p>
     * However, if multiple processors update the value of the same key, the
     * value of an updated key can be different on different processors.
     * 
     * @param key
     *            The key of the new tuple.
     * @param data
     *            The data associated with the key.
     */
    public static void addTuple(String key, Serializable data) {
        if (!initialized) {
            initTupleSpace();
        }
        if (tupleLogger.isDebugEnabled()) {
            tupleLogger.debug("SATIN '" + this_satin.ident
                    + ": added key " + key);
        }

        if (this_satin != null) { // can happen with sequential versions of
            // Satin
            // programs
            this_satin.broadcastTuple(key, data);
        }

        if (!use_seq || this_satin == null) {
            if (data instanceof ActiveTuple) {
                ((ActiveTuple) data).handleTuple(key);
            } else {
                synchronized (space) {
                    space.put(key, data);
                }
            }
        }

        if (tupleLogger.isDebugEnabled()) {
            tupleLogger.debug("SATIN '" + this_satin.ident
                    + ": adding of key " + key + " done");
        }
    }

    /**
     * Retrieves an element from the tuple space. If the element is not in the
     * space yet, this operation blocks until the element is inserted.
     * 
     * @param key
     *            the key of the element retrieved.
     * @return the data associated with the key.
     */
    public static Serializable peekTuple(String key) {
        Serializable data = null;

        if (!initialized) {
            initTupleSpace();
        }

        if (tupleLogger.isDebugEnabled()) {
            tupleLogger.debug("SATIN '" + this_satin.ident
                        + ": peek key " + key);
        }

        synchronized (space) {
            data = (Serializable) space.get(key);
        }

        return data;
    }

    /**
     * Retrieves an element from the tuple space. If the element is not in the
     * space yet, this operation blocks until the element is inserted.
     * 
     * @param key
     *            the key of the element retrieved.
     * @return the data associated with the key.
     */
    public static Serializable getTuple(String key) {
        Serializable data = null;

        if (!initialized) {
            initTupleSpace();
        }

        if (tupleLogger.isDebugEnabled()) {
            tupleLogger.debug("SATIN '" + this_satin.ident + ": get key "
                    + key);
        }

        synchronized (space) {
            while (data == null) {
                data = (Serializable) space.get(key);
                if (data == null) {
                    try {
                        if (tupleLogger.isDebugEnabled()) {
                            tupleLogger.debug("SATIN '"
                                    + this_satin.ident
                                    + ": get key " + key + " waiting");
                        }

                        space.wait();

                        if (tupleLogger.isDebugEnabled()) {
                            tupleLogger.debug("SATIN '" 
                                    + this_satin.ident
                                    + ": get key " + key + " waiting DONE");
                        }

                    } catch (Exception e) {
                        // Ignore.
                    }
                }
            }

            if (tupleLogger.isDebugEnabled()) {
                tupleLogger.debug("SATIN '" + this_satin.ident
                        + ": get key " + key + " DONE");
            }

            return data;
        }
    }

    /**
     * Removes an element from the tuple space.
     * 
     * @param key
     *            the key of the tuple to be removed.
     */
    public static void removeTuple(String key) {

        if (!initialized) {
            initTupleSpace();
        }

        if (tupleLogger.isDebugEnabled()) {
            tupleLogger.debug("SATIN '" + this_satin.ident
                    + ": removed key " + key);
        }
        if (this_satin != null) {
            this_satin.broadcastRemoveTuple(key);
            if (tupleLogger.isDebugEnabled()) {
                tupleLogger.debug("SATIN '" + this_satin.ident
                        + ": bcast remove key " + key + " done");
            }
        }
        if (!use_seq || this_satin == null) {
            synchronized (space) {
                if (ASSERTS && !space.containsKey(key)) {
                    throw new IbisError("Key " + key
                            + " is not in the tuple space");
                }

                space.remove(key);

                // also remove it from the new lists (if there)
                // int index = newKeys.indexOf(key);
                // if (index != -1) {
                //     newData.remove(index);
                // }
            }
        }
    }

    public static void remoteAdd(String key, Serializable data) {
        if (!initialized) {
            initTupleSpace();
        }
        if (tupleLogger.isDebugEnabled()) {
            tupleLogger.debug("SATIN '" + this_satin.ident
                    + ": remote add of key " + key);
        }

        synchronized (space) {
            space.put(key, data);
            // newKeys.add(key);
            // newData.add(data);
            space.notifyAll();
        }
        if (tupleLogger.isDebugEnabled()) {
            tupleLogger.debug("SATIN '" + this_satin.ident
                    + ": remote add of key " + key + " DONE");
        }
    }

    public static void remoteDel(String key) {
        if (!initialized) {
            initTupleSpace();
        }
        if (tupleLogger.isDebugEnabled()) {
            tupleLogger.debug("SATIN '" + this_satin.ident
                    + ": remote del of key " + key);
        }
        synchronized (space) {
            space.remove(key);

            // also remove it from the new lists (if there)
            // int index = newKeys.indexOf(key);
            // if (index != -1) {
            //     newData.remove(index);
            // }
        }
    }

    /* ------------------- tuple space stuff ---------------------- */

    private void tupleConnect() {
        boolean must_connect = false;

        synchronized (this) {
            if (!tuple_connected && !tuple_connecting) {
                tuple_connecting = true;
                must_connect = true;
            }
        }
        if (must_connect) {
            // You cannot hold the satin lock while connecting ...
            connectTuplePort();
            synchronized (this) {
                tuple_connected = true;
                this.notifyAll();
            }
        } else {
            synchronized (this) {
                while (!tuple_connected) {
                    try {
                        this.wait();
                    } catch (Exception e) {
                        // ignored
                    }
                }
            }
        }
    }

    protected void broadcastTuple(String key, Serializable data) {
        long count = 0;
        int size = 0;

        if (tupleLogger.isDebugEnabled()) {
            tupleLogger.debug("SATIN '" + ident + "': bcasting tuple "
                    + key);
        }

        synchronized (this) {
            size = victims.size();
        }

        if (size == 0 && (!use_seq || this_satin == null)) {
            return; // don't multicast when there is no-one.
        }

        if (TUPLE_TIMING) {
            tupleTimer.start();
        }

        if (SUPPORT_TUPLE_MULTICAST) {
            tupleConnect();
            synchronized (this) {
                tuple_message_sent = true;
            }

            try {
                WriteMessage writeMessage = tuplePort.newMessage();
                writeMessage.writeByte(Protocol.TUPLE_ADD);
                writeMessage.writeString(key);
                writeMessage.writeObject(data);

                if (TUPLE_STATS) {
                    tupleMsgs++;
                    count = writeMessage.finish();
                } else {
                    writeMessage.finish();
                }

            } catch (IOException e) {
                if (!FAULT_TOLERANCE) {
                    tupleLogger.fatal("SATIN '" + ident
                            + "': Got Exception while sending tuple update: "
                            + e, e);
                    System.exit(1);
                }
                //always happens after crash
            }

            // Wait until the message is delivered locally.
            if (use_seq) {
                synchronized (this) {
                    while (tuple_message_sent) {
                        try {
                            wait();
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                }
            }
        } else {
            for (int i = 0; i < size; i++) {
                try {
                    Victim v = null;
                    synchronized (this) {
                        v = victims.getVictim(i);
                    }
                    WriteMessage writeMessage = v.newMessage();
                    writeMessage.writeByte(Protocol.TUPLE_ADD);
                    writeMessage.writeString(key);
                    writeMessage.writeObject(data);

                    if (TUPLE_STATS && i == 0) {
                        tupleMsgs++;
                        count = writeMessage.finish();
                    } else {
                        writeMessage.finish();
                    }

                } catch (IOException e) {
                    tupleLogger.fatal("SATIN '" + ident
                            + "': Got Exception while sending tuple update: "
                            + e, e);
                    System.exit(1);
                }
            }
        }

        tupleBytes += count;

        if (TUPLE_TIMING) {
            tupleTimer.stop();
        }

        if (tupleLogger.isDebugEnabled()) {
            tupleLogger.debug("SATIN '" + ident + "': bcasting tuple "
                    + key + " DONE");
        }
    }

    private void connectTuplePort() {
        for (int i = 0; i < allIbises.size(); i++) {
            IbisIdentifier id = (IbisIdentifier) allIbises.get(i);
            if (!id.equals(ident)) {
                ReceivePortIdentifier r;
                try {
                    r = lookup("satin tuple port on " + id.name());
                    connect(tuplePort, r);
                } catch (IOException e) {
                    if (!FAULT_TOLERANCE) {
                        tupleLogger.fatal("SATIN '" + ident
                                + "': Got Exception while connecting tuple "
                                + "port: " + e, e);
                        System.exit(1);
                    }
                }
            }
        }
    }

    protected void broadcastRemoveTuple(String key) {
        long count = 0;
        int size = 0;

        if (tupleLogger.isDebugEnabled()) {
            tupleLogger.debug("SATIN '" + ident
                    + "': bcasting remove tuple" + key);
        }

        synchronized (this) {
            size = victims.size();
        }

        if (size == 0 && (!use_seq || this_satin == null)) {
            return; // don't multicast when there is no-one.
        }

        if (TUPLE_TIMING) {
            tupleTimer.start();
        }

        if (SUPPORT_TUPLE_MULTICAST) {
            synchronized (this) {
                tupleConnect();
                tuple_message_sent = true;
            }

            try {
                WriteMessage writeMessage = tuplePort.newMessage();
                writeMessage.writeByte(Protocol.TUPLE_DEL);
                writeMessage.writeString(key);

                if (TUPLE_STATS) {
                    tupleMsgs++;
                    count += writeMessage.finish();
                } else {
                    writeMessage.finish();
                }

            } catch (IOException e) {
                if (!FAULT_TOLERANCE) {
                    tupleLogger.fatal("SATIN '" + ident
                            + "': Got Exception while sending tuple update: "
                            + e, e);
                    System.exit(1);
                }
                //always happen after crashes
            }
            if (use_seq) {
                synchronized (this) {
                    while (tuple_message_sent) {
                        try {
                            wait();
                        } catch (Exception e) {
                            /* ignore */
                        }
                    }
                }
            }
        } else {
            for (int i = 0; i < size; i++) {
                try {
                    Victim v = null;
                    synchronized (this) {
                        v = victims.getVictim(i);
                    }
                    WriteMessage writeMessage = v.newMessage();
                    writeMessage.writeByte(Protocol.TUPLE_DEL);
                    writeMessage.writeString(key);

                    if (TUPLE_STATS && i == 0) {
                        tupleMsgs++;
                        count += writeMessage.finish();
                    } else {
                        writeMessage.finish();
                    }

                } catch (IOException e) {
                    tupleLogger.fatal("SATIN '" + ident
                            + "': Got Exception while sending tuple update: "
                            + e, e);
                    System.exit(1);
                }
            }
        }

        tupleBytes += count;

        if (TUPLE_TIMING) {
            tupleTimer.stop();
        }
    }

    // hold the lock when calling this
    protected void addToActiveTupleList(String key, Serializable data) {
        if (ASSERTS) {
            assertLocked(this);
        }
        activeTupleKeyList.add(key);
        activeTupleDataList.add(data);
    }

    void handleActiveTuples() {
        String key = null;
        ActiveTuple data = null;

        while (true) {
            synchronized (this) {
                if (activeTupleKeyList.size() == 0) {
                    gotActiveTuples = false;
                    return;
                }

		/*		if (TUPLE_TIMING) {
		    handleTupleTimer.start();
		    }*/

                // do upcall
                key = (String) activeTupleKeyList.remove(0);
                data = (ActiveTuple) activeTupleDataList.remove(0);
                if (tupleLogger.isDebugEnabled()) {
                    tupleLogger.debug("calling active tuple key = " + key
                            + " data = " + data);
                }

		/*		if (TUPLE_TIMING) {
		    handleTupleTimer.stop();
		    }*/
            }

            try {
                data.handleTuple(key);
            } catch (Throwable t) {
                tupleLogger.warn("WARNING: active tuple threw exception: "
                        + t, t);
            }
        }
    }

    private static void enableActiveTupleOrdening() {
        if (this_satin == null) {
            return;
        }
        connect(this_satin.tuplePort, this_satin.tupleReceivePort.identifier());
    }

    /* ------------------- for fault tolerance ---------------------- */

    //returns ready to send contents of the table
    Map getContents() {
        if (ASSERTS) {
            SatinBase.assertLocked(this_satin);
        }

        return space;
    }

    void addContents(Map contents) {
        if (ASSERTS) {
            SatinBase.assertLocked(this_satin);
        }

        synchronized (space) {
            space.putAll(contents);
            space.notifyAll();
        }
    }
}

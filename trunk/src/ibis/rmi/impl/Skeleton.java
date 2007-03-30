/* $Id$ */

package ibis.rmi.impl;

import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.rmi.RemoteException;

//import java.io.IOException;

public abstract class Skeleton {

    public int skeletonId;

    public Object destination;

    public SendPort[] stubs;

    public String stubType;

    private int num_ports = 0;

    private int max_ports = 0;

    private static final int INCR = 16;

    public Skeleton() {
        stubs = new SendPort[INCR];
        max_ports = INCR;
    }

    public void init(int id, Object o) {
        skeletonId = id;
        destination = o;
    }

    protected void finalize() {
        cleanup();
    }

    public synchronized int addStub(ReceivePortIdentifier rpi) {
        try {
            int id = 0;
            SendPort s = RTS.getSkeletonSendPort(rpi);

            for (int i = 0; i <= num_ports; i++) {
                if (stubs[i] == s) {
                    id = i;
                    break;
                }
            }

            if (id == 0) {
                id = ++num_ports;
                if (id >= max_ports) {
                    SendPort[] newports = new SendPort[max_ports + INCR];
                    for (int i = 0; i < max_ports; i++)
                        newports[i] = stubs[i];
                    max_ports += INCR;
                    stubs = newports;
                }
                stubs[id] = s;
            }

            return id;
        } catch (Exception e) {
            System.out.println("Exception in RMI runtime system: " + e);
            e.printStackTrace();
            System.exit(1);
        }
        return -1;
    }

    private void cleanup() {
        destination = null;
        for (int i = 0; i < stubs.length; i++) {
            //	    if (stubs[i] != null) {
            //		try {
            //		    stubs[i].close();
            //		} catch(Exception e) {
            //		}
            //	    }
            stubs[i] = null;
        }
    }

    public abstract void upcall(ReadMessage m, int method, int stubID)
            throws RemoteException;
}
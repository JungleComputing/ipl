/* $Id$ */

package ibis.impl.nameServer.tcp;

import ibis.connect.IbisSocketFactory;

import ibis.ipl.IbisRuntimeException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

class ReceivePortNameServer extends Thread implements Protocol {

    private Hashtable ports;

    Hashtable requestedPorts;

    boolean finishSweeper = false;

    private ServerSocket serverSocket;

    private DataInputStream in;

    private DataOutputStream out;

    private boolean silent;

    private static class PortLookupRequest {
        Socket s;

        DataInputStream in;

        DataOutputStream out;

        String[] names;

        byte[][] ports;

        long timeout;

        int unknown;

        boolean done = false;

        PortLookupRequest(Socket s, DataInputStream in, DataOutputStream out,
                String[] names, byte[][] ports, long timeout, int unknown) {
            this.s = s;
            this.in = in;
            this.out = out;
            this.names = names;
            this.ports = ports;
            this.timeout = timeout;
            this.unknown = unknown;
        }

        public void writeResult() throws IOException {
            try {
                out.writeByte(PORT_KNOWN);
                for (int i = 0; i < ports.length; i++) {
                    out.writeInt(ports[i].length);
                    out.write(ports[i]);
                }
            } finally {
                NameServer.closeConnection(in, out, s);
            }

            done = true;
        }
    }

    ReceivePortNameServer(boolean silent, IbisSocketFactory socketFactory)
            throws IOException {
        ports = new Hashtable();
        requestedPorts = new Hashtable();
        this.silent = silent;
        serverSocket = socketFactory.createServerSocket(0, null, true, null);
        setName("ReceivePort Name Server");
        start();
    }

    int getPort() {
        return serverSocket.getLocalPort();
    }

    private void handlePortNew() throws IOException {

        byte[] id, storedId;

        String name = in.readUTF();
        int len = in.readInt();
        id = new byte[len];
        in.readFully(id, 0, len);

        /* Check wheter the name is in use. */
        storedId = (byte[]) ports.get(name);

        if (storedId != null) {
            out.writeByte(PORT_REFUSED);
        } else {
            out.writeByte(PORT_ACCEPTED);
            addPort(name, id);
        }
    }

    //gosia
    private void handlePortRebind() throws IOException {

        byte[] id;

        String name = in.readUTF();
        int len = in.readInt();
        id = new byte[len];
        in.readFully(id, 0, len);

        /* Don't check whether the name is in use. */
        out.writeByte(PORT_ACCEPTED);
        addPort(name, id);
    }

    private void addPort(String name, byte[] id)
            throws IOException {
        ports.put(name, id);
        synchronized (requestedPorts) {
            ArrayList v = (ArrayList) requestedPorts.get(name);
            if (v != null) {
                requestedPorts.remove(name);
                for (int i = 0; i < v.size(); i++) {
                    PortLookupRequest p = (PortLookupRequest) v.get(i);
                    for (int j = 0; i < p.names.length; j++) {
                        if (p.names[j].equals(name)) {
                            p.ports[j] = id;
                            p.unknown--;
                            if (p.unknown == 0) {
                                p.writeResult();
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    private void handlePortList() throws IOException {

        ArrayList goodNames = new ArrayList();

        String pattern = in.readUTF();
        Enumeration names = ports.keys();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            if (name.matches(pattern)) {
                goodNames.add(name);
            }
        }

        out.writeByte(goodNames.size());
        for (int i = 0; i < goodNames.size(); i++) {
            out.writeUTF((String) goodNames.get(i));
        }
    }

    //end gosia	

    private class RequestSweeper extends Thread {
        public void run() {
            long timeout = 1000000L;
            while (true) {
                long current = System.currentTimeMillis();
                synchronized (requestedPorts) {
                    if (finishSweeper) {
                        return;
                    }
                    Enumeration names = requestedPorts.keys();
                    while (names.hasMoreElements()) {
                        String name = (String) names.nextElement();
                        ArrayList v = (ArrayList) requestedPorts.get(name);
                        if (v != null) {
                            for (int i = v.size() - 1; i >= 0; i--) {
                                PortLookupRequest p
                                        = (PortLookupRequest) v.get(i);
                                if (! p.done && p.timeout != 0) {
                                    if (p.timeout <= current) {
                                        p.done = true;
                                        try {
                                            p.out.writeByte(PORT_UNKNOWN);
                                            p.out.writeInt(p.unknown);
                                            for (int j = 0; j < p.ports.length;
                                                    j++) {
                                                if (p.ports[j] == null) {
                                                    p.out.writeUTF(p.names[j]);
                                                }
                                            }
                                        } catch (IOException e) {
                                            if (! silent) {
                                                System.out.println("RequestSweeper "
                                                        + "got IOException" + e);
                                                e.printStackTrace();
                                            }
                                        } finally {
                                            NameServer.closeConnection(
                                                    p.in, p.out, p.s);
                                        }
                                        v.remove(i);
                                    } else if (p.timeout - current < timeout) {
                                        timeout = p.timeout - current;
                                    }
                                }
                            }
                            if (v.size() == 0) {
                                requestedPorts.remove(name);
                            }
                        }
                    }
                    try {
                        if (timeout < 100) {
                            timeout = 100;
                        }
                        requestedPorts.wait(timeout);
                    } catch (InterruptedException e) {
                        // ignored
                    }
                }
            }
        }
    }

    private void handlePortLookup(Socket s) throws IOException {

        int count = in.readInt();
        System.out.println("handlePortLookup, count = " + count);
        String[] names = new String[count];
        byte[][] prts = new byte[count][];
        int unknown = 0;

        for (int i = 0; i < count; i++) {
            names[i] = in.readUTF();
            prts[i] = (byte[]) ports.get(names[i]);
            if (prts[i] == null) {
                unknown++;
            }
        }

        long timeout = in.readLong();

        if (timeout != 0) {
            timeout += System.currentTimeMillis();
        }

        PortLookupRequest p = new PortLookupRequest(s, in, out, names, prts,
                timeout, unknown);

        if (unknown == 0) {
            p.writeResult();
            return;
        }

        synchronized (requestedPorts) {
            for (int i = 0; i < count; i++) {
                if (prts[i] == null) {
                    ArrayList v = (ArrayList) requestedPorts.get(names[i]);
                    if (v == null) {
                        v = new ArrayList();
                        requestedPorts.put(names[i], v);
                    }
                    v.add(p);
                }
            }
            if (timeout != 0) {
                requestedPorts.notify();
            }
        }
    }

    private void handlePortFree() throws IOException {
        byte[] id;

        String name = in.readUTF();

        id = (byte[]) ports.get(name);

        if (id == null) {
            out.writeByte(1);
        }
        ports.remove(name);
        out.writeByte(0);
    }

    public void run() {

        Socket s;
        boolean stop = false;
        int opcode;

        RequestSweeper p = new RequestSweeper();
        p.setDaemon(true);
        p.start();

        while (!stop) {

            try {
                s = serverSocket.accept();
            } catch (Exception e) {
                throw new IbisRuntimeException(
                        "ReceivePortNameServer: got an error ", e);
            }

            in = null;
            out = null;
            boolean mustClose = true;

            try {
                in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
                out = new DataOutputStream(
                        new BufferedOutputStream(s.getOutputStream(), 4096));

                opcode = in.readByte();

                switch (opcode) {
                case (PORT_NEW):
                    handlePortNew();
                    break;

                case (PORT_REBIND):
                    handlePortRebind();
                    break;

                case (PORT_LIST):
                    handlePortList();
                    break;

                case (PORT_FREE):
                    handlePortFree();
                    break;

                case (PORT_LOOKUP):
                    mustClose = false;
                    handlePortLookup(s);
                    break;

                case (PORT_EXIT):
                    synchronized (requestedPorts) {
                        finishSweeper = true;
                        requestedPorts.notifyAll();
                    }
                    serverSocket.close();
                    return;
                default:
                    if (! silent) {
                        System.err.println("ReceivePortNameServer: got an illegal "
                                + "opcode " + opcode);
                    }
                }
            } catch (Exception e1) {
                if (! silent) {
                    System.err.println("Got an exception in "
                            + "ReceivePortNameServer.run " + e1 + ", continuing");
                    // e1.printStackTrace();
                }
            } finally {
                if (mustClose) {
                    NameServer.closeConnection(in, out, s);
                }
            }
        }
    }
}

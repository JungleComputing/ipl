package ibis.impl.nameServer.tcp;

import ibis.io.DummyInputStream;
import ibis.io.DummyOutputStream;
import ibis.ipl.IbisRuntimeException;
import ibis.ipl.ReceivePortIdentifier;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

    private ObjectInputStream in;

    private ObjectOutputStream out;

    private static class PortLookupRequest {
        Socket s;

        ObjectInputStream in;

        ObjectOutputStream out;

        String name;

        long timeout;

        PortLookupRequest(Socket s, ObjectInputStream in,
                ObjectOutputStream out, String name, long timeout) {
            this.s = s;
            this.in = in;
            this.out = out;
            this.name = name;
            this.timeout = timeout;
        }
    }

    ReceivePortNameServer() throws IOException {
        ports = new Hashtable();
        requestedPorts = new Hashtable();
        serverSocket = NameServerClient.socketFactory.createServerSocket(0,
                null, true);
        setName("ReceivePort Name Server");
        start();
    }

    int getPort() {
        return serverSocket.getLocalPort();
    }

    private void handlePortNew() throws IOException, ClassNotFoundException {

        ReceivePortIdentifier id, storedId;

        String name = in.readUTF();
        id = (ReceivePortIdentifier) in.readObject();

        /* Check wheter the name is in use. */
        storedId = (ReceivePortIdentifier) ports.get(name);

        if (storedId != null) {
            out.writeByte(PORT_REFUSED);
        } else {
            out.writeByte(PORT_ACCEPTED);
            addPort(name, id);
        }
    }

    //gosia
    private void handlePortRebind() throws IOException, ClassNotFoundException {

        ReceivePortIdentifier id;

        String name = in.readUTF();
        id = (ReceivePortIdentifier) in.readObject();

        /* Don't check whether the name is in use. */
        out.writeByte(PORT_ACCEPTED);
        addPort(name, id);
    }

    private void addPort(String name, ReceivePortIdentifier id)
            throws IOException {
        ports.put(name, id);
        synchronized (requestedPorts) {
            ArrayList v = (ArrayList) requestedPorts.get(name);
            if (v != null) {
                requestedPorts.remove(name);
                for (int i = 0; i < v.size(); i++) {
                    PortLookupRequest p = (PortLookupRequest) v.get(i);
                    p.out.writeByte(PORT_KNOWN);
                    p.out.writeObject(id);
                    NameServerClient.socketFactory.close(p.in, p.out, p.s);
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
                                PortLookupRequest p = (PortLookupRequest) v
                                        .get(i);
                                if (p.timeout != 0) {
                                    if (p.timeout <= current) {
                                        try {
                                            p.out.writeByte(PORT_UNKNOWN);
                                        } catch (IOException e) {
                                            System.out
                                                    .println("RequestSweeper got IOException"
                                                            + e);
                                            e.printStackTrace();
                                        }
                                        NameServerClient.socketFactory.close(
                                                p.in, p.out, p.s);
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
                        if (timeout < 100)
                            timeout = 100;
                        requestedPorts.wait(timeout);
                    } catch (InterruptedException e) {
                        // ignored
                    }
                }
            }
        }
    }

    private void handlePortLookup(Socket s) throws IOException {

        ReceivePortIdentifier storedId;

        String name = in.readUTF();
        long timeout = in.readLong();

        storedId = (ReceivePortIdentifier) ports.get(name);

        if (storedId != null) {
            out.writeByte(PORT_KNOWN);
            out.writeObject(storedId);
            NameServerClient.socketFactory.close(in, out, s);
        } else {
            if (timeout != 0) {
                timeout += System.currentTimeMillis();
            }
            PortLookupRequest p = new PortLookupRequest(s, in, out, name,
                    timeout);
            synchronized (requestedPorts) {
                ArrayList v = (ArrayList) requestedPorts.get(name);
                if (v == null) {
                    v = new ArrayList();
                    requestedPorts.put(name, v);
                }
                v.add(p);
                if (timeout != 0)
                    requestedPorts.notify();
            }
        }
    }

    private void handlePortFree() throws IOException {
        ReceivePortIdentifier id;

        String name = in.readUTF();

        id = (ReceivePortIdentifier) ports.get(name);

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
                s = NameServerClient.socketFactory.accept(serverSocket);
            } catch (Exception e) {
                throw new IbisRuntimeException(
                        "ReceivePortNameServer: got an error ", e);
            }

            try {
                DummyInputStream di = new DummyInputStream(s.getInputStream());
                in = new ObjectInputStream(new BufferedInputStream(di));
                DummyOutputStream dos = new DummyOutputStream(s
                        .getOutputStream());
                out = new ObjectOutputStream(
                        new BufferedOutputStream(dos, 4096));

                opcode = in.readByte();

                switch (opcode) {
                case (PORT_NEW):
                    handlePortNew();
                    break;

                //gosia
                case (PORT_REBIND):
                    handlePortRebind();
                    break;

                case (PORT_LIST):
                    handlePortList();
                    break;
                //end gosia

                case (PORT_FREE):
                    handlePortFree();
                    break;

                case (PORT_LOOKUP):
                    handlePortLookup(s);
                    break;
                case (PORT_EXIT):
                    NameServerClient.socketFactory.close(in, out, s);
                    synchronized (requestedPorts) {
                        finishSweeper = true;
                        requestedPorts.notifyAll();
                    }
                    serverSocket.close();
                    return;
                default:
                    System.err
                            .println("ReceivePortNameServer: got an illegal opcode "
                                    + opcode);
                }

                if (opcode != PORT_LOOKUP) {
                    NameServerClient.socketFactory.close(in, out, s);
                }
            } catch (Exception e1) {
                System.err
                        .println("Got an exception in ReceivePortNameServer.run "
                                + e1 + ", continuing");
                //				e1.printStackTrace();
                NameServerClient.socketFactory.close(in, out, s);
            }
        }
    }
}
package ibis.rmi.impl;

import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;
import ibis.rmi.server.RemoteRef;
import ibis.rmi.server.RemoteStub;

import java.io.IOException;

public class Stub extends RemoteStub {

    transient protected int stubID;

    transient protected SendPort send;

    transient protected ReceivePort reply;

    protected ReceivePortIdentifier skeletonPortId;

    protected int skeletonId;

    transient private boolean initialized = false;

    transient private boolean initializing = false;

    public Stub() {
        // nothing here for now.
    }

    public void init(SendPort s, ReceivePort r, int id, int skelId,
            ReceivePortIdentifier rpi, boolean inited, RemoteRef reference) {

        stubID = id;
        skeletonPortId = rpi;
        this.initialized = inited;
        send = s;
        reply = r;
        skeletonId = skelId;

        if (reference != null) {
            this.ref = reference;
        }
        // System.out.println("Stub " + this + " gets ref " + ref);
    }

    public final void initSend() throws IOException {
        if (!initialized) {
            synchronized (this) {
                if (initializing) {
                    while (!initialized) {
                        try {
                            wait();
                        } catch (Exception e) {
                            // ignored
                        }
                    }
                    return;
                }
                initializing = true;
            }
            if (send == null) {
                // System.out.println("Setting up connection for " + this);
                send = RTS.getStubSendPort(skeletonPortId);
                reply = RTS.getStubReceivePort(skeletonPortId.ibis());
            }
            WriteMessage wm = newMessage();
            wm.writeInt(-1);
            wm.writeInt(0);
            wm.writeObject(reply.identifier());
            wm.finish();

            ReadMessage rm = reply.receive();
            stubID = rm.readInt();
            rm.readInt();
            try {
                rm.readObject();
            } catch (Exception e) {
                // don't care.
            }
            rm.finish();
            synchronized (this) {
                initialized = true;
                notifyAll();
            }
        }
    }

    public final WriteMessage newMessage() throws IOException {
        WriteMessage w = send.newMessage();
        w.writeInt(skeletonId);
        return w;
    }

    protected void finalize() {
        // Give up resources.
        try {
            /* if (send != null) send.close(); */
            if (reply != null) {
                RTS.putStubReceivePort(reply, skeletonPortId.ibis());
            }
        } catch (Exception e) {
            // don't care.
        }
    }
}

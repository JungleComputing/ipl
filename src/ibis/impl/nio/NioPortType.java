/* $Id: NioPortType.java 4360 2006-09-29 12:00:16Z ceriel $ */

package ibis.impl.nio;

import ibis.impl.PortType;
import ibis.impl.ReceivePort;
import ibis.impl.SendPort;
import ibis.ipl.CapabilitySet;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.Upcall;

import java.io.IOException;

import org.apache.log4j.Logger;

class NioPortType extends PortType {

    static final byte IMPLEMENTATION_BLOCKING = 0;

    static final byte IMPLEMENTATION_NON_BLOCKING = 1;

    static final byte IMPLEMENTATION_THREAD = 2;

    static final String[] IMPLEMENTATION_NAMES = { "Blocking", "Non Blocking",
            "Thread" };

    private static Logger logger = Logger.getLogger(NioPortType.class);

    byte sendPortImplementation;

    byte receivePortImplementation;

    NioPortType(NioIbis ibis, CapabilitySet p) {
        super(ibis, p);

        String typeSPI = null;

        if (typeSPI != null) {
            if (typeSPI.equalsIgnoreCase("Blocking")) {
                sendPortImplementation = IMPLEMENTATION_BLOCKING;
            } else if (typeSPI.equalsIgnoreCase("NonBlocking")) {
                sendPortImplementation = IMPLEMENTATION_NON_BLOCKING;
            } else if (typeSPI.equalsIgnoreCase("Thread")) {
                sendPortImplementation = IMPLEMENTATION_THREAD;
            } else {
                throw new Error("unknown value \"" + typeSPI
                        + "\" for sendport implementation");
            }
        } else if (oneToMany) {
            sendPortImplementation = IMPLEMENTATION_NON_BLOCKING;
        } else {
            sendPortImplementation = IMPLEMENTATION_BLOCKING;
        }

        String typeRPI = null;

        if (typeRPI != null) {
            if (typeRPI.equalsIgnoreCase("Blocking")) {
                receivePortImplementation = IMPLEMENTATION_BLOCKING;
            } else if (typeRPI.equalsIgnoreCase("NonBlocking")) {
                receivePortImplementation = IMPLEMENTATION_NON_BLOCKING;
            } else if (typeRPI.equalsIgnoreCase("Thread")) {
                receivePortImplementation = IMPLEMENTATION_THREAD;
            } else {
                throw new Error("unknown value \"" + typeRPI
                        + "\" for receiveport implementation");
            }
        } else if (manyToOne) {
            receivePortImplementation = IMPLEMENTATION_NON_BLOCKING;
        } else {
            receivePortImplementation = IMPLEMENTATION_BLOCKING;
        }

        logger.info("new port type : serialization = "
                + serialization + ", send port implementation = "
                + IMPLEMENTATION_NAMES[sendPortImplementation]
                + ", receive port implementation = "
                + IMPLEMENTATION_NAMES[receivePortImplementation]
                + ", numbered = " + numbered);

    }

    protected SendPort doCreateSendPort(String name,
            SendPortDisconnectUpcall cU, boolean connectionDowncalls)
            throws IOException {
        return new NioSendPort(ibis, this, name, connectionDowncalls, cU);
    }

    protected ReceivePort doCreateReceivePort(String name, Upcall u,
            ReceivePortConnectUpcall cU, boolean connectionDowncalls)
            throws IOException {
        switch (receivePortImplementation) {
        case IMPLEMENTATION_BLOCKING:
            return new BlockingChannelNioReceivePort(ibis, this, name, u,
                    connectionDowncalls, cU);
        case IMPLEMENTATION_NON_BLOCKING:
            return new NonBlockingChannelNioReceivePort(ibis, this, name, u,
                    connectionDowncalls, cU);
        case IMPLEMENTATION_THREAD:
            return new ThreadNioReceivePort(ibis, this, name, u,
                    connectionDowncalls, cU);
        default:
            throw new Error("unknown receiveport implementation" + " type "
                    + receivePortImplementation);
        }
    }
}

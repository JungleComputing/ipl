/* $Id$ */

package ibis.impl.nio;

import ibis.io.DataInputStream;
import ibis.io.DataOutputStream;
import ibis.io.SerializationBase;
import ibis.io.SerializationInput;
import ibis.io.SerializationOutput;
import ibis.ipl.IbisError;
import ibis.ipl.IbisException;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortConnectUpcall;
import ibis.ipl.StaticProperties;
import ibis.ipl.Upcall;

import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

class NioPortType extends PortType implements Config {

    StaticProperties p;

    String name;

    NioIbis ibis;

    String serializationType;

    static final byte IMPLEMENTATION_BLOCKING = 0;

    static final byte IMPLEMENTATION_NON_BLOCKING = 1;

    static final byte IMPLEMENTATION_THREAD = 2;

    static final String[] IMPLEMENTATION_NAMES = { "Blocking", "Non Blocking",
            "Thread" };

    static Logger logger = Logger.getLogger(NioPortType.class.getName());

    byte sendPortImplementation;

    byte receivePortImplementation;

    final boolean numbered;

    final boolean oneToMany;

    final boolean manyToOne;

    NioPortType(NioIbis ibis, String name, StaticProperties p)
            throws IbisException {
        this.ibis = ibis;
        this.name = name;
        this.p = p;

        Properties systemProperties = System.getProperties();

        oneToMany = p.isProp("Communication", "OneToMany");
        manyToOne = p.isProp("Communication", "ManyToOne");

        if (systemProperties.getProperty(NioIbis.s_numbered) != null) {
            numbered = true;
        } else {
            numbered = p.isProp("Communication", "Numbered");
        }

        serializationType = this.p.find("Serialization");
        if (serializationType == null) {
            serializationType = "sun";
        }

        if (p.isProp("Serialization", "byte")) {
            if (numbered) {
                throw new IbisException("Numbered communication is not"
                        + " supported on byte serialization streams");
            }
        }

        String globalSPI = systemProperties.getProperty(NioIbis.s_spi);
        String typeSPI = systemProperties.getProperty(NioIbis.s_spi + "."
                + name);

        if (typeSPI != null) {
            if (typeSPI.equalsIgnoreCase("Blocking")) {
                sendPortImplementation = IMPLEMENTATION_BLOCKING;
            } else if (typeSPI.equalsIgnoreCase("NonBlocking")) {
                sendPortImplementation = IMPLEMENTATION_NON_BLOCKING;
            } else if (typeSPI.equalsIgnoreCase("Thread")) {
                sendPortImplementation = IMPLEMENTATION_THREAD;
            } else {
                throw new IbisException("unknown value \"" + typeSPI
                        + "\" for sendport implementation");
            }
        } else if (globalSPI != null) {
            if (globalSPI.equalsIgnoreCase("Blocking")) {
                sendPortImplementation = IMPLEMENTATION_BLOCKING;
            } else if (globalSPI.equalsIgnoreCase("NonBlocking")) {
                sendPortImplementation = IMPLEMENTATION_NON_BLOCKING;
            } else if (globalSPI.equalsIgnoreCase("Thread")) {
                sendPortImplementation = IMPLEMENTATION_THREAD;
            } else {
                throw new IbisException("unknown value \"" + typeSPI
                        + "\" for sendport implementation");
            }
        } else if (oneToMany) {
            sendPortImplementation = IMPLEMENTATION_NON_BLOCKING;
        } else {
            sendPortImplementation = IMPLEMENTATION_BLOCKING;
        }

        String globalRPI = systemProperties.getProperty(NioIbis.s_rpi);
        String typeRPI = systemProperties.getProperty(NioIbis.s_rpi + "."
                + name);

        if (typeRPI != null) {
            if (typeRPI.equalsIgnoreCase("Blocking")) {
                receivePortImplementation = IMPLEMENTATION_BLOCKING;
            } else if (typeRPI.equalsIgnoreCase("NonBlocking")) {
                receivePortImplementation = IMPLEMENTATION_NON_BLOCKING;
            } else if (typeRPI.equalsIgnoreCase("Thread")) {
                receivePortImplementation = IMPLEMENTATION_THREAD;
            } else {
                throw new IbisException("unknown value \"" + typeRPI
                        + "\" for receiveport implementation");
            }
        } else if (globalRPI != null) {
            if (globalRPI.equalsIgnoreCase("Blocking")) {
                receivePortImplementation = IMPLEMENTATION_BLOCKING;
            } else if (globalRPI.equalsIgnoreCase("NonBlocking")) {
                receivePortImplementation = IMPLEMENTATION_NON_BLOCKING;
            } else if (globalRPI.equalsIgnoreCase("Thread")) {
                receivePortImplementation = IMPLEMENTATION_THREAD;
            } else {
                throw new IbisException("unknown value \"" + globalRPI
                        + "\" for receiveport implementation");
            }
        } else if (manyToOne) {
            receivePortImplementation = IMPLEMENTATION_NON_BLOCKING;
        } else {
            receivePortImplementation = IMPLEMENTATION_BLOCKING;
        }

        logger.info("new port type: serialization = " + serializationType
                + "\nsend port implementation = "
                + IMPLEMENTATION_NAMES[sendPortImplementation]
                + "\nreceive port implementation = "
                + IMPLEMENTATION_NAMES[receivePortImplementation]
                + "\nnumbered = " + numbered + "\n");

    }

    public String name() {
        return name;
    }

    private boolean equals(NioPortType other) {
        return name.equals(other.name) && ibis.equals(other.ibis);
    }

    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof NioPortType)) {
            return false;
        }
        return equals((NioPortType) other);
    }

    public int hashCode() {
        return name.hashCode() + ibis.hashCode();
    }

    public StaticProperties properties() {
        return p;
    }

    public SendPort createSendPort(String name, SendPortConnectUpcall cU,
            boolean connectionAdministration) throws IOException {
        return new NioSendPort(ibis, this, name, connectionAdministration, cU);
    }

    public ReceivePort createReceivePort(String name, Upcall u,
            ReceivePortConnectUpcall cU, boolean connectionAdministration)
            throws IOException {

        switch (receivePortImplementation) {
        case IMPLEMENTATION_BLOCKING:
            return new BlockingChannelNioReceivePort(ibis, this, name, u,
                    connectionAdministration, cU);
        case IMPLEMENTATION_NON_BLOCKING:
            return new NonBlockingChannelNioReceivePort(ibis, this, name, u,
                    connectionAdministration, cU);
        case IMPLEMENTATION_THREAD:
            return new ThreadNioReceivePort(ibis, this, name, u,
                    connectionAdministration, cU);
        default:
            throw new IbisError("unknown receiveport implementation" + " type "
                    + receivePortImplementation);
        }
    }

    public String toString() {
        return ("(NioPortType: name = " + name + ")");
    }

    SerializationOutput createSerializationOutputStream(DataOutputStream out) {
        return SerializationBase.createSerializationOutput(serializationType,
                out);

    }

    SerializationInput createSerializationInputStream(DataInputStream in) {
        return SerializationBase
                .createSerializationInput(serializationType, in);
    }
}

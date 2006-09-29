/* $Id$ */

package ibis.impl.messagePassing;

import ibis.ipl.IbisException;
import ibis.ipl.SendPortConnectUpcall;
import ibis.ipl.StaticProperties;

import java.io.IOException;

/**
 * messagePassing PortType
 */
public class PortType extends ibis.ipl.PortType {

    private StaticProperties p;

    private String name;

    boolean numbered;

    public static final byte SERIALIZATION_NONE = 0;

    public static final byte SERIALIZATION_SUN = 1;

    public static final byte SERIALIZATION_IBIS = 2;

    public static final byte SERIALIZATION_DATA = 3;

    public byte serializationType = SERIALIZATION_SUN;

    PortType(String name, StaticProperties p) throws IbisException {
        this.name = name;
        this.p = p;

        String ser = p.find("Serialization");
        if (ser == null) {
            this.p = new StaticProperties(p);
            this.p.add("Serialization", "bytes");
            serializationType = SERIALIZATION_NONE;
        } else if (ser.equals("object")) {
            serializationType = SERIALIZATION_IBIS;
        } else if (ser.equals("byte")) {
            serializationType = SERIALIZATION_NONE;
        } else if (ser.equals("sun")) {
            serializationType = SERIALIZATION_SUN;
        } else if (ser.equals("manta")) {
            // For backwards compatibility ...
            serializationType = SERIALIZATION_IBIS;
        } else if (ser.equals("ibis")) {
            serializationType = SERIALIZATION_IBIS;
        } else if (ser.equals("data")) {
            serializationType = SERIALIZATION_DATA;
        } else {
            throw new IbisException("Unknown Serialization type " + ser);
        }
        numbered = p.isProp("communication", "Numbered");
    }

    public String name() {
        return name;
    }

    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof PortType)) {
            return false;
        }

        PortType temp = (PortType) other;

        return name.equals(temp.name);
    }

    public int hashCode() {
        return name.hashCode();
    }

    public StaticProperties properties() {
        return p;
    }

    public ibis.ipl.SendPort createSendPort(String portname,
            SendPortConnectUpcall cU, boolean connectionAdministration)
            throws IOException {

        if (cU != null) {
            System.err.println(this
                    + ": createSendPort with ConnectUpcall. UNIMPLEMENTED");
            connectionAdministration = true;
        }

        if (connectionAdministration) {
            System.err.println(this
                    + ": createSendPort with ConnectionAdministration. "
                    + " UNIMPLEMENTED");
        }

        SendPort s;

        switch (serializationType) {
        case PortType.SERIALIZATION_NONE:
            if (Ibis.DEBUG_RUTGER) {
                System.err.println("MSG: NO SER, name = " + portname);
            }
            s = new SendPort(this, portname);
            break;

        case PortType.SERIALIZATION_SUN:
            if (Ibis.DEBUG_RUTGER) {
                System.err.println("MSG: SUN SER, name = " + portname);
            }
            s = new SerializeSendPort(this, portname);
            break;

        case PortType.SERIALIZATION_DATA:
        case PortType.SERIALIZATION_IBIS:
            if (Ibis.DEBUG_RUTGER) {
                System.err.println("MSG: IBIS SER, name = " + portname);
            }
            s = new IbisSendPort(this, portname);
            break;

        default:
            throw new Error("No such serialization type " + serializationType);
        }

        if (Ibis.DEBUG) {
            System.out.println(Ibis.myIbis.myCpu + ": Sendport " + portname
                    + " created of of type '" + this + "'" + " cpu "
                    + s.ident.cpu + " port " + s.ident.port);
        }

        return s;
    }

    public ibis.ipl.ReceivePort createReceivePort(String nm, ibis.ipl.Upcall u,
            ibis.ipl.ReceivePortConnectUpcall cU,
            boolean connectionAdministration, boolean global) throws IOException {

        ReceivePort port = new ReceivePort(this, nm, u, cU,
                connectionAdministration, global);

        if (Ibis.DEBUG) {
            System.out.println(Ibis.myIbis.myCpu
                    + ": Receiveport created of type '" + this.name
                    + "', name = '" + nm + "'" + " id " + port.identifier());
        }

        return port;
    }

    void freeReceivePort(String nm) throws IOException {
        ((Registry) Ibis.myIbis.registry()).unbind(nm);
    }

    public String toString() {
        return ("(PortType: name = " + name + ")");
    }
}

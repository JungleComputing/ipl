/* $Id$ */

package ibis.impl.messagePassing;

import ibis.io.Conversion;

import java.io.IOException;

/**
 * MessagePassing ReceivePortIdentifier that exploits closed-world
 * properties, so Ibises and ports are just ranked.
 */
public final class ReceivePortIdentifier implements
        ibis.ipl.ReceivePortIdentifier, java.io.Serializable {

    private String name;

    private String type;

    int cpu;

    int port;

    private ibis.ipl.IbisIdentifier ibisIdentifier;

    private transient byte[] serialForm;

    ReceivePortIdentifier(String name, String type) {

        synchronized (Ibis.myIbis) {
            port = Ibis.myIbis.receivePort++;
        }
        cpu = Ibis.myIbis.myCpu;
        this.name = name;
        this.type = type;
        ibisIdentifier = Ibis.myIbis.identifier();
        makeSerialForm();
    }

    private void makeSerialForm() {
        try {
            serialForm = Conversion.object2byte(this);
        } catch (IOException e) {
            throw new Error("Cannot serialize myself", e);
        }
    }

    byte[] getSerialForm() {
        if (serialForm == null) {
            makeSerialForm();
        }
        return serialForm;
    }

    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }

        if (!(other instanceof ReceivePortIdentifier)) {
            return false;
        }

        ReceivePortIdentifier temp = (ReceivePortIdentifier) other;
        return (cpu == temp.cpu && port == temp.port
                && name().equals(temp.name()) && type().equals(temp.type()));
    }

    //gosia
    public int hashCode() {
        return name.hashCode() + type.hashCode() + cpu + port;
    }

    public String name() {
        return name;
    }

    public String type() {
        if (type != null) {
            return type;
        }
        return "__notype__";
    }

    public ibis.ipl.IbisIdentifier ibis() {
        return ibisIdentifier;
    }

    public String toString() {
        return ("(RecPortIdent: name \"" + (name == null ? "null" : name)
                + "\" type \"" + (type == null ? "null" : type)
                + "\" cpu " + cpu
                + " port " + port
                + " ibis \""
                + ((ibisIdentifier == null || ibisIdentifier.name() == null)
                    ? "null" : ibisIdentifier.name()) + "\")");
    }

}

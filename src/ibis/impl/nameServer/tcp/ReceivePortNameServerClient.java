/* $Id$ */

package ibis.impl.nameServer.tcp;

import ibis.io.DummyInputStream;
import ibis.io.DummyOutputStream;
import ibis.ipl.BindingException;
import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.Socket;

import org.apache.log4j.Logger;

class ReceivePortNameServerClient implements Protocol {

    InetAddress server;

    int port;

    InetAddress localAddress;

    private static Logger logger
            = ibis.util.GetLogger.getLogger(ReceivePortNameServerClient.class.getName());

    ReceivePortNameServerClient(InetAddress localAddress, InetAddress server,
            int port) {
        this.server = server;
        this.port = port;
        this.localAddress = localAddress;
    }

    public ReceivePortIdentifier lookup(String name, long timeout)
            throws IOException {
        ObjectOutputStream out;
        ObjectInputStream in;
        ReceivePortIdentifier id = null;
        int result;
        Socket s = NameServerClient.nsConnect(server, port, localAddress,
                false, 10);

        DummyOutputStream dos = new DummyOutputStream(s.getOutputStream());
        out = new ObjectOutputStream(new BufferedOutputStream(dos));

        // request a new Port.
        out.writeByte(PORT_LOOKUP);
        out.writeUTF(name);
        out.writeLong(timeout);
        out.flush();

        DummyInputStream di = new DummyInputStream(s.getInputStream());
        in = new ObjectInputStream(new BufferedInputStream(di));
        result = in.readByte();
        logger.debug(this + ": lookup port \"" + name + "\"");

        switch (result) {
        case PORT_UNKNOWN:
            logger.debug("Port " + name + ": PORT_UNKNOWN");
            NameServerClient.socketFactory.close(in, out, s);
            throw new ConnectionTimedOutException("could not connect");
        case PORT_KNOWN:
            logger.debug("Port " + name + ": PORT_KNOWN");
            try {
                id = (ReceivePortIdentifier) in.readObject();
            } catch (ClassNotFoundException e) {
                NameServerClient.socketFactory.close(in, out, s);
                throw new IOException("Unmarshall fails " + e);
            }
            break;
        default:
            NameServerClient.socketFactory.close(in, out, s);
            throw new StreamCorruptedException(
                    "Registry: lookup got illegal opcode " + result);
        }

        NameServerClient.socketFactory.close(in, out, s);

        return id;
    }

    public ReceivePortIdentifier[] query(IbisIdentifier ident) {
        /* not implemented yet */
        return new ReceivePortIdentifier[0];
    }

    public void bind(String name, ReceivePort prt) throws IOException {
        bind(name, prt.identifier());
    }

    //gosia
    public void bind(String name, ReceivePortIdentifier id) throws IOException {
        Socket s = null;
        ObjectOutputStream out;
        ObjectInputStream in;
        int result;

        logger.debug(this + ": bind \"" + name + "\" to " + id);

        s = NameServerClient.nsConnect(server, port, localAddress, false, 10);

        DummyOutputStream dos = new DummyOutputStream(s.getOutputStream());
        out = new ObjectOutputStream(new BufferedOutputStream(dos));

        // request a new Port.
        out.writeByte(PORT_NEW);
        out.writeUTF(name);
        out.writeObject(id);
        out.flush();

        DummyInputStream di = new DummyInputStream(s.getInputStream());
        in = new ObjectInputStream(new BufferedInputStream(di));
        result = in.readByte();

        NameServerClient.socketFactory.close(in, out, s);

        switch (result) {
        case PORT_REFUSED:
            throw new BindingException("Port name \"" + name
                    + "\" is not unique!");
        case PORT_ACCEPTED:
            break;
        default:
            throw new StreamCorruptedException(
                    "Registry: bind got illegal opcode " + result);
        }

        logger.debug(this + ": bound \"" + name + "\" to " + id);

    }

    public void rebind(String name, ReceivePortIdentifier id)
            throws IOException {
        Socket s = null;
        ObjectOutputStream out;
        ObjectInputStream in;
        int result;

        logger.debug(this + ": rebind \"" + name + "\" to " + id);

        s = NameServerClient.nsConnect(server, port, localAddress, false, 10);

        DummyOutputStream dos = new DummyOutputStream(s.getOutputStream());
        out = new ObjectOutputStream(new BufferedOutputStream(dos));

        // request a rebind
        out.writeByte(PORT_REBIND);
        out.writeUTF(name);
        out.writeObject(id);
        out.flush();

        DummyInputStream di = new DummyInputStream(s.getInputStream());
        in = new ObjectInputStream(new BufferedInputStream(di));
        result = in.readByte();

        NameServerClient.socketFactory.close(in, out, s);

        switch (result) {
        case PORT_ACCEPTED:
            break;
        default:
            throw new StreamCorruptedException(
                    "Registry: bind got illegal opcode " + result);
        }

        logger.debug(this + ": rebound \"" + name + "\" to " + id);
    }

    //end gosia

    public void unbind(String name) {

        try {
            Socket s = null;
            ObjectOutputStream out;
            ObjectInputStream in;

            s = NameServerClient.nsConnect(server, port, localAddress, false, 5);

            DummyOutputStream dos = new DummyOutputStream(s.getOutputStream());
            out = new ObjectOutputStream(new BufferedOutputStream(dos));

            // request a new Port.
            out.writeByte(PORT_FREE);
            out.writeUTF(name);
            out.flush();

            DummyInputStream di = new DummyInputStream(s.getInputStream());
            in = new ObjectInputStream(new BufferedInputStream(di));

            byte temp = in.readByte();
            NameServerClient.socketFactory.close(in, out, s);
            if (temp != 0) {
                throw new BindingException("Port name \"" + name
                        + "\" is not bound!");
            }
        } catch(Exception e) {
            logger.info("unbind of " + name + " failed");
        }
    }

    //gosia
    public String[] list(String pattern) throws IOException {
        Socket s = null;
        ObjectOutputStream out;
        ObjectInputStream in;
        String[] result;

        s = NameServerClient.nsConnect(server, port, localAddress, false, 10);

        DummyOutputStream dos = new DummyOutputStream(s.getOutputStream());
        out = new ObjectOutputStream(new BufferedOutputStream(dos));

        // request a list of names.
        out.writeByte(PORT_LIST);
        out.writeUTF(pattern);
        out.flush();

        DummyInputStream di = new DummyInputStream(s.getInputStream());
        in = new ObjectInputStream(new BufferedInputStream(di));

        byte num = in.readByte();

        result = new String[num];
        for (int i = 0; i < num; i++) {
            result[i] = in.readUTF();
        }

        NameServerClient.socketFactory.close(in, out, s);

        return result;

    }
    //end gosia
}

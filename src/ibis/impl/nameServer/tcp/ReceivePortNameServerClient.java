/* $Id$ */

package ibis.impl.nameServer.tcp;


import ibis.io.Conversion;
import ibis.ipl.BindingException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StreamCorruptedException;

import org.apache.log4j.Logger;

import smartsockets.virtual.*;

class ReceivePortNameServerClient implements Protocol {

    VirtualSocketAddress server;

    byte[] localAddressMarshalled;

    String ibisName;

    private static Logger logger = ibis.util.GetLogger
            .getLogger(ReceivePortNameServerClient.class.getName());

    ReceivePortNameServerClient(VirtualSocketAddress server, String name, 
            VirtualSocketAddress localAddress) 
        throws IOException {
        
        this.server = server;
        ibisName = name;
        localAddressMarshalled = Conversion.object2byte(localAddress);                
    }

    public ReceivePortIdentifier lookup(String name, long timeout)
            throws IOException {
        ReceivePortIdentifier[] r = lookup(new String[] {name}, timeout, false);
        if (r != null && r.length != 0) {
            return r[0];
        }
        return null;
    }

    private String namesList(String[] names) {
        StringBuffer s = new StringBuffer("");
        s.append(names.length);
        s.append(" [");
        
        for (int i = 0; i < names.length; i++) {
            s.append(names[i]);
            if (i < names.length-1) {
                s.append(", ");
            }
        }
      
        s.append("]");
        
        return s.toString();
    }

    ReceivePortIdentifier[] ids = null;
    boolean gotAnswer = false;
    
    public ReceivePortIdentifier[] lookup(String[] names, long timeout, 
            boolean allowPartialResults) throws IOException {
        
        DataOutputStream out = null;
        DataInputStream in = null;
        VirtualSocket s = null;

        try {
            
            if (logger.isDebugEnabled()) {
                logger.debug("ReceivePort lookup: " + namesList(names));
            }
                                          
            s = NameServerClient.nsConnect(server, false, 10);

            out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));

            ids = null;
            gotAnswer = false;

            // request a new Port.
            out.writeByte(PORT_LOOKUP);
            out.writeInt(localAddressMarshalled.length);
            out.write(localAddressMarshalled);            
            out.writeBoolean(allowPartialResults);       
            out.writeInt(names.length);
            for (int i = 0; i < names.length; i++) {
                out.writeUTF(names[i]);
            }
            out.writeLong(timeout);
            out.flush();

            in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
            byte result = in.readByte();

            switch (result) {
            case PORT_WAIT:
                
                if (logger.isDebugEnabled()) { 
                    logger.debug("ReceivePort lookup: wait while disconnected");
                }
                
                VirtualSocketFactory.close(s, out, in);
                in = null;
                out = null;
                s = null;
                synchronized(this) {
                    while (! gotAnswer) {
                        try {
                            wait();
                        } catch(Exception e) {
                            // ignored
                        }
                    }
//                    if (ids == null) {
//                    }
                    return ids;
                }

            case PORT_UNKNOWN:
            case PORT_KNOWN:
                return gotAnswer(result, in);

            default:
                throw new StreamCorruptedException(
                        "Registry: lookup got illegal opcode " + result);
            }
        } finally {
            VirtualSocketFactory.close(s, out, in);
        }
    }

    public synchronized ReceivePortIdentifier[] gotAnswer(byte opcode, DataInputStream in) throws IOException {
        int cnt = in.readInt();
        String[] names = new String[cnt];

        switch(opcode) {
        case PORT_UNKNOWN:
            for (int i = 0; i < cnt; i++) {
                names[i] = in.readUTF();
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Port request returns " + namesList(names) + 
                        ": PORT_UNKNOWN");
            }
            break;

        case PORT_KNOWN:
            ids = new ReceivePortIdentifier[names.length];
            
            if (logger.isDebugEnabled()) {
                logger.debug("Port returns " + 
                        namesList(names) + ": PORT_KNOWN");
            }
            
            for (int i = 0; i < names.length; i++) {
                try {
                    int len = in.readInt();
                    
                    if (len == 0) {
                        ids[i] = null;                            
                    } else { 
                        byte[] b = new byte[len];
                        in.readFully(b, 0, len);
                        ids[i] = 
                            (ReceivePortIdentifier) Conversion.byte2object(b);
                    } 
                } catch (ClassNotFoundException e) {
                    throw new IOException("Unmarshall fails " + e);
                }
            }
            break;
        default:
            throw new StreamCorruptedException(
                    "Registry: gotAnswer got illegal opcode " + opcode);
        }
        gotAnswer = true;
        notifyAll();
        return ids;
    }

    public ReceivePortIdentifier[] query(IbisIdentifier ident) {
        /* not implemented yet */
        return new ReceivePortIdentifier[0];
    }

    public void bind(String name, ReceivePort prt) throws IOException {
        bind(name, prt.identifier());
    }

    public void bind(String name, ReceivePortIdentifier prt) throws IOException {
        VirtualSocket s = null;
        DataOutputStream out = null;
        DataInputStream in = null;
        int result;

        if (logger.isDebugEnabled()) {
            logger.debug(this + ": bind \"" + name + "\" to " + prt);
        }

        try {
            s = NameServerClient.nsConnect(server, false, 10);

            out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));

            // request a new Port.
            out.writeByte(PORT_NEW);
            out.writeUTF(ibisName);
            out.writeUTF(name);
            byte[] buf = Conversion.object2byte(prt);
            out.writeInt(buf.length);
            out.write(buf);
            out.flush();

            in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
            result = in.readByte();
        } finally {
            VirtualSocketFactory.close(s, out, in);
        }

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

        if (logger.isDebugEnabled()) {
            logger.debug(this + ": bound \"" + name + "\" to " + prt);
        }
    }

    public void unbind(String name) {

        VirtualSocket s = null;
        DataOutputStream out = null;
        DataInputStream in = null;

        try {
            s = NameServerClient.nsConnect(server, false, 5);

            out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));

            // request a new Port.
            out.writeByte(PORT_FREE);
            out.writeUTF(name);
            out.flush();

            in = new DataInputStream(new BufferedInputStream(s.getInputStream()));

            byte temp = in.readByte();
            if (temp != 0) {
                throw new BindingException("Port name \"" + name
                        + "\" is not bound!");
            }
        } catch (Exception e) {
            logger.info("unbind of " + name + " failed");
        } finally {
            VirtualSocketFactory.close(s, out, in);
        }
    }
}

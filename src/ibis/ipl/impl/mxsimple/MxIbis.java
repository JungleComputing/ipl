/* $Id: MxIbis.java 7432 2008-01-31 12:34:40Z ceriel $ */

package ibis.ipl.impl.mxsimple;

import ibis.ipl.AlreadyConnectedException;
import ibis.ipl.CapabilitySet;
import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.MessageUpcall;
import ibis.ipl.PortMismatchException;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.ReceivePort;
import ibis.ipl.impl.SendPort;
import ibis.ipl.impl.SendPortIdentifier;
import ibis.ipl.impl.mx.channels.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Properties;

import org.apache.log4j.Logger;

public final class MxIbis extends ibis.ipl.impl.Ibis 
        implements MxProtocol, MxListener {

    static final Logger logger
            = Logger.getLogger("ibis.ipl.impl.mxsimple.MxIbis");


    private MxSocket ep;
    
	private ChannelManager channelManager;

    private MxAddress myAddress;

    private boolean quiting = false;

    private HashMap<ibis.ipl.IbisIdentifier, MxAddress> addresses
        = new HashMap<ibis.ipl.IbisIdentifier, MxAddress>();

    
    public MxIbis(RegistryEventHandler registryEventHandler, IbisCapabilities capabilities, PortType[] types, Properties userProperties) {
        super(registryEventHandler, capabilities, types, userProperties);

        this.properties.checkProperties("ibis.ipl.impl.mxsimple.",
                new String[] {"ibis.ipl.impl.mxsimple.mxsimple"}, null, true);

    }

    protected byte[] getData() throws IOException {
        ep = new MxSocket(this);
        myAddress = ep.getMyAddress();
        channelManager = ep.createChannelManager();
        channelManager.setOwner(this);

        if (logger.isInfoEnabled()) {
            logger.info("--> MxIbis: address = " + myAddress);
        }

        return myAddress.toBytes();
    }

    /*
    // NOTE: this is wrong ? Even though the ibis has left, the IbisIdentifier 
             may still be floating around in the system... We should just have
             some timeout on the cache entries instead...
    
    public void left(ibis.ipl.IbisIdentifier id) {
        super.left(id);
        synchronized(addresses) {
            addresses.remove(id);
        }
    }

    public void died(ibis.ipl.IbisIdentifier id) {
        super.died(id);
        synchronized(addresses) {
            addresses.remove(id);
        }
    }
    */

    WriteChannel connect(MxSendPort sp, ibis.ipl.impl.ReceivePortIdentifier rip,
            int timeout, boolean fillTimeout) throws IOException {
        
        IbisIdentifier id = (IbisIdentifier) rip.ibisIdentifier();
        String name = rip.name();
        MxAddress addr;

        synchronized(addresses) {
            addr = addresses.get(id);
            if (addr == null) {
                addr = MxAddress.fromBytes(id.getImplementationData());
                addresses.put(id, addr);
            }
        }

        long startTime = System.currentTimeMillis();

        if (logger.isDebugEnabled()) {
            logger.debug("--> Creating socket for connection to " + name
                    + " at " + addr);
        }

        PortType sendPortType = sp.getPortType();
        
        do {           
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeUTF(name);
            sp.getIdent().writeTo(out);
            sendPortType.writeTo(out);
            out.flush();
            Connection c = channelManager.connect(addr, baos.toByteArray());
            
            ByteArrayInputStream bais = new ByteArrayInputStream(c.getReplyMessage());
            DataInputStream in = new DataInputStream(bais);
            WriteChannel wc;
            if(c.getReply() == Connection.ACCEPT) {
            	wc = c.getWriteChannel();
            } else {
            	wc = null;
            }

            
            int result = in.readInt();

            try {
                switch(result) {
                case ReceivePort.ACCEPTED:
                    return c.getWriteChannel();
                case ReceivePort.ALREADY_CONNECTED:
                    throw new AlreadyConnectedException("Already connected", rip);
                case ReceivePort.TYPE_MISMATCH:
                    // Read receiveport type from input, to produce a
                    // better error message.
                    PortType rtp = new PortType(in);
                    CapabilitySet s1 = rtp.unmatchedCapabilities(sendPortType);
                    CapabilitySet s2 = sendPortType.unmatchedCapabilities(rtp);
                    String message = "";
                    if (s1.size() != 0) {
                        message = message
                            + "\nUnmatched receiveport capabilities: "
                            + s1.toString() + ".";
                    }
                    if (s2.size() != 0) {
                        message = message
                            + "\nUnmatched sendport capabilities: "
                            + s2.toString() + ".";
                    }
                    throw new PortMismatchException(
                            "Cannot connect ports of different port types."
                            + message, rip);
                case ReceivePort.DENIED:
                    throw new ConnectionRefusedException(
                            "Receiver denied connection", rip);
                case ReceivePort.NO_MANY_TO_X:
                    throw new ConnectionRefusedException(
                            "Receiver already has a connection and neither ManyToOne not ManyToMany "
                            + "is set", rip);
                case ReceivePort.NOT_PRESENT:
                case ReceivePort.DISABLED:
                    // and try again if we did not reach the timeout...
                    if (timeout > 0 && System.currentTimeMillis()
                            > startTime + timeout) {
                        throw new ConnectionTimedOutException(
                                "Could not connect", rip);
                    }
                    break;
                case -1:
                    throw new IOException("Encountered EOF in MxIbis.connect");
                default:
                    throw new IOException("Illegal opcode in MxIbis.connect");
                }
            } catch(SocketTimeoutException e) {
                throw new ConnectionTimedOutException("Could not connect", rip);
            } finally {
                if (result != ReceivePort.ACCEPTED) {
                    try {
                        out.close();
                    } catch(Throwable e) {
                        // ignored
                    }
                    try {
                        wc.close();
                    } catch(Throwable e) {
                        // ignored
                    }
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
        } while (true);
    }

    protected synchronized void quit() {
        quiting = true;
        cleanup();
    }

    /*
    private MxReceivePort nextRP = null;
    private SendPortIdentifier nextSPI = null;
    
    public synchronized void newConnection(ReadChannel rc) {
    	if (logger.isDebugEnabled()) {
            logger.debug("-newConnection()");
        }
		// TODO Auto-generated method stub
    	try {
			nextRP.connect(nextSPI, rc, new BufferedArrayInputStream(rc.getInputStream(), 4096));
			if (logger.isDebugEnabled()) {
                logger.debug("--> S connect done ");
            }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public synchronized ConnectionReply newConnectionRequest(ConnectionRequest request) {
		ConnectionReply reply;
		if (logger.isDebugEnabled()) {
            logger.debug("--> MxIbis got connection request from " + request.getSourceAddress());
        }
		
		ByteArrayInputStream bais = new ByteArrayInputStream(request.getDescriptor());
        DataInputStream in = new DataInputStream(bais);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        String name;
		SendPortIdentifier send;
		PortType sp;	
		try {
			name = in.readUTF();
			send = new SendPortIdentifier(in);
			sp = new PortType(in);	
		} catch (IOException e) {
			//TODO something
			e.printStackTrace();
			return request.reject();
		}

		
        // First, lookup receiveport.
        MxReceivePort rp = (MxReceivePort) findReceivePort(name);

        int result;
        if (rp == null) {
            result = ReceivePort.NOT_PRESENT;
        } else {
            result = rp.connectionAllowed(send, sp);
        }
        if(result == ReceivePort.ACCEPTED) {
        	reply = rp.accept(request);
        	if(reply.isAccepted()) {
        		nextRP = rp;
        		nextSPI = send;
        	} else {
        		result = ReceivePort.DENIED;
        	}
        } else {
        	reply = request.reject();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("--> S RP = " + name + ": "
                    + ReceivePort.getString(result));
        }

        try {
			out.writeInt(result);
	        if (result == ReceivePort.TYPE_MISMATCH) {
	            rp.getPortType().writeTo(out);
	        }
	        out.flush();
        } catch (IOException e) {
			e.printStackTrace();
			throw new Error("error creating connection reply");
		}

        reply.setMessage(baos.toByteArray());
        try {
			out.close();
			in.close();
		} catch (IOException e) {
			// ignore
		}
		if (logger.isDebugEnabled()) {
            logger.debug("newConnectionRequest() finished");
        }
		return reply;
	}

    */
	public void newConnection(ConnectionRequest request) {
		// TODO Auto-generated method stub
		if (logger.isDebugEnabled()) {
            logger.debug("--> MxIbis got connection request from " + request.getSourceAddress());
        }
		boolean accepted = false;
		ByteArrayInputStream bais = new ByteArrayInputStream(request.getDescriptor());
        DataInputStream in = new DataInputStream(bais);
        try {
        	in.close();
        } catch (IOException e) {
        	// ignore
        }
        
        String name;
		SendPortIdentifier send;
		PortType sp;	
		try {
			name = in.readUTF();
			send = new SendPortIdentifier(in);
			sp = new PortType(in);	
		} catch (IOException e) {
			//TODO something
			e.printStackTrace();
			request.reject();
			return;
		}

        // First, lookup receiveport.
        MxReceivePort rp = (MxReceivePort) findReceivePort(name);

        int result;
        if (rp == null) {
            result = ReceivePort.NOT_PRESENT;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            
            try {
    			out.writeInt(result);
    	        out.flush();
            } catch (IOException e) {
    			e.printStackTrace();
    			throw new Error("error creating connection reply");
    		}

            request.setReplyMessage(baos.toByteArray());
            request.reject();
            try {
    			out.close();
    			in.close();
    		} catch (IOException e) {
    			// ignore
    		}
    		if (logger.isDebugEnabled()) {
                logger.debug("newConnectionRequest() finished");
            }
        } else { 
        	rp.accept(request, send, sp);
        }
	}
    
    
    
    private void cleanup() {
        try {
            ep.close();
        } catch (Throwable e) {
            // Ignore
        }
    }

    protected SendPort doCreateSendPort(PortType tp, String nm,
            SendPortDisconnectUpcall cU, Properties props) throws IOException {
        return new MxSendPort(this, tp, nm, cU, props);
    }

    protected ReceivePort doCreateReceivePort(PortType tp, String nm,
            MessageUpcall u, ReceivePortConnectUpcall cU, Properties props)
            throws IOException {
        return new MxReceivePort(this, tp, nm, u, cU, props, ep.createChannelManager());
    }
	   
}

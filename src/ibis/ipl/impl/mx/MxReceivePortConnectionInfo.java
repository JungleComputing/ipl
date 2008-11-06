package ibis.ipl.impl.mx;

import ibis.io.BufferedArrayInputStream;
import ibis.io.Conversion;
import ibis.ipl.impl.ReceivePortConnectionInfo;
import ibis.ipl.impl.ReceivePortIdentifier;
import ibis.ipl.impl.SendPortIdentifier;
import ibis.ipl.impl.mx.channels.ReadChannel;
import ibis.util.ThreadPool;

import java.io.IOException;

import org.apache.log4j.Logger;

class MxReceivePortConnectionInfo extends
		ReceivePortConnectionInfo implements  
		MxProtocol {
	private static Logger logger = Logger.getLogger(MxReceivePortConnectionInfo.class);
	
	private final ReadChannel rc;
	protected short channelId;
	boolean portClosed = false;
	
	MxReceivePortConnectionInfo(SendPortIdentifier origin, ReadChannel rc,
			MxReceivePort rp, BufferedArrayInputStream in) 
			throws IOException {
		super(origin, rp, in);
		this.rc = rc;
		rc.setOwner(this);
	}
	
	boolean available() throws IOException {
		if(in == null) {
			newStream();
		}
		logger.debug("available(): " + in.available());
		return in.available() > 0;
	}	

	
	/**
	 * receives an Mx Message and checks whether it contains a new data message
	 * @return true when a message is delivered, false when no message is delivered
	 * @throws IOException
	 */
	boolean receive() throws IOException {
		logger.debug("receiving");
		if (in == null) {
            newStream();
        }
		/* OLD code
		 * TODO remove this
		if (((MxDataInputStream)dataIn).waitUntilAvailable(0) >= 0) { 
			// MX message available
		} else {
			throw new IOException("Error polling for message");
		}
		*/
		
		short opcode = in.readByte();
        switch (opcode) {
        	case NEW_MESSAGE:
        		message.setFinished(false);
                ((MxReceivePort)port).messageArrived(message);
        		return true;
        	case NEW_RECEIVER:
        		newStream();
        		return false;
        	case CLOSE_ALL_CONNECTIONS:
                if (logger.isDebugEnabled()) {
                    logger.debug(port.name 
                            + ": Got a CLOSE_ALL_CONNECTIONS from "
                            + origin);
                }
                close(null);
                return false;
            case CLOSE_ONE_CONNECTION:
                if (logger.isDebugEnabled()) {
                    logger.debug(port.name + ": Got a CLOSE_ONE_CONNECTION from "
                            + origin);
                }
                // read the receiveport identifier from which the sendport
                // disconnects.
                byte[] length = new byte[Conversion.INT_SIZE];
                in.readArray(length);
                byte[] bytes = new byte[Conversion.defaultConversion
                        .byte2int(length, 0)];
                in.readArray(bytes);
                ReceivePortIdentifier identifier
                        = new ReceivePortIdentifier(bytes);
                if (port.ident.equals(identifier)) {
                    // Sendport is disconnecting from me.
                    if (logger.isDebugEnabled()) {
                        logger.debug(port.name + ": disconnect from " + origin);
                    }
                    close(null);
                }
                return false;
    		default:
    			throw new IOException(port.name + ": Got illegal opcode "
                        + opcode + " from " + origin);
        }
	}

	
	/* (non-Javadoc)
	 * @see ibis.ipl.impl.ReceivePortConnectionInfo#upcallCalledFinish()
	 */
	@Override
	protected void upcallCalledFinish() {
		super.upcallCalledFinish();
		ThreadPool.createNew((MxReceivePort) port, "MxReceivePort Upcall Thread");
	}

	@Override
	public void close(Throwable e) {
        super.close(e);
        try {
            rc.close();
        } catch (Throwable x) {
            // ignore
        }
        logger.debug("closed!");
	}
		
}

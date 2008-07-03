/**
 * 
 */
package ibis.ipl.impl.mx;

import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

import ibis.ipl.MessageUpcall;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.impl.Ibis;
import ibis.ipl.impl.ReadMessage;
import ibis.ipl.impl.ReceivePort;
import ibis.ipl.impl.ReceivePortConnectionInfo;
import ibis.ipl.impl.SendPortIdentifier;
import ibis.util.ThreadPool;

class MxReceivePort extends ReceivePort implements Identifiable<MxReceivePort>, Runnable {
	private static Logger logger = Logger.getLogger(MxReceivePort.class);
	
		protected short portId = 0;
	protected IdManager<MxReceivePortConnectionInfo> channelManager;
	private IdManager<MxReceivePort> portManager = null;
	
	private boolean reader_busy = false;
    /**
     * Set when the current message has been delivered. Only used for
     * explicit receive.
     */
	
	/**
	 * @param ibis
	 * @param type
	 * @param name
	 * @param upcall
	 * @param connectUpcall
	 * @param properties
	 * @throws IOException
	 */
	MxReceivePort(Ibis ibis, PortType type, String name,
			MessageUpcall upcall, ReceivePortConnectUpcall connectUpcall,
			Properties properties) throws IOException {
		super(ibis, type, name, upcall, connectUpcall, properties);
		channelManager = new IdManager<MxReceivePortConnectionInfo>();
		if(upcall != null) {
			ThreadPool.createNew(this, "MxReceivePort Upcall Thread");
		}
	}
		
	/* ********************************************
	 * Methods that can be overridden optionally:
	 *********************************************/

	@Override
	public ReadMessage getMessage(long timeout) throws IOException {

		if (logger.isDebugEnabled()) {
			logger.debug("GetMessage()");			
		}
		
		if(timeout == 0) { // no timeouts
	       	synchronized(this) {
            	while(reader_busy) {
            		// another reader is busy. Wait for it
            		try {
            			if (logger.isDebugEnabled()) {
            				logger.debug("Waiting for another reader to finish");
            			}
						wait();
					} catch (InterruptedException e) {
						// ignore
					}
					if (logger.isDebugEnabled()) {
        				logger.debug("other reader is finished");
        			}
            	}
            	reader_busy = true;
            }
        	// wait until a the current message is finished
        	while (message != null) {
        		synchronized(this) {
        			if(closed) {
            			if (logger.isDebugEnabled()) {
            				logger.debug("Connection closed");
            			}
                		reader_busy = false;
                		notifyAll();
    	        		throw new IOException("receive() on closed port");
        			}
        		}
                try {
                	if (logger.isDebugEnabled()) {
        				logger.debug("Waiting for current message to finish");
        			}
                	wait();
                } catch (InterruptedException e) {
                    /* ignore */
                }
            }
  	
        	short channelId = getReadyConnection(0);
    		if (logger.isDebugEnabled()) {
				logger.debug("channel " + channelId + " is ready");
			}
        	if (channelId == 0) {
        		if (logger.isDebugEnabled()) {
    				logger.debug("Warning: channel ID is 0, returning null");
    			}
        		//no channel is ready
        		//TODO huh, how can that happen? Maybe the Port closed?
            	synchronized(this) {
            		reader_busy = false;
            		notifyAll();
            	}
        		//FIXME cannot deliver a null message. come up with a timeout
        		return null;
        	}
        	MxReceivePortConnectionInfo rpi =  channelManager.find(channelId);
        	if (rpi == null) {
        		/*if (logger.isDebugEnabled()) {
    				logger.debug("Channel not found: " + channelId);
    			}*/
        		//TODO handle this nice and gently and just throw the message away?
        		// Note: that needs a reception request here...
        		synchronized(this) {
            		reader_busy = false;
            		notifyAll();
            	}
        		throw new IOException("Non-existent connection is ready");
        	}
        	/*if (logger.isDebugEnabled()) {
				logger.debug("Receiving message...");
			}*/
        	rpi.receive();
        	//FIXME when in message upcall thread
        	// rpi calls this.messageArrived() when the message is arrived, so message will contain the message.
        	synchronized(this) {
        		reader_busy = false;
        		notifyAll();
        	}
        	/*if (logger.isDebugEnabled()) {
				logger.debug("Returning message that is delivered by the rpi");
				if(message == null) {
					logger.debug("rpi delivered 'null'");
				}
			}*/
            return message;
            
            
            
		} else { // timeouts enabled
			long deadline = System.currentTimeMillis() + timeout;
			long remainingTime;
			// Allow only one reader in.
	        synchronized(this) {
	        	if(reader_busy) {
	        		// another reader is busy. We don't wait for it when polling
	        		return null;
	        	}
	        	reader_busy = true;
	        	
		    	// wait until a the current message is finished
		    	while (message != null && !closed) {
		            try {
		            	remainingTime = System.currentTimeMillis() - deadline;
		            	if(remainingTime <= 0) {
		            		// timeout expired
	                		reader_busy = false;
	                		notifyAll();
		            		return null;
		            	}
		                wait(remainingTime);
		            } catch (InterruptedException e) {
		                /* ignore */
		            }
		        }
		    	if(closed) {
	        		reader_busy = false;
	        		notifyAll();
		    		throw new IOException("receive() on closed port");
		    	}
			}
        	remainingTime = System.currentTimeMillis() - deadline;
        	if(remainingTime <= 0) {
            	synchronized(this) {
            		reader_busy = false;
            		notifyAll();
            	}
        		return null;
        	}
        	short channelId = getReadyConnection(remainingTime);
        	if (channelId == 0) {
        		//no channel is ready
            	synchronized(this) {
            		reader_busy = false;
            		notifyAll();
            	}
        		return null;
        	}
        	
        	MxReceivePortConnectionInfo rpi =  channelManager.find(channelId);
        	if (rpi == null) {
        		//TODO handle this nice and gently and just throw the message away?
        		// Note: that needs a reception request here...
            	synchronized(this) {
            		reader_busy = false;
            		notifyAll();
            	}
        		throw new IOException("Non-existent connection is ready");
        	}
        	rpi.receive();
        	//FIXME when in message upcall thread
        	// rpi calls this.messageArrived() when the message is arrived, so message will contain the message.
        	synchronized(this) {
        		reader_busy = false;
        		notifyAll();
        	}
            return message;
        }
	}
	
	@Override
	public void messageArrived(ReadMessage msg) {
		if (numbered) {
			try {
				msg.setSequenceNumber(msg.readLong());
			} catch (IOException e) {
				// TODO ignore now, needs a solution
			}
        }
		/*if (logger.isDebugEnabled()) {
			logger.debug("A Message has arrived...");
		}*/
		super.messageArrived(msg);
		/*if (logger.isDebugEnabled()) {
			logger.debug("A Message has arrived!");
		}*/
	}
	
	@Override
	/**
	 * This version delivers the data itself.
	 */
	protected ReadMessage doPoll() throws IOException {
        if (upcall != null) {
            return null;
        }
        synchronized(this) {
        	if(reader_busy) {
        		// another reader is busy. We don't wait for it when polling
        		return null;
        	}
        	reader_busy = true;
        }
        
    	ReadMessage msg = super.doPoll();
    	if (msg != null) { //super.doPoll() found a new message
        	synchronized(this) {
        		reader_busy = false;
        		notifyAll();
        	}
    		return msg;
    	}
    	
    	if(message != null) {
    		// current message is already delivered, but not finished yet, so end the poll
        	synchronized(this) {
        		reader_busy = false;
        		notifyAll();
        	}
    		return null;
    	}
    	
    	// probe for a message for this port:
    	short channelId = getReadyConnection();
    	if (channelId == 0) {
    		//no channel is ready
        	synchronized(this) {
        		reader_busy = false;
        		notifyAll();
        	}
    		return null;
    	}
    	
    	MxReceivePortConnectionInfo rpi =  channelManager.find(channelId);
    	if (rpi == null) {
    		//TODO handle this nice and gently and just throw the message away?
    		// Note: that needs a reception request here...
    		throw new IOException("Non-existent connection is ready");
    	}
    	if(rpi.poll()) {
    		// receive a message, when it is available
    		rpi.receive();
    	}
    	// rpi calls this.messageArrived() when the message is arrived, so message will contain the message.
    	synchronized(this) {
    		reader_busy = false;
    		notifyAll();
    	}
        return message;
	}
	
	@Override
	public synchronized void closePort(long timeout) {
		if (logger.isDebugEnabled()) {
			logger.debug("closing port...");
		}
		// TODO when doing a hard close, come up with something like this
		ReceivePortConnectionInfo[] conns = connections();
		for(ReceivePortConnectionInfo rpci: conns) {
			((MxReceivePortConnectionInfo)rpci).receivePortcloses();
		}
		
		//super.closePort(timeout);
		// FIXME broken without timeout
		super.closePort(10000);
		portManager.remove(portId);
	}



    // non-blocking
    private short getReadyConnection() throws IOException {
    	MxReceivePortConnectionInfo conn2;
    	ReceivePortConnectionInfo[] connections = connections();
    	for(ReceivePortConnectionInfo conn: connections) {
    		conn2 = (MxReceivePortConnectionInfo)conn;
    		if(conn2.poll()) {
    			return conn2.channelId;
    		}
    	}
    	return 0;
    }

    // poll for a connection that has some data available
    private short getReadyConnection(long timeout) throws IOException {
    	long deadline = 0;
    	short result = 0;
    	if(timeout > 0) {
    		deadline = System.currentTimeMillis() + timeout;
    	}
    	
		while (result == 0 && (deadline == 0 || System.currentTimeMillis() < deadline)) {
			/*
			if (logger.isDebugEnabled()) {
				logger.debug("getReadyConnection(long): probing channels");
			}
			*/
			result = getReadyConnection();
			if (result == 0) {
				//	FIXME take deadline in account when waiting for message
				// FIXME perfromance degradation because of the local loop?
				result = Matching.getChannel(
						JavaMx.waitForMessage(((MxIbis)ibis).factory.endpointId, 100, Matching.construct(Matching.PROTOCOL_DATA, portId, (short)0), ~Matching.CHANNEL_MASK)
					);
			}
		}
    	return result;
    }

    
    
	/* IdManager methods */
	
	/* (non-Javadoc)
	 * @see ibis.ipl.impl.ReceivePort#addInfo(ibis.ipl.impl.SendPortIdentifier, ibis.ipl.impl.ReceivePortConnectionInfo)
	 */
	@Override
	public synchronized void addInfo(SendPortIdentifier id,
			ReceivePortConnectionInfo info) {
		super.addInfo(id, info);
		try {
			channelManager.insert((MxReceivePortConnectionInfo)info);
		} catch (Exception e) {
			e.printStackTrace();
			//Wow, we have a lot of connection here!
			//TODO something nice?
			System.exit(1);
		}
	}

	/* (non-Javadoc)
	 * @see ibis.ipl.impl.ReceivePort#removeInfo(ibis.ipl.impl.SendPortIdentifier)
	 */
	@Override
	public synchronized MxReceivePortConnectionInfo removeInfo(
			SendPortIdentifier id) {
		MxReceivePortConnectionInfo info = (MxReceivePortConnectionInfo)(super.removeInfo(id));
		if (info != null) {
			channelManager.remove(info.getIdentifier());
		}
		return info;
	}

	public IdManager<MxReceivePort> getIdManager() {
		return portManager;
	}

	public short getIdentifier() {
		return portId;
	}

	public void setIdManager(IdManager<MxReceivePort> manager) {
		portManager = manager;
	}

	public void setIdentifier(short id) {
		portId = id;
	}

	public void run() {
		while(true) {
			synchronized(this) {
				if(closed) {
					return;
				}
				while (!allowUpcalls) {
					try {
						// we need to wait with waiting for upcalls until the portId is set
						// MxIbis will do this, but we have to wait for it, and waiting for allowUpcalls is safe
						wait();
					} catch (InterruptedException e) {
						// ignore
					}
				}
			}
			if (logger.isDebugEnabled()) {
				logger.debug("getting message for upcall...");
			}
			try {	
				getMessageForUpcall();
			} catch (IOException e) {
				// 	TODO most likely an IOException telling us that the connection is closed.
				// 	we just ignore it for now 	
				// FIXME catch connectionclosedException
				// or even portclosedException or something
			}
		}
	}
	
	public void getMessageForUpcall() throws IOException {
    	// wait until a the current message is finished
    	while (message != null) { // probably never when upcalls are used	
            try {
            	if (logger.isDebugEnabled()) {
    				logger.debug("Waiting for current message to finish");
    			}
            	wait();
            } catch (InterruptedException e) {
                /* ignore */
            }
        }
    	synchronized(this) {
			if(closed) {
    			if (logger.isDebugEnabled()) {
    				logger.debug("Connection closed");
    			}
        		return;
			}
		}
    	short channelId = getReadyConnection(0);
		if (logger.isDebugEnabled()) {
			logger.debug("channel " + channelId + " is ready");
		}
    	if (channelId == 0) {
    		if (logger.isDebugEnabled()) {
				logger.debug("Warning: channel ID is 0, no message delivery");
			}
    		//no channel is ready
    		//TODO huh, how can this happen? Maybe the Port closed?
    		return;
    	}
    	MxReceivePortConnectionInfo rpi =  channelManager.find(channelId);
    	if (rpi == null) {
    		// channel not found
    		//TODO handle this nice and gently and just throw the message away?
    		// Note: that needs a reception request here...
    		throw new IOException("Non-existent connection is ready");
    	}
    	rpi.receive();
        return;
	}
	
}

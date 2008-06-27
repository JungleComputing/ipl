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
import ibis.util.ThreadPool;

class MxReceivePort extends ReceivePort implements Identifiable<MxReceivePort>, Runnable {
	private static Logger logger = Logger.getLogger(MxReceivePort.class);
	
	
	//TODO debug value
	private long getMessageTime = 0;
	private long getConnTime = 0;
	private long getConnTime2 = 0;
	private long getMessageSleepTime = 0;
	private long channelFindTime = 0;
	protected short portId = 0;
	protected IdManager<MxReceivePortConnectionInfo> channelManager;
	protected IdManager<MxReceivePort> portManager = null;
	
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
			//getMessageTime -= System.currentTimeMillis();			
		}
		
		if(timeout == 0) { // no timeouts
	       	synchronized(this) {
            	while(reader_busy) {
            		// another reader is busy. Wait for it
            		/*if (logger.isDebugEnabled()) {
            			getMessageSleepTime -= System.currentTimeMillis();			
            		}*/
            		try {
            			if (logger.isDebugEnabled()) {
            				logger.debug("Waiting for another reader to finish");
            			}
						wait();
					} catch (InterruptedException e) {
						// ignore
					}
            		/*if (logger.isDebugEnabled()) {
            			getMessageSleepTime += System.currentTimeMillis();			
            		}*/
					if (logger.isDebugEnabled()) {
        				logger.debug("other reader is finished");
        			}
            	}
            	reader_busy = true;
            }
        	// wait until a the current message is finished
        	while (message != null && !closed) {
        		/*if (logger.isDebugEnabled()) {
        			getMessageSleepTime -= System.currentTimeMillis();			
        		}*/
                try {
                	if (logger.isDebugEnabled()) {
        				logger.debug("Waiting for current message to finish");
        			}
                	wait();
                } catch (InterruptedException e) {
                    /* ignore */
                }
        		if (logger.isDebugEnabled()) {
        			getMessageSleepTime += System.currentTimeMillis();			
        		}
            }
        	if(closed) {
        		synchronized(this) {
        			if (logger.isDebugEnabled()) {
        				logger.debug("Connection closed");
        			}
            		reader_busy = false;
            		notifyAll();
            	}
        		if (logger.isDebugEnabled()) {
        			getMessageTime += System.currentTimeMillis();			
        		}
        		throw new IOException("receive() on closed port");
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
        		if (logger.isDebugEnabled()) {
        			getMessageTime += System.currentTimeMillis();			
        		}
        		return null;
        	}
    		if (logger.isDebugEnabled()) {
    			channelFindTime -= System.currentTimeMillis();			
    		}
        	MxReceivePortConnectionInfo rpi =  channelManager.find(channelId);
        	if (logger.isDebugEnabled()) {
    			channelFindTime += System.currentTimeMillis();			
    		}
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
        		if (logger.isDebugEnabled()) {
        			getMessageTime += System.currentTimeMillis();			
        		}
        		throw new IOException("Non-existent connection is ready");
        	}
        	/*if (logger.isDebugEnabled()) {
				logger.debug("Receiving message...");
			}*/
        	rpi.receive();
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
    		if (logger.isDebugEnabled()) {
    			getMessageTime += System.currentTimeMillis();			
    		}
            return message;
            
            
            
		} else { // timeouts enabled
			long deadline = System.currentTimeMillis() + timeout;
			long remainingTime;
			// Allow only one reader in.
	        synchronized(this) {
	        	if(reader_busy) {
	        		// another reader is busy. We don't wait for it when polling
	        		if (logger.isDebugEnabled()) {
	        			getMessageTime += System.currentTimeMillis();			
	        		}
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
	                		if (logger.isDebugEnabled()) {
	                			getMessageTime += System.currentTimeMillis();			
	                		}
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
	        		if (logger.isDebugEnabled()) {
	        			getMessageTime += System.currentTimeMillis();			
	        		}
		    		throw new IOException("receive() on closed port");
		    	}
			}
        	remainingTime = System.currentTimeMillis() - deadline;
        	if(remainingTime <= 0) {
            	synchronized(this) {
            		reader_busy = false;
            		notifyAll();
            	}
        		if (logger.isDebugEnabled()) {
        			getMessageTime += System.currentTimeMillis();			
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
        		if (logger.isDebugEnabled()) {
        			getMessageTime += System.currentTimeMillis();			
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
        		if (logger.isDebugEnabled()) {
        			getMessageTime += System.currentTimeMillis();			
        		}
        		throw new IOException("Non-existent connection is ready");
        	}
        	rpi.receive();
        	// rpi calls this.messageArrived() when the message is arrived, so message will contain the message.
        	synchronized(this) {
        		reader_busy = false;
        		notifyAll();
        	}
    		if (logger.isDebugEnabled()) {
    			getMessageTime += System.currentTimeMillis();			
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
		if (logger.isDebugEnabled()) {
			logger.debug("A Message has arrived...");
		}
		super.messageArrived(msg);
		if (logger.isDebugEnabled()) {
			logger.debug("A Message has arrived!");
		}
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
			//logger.debug("closing port...");
			logger.debug("time spent in getMessage(): " + getMessageTime + " ms");
			logger.debug("time spending asleep in getMessage(): " + getMessageSleepTime + " ms");
			logger.debug("time spent in getReadyConnection(): " + getConnTime2 + " ms");
			logger.debug("Total time spent in getReadyConnection(long): " + getConnTime + " ms");
			logger.debug("Channel lookup time: " + channelFindTime + " ms");
		}
		//TODO hack: timeout is broken otherwise
		/*if(timeout == 0) {
			timeout = 1;
		}*/
		ReceivePortConnectionInfo[] conns = connections();
		for(ReceivePortConnectionInfo rpci: conns) {
			((MxReceivePortConnectionInfo)rpci).receiverClose();
		}
		super.closePort(timeout);
		portManager.remove(portId);
	}



    // non-blocking
    private short getReadyConnection() throws IOException {
    	ReceivePortConnectionInfo[] connections = connections();
    	for(ReceivePortConnectionInfo conn: connections) {
    		MxReceivePortConnectionInfo conn2 = (MxReceivePortConnectionInfo)conn;
    		if(conn2.poll()) {
    			return conn2.channelId;
    		}
    	}
    	return 0;
    }

    // poll for a connection that has some data available
    private short getReadyConnection(long timeout) throws IOException {
    	
    	long temp = 0; //TODO remove when no debug
		if (logger.isDebugEnabled()) {
			temp = System.currentTimeMillis();			
		}
    	short result = getReadyConnection();
    	if (logger.isDebugEnabled()) {
    		getConnTime2 += System.currentTimeMillis() - temp;			
		}
		if (result == 0) {
			/*
			if (logger.isDebugEnabled()) {
				logger.debug("getReadyConnection(long): probing channels");
			}
			*/
			result = Matching.getChannel(
				JavaMx.waitForMessage(((MxIbis)ibis).factory.endpointId, timeout, Matching.construct(Matching.PROTOCOL_DATA, portId, (short)0), ~Matching.CHANNEL_MASK)
				);
		}
		if (logger.isDebugEnabled()) {
			getConnTime += System.currentTimeMillis() - temp;			
		}
    	return result;
    }

	/* IdManager methods */
	
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
		while(!closed) {
			synchronized(this) {
				while(!allowUpcalls) {
					try {
						wait();
					} catch (InterruptedException e) {
						//	ignore
					}
				}
			}
			try {
				if (logger.isDebugEnabled()) {
					logger.debug("getting message for upcall...");
				}
				getMessage(0);
			} catch (IOException e) {
			// 	TODO most likely an IOException telling us that the connection is closed.
			// we just ignore it for now 	
			}
		}
	}
}

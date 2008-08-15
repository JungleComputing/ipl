package ibis.ipl.impl.mx;

import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

import ibis.ipl.MessageUpcall;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.ReceiveTimedOutException;
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
	private boolean threadActive = false;
	private boolean forced_close = false;
	
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

	@Override
	public synchronized void closePort(long timeout) {
		logger.debug("closing port...");
		// TODO when doing a hard close, come up with something like this
		/*ReceivePortConnectionInfo[] conns = connections();
		for(ReceivePortConnectionInfo rpci: conns) {
			((MxReceivePortConnectionInfo)rpci).receivePortcloses();
			logger.debug("notifying rpci...");
		}
		logger.debug("rpci's notified");
		*/

		//FIXME receive close() message with a thread
		ThreadPool.createNew(this, "MxReceivePort closePort Thread");
		notifyAll();
		super.closePort(timeout);
		forced_close = true;
		portManager.remove(portId);
		logger.debug("port closed!");
	}

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
			//Wow, we have a lot of connections here!
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
	
	public void run() {
		synchronized(this) {
			if (threadActive) {
				return;
			}
			threadActive = true;
		}
		while(true) {
			synchronized(this) {
				if(closed) {
					//FIXME wait for channels to close
					portCloser();
					threadActive = false;
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
			try {	
				getMessageForUpcall();
			} catch (IOException e) {
				// 	TODO most likely an IOException telling us that the connection is closed.
				// 	we just ignore it for now 	
			}
		}
	}
	
	private void portCloser() {
		MxReceivePortConnectionInfo rpi;
		logger.debug("PortCloser()");
		while (true) {
			synchronized(this) {
				if (connections().length <= 0) {
					return;
				}
				if(forced_close) {
					return;
				}
				reader_busy = true;
			}
			try {
				do {
					// get the next message
					short channelId = getReadyConnection(System.currentTimeMillis() + 100);
					if (channelId == 0) {
						//no channel is ready
						synchronized(this) {
							reader_busy = false;
							notifyAll();
						}
						break;
					}
					rpi =  channelManager.find(channelId);
					if (rpi == null) {
						// channel not found
						// FIXME handle this nice and just throw the message away?
						// Note: that needs a reception request here...
						synchronized(this) {
							reader_busy = false;
							notifyAll();
						}
					}
				} while (!rpi.receive());
			} catch (IOException e) {
				// we ignore them here
			}
		}
	}
	
	private void getMessageForUpcall() throws IOException {
		logger.debug("getMessageForUpcall()");
    	// wait until a the current message is finished
		synchronized(this) {
	    	while (message != null) {	
	            try {
	    			//logger.debug("Waiting for current message to finish");
	            	wait();
	            } catch (InterruptedException e) {
	                /* ignore */
	            }
	        }   	
	    	while(reader_busy) {
	    		// another reader is busy. Wait for it
	    		try {
	   				wait();
				} catch (InterruptedException e) {
					// ignore
				}
	    	}    	
			if(closed) {
    			//logger.debug("Connection closed");
        		return;
			}
			reader_busy = true;
		}
		MxReceivePortConnectionInfo rpi;
		do {
			logger.debug("getMessageForUpcall loop");
	    	// get the next message
	    	short channelId = getReadyConnection(System.currentTimeMillis() + 1000); //timeout, so we can check whether the state of the port changed (to closed)
	    	if (channelId == 0) {
	    		//no channel is ready
	    		synchronized(this) {
	    			reader_busy = false;
	    			notifyAll();
	    		}
	    		return;
	    	}
	    	rpi =  channelManager.find(channelId);
	    	if (rpi == null) {
	    		// channel not found
	    		//TODO handle this nice and gently and just throw the message away?
	    		// Note: that needs a reception request here...
	    		synchronized(this) {
	    			reader_busy = false;
	    			notifyAll();
	    		}
	    		throw new IOException("Non-existent connection is ready");
	    	}
		} while (!rpi.receive());
    	synchronized(this) {
			reader_busy = false;
			notifyAll();
		}
        return;
	}

	/* (non-Javadoc)
	 * @see ibis.ipl.impl.ReceivePort#getMessage(long)
	 */
	@Override
	public ReadMessage getMessage(long timeout) throws IOException {
		long deadline = 0;
		if(deadline > 0) {
			deadline = System.currentTimeMillis() + timeout;
		}
		// get reader access
        synchronized(this) {
	    	while (message != null) {	
	            try {
	    			//logger.debug("Waiting for current message to finish");
	            	if(deadline == 0) {
	            		wait();	
	            	} else {
		            	long time = deadline - System.currentTimeMillis();
		    			if(time <= 0) {
		    				// timeout
		    				return null;
		    			}
		   				wait(time);
	            	}
	            } catch (InterruptedException e) {
	                /* ignore */
	            }
	        }        	
        	while(reader_busy) {
	    		// another reader is busy. Wait for it
	    		try {
	            	if(deadline == 0) {
	            		wait();	
	            	} else {
		            	long time = deadline - System.currentTimeMillis();
		    			if(time <= 0) {
		    				// timeout
		    				return null;
		    			}
		   				wait(time);
	            	}
				} catch (InterruptedException e) {
					// ignore
				}
	    	}
        	if(closed) {
           		notifyAll();
	       		throw new IOException("receive() on closed port");
        	}
        	reader_busy = true;
        }

        MxReceivePortConnectionInfo rpi;
        do { 
			// wait for the first channel that has a message available
			short channelId = getReadyConnection(deadline);
	    	if (channelId == 0) {
	    		//no channel is ready
	        	synchronized(this) {
	        		reader_busy = false;
	        		notifyAll();
	        	}
	        	throw new ReceiveTimedOutException("Timed out while waiting for a connection to get ready");
	    	}
	    	rpi =  channelManager.find(channelId);
	    	if (rpi == null) {
				//logger.debug("Channel not found: " + channelId);
	    		synchronized(this) {
	        		reader_busy = false;
	        		notifyAll();
	        	}
	    		throw new IOException("Non-existent connection is ready");
	    	}
	    	// calls messageArrived
        } while (!rpi.receive());
		synchronized(this) {
    		reader_busy = false;
    		notifyAll();
    	}
		return message;
	}
	
	/* (non-Javadoc)
	 * @see ibis.ipl.impl.ReceivePort#doPoll()
	 */
	@Override
	protected ReadMessage doPoll() throws IOException {
		if (upcall != null) {
            return null;
        }
     
        synchronized(this) {
        	if(message != null) {
        		return null;
        	}
        	if(closed) {
           		notifyAll();
	       		throw new IOException("receive() on closed port");
        	}
        	if(reader_busy) {
        		// another reader is busy. We don't wait for it when polling
        		return null;
        	}
        	reader_busy = true;
        }
     // poll whether a channel has a message
        MxReceivePortConnectionInfo rpi;
        do {
			short channelId = pollForReadyConnection();
	    	if (channelId == 0) {
	    		//no channel is ready
	        	synchronized(this) {
	        		reader_busy = false;
	        		notifyAll();
	        	}
	        	return null;
	    	}
	    	rpi =  channelManager.find(channelId);
	    	if (rpi == null) {
				//logger.debug("Channel not found: " + channelId);
	    		synchronized(this) {
	        		reader_busy = false;
	        		notifyAll();
	        	}
	    		throw new IOException("Non-existent connection is ready");
	    	}
	    	// calls messageArrived
        } while (!rpi.receive());
		synchronized(this) {
    		reader_busy = false;
    		notifyAll();
    	}
		return message;
	}



	/* (non-Javadoc)
	 * @see ibis.ipl.impl.ReceivePort#messageArrived(ibis.ipl.impl.ReadMessage)
	 */
	@Override
	public void messageArrived(ReadMessage msg) {
		if (numbered) {
			try {
				msg.setSequenceNumber(msg.readLong());
			} catch (IOException e) {
				// TODO ignore now, needs a solution
			}
        }
		
		// Wait until the previous message was finished.
        synchronized(this) {
            while (message != null) {
                try {
                    wait();
                } catch(InterruptedException e) {
                    // ignored.
                }
            }
            message = msg;
            notifyAll();
        }
        if (upcall != null) {
            doUpcall(message);
        }	
	}
	
    // non-blocking
    private short pollForReadyConnection() throws IOException {
    	MxReceivePortConnectionInfo conn2;
    	ReceivePortConnectionInfo[] connections = connections();
    	
    	
    	for(ReceivePortConnectionInfo conn: connections) {
    		conn2 = (MxReceivePortConnectionInfo)conn;
    		if(conn2.available()) {
    			logger.debug("pollForReadyConnection() found channel");
    			return conn2.channelId;
    		}
    	}
    	return Matching.getChannel(
				JavaMx.pollForMessage(((MxIbis)ibis).factory.endpointId, Matching.construct(Matching.PROTOCOL_DATA, portId, (short)0), ~Matching.CHANNEL_MASK)
			);
    }
	
    // poll for a connection that has some data available
    private short getReadyConnection(long deadline) throws IOException {
    	/* FIXME using a deadline in the function definition is asking for bugs created 
    	 * by calling this function with a timeout instead. So change deadline to timeout
    	 */
    	short result = 0;
    	
    	if(deadline == 0) {
			while (result == 0) {
				synchronized(this) {
					if(closed) {
						return 0;
					}
				}
				//keep polling for the local channels every x millis
				// TODO determine a nice poll interval
				result = pollForReadyConnection();
				if(result != 0) {
					logger.debug("getReadyConnection(deadline) found channel");
					return result;
				}
				result = Matching.getChannel(
    					JavaMx.waitForMessage(((MxIbis)ibis).factory.endpointId, 50, Matching.construct(Matching.PROTOCOL_DATA, portId, (short)0), ~Matching.CHANNEL_MASK)
					);
				// FIXME build separate thread for Local Channels instead of this loop.
				// Note: The deadline in waitForMessage will still be useful for detecting closing ports
			}
			if(logger.isDebugEnabled() && result != 0) {
				logger.debug("getReadyConnection(deadline) found channel");
			}
	    	return result;	
    	} else {
    		// we have a deadline
	    	// poll whether a channel is ready
	    	result = pollForReadyConnection();
	    	if (result == 0) {
	    		// no one is ready, wait until someone is (or the timeout expired)    			
	    		result = Matching.getChannel(
	    				JavaMx.waitForMessage(((MxIbis)ibis).factory.endpointId, deadline - System.currentTimeMillis(), Matching.construct(Matching.PROTOCOL_DATA, portId, (short)0), ~Matching.CHANNEL_MASK)
					);
	    		// we do not bother to check for the local channels again
	    	}
	    	if(logger.isDebugEnabled()) {
	    		if(result != 0) {
	    			logger.debug("getReadyConnection(deadline) found channel");
	    		} else {
	    			logger.debug("getReadyConnection(deadline) did not find channel");
	    		}
	    	}
	    	return result;
		}	
    }
	
	/* "Identifiable" functions */
    
    
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
}


package ibis.ipl.impl.mx;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

import ibis.io.BufferedArrayInputStream;
import ibis.ipl.MessageUpcall;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.ReceiveTimedOutException;
import ibis.ipl.impl.Ibis;
import ibis.ipl.impl.ReadMessage;
import ibis.ipl.impl.ReceivePort;
import ibis.ipl.impl.ReceivePortConnectionInfo;
import ibis.ipl.impl.SendPortIdentifier;
import ibis.ipl.impl.mx.channels.ChannelManager;
import ibis.ipl.impl.mx.channels.ConnectionRequest;
import ibis.ipl.impl.mx.channels.ReadChannel;
import ibis.ipl.impl.mx.channels.SimpleOutputStream;
import ibis.util.ThreadPool;

class MxReceivePort extends ReceivePort implements Runnable {
	private static Logger logger = Logger.getLogger(MxReceivePort.class);
	
	protected short portId = 0;
	
	private boolean reader_busy = false;
	private boolean threadActive = false;
	private boolean forced_close = false;
	
	private ChannelManager channelManager;
	
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
			Properties properties, ChannelManager channelManager) throws IOException {
		super(ibis, type, name, upcall, connectUpcall, properties);
		this.channelManager = channelManager;
        this.channelManager.setOwner(this);
		if(upcall != null) {
			ThreadPool.createNew(this, "MxReceivePort Upcall Thread");
		}
	}

	@Override
	public synchronized void closePort(long timeout) {
		//logger.debug("closing port...");
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
		channelManager.close();
		//logger.debug("port closed!");
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
		MxReceivePortConnectionInfo rpi = null;
		//logger.debug("PortCloser()");
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
					rpi = getReadyConnection(System.currentTimeMillis() + 100);
					if (rpi == null) {
						//no channel is ready
						synchronized(this) {
							reader_busy = false;
							notifyAll();
						}
						break;
					}
				} while (!rpi.receive());
			} catch (IOException e) {
				// we ignore them here
			}
		}
	}
	
	private void getMessageForUpcall() throws IOException {
		//logger.debug("getMessageForUpcall()");
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
			//logger.debug("getMessageForUpcall loop");
	    	// get the next message
			rpi = getReadyConnection(System.currentTimeMillis() + 1000); //timeout, so we can check whether the state of the port changed (to closed)
	    	if (rpi == null) {
	    		//no channel is ready
	    		synchronized(this) {
	    			reader_busy = false;
	    			notifyAll();
	    		}
	    		return;
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
		if(timeout > 0) {
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
        	rpi = getReadyConnection(deadline);
	    	if (rpi == null) {
	    		//no channel is ready
	        	synchronized(this) {
	        		reader_busy = false;
	        		notifyAll();
	        	}
	        	throw new ReceiveTimedOutException("Timed out while waiting for a connection to get ready");
	    	}
        } while (!rpi.receive()); // calls messageArrived
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
        	rpi = pollForReadyConnection();
	    	if (rpi == null) {
	    		//no channel is ready
	        	synchronized(this) {
	        		reader_busy = false;
	        		notifyAll();
	        	}
	        	return null;
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
    private MxReceivePortConnectionInfo pollForReadyConnection() throws IOException {
    	MxReceivePortConnectionInfo conn2;
    	ReceivePortConnectionInfo[] connections = connections();
    	
    	
    	for(ReceivePortConnectionInfo conn: connections) {
    		conn2 = (MxReceivePortConnectionInfo)conn;
    		if(conn2.available()) {
    			//logger.debug("pollForReadyConnection() found channel");
    			return conn2;
    		}
    	}
    	return null;
    }
	
    // poll for a connection that has some data available
    private MxReceivePortConnectionInfo getReadyConnection(long deadline) throws IOException {   	
    	/* FIXME using a deadline in the function definition is asking for bugs created 
    	 * by calling this function with a timeout instead. So change deadline to timeout
    	 */
    	MxReceivePortConnectionInfo result = null;
    	
    	if(deadline == 0) {
			while (result == null) {
				synchronized(this) {
					if(closed) {
						return null;
					}
				}

				// TODO determine a nice poll interval
				result = pollForReadyConnection();
				if(result != null) {
					//logger.debug("getReadyConnection(deadline) found channel");
					return result;
				}
				ReadChannel rc = channelManager.select(100);
	    		if(rc != null) {
	    			result = (MxReceivePortConnectionInfo)(rc.getOwner());	
	    		}
			}
			/*if(logger.isDebugEnabled() && result != 0) {
				logger.debug("getReadyConnection(deadline) found channel");
			}*/
	    	return result;	
    	} else {
    		// we have a deadline
	    	// poll whether a channel is ready
	    	result = pollForReadyConnection();
	    	if (result == null) {
	    		// no one is ready, wait until someone is (or the timeout expired)
	    		ReadChannel rc = channelManager.select(deadline - System.currentTimeMillis());
	    		if(rc != null) {
	    			result = (MxReceivePortConnectionInfo)(rc.getOwner());	
	    		}
	    	}
	    	/*if(logger.isDebugEnabled()) {
	    		if(result != 0) {
	    			logger.debug("getReadyConnection(deadline) found channel");
	    		} else {
	    			logger.debug("getReadyConnection(deadline) did not find channel");
	    		}
	    	}*/
	    	return result;
		}	
    }
    	
    synchronized void accept(ConnectionRequest req, SendPortIdentifier origin, PortType sp) {
    	//TODO check descriptor
    	int result = connectionAllowed(origin, sp);
    	
    	//TODO set reply message
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);        
        try {
			out.writeInt(result);
	        if (result == ReceivePort.TYPE_MISMATCH) {
	            getPortType().writeTo(out);
	        }
	        out.flush();
        } catch (IOException e) {
			e.printStackTrace();
			throw new Error("error creating connection reply");
		}
        req.setReplyMessage(baos.toByteArray());    	
    	
    	if (result == ACCEPTED) {
    		ReadChannel rc = channelManager.accept(req);
    		if(rc == null) {
    			result = DENIED;
    			this.lostConnection(origin, new IOException("ChannelManager denied connection"));
    		} else {
            	try {
                	@SuppressWarnings("unused")
					MxReceivePortConnectionInfo conn = new MxReceivePortConnectionInfo(origin, rc, this, new BufferedArrayInputStream(rc.getInputStream(), SimpleOutputStream.bufferSize()));
				} catch (IOException e) {
					result = DENIED;
					//TODO set error message
					this.lostConnection(origin, e);
				}
	        	/*
	        	if (logger.isDebugEnabled()) {
	                logger.debug("--> S RP = " + name + ": "
	                        + ReceivePort.getString(result));
	            }*/
    		}
    	} else {  		
            req.reject();
    	}
    }
    
}


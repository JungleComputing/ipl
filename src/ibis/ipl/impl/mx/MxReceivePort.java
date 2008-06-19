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

class MxReceivePort extends ReceivePort implements Identifiable<MxReceivePort> {
	private static Logger logger = Logger.getLogger(MxReceivePort.class);
	
	
	protected short portId = 0;
	protected IdManager<MxReceivePortConnectionInfo> channelManager;
	protected IdManager<MxReceivePort> portManager = null;
	
	private boolean reader_busy = false;

	
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
		//TODO portMAnager setup

		channelManager = new IdManager<MxReceivePortConnectionInfo>();
	}
		
	/* ********************************************
	 * Methods that can be overridden optionally:
	 *********************************************/

	@Override
	public synchronized void addInfo(SendPortIdentifier id,
			ReceivePortConnectionInfo info) {
		// TODO Auto-generated method stub
		super.addInfo(id, info);
	}

	@Override
	public synchronized ReceivePortConnectionInfo[] connections() {
		// TODO Auto-generated method stub
		return super.connections();
	}

	@Override
	public void doUpcall(ReadMessage msg) {
		// TODO Auto-generated method stub
		super.doUpcall(msg);
	}

	@Override
	public synchronized ReceivePortConnectionInfo getInfo(SendPortIdentifier id) {
		// TODO Auto-generated method stub
		return super.getInfo(id);
	}

	@Override
	public ReadMessage getMessage(long timeout) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("GetMessage()");
		}
		// Allow only one reader in.
        synchronized(this) {
            while (reader_busy && ! closed) {
                try {
                    wait();
                } catch(Exception e) {
                    // ignored
                }
            }
            if (closed) {
                throw new IOException("receive() on closed port");
            }
            reader_busy = true;
        }
        // Since we don't have any threads or timeout here, this 'reader' 
        // call directly handles the receive.              
        for (;;) {
            // Wait until there is a connection            
            synchronized(this) {
                while (connections.size() == 0 && ! closed) {
                    try {
                        wait();
                    } catch (Exception e) {
                        /* ignore */
                    }
                }

                // Wait until the current message is done
                while (message != null && ! closed) {
                    try {
                        wait();
                    } catch (Exception e) {
                        /* ignore */
                    }
                }
                if (closed) {
                    reader_busy = false;
                    notifyAll();
                    throw new IOException("receive() on closed port");
                }
            }

            ReceivePortConnectionInfo conns[] = connections();
            // Note: This call does NOT always result in a message!
            ((MxReceivePortConnectionInfo)conns[0]).receive(); // why 'conns[0]'?
            synchronized(this) {
                if (message != null) {
            		if (logger.isDebugEnabled()) {
            			logger.debug("GetMessage() finished");
            		}
                    reader_busy = false;
                    notifyAll();
                    return message;
                }
            }
        }
	}

	@Override
	public void lostConnection(SendPortIdentifier id, Throwable e) {
		// TODO Auto-generated method stub
		super.lostConnection(id, e);
	}

	@Override
	public synchronized ReceivePortConnectionInfo removeInfo(
			SendPortIdentifier id) {
		// TODO Auto-generated method stub
		return super.removeInfo(id);
	}

	@Override
	protected ReadMessage doPoll() throws IOException {
		// TODO Auto-generated method stub
		return super.doPoll();
	}

	@Override
	public synchronized void finishMessage(ReadMessage r, IOException e) {
		// TODO Auto-generated method stub
		super.finishMessage(r, e);
	}

	@Override
	public synchronized void finishMessage(ReadMessage r, long cnt) {
		// TODO Auto-generated method stub
		super.finishMessage(r, cnt);
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
		super.messageArrived(msg);

	}
	
	@Override
	public synchronized void closePort(long timeout) {
		if (logger.isDebugEnabled()) {
			logger.debug("closing port...");
		}
		if(timeout == 0) {
			//TODO hack: timeout is broken otherwise
			//timeout = 1;
		}
		super.closePort(timeout);
		portManager.remove(portId);
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

}
